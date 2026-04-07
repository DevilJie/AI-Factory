package com.aifactory.service.task.impl;

import com.aifactory.common.NameMatchUtil;
import com.aifactory.common.XmlParser;
import com.aifactory.dto.ChapterPlanXmlDto;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.NovelCharacterService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.aifactory.service.task.context.ChapterContextBuilder;
import com.aifactory.service.task.validator.ChapterPlotStageValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 章节生成任务策略（重构版：支持情节阶段和分批生成）
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component("generate_chaptersTaskStrategy")
public class ChapterGenerationTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ChapterPlotStageValidator stageValidator;

    @Autowired
    private ChapterContextBuilder contextBuilder;

    @Autowired
    private XmlParser xmlParser;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private FactionService factionService;

    @Autowired
    private NovelCharacterService novelCharacterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getTaskType() {
        return "generate_chapters";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            JsonNode config = objectMapper.readTree(task.getConfigJson());

            List<StepConfig> steps = new ArrayList<>();

            // 步骤: 生成分卷章节计划
            Map<String, Object> chapterConfig = new HashMap<>();
            chapterConfig.put("volumeId", config.has("volumeId") ? config.get("volumeId").asLong() : null);
            chapterConfig.put("plotStage", config.has("plotStage") ? config.get("plotStage").asText() : "introduction");
            chapterConfig.put("volumeNumber", config.has("volumeNumber") ? config.get("volumeNumber").asInt() : 1);
            chapterConfig.put("volumeTitle", config.has("volumeTitle") ? config.get("volumeTitle").asText() : "");
            chapterConfig.put("volumeTheme", config.has("volumeTheme") ? config.get("volumeTheme").asText() : "");
            chapterConfig.put("mainConflict", config.has("mainConflict") ? config.get("mainConflict").asText() : "");
            chapterConfig.put("plotArc", config.has("plotArc") ? config.get("plotArc").asText() : "");
            chapterConfig.put("targetChapterCount", config.has("targetChapterCount") ? config.get("targetChapterCount").asInt() : 10);
            chapterConfig.put("projectDescription", config.has("projectDescription") ? config.get("projectDescription").asText() : "");
            chapterConfig.put("storyTone", config.has("storyTone") ? config.get("storyTone").asText() : "");
            chapterConfig.put("storyGenre", config.has("storyGenre") ? config.get("storyGenre").asText() : "");

            steps.add(new StepConfig(1, "生成分卷章节计划", "generate_chapters", chapterConfig));

            log.info("创建章节生成任务步骤完成");

            return steps;

        } catch (Exception e) {
            log.error("创建章节生成任务步骤失败", e);
            throw new RuntimeException("创建章节生成任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "generate_chapters":
                    return generateChaptersForVolume(step, context);
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
        // 默认进度计算
        return TaskStrategy.super.calculateProgress(currentStep, allSteps);
    }

    /**
     * 步骤: 生成分卷章节计划（重构版：支持分批生成）
     */
    private StepResult generateChaptersForVolume(AiTaskStep step, TaskContext context) {
        try {
            JsonNode config = context.getConfig();
            Long projectId = context.getProjectId();
            Long volumeId = config.has("volumeId") ? config.get("volumeId").asLong() : null;
            String plotStage = config.has("plotStage") ? config.get("plotStage").asText() : "introduction";
            int volumeNumber = config.has("volumeNumber") ? config.get("volumeNumber").asInt() : 1;
            String volumeTitle = config.has("volumeTitle") ? config.get("volumeTitle").asText() : "";
            String volumeTheme = config.has("volumeTheme") ? config.get("volumeTheme").asText() : "";
            String mainConflict = config.has("mainConflict") ? config.get("mainConflict").asText() : "";
            String plotArc = config.has("plotArc") ? config.get("plotArc").asText() : "";
            int targetChapterCount = config.has("targetChapterCount") ? config.get("targetChapterCount").asInt() : 10;
            String projectDescription = config.has("projectDescription") ? config.get("projectDescription").asText() : "";
            String storyTone = config.has("storyTone") ? config.get("storyTone").asText() : "";
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            log.info("开始为第{}卷生成{}阶段章节计划，目标章节数: {}", volumeNumber, plotStage, targetChapterCount);

            // 1. 验证阶段合法性
            ChapterPlotStageValidator.ValidationResult validation = stageValidator.validateStageRequest(volumeId, plotStage);
            if (!validation.isValid()) {
                return StepResult.failure(validation.getError());
            }

            // 2. 计算下一个章节编号
            int nextChapterNumber = stageValidator.calculateNextChapterNumber(volumeId);

            // 3. 构建连贯性上下文
            String recentContext = contextBuilder.buildRecentChaptersContext(volumeId, 5);

            // 4. 从数据库获取完整的分卷信息
            NovelVolumePlan volumeEntity = volumePlanMapper.selectById(volumeId);
            String volumeDescription = volumeEntity != null ? volumeEntity.getVolumeDescription() : "";
            String keyEvents = volumeEntity != null ? volumeEntity.getKeyEvents() : "";
            String coreGoal = volumeEntity != null ? volumeEntity.getCoreGoal() : "";
            String climax = volumeEntity != null ? volumeEntity.getClimax() : "";
            String ending = volumeEntity != null ? volumeEntity.getEnding() : "";
            String timelineSetting = volumeEntity != null ? volumeEntity.getTimelineSetting() : "";
            String volumeNotes = volumeEntity != null ? volumeEntity.getVolumeNotes() : "";

            // 5. 从数据库查询世界观并设置到context
            NovelWorldview worldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );
            if (worldview != null) {
                continentRegionService.fillGeography(worldview);
                factionService.fillForces(worldview);
                context.putSharedData("worldview", worldview);
                log.info("已加载世界观信息到context");
            } else {
                log.info("未找到世界观设定");
            }

            // 5b. 加载非NPC角色用于名称匹配
            List<NovelCharacter> allCharacters = novelCharacterService.getNonNpcCharacters(projectId);
            log.info("已加载 {} 个非NPC角色用于名称匹配", allCharacters.size());

            // 6. 格式化keyEvents（如果是JSON格式，转换为可读文本）
            String formattedKeyEvents = formatKeyEvents(keyEvents);

            // 7. 构建完整的分卷信息字符串
            String volumeInfo = String.format("""
                【分卷信息】
                - 卷号：第%d卷
                - 卷名：%s
                - 本卷主旨：%s
                - 主要冲突：%s
                - 情节走向：%s
                - 本卷简介：%s
                - 关键事件：%s
                - 核心目标：%s
                - 高潮事件：%s
                - 收尾描述：%s
                - 时间线设定：%s
                - 分卷备注：%s
                """, volumeNumber, volumeTitle, volumeTheme, mainConflict, plotArc,
                volumeDescription, formattedKeyEvents, coreGoal, climax, ending,
                timelineSetting, volumeNotes);

            // 8. 分批生成（每批5章）
            int batchSize = 5;
            int totalGenerated = 0;
            int currentChapterNumber = nextChapterNumber;
            int remainingChapters = targetChapterCount;
            int maxRetries = 3;  // 每批最大重试次数

            while (remainingChapters > 0) {
                int currentBatchSize = Math.min(batchSize, remainingChapters);
                int batchNumber = (totalGenerated / batchSize + 1);
                log.info("开始生成第{}批章节，本批章节数: {}, 起始章节号: {}",
                    batchNumber, currentBatchSize, currentChapterNumber);

                // 构建提示词
                String prompt = buildChapterPromptUsingTemplate(
                        context,
                        projectDescription,
                        storyTone,
                        storyGenre,
                        volumeNumber,
                        volumeInfo,
                        plotStage,
                        currentBatchSize,
                        currentChapterNumber,
                        recentContext,
                        3000
                );

                // 调用LLM生成（带重试）
                List<Map<String, String>> chaptersList = null;
                int retryCount = 0;

                while (retryCount < maxRetries && (chaptersList == null || chaptersList.isEmpty())) {
                    if (retryCount > 0) {
                        log.info("第{}批章节第{}次重试...", batchNumber, retryCount);
                    }

                    try {
                        com.aifactory.dto.AIGenerateRequest aiRequest = new com.aifactory.dto.AIGenerateRequest();
                        aiRequest.setProjectId(projectId);
                        aiRequest.setVolumePlanId(volumeId);
                        aiRequest.setRequestType("llm_outline_chapter_generate");
                        aiRequest.setRole(AIRole.NOVEL_WRITER);
                        aiRequest.setTask(prompt);

                        com.aifactory.dto.AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
                        String responseContent = aiResponse.getContent();

                        log.info("LLM返回章节内容长度: {}", responseContent.length());

                        // 解析章节XML
                        chaptersList = parseChaptersXml(responseContent);

                        if (chaptersList == null || chaptersList.isEmpty()) {
                            log.warn("第{}批章节第{}次尝试解析失败", batchNumber, retryCount + 1);
                        }
                    } catch (Exception e) {
                        log.error("第{}批章节第{}次尝试失败: {}", batchNumber, retryCount + 1, e.getMessage());
                    }

                    retryCount++;
                }

                if (chaptersList == null || chaptersList.isEmpty()) {
                    log.error("第{}批章节解析失败，已重试{}次，跳过该批", batchNumber, maxRetries);
                    // 跳过这一批，继续下一批（但正确更新章节号）
                    currentChapterNumber += currentBatchSize;
                    remainingChapters -= currentBatchSize;
                    continue;
                }

                log.info("成功解析 {} 个章节", chaptersList.size());

                // 保存到数据库
                saveChaptersToDatabase(projectId, volumeId, plotStage, currentChapterNumber, chaptersList, allCharacters);

                // 更新上下文（用于下一批的连贯性）
                recentContext = buildRecentContextFromChapters(chaptersList);

                // 使用实际生成的章节数更新状态
                int actualGenerated = chaptersList.size();
                totalGenerated += actualGenerated;
                currentChapterNumber += actualGenerated;
                remainingChapters -= currentBatchSize;  // 减去期望值，因为下一批从新的位置开始
            }

            Map<String, Object> result = new HashMap<>();
            result.put("volumeNumber", volumeNumber);
            result.put("plotStage", plotStage);
            result.put("chapterCount", totalGenerated);

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("生成章节计划失败", e);
            return StepResult.failure("生成章节计划失败: " + e.getMessage());
        }
    }

    /**
     * 格式化keyEvents（如果是JSON格式，转换为可读文本）
     */
    private String formatKeyEvents(String keyEvents) {
        if (keyEvents == null || keyEvents.isEmpty()) {
            return "";
        }

        // 尝试解析为JSON（5阶段格式）
        if (keyEvents.startsWith("{")) {
            try {
                JsonNode jsonNode = objectMapper.readTree(keyEvents);
                StringBuilder sb = new StringBuilder();

                String[] stages = {"opening", "development", "turning", "climax", "ending"};
                String[] stageNames = {"【开篇阶段】", "【发展阶段】", "【转折阶段】", "【高潮阶段】", "【收尾阶段】"};

                for (int i = 0; i < stages.length; i++) {
                    if (jsonNode.has(stages[i])) {
                        JsonNode events = jsonNode.get(stages[i]);
                        if (events.isArray() && events.size() > 0) {
                            sb.append(stageNames[i]).append("\n");
                            for (int j = 0; j < events.size(); j++) {
                                sb.append("  ").append(j + 1).append(". ").append(events.get(j).asText()).append("\n");
                            }
                        }
                    }
                }

                return sb.toString();
            } catch (Exception e) {
                log.warn("解析keyEvents JSON失败，返回原始内容: {}", e.getMessage());
                return keyEvents;
            }
        }

        return keyEvents;
    }

    /**
     * 从生成的章节构建最近上下文（用于下一批的连贯性）
     */
    private String buildRecentContextFromChapters(List<Map<String, String>> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【上一批章节摘要】\n");

        for (Map<String, String> chapter : chapters) {
            String chapterNum = chapter.getOrDefault("chapterNumber", "?");
            String title = chapter.getOrDefault("chapterTitle", "");
            String plotOutline = chapter.getOrDefault("plotOutline", "");
            String endingScene = chapter.getOrDefault("chapterEndingScene", "");

            sb.append("第").append(chapterNum).append("章 ").append(title).append("\n");
            sb.append("  情节：").append(plotOutline.length() > 100 ? plotOutline.substring(0, 100) + "..." : plotOutline).append("\n");
            if (!endingScene.isEmpty()) {
                sb.append("  结尾场景：").append(endingScene).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 使用模板系统构建章节规划提示词
     *
     * @param context 任务上下文
     * @param projectDescription 项目描述
     * @param storyTone 故事基调
     * @param storyGenre 故事类型
     * @param volumeNumber 卷号
     * @param volumeInfo 分卷信息
     * @param plotStage 情节阶段
     * @param chapterCount 章节数
     * @param startChapterNumber 起始章节编号
     * @param recentContext 前情上下文
     * @param wordCount 每章字数
     * @return 填充后的完整提示词
     */
    private String buildChapterPromptUsingTemplate(TaskContext context, String projectDescription, String storyTone,
                                                   String storyGenre, int volumeNumber,
                                                   String volumeInfo, String plotStage, int chapterCount,
                                                   int startChapterNumber, String recentContext, int wordCount) {
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

            // 世界观信息
            StringBuilder worldviewBuilder = new StringBuilder();
            if (context.getSharedData().containsKey("worldview")) {
                NovelWorldview worldview = (NovelWorldview) context.getSharedData().get("worldview");
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
            }
            variables.put("worldviewInfo", worldviewBuilder.length() > 0 ? worldviewBuilder.toString() : "暂无世界观设定");

            // 角色信息（当前策略不直接使用角色数据，设为默认值）
            variables.put("characterInfo", "暂无登场角色");

            // 叙事设置（当前策略不直接使用叙事设置，设为默认值）
            variables.put("narrativeSettings", "");

            // 章节节奏指导（根据情节阶段生成）
            String paceGuide = buildPaceGuide(plotStage, startChapterNumber, recentContext);
            variables.put("paceGuide", paceGuide);

            // 执行模板
            String templateCode = "llm_outline_chapter_generate";
            return promptTemplateService.executeTemplate(templateCode, variables);

        } catch (Exception e) {
            log.error("使用模板构建章节规划提示词失败，降级到原有方法", e);
            // 降级到原有方法
            return buildChapterPrompt(context, projectDescription, storyTone, storyGenre,
                    volumeNumber, volumeInfo, plotStage, chapterCount,
                    startChapterNumber, recentContext, wordCount);
        }
    }

    /**
     * 根据情节阶段构建章节节奏指导
     */
    private String buildPaceGuide(String plotStage, int startChapterNumber, String recentContext) {
        StringBuilder guide = new StringBuilder();

        // 添加阶段特定的指导
        guide.append("【当前阶段：").append(stageValidator.getStageDisplayName(plotStage)).append("】\n");
        guide.append(getStageDescription(plotStage)).append("\n");

        // 添加起始章节信息
        guide.append("【起始章节】\n");
        guide.append("- 章节号从第").append(startChapterNumber).append("章开始\n");

        // 添加前情上下文
        if (recentContext != null && !recentContext.isEmpty()) {
            guide.append("【前情提要】\n");
            guide.append(recentContext).append("\n");
        }

        return guide.toString();
    }

    /**
     * 构建生成章节的提示词（重构版：支持阶段描述和连贯性上下文）
     * 注意：此方法作为模板系统的降级方案保留
     */
    private String buildChapterPrompt(TaskContext context, String projectDescription, String storyTone,
                                      String storyGenre, int volumeNumber,
                                      String volumeInfo, String plotStage, int chapterCount,
                                      int startChapterNumber, String recentContext, int wordCount) {
        StringBuilder prompt = new StringBuilder();

        // ========== 【第一部分：XML格式要求 - 最高优先级】 ==========
        prompt.append("【XML输出格式要求 - CRITICAL - 必须严格遵守】\n\n");
        prompt.append("你返回的内容必须是纯XML格式，任何格式错误都会导致解析失败！\n\n");

        prompt.append("✅ 正确格式示例：\n");
        prompt.append("<c>\n");
        prompt.append("  <o>\n");
        prompt.append("    <n>1</n>\n");
        prompt.append("    <t><![CDATA[章节标题]]></t>\n");
        prompt.append("    <p><![CDATA[本章情节大纲（100-200字）]]></p>\n");
        prompt.append("    <e><![CDATA[关键事件1；关键事件2]]></e>\n");
        prompt.append("    <g><![CDATA[本章要达成的目标]]></g>\n");
        prompt.append("    <w>3000</w>\n");
        prompt.append("    <s><![CDATA[章节起点场景]]></s>\n");
        prompt.append("    <ed><![CDATA[章节终点场景]]></ed>\n");
        prompt.append("  </o>\n");
        prompt.append("  <o>\n");
        prompt.append("    <n>2</n>\n");
        prompt.append("    <t><![CDATA[第2章标题]]></t>\n");
        prompt.append("    <p><![CDATA[第2章情节大纲]]></p>\n");
        prompt.append("    <e><![CDATA[关键事件]]></e>\n");
        prompt.append("    <g><![CDATA[目标]]></g>\n");
        prompt.append("    <w>3000</w>\n");
        prompt.append("    <s><![CDATA[起点]]></s>\n");
        prompt.append("    <ed><![CDATA[终点]]></ed>\n");
        prompt.append("  </o>\n");
        prompt.append("</c>\n\n");

        prompt.append("❌ 常见错误（绝对不能犯）：\n");
        prompt.append("错误1：缺少<o>标签 -> </o>\\n  <n>8</n>\\n  <t>...（这会导致第8章完全丢失！）\n");
        prompt.append("错误2：缺少<p>标签 -> <o>\\n    <n>9</n>\\n    <t>...\\n    <e>...（跳过<p>直接写<e>）\n");
        prompt.append("错误3：标签不匹配 -> <o>...</o>（忘记写</o>结束标签）\n");
        prompt.append("错误4：使用markdown -> ```xml\\n<c>...（不要使用代码块标记）\n\n");

        prompt.append("🔍 生成后自我检查清单：\n");
        prompt.append("1. 每个章节是否都有<o>开始标签？\n");
        prompt.append("2. 每个章节是否都有</o>结束标签？\n");
        prompt.append("3. 每个章节是否都有<p>标签？\n");
        prompt.append("4. <o>标签的数量是否等于</o>标签的数量？\n");
        prompt.append("5. 标签顺序是否严格按照：n -> t -> p -> e -> g -> w -> s -> ed？\n\n");

        prompt.append("标签说明：c=章节列表, o=单个章节, n=编号, t=标题, p=情节大纲, e=关键事件, g=目标, w=字数, s=起点, ed=终点\n\n");
        prompt.append("====================================\n\n");

        // ========== 【第二部分：任务背景】 ==========
        prompt.append("【任务背景】\n");
        prompt.append("你是一位资深的网络小说作家和编辑。\n");
        prompt.append("当前任务：为第").append(volumeNumber).append("卷的")
            .append(stageValidator.getStageDisplayName(plotStage)).append("生成").append(chapterCount)
            .append("章详细大纲（章节号从第").append(startChapterNumber).append("章开始）\n\n");

        // 添加阶段描述
        prompt.append(getStageDescription(plotStage)).append("\n");

        prompt.append("【故事设定】\n");
        prompt.append("故事背景：").append(projectDescription).append("\n");
        prompt.append("故事基调：").append(storyTone).append("\n");
        prompt.append("故事类型：").append(storyGenre).append("\n");
        prompt.append(volumeInfo).append("\n");

        // 添加世界观信息（如果有）
        if (context.getSharedData().containsKey("worldview")) {
            NovelWorldview worldview = (NovelWorldview) context.getSharedData().get("worldview");
            prompt.append("世界观设定：\n");
            prompt.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
            String powerConstraint2 = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
            if (powerConstraint2 != null && !powerConstraint2.trim().isEmpty()) {
                prompt.append("- 力量体系：").append(powerConstraint2).append("\n");
            }
            if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                prompt.append("- 地理环境：").append(worldview.getGeography()).append("\n");
            }
            if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                prompt.append("- 势力分布：").append(worldview.getForces()).append("\n");
            }
            prompt.append("\n");
        }

        // 添加连贯性上下文
        if (recentContext != null && !recentContext.isEmpty()) {
            prompt.append("【前情提要】\n").append(recentContext).append("\n\n");
        }

        prompt.append("【章节要求】\n");
        prompt.append("- 生成").append(chapterCount).append("章新大纲\n");
        prompt.append("- 每章字数：约").append(wordCount).append("字\n");
        prompt.append("- 章节号从第").append(startChapterNumber).append("章开始\n\n");

        prompt.append("====================================\n");
        prompt.append("现在请按照上述XML格式要求，返回").append(chapterCount).append("个章节的完整数据：\n");

        return prompt.toString();
    }

    /**
     * 获取情节阶段描述
     */
    private String getStageDescription(String plotStage) {
        return switch (plotStage) {
            case "introduction" -> """
                    【起始阶段 - 世界观建立与人物登场】
                    本阶段目标：
                    - 建立世界观基础，展示力量体系、社会结构
                    - 主角登场，展示初始性格、能力、背景
                    - 铺垫主要矛盾，引入核心冲突线索
                    - 介绍关键配角，构建人际关系网络
                    """;

            case "development" -> """
                    【发展阶段 - 矛盾积累与力量成长】
                    本阶段目标：
                    - 深化主要矛盾，冲突逐步升级
                    - 主角经历挑战，能力与心智成长
                    - 揭示更多世界观细节和势力格局
                    - 埋设伏笔，为后续高潮做铺垫
                    - 节奏加快，事件密度增加
                    """;

            case "climax" -> """
                    【高潮阶段 - 决战与矛盾爆发】
                    本阶段目标：
                    - 主要矛盾集中爆发，达到顶点
                    - 关键伏笔回收，前情呼应
                    - 主角面临最大挑战，实现蜕变
                    - 多条线索汇聚，节奏紧凑
                    - 情感冲击力强，留下深刻印象
                    """;

            case "conclusion" -> """
                    【结局阶段 - 收尾与新开端】
                    本阶段目标：
                    - 解决剩余矛盾，完成角色弧光
                    - 回收关键伏笔，给出交代
                    - 展现角色成长，呼应开篇
                    - 为可能的续作留下钩子（可选）
                    - 节奏放缓，情感沉淀
                    """;

            default -> "";
        };
    }

    /**
     * 解析章节XML（DOM解析，支持嵌套同名<ch>标签）
     * 替代原有Jackson XmlParser，因为Jackson无法正确处理<o>内嵌套的多个<ch>同名标签。
     * 遵循WorldviewXmlParser的DOM解析模式。
     */
    private List<Map<String, String>> parseChaptersXml(String xmlStr) {
        try {
            // 提取 <c>...</c> 片段
            int start = xmlStr.indexOf("<c>");
            int end = xmlStr.indexOf("</c>");
            if (start < 0 || end < 0) {
                log.error("XML缺少根元素<c>");
                return null;
            }
            String chapterXml = "<root>" + xmlStr.substring(start, end + 4) + "</root>";

            // 清理XML，修复常见的LLM输出问题
            chapterXml = sanitizeXmlForDomParsing(chapterXml, new String[]{"o", "ch"});

            // DOM解析
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(chapterXml)));

            Element root = doc.getDocumentElement();

            // 使用getChildNodes()查找<c>元素（不使用getElementsByTagName）
            Element cElement = null;
            org.w3c.dom.NodeList rootChildren = root.getChildNodes();
            for (int i = 0; i < rootChildren.getLength(); i++) {
                org.w3c.dom.Node node = rootChildren.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "c".equals(node.getNodeName())) {
                    cElement = (Element) node;
                    break;
                }
            }
            if (cElement == null) {
                log.error("<c> 标签内无内容");
                return null;
            }

            // 遍历 <o> 子节点
            List<Map<String, String>> chapters = new ArrayList<>();
            org.w3c.dom.NodeList oNodes = cElement.getChildNodes();
            for (int i = 0; i < oNodes.getLength(); i++) {
                org.w3c.dom.Node oNode = oNodes.item(i);
                if (oNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "o".equals(oNode.getNodeName())) {
                    Map<String, String> chapterMap = parseSingleChapter((Element) oNode);
                    if (chapterMap != null) {
                        chapters.add(chapterMap);
                    }
                }
            }

            if (chapters.isEmpty()) {
                log.error("解析后的章节列表为空");
                return null;
            }

            log.info("成功解析 {} 个章节", chapters.size());
            return chapters;

        } catch (Exception e) {
            log.error("DOM解析章节XML失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析单个<o>章节元素：提取章节字段和<ch>角色标签
     */
    private Map<String, String> parseSingleChapter(Element oElement) {
        List<Map<String, Object>> plannedChars = new ArrayList<>();
        Map<String, String> chapter = new HashMap<>();

        org.w3c.dom.NodeList children = oElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();
            String text = child.getTextContent().trim();

            switch (tag) {
                case "n" -> chapter.put("chapterNumber", text);
                case "t" -> chapter.put("chapterTitle", text);
                case "p" -> chapter.put("plotOutline", text);
                case "e" -> chapter.put("keyEvents", text);
                case "g" -> chapter.put("chapterGoal", text);
                case "w" -> chapter.put("wordCountTarget", text.isEmpty() ? "3000" : text);
                case "s" -> chapter.put("chapterStartingScene", text);
                case "ed", "f" -> chapter.put("chapterEndingScene", text);
                case "ch" -> {
                    // 解析角色标签: <ch><cn>名称</cn><cd>描述</cd><ci>重要度</ci></ch>
                    Map<String, Object> charData = parseCharacterTag((Element) child);
                    if (charData != null) {
                        plannedChars.add(charData);
                    }
                }
            }
        }

        // 章节编号为空则跳过
        if (chapter.get("chapterNumber") == null || chapter.get("chapterNumber").isEmpty()) {
            log.warn("章节编号为空，跳过该章节");
            return null;
        }

        // 如果有角色数据，序列化为JSON
        if (!plannedChars.isEmpty()) {
            try {
                chapter.put("plannedCharacters", objectMapper.writeValueAsString(plannedChars));
            } catch (Exception e) {
                log.warn("序列化角色数据失败: {}", e.getMessage());
            }
        }

        return chapter;
    }

    /**
     * 解析<ch>角色标签: 提取 <cn>(characterName), <cd>(roleDescription), <ci>(importance)
     */
    private Map<String, Object> parseCharacterTag(Element chElement) {
        Map<String, Object> charData = new HashMap<>();
        charData.put("characterId", null); // 默认null，后续由resolveCharacterIds填充

        org.w3c.dom.NodeList children = chElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();
            String text = child.getTextContent().trim();

            switch (tag) {
                case "cn" -> charData.put("characterName", text);
                case "cd" -> charData.put("roleDescription", text);
                case "ci" -> charData.put("importance", text);
            }
        }

        // 至少要有角色名称
        if (charData.get("characterName") == null || ((String) charData.get("characterName")).isEmpty()) {
            return null;
        }

        return charData;
    }

    /**
     * 角色名称匹配：将解析出的角色名与项目中已有角色进行三级名称匹配
     * 匹配成功设置characterId和roleType，未匹配设置characterId=null且roleType="supporting"
     */
    private String resolveCharacterIds(String plannedCharactersJson, List<NovelCharacter> allCharacters) {
        try {
            List<Map<String, Object>> plannedChars = objectMapper.readValue(plannedCharactersJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            for (Map<String, Object> charData : plannedChars) {
                String characterName = (String) charData.get("characterName");
                Long matchedId = NameMatchUtil.matchByName(allCharacters, characterName, NameMatchUtil.CHARACTER_SUFFIXES);
                if (matchedId != null) {
                    charData.put("characterId", matchedId);
                    // 从匹配到的角色获取roleType
                    for (NovelCharacter c : allCharacters) {
                        if (c.getId().equals(matchedId)) {
                            charData.put("roleType", c.getRoleType());
                            break;
                        }
                    }
                } else {
                    charData.put("characterId", null);
                    charData.put("roleType", "supporting");
                }
            }

            return objectMapper.writeValueAsString(plannedChars);
        } catch (Exception e) {
            log.error("解析角色JSON失败: {}", e.getMessage());
            return plannedCharactersJson;
        }
    }

    /**
     * 保存章节到数据库（重构版：保存plotStage字段 + 角色规划数据）
     * 注意：继续生成模式，不删除已有章节
     */
    private void saveChaptersToDatabase(Long projectId, Long volumeId, String plotStage,
                                       int startChapterNumber, List<Map<String, String>> chaptersList,
                                       List<NovelCharacter> allCharacters) {
        LocalDateTime now = LocalDateTime.now();

        // 继续生成模式：不删除已有章节，直接追加新章节
        log.info("追加保存第{}卷{}阶段的新章节 {} 个", volumeId, plotStage, chaptersList.size());

        // 保存新章节
        int chapterNumber = startChapterNumber;
        for (Map<String, String> chapterData : chaptersList) {
            NovelChapterPlan chapterPlan = new NovelChapterPlan();
            chapterPlan.setProjectId(projectId);
            chapterPlan.setVolumePlanId(volumeId);
            chapterPlan.setChapterNumber(chapterNumber);
            chapterPlan.setChapterTitle(chapterData.getOrDefault("chapterTitle", ""));
            chapterPlan.setPlotOutline(chapterData.getOrDefault("plotOutline", ""));
            chapterPlan.setKeyEvents(chapterData.getOrDefault("keyEvents", ""));
            chapterPlan.setChapterGoal(chapterData.getOrDefault("chapterGoal", ""));
            chapterPlan.setWordCountTarget(Integer.parseInt(chapterData.getOrDefault("wordCountTarget", "3000")));
            chapterPlan.setChapterStartingScene(chapterData.getOrDefault("chapterStartingScene", ""));
            chapterPlan.setChapterEndingScene(chapterData.getOrDefault("chapterEndingScene", ""));
            chapterPlan.setPlotStage(plotStage);
            chapterPlan.setStatus("planned");
            chapterPlan.setCreateTime(now);
            chapterPlan.setUpdateTime(now);

            // 角色规划：名称匹配并持久化
            String plannedCharactersJson = chapterData.get("plannedCharacters");
            if (plannedCharactersJson != null) {
                chapterPlan.setPlannedCharacters(resolveCharacterIds(plannedCharactersJson, allCharacters));
            }

            chapterPlanMapper.insert(chapterPlan);
            chapterNumber++;
        }

        log.info("保存第{}卷{}阶段的新章节 {} 个", volumeId, plotStage, chaptersList.size());
    }

    // ======================== XML Sanitization (duplicated from WorldviewXmlParser) ========================

    /**
     * Sanitize LLM-generated XML to fix common structural issues before DOM parsing.
     * Duplicated from WorldviewXmlParser to avoid creating a shared dependency
     * for this well-isolated ~80-line utility.
     */
    private String sanitizeXmlForDomParsing(String xml, String[] containerTags) {
        // Step 1: Escape illegal XML characters in text nodes
        xml = escapeIllegalXmlChars(xml);

        // Step 2: Fix tag balance
        for (String tag : containerTags) {
            String openTag = "<" + tag + ">";
            String closeTag = "</" + tag + ">";

            int openCount = countTagOccurrences(xml, openTag);
            int closeCount = countTagOccurrences(xml, closeTag);

            if (closeCount == openCount) continue;

            if (closeCount > openCount) {
                // Remove excess closing tags from the end
                int excess = closeCount - openCount;
                int searchFrom = xml.length();
                while (excess > 0 && searchFrom > 0) {
                    int idx = xml.lastIndexOf(closeTag, searchFrom - 1);
                    if (idx < 0) break;
                    xml = xml.substring(0, idx) + xml.substring(idx + closeTag.length());
                    searchFrom = idx;
                    excess--;
                }
                log.debug("XML sanitizer: removed {} excess </{}> tags", closeCount - openCount - excess, tag);
            } else {
                // Add missing closing tags at the end
                int missing = openCount - closeCount;
                StringBuilder suffix = new StringBuilder();
                for (int i = 0; i < missing; i++) {
                    suffix.append(closeTag);
                }
                xml = xml + suffix;
                log.debug("XML sanitizer: added {} missing </{}> tags", missing, tag);
            }
        }
        return xml;
    }

    /**
     * Escape illegal XML characters in text nodes (outside tags and CDATA sections).
     */
    private String escapeIllegalXmlChars(String xml) {
        // Phase 1: Extract and replace CDATA sections with placeholders
        List<String> cdataSections = new ArrayList<>();
        StringBuilder buffer = new StringBuilder(xml);
        String cdataOpen = "<![CDATA[";
        String cdataClose = "]]>";

        int searchStart = 0;
        while (searchStart < buffer.length()) {
            int cStart = buffer.indexOf(cdataOpen, searchStart);
            if (cStart < 0) break;

            int cEnd = buffer.indexOf(cdataClose, cStart + cdataOpen.length());
            if (cEnd < 0) break;

            String cdata = buffer.substring(cStart, cEnd + cdataClose.length());
            cdataSections.add(cdata);

            String placeholder = "\u0000CDATA" + (cdataSections.size() - 1) + "\u0000";
            buffer.replace(cStart, cEnd + cdataClose.length(), placeholder);
            searchStart = cStart + placeholder.length();
        }

        // Phase 2: Escape illegal chars in remaining text
        String result = buffer.toString();
        result = result.replaceAll("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)", "&amp;");
        result = result.replaceAll("<(?![/!a-zA-Z])", "&lt;");

        // Phase 3: Restore CDATA sections
        for (int i = 0; i < cdataSections.size(); i++) {
            String placeholder = "\u0000CDATA" + i + "\u0000";
            result = result.replace(placeholder, cdataSections.get(i));
        }

        return result;
    }

    /**
     * Count non-overlapping occurrences of a substring.
     */
    private int countTagOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
