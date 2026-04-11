package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelOutline;
import com.aifactory.entity.Project;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.mapper.NovelOutlineMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.ForeshadowingService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.dto.ForeshadowingQueryDto;
import com.aifactory.dto.ForeshadowingDto;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptContextBuilder;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI大纲生成任务策略
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Slf4j
@Component
public class OutlineTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelOutlineMapper outlineMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelCharacterMapper characterMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PromptContextBuilder promptContextBuilder;

    @Autowired
    private com.aifactory.service.chapter.prompt.PromptTemplateBuilder promptTemplateBuilder;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private FactionService factionService;

    @Autowired
    private ForeshadowingService foreshadowingService;

    @Override
    public String getTaskType() {
        return "outline";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            JsonNode config = objectMapper.readTree(task.getConfigJson());
            int targetVolumeCount = config.has("targetVolumeCount") ? config.get("targetVolumeCount").asInt() : 3;
            boolean regenerate = config.has("regenerate") && config.get("regenerate").asBoolean();

            List<StepConfig> steps = new ArrayList<>();
            int stepOrder = 1;

            // 重新生成模式：第一步清空现有数据
            if (regenerate) {
                steps.add(new StepConfig(stepOrder++, "清空现有大纲", "clear_existing_outline", new HashMap<>()));
            }

            // 计算起始卷号：查询数据库已有分卷数
            int startVolumeNumber;
            if (regenerate) {
                startVolumeNumber = 1;
            } else {
                // 从 config 中读取 startVolumeNumber（前端可指定），默认从已有分卷之后续接
                if (config.has("startVolumeNumber") && config.get("startVolumeNumber").asInt() > 1) {
                    startVolumeNumber = config.get("startVolumeNumber").asInt();
                } else {
                    // 查询数据库已有分卷数量，自动续接
                    Long existingCount = volumePlanMapper.selectCount(
                        new LambdaQueryWrapper<NovelVolumePlan>()
                            .eq(NovelVolumePlan::getProjectId, task.getProjectId())
                    );
                    startVolumeNumber = existingCount.intValue() + 1;
                }
            }

            // 为每个分卷创建独立的 step
            for (int i = 0; i < targetVolumeCount; i++) {
                int volumeNumber = startVolumeNumber + i;
                Map<String, Object> stepConfig = new HashMap<>();
                stepConfig.put("volumeNumber", volumeNumber);

                String stepName = String.format("生成分卷规划（第%d卷）", volumeNumber);
                steps.add(new StepConfig(stepOrder++, stepName, "generate_single_volume", stepConfig));
            }

            // 最后一步: 保存并返回结果
            steps.add(new StepConfig(stepOrder, "保存大纲结果", "save_result", new HashMap<>()));

            log.info("创建大纲任务步骤完成，模式: {}, 起始卷号: {}, 生成数量: {}",
                regenerate ? "重新生成" : startVolumeNumber > 1 ? "继续生成" : "新生成",
                startVolumeNumber, targetVolumeCount);

            return steps;

        } catch (Exception e) {
            log.error("创建大纲任务步骤失败", e);
            throw new RuntimeException("创建大纲任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "clear_existing_outline":
                    return clearExistingOutline(step, context);
                case "generate_single_volume":
                    return generateSingleVolume(step, context);
                case "generate_chapters":
                    return generateChaptersForVolume(step, context);
                case "save_result":
                    return saveResult(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    @Override
    public int calculateProgress(AiTaskStep currentStep, List<AiTaskStep> allSteps) {
        return TaskStrategy.super.calculateProgress(currentStep, allSteps);
    }

    /**
     * 步骤: 清空现有大纲
     */
    private StepResult clearExistingOutline(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            log.info("开始清空项目 {} 的现有大纲数据", projectId);

            // 删除所有章节数据
            int deletedChapters = chapterPlanMapper.delete(
                new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(com.aifactory.entity.NovelChapterPlan::getProjectId, projectId)
            );

            // 删除所有分卷数据
            int deletedVolumes = volumePlanMapper.delete(
                new LambdaQueryWrapper<NovelVolumePlan>()
                    .eq(com.aifactory.entity.NovelVolumePlan::getProjectId, projectId)
            );

            // 删除大纲总览
            int deletedOutlines = outlineMapper.delete(
                new LambdaQueryWrapper<NovelOutline>()
                    .eq(com.aifactory.entity.NovelOutline::getProjectId, projectId)
            );

            log.info("清空项目 {} 大纲数据完成，删除大纲: {}, 分卷: {}, 章节: {}",
                projectId, deletedOutlines, deletedVolumes, deletedChapters);

            Map<String, Object> result = new HashMap<>();
            result.put("deletedOutlines", deletedOutlines);
            result.put("deletedVolumes", deletedVolumes);
            result.put("deletedChapters", deletedChapters);

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("清空现有大纲失败", e);
            return StepResult.failure("清空现有大纲失败: " + e.getMessage());
        }
    }

    /**
     * 步骤: 生成单个分卷规划
     * 每个 step 只生成一个分卷，1次 LLM 调用
     */
    private StepResult generateSingleVolume(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            // 从 step config 中获取当前要生成的卷号
            JsonNode stepConfigNode = objectMapper.readTree(step.getConfigJson());
            int volumeNumber = stepConfigNode.has("volumeNumber") ?
                stepConfigNode.get("volumeNumber").asInt() : 1;

            // 基础信息
            Project project = projectMapper.selectById(projectId);
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String storyGenre = project.getStoryGenre();
            String tags = project.getTags();

            log.info("开始生成第{}卷规划，项目ID: {}", volumeNumber, projectId);

            // 获取上一卷简介（仅上一卷，不是所有已有分卷）
            String previousVolumeDesc = getPreviousVolumeDescription(projectId, volumeNumber);

            // 使用 PromptContextBuilder 构建公共提示词片段
            String narrativeSettings = promptContextBuilder.buildBasicSettingsContext(projectId);
            String endingSettings = promptContextBuilder.buildEndingSettingsContext(projectId);
            String worldviewInfo = promptContextBuilder.buildWorldviewContext(projectId);

            // 构建模板变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
            variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");
            variables.put("tags", tags != null && !tags.isEmpty() ? tags : "待补充");
            variables.put("volumeNumber", volumeNumber);
            variables.put("narrativeSettings", narrativeSettings);
            variables.put("endingSettings", endingSettings);
            variables.put("worldviewInfo", worldviewInfo);
            variables.put("previousVolumeDesc", previousVolumeDesc != null && !previousVolumeDesc.isEmpty() ? previousVolumeDesc : "");

            // 执行模板
            String templateCode = "llm_outline_volume_generate";
            String prompt = promptTemplateService.executeTemplate(templateCode, variables);

            // 调用 LLM
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_outline_volume_generate");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成第{}卷原始响应:\n{}", volumeNumber, responseContent);

            // 解析 XML 响应
            String volumesXml = extractXml(responseContent);
            List<Map<String, String>> volumesList = parseVolumesXml(volumesXml);

            if (volumesList == null || volumesList.isEmpty()) {
                return StepResult.failure("第" + volumeNumber + "卷规划解析失败");
            }

            // 取第一个分卷（因为我们只生成一个）
            Map<String, String> volumeData = volumesList.get(0);

            // 获取或创建 outline
            Long outlineId = getOrCreateOutline(context);
            if (outlineId == null) {
                return StepResult.failure("获取或创建大纲记录失败");
            }

            // 保存到数据库
            saveSingleVolumeToDatabase(projectId, outlineId, volumeData);

            Map<String, Object> result = new HashMap<>();
            result.put("volumeNumber", volumeNumber);
            result.put("volumeTitle", volumeData.getOrDefault("volumeTitle", ""));

            log.info("第{}卷规划生成完成，项目ID: {}", volumeNumber, projectId);
            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("生成单卷规划失败", e);
            return StepResult.failure("生成单卷规划失败: " + e.getMessage());
        }
    }

    /**
     * 获取上一卷的简介（仅上一卷，不是所有已有分卷）
     */
    private String getPreviousVolumeDescription(Long projectId, int currentVolumeNumber) {
        if (currentVolumeNumber <= 1) {
            return "";
        }

        try {
            NovelVolumePlan previousVolume = volumePlanMapper.selectOne(
                new LambdaQueryWrapper<NovelVolumePlan>()
                    .eq(NovelVolumePlan::getProjectId, projectId)
                    .eq(NovelVolumePlan::getVolumeNumber, currentVolumeNumber - 1)
            );

            if (previousVolume == null || previousVolume.getVolumeDescription() == null
                || previousVolume.getVolumeDescription().isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("第").append(currentVolumeNumber - 1).append("卷简介：\n");
            sb.append(previousVolume.getVolumeDescription());
            return sb.toString();

        } catch (Exception e) {
            log.warn("获取上一卷简介失败", e);
            return "";
        }
    }

    /**
     * 保存单个分卷到数据库
     */
    private void saveSingleVolumeToDatabase(Long projectId, Long outlineId, Map<String, String> volumeData) {
        try {
            int volumeNumber = Integer.parseInt(volumeData.getOrDefault("volumeNumber", "0"));
            if (volumeNumber == 0) {
                log.warn("分卷编号为0，跳过");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // 检查是否已存在
            NovelVolumePlan existingVolume = volumePlanMapper.selectOne(
                new LambdaQueryWrapper<NovelVolumePlan>()
                    .eq(NovelVolumePlan::getProjectId, projectId)
                    .eq(NovelVolumePlan::getOutlineId, outlineId)
                    .eq(NovelVolumePlan::getVolumeNumber, volumeNumber)
            );

            if (existingVolume != null) {
                // 更新
                existingVolume.setVolumeTitle(volumeData.getOrDefault("volumeTitle", ""));
                existingVolume.setVolumeTheme(volumeData.getOrDefault("volumeTheme", ""));
                existingVolume.setMainConflict(volumeData.getOrDefault("mainConflict", ""));
                existingVolume.setPlotArc(volumeData.getOrDefault("plotArc", ""));
                existingVolume.setVolumeDescription(volumeData.getOrDefault("volumeDescription", ""));
                existingVolume.setKeyEvents(volumeData.getOrDefault("keyEvents", ""));
                existingVolume.setTimelineSetting(volumeData.getOrDefault("timelineSetting", ""));
                existingVolume.setCoreGoal(volumeData.getOrDefault("coreGoal", ""));
                existingVolume.setClimax(volumeData.getOrDefault("climax", ""));
                existingVolume.setEnding(volumeData.getOrDefault("ending", ""));
                existingVolume.setVolumeNotes(volumeData.getOrDefault("volumeNotes", ""));
                existingVolume.setStageForeshadowings(volumeData.getOrDefault("stageForeshadowings", "[]"));

                String targetChapterCountStr = volumeData.get("targetChapterCount");
                if (targetChapterCountStr != null && !targetChapterCountStr.isEmpty()) {
                    try {
                        existingVolume.setTargetChapterCount(Integer.parseInt(targetChapterCountStr));
                    } catch (NumberFormatException e) {
                        log.warn("目标章节数解析失败: {}", targetChapterCountStr);
                    }
                }

                existingVolume.setUpdateTime(now);
                volumePlanMapper.updateById(existingVolume);
                log.info("更新第{}卷: {}", volumeNumber, existingVolume.getVolumeTitle());
            } else {
                // 创建
                NovelVolumePlan volumePlan = new NovelVolumePlan();
                volumePlan.setProjectId(projectId);
                volumePlan.setOutlineId(outlineId);
                volumePlan.setVolumeNumber(volumeNumber);
                volumePlan.setVolumeTitle(volumeData.getOrDefault("volumeTitle", ""));
                volumePlan.setVolumeTheme(volumeData.getOrDefault("volumeTheme", ""));
                volumePlan.setMainConflict(volumeData.getOrDefault("mainConflict", ""));
                volumePlan.setPlotArc(volumeData.getOrDefault("plotArc", ""));
                volumePlan.setVolumeDescription(volumeData.getOrDefault("volumeDescription", ""));
                volumePlan.setKeyEvents(volumeData.getOrDefault("keyEvents", ""));
                volumePlan.setTimelineSetting(volumeData.getOrDefault("timelineSetting", ""));
                volumePlan.setCoreGoal(volumeData.getOrDefault("coreGoal", ""));
                volumePlan.setClimax(volumeData.getOrDefault("climax", ""));
                volumePlan.setEnding(volumeData.getOrDefault("ending", ""));
                volumePlan.setVolumeNotes(volumeData.getOrDefault("volumeNotes", ""));
                volumePlan.setStageForeshadowings(volumeData.getOrDefault("stageForeshadowings", "[]"));
                volumePlan.setStatus("planned");
                volumePlan.setSortOrder(volumeNumber);

                String targetChapterCountStr = volumeData.get("targetChapterCount");
                if (targetChapterCountStr != null && !targetChapterCountStr.isEmpty()) {
                    try {
                        volumePlan.setTargetChapterCount(Integer.parseInt(targetChapterCountStr));
                    } catch (NumberFormatException e) {
                        volumePlan.setTargetChapterCount(10);
                    }
                } else {
                    volumePlan.setTargetChapterCount(10);
                }

                volumePlan.setCreateTime(now);
                volumePlan.setUpdateTime(now);
                volumePlanMapper.insert(volumePlan);
                log.info("创建第{}卷: {}", volumeNumber, volumePlan.getVolumeTitle());
            }

        } catch (Exception e) {
            log.error("保存单卷到数据库失败", e);
            throw new RuntimeException("保存分卷失败: " + e.getMessage());
        }
    }

    /**
     * 步骤: 生成指定分卷的章节大纲（已废弃，保留兼容）
     */
    private StepResult generateChaptersInVolume(AiTaskStep step, TaskContext context) {
        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            int volumeNumber = stepConfig.get("volumeNumber").asInt();
            JsonNode taskConfig = stepConfig.get("taskConfig");

            String projectDescription = taskConfig.has("projectDescription") ?
                taskConfig.get("projectDescription").asText() : "";
            String storyTone = taskConfig.has("storyTone") ? taskConfig.get("storyTone").asText() : "";
            String storyGenre = taskConfig.has("storyGenre") ? taskConfig.get("storyGenre").asText() : "";
            int targetChapterCount = taskConfig.has("targetChapterCount") ?
                taskConfig.get("targetChapterCount").asInt() : 10;
            int targetWordCount = taskConfig.has("targetWordCount") ?
                taskConfig.get("targetWordCount").asInt() : 3000;

            // 获取之前生成的分卷信息
            String volumesRaw = (String) context.getSharedData("volumesRaw");

            // 构建提示词
            String prompt = buildChapterPromptUsingTemplate(context, projectDescription, storyTone, storyGenre,
                volumeNumber, volumesRaw, targetChapterCount, targetWordCount);

            // 调用AI
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_outline_chapter_generate");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            // maxTokens将从数据库提供商配置中读取

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成第{}卷章节原始响应:\n{}", volumeNumber, responseContent);

            // 解析响应
            String chaptersXml = extractXml(responseContent);

            // 保存到共享数据
            context.putSharedData("chapters_" + volumeNumber, chaptersXml);
            context.putSharedData("chaptersRaw_" + volumeNumber, responseContent);

            Map<String, Object> result = new HashMap<>();
            result.put("volumeNumber", volumeNumber);
            result.put("chapters", chaptersXml);
            result.put("chaptersRaw", responseContent);

            log.info("生成第{}卷章节大纲完成，任务ID: {}", volumeNumber, context.getTaskId());

            // 立即保存到数据库，让用户可以实时查看
            saveVolumeChaptersToDatabase(context, volumeNumber, chaptersXml);

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("生成章节大纲失败", e);
            return StepResult.failure("生成章节大纲失败: " + e.getMessage());
        }
    }

    /**
     * 步骤: 生成角色信息
     */
    private StepResult generateCharacters(AiTaskStep step, TaskContext context) {
        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            int volumeNumber = stepConfig.get("volumeNumber").asInt();
            JsonNode taskConfig = stepConfig.get("taskConfig");

            String projectDescription = taskConfig.has("projectDescription") ?
                taskConfig.get("projectDescription").asText() : "";
            String storyTone = taskConfig.has("storyTone") ? taskConfig.get("storyTone").asText() : "";
            String storyGenre = taskConfig.has("storyGenre") ? taskConfig.get("storyGenre").asText() : "";

            // 获取之前生成的分卷信息和章节信息
            String volumesRaw = (String) context.getSharedData("volumesRaw");
            String chaptersKey = "chaptersRaw_" + volumeNumber;
            String chaptersRaw = context.getSharedData().containsKey(chaptersKey) ?
                (String) context.getSharedData().get(chaptersKey) : "";

            // 获取已有角色列表
            List<com.aifactory.entity.NovelCharacter> existingCharacters = characterMapper.selectList(
                new LambdaQueryWrapper<NovelCharacter>()
                    .eq(com.aifactory.entity.NovelCharacter::getProjectId, context.getProjectId())
            );

            // 构建已有角色摘要
            String existingCharactersSummary = "";
            if (!existingCharacters.isEmpty()) {
                StringBuilder summary = new StringBuilder("\n\n【已有角色】\n");
                for (com.aifactory.entity.NovelCharacter character : existingCharacters) {
                    summary.append(String.format("- %s（%s，%s）\n",
                        character.getName(),
                        character.getGender(),
                        character.getRoleType()));
                }
                existingCharactersSummary = summary.toString();
            }

            // 构建生成角色的提示词
            String prompt = buildCharacterPromptUsingTemplate(context, projectDescription, storyTone, storyGenre,
                volumeNumber, volumesRaw, chaptersRaw, existingCharactersSummary);

            // 调用AI生成角色
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_outline_character_generate");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            // maxTokens将从数据库提供商配置中读取

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成第{}卷角色原始响应:\n{}", volumeNumber, responseContent);

            // 解析并保存角色
            List<com.aifactory.entity.NovelCharacter> newCharacters = parseAndSaveCharacters(
                context.getProjectId(), responseContent, existingCharacters);

            if (!newCharacters.isEmpty()) {
                // 将新角色保存到上下文
                List<com.aifactory.entity.NovelCharacter> allCharacters = new ArrayList<>(existingCharacters);
                allCharacters.addAll(newCharacters);
                context.putSharedData("characters_volume_" + volumeNumber, newCharacters);
                context.putSharedData("all_characters", allCharacters);

                log.info("生成第{}卷角色完成，新增{}个角色", volumeNumber, newCharacters.size());
                return StepResult.success(Map.of("volumeNumber", volumeNumber, "characterCount", newCharacters.size()), 100);
            } else {
                log.info("第{}卷没有新角色生成", volumeNumber);
                return StepResult.success(Map.of("volumeNumber", volumeNumber, "characterCount", 0), 100);
            }

        } catch (Exception e) {
            log.error("生成角色信息失败", e);
            return StepResult.failure("生成角色信息失败: " + e.getMessage());
        }
    }

    /**
     * 立即保存分卷章节到数据库
     */
    private void saveVolumeChaptersToDatabase(TaskContext context, int volumeNumber, String chaptersXml) {
        try {
            log.info(">>> 第{}卷章节XML:\n{}", volumeNumber, chaptersXml);

            // 使用正则表达式解析XML
            List<Map<String, String>> chaptersList = parseChaptersXml(chaptersXml);

            if (chaptersList == null || chaptersList.isEmpty()) {
                log.warn("第{}卷的章节数据格式错误", volumeNumber);
                return;
            }

            // 保存章节XML到上下文，确保不会丢失
            context.putSharedData("chaptersXml_" + volumeNumber, chaptersXml);

            Long projectId = context.getProjectId();
            LocalDateTime now = LocalDateTime.now();

            // 获取或创建大纲和分卷ID（使用上下文中的outlineId）
            Long outlineId = getOrCreateOutline(context);
            if (outlineId == null) {
                log.error("获取outlineId失败，无法保存第{}卷章节，章节数据已保存到上下文", volumeNumber);
                return;
            }

            Long volumePlanId = getOrCreateVolumePlan(context, outlineId, volumeNumber);
            if (volumePlanId == null) {
                log.error("获取第{}卷规划ID失败，章节数据已保存到上下文", volumeNumber);
                return;
            }

            // 保存每个章节
            for (Map<String, String> chapterData : chaptersList) {
                int chapterNumber = Integer.parseInt(chapterData.getOrDefault("chapterNumber", "0"));

                // 检查章节是否已存在
                com.aifactory.entity.NovelChapterPlan existingChapter = chapterPlanMapper.selectOne(
                    new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(com.aifactory.entity.NovelChapterPlan::getProjectId, projectId)
                        .eq(com.aifactory.entity.NovelChapterPlan::getVolumePlanId, volumePlanId)
                        .eq(com.aifactory.entity.NovelChapterPlan::getChapterNumber, chapterNumber)
                );

                if (existingChapter != null) {
                    // 更新现有章节
                    existingChapter.setChapterTitle(chapterData.getOrDefault("chapterTitle", ""));
                    existingChapter.setPlotOutline(chapterData.getOrDefault("plotOutline", ""));
                    existingChapter.setKeyEvents(chapterData.getOrDefault("keyEvents", ""));
                    existingChapter.setChapterGoal(chapterData.getOrDefault("chapterGoal", ""));
                    existingChapter.setWordCountTarget(Integer.parseInt(chapterData.getOrDefault("wordCountTarget", "3000")));
                    existingChapter.setChapterStartingScene(chapterData.getOrDefault("chapterStartingScene", ""));
                    existingChapter.setChapterEndingScene(chapterData.getOrDefault("chapterEndingScene", ""));
                    existingChapter.setUpdateTime(now);
                    chapterPlanMapper.updateById(existingChapter);
                    log.debug("更新第{}卷第{}章", volumeNumber, chapterNumber);
                } else {
                    // 创建新章节
                    NovelChapterPlan chapterPlan = new NovelChapterPlan();
                    chapterPlan.setProjectId(projectId);
                    chapterPlan.setVolumePlanId(volumePlanId);
                    chapterPlan.setChapterNumber(chapterNumber);
                    chapterPlan.setChapterTitle(chapterData.getOrDefault("chapterTitle", ""));
                    chapterPlan.setPlotOutline(chapterData.getOrDefault("plotOutline", ""));
                    chapterPlan.setKeyEvents(chapterData.getOrDefault("keyEvents", ""));
                    chapterPlan.setChapterGoal(chapterData.getOrDefault("chapterGoal", ""));
                    chapterPlan.setWordCountTarget(Integer.parseInt(chapterData.getOrDefault("wordCountTarget", "3000")));
                    chapterPlan.setChapterStartingScene(chapterData.getOrDefault("chapterStartingScene", ""));
                    chapterPlan.setChapterEndingScene(chapterData.getOrDefault("chapterEndingScene", ""));
                    chapterPlan.setStatus("planned");
                    chapterPlan.setCreateTime(now);
                    chapterPlan.setUpdateTime(now);
                    chapterPlanMapper.insert(chapterPlan);
                    log.debug("创建第{}卷第{}章（包含起终点场景）", volumeNumber, chapterNumber);
                }
            }

            log.info("保存第{}卷章节完成，共{}章", volumeNumber, chaptersList.size());

            // Per D-06: 重新规划时先删除该卷pending伏笔，再根据LLM输出重建
            foreshadowingService.deletePendingForeshadowingForVolume(projectId, volumeNumber);

            // 解析并保存伏笔数据
            int foreshadowingCount = 0;
            log.info("开始解析第{}卷伏笔数据(saveVolumeChaptersToDatabase)，章节数: {}", volumeNumber, chaptersList.size());
            for (Map<String, String> chapterData : chaptersList) {
                String plantCountStr = chapterData.get("_foreshadowingPlants_count");
                String chNum = chapterData.getOrDefault("chapterNumber", "?");
                log.info("章节 {} : _foreshadowingPlants_count = {}", chNum, plantCountStr);
                if (plantCountStr != null) {
                    int plantCount = Integer.parseInt(plantCountStr);
                    String chapterNumberStr = chapterData.getOrDefault("chapterNumber", "0");
                    for (int i = 0; i < plantCount; i++) {
                        try {
                            com.aifactory.dto.ForeshadowingCreateDto fsDto = new com.aifactory.dto.ForeshadowingCreateDto();
                            fsDto.setTitle(chapterData.getOrDefault("_fs_" + i + "_ft", "未命名伏笔"));
                            fsDto.setType(chapterData.getOrDefault("_fs_" + i + "_fy", "event"));
                            fsDto.setDescription(chapterData.getOrDefault("_fs_" + i + "_fd", ""));
                            fsDto.setLayoutType(chapterData.getOrDefault("_fs_" + i + "_fl", "bright1"));
                            fsDto.setPlantedChapter(Integer.parseInt(chapterNumberStr));
                            fsDto.setPlantedVolume(volumeNumber);

                            String fcStr = chapterData.get("_fs_" + i + "_fc");
                            if (fcStr != null && !fcStr.isEmpty()) {
                                fsDto.setPlannedCallbackVolume(parseIntSafe(fcStr, null));
                            }
                            String frStr = chapterData.get("_fs_" + i + "_fr");
                            if (frStr != null && !frStr.isEmpty()) {
                                fsDto.setPlannedCallbackChapter(parseIntSafe(frStr, null));
                            }

                            foreshadowingService.createForeshadowing(projectId, fsDto);
                            foreshadowingCount++;
                        } catch (Exception e2) {
                            log.warn("保存第{}卷第{}章伏笔失败(第{}个): {}", volumeNumber, chapterNumberStr, i, e2.getMessage());
                        }
                    }
                }
            }
            log.info("保存第{}卷伏笔 {} 个", volumeNumber, foreshadowingCount);

        } catch (Exception e) {
            log.error("!!! 保存第{}卷章节到数据库失败", volumeNumber, e);
            log.error("!!! 原始XML数据:\n{}", chaptersXml);
        }
    }

    /**
     * 获取或创建大纲
     * 注意：这个方法会被saveVolumeChaptersToDatabase调用，需要处理继续生成场景
     */
    private Long getOrCreateOutline(TaskContext context) {
        try {
            // 先检查上下文中是否已经有outlineId（优先使用上下文中的）
            if (context.getSharedData().containsKey("outlineId")) {
                Long outlineId = (Long) context.getSharedData().get("outlineId");
                log.info(">>> 使用上下文中的outlineId: {}", outlineId);
                return outlineId;
            } else {
                log.warn("!!! 上下文中没有outlineId，需要查询或创建");
            }

            Long projectId = context.getProjectId();
            JsonNode config = context.getConfig();

            // 检查是否是继续生成场景
            int startVolumeNumber = config.has("startVolumeNumber") ? config.get("startVolumeNumber").asInt() : 1;
            boolean regenerate = config.has("regenerate") && config.get("regenerate").asBoolean();
            boolean isContinueGeneration = (startVolumeNumber > 1 && !regenerate);

            log.info(">>> getOrCreateOutline检查，项目ID: {}, 起始卷号: {}, 是否继续生成: {}",
                projectId, startVolumeNumber, isContinueGeneration);

            // 尝试获取现有大纲（优先使用项目已有的outline，而不是创建新的）
            com.aifactory.entity.NovelOutline existingOutline = outlineMapper.selectOne(
                new LambdaQueryWrapper<NovelOutline>()
                    .eq(com.aifactory.entity.NovelOutline::getProjectId, projectId)
                    .orderByDesc(com.aifactory.entity.NovelOutline::getCreateTime)
                    .last("LIMIT 1")
            );

            if (existingOutline != null) {
                // 将找到的outlineId保存到上下文
                context.putSharedData("outlineId", existingOutline.getId());
                log.info(">>> 找到现有大纲，ID: {}，已保存到上下文（复用已有大纲）", existingOutline.getId());
                return existingOutline.getId();
            }

            // 如果是继续生成模式但没有找到现有大纲，这是异常情况
            if (isContinueGeneration) {
                log.error("!!! 继续生成模式：未找到现有大纲记录，项目ID: {}", projectId);
                return null;
            }

            // 创建新大纲（只有在没有现有大纲且不是继续生成模式时才创建）
            LocalDateTime now = LocalDateTime.now();
            NovelOutline outline = new NovelOutline();
            outline.setProjectId(projectId);
            outline.setOverallConcept(config.has("projectDescription") ?
                config.get("projectDescription").asText() : "");
            outline.setMainTheme(config.has("storyGenre") ?
                config.get("storyGenre").asText() : "未分类");
            outline.setTargetVolumeCount(config.has("targetVolumeCount") ?
                config.get("targetVolumeCount").asInt() : 3);
            outline.setTargetChapterCount(config.has("targetChapterCount") ?
                config.get("targetChapterCount").asInt() : 30);
            outline.setTargetWordCount(config.has("targetWordCount") ?
                config.get("targetWordCount").asInt() : 90000);
            outline.setGenre(config.has("storyGenre") ? config.get("storyGenre").asText() : "");
            outline.setTone(config.has("storyTone") ? config.get("storyTone").asText() : "");
            outline.setStatus("draft");
            outline.setCreateTime(now);
            outline.setUpdateTime(now);

            outlineMapper.insert(outline);
            // 将新创建的outlineId保存到上下文
            context.putSharedData("outlineId", outline.getId());
            log.info(">>> 创建新大纲，ID: {}，已保存到上下文", outline.getId());
            return outline.getId();

        } catch (Exception e) {
            log.error("获取或创建大纲失败", e);
            return null;
        }
    }

    /**
     * 获取或创建分卷规划
     */
    private Long getOrCreateVolumePlan(TaskContext context, Long outlineId, int volumeNumber) {
        try {
            Long projectId = context.getProjectId();

            // 尝试获取现有分卷
            com.aifactory.entity.NovelVolumePlan existingVolume = volumePlanMapper.selectOne(
                new LambdaQueryWrapper<NovelVolumePlan>()
                    .eq(com.aifactory.entity.NovelVolumePlan::getProjectId, projectId)
                    .eq(com.aifactory.entity.NovelVolumePlan::getOutlineId, outlineId)
                    .eq(com.aifactory.entity.NovelVolumePlan::getVolumeNumber, volumeNumber)
            );

            if (existingVolume != null) {
                return existingVolume.getId();
            }

            // 从共享数据中获取分卷信息
            String volumesXml = (String) context.getSharedData("volumes");
            List<Map<String, String>> volumesList = parseVolumesXml(volumesXml);

            Map<String, String> targetVolumeData = null;
            for (Map<String, String> volumeData : volumesList) {
                if (Integer.parseInt(volumeData.getOrDefault("volumeNumber", "0")) == volumeNumber) {
                    targetVolumeData = volumeData;
                    break;
                }
            }

            if (targetVolumeData == null) {
                log.warn("未找到第{}卷的信息", volumeNumber);
                return null;
            }

            // 创建新分卷
            LocalDateTime now = LocalDateTime.now();
            NovelVolumePlan volumePlan = new NovelVolumePlan();
            volumePlan.setProjectId(projectId);
            volumePlan.setOutlineId(outlineId);
            volumePlan.setVolumeNumber(volumeNumber);
            volumePlan.setVolumeTitle(targetVolumeData.getOrDefault("volumeTitle", ""));
            volumePlan.setVolumeTheme(targetVolumeData.getOrDefault("volumeTheme", ""));
            volumePlan.setMainConflict(targetVolumeData.getOrDefault("mainConflict", ""));
            volumePlan.setPlotArc(targetVolumeData.getOrDefault("plotArc", ""));
            volumePlan.setVolumeDescription(targetVolumeData.getOrDefault("volumeDescription", ""));
            volumePlan.setKeyEvents(targetVolumeData.getOrDefault("keyEvents", ""));

            volumePlan.setTimelineSetting(targetVolumeData.getOrDefault("timelineSetting", ""));

            // 解析目标章节数，默认10
            int targetChapterCount = 10;
            try {
                String countStr = targetVolumeData.get("targetChapterCount");
                if (countStr != null && !countStr.isEmpty()) {
                    targetChapterCount = Integer.parseInt(countStr);
                }
            } catch (NumberFormatException e) {
                log.warn("解析目标章节数失败: {}", targetVolumeData.get("targetChapterCount"));
            }
            volumePlan.setTargetChapterCount(targetChapterCount);

            volumePlan.setVolumeNotes(targetVolumeData.getOrDefault("volumeNotes", ""));
            volumePlan.setCoreGoal(targetVolumeData.getOrDefault("coreGoal", ""));
            volumePlan.setClimax(targetVolumeData.getOrDefault("climax", ""));
            volumePlan.setEnding(targetVolumeData.getOrDefault("ending", ""));

            // 保存新人物（JSON字符串）
            String newCharacters = targetVolumeData.getOrDefault("newCharacters", "[]");
            volumePlan.setNewCharacters(newCharacters);

            // 保存伏笔（JSON字符串）
            String foreshadowings = targetVolumeData.getOrDefault("stageForeshadowings", "[]");
            volumePlan.setStageForeshadowings(foreshadowings);

            volumePlan.setStatus("planned");
            volumePlan.setSortOrder(volumeNumber);
            volumePlan.setCreateTime(now);
            volumePlan.setUpdateTime(now);

            volumePlanMapper.insert(volumePlan);
            log.info("创建第{}卷规划，ID: {}", volumeNumber, volumePlan.getId());
            return volumePlan.getId();

        } catch (Exception e) {
            log.error("获取或创建分卷规划失败", e);
            return null;
        }
    }

    /**
     * 步骤: 保存大纲结果到数据库（简化版）
     * 重构后分卷在每个 step 中已即时保存，此步骤仅做最终汇总
     */
    private StepResult saveResult(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            // 获取 outline
            Long outlineId = getOrCreateOutline(context);
            if (outlineId == null) {
                return StepResult.failure("获取或创建大纲失败");
            }

            // 统计生成的分卷数
            Long volumeCount = volumePlanMapper.selectCount(
                new LambdaQueryWrapper<NovelVolumePlan>()
                    .eq(NovelVolumePlan::getProjectId, projectId)
                    .eq(NovelVolumePlan::getOutlineId, outlineId)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("message", "大纲生成任务完成");
            result.put("outlineId", outlineId);
            result.put("volumeCount", volumeCount);

            log.info("大纲生成任务完成，项目ID: {}, 大纲ID: {}, 分卷数: {}",
                projectId, outlineId, volumeCount);
            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("保存大纲结果失败", e);
            return StepResult.failure("保存大纲结果失败: " + e.getMessage());
        }
    }

    /**
     * 使用模板系统构建章节规划提示词
     * 已更新：使用 PromptContextBuilder 替代 addChapterBasicSettingsVariables
     */
    private String buildChapterPromptUsingTemplate(TaskContext context, String projectDescription, String storyTone,
                                                      String storyGenre, int volumeNumber,
                                                      String volumeInfo, int chapterCount, int wordCount) {
        try {
            // 准备模板变量
            Map<String, Object> variables = new HashMap<>();

            variables.put("volumeNumber", volumeNumber);
            variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
            variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");
            variables.put("volumeInfo", volumeInfo != null && !volumeInfo.isEmpty() ? volumeInfo : "");
            variables.put("chapterCount", chapterCount);
            variables.put("wordCount", wordCount);

            // 添加基础设置变量（由 PromptContextBuilder 替代)
            variables.put("narrativeSettings", promptContextBuilder.buildBasicSettingsContext(context.getProjectId()));
            StringBuilder worldviewBuilder = new StringBuilder();
            if (context.getSharedData().containsKey("worldview")) {
                NovelWorldview worldview = (NovelWorldview) context.getSharedData().get("worldview");
                continentRegionService.fillGeography(worldview);
                factionService.fillForces(worldview);
                worldviewBuilder.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
                if (worldview.getWorldBackground() != null && !worldview.getWorldBackground().isEmpty()) {
                    worldviewBuilder.append("- 世界背景：").append(worldview.getWorldBackground()).append("\n");
                }
                if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                    worldviewBuilder.append("- 地理环境：").append(worldview.getGeography()).append("\n");
                }
                String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
                if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                    worldviewBuilder.append("- 力量体系：").append(powerConstraint).append("\n");
                }
                if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                    worldviewBuilder.append("- 势力分布：").append(worldview.getForces()).append("\n");
                }
                if (worldview.getTimeline() != null && !worldview.getTimeline().isEmpty()) {
                    worldviewBuilder.append("- 时间线：").append(worldview.getTimeline()).append("\n");
                }
                if (worldview.getRules() != null && !worldview.getRules().isEmpty()) {
                    worldviewBuilder.append("- 世界规则：").append(worldview.getRules()).append("\n");
                }
            }
            variables.put("worldviewInfo", worldviewBuilder.length() > 0 ? worldviewBuilder.toString() : "暂无世界观设定");

            // 角色信息
            StringBuilder characterBuilder = new StringBuilder();
            if (context.getSharedData().containsKey("all_characters")) {
                @SuppressWarnings("unchecked")
                List<NovelCharacter> allCharacters = (List<NovelCharacter>) context.getSharedData().get("all_characters");
                if (!allCharacters.isEmpty()) {
                    for (NovelCharacter character : allCharacters) {
                        characterBuilder.append(String.format("- %s（%s，%s）：%s\n",
                            character.getName(),
                            character.getGender(),
                            character.getRole(),
                            character.getPersonality() != null && !character.getPersonality().isEmpty() ?
                                character.getPersonality() : "性格待定"));
                    }
                }
            }
            variables.put("characterInfo", characterBuilder.length() > 0 ? characterBuilder.toString() : "暂无登场角色");

            // 伏笔上下文 (per D-01, D-02)
            String foreshadowingContext = buildActiveForeshadowingContext(context.getProjectId(), volumeNumber);
            variables.put("foreshadowingContext", foreshadowingContext);

            // 执行模板
            String templateCode = "llm_outline_chapter_generate";
            return promptTemplateService.executeTemplate(templateCode, variables);

        } catch (Exception e) {
            log.error("使用模板构建章节规划提示词失败", e);
            // 降级到原有方法
            return buildChapterPrompt(context, projectDescription, storyTone, storyGenre,
                volumeNumber, volumeInfo, chapterCount, wordCount);
        }
    }

    /**
     * 构建生成章节的提示词
     */
    private String buildChapterPrompt(TaskContext context, String projectDescription, String storyTone,
                                      String storyGenre, int volumeNumber,
                                      String volumeInfo, int chapterCount, int wordCount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的网络小说作家和编辑。\n\n");
        prompt.append("请为第").append(volumeNumber).append("卷规划详细章节大纲：\n\n");
        prompt.append("【故事背景】\n").append(projectDescription).append("\n\n");
        prompt.append("【故事基调】").append(storyTone).append("\n");
        prompt.append("【故事类型】").append(storyGenre).append("\n");
        prompt.append("【分卷信息】\n").append(volumeInfo).append("\n\n");

        // 添加世界观信息（如果有）
        if (context.getSharedData().containsKey("worldview")) {
            com.aifactory.entity.NovelWorldview worldview =
                (com.aifactory.entity.NovelWorldview) context.getSharedData().get("worldview");
            continentRegionService.fillGeography(worldview);
            factionService.fillForces(worldview);
            prompt.append("【世界观设定】\n");
            prompt.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
            String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
            if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                prompt.append("- 力量体系：").append(powerConstraint).append("\n");
            }
            if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                prompt.append("- 地理环境：").append(worldview.getGeography()).append("\n");
            }
            if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                prompt.append("- 势力分布：").append(worldview.getForces()).append("\n");
            }
            prompt.append("\n");
        }

        // 添加角色信息（如果有）
        if (context.getSharedData().containsKey("all_characters")) {
            @SuppressWarnings("unchecked")
            List<com.aifactory.entity.NovelCharacter> allCharacters =
                (List<com.aifactory.entity.NovelCharacter>) context.getSharedData().get("all_characters");
            if (!allCharacters.isEmpty()) {
                prompt.append("【登场角色】\n");
                for (com.aifactory.entity.NovelCharacter character : allCharacters) {
                    prompt.append(String.format("- %s（%s，%s）：%s\n",
                        character.getName(),
                        character.getGender(),
                        character.getRole(),
                        character.getPersonality() != null && !character.getPersonality().isEmpty() ?
                            character.getPersonality() : "性格待定"));
                }
                prompt.append("\n");
            }
        }

        // 添加伏笔上下文（如果有）
        String foreshadowingContext = buildActiveForeshadowingContext(context.getProjectId(), volumeNumber);
        if (foreshadowingContext != null && !foreshadowingContext.isEmpty()) {
            prompt.append(foreshadowingContext);
        }

        prompt.append("【预计章节数】").append(chapterCount).append("章\n");
        prompt.append("【每章字数】").append(wordCount).append("字左右\n\n");
        
        // 添加专业网文写作指导
        prompt.append("【专业网文结构设计 - 必须遵循】\n\n");
        
        prompt.append("**1. 三明一暗伏笔架构**\n");
        prompt.append("- 明线1（主线）：主角成长、升级、变强\n");
        prompt.append("- 明线2（情感线）：友情、爱情、师徒情发展\n");
        prompt.append("- 明线3（任务线）：寻宝、探秘、解决事件\n");
        prompt.append("- 暗线（核心秘密）：贯穿全卷甚至全书的最大悬念\n\n");
        
        prompt.append("**2. 章节节奏设计（每3章一个小循环）**\n");
        prompt.append("- 第1章（铺垫章）：铺设场景、埋下伏笔、制造期待\n");
        prompt.append("- 第2章（升级章）：冲突升级、遇到阻碍、紧张感增强\n");
        prompt.append("- 第3章（高潮章）：小高潮爆发、问题部分解决、但留下更大悬念\n\n");
        
        prompt.append("**3. 悬念设置要求**\n");
        prompt.append("- 每章结尾必须设置钩子（让读者想读下一章）\n");
        prompt.append("- 每3章一个小高潮，满足读者期待\n");
        prompt.append("- 每章揭示一个新信息，但引出更多问题\n");
        prompt.append("- 长期悬念要逐步透露线索，保持神秘感\n\n");
        
        prompt.append("**4. 伏笔埋设原则**\n");
        prompt.append("- 短期伏笔（1-3章回收）：小道具、小线索、人物小动作\n");
        prompt.append("- 中期伏笔（5-10章回收）：次要角色的秘密、次要物品的来历\n");
        prompt.append("- 长期伏笔（本卷或全书回收）：核心秘密、主角身世、世界真相\n");
        prompt.append("- 回收伏笔时要有情感冲击，让读者有'原来如此'的感觉\n\n");

        prompt.append("**5. 章节起终点设计（重要！确保剧情连贯）**\n");
        prompt.append("- 每章必须有明确的起点场景和终点场景\n");
        prompt.append("- **起点场景**：描述章节开始时主角在哪里、什么时间、什么状态\n");
        prompt.append("- **终点场景**：描述章节结束时主角在哪里、什么时间、什么状态\n");
        prompt.append("- **章节衔接**：第N章的终点场景 = 第N+1章的起点场景\n");
        prompt.append("- **防止时间倒流**：明确标注场景状态，避免剧情重复或倒退\n");
        prompt.append("- 起点场景示例：\"青石镇家中，清晨，准备出发，正在收拾行李\"\n");
        prompt.append("- 终点场景示例：\"天剑宗外门广场，下午，刚完成考核，等待分配住所\"\n\n");

        prompt.append("【重要】请严格按照以下XML格式返回数据：\n");
        prompt.append("1. 必须返回纯XML格式，不要使用markdown代码块（不要使用```xml```）\n");
        prompt.append("2. 不要包含任何解释文字，直接返回XML数据\n");
        prompt.append("3. 字符串内容使用中文标点符号（逗号、句号等）\n");
        prompt.append("4. 使用CDATA包裹内容，避免特殊字符转义问题\n\n");

        prompt.append("返回格式示例（使用简化标签节省token）：\n");
        prompt.append("<c>\n");
        prompt.append("  <o>\n");
        prompt.append("    <n>1</n>\n");
        prompt.append("    <v>").append(volumeNumber).append("</v>\n");
        prompt.append("    <t><![CDATA[章节标题]]></t>\n");
        prompt.append("    <p><![CDATA[本章情节大纲]]></p>\n");
        prompt.append("    <e><![CDATA[关键事件1；关键事件2]]></e>\n");
        prompt.append("    <g><![CDATA[本章目标]]></g>\n");
        prompt.append("    <w>3000</w>\n");
        prompt.append("    <s><![CDATA[章节起点场景（地点、时间、状态）]]></s>\n");
        prompt.append("    <f><![CDATA[章节终点场景（地点、时间、状态）]]></f>\n");
        prompt.append("  </o>\n");
        prompt.append("</c>\n\n");

        prompt.append("现在请返回").append(chapterCount).append("个章节的完整XML数据：\n");

        return prompt.toString();
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJson(String response) {
        String jsonStr = response;

        // 1. 提取代码块中的JSON
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                jsonStr = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                jsonStr = response.substring(start, end).trim();
            }
        }

        // 2. 提取最外层的JSON对象
        int firstBrace = jsonStr.indexOf("{");
        int lastBrace = jsonStr.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
        }

        // 3. 清理JSON字符串（去除注释、修复尾随逗号等）
        jsonStr = cleanJson(jsonStr);

        log.debug(">>> 提取的JSON（前500字符）:\n{}", jsonStr.length() > 500 ? jsonStr.substring(0, 500) + "..." : jsonStr);

        return jsonStr;
    }

    /**
     * 从响应中提取XML内容
     */
    private String extractXml(String response) {
        String xmlStr = response;

        // 1. 提取代码块中的XML
        if (response.contains("```xml")) {
            int start = response.indexOf("```xml") + 5;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        }

        // 2. 智能提取最外层的XML对象（自动检测根标签）
        // 尝试匹配常见的根标签：<V>, <v>, <c>, <w>, <h>等
        String[] commonRootTags = {"<V>", "<v>", "<c>", "<w>", "<h>", "<o>"};
        boolean found = false;

        for (String tag : commonRootTags) {
            String closingTag = "</" + tag.substring(1); // <v> -> </v>
            int firstTag = xmlStr.indexOf(tag);
            int lastTag = xmlStr.lastIndexOf(closingTag);

            if (firstTag >= 0 && lastTag > firstTag) {
                xmlStr = xmlStr.substring(firstTag, lastTag + closingTag.length());
                found = true;
                break;
            }
        }

        // 如果没有找到常见标签，尝试提取任意根节点
        if (!found) {
            int firstTag = xmlStr.indexOf("<");
            if (firstTag >= 0) {
                // 找到第一个标签的名称
                int tagEnd = xmlStr.indexOf(">", firstTag);
                if (tagEnd > firstTag) {
                    String tagContent = xmlStr.substring(firstTag + 1, tagEnd);
                    // 提取标签名（忽略属性和自闭合）
                    String tagName = tagContent.split(" ")[0];
                    if (!tagName.startsWith("/")) {
                        // 查找对应的结束标签
                        String closingTag = "</" + tagName + ">";
                        int lastTag = xmlStr.lastIndexOf(closingTag);
                        if (lastTag > firstTag) {
                            xmlStr = xmlStr.substring(firstTag, lastTag + closingTag.length());
                        } else {
                            // 如果没有结束标签，尝试提取到最后一个>为止
                            lastTag = xmlStr.lastIndexOf(">");
                            if (lastTag > firstTag) {
                                xmlStr = xmlStr.substring(firstTag, lastTag + 1);
                            }
                        }
                    }
                }
            }
        }

        log.debug(">>> 提取的XML（前500字符）:\n{}", xmlStr.length() > 500 ? xmlStr.substring(0, 500) + "..." : xmlStr);

        return xmlStr;
    }

    /**
     * 解析XML格式的章节数据
     */
    private List<Map<String, String>> parseChaptersXml(String xml) {
        List<Map<String, String>> chaptersList = new ArrayList<>();

        try {
            // 简化的XML解析 - 提取所有chapter元素（使用简化标签）
            // c=chapters, o=chapter, n=chapterNumber, v=volumeNumber, t=chapterTitle
            // p=plotOutline, e=keyEvents, g=chapterGoal, w=wordCountTarget
            // s=chapterStartingScene, f=chapterEndingScene
            java.util.regex.Pattern chapterPattern = java.util.regex.Pattern.compile(
                    "<o>\\s*([\\s\\S]*?)\\s*</o>",
                    java.util.regex.Pattern.DOTALL
            );

            java.util.regex.Matcher chapterMatcher = chapterPattern.matcher(xml);

            while (chapterMatcher.find()) {
                String chapterContent = chapterMatcher.group(1);
                Map<String, String> chapterData = new HashMap<>();

                // 提取各个字段的值（简化标签）
                extractXmlField(chapterContent, "n", chapterData);
                extractXmlField(chapterContent, "v", chapterData);
                extractXmlFieldCData(chapterContent, "t", chapterData);
                extractXmlFieldCData(chapterContent, "p", chapterData);
                extractXmlFieldCData(chapterContent, "e", chapterData);
                extractXmlFieldCData(chapterContent, "g", chapterData);
                extractXmlField(chapterContent, "w", chapterData);
                extractXmlFieldCData(chapterContent, "s", chapterData);
                extractXmlFieldCData(chapterContent, "f", chapterData);

                // 提取伏笔埋设和回收标签 (per D-03, D-04)
                List<Map<String, String>> foreshadowingPlants = extractForeshadowingPlants(chapterContent);
                log.info("章节 {} 解析到 {} 个伏笔埋设标签, chapterContent长度: {}",
                    chapterData.get("n"), foreshadowingPlants.size(), chapterContent.length());
                if (!foreshadowingPlants.isEmpty()) {
                    chapterData.put("_foreshadowingPlants_count", String.valueOf(foreshadowingPlants.size()));
                    for (int i = 0; i < foreshadowingPlants.size(); i++) {
                        Map<String, String> plant = foreshadowingPlants.get(i);
                        for (Map.Entry<String, String> entry : plant.entrySet()) {
                            chapterData.put("_fs_" + i + "_" + entry.getKey(), entry.getValue());
                        }
                    }
                }

                List<Map<String, String>> foreshadowingPayoffs = extractForeshadowingPayoffs(chapterContent);
                if (!foreshadowingPayoffs.isEmpty()) {
                    chapterData.put("_foreshadowingPayoffs_count", String.valueOf(foreshadowingPayoffs.size()));
                    for (int i = 0; i < foreshadowingPayoffs.size(); i++) {
                        Map<String, String> payoff = foreshadowingPayoffs.get(i);
                        for (Map.Entry<String, String> entry : payoff.entrySet()) {
                            chapterData.put("_fp_" + i + "_" + entry.getKey(), entry.getValue());
                        }
                    }
                }

                // 将简化标签映射回标准字段名
                if (chapterData.containsKey("n")) {
                    chapterData.put("chapterNumber", chapterData.get("n"));
                }
                if (chapterData.containsKey("v")) {
                    chapterData.put("volumeNumber", chapterData.get("v"));
                }
                if (chapterData.containsKey("t")) {
                    chapterData.put("chapterTitle", chapterData.get("t"));
                }
                if (chapterData.containsKey("p")) {
                    chapterData.put("plotOutline", chapterData.get("p"));
                }
                if (chapterData.containsKey("e")) {
                    chapterData.put("keyEvents", chapterData.get("e"));
                }
                if (chapterData.containsKey("g")) {
                    chapterData.put("chapterGoal", chapterData.get("g"));
                }
                if (chapterData.containsKey("w")) {
                    chapterData.put("wordCountTarget", chapterData.get("w"));
                }
                if (chapterData.containsKey("s")) {
                    chapterData.put("chapterStartingScene", chapterData.get("s"));
                }
                if (chapterData.containsKey("f")) {
                    chapterData.put("chapterEndingScene", chapterData.get("f"));
                }

                if (!chapterData.isEmpty()) {
                    chaptersList.add(chapterData);
                }
            }

        } catch (Exception e) {
            log.error("解析章节数据XML失败", e);
        }

        return chaptersList;
    }

    /**
     * 提取XML字段值（非CDATA）
     */
    private void extractXmlField(String content, String fieldName, Map<String, String> result) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<" + fieldName + "\\s*>([^<]*)</" + fieldName + ">"
            );
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                result.put(fieldName, matcher.group(1).trim());
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    /**
     * 提取XML字段值（CDATA）
     */
    private void extractXmlFieldCData(String content, String fieldName, Map<String, String> result) {
        try {
            // 尝试匹配CDATA格式
            java.util.regex.Pattern cdataPattern = java.util.regex.Pattern.compile(
                    "<" + fieldName + "\\s*>\\s*<!\\[CDATA\\[([^\\]]*?)\\]\\]>\\s*</" + fieldName + ">",
                    java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher cdataMatcher = cdataPattern.matcher(content);
            if (cdataMatcher.find()) {
                result.put(fieldName, cdataMatcher.group(1).trim());
            } else {
                // 如果没有CDATA，尝试普通格式
                extractXmlField(content, fieldName, result);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    /**
     * 从章节内容中提取 <fs> 伏笔埋设标签
     * Per D-03: <fs> 子标签: <ft>标题, <fy>类型, <fl>布局线, <fd>描述, <fc>回收分卷, <fr>回收章节
     * Per D-04: 每个章节可包含零个或多个 <fs> 标签
     */
    private List<Map<String, String>> extractForeshadowingPlants(String chapterContent) {
        List<Map<String, String>> plants = new ArrayList<>();
        Pattern fsPattern = Pattern.compile("<fs>\\s*([\\s\\S]*?)\\s*</fs>", Pattern.DOTALL);
        Matcher fsMatcher = fsPattern.matcher(chapterContent);
        while (fsMatcher.find()) {
            String fsContent = fsMatcher.group(1);
            Map<String, String> data = new HashMap<>();
            extractXmlFieldCData(fsContent, "ft", data);  // title
            extractXmlField(fsContent, "fy", data);        // type (character/item/event/secret)
            extractXmlField(fsContent, "fl", data);         // layout line (bright1/bright2/bright3/dark)
            extractXmlFieldCData(fsContent, "fd", data);   // description
            extractXmlField(fsContent, "fc", data);         // callback volume
            extractXmlField(fsContent, "fr", data);         // callback chapter
            if (data.containsKey("ft")) {
                plants.add(data);
            }
        }
        return plants;
    }

    /**
     * 从章节内容中提取 <fp> 伏笔回收标签
     * Per D-03: <fp> 子标签: <ft>标题, <fd>回收方式描述
     * Per D-05: <fp> 仅作为信息参考，不自动更新已有伏笔状态
     */
    private List<Map<String, String>> extractForeshadowingPayoffs(String chapterContent) {
        List<Map<String, String>> payoffs = new ArrayList<>();
        Pattern fpPattern = Pattern.compile("<fp>\\s*([\\s\\S]*?)\\s*</fp>", Pattern.DOTALL);
        Matcher fpMatcher = fpPattern.matcher(chapterContent);
        while (fpMatcher.find()) {
            String fpContent = fpMatcher.group(1);
            Map<String, String> data = new HashMap<>();
            extractXmlFieldCData(fpContent, "ft", data);  // title (for reference)
            extractXmlFieldCData(fpContent, "fd", data);   // payoff description
            if (data.containsKey("ft")) {
                payoffs.add(data);
            }
        }
        return payoffs;
    }

    /**
     * 构建当前分卷的活跃伏笔上下文（用于章节规划提示词注入）
     * Per D-01: 结构化列表格式，每个伏笔显示标题、类型、布局线、状态、埋设/回收位置
     * Per D-02: 仅注入当前分卷活跃伏笔（status=pending 或 in_progress）
     */
    private String buildActiveForeshadowingContext(Long projectId, int volumeNumber) {
        StringBuilder sb = new StringBuilder();

        // 查询当前分卷待埋设的伏笔（status=pending）
        ForeshadowingQueryDto plantQuery = new ForeshadowingQueryDto();
        plantQuery.setProjectId(projectId);
        plantQuery.setPlantedVolume(volumeNumber);
        plantQuery.setStatus("pending");
        List<ForeshadowingDto> pendingPlants = foreshadowingService.getForeshadowingList(plantQuery);

        // 查询当前分卷待回收的伏笔（status=in_progress）
        ForeshadowingQueryDto callbackQuery = new ForeshadowingQueryDto();
        callbackQuery.setProjectId(projectId);
        callbackQuery.setPlannedCallbackVolume(volumeNumber);
        callbackQuery.setStatus("in_progress");
        List<ForeshadowingDto> pendingCallbacks = foreshadowingService.getForeshadowingList(callbackQuery);

        if (pendingPlants.isEmpty() && pendingCallbacks.isEmpty()) {
            return "";  // 当前分卷无活跃伏笔
        }

        sb.append("\n【当前卷活跃伏笔】\n");

        if (!pendingPlants.isEmpty()) {
            sb.append("\n待埋设伏笔（需在本卷各章节中埋设）：\n");
            for (int i = 0; i < pendingPlants.size(); i++) {
                ForeshadowingDto fs = pendingPlants.get(i);
                sb.append(i + 1).append(". ").append(fs.getTitle());
                sb.append(" | 类型: ").append(fs.getType() != null ? fs.getType() : "未设定");
                sb.append(" | 布局线: ").append(fs.getLayoutType() != null ? fs.getLayoutType() : "未设定");
                sb.append(" | 状态: ").append(fs.getStatus());
                if (fs.getPlannedCallbackVolume() != null && fs.getPlannedCallbackChapter() != null) {
                    sb.append(" | 计划回收: 第").append(fs.getPlannedCallbackVolume()).append("卷第").append(fs.getPlannedCallbackChapter()).append("章");
                }
                sb.append("\n");
            }
        }

        if (!pendingCallbacks.isEmpty()) {
            sb.append("\n待回收伏笔（前文已埋设，需在本卷回收）：\n");
            for (int i = 0; i < pendingCallbacks.size(); i++) {
                ForeshadowingDto fs = pendingCallbacks.get(i);
                sb.append(i + 1).append(". ").append(fs.getTitle());
                sb.append(" | 类型: ").append(fs.getType() != null ? fs.getType() : "未设定");
                sb.append(" | 布局线: ").append(fs.getLayoutType() != null ? fs.getLayoutType() : "未设定");
                sb.append(" | 状态: ").append(fs.getStatus());
                if (fs.getPlantedVolume() != null && fs.getPlantedChapter() != null) {
                    sb.append(" | 埋设: 第").append(fs.getPlantedVolume()).append("卷第").append(fs.getPlantedChapter()).append("章");
                }
                sb.append("\n");
            }
        }

        sb.append("\n注：暗线(dark)伏笔为全书级悬念，可跨卷埋设线索，不急于回收。\n");

        return sb.toString();
    }

    /**
     * 解析角色XML列表
     */
    private List<Map<String, String>> parseCharactersXml(String xml) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            // 提取 <c> 标签内容（characters简化）
            Pattern charactersPattern = Pattern.compile("<c>\\s*([\\s\\S]*?)\\s*</c>", Pattern.DOTALL);
            Matcher charactersMatcher = charactersPattern.matcher(xml);

            if (!charactersMatcher.find()) {
                log.warn("未找到 <c> 标签");
                return result;
            }

            String charactersContent = charactersMatcher.group(1);

            // 提取每个 <o> 标签（character简化）
            Pattern characterPattern = Pattern.compile("<o>\\s*([\\s\\S]*?)\\s*</o>", Pattern.DOTALL);
            Matcher characterMatcher = characterPattern.matcher(charactersContent);

            while (characterMatcher.find()) {
                String characterContent = characterMatcher.group(1);
                Map<String, String> characterData = new HashMap<>();

                // 提取各个字段（简化标签）
                // n=name, g=gender, a=age, t=roleType, d=role, p=personality
                // e=appearance, q=appearancePrompt, b=background, l=abilities, s=tags
                extractXmlFieldCData(characterContent, "n", characterData);
                extractXmlFieldCData(characterContent, "g", characterData);
                extractXmlFieldCData(characterContent, "a", characterData);
                extractXmlFieldCData(characterContent, "t", characterData);
                extractXmlFieldCData(characterContent, "d", characterData);
                extractXmlFieldCData(characterContent, "p", characterData);
                extractXmlFieldCData(characterContent, "e", characterData);
                extractXmlFieldCData(characterContent, "q", characterData);
                extractXmlFieldCData(characterContent, "b", characterData);
                extractXmlFieldCData(characterContent, "l", characterData);
                extractXmlFieldCData(characterContent, "s", characterData);

                // 将简化标签映射回标准字段名
                if (characterData.containsKey("n")) {
                    characterData.put("name", characterData.get("n"));
                }
                if (characterData.containsKey("g")) {
                    characterData.put("gender", characterData.get("g"));
                }
                if (characterData.containsKey("a")) {
                    characterData.put("age", characterData.get("a"));
                }
                if (characterData.containsKey("t")) {
                    characterData.put("roleType", characterData.get("t"));
                }
                if (characterData.containsKey("d")) {
                    characterData.put("role", characterData.get("d"));
                }
                if (characterData.containsKey("p")) {
                    characterData.put("personality", characterData.get("p"));
                }
                if (characterData.containsKey("e")) {
                    characterData.put("appearance", characterData.get("e"));
                }
                if (characterData.containsKey("q")) {
                    characterData.put("appearancePrompt", characterData.get("q"));
                }
                if (characterData.containsKey("b")) {
                    characterData.put("background", characterData.get("b"));
                }
                if (characterData.containsKey("l")) {
                    characterData.put("abilities", characterData.get("l"));
                }
                if (characterData.containsKey("s")) {
                    characterData.put("tags", characterData.get("s"));
                }

                if (characterData.containsKey("name") && !characterData.get("name").isEmpty()) {
                    result.add(characterData);
                }
            }

            log.info("解析角色XML成功，提取到 {} 个角色", result.size());
        } catch (Exception e) {
            log.error("解析角色XML失败", e);
        }
        return result;
    }

    /**
     * 解析世界观XML
     */
    private Map<String, String> parseWorldviewXml(String xml) {
        Map<String, String> result = new HashMap<>();
        try {
            // 提取 <w> 标签内容（worldview简化）
            Pattern worldviewPattern = Pattern.compile("<w>\\s*([\\s\\S]*?)\\s*</w>", Pattern.DOTALL);
            Matcher worldviewMatcher = worldviewPattern.matcher(xml);

            if (!worldviewMatcher.find()) {
                log.warn("未找到 <w> 标签");
                return result;
            }

            String worldviewContent = worldviewMatcher.group(1);

            // 提取各个字段（简化标签）
            // t=worldType, b=worldBackground, p=powerSystemXml, g=geography, f=forces, l=timeline, r=rules
            // p (powerSystem) is parsed separately by WorldviewTaskStrategy into novel_power_system tables
            extractXmlFieldCData(worldviewContent, "t", result);
            extractXmlFieldCData(worldviewContent, "b", result);
            extractXmlFieldCData(worldviewContent, "p", result);
            extractXmlFieldCData(worldviewContent, "g", result);
            extractXmlFieldCData(worldviewContent, "f", result);
            extractXmlFieldCData(worldviewContent, "l", result);
            extractXmlFieldCData(worldviewContent, "r", result);

            // 将简化标签映射回标准字段名
            if (result.containsKey("t")) {
                result.put("worldType", result.get("t"));
            }
            if (result.containsKey("b")) {
                result.put("worldBackground", result.get("b"));
            }
            // p (powerSystem) kept as-is for reference; not written to novel_worldview
            if (result.containsKey("g")) {
                result.put("geography", result.get("g"));
            }
            if (result.containsKey("f")) {
                result.put("forces", result.get("f"));
            }
            if (result.containsKey("l")) {
                result.put("timeline", result.get("l"));
            }
            if (result.containsKey("r")) {
                result.put("rules", result.get("r"));
            }

            log.info("解析世界观XML成功，提取到 {} 个字段", result.size());
        } catch (Exception e) {
            log.error("解析世界观XML失败", e);
        }
        return result;
    }

    /**
     * 解析分卷XML
     * 标签映射与数据库模板 llm_outline_volume_generate 保持一致：
     * V: Volumes（分卷列表）- 外层标签
     * v: volume（单个分卷）- 内层标签
     * N: Number（卷号）
     * T: Title（标题）
     * Z: 主题（Theme）
     * F: 矛盾（Conflict）
     * P: 情节（Plot）
     * D: 描述（Description）
     * E: 事件（Events）
     * W: 字数（Word Count）
     * L: 时间线（Timeline）
     * G: 目标章节数（Goal/Count）
     * B: 备注（Notes）
     * O: 核心目标（Objective）
     * H: 高潮（Climax）
     * R: 收尾（Resolution）
     * S: 起始章节（Start）
     * X: 结束章节（End）
     * A: 人物（Actors/Characters）- JSON数组
     * Y: 伏笔（Yield/Foreshadowing）- JSON数组
     */
    private List<Map<String, String>> parseVolumesXml(String xml) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            // 提取 <V> 标签内容（分卷列表）
            Pattern volumesPattern = Pattern.compile("<V>\\s*([\\s\\S]*?)\\s*</V>", Pattern.DOTALL);
            Matcher volumesMatcher = volumesPattern.matcher(xml);

            if (!volumesMatcher.find()) {
                log.warn("未找到 <V> 标签");
                return result;
            }

            String volumesContent = volumesMatcher.group(1);

            // 提取每个 <v> 标签（单个分卷）
            Pattern volumePattern = Pattern.compile("<v>\\s*([\\s\\S]*?)\\s*</v>", Pattern.DOTALL);
            Matcher volumeMatcher = volumePattern.matcher(volumesContent);

            while (volumeMatcher.find()) {
                String volumeContent = volumeMatcher.group(1);
                Map<String, String> volumeData = new HashMap<>();

                // 解析字段（保留: N, T, Z, F, P, D, E, L, G, B, O, H, R, Y）
                extractXmlField(volumeContent, "N", volumeData);
                extractXmlFieldCData(volumeContent, "T", volumeData);
                extractXmlFieldCData(volumeContent, "Z", volumeData);
                extractXmlFieldCData(volumeContent, "F", volumeData);
                extractXmlFieldCData(volumeContent, "P", volumeData);
                extractXmlFieldCData(volumeContent, "D", volumeData);
                // E: 尝试5阶段XML子节点格式，回退到CDATA/纯文本
                String keyEventsJson = parseKeyEventsFromXml(volumeContent);
                if (keyEventsJson != null) {
                    volumeData.put("E", keyEventsJson);
                } else {
                    extractXmlFieldCData(volumeContent, "E", volumeData);
                }
                extractXmlFieldCData(volumeContent, "L", volumeData);
                extractXmlField(volumeContent, "G", volumeData);
                extractXmlFieldCData(volumeContent, "B", volumeData);
                extractXmlFieldCData(volumeContent, "O", volumeData);
                extractXmlFieldCData(volumeContent, "H", volumeData);
                extractXmlFieldCData(volumeContent, "R", volumeData);
                extractXmlFieldCData(volumeContent, "Y", volumeData);  // 伏笔JSON

                // 使用 mapField 映射到标准字段名
                mapField(volumeData, "N", "volumeNumber");
                mapField(volumeData, "T", "volumeTitle");
                mapField(volumeData, "Z", "volumeTheme");
                mapField(volumeData, "F", "mainConflict");
                mapField(volumeData, "P", "plotArc");
                mapField(volumeData, "D", "volumeDescription");
                mapField(volumeData, "E", "keyEvents");
                mapField(volumeData, "L", "timelineSetting");
                mapField(volumeData, "G", "targetChapterCount");
                mapField(volumeData, "B", "volumeNotes");
                mapField(volumeData, "O", "coreGoal");
                mapField(volumeData, "H", "climax");
                mapField(volumeData, "R", "ending");
                mapField(volumeData, "Y", "stageForeshadowings");

                if (volumeData.containsKey("volumeNumber") && !volumeData.get("volumeNumber").isEmpty()) {
                    result.add(volumeData);
                }
            }

            log.info("解析分卷XML成功，提取到 {} 个分卷", result.size());
        } catch (Exception e) {
            log.error("解析分卷XML失败", e);
        }
        return result;
    }

    /**
     * 映射XML标签到标准字段名
     */
    private void mapField(Map<String, String> data, String xmlTag, String fieldName) {
        if (data.containsKey(xmlTag)) {
            data.put(fieldName, data.get(xmlTag));
        }
    }

    /**
     * 从XML子节点解析keyEvents（5阶段格式）
     * 格式：<E><opening><item>事件</item></opening>...</E>
     * 转换为JSON：{"opening":["事件1","事件2"],...}
     */
    private String parseKeyEventsFromXml(String volumeContent) {
        try {
            // 提取<E>标签内容
            int eStart = volumeContent.indexOf("<E>");
            int eEnd = volumeContent.indexOf("</E>");
            if (eStart == -1 || eEnd == -1) {
                return null;
            }
            String eContent = volumeContent.substring(eStart + 3, eEnd);

            // 检查是否包含5阶段子节点格式
            if (!eContent.contains("<opening>") && !eContent.contains("<development>")) {
                return null;  // 不是5阶段格式，回退到普通解析
            }

            // 使用Jackson构建JSON
            com.fasterxml.jackson.databind.node.ObjectNode resultNode = objectMapper.createObjectNode();

            // 定义5个阶段
            String[] stages = {"opening", "development", "turning", "climax", "ending"};

            for (String stage : stages) {
                String stageStartTag = "<" + stage + ">";
                String stageEndTag = "</" + stage + ">";

                int stageStart = eContent.indexOf(stageStartTag);
                int stageEnd = eContent.indexOf(stageEndTag);

                if (stageStart == -1 || stageEnd == -1) {
                    log.warn("keyEvents缺少阶段: {}", stage);
                    continue;
                }

                String stageContent = eContent.substring(stageStart + stageStartTag.length(), stageEnd);

                // 提取所有<item>内容
                List<String> items = new ArrayList<>();
                int itemStart = 0;
                while ((itemStart = stageContent.indexOf("<item>", itemStart)) != -1) {
                    int itemEnd = stageContent.indexOf("</item>", itemStart);
                    if (itemEnd == -1) break;

                    String itemContent = stageContent.substring(itemStart + 6, itemEnd);

                    // 处理CDATA
                    String cdataStart = "<![CDATA[";
                    String cdataEnd = "]]>";
                    int cStart = itemContent.indexOf(cdataStart);
                    if (cStart != -1) {
                        int cEnd = itemContent.indexOf(cdataEnd, cStart + cdataStart.length());
                        if (cEnd != -1) {
                            itemContent = itemContent.substring(cStart + cdataStart.length(), cEnd);
                        }
                    }

                    items.add(itemContent.trim());
                    itemStart = itemEnd + 7;
                }

                // 添加到JSON数组
                com.fasterxml.jackson.databind.node.ArrayNode arrayNode = resultNode.putArray(stage);
                for (String item : items) {
                    arrayNode.add(item);
                }
            }

            // 返回JSON字符串
            return objectMapper.writeValueAsString(resultNode);

        } catch (Exception e) {
            log.error("解析keyEvents XML失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建生成世界观的提示词
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String storyGenre) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的网络小说作家和世界观构建师。\n\n");
        prompt.append("请根据以下故事信息，构建一个完整的世界观设定：\n\n");

        prompt.append("【故事背景】\n").append(projectDescription).append("\n\n");
        prompt.append("【故事基调】").append(storyTone).append("\n");
        prompt.append("【故事类型】").append(storyGenre).append("\n\n");

        prompt.append("【重要】请严格按照以下XML格式返回世界观设定（使用简化标签节省token）：\n");
        prompt.append("<w>\n");
        prompt.append("  <t>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</t>\n");
        prompt.append("  <b><![CDATA[世界背景描述（200-300字）]]></b>\n");
        prompt.append("  <p><![CDATA[力量体系或修炼体系（如有），包括等级划分、能力来源等]]></p>\n");
        prompt.append("  <g><![CDATA[地理环境描述，包括重要地点、国家、区域等]]></g>\n");
        prompt.append("  <f><![CDATA[势力分布，包括主要组织、国家、门派等]]></f>\n");
        prompt.append("  <l><![CDATA[时间线设定（如适用）]]></l>\n");
        prompt.append("  <r><![CDATA[世界的基本规则和限制]]></r>\n");
        prompt.append("</w>\n\n");

        prompt.append("【XML格式要求】\n");
        prompt.append("1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n");
        prompt.append("2. 不要包含markdown代码块标记（```xml），直接返回XML\n");
        prompt.append("3. 不要包含任何解释或说明文字，只返回XML数据\n\n");

        prompt.append("内容要求：\n");
        prompt.append("1. 世界观要符合故事类型和基调\n");
        prompt.append("2. 力量体系要清晰、合理，有可发展性\n");
        prompt.append("3. 各个要素之间要相互关联，形成完整的世界\n");
        prompt.append("4. 返回的必须是纯XML格式，不要有任何其他说明文字\n");

        return prompt.toString();
    }

    /**
     * 解析并保存世界观设定
     */
    private com.aifactory.entity.NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {
            // 提取XML
            String xmlStr = extractXml(aiResponse);
            Map<String, String> worldviewData = parseWorldviewXml(xmlStr);

            if (worldviewData.isEmpty()) {
                log.warn("解析世界观XML失败，数据为空");
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            NovelWorldview worldview = new NovelWorldview();
            worldview.setProjectId(projectId);
            worldview.setWorldType(worldviewData.containsKey("worldType") ? worldviewData.get("worldType") : storyGenre);
            worldview.setWorldBackground(worldviewData.getOrDefault("worldBackground", ""));
            // powerSystem 已迁移到 novel_power_system 表，不再写入 worldview
            // geography 已迁移到 novel_continent_region 表，不再写入 worldview
            worldview.setForces(worldviewData.getOrDefault("forces", ""));
            worldview.setTimeline(worldviewData.getOrDefault("timeline", ""));
            worldview.setRules(worldviewData.getOrDefault("rules", ""));
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
     * 使用模板系统构建角色生成提示词
     */
    private String buildCharacterPromptUsingTemplate(TaskContext context, String projectDescription, String storyTone,
                                                       String storyGenre, int volumeNumber,
                                                       String volumeInfo, String chapterInfo,
                                                       String existingCharactersSummary) {
        try {
            // 准备模板变量
            Map<String, Object> variables = new HashMap<>();

            variables.put("volumeNumber", volumeNumber);
            variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
            variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");
            variables.put("volumeInfo", volumeInfo != null && !volumeInfo.isEmpty() ? volumeInfo : "");
            variables.put("chapterInfo", chapterInfo != null && !chapterInfo.isEmpty() ? chapterInfo : "");
            variables.put("existingCharactersSummary", existingCharactersSummary != null && !existingCharactersSummary.isEmpty() ? existingCharactersSummary : "");

            // 世界观信息
            StringBuilder worldviewBuilder = new StringBuilder();
            if (context.getSharedData().containsKey("worldview")) {
                NovelWorldview worldview = (NovelWorldview) context.getSharedData().get("worldview");
                continentRegionService.fillGeography(worldview);
                factionService.fillForces(worldview);
                worldviewBuilder.append("【世界观设定】\n");
                worldviewBuilder.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
                String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
                if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                    worldviewBuilder.append("- 力量体系：").append(powerConstraint).append("\n");
                }
                if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                    worldviewBuilder.append("- 地理环境：").append(worldview.getGeography()).append("\n");
                }
                if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                    worldviewBuilder.append("- 势力分布：").append(worldview.getForces()).append("\n");
                }
                worldviewBuilder.append("\n");
            }
            variables.put("worldviewInfo", worldviewBuilder.length() > 0 ? worldviewBuilder.toString() : "");

            // 执行模板
            String templateCode = "llm_outline_character_generate";
            return promptTemplateService.executeTemplate(templateCode, variables);

        } catch (Exception e) {
            log.error("使用模板构建角色生成提示词失败", e);
            // 降级到原有方法
            return buildCharacterPrompt(context, projectDescription, storyTone, storyGenre,
                volumeNumber, volumeInfo, chapterInfo, existingCharactersSummary);
        }
    }

    /**
     * 构建生成角色的提示词
     */
    private String buildCharacterPrompt(TaskContext context, String projectDescription, String storyTone,
                                         String storyGenre, int volumeNumber,
                                         String volumeInfo, String chapterInfo,
                                         String existingCharactersSummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的网络小说作家和角色设计师。\n\n");
        prompt.append("请为第").append(volumeNumber).append("卷设计角色信息：\n\n");
        prompt.append("【故事背景】\n").append(projectDescription).append("\n\n");
        prompt.append("【故事基调】").append(storyTone).append("\n");
        prompt.append("【故事类型】").append(storyGenre).append("\n\n");

        // 添加世界观信息（如果有）
        if (context.getSharedData().containsKey("worldview")) {
            com.aifactory.entity.NovelWorldview worldview =
                (com.aifactory.entity.NovelWorldview) context.getSharedData().get("worldview");
            continentRegionService.fillGeography(worldview);
            factionService.fillForces(worldview);
            prompt.append("【世界观设定】\n");
            prompt.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
            String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
            if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                prompt.append("- 力量体系：").append(powerConstraint).append("\n");
            }
            if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                prompt.append("- 地理环境：").append(worldview.getGeography()).append("\n");
            }
            if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                prompt.append("- 势力分布：").append(worldview.getForces()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("【分卷信息】\n").append(volumeInfo).append("\n\n");
        if (chapterInfo != null && !chapterInfo.isEmpty()) {
            prompt.append("【章节概要】\n").append(chapterInfo).append("\n\n");
        }

        prompt.append(existingCharactersSummary);

        prompt.append("【重要提示】\n");
        prompt.append("1. 只生成本卷新出现的角色\n");
        prompt.append("2. 已有角色不需要重新生成\n");
        prompt.append("3. 主角和重要配角在第一卷生成，后续卷只生成新角色\n");
        prompt.append("4. 角色数量控制在3-8个（包括主角、配角、反派等）\n\n");

        prompt.append("【重要】请严格按照以下XML格式返回角色数据（使用简化标签节省token）：\n");
        prompt.append("<c>\n");
        prompt.append("  <o>\n");
        prompt.append("    <n>角色名称</n>\n");
        prompt.append("    <g>male/female/other</g>\n");
        prompt.append("    <a>年龄数字</a>\n");
        prompt.append("    <t>protagonist/supporting/antagonist/npc</t>\n");
        prompt.append("    <d><![CDATA[角色定位（如：主角、男二号、反派BOSS等）]]></d>\n");
        prompt.append("    <p><![CDATA[性格特点（如：勇敢、智慧、冷酷等）]]></p>\n");
        prompt.append("    <e><![CDATA[外貌描述（中文自然语言）]]></e>\n");
        prompt.append("    <q><![CDATA[AI图像生成描述词（专业3D动漫风格，英文，逗号分隔）]]></q>\n");
        prompt.append("    <b><![CDATA[背景故事]]></b>\n");
        prompt.append("    <l><![CDATA[能力或技能描述]]></l>\n");
        prompt.append("    <s><![CDATA[标签，用逗号分隔（如：热血,智慧,正义）]]></s>\n");
        prompt.append("  </o>\n");
        prompt.append("</c>\n\n");

        prompt.append("【XML格式要求】\n");
        prompt.append("1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n");
        prompt.append("2. 不要包含markdown代码块标记（```xml），直接返回XML\n");
        prompt.append("3. 不要包含任何解释或说明文字，只返回XML数据\n\n");

        prompt.append("要求：\n");
        prompt.append("1. 角色要符合世界观和故事背景\n");
        prompt.append("2. 性格要鲜明，有成长空间\n");
        prompt.append("3. 背景故事要与主线剧情相关\n");
        prompt.append("4. appearancePrompt 必须是专业的3D动漫风格描述词，用于AI文生图\n");
        prompt.append("5. appearancePrompt 格式要求：\n");
        prompt.append("   - 必须使用英文关键词，逗号分隔\n");
        prompt.append("   - 风格限定：3D anime style, cel shading, Unity engine render\n");
        prompt.append("   - 包含要素：面部特征、发型、发色、眼睛、服装、配饰、姿态、表情\n");
        prompt.append("   - 示例格式：\"3D anime style, cel shading, male protagonist, spiky black hair, determined red eyes, wearing modern black jacket, red scarf, confident smile, dynamic pose, high quality render, Unity engine, 8k resolution\"\n");
        prompt.append("6. 返回的必须是纯XML格式，不要有任何其他说明文字\n");

        return prompt.toString();
    }

    /**
     * 解析并保存角色设定
     */
    private List<com.aifactory.entity.NovelCharacter> parseAndSaveCharacters(
            Long projectId, String aiResponse,
            List<com.aifactory.entity.NovelCharacter> existingCharacters) {
        try {
            // 提取XML
            String xmlStr = extractXml(aiResponse);
            List<Map<String, String>> charactersData = parseCharactersXml(xmlStr);

            if (charactersData.isEmpty()) {
                log.warn("解析角色XML失败，数据为空");
                return new ArrayList<>();
            }

            List<com.aifactory.entity.NovelCharacter> newCharacters = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // 构建已有角色名称集合，用于去重
            Set<String> existingNames = new HashSet<>();
            for (com.aifactory.entity.NovelCharacter character : existingCharacters) {
                existingNames.add(character.getName());
            }

            // 解析并保存新角色
            for (Map<String, String> characterData : charactersData) {
                String name = characterData.getOrDefault("name", "");
                if (name.isEmpty()) {
                    continue;
                }

                // 检查角色是否已存在
                if (existingNames.contains(name)) {
                    log.info("角色 {} 已存在，跳过创建", name);
                    continue;
                }

                NovelCharacter character = new NovelCharacter();
                character.setProjectId(projectId);
                character.setName(name);
                character.setGender(characterData.getOrDefault("gender", "other"));

                // 解析年龄
                String age = "20";
                try {
                    String ageStr = characterData.get("age");
                    if (ageStr != null && !ageStr.isEmpty()) {
                        age = ageStr;
                    }
                } catch (Exception e) {
                    log.warn("解析角色年龄失败: {}", characterData.get("age"));
                }
                character.setAge(age);

                character.setRoleType(characterData.getOrDefault("roleType", "supporting"));
                character.setRole(characterData.getOrDefault("role", ""));
                character.setPersonality(characterData.getOrDefault("personality", ""));
                character.setAppearance(characterData.getOrDefault("appearance", ""));
                character.setAppearancePrompt(characterData.getOrDefault("appearancePrompt", ""));
                character.setBackground(characterData.getOrDefault("background", ""));
                character.setAbilities(characterData.getOrDefault("abilities", ""));
                character.setTags(characterData.getOrDefault("tags", ""));
                character.setCreateTime(now);
                character.setUpdateTime(now);

                characterMapper.insert(character);
                newCharacters.add(character);
                existingNames.add(name); // 添加到已存在集合，防止同一批次重复

                log.info("保存新角色: {} ({})", name, character.getRoleType());
            }

            return newCharacters;

        } catch (Exception e) {
            log.error("解析角色失败", e);
            log.error("AI响应: {}", aiResponse);
            return new ArrayList<>();
        }
    }

    /**
     * 清理JSON字符串，修复常见格式问题
     * 处理AI返回的中文标点、注释、缺失逗号等问题
     */
    private String cleanJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }

        // 去除markdown代码块标记 ```json ... ```
        String cleaned = json.replaceAll("(?s)```\\s*json\\s*", "");
        cleaned = cleaned.replaceAll("(?s)```\\s*", "");

        // 去除单行注释 // ...
        cleaned = cleaned.replaceAll("//[^\\n\\r]*", "");

        // 去除多行注释 /* ... */
        cleaned = cleaned.replaceAll("/\\*.*?\\*/", "");

        // 处理中文标点符号
        // 中文引号 -> 英文引号
        cleaned = cleaned.replace('"', '"').replace('"', '"');
        // 中文冒号 -> 英文冒号
        cleaned = cleaned.replace('：', ':');
        // 修复双冒号问题：：  -> :
        cleaned = cleaned.replaceAll("::", ":");
        cleaned = cleaned.replaceAll("：:", ":");
        // 中文逗号 -> 英文逗号
        cleaned = cleaned.replace('，', ',');

        // 修复尾随逗号（在 } 或 ] 之前的逗号）
        cleaned = cleaned.replaceAll(",\\s*([\\]}])", "$1");

        // 修复缺失的逗号：在字符串值后面的换行+空格+引号之间插入逗号
        // 模式1: "value"\n  "key" -> "value",\n  "key"
        cleaned = cleaned.replaceAll("\"\n\\s*\"", "\",\n      \"");

        // 修复缺失的逗号：在数字、}或]后面的换行+空格+引号之间插入逗号
        cleaned = cleaned.replaceAll("([\\d]}])\\n\\s*\"", "$1,\n      \"");

        // 修复缺失的逗号：在{}后面的换行和引号之间插入逗号
        cleaned = cleaned.replaceAll("(\\}|\")\\n\\s*\"", "$1,\n  \"");

        // 修复对象之间缺少逗号的问题：在 "key" 后面如果直接跟 "key" 而没有逗号，添加逗号
        // 这种情况可能发生在同一行：}    {
        cleaned = cleaned.replaceAll("\\}\\s+\\{", "},\n    {");

        // 修复同一行内对象之间缺少逗号：    "key": "value"    "key2"
        cleaned = cleaned.replaceAll("\"\\s+\"", "\", \"");

        // 修复更复杂的缺失逗号情况：两个双引号字符串之间缺失逗号
        // 模式: "任意内容"\n\s+"任意内容" -> "任意内容",\n  "任意内容"
        cleaned = cleaned.replaceAll("([^\"]\")\\n\\s+\"", "$1\",\n      \"");

        // 修复字段值后面直接跟下一个字段名的情况（在同一行）
        // 模式: "value"  "key": -> "value",  "key":
        cleaned = cleaned.replaceAll("\"\\s{2,}\"", "\", \"");

        // 修复缺失引号的问题："key":value -> "key": "value"
        // 当冒号后面直接跟非引号、非花括号、非方括号、非数字、非布尔值、非null的文本时，添加引号
        // 模式: "key":text, -> "key": "text",
        // 使用负向预查确保不匹配数字、true、false、null、{、[
        cleaned = cleaned.replaceAll(
            "\"([^\"]+)\":\\s*([0-9]|true|false|null|\\{|\\[)",
            "\"$1\": $2"
        );
        // 对于不是上述情况的，添加引号（但要小心不要破坏已有引号的情况）
        // 模式: "key":value (其中value以中文或字母开头) -> "key": "value"
        cleaned = cleaned.replaceAll(
            "\"([^\"]+)\":\\s*([\\u4e00-\\u9fa5a-zA-Z][^,\"]*?),",
            "\"$1\": \"$2\","
        );
        cleaned = cleaned.replaceAll(
            "\"([^\"]+)\":\\s*([\\u4e00-\\u9fa5a-zA-Z][^,\"]*?)\\n",
            "\"$1\": \"$2\",\n"
        );

        // 修复错误的字段名：chapterCountTarget -> wordCountTarget
        cleaned = cleaned.replaceAll("chapterCountTarget", "wordCountTarget");

        // 去除控制字符（除了换行、制表符等）
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B-\\x0C\\x0E-\\x1F\\x7F]", "");

        return cleaned;
    }

    /**
     * 步骤: 生成分卷章节计划
     */
    private StepResult generateChaptersForVolume(AiTaskStep step, TaskContext context) {
        try {
            JsonNode config = context.getConfig();
            Long projectId = context.getProjectId();
            Long volumeId = config.has("volumeId") ? config.get("volumeId").asLong() : null;
            int targetChapterCount = config.has("targetChapterCount") ? config.get("targetChapterCount").asInt() : 35;
            String density = config.has("density") ? config.get("density").asText() : "medium";
            String projectDescription = config.has("projectDescription") ? config.get("projectDescription").asText() : "";
            String storyTone = config.has("storyTone") ? config.get("storyTone").asText() : "";
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            // 从数据库查询完整的分卷信息
            NovelVolumePlan volume = volumePlanMapper.selectById(volumeId);
            if (volume == null) {
                return StepResult.failure("分卷不存在，ID: " + volumeId);
            }

            int volumeNumber = volume.getVolumeNumber();
            log.info("开始为第{}卷生成章节计划，目标章节数: {}, 密度: {}", volumeNumber, targetChapterCount, density);

            // 构建完整的分卷信息字符串（使用公共方法）
            String volumeInfo = promptTemplateBuilder.buildVolumeInfo(volume);

            // 构建提示词
            String prompt = buildChapterPrompt(
                context,
                projectDescription,
                storyTone,
                storyGenre,
                volumeNumber,
                volumeInfo,
                targetChapterCount,
                3000
            );

            // 调用LLM生成
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(16000);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("LLM返回章节内容长度: {}", responseContent.length());

            // 解析章节XML
            List<Map<String, String>> chaptersList = parseChaptersXml(responseContent);

            if (chaptersList == null || chaptersList.isEmpty()) {
                return StepResult.failure("章节解析失败");
            }

            log.info("成功解析 {} 个章节", chaptersList.size());

            // 保存章节数据到共享数据
            String chaptersXmlKey = "chaptersXml_" + volumeNumber;
            context.putSharedData(chaptersXmlKey, responseContent);
            context.putSharedData("chaptersList_" + volumeNumber, chaptersList);
            context.putSharedData("volumeId_" + volumeNumber, volumeId);

            // 保存到数据库
            saveChaptersToDatabase(projectId, volumeId, volumeNumber, chaptersList);

            Map<String, Object> result = new HashMap<>();
            result.put("volumeNumber", volumeNumber);
            result.put("chapterCount", chaptersList.size());
            result.put("density", density);

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("生成章节计划失败", e);
            return StepResult.failure("生成章节计划失败: " + e.getMessage());
        }
    }

    /**
     * 保存章节到数据库
     */
    private void saveChaptersToDatabase(Long projectId, Long volumeId, int volumeNumber,
                                       List<Map<String, String>> chaptersList) {
        LocalDateTime now = LocalDateTime.now();

        // 先删除该分卷的旧章节
        int deletedCount = chapterPlanMapper.delete(
            new LambdaQueryWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getProjectId, projectId)
                .eq(NovelChapterPlan::getVolumePlanId, volumeId)
        );

        log.info("删除第{}卷的旧章节 {} 个", volumeNumber, deletedCount);

        // 保存新章节
        for (Map<String, String> chapterData : chaptersList) {
            NovelChapterPlan chapterPlan = new NovelChapterPlan();
            chapterPlan.setProjectId(projectId);
            chapterPlan.setVolumePlanId(volumeId);
            chapterPlan.setChapterNumber(Integer.parseInt(chapterData.getOrDefault("chapterNumber", "0")));
            chapterPlan.setChapterTitle(chapterData.getOrDefault("chapterTitle", ""));
            chapterPlan.setPlotOutline(chapterData.getOrDefault("plotOutline", ""));
            chapterPlan.setKeyEvents(chapterData.getOrDefault("keyEvents", ""));
            chapterPlan.setChapterGoal(chapterData.getOrDefault("chapterGoal", ""));
            chapterPlan.setWordCountTarget(Integer.parseInt(chapterData.getOrDefault("wordCountTarget", "3000")));
            chapterPlan.setChapterStartingScene(chapterData.getOrDefault("chapterStartingScene", ""));
            chapterPlan.setChapterEndingScene(chapterData.getOrDefault("chapterEndingScene", ""));
            chapterPlan.setStatus("planned");
            chapterPlan.setCreateTime(now);
            chapterPlan.setUpdateTime(now);

            chapterPlanMapper.insert(chapterPlan);
        }

        log.info("保存第{}卷的新章节 {} 个", volumeNumber, chaptersList.size());

        // Per D-06: 重新规划时先删除该卷pending伏笔
        foreshadowingService.deletePendingForeshadowingForVolume(projectId, volumeNumber);

        // 解析并保存伏笔数据
        int foreshadowingCount = 0;
        log.info("开始解析第{}卷伏笔数据，章节数: {}", volumeNumber, chaptersList.size());
        for (Map<String, String> chapterData : chaptersList) {
            String plantCountStr = chapterData.get("_foreshadowingPlants_count");
            String chapterNum = chapterData.getOrDefault("chapterNumber", "?");
            log.info("章节 {} : _foreshadowingPlants_count = {}", chapterNum, plantCountStr);
            if (plantCountStr != null) {
                int plantCount = Integer.parseInt(plantCountStr);
                String chapterNumberStr = chapterData.getOrDefault("chapterNumber", "0");
                for (int i = 0; i < plantCount; i++) {
                    try {
                        com.aifactory.dto.ForeshadowingCreateDto fsDto = new com.aifactory.dto.ForeshadowingCreateDto();
                        fsDto.setTitle(chapterData.getOrDefault("_fs_" + i + "_ft", "未命名伏笔"));
                        fsDto.setType(chapterData.getOrDefault("_fs_" + i + "_fy", "event"));
                        fsDto.setDescription(chapterData.getOrDefault("_fs_" + i + "_fd", ""));
                        fsDto.setLayoutType(chapterData.getOrDefault("_fs_" + i + "_fl", "bright1"));
                        fsDto.setPlantedChapter(Integer.parseInt(chapterNumberStr));
                        fsDto.setPlantedVolume(volumeNumber);

                        String fcStr = chapterData.get("_fs_" + i + "_fc");
                        if (fcStr != null && !fcStr.isEmpty()) {
                            fsDto.setPlannedCallbackVolume(parseIntSafe(fcStr, null));
                        }
                        String frStr = chapterData.get("_fs_" + i + "_fr");
                        if (frStr != null && !frStr.isEmpty()) {
                            fsDto.setPlannedCallbackChapter(parseIntSafe(frStr, null));
                        }

                        foreshadowingService.createForeshadowing(projectId, fsDto);
                        foreshadowingCount++;
                    } catch (Exception e) {
                        log.warn("保存第{}卷第{}章伏笔失败(第{}个): {}", volumeNumber, chapterNumberStr, i, e.getMessage());
                    }
                }
            }
        }
        log.info("保存第{}卷伏笔 {} 个", volumeNumber, foreshadowingCount);
    }

    /**
     * 安全解析整数，解析失败返回默认值
     */
    private Integer parseIntSafe(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
