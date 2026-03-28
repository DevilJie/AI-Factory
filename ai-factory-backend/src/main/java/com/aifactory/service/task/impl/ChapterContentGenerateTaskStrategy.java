package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.CharacterPromptInfo;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.ChapterPlotMemory;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.Project;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.ChapterPlotMemoryMapper;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.chapter.persistence.ChapterPersistenceService;
import com.aifactory.service.chapter.prompt.PromptTemplateBuilder;
import com.aifactory.service.ChapterCharacterExtractService;
import com.aifactory.service.ChapterService;
import com.aifactory.service.llm.LLMProvider;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.task.TaskStrategy;
import com.aifactory.common.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 章节内容生成任务策略（同步，非流式）
 *
 * @Author CaiZy
 * @Date 2025-02-07
 */
@Slf4j
@Component("generate_chapter_contentTaskStrategy")
public class ChapterContentGenerateTaskStrategy implements TaskStrategy {

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ChapterPersistenceService chapterPersistenceService;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateBuilder promptTemplateBuilder;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChapterPlotMemoryMapper plotMemoryMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ChapterCharacterExtractService chapterCharacterExtractService;

    @Override
    public String getTaskType() {
        return "generate_chapter_content";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            JsonNode config = objectMapper.readTree(task.getConfigJson());

            List<StepConfig> steps = new ArrayList<>();

            Map<String, Object> generateConfig = new HashMap<>();
            generateConfig.put("planId", config.get("planId"));
            generateConfig.put("userId", config.get("userId"));
            steps.add(new StepConfig(1, "生成章节内容", "generate_content", generateConfig));

            log.info("创建章节内容生成任务步骤完成，planId={}", config.get("planId"));

            return steps;

        } catch (Exception e) {
            log.error("创建章节内容生成任务步骤失败", e);
            throw new RuntimeException("创建章节内容生成任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            if ("generate_content".equals(stepType)) {
                return generateContent(step, context);
            } else {
                return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    @Override
    public int calculateProgress(AiTaskStep currentStep, List<AiTaskStep> allSteps) {
        if ("completed".equals(currentStep.getStatus())) {
            return 100;
        } else if ("running".equals(currentStep.getStatus())) {
            if (currentStep.getProgress() != null) {
                return currentStep.getProgress();
            }
            return 50;
        }
        return 0;
    }

    /**
     * 生成章节内容（同步模式，不使用流式）
     */
    private StepResult generateContent(AiTaskStep step, TaskContext context) {
        Long planId = null;
        Long userId = null;

        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            planId = stepConfig.get("planId").asLong();
            userId = stepConfig.get("userId").asLong();

            log.info("开始生成章节内容，planId={}, userId={}", planId, userId);

            // 获取章节规划
            NovelChapterPlan chapterPlan = chapterPlanMapper.selectById(planId);
            if (chapterPlan == null) {
                return StepResult.failure("章节规划不存在，planId: " + planId);
            }

            // 创建或获取章节
            Chapter chapter = chapterPersistenceService.findByPlanId(planId);
            if (chapter == null) {
                chapter = chapterPersistenceService.createChapterFromPlan(chapterPlan);
                log.info("创建新章节，章节ID: {}", chapter.getId());
            }

            // 获取分卷规划
            NovelVolumePlan volumePlan = volumePlanMapper.selectById(chapterPlan.getVolumePlanId());
            if (volumePlan == null) {
                return StepResult.failure("分卷规划不存在，volumePlanId: " + chapterPlan.getVolumePlanId());
            }

            // 获取世界观
            NovelWorldview worldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, chapterPlan.getProjectId())
                    .orderByDesc(NovelWorldview::getUpdateTime)
                    .last("LIMIT 1")
            );

            // 获取前置章节上下文（最近5章）
            List<Chapter> recentChapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                    .eq(Chapter::getProjectId, chapterPlan.getProjectId())
                    .lt(Chapter::getChapterNumber, chapterPlan.getChapterNumber())
                    .orderByDesc(Chapter::getChapterNumber)
                    .last("LIMIT 5")
            );

            // 获取上一章的结束场景（确保连贯性）
            String lastChapterEndingScene = getLastChapterEndingScene(
                chapterPlan.getProjectId(), chapterPlan.getChapterNumber()
            );

            // 构建角色提示词信息列表（新增）
            List<CharacterPromptInfo> characterPromptInfoList = promptTemplateBuilder
                    .buildCharacterPromptInfoList(chapterPlan.getProjectId(), chapterPlan.getChapterNumber());            // 构建提示词（使用模板体系）
            String prompt = promptTemplateBuilder.buildChapterPrompt(
                chapterPlan.getProjectId(),
                chapterPlan,
                volumePlan,
                worldview,
                recentChapters,
                "",  // 知识库检索上下文（已移除Dify）
                lastChapterEndingScene
            );

            log.info("提示词长度: {}", prompt.length());

            // 设置用户上下文
            UserContext.setUserId(userId);

            // 创建 AI 请求
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(chapterPlan.getProjectId());
            aiRequest.setVolumePlanId(chapterPlan.getVolumePlanId());  // 分卷规划ID
            aiRequest.setChapterPlanId(chapterPlan.getId());  // 章节规划ID
            aiRequest.setChapterId(chapter.getId());  // 章节ID
            aiRequest.setRequestType("llm_chapter_generate_standard");
            aiRequest.setTask(prompt);
            aiRequest.setTemperature(0.8);
            aiRequest.setMaxTokens(null);

            // 获取 LLM Provider 并调用同步生成
            LLMProvider provider = llmProviderFactory.getDefaultProvider();
            log.info("开始调用 LLM 生成章节（同步模式）...");

            // 同步调用，等待完整响应
            AIGenerateResponse response = provider.generate(aiRequest);
            String content = response.getContent();

            if (content == null || content.isEmpty()) {
                log.error("LLM 返回的内容为空");
                return StepResult.failure("章节生成失败：LLM 返回内容为空");
            }

            log.info("LLM 生成完成，内容长度: {}", content.length());

            // 检查字数并压缩
            int wordCount = content.length();
            String finalContent = content;


            // 保存章节内容（直接更新，不使用 completeChapter，避免被覆盖）
            chapter.setContent(finalContent);
            chapter.setWordCount(wordCount);
            chapter.setUpdateTime(LocalDateTime.now());
            chapterMapper.updateById(chapter);

            log.info("章节内容生成完成，planId={}, chapterId={}, 字数={}",
                planId, chapter.getId(), chapter.getWordCount());

            // 生成章节记忆（异步，不阻塞返回）
            try {
                chapterService.rebuildChapterMemory(chapter.getId());
                log.info("章节记忆生成任务已提交，chapterId={}", chapter.getId());
            } catch (Exception e) {
                log.error("生成章节记忆失败，不影响章节保存，chapterId={}，错误：{}",
                    chapter.getId(), e.getMessage());
            }

            // 同步提取章节角色（章节保存后立即执行）
            try {
                chapterCharacterExtractService.extractCharacters(chapter, userId);
                log.info("章节角色提取完成，chapterId={}", chapter.getId());
            } catch (Exception e) {
                log.error("章节角色提取失败，chapterId={}，错误：{}",
                    chapter.getId(), e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("chapterId", chapter.getId());
            result.put("planId", planId);
            result.put("wordCount", chapter.getWordCount());
            result.put("title", chapter.getTitle());
            result.put("message", "章节生成完成");

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("生成章节内容失败，planId={}", planId, e);
            return StepResult.failure("生成章节内容失败: " + e.getMessage());
        }
    }

    /**
     * 获取上一章的结束场景
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节号
     * @return 上一章的结束场景，如果不存在返回空字符串
     */
    private String getLastChapterEndingScene(Long projectId, Integer currentChapterNumber) {
        if (currentChapterNumber <= 1) {
            return "";  // 第一章没有上一章
        }

        try {
            // 查询上一章的剧情记忆
            List<ChapterPlotMemory> memories = plotMemoryMapper.selectRecentMemories(
                projectId, currentChapterNumber, 1
            );

            if (memories.isEmpty()) {
                log.debug("未找到第 {} 章的剧情记忆", currentChapterNumber - 1);
                return "";
            }

            ChapterPlotMemory lastMemory = memories.get(0);
            String endingScene = lastMemory.getChapterEndingScene();

            if (endingScene != null && !endingScene.isEmpty()) {
                log.info("成功获取第 {} 章的结束场景", currentChapterNumber - 1);
                return endingScene;
            }

            return "";
        } catch (Exception e) {
            log.error("获取上一章结束场景失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 压缩章节内容（字数超过3000时调用）
     *
     * @param originalContent 原始章节内容
     * @param chapter 章节对象，用于设置上下文ID
     * @return 压缩后的内容
     */
    private String compressChapterContent(String originalContent, Chapter chapter) throws Exception {
        // // 构建压缩提示词
        // String compressPrompt = buildCompressPrompt(originalContent);

        // // 调用LLM进行压缩
        // AIGenerateRequest aiRequest = new AIGenerateRequest();
        // aiRequest.setProjectId(chapter.getProjectId());
        // aiRequest.setVolumePlanId(chapter.getVolumePlanId());
        // aiRequest.setChapterPlanId(chapter.getChapterPlanId());
        // aiRequest.setChapterId(chapter.getId());
        // aiRequest.setRequestType("llm_chapter_compress");
        // aiRequest.setTask(compressPrompt);
        // aiRequest.setTemperature(0.3); // 使用较低的温度保证稳定性

        // LLMProvider provider = llmProviderFactory.getDefaultProvider();

        // log.info("开始调用LLM压缩章节内容...");
        // AIGenerateResponse response = provider.generate(aiRequest);
        // String result = response.getContent();

        // if (result == null || result.isEmpty()) {
        //     throw new RuntimeException("压缩结果为空");
        // }

        return originalContent;
    }

    /**
     * 构建压缩提示词
     */
    private String buildCompressPrompt(String content) {
        return String.format("""
# 章节内容压缩任务

你是资深网文编辑，擅长压缩文章内容而不损失核心剧情。

## 任务说明

对提供的章节内容进行压缩，将字数严格控制在2500字左右。

## 压缩原则

1. **保持核心剧情完整**：
   - 保留所有关键情节转折
   - 保留所有重要对话
   - 保留所有人物互动和冲突
   - 保留情节推进的核心节奏

2. **删除冗余内容**：
   - 删除过于冗长的环境描写
   - 删除重复的心理描写
   - 删除不必要的外貌和服装细节
   - 删除无关紧要的动作细节
   - 删除啰嗦的过渡段落

3. **精简表达方式**：
   - 将冗长的对话改为简洁表达
   - 合并相似的段落
   - 用概括性语言替代细节描写
   - 删除过多的修饰语和形容词

4. **保持原文格式**：
   - 保持段落分隔（每段开头两个全角空格）
   - 保持对话独立成段
   - 保持引号使用规范

## 字数要求

- 目标字数：2400-2600字（必须严格遵守）
- 不得低于2400字，不得高于2600字

## 输出要求

1. 只输出压缩后的正文内容
2. 不包含标题、说明或标记
3. 确保压缩后内容连贯、流畅、可读

## 待压缩内容

%s
""", content);
    }

    /**
     * 统计中文字数（不包括标点符号和空格）
     */
    private int countChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 统计汉字、中文标点
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChineseCharacter(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChineseCharacter(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

}
