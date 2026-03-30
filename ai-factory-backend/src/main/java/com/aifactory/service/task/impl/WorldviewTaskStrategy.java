package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.*;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.*;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 世界观生成任务策略
 * <p>
 * 改造要点：
 * 1. 提示词模板化 — 所有 prompt 内容由 DB 模板驱动，不再硬编码 XML 格式要求
 * 2. XML 解析力量体系入库 — AI 返回的 <p><system>...</system></p> 会被解析并写入
 * novel_power_system / novel_power_system_level / novel_power_system_level_step 三张表
 * 3. 关联表 novel_worldview_power_system 在保存时自动建立
 * 4. 移除 novel_worldview.powerSystem 文本字段的一切读写
 *
 * @Author CaiZy
 * @Date 2025-02-02
 */
@Slf4j
@Component
public class WorldviewTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ObjectMapper objectMapper;

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
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Autowired
    private XmlParser xmlParser;

    @Override
    public String getTaskType() {
        return "worldview";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            List<StepConfig> steps = new ArrayList<>();

            // 步骤: 检查是否已有世界观设定（删除旧数据 + 关联）
            steps.add(new StepConfig(1, "检查现有世界观", "check_existing", new HashMap<>()));

            // 步骤: 生成世界观设定
            steps.add(new StepConfig(2, "生成世界观设定", "generate_worldview", new HashMap<>()));

            // 步骤: 保存世界观设定
            steps.add(new StepConfig(3, "保存世界观设定", "save_worldview", new HashMap<>()));

            log.info("创建世界观任务步骤完成，项目ID: {}", task.getProjectId());
            return steps;

        } catch (Exception e) {
            log.error("创建世界观任务步骤失败", e);
            throw new RuntimeException("创建世界观任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "check_existing":
                    return checkExisting(step, context);
                case "generate_worldview":
                    return generateWorldview(step, context);
                case "save_worldview":
                    return saveWorldview(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    // ======================== check_existing ========================

    /**
     * 检查是否已有世界观设定，如有则删除旧世界观 + 关联的力量体系行
     */
    private StepResult checkExisting(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            log.info("检查项目 {} 是否已有世界观设定", projectId);

            NovelWorldview existingWorldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            boolean hadExisting = existingWorldview != null;

            if (hadExisting) {
                log.info("项目 {} 已有世界观设定，ID={}，将删除并重新生成", projectId, existingWorldview.getId());

                // 1. 查找关联的力量体系ID
                List<NovelWorldviewPowerSystem> associations = worldviewPowerSystemMapper.selectList(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existingWorldview.getId())
                );

                // 2. 级联删除力量体系（等级→境界→体系）
                for (NovelWorldviewPowerSystem assoc : associations) {
                    Long psId = assoc.getPowerSystemId();
                    // 删除境界
                    List<NovelPowerSystemLevel> levels = levelMapper.selectList(
                        new LambdaQueryWrapper<NovelPowerSystemLevel>()
                            .eq(NovelPowerSystemLevel::getPowerSystemId, psId)
                    );
                    for (NovelPowerSystemLevel level : levels) {
                        stepMapper.delete(
                            new LambdaQueryWrapper<NovelPowerSystemLevelStep>()
                                .eq(NovelPowerSystemLevelStep::getPowerSystemLevelId, level.getId())
                        );
                    }
                    // 删除等级
                    levelMapper.delete(
                        new LambdaQueryWrapper<NovelPowerSystemLevel>()
                            .eq(NovelPowerSystemLevel::getPowerSystemId, psId)
                    );
                    // 删除力量体系
                    powerSystemMapper.deleteById(psId);
                }

                // 3. 删除关联表行
                worldviewPowerSystemMapper.delete(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existingWorldview.getId())
                );

                // 4. 删除旧世界观
                worldviewMapper.deleteById(existingWorldview.getId());
            } else {
                log.info("项目 {} 没有世界观设定，需要生成", projectId);
            }

            context.putSharedData("skipGeneration", false);
            return StepResult.success(Map.of("hasExisting", hadExisting), 100);

        } catch (Exception e) {
            log.error("检查现有世界观失败", e);
            return StepResult.failure("检查现有世界观失败: " + e.getMessage());
        }
    }

    // ======================== generate_worldview ========================

    /**
     * 生成世界观设定 — 提示词完全由 DB 模板驱动
     */
    private StepResult generateWorldview(AiTaskStep step, TaskContext context) {
        try {
            Project project = projectMapper.selectById(context.getProjectId());
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String novelType = project.getNovelType();
            String tags = project.getTags();

            // 构建提示词（模板已包含 XML 格式要求，不再手动追加）
            String prompt = buildWorldviewPrompt(projectDescription, storyTone, novelType, tags);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_worldview_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成世界观原始响应:\n{}", responseContent);

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("生成世界观设定失败", e);
            return StepResult.failure("生成世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== save_worldview ========================

    /**
     * 保存世界观设定：解析 XML、存储世界观基本信息 + 结构化力量体系入库 + 建立关联
     */
    private StepResult saveWorldview(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的世界观内容");
            }

            JsonNode config = context.getConfig();
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            // 解析并保存
            NovelWorldview worldview = parseAndSaveWorldview(projectId, aiResponse, storyGenre);

            if (worldview != null) {
                log.info("世界观设定保存成功，ID: {}", worldview.getId());

                updateProjectSetupStage(projectId, "worldview_configured");

                return StepResult.success(Map.of("worldviewId", worldview.getId()), 100);
            } else {
                return StepResult.failure("解析并保存世界观失败");
            }

        } catch (Exception e) {
            log.error("保存世界观设定失败", e);
            return StepResult.failure("保存世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== buildWorldviewPrompt ========================

    /**
     * 构建生成世界观的提示词 — 完全由 DB 模板驱动
     * <p>
     * 模板（llm_worldview_create）已包含完整的 XML 格式要求，
     * 这里只负责准备模板变量和补充 tags 信息。
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String novelType, String tags) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
        variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
        variables.put("novelType", novelType != null && !novelType.isEmpty() ? BasicSettingsDictionary.getNovelType(novelType) : "待补充");

        // tags 是可选的，模板中使用 {tagsSection} 占位
        if (tags != null && !tags.isEmpty()) {
            variables.put("tagsSection", "【标签】" + tags);
        } else {
            variables.put("tagsSection", "");
        }

        String templateCode = "llm_worldview_create";

        return promptTemplateService.executeTemplate(templateCode, variables);
    }

    // ======================== parseAndSaveWorldview ========================

    /**
     * 解析 AI 响应并保存世界观 + 力量体系
     * <p>
     * 流程：
     * 1. 用 XmlParser 解析 <w> 根标签下的 t/b/p/g/f/l/r 字段
     * 2. 将 t/b/g/f/l/r 保存到 novel_worldview（不含 powerSystem）
     * 3. 解析 <p> 下的 <system> 子节点，结构化写入 novel_power_system / level / step
     * 4. 建立 novel_worldview_power_system 关联
     */
    private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {

            WorldSettingXmlDto worldSetting = xmlParser.parse(aiResponse, WorldSettingXmlDto.class);

            // Step 2: 获取项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.error("项目不存在，projectId={}", projectId);
                return null;
            }

            LocalDateTime now = LocalDateTime.now();

            // Step 3: 保存世界观基本信息（不含 powerSystem）
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);
            worldview.setWorldBackground(worldSetting.getBackground());
            worldview.setGeography(worldSetting.getGeography());
            worldview.setForces(worldSetting.getForces());
            worldview.setTimeline(worldSetting.getTimeline());
            worldview.setRules(worldSetting.getRules());
            worldview.setCreateTime(now);
            worldview.setUpdateTime(now);

            worldviewMapper.insert(worldview);
            log.info("世界观基本信息保存成功，ID: {}", worldview.getId());

            // Step 4: 解析并保存结构化力量体系
            savePowerSystems(projectId, worldview.getId(), worldSetting.getSystems(), now);

            return worldview;

        } catch (Exception e) {
            log.error("解析世界观失败", e);
            log.error("AI响应: {}", aiResponse);
            return null;
        }
    }

    // ======================== savePowerSystems ========================

    /**
     * 解析并保存结构化力量体系
     * <p>
     * 流程：
     * 1. 遍历每个 <system>，插入 novel_power_system
     * 2. 遍历 <levels>/<level>，插入 novel_power_system_level
     * 3. 遍历 <steps>/<step>，插入 novel_power_system_level_step
     * 4. 建立 novel_worldview_power_system 关联
     */
    private void savePowerSystems(Long projectId, Long worldviewId, WorldSettingXmlDto.Systems systems, LocalDateTime now) {
        if (systems == null || systems.getSystemList() == null || systems.getSystemList().isEmpty()) {
            log.info("无力量体系数据，跳过入库");
            return;
        }

        for (WorldSettingXmlDto.CultivationSystem cs : systems.getSystemList()) {
            // 1. 插入力量体系主记录
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
            log.info("力量体系保存成功，ID={}, name={}", powerSystem.getId(), cs.getName());

            // 2. 插入等级记录
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
                    log.debug("等级保存成功，ID={}, name={}", level.getId(), cl.getLevelName());

                    // 3. 插入境界记录
                    if (cl.getSteps() != null && cl.getSteps().getStepList() != null) {
                        int stepIndex = 1;
                        for (String stepName : cl.getSteps().getStepList()) {
                            NovelPowerSystemLevelStep step = new NovelPowerSystemLevelStep();
                            step.setPowerSystemLevelId(level.getId());
                            step.setLevel(stepIndex++);
                            step.setLevelName(stepName);
                            step.setCreateTime(now);
                            step.setUpdateTime(now);
                            stepMapper.insert(step);
                        }
                        log.debug("已保存 {} 个境界", cl.getSteps().getStepList().size());
                    }
                }
                log.info("已保存 {} 个等级", cs.getLevels().getLevelList().size());
            }

            // 4. 建立世界观-力量体系关联
            NovelWorldviewPowerSystem association = new NovelWorldviewPowerSystem();
            association.setWorldviewId(worldviewId);
            association.setPowerSystemId(powerSystem.getId());
            worldviewPowerSystemMapper.insert(association);
            log.info("世界观-力量体系关联已建立，worldviewId={}, powerSystemId={}", worldviewId, powerSystem.getId());
        }

        log.info("力量体系入库完成，共 {} 套体系", systems.getSystemList().size());
    }

    // ======================== updateProjectSetupStage ========================

    /**
     * 更新项目的 setup_stage 状态
     */
    private void updateProjectSetupStage(Long projectId, String stage) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                project.setSetupStage(stage);
                projectMapper.updateById(project);
                log.info("项目状态已更新，projectId={}, setupStage={}", projectId, stage);
            }
        } catch (Exception e) {
            log.error("更新项目状态失败，projectId={}, stage={}", projectId, stage, e);
        }
    }
}
