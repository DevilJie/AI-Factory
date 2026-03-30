package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.common.xml.ChapterFixApplyResultXmlDto;
import com.aifactory.common.xml.ChapterFixCheckResultXmlDto;
import com.aifactory.dto.ChapterAiFixResponse;
import com.aifactory.dto.ChapterAiFixResponse.FixReportItem;
import com.aifactory.entity.*;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.ChapterPlotMemoryMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
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
import java.util.stream.Collectors;

/**
 * AI章节修复任务策略
 * 使用提示词模板系统进行章节问题检查和修复
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Slf4j
@Component("chapter_fixTaskStrategy")
public class ChapterFixTaskStrategy implements TaskStrategy {

    private static final String TEMPLATE_CHECK = "llm_chapter_fix_check";
    private static final String TEMPLATE_APPLY = "llm_chapter_fix_apply";

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private ChapterPlotMemoryMapper plotMemoryMapper;

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
    private XmlParser xmlParser;

    @Autowired
    private PowerSystemService powerSystemService;

    @Override
    public String getTaskType() {
        return "chapter_fix";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            JsonNode config = objectMapper.readTree(task.getConfigJson());

            List<StepConfig> steps = new ArrayList<>();

            // 步骤1: 检查章节问题
            Map<String, Object> checkConfig = new HashMap<>();
            checkConfig.put("chapterId", config.get("chapterId"));
            checkConfig.put("content", config.get("content"));
            checkConfig.put("fixOptions", config.get("fixOptions"));
            checkConfig.put("customRequirements", config.get("customRequirements"));
            steps.add(new StepConfig(1, "检查章节问题", "check_issues", checkConfig));

            // 步骤2: 应用修复
            Map<String, Object> fixConfig = new HashMap<>();
            fixConfig.put("chapterId", config.get("chapterId"));
            fixConfig.put("content", config.get("content"));
            fixConfig.put("prevChapterId", config.get("prevChapterId"));
            steps.add(new StepConfig(2, "应用AI修复", "apply_fix", fixConfig));

            // 步骤3: 保存修复结果
            Map<String, Object> saveConfig = new HashMap<>();
            saveConfig.put("chapterId", config.get("chapterId"));
            steps.add(new StepConfig(3, "保存修复结果", "save_result", saveConfig));

            log.info("创建章节修复任务步骤完成，chapterId={}", config.get("chapterId"));

            return steps;

        } catch (Exception e) {
            log.error("创建章节修复任务步骤失败", e);
            throw new RuntimeException("创建章节修复任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        String stepType = step.getStepType();

        switch (stepType) {
            case "check_issues":
                return checkIssues(step, context);
            case "apply_fix":
                return applyFix(step, context);
            case "save_result":
                return saveResult(step, context);
            default:
                return StepResult.failure("未知的步骤类型: " + stepType);
        }
    }

    @Override
    public int calculateProgress(AiTaskStep currentStep, List<AiTaskStep> allSteps) {
        int completedSteps = 0;
        for (AiTaskStep step : allSteps) {
            if ("completed".equals(step.getStatus())) {
                completedSteps++;
            }
        }

        int progress = 0;
        if (completedSteps > 0) {
            progress = 30;
        }
        if (completedSteps > 1) {
            progress = 90;
        }
        if (completedSteps > 2) {
            progress = 100;
        }

        if ("running".equals(currentStep.getStatus()) && currentStep.getProgress() != null) {
            int baseProgress = (currentStep.getStepOrder() - 1) * 30;
            int stepWeight = currentStep.getStepOrder() == 1 ? 30 :
                           currentStep.getStepOrder() == 2 ? 60 : 10;
            progress = baseProgress + (int) (stepWeight * currentStep.getProgress() / 100.0);
        }

        return Math.min(100, progress);
    }

    /**
     * 步骤1: 检查章节问题
     */
    private StepResult checkIssues(AiTaskStep step, TaskContext context) {
        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            Long chapterId = stepConfig.get("chapterId").asLong();
            String content = stepConfig.get("content").asText();

            // 获取章节信息
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter == null) {
                return StepResult.failure("章节不存在");
            }

            // 获取上下文信息
            String prevChapterMemory = getPrevChapterMemory(chapter);
            String worldview = getWorldview(chapter.getProjectId());
            String characterProfiles = getCharacterProfiles(chapter.getProjectId(), content);
            String pendingForeshadowing = getPendingForeshadowing(chapter.getProjectId());

            // 使用模板构建提示词
            Map<String, Object> variables = new HashMap<>();
            variables.put("content", content);
            variables.put("worldview", worldview != null ? worldview : "暂无世界观设定");
            variables.put("prevChapterMemory", prevChapterMemory != null ? prevChapterMemory : "这是第一章，无前置章节");
            variables.put("characterProfiles", characterProfiles != null ? characterProfiles : "暂无人物档案");
            variables.put("pendingForeshadowing", pendingForeshadowing != null ? pendingForeshadowing : "暂无待回收伏笔");

            String prompt = promptTemplateService.executeTemplate(TEMPLATE_CHECK, variables);

            // 调用AI检查
            var aiRequest = new com.aifactory.dto.AIGenerateRequest();
            aiRequest.setProjectId(chapter.getProjectId());
            aiRequest.setVolumePlanId(chapter.getVolumePlanId());
            aiRequest.setChapterPlanId(chapter.getChapterPlanId());
            aiRequest.setChapterId(chapter.getId());
            aiRequest.setRequestType(TEMPLATE_CHECK);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(4000);
            aiRequest.setTemperature(0.3);

            var aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("AI检查完成，响应长度: {}", responseContent.length());

            // 将检查结果保存到上下文
            context.putSharedData("checkResponse", responseContent);

            // 尝试解析检查结果
            try {
                ChapterFixCheckResultXmlDto checkResult = xmlParser.parse(responseContent, ChapterFixCheckResultXmlDto.class);
                int issueCount = checkResult.getTotalIssues() != null ? checkResult.getTotalIssues() : 0;
                log.info("解析检查结果成功，发现问题数: {}", issueCount);
            } catch (Exception e) {
                log.warn("解析检查结果失败，但继续执行: {}", e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("issuesFound", true);

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("检查章节问题失败", e);
            return StepResult.failure("检查章节问题失败: " + e.getMessage());
        }
    }

    /**
     * 步骤2: 应用AI修复
     */
    private StepResult applyFix(AiTaskStep step, TaskContext context) {
        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            Long chapterId = stepConfig.get("chapterId").asLong();
            String content = stepConfig.get("content").asText();
            String checkResponse = (String) context.getSharedData("checkResponse");

            if (checkResponse == null || checkResponse.isEmpty()) {
                return StepResult.failure("未找到检查结果");
            }

            // 获取章节信息
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter == null) {
                return StepResult.failure("章节不存在");
            }

            // 获取上下文信息
            String worldview = getWorldview(chapter.getProjectId());
            String characterProfiles = getCharacterProfiles(chapter.getProjectId(), content);

            // 使用模板构建提示词
            Map<String, Object> variables = new HashMap<>();
            variables.put("content", content);
            variables.put("checkResult", checkResponse);
            variables.put("worldview", worldview != null ? worldview : "暂无世界观设定");
            variables.put("characterProfiles", characterProfiles != null ? characterProfiles : "暂无人物档案");

            String prompt = promptTemplateService.executeTemplate(TEMPLATE_APPLY, variables);

            // 调用AI修复
            var aiRequest = new com.aifactory.dto.AIGenerateRequest();
            aiRequest.setProjectId(chapter.getProjectId());
            aiRequest.setVolumePlanId(chapter.getVolumePlanId());
            aiRequest.setChapterPlanId(chapter.getChapterPlanId());
            aiRequest.setChapterId(chapter.getId());
            aiRequest.setRequestType(TEMPLATE_APPLY);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(8000);
            aiRequest.setTemperature(0.3);

            var aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("AI修复完成，响应长度: {}", responseContent.length());

            // 解析修复结果
            ChapterAiFixResponse fixResponse = parseFixResponse(responseContent, content);

            // 将修复结果保存到上下文
            context.putSharedData("fixResponse", fixResponse);
            context.putSharedData("fixedContent", fixResponse.getFixedContent());

            Map<String, Object> result = new HashMap<>();
            result.put("totalFixes", fixResponse.getTotalFixes());
            result.put("fixSummary", fixResponse.getFixSummary());

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("应用AI修复失败", e);
            return StepResult.failure("应用AI修复失败: " + e.getMessage());
        }
    }

    /**
     * 步骤3: 保存修复结果
     */
    private StepResult saveResult(AiTaskStep step, TaskContext context) {
        try {
            JsonNode stepConfig = objectMapper.readTree(step.getConfigJson());
            Long chapterId = stepConfig.get("chapterId").asLong();

            String fixedContent = (String) context.getSharedData("fixedContent");
            if (fixedContent == null || fixedContent.isEmpty()) {
                return StepResult.failure("未找到修复后的内容");
            }

            // 更新章节内容
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter == null) {
                return StepResult.failure("章节不存在");
            }

            chapter.setContent(fixedContent);
            chapter.setWordCount(fixedContent.length());
            chapter.setUpdateTime(LocalDateTime.now());
            chapterMapper.updateById(chapter);

            log.info("章节修复结果已保存，chapterId={}", chapterId);

            // 获取完整修复响应并返回给前端
            ChapterAiFixResponse fixResponse = (ChapterAiFixResponse) context.getSharedData("fixResponse");

            Map<String, Object> result = new HashMap<>();
            result.put("chapterId", chapterId);
            result.put("message", "章节修复完成");
            result.put("fixedContent", fixedContent);
            if (fixResponse != null) {
                result.put("totalFixes", fixResponse.getTotalFixes());
                result.put("fixSummary", fixResponse.getFixSummary());
                result.put("fixReport", fixResponse.getFixReport());
            }

            return StepResult.success(result, 100);

        } catch (Exception e) {
            log.error("保存修复结果失败", e);
            return StepResult.failure("保存修复结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取前一章的结构化记忆
     */
    private String getPrevChapterMemory(Chapter chapter) {
        if (chapter.getChapterNumber() == null || chapter.getChapterNumber() <= 1) {
            return null;
        }

        // 查找前一章的记忆
        Chapter prevChapter = chapterMapper.selectOne(
            new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, chapter.getProjectId())
                .eq(Chapter::getChapterNumber, chapter.getChapterNumber() - 1)
        );

        if (prevChapter == null) {
            return null;
        }

        ChapterPlotMemory prevMemory = plotMemoryMapper.selectOne(
            new LambdaQueryWrapper<ChapterPlotMemory>()
                .eq(ChapterPlotMemory::getChapterId, prevChapter.getId())
        );

        if (prevMemory == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (prevMemory.getKeyEvents() != null && !prevMemory.getKeyEvents().isEmpty()) {
            sb.append("**关键事件**：\n").append(prevMemory.getKeyEvents()).append("\n\n");
        }
        if (prevMemory.getCharacterStatus() != null && !prevMemory.getCharacterStatus().isEmpty()) {
            sb.append("**人物状态**：\n").append(prevMemory.getCharacterStatus()).append("\n\n");
        }
        if (prevMemory.getNewSettings() != null && !prevMemory.getNewSettings().isEmpty()) {
            sb.append("**新增设定**：\n").append(prevMemory.getNewSettings()).append("\n\n");
        }
        if (prevMemory.getChapterEndingScene() != null && !prevMemory.getChapterEndingScene().isEmpty()) {
            sb.append("**前一章结尾场景**：\n").append(prevMemory.getChapterEndingScene()).append("\n\n");
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 获取世界观设定
     */
    private String getWorldview(Long projectId) {
        NovelWorldview worldview = worldviewMapper.selectOne(
            new LambdaQueryWrapper<NovelWorldview>()
                .eq(NovelWorldview::getProjectId, projectId)
        );

        if (worldview == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (worldview.getWorldType() != null && !worldview.getWorldType().isEmpty()) {
            sb.append("**世界类型**：").append(worldview.getWorldType()).append("\n");
        }
        if (worldview.getWorldBackground() != null && !worldview.getWorldBackground().isEmpty()) {
            sb.append("**世界背景**：").append(worldview.getWorldBackground()).append("\n");
        }
        String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
        if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
            sb.append("**力量体系**：").append(powerConstraint).append("\n");
        }
        if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
            sb.append("**地理环境**：").append(worldview.getGeography()).append("\n");
        }
        if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
            sb.append("**势力分布**：").append(worldview.getForces()).append("\n");
        }
        if (worldview.getTimeline() != null && !worldview.getTimeline().isEmpty()) {
            sb.append("**时间线**：").append(worldview.getTimeline()).append("\n");
        }
        if (worldview.getRules() != null && !worldview.getRules().isEmpty()) {
            sb.append("**世界规则**：").append(worldview.getRules()).append("\n");
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 获取本章涉及的人物档案
     */
    private String getCharacterProfiles(Long projectId, String chapterContent) {
        // TODO: 实现人物档案获取逻辑
        // 1. 从章节内容中识别人物名
        // 2. 从数据库中获取这些人物的档案
        // 目前返回null，后续可以实现
        return null;
    }

    /**
     * 获取待回收的伏笔列表
     */
    private String getPendingForeshadowing(Long projectId) {
        // TODO: 实现伏笔获取逻辑
        // 1. 从数据库中查询状态为"pending"的伏笔
        // 2. 格式化返回
        // 目前返回null，后续可以实现
        return null;
    }

    /**
     * 解析修复响应（使用新的XML DTO）
     */
    private ChapterAiFixResponse parseFixResponse(String response, String originalContent) {
        try {
            // 尝试使用XML解析器解析
            ChapterFixApplyResultXmlDto xmlResult = xmlParser.parse(response, ChapterFixApplyResultXmlDto.class);

            ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();
            fixResponse.setFixedContent(xmlResult.getFixedContent() != null ? xmlResult.getFixedContent() : originalContent);
            fixResponse.setFixSummary(xmlResult.getSummary() != null ? xmlResult.getSummary() : "修复完成");
            fixResponse.setTotalFixes(xmlResult.getTotalFixes() != null ? xmlResult.getTotalFixes() : 0);

            // 转换修复报告
            List<FixReportItem> fixReport = new ArrayList<>();
            if (xmlResult.getFixReport() != null) {
                for (ChapterFixApplyResultXmlDto.FixItem item : xmlResult.getFixReport()) {
                    FixReportItem reportItem = new FixReportItem();
                    reportItem.setType(item.getType());
                    reportItem.setSeverity(item.getSeverity());
                    reportItem.setOriginal(item.getOriginal());
                    reportItem.setFixed(item.getFixed());
                    reportItem.setReason(item.getExplanation());
                    fixReport.add(reportItem);
                }
            }
            fixResponse.setFixReport(fixReport);

            log.info("XML解析修复结果成功，修复数量: {}", fixResponse.getTotalFixes());
            return fixResponse;

        } catch (Exception e) {
            log.warn("XML解析修复结果失败，尝试兼容解析: {}", e.getMessage());

            // 降级到兼容解析（支持旧格式）
            return parseFixResponseLegacy(response, originalContent);
        }
    }

    /**
     * 兼容旧格式的解析方法
     */
    private ChapterAiFixResponse parseFixResponseLegacy(String response, String originalContent) {
        try {
            String fixedContent = null;
            String fixSummary = null;
            int totalFixes = 0;

            // 尝试提取新格式 <C> 标签
            if (response.contains("<C>")) {
                int start = response.indexOf("<C>") + 3;
                int end = response.indexOf("</C>");
                if (end > start) {
                    String content = response.substring(start, end);
                    fixedContent = extractCData(content);
                }
            }
            // 兼容旧格式 <fixedContent> 标签
            else if (response.contains("<fixedContent>")) {
                int start = response.indexOf("<fixedContent>") + "<fixedContent>".length();
                int end = response.indexOf("</fixedContent>");
                if (end > start) {
                    String content = response.substring(start, end);
                    fixedContent = extractCData(content);
                }
            }

            // 尝试提取新格式 <SUM> 标签
            if (response.contains("<SUM>")) {
                int start = response.indexOf("<SUM>") + 5;
                int end = response.indexOf("</SUM>");
                if (end > start) {
                    String content = response.substring(start, end);
                    fixSummary = extractCData(content);
                }
            }
            // 兼容旧格式 <fixSummary> 标签
            else if (response.contains("<fixSummary>")) {
                int start = response.indexOf("<fixSummary>") + "<fixSummary>".length();
                int end = response.indexOf("</fixSummary>");
                if (end > start) {
                    String content = response.substring(start, end);
                    fixSummary = extractCData(content);
                }
            }

            // 尝试提取新格式 <N> 标签
            if (response.contains("<N>")) {
                int start = response.indexOf("<N>") + 3;
                int end = response.indexOf("</N>");
                if (end > start) {
                    totalFixes = Integer.parseInt(response.substring(start, end).trim());
                }
            }
            // 兼容旧格式 <totalFixes> 标签
            else if (response.contains("<totalFixes>")) {
                int start = response.indexOf("<totalFixes>") + "<totalFixes>".length();
                int end = response.indexOf("</totalFixes>");
                if (end > start) {
                    totalFixes = Integer.parseInt(response.substring(start, end).trim());
                }
            }

            ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();
            fixResponse.setFixedContent(fixedContent != null ? fixedContent : originalContent);
            fixResponse.setFixSummary(fixSummary != null ? fixSummary : "修复完成");
            fixResponse.setTotalFixes(totalFixes);
            fixResponse.setFixReport(new ArrayList<>());

            return fixResponse;

        } catch (Exception e) {
            log.error("兼容解析也失败，返回原内容", e);
            ChapterAiFixResponse errorResponse = new ChapterAiFixResponse();
            errorResponse.setFixedContent(originalContent);
            errorResponse.setFixSummary("解析失败，返回原内容");
            errorResponse.setTotalFixes(0);
            errorResponse.setFixReport(new ArrayList<>());
            return errorResponse;
        }
    }

    /**
     * 提取CDATA内容
     */
    private String extractCData(String content) {
        if (content.contains("<![CDATA[")) {
            int start = content.indexOf("<![CDATA[") + "<![CDATA[".length();
            int end = content.indexOf("]]>");
            if (end > start) {
                return content.substring(start, end);
            }
        }
        return content.trim();
    }
}
