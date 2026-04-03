package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.WorldSettingXmlDto;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.NovelWorldviewPowerSystem;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.NovelWorldviewPowerSystemMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
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

import java.time.LocalDateTime;
import java.util.*;

/**
 * 世界观生成任务策略 — 9步编排器
 * <p>
 * 将地理环境、力量体系、阵营势力的生成委托给三个独立 Strategy bean，
 * 自身仅负责核心世界观（<t>/<b>/<l>/<r>）的生成和保存。
 * <p>
 * 9步流程（per D-04）：
 * 1. check_existing    — 删除旧世界观记录及关联（不删除模块数据）
 * 2. clean_geography   — 委托 GeographyTaskStrategy 清理旧地理环境
 * 3. generate_geography — 委托 GeographyTaskStrategy 生成+保存地理环境
 * 4. clean_power_system — 委托 PowerSystemTaskStrategy 清理旧力量体系
 * 5. generate_power_system — 委托 PowerSystemTaskStrategy 生成+保存力量体系
 * 6. clean_faction     — 委托 FactionTaskStrategy 清理旧阵营势力
 * 7. generate_faction  — 委托 FactionTaskStrategy 生成+保存阵营势力（含依赖上下文注入）
 * 8. generate_core     — 调用 llm_worldview_create 精简模板生成核心世界观
 * 9. save_core         — 保存核心世界观记录、建立力量体系关联、组装完整结果
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
    private PromptTemplateService promptTemplateService;

    @Autowired
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private FactionService factionService;

    @Autowired
    private XmlParser xmlParser;

    @Autowired
    private GeographyTaskStrategy geographyTaskStrategy;

    @Autowired
    private PowerSystemTaskStrategy powerSystemTaskStrategy;

    @Autowired
    private FactionTaskStrategy factionTaskStrategy;

    @Override
    public String getTaskType() {
        return "worldview";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "检查现有世界观", "check_existing", new HashMap<>()));
        steps.add(new StepConfig(2, "清理旧地理环境", "clean_geography", new HashMap<>()));
        steps.add(new StepConfig(3, "生成地理环境", "generate_geography", new HashMap<>()));
        steps.add(new StepConfig(4, "清理旧力量体系", "clean_power_system", new HashMap<>()));
        steps.add(new StepConfig(5, "生成力量体系", "generate_power_system", new HashMap<>()));
        steps.add(new StepConfig(6, "清理旧阵营势力", "clean_faction", new HashMap<>()));
        steps.add(new StepConfig(7, "生成阵营势力", "generate_faction", new HashMap<>()));
        steps.add(new StepConfig(8, "生成核心世界观", "generate_core", new HashMap<>()));
        steps.add(new StepConfig(9, "保存核心世界观", "save_core", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();
            switch (stepType) {
                case "check_existing": return checkExisting(step, context);
                case "clean_geography": return cleanGeography(step, context);
                case "generate_geography": return generateGeography(step, context);
                case "clean_power_system": return cleanPowerSystem(step, context);
                case "generate_power_system": return generatePowerSystem(step, context);
                case "clean_faction": return cleanFaction(step, context);
                case "generate_faction": return generateFaction(step, context);
                case "generate_core": return generateCore(step, context);
                case "save_core": return saveCore(step, context);
                default: return StepResult.failure("未知的步骤类型: " + stepType);
            }
        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    // ======================== Step 1: check_existing ========================

    /**
     * 检查现有世界观 — 仅删除世界观记录及世界观-力量体系关联，不删除模块数据。
     * 模块数据（地理环境、力量体系、阵营势力）由各自的 clean 步骤负责清理。
     */
    private StepResult checkExisting(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            log.info("检查项目 {} 是否已有世界观设定", projectId);

            NovelWorldview existing = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            if (existing != null) {
                // 删除世界观-力量体系关联
                worldviewPowerSystemMapper.delete(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existing.getId())
                );
                // 删除世界观记录
                worldviewMapper.deleteById(existing.getId());
                log.info("已删除旧世界观记录及关联, projectId={}, worldviewId={}", projectId, existing.getId());
            } else {
                log.info("项目 {} 没有世界观设定，需要生成", projectId);
            }

            return StepResult.success(Map.of("hadExisting", existing != null), 100);
        } catch (Exception e) {
            log.error("检查现有世界观失败", e);
            return StepResult.failure("检查现有世界观失败: " + e.getMessage());
        }
    }

    // ======================== Step 2-3: Geography delegation ========================

    private StepResult cleanGeography(AiTaskStep step, TaskContext context) {
        return geographyTaskStrategy.executeStep(createStepStub("clean_geography"), context);
    }

    private StepResult generateGeography(AiTaskStep step, TaskContext context) {
        StepResult genResult = geographyTaskStrategy.executeStep(createStepStub("generate_geography"), context);
        if (!genResult.isSuccess()) return genResult;
        return geographyTaskStrategy.executeStep(createStepStub("save_geography"), context);
    }

    // ======================== Step 4-5: PowerSystem delegation ========================

    private StepResult cleanPowerSystem(AiTaskStep step, TaskContext context) {
        return powerSystemTaskStrategy.executeStep(createStepStub("clean_power_system"), context);
    }

    private StepResult generatePowerSystem(AiTaskStep step, TaskContext context) {
        StepResult genResult = powerSystemTaskStrategy.executeStep(createStepStub("generate_power_system"), context);
        if (!genResult.isSuccess()) return genResult;
        return powerSystemTaskStrategy.executeStep(createStepStub("save_power_system"), context);
    }

    // ======================== Step 6-7: Faction delegation ========================

    private StepResult cleanFaction(AiTaskStep step, TaskContext context) {
        return factionTaskStrategy.executeStep(createStepStub("clean_faction"), context);
    }

    /**
     * 生成阵营势力 — 在地理环境和力量体系都已入库后构建依赖上下文，
     * 注入到 FactionTaskStrategy 的 config 中。
     */
    private StepResult generateFaction(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            // Per D-05: 在地理环境和力量体系已保存后构建上下文
            String geographyContext = continentRegionService.buildGeographyText(projectId);
            String powerSystemContext = powerSystemService.buildPowerSystemConstraint(projectId);

            // 合并 config：保留原有字段 + 添加依赖上下文
            Map<String, Object> configMap = new HashMap<>();
            JsonNode config = context.getConfig();
            if (config != null) {
                config.fields().forEachRemaining(entry ->
                    configMap.put(entry.getKey(), entry.getValue().asText()));
            }
            configMap.put("geographyContext", geographyContext);
            configMap.put("powerSystemContext", powerSystemContext);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode factionConfig = mapper.valueToTree(configMap);

            TaskContext factionContext = new TaskContext(
                context.getTaskId(), projectId, context.getTaskType(),
                factionConfig, context.getSharedData()
            );

            StepResult genResult = factionTaskStrategy.executeStep(createStepStub("generate_faction"), factionContext);
            if (!genResult.isSuccess()) return genResult;
            return factionTaskStrategy.executeStep(createStepStub("save_faction"), factionContext);
        } catch (Exception e) {
            log.error("生成阵营势力失败", e);
            return StepResult.failure("生成阵营势力失败: " + e.getMessage());
        }
    }

    // ======================== Step 8: generate_core ========================

    /**
     * 生成核心世界观 — 调用精简版 llm_worldview_create 模板，仅生成 <t>/<b>/<l>/<r>
     */
    private StepResult generateCore(AiTaskStep step, TaskContext context) {
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
            variables.put("novelType",
                storyGenre != null && !storyGenre.isEmpty() ? BasicSettingsDictionary.getNovelType(storyGenre) : "待补充");
            variables.put("tagsSection",
                tags != null && !tags.isEmpty() ? "【标签】" + tags : "");

            String prompt = promptTemplateService.executeTemplate("llm_worldview_create", variables);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_worldview_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("核心世界观生成完成, projectId={}, 响应长度={}", projectId, responseContent.length());

            context.putSharedData("coreAiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);
        } catch (Exception e) {
            log.error("生成核心世界观失败", e);
            return StepResult.failure("生成核心世界观失败: " + e.getMessage());
        }
    }

    // ======================== Step 9: save_core ========================

    /**
     * 保存核心世界观 — 解析 <t>/<b>/<l>/<r>，保存世界观记录，
     * 建立世界观-力量体系关联，更新项目阶段，重新查询完整数据。
     */
    private StepResult saveCore(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("coreAiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到核心世界观AI响应");
            }

            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return StepResult.failure("项目不存在");
            }

            // 解析 <w> 根标签下的 <t>/<b>/<l>/<r>
            WorldSettingXmlDto worldSetting = xmlParser.parse(aiResponse, WorldSettingXmlDto.class);

            // 保存核心世界观记录
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);
            worldview.setWorldBackground(worldSetting.getBackground());
            worldview.setTimeline(worldSetting.getTimeline());
            worldview.setRules(worldSetting.getRules());
            worldview.setCreateTime(LocalDateTime.now());
            worldview.setUpdateTime(LocalDateTime.now());
            worldviewMapper.insert(worldview);
            log.info("核心世界观保存成功, ID={}", worldview.getId());

            // 创建世界观-力量体系关联（per Pitfall 2: 此时力量体系已在步骤5中入库）
            List<NovelPowerSystem> systems = powerSystemService.listByProjectId(projectId);
            for (NovelPowerSystem ps : systems) {
                NovelWorldviewPowerSystem assoc = new NovelWorldviewPowerSystem();
                assoc.setWorldviewId(worldview.getId());
                assoc.setPowerSystemId(ps.getId());
                worldviewPowerSystemMapper.insert(assoc);
            }
            log.info("世界观-力量体系关联建立完成, worldviewId={}, systemCount={}", worldview.getId(), systems.size());

            // 更新项目阶段
            updateProjectSetupStage(projectId, "worldview_configured");

            // 重新查询完整数据（含地理环境和势力填充，per D-06/D-07）
            NovelWorldview complete = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );
            if (complete != null) {
                continentRegionService.fillGeography(complete);
                factionService.fillForces(complete);
            }

            return StepResult.success(Map.of("worldviewId", complete != null ? complete.getId() : null), 100);
        } catch (Exception e) {
            log.error("保存核心世界观失败", e);
            return StepResult.failure("保存核心世界观失败: " + e.getMessage());
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 创建 AiTaskStep 桩对象 — 仅设置 stepType 用于 Strategy 内部 switch 分发
     */
    private AiTaskStep createStepStub(String stepType) {
        AiTaskStep stub = new AiTaskStep();
        stub.setStepType(stepType);
        return stub;
    }

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
