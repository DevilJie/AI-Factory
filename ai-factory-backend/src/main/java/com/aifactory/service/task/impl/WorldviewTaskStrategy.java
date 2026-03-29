package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.aifactory.util.XmlParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 世界观生成任务策略
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

    @Override
    public String getTaskType() {
        return "worldview";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            JsonNode config = objectMapper.readTree(task.getConfigJson());

            List<StepConfig> steps = new ArrayList<>();

            // 步骤: 检查是否已有世界观设定
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

    /**
     * 步骤: 检查是否已有世界观设定
     * 如果存在则删除，允许重新生成
     */
    private StepResult checkExisting(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            log.info("检查项目 {} 是否已有世界观设定", projectId);

            NovelWorldview existingWorldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            if (existingWorldview != null) {
                log.info("项目 {} 已有世界观设定，ID={}，将删除并重新生成", projectId, existingWorldview.getId());

                // 删除旧的世界观设定，允许重新生成
                worldviewMapper.deleteById(existingWorldview.getId());
                context.putSharedData("skipGeneration", false);
            } else {
                log.info("项目 {} 没有世界观设定，需要生成", projectId);
                context.putSharedData("skipGeneration", false);
            }

            return StepResult.success(Map.of("hasExisting", existingWorldview != null), 100);

        } catch (Exception e) {
            log.error("检查现有世界观失败", e);
            return StepResult.failure("检查现有世界观失败: " + e.getMessage());
        }
    }

    /**
     * 步骤: 生成世界观设定
     */
    private StepResult generateWorldview(AiTaskStep step, TaskContext context) {
        try {
            JsonNode config = context.getConfig();
                
            // 基础信息
            Project project = projectMapper.selectById(context.getProjectId());
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String storyGenre = project.getStoryGenre();
            String tags = project.getTags();

            // 构建生成世界观的提示词
            String prompt = buildWorldviewPrompt(projectDescription, storyTone, storyGenre, tags);

            // 调用AI生成世界观
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_worldview_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            // 设置更大的token限制以防止响应被截断
            aiRequest.setMaxTokens(8000);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成世界观原始响应:\n{}", responseContent);

            // 保存AI响应到上下文，供下一步解析
            context.putSharedData("aiResponse", responseContent);

            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("生成世界观设定失败", e);
            return StepResult.failure("生成世界观设定失败: " + e.getMessage());
        }
    }

    /**
     * 步骤: 保存世界观设定
     */
    private StepResult saveWorldview(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            // 获取AI响应
            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的世界观内容");
            }

            JsonNode config = context.getConfig();
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            // 解析并保存世界观到数据库
            NovelWorldview worldview = parseAndSaveWorldview(projectId, aiResponse, storyGenre);

            if (worldview != null) {
                log.info("世界观设定保存成功，ID: {}", worldview.getId());

                // 更新项目状态为 worldview_configured
                updateProjectSetupStage(projectId, "worldview_configured");

                return StepResult.success(Map.of(
                    "worldviewId", worldview.getId()
                ), 100);
            } else {
                return StepResult.failure("解析并保存世界观失败");
            }

        } catch (Exception e) {
            log.error("保存世界观设定失败", e);
            return StepResult.failure("保存世界观设定失败: " + e.getMessage());
        }
    }

    /**
     * 构建生成世界观的提示词
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String storyGenre, String tags) {
        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
        variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
        variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");

        // 执行模板
        String templateCode = "llm_worldview_create";
        String prompt = promptTemplateService.executeTemplate(templateCode, variables);

        // 添加XML格式要求（这部分保持原样）
        StringBuilder fullPrompt = new StringBuilder(prompt);

        fullPrompt.append("【重要】请严格按照以下XML格式返回世界观设定（使用简化标签节省token）：\n");
        fullPrompt.append("<w>\n");
        fullPrompt.append("  <t>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</t>\n");
        fullPrompt.append("  <b><![CDATA[世界背景描述（200-300字）]]></b>\n");
        fullPrompt.append("  <p><![CDATA[力量体系或修炼体系（如有），包括等级划分、能力来源等]]></p>\n");
        fullPrompt.append("  <g><![CDATA[地理环境描述，包括重要地点、国家、区域等]]></g>\n");
        fullPrompt.append("  <f><![CDATA[势力分布，包括主要组织、国家、门派等]]></f>\n");
        fullPrompt.append("  <l><![CDATA[时间线设定（如适用）]]></l>\n");
        fullPrompt.append("  <r><![CDATA[世界的基本规则和限制]]></r>\n");
        fullPrompt.append("</w>\n\n");

        fullPrompt.append("【XML格式要求】\n");
        fullPrompt.append("1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n");
        fullPrompt.append("2. 不要包含markdown代码块标记（```xml），直接返回XML\n");
        fullPrompt.append("3. 不要包含任何解释或说明文字，只返回XML数据\n\n");

        // 添加标签信息
        if (tags != null && !tags.isEmpty()) {
            fullPrompt.append("【标签】").append(tags).append("\n\n");
        }

        fullPrompt.append("内容要求：\n");
        fullPrompt.append("1. 世界观要符合故事类型和基调\n");
        fullPrompt.append("2. 力量体系要清晰、合理，有可发展性\n");
        fullPrompt.append("3. 各个要素之间要相互关联，形成完整的世界\n");
        fullPrompt.append("4. 返回的必须是纯XML格式，不要有任何其他说明文字\n");

        return fullPrompt.toString();
    }

    /**
     * 解析并保存世界观设定
     */
    private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {
            // 使用XML工具类解析（使用简化标签）
            // w=worldview, t=worldType, b=worldBackground, p=powerSystem
            // g=geography, f=forces, l=timeline, r=rules
            Map<String, String> worldviewData = XmlParser.parseXml(
                aiResponse,
                "w",
                "t", "b", "p", "g", "f", "l", "r"
            );

            if (worldviewData.isEmpty()) {
                log.warn("解析世界观XML失败，数据为空");
                return null;
            }

            // 将简化标签映射回标准字段名
            Map<String, String> standardData = new HashMap<>();
            if (worldviewData.containsKey("t")) {
                standardData.put("worldType", worldviewData.get("t"));
            }
            if (worldviewData.containsKey("b")) {
                standardData.put("worldBackground", worldviewData.get("b"));
            }
            if (worldviewData.containsKey("p")) {
                standardData.put("powerSystem", worldviewData.get("p"));
            }
            if (worldviewData.containsKey("g")) {
                standardData.put("geography", worldviewData.get("g"));
            }
            if (worldviewData.containsKey("f")) {
                standardData.put("forces", worldviewData.get("f"));
            }
            if (worldviewData.containsKey("l")) {
                standardData.put("timeline", worldviewData.get("l"));
            }
            if (worldviewData.containsKey("r")) {
                standardData.put("rules", worldviewData.get("r"));
            }

            // 获取项目信息以获取userId
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.error("项目不存在，projectId={}", projectId);
                return null;
            }

            log.info("项目信息: projectId={}, userId={}", project.getId(), project.getUserId());

            LocalDateTime now = LocalDateTime.now();
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);

            log.info("准备插入世界观: userId={}, projectId={}", worldview.getUserId(), worldview.getProjectId());
            worldview.setWorldType(standardData.getOrDefault("worldType", storyGenre));
            worldview.setWorldBackground(standardData.getOrDefault("worldBackground", ""));
            worldview.setPowerSystem(standardData.getOrDefault("powerSystem", ""));
            worldview.setGeography(standardData.getOrDefault("geography", ""));
            worldview.setForces(standardData.getOrDefault("forces", ""));
            worldview.setTimeline(standardData.getOrDefault("timeline", ""));
            worldview.setRules(standardData.getOrDefault("rules", ""));
            worldview.setCreateTime(now);
            worldview.setUpdateTime(now);

            worldviewMapper.insert(worldview);
            log.info("世界观设定保存成功，ID: {}", worldview.getId());

            return worldview;

        } catch (Exception e) {
            log.error("解析世界观失败", e);
            log.error("AI响应: {}", aiResponse);
            return null;
        }
    }

    /**
     * 更新项目的setup_stage状态
     *
     * @param projectId 项目ID
     * @param stage 要设置的阶段
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
