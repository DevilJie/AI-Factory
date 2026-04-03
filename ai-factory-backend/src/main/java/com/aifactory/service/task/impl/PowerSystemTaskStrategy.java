package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.WorldSettingXmlDto;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.entity.NovelPowerSystemLevel;
import com.aifactory.entity.NovelPowerSystemLevelStep;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelPowerSystemLevelMapper;
import com.aifactory.mapper.NovelPowerSystemLevelStepMapper;
import com.aifactory.mapper.NovelPowerSystemMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 力量体系独立生成任务策略
 * <p>
 * 三步流程：
 * 1. 清理旧力量体系数据 — 级联删除项目下所有 power_system + level + step
 * 2. AI生成力量体系 — 调用 llm_power_system_create 独立提示词模板
 * 3. 保存力量体系 — 解析 <p> XML 数据，写入 novel_power_system / level / step 三表
 * <p>
 * 设计要点：
 * - 使用 XmlParser（Jackson XML）解析 <p> 标签（无嵌套同名标签问题）
 * - 不创建 novel_worldview_power_system 关联记录（独立生成时世界观可能不存在，Phase 8 处理关联）
 * - @Component 注解确保 AsyncTaskExecutor 的 strategyMap 自动注册
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@Slf4j
@Component
public class PowerSystemTaskStrategy implements TaskStrategy {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private NovelPowerSystemMapper powerSystemMapper;

    @Autowired
    private NovelPowerSystemLevelMapper levelMapper;

    @Autowired
    private NovelPowerSystemLevelStepMapper stepMapper;

    @Autowired
    private XmlParser xmlParser;

    @Override
    public String getTaskType() {
        return "power_system";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧力量体系数据", "clean_power_system", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成力量体系", "generate_power_system", new HashMap<>()));
        steps.add(new StepConfig(3, "保存力量体系", "save_power_system", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        String stepType = step.getStepType();
        switch (stepType) {
            case "clean_power_system":
                return cleanPowerSystem(step, context);
            case "generate_power_system":
                return generatePowerSystem(step, context);
            case "save_power_system":
                return savePowerSystem(step, context);
            default:
                return StepResult.failure("未知的步骤类型: " + stepType);
        }
    }

    // ======================== clean_power_system ========================

    /**
     * 清理旧力量体系数据 — 级联删除项目下所有 power_system（含 level + step）
     */
    private StepResult cleanPowerSystem(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            List<NovelPowerSystem> systems = powerSystemService.listByProjectId(projectId);
            for (NovelPowerSystem system : systems) {
                powerSystemService.deleteById(system.getId());
            }
            log.info("清理旧力量体系数据完成, projectId={}, 删除数量={}", projectId, systems.size());
            return StepResult.success(Map.of("projectId", projectId, "deletedCount", systems.size()), 100);
        } catch (Exception e) {
            log.error("清理旧力量体系数据失败", e);
            return StepResult.failure("清理旧力量体系数据失败: " + e.getMessage());
        }
    }

    // ======================== generate_power_system ========================

    /**
     * AI生成力量体系 — 加载项目信息，构建模板变量，调用 LLM
     */
    private StepResult generatePowerSystem(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return StepResult.failure("项目不存在, projectId=" + projectId);
            }

            JsonNode config = context.getConfig();
            String projectDescription = getTextOrDefault(config, "projectDescription", project.getDescription(), "待补充");
            String storyTone = getTextOrDefault(config, "storyTone", project.getStoryTone(), null);
            String storyGenre = getTextOrDefault(config, "storyGenre", project.getNovelType(), null);
            String tags = config != null && config.has("tags") ? config.get("tags").asText() : project.getTags();

            Map<String, Object> variables = new HashMap<>();
            variables.put("projectDescription",
                projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone",
                storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
            variables.put("storyGenre",
                storyGenre != null && !storyGenre.isEmpty() ? BasicSettingsDictionary.getNovelType(storyGenre) : "待补充");
            variables.put("tagsSection",
                tags != null && !tags.isEmpty() ? "【标签】" + tags : "");

            String prompt = promptTemplateService.executeTemplate("llm_power_system_create", variables);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_power_system_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("AI生成力量体系完成, projectId={}, 响应长度={}", projectId, responseContent.length());

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("AI生成力量体系失败", e);
            return StepResult.failure("AI生成力量体系失败: " + e.getMessage());
        }
    }

    // ======================== save_power_system ========================

    /**
     * 保存力量体系 — 使用 XmlParser 解析 AI 响应，写入三表
     * <p>
     * 关键差异：不创建 novel_worldview_power_system 关联记录
     */
    private StepResult savePowerSystem(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的力量体系内容");
            }

            WorldSettingXmlDto worldSetting = xmlParser.parse(aiResponse, WorldSettingXmlDto.class);
            if (worldSetting.getSystems() == null || worldSetting.getSystems().getSystemList() == null
                || worldSetting.getSystems().getSystemList().isEmpty()) {
                log.warn("力量体系解析结果为空, projectId={}", projectId);
                return StepResult.success(Map.of("projectId", projectId, "systemCount", 0), 100);
            }

            LocalDateTime now = LocalDateTime.now();
            int systemCount = 0;

            for (WorldSettingXmlDto.CultivationSystem cs : worldSetting.getSystems().getSystemList()) {
                // 插入力量体系主记录
                NovelPowerSystem powerSystem = new NovelPowerSystem();
                powerSystem.setProjectId(projectId);
                powerSystem.setName(cs.getName());
                powerSystem.setSourceFrom(cs.getSourceFrom());
                powerSystem.setCoreResource(cs.getCoreResource());
                powerSystem.setCultivationMethod(cs.getCultivationMethod());
                powerSystem.setDescription(cs.getDescription());
                powerSystem.setCreateTime(now);
                powerSystem.setUpdateTime(now);
                powerSystemMapper.insert(powerSystem);
                log.info("力量体系保存成功, ID={}, name={}", powerSystem.getId(), cs.getName());

                // 插入等级记录
                if (cs.getLevels() != null && cs.getLevels().getLevelList() != null) {
                    int levelIndex = 1;
                    for (WorldSettingXmlDto.CultivationLevel cl : cs.getLevels().getLevelList()) {
                        NovelPowerSystemLevel level = new NovelPowerSystemLevel();
                        level.setPowerSystemId(powerSystem.getId());
                        level.setLevel(levelIndex++);
                        level.setLevelName(cl.getLevelName());
                        level.setDescription(cl.getDescription());
                        level.setBreakthroughCondition(cl.getBreakthroughCondition());
                        level.setLifespan(cl.getLifespan());
                        level.setPowerRange(cl.getPowerRange());
                        level.setLandmarkAbility(cl.getLandmarkAbility());
                        level.setCreateTime(now);
                        level.setUpdateTime(now);
                        levelMapper.insert(level);

                        // 插入境界记录
                        if (cl.getSteps() != null && cl.getSteps().getStepList() != null) {
                            int stepIndex = 1;
                            for (String stepName : cl.getSteps().getStepList()) {
                                NovelPowerSystemLevelStep stepEntity = new NovelPowerSystemLevelStep();
                                stepEntity.setPowerSystemLevelId(level.getId());
                                stepEntity.setLevel(stepIndex++);
                                stepEntity.setLevelName(stepName);
                                stepEntity.setCreateTime(now);
                                stepEntity.setUpdateTime(now);
                                stepMapper.insert(stepEntity);
                            }
                        }
                    }
                    log.info("已保存 {} 个等级 for system={}", cs.getLevels().getLevelList().size(), cs.getName());
                }

                systemCount++;
            }

            log.info("力量体系入库完成, projectId={}, 共{}套体系", projectId, systemCount);
            return StepResult.success(Map.of("projectId", projectId, "systemCount", systemCount), 100);

        } catch (Exception e) {
            log.error("保存力量体系失败", e);
            return StepResult.failure("保存力量体系失败: " + e.getMessage());
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 从 config 中读取文本值，如果不存在则使用 fallback
     */
    private String getTextOrDefault(JsonNode config, String key, String primaryFallback, String secondaryFallback) {
        if (config != null && config.has(key)) {
            String value = config.get(key).asText();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        if (primaryFallback != null && !primaryFallback.isEmpty()) {
            return primaryFallback;
        }
        return secondaryFallback;
    }
}
