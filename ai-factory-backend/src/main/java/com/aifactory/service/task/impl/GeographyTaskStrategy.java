package com.aifactory.service.task.impl;

import com.aifactory.common.WorldviewXmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 地理环境独立生成任务策略
 * <p>
 * 三步流程：
 * 1. 清理旧地理环境数据 — 删除项目下所有 continent_region
 * 2. AI生成地理环境 — 调用 llm_geography_create 独立提示词模板
 * 3. 保存地理环境 — 委托 WorldviewXmlParser 解析 XML，调用 ContinentRegionService.saveTree 入库
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@Slf4j
@Component
public class GeographyTaskStrategy implements TaskStrategy {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private WorldviewXmlParser worldviewXmlParser;

    @Override
    public String getTaskType() {
        return "geography";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧地理环境数据", "clean_geography", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成地理环境", "generate_geography", new HashMap<>()));
        steps.add(new StepConfig(3, "保存地理环境", "save_geography", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        String stepType = step.getStepType();
        switch (stepType) {
            case "clean_geography":
                return cleanGeography(step, context);
            case "generate_geography":
                return generateGeography(step, context);
            case "save_geography":
                return saveGeography(step, context);
            default:
                return StepResult.failure("未知的步骤类型: " + stepType);
        }
    }

    // ======================== clean_geography ========================

    /**
     * 清理旧地理环境数据 — 删除项目下所有 continent_region 记录
     */
    private StepResult cleanGeography(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            continentRegionService.deleteByProjectId(projectId);
            log.info("清理旧地理环境数据完成, projectId={}", projectId);
            return StepResult.success(Map.of("projectId", projectId), 100);
        } catch (Exception e) {
            log.error("清理旧地理环境数据失败", e);
            return StepResult.failure("清理旧地理环境数据失败: " + e.getMessage());
        }
    }

    // ======================== generate_geography ========================

    /**
     * AI生成地理环境 — 加载项目信息，构建模板变量，调用 LLM
     */
    private StepResult generateGeography(AiTaskStep step, TaskContext context) {
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

            String prompt = promptTemplateService.executeTemplate("llm_geography_create", variables);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_geography_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("AI生成地理环境完成, projectId={}, 响应长度={}", projectId, responseContent.length());

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("AI生成地理环境失败", e);
            return StepResult.failure("AI生成地理环境失败: " + e.getMessage());
        }
    }

    // ======================== save_geography ========================

    /**
     * 保存地理环境 — 委托 WorldviewXmlParser 解析 AI 响应中的 {@code <g>} 标签，
     * 调用 ContinentRegionService.saveTree 入库
     */
    private StepResult saveGeography(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的地理环境内容");
            }

            List<NovelContinentRegion> rootNodes = worldviewXmlParser.parseGeographyXml(aiResponse, projectId);
            if (!rootNodes.isEmpty()) {
                continentRegionService.saveTree(projectId, rootNodes);
                log.info("地理区域入库完成, projectId={}, 根节点数={}", projectId, rootNodes.size());
            }

            return StepResult.success(Map.of("projectId", projectId), 100);

        } catch (Exception e) {
            log.error("保存地理环境失败", e);
            return StepResult.failure("保存地理环境失败: " + e.getMessage());
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
