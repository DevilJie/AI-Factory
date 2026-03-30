package com.aifactory.service.chapter.prompt;

import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 章节生成提示词构建器
 *
 * 职责：
 * - 根据章节规划构建完整的提示词
 * - 整合世界观、分卷信息、前置章节上下文
 * - 生成XML格式返回要求
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component
public class ChapterPromptBuilder {

    @Autowired
    private com.aifactory.service.PowerSystemService powerSystemService;

    /**
     * 构建单章节生成提示词
     *
     * @param chapterPlan 章节规划
     * @param volumePlan 分卷规划
     * @param worldview 世界观设定
     * @param recentChapters 前置章节上下文（最近5章）
     * @param projectDescription 项目描述
     * @param storyTone 故事基调
     * @param storyGenre 故事类型
     * @return 完整提示词
     */
    public String buildPrompt(
        NovelChapterPlan chapterPlan,
        NovelVolumePlan volumePlan,
        NovelWorldview worldview,
        List<Chapter> recentChapters,
        String projectDescription,
        String storyTone,
        String storyGenre
    ) {
        StringBuilder prompt = new StringBuilder();

        // 1. 角色定义
        prompt.append("你是资深网文作家。\n\n");

        // 2. 【CRITICAL】字数硬性约束
        appendWordCountRequirement(prompt, chapterPlan);

        // 3. 任务
        appendTaskDefinition(prompt, chapterPlan, volumePlan);

        // 4. 情节阶段
        prompt.append(getStageDescription(chapterPlan.getPlotStage())).append("\n");

        // 5. 分卷信息
        prompt.append(buildVolumeInfo(volumePlan));

        // 6. 世界观
        if (worldview != null) {
            prompt.append(buildWorldviewInfo(worldview));
        }

        // 7. 章节规划
        prompt.append(buildChapterPlanInfo(chapterPlan));

        // 8. 前置章节
        if (recentChapters != null && !recentChapters.isEmpty()) {
            prompt.append(buildContinuityContext(recentChapters));
        }

        // 9. 内容要求
        appendContentRequirements(prompt, chapterPlan);

        // 10. XML格式
        appendXmlFormatRequirements(prompt, chapterPlan);

        log.debug("构建提示词完成，章节规划ID: {}", chapterPlan.getId());

        return prompt.toString();
    }

    /**
     * 字数硬性约束（放在提示词最前面，强调重要性）
     */
    private void appendWordCountRequirement(StringBuilder prompt, NovelChapterPlan chapterPlan) {
        int targetWordCount = chapterPlan.getWordCountTarget() != null ?
            chapterPlan.getWordCountTarget() : 3000;

        prompt.append("【CRITICAL: 字数硬性约束 - MUST READ】\n");
        prompt.append("REQUIRED: 章节正文MUST控制在").append(targetWordCount).append("字以内\n");
        prompt.append("MAXIMUM: ").append((int)(targetWordCount * 1.05)).append("字（严格上限，不可突破）\n");
        prompt.append("FAIL: 超出上限将被系统截断，导致章节结尾缺失\n\n");
    }

    /**
     * 内容要求（精简版）
     */
    private void appendContentRequirements(StringBuilder prompt, NovelChapterPlan chapterPlan) {
        int targetWordCount = chapterPlan.getWordCountTarget() != null ?
            chapterPlan.getWordCountTarget() : 3000;

        prompt.append("【内容要求 - MUST FOLLOW】\n");
        prompt.append("1. 从起点写到终点，MUST有完整结尾\n");
        prompt.append("2. 遵循章节规划，主线优先，次要情节可省略\n");
        prompt.append("3. 目标字数: ").append(targetWordCount).append("字，控制在")
              .append((int)(targetWordCount * 0.9)).append("-").append((int)(targetWordCount * 1.05))
              .append("字范围内\n");
        prompt.append("4. 超出上限会被强制截断，导致结尾缺失\n\n");
    }

    /**
     * 添加任务定义
     */
    private void appendTaskDefinition(StringBuilder prompt, NovelChapterPlan chapterPlan, NovelVolumePlan volumePlan) {
        prompt.append("【任务】创作第").append(volumePlan.getVolumeNumber()).append("卷")
              .append("第").append(chapterPlan.getChapterNumber()).append("章")
              .append("《").append(chapterPlan.getChapterTitle()).append("》\n\n");
    }

    /**
     * 构建分卷信息字符串（精简版）
     */
    private String buildVolumeInfo(NovelVolumePlan volumePlan) {
        return String.format("【分卷】第%d卷 %s - %s\n",
            volumePlan.getVolumeNumber(),
            volumePlan.getVolumeTitle(),
            volumePlan.getVolumeTheme()
        );
    }

    /**
     * 构建世界观信息（精简版）
     */
    private String buildWorldviewInfo(NovelWorldview worldview) {
        StringBuilder sb = new StringBuilder();
        sb.append("【世界观】").append(worldview.getWorldType());

        String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
        if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
            sb.append(" | 力量: ").append(powerConstraint);
        }
        if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
            sb.append(" | 地理: ").append(worldview.getGeography());
        }
        if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
            sb.append(" | 势力: ").append(worldview.getForces());
        }

        sb.append("\n\n");
        return sb.toString();
    }

    /**
     * 构建章节规划信息（精简版）
     */
    private String buildChapterPlanInfo(NovelChapterPlan chapterPlan) {
        StringBuilder sb = new StringBuilder();
        sb.append("【章节规划】\n");
        sb.append("标题: ").append(chapterPlan.getChapterTitle()).append("\n");

        if (chapterPlan.getPlotOutline() != null && !chapterPlan.getPlotOutline().isEmpty()) {
            sb.append("大纲: ").append(chapterPlan.getPlotOutline()).append("\n");
        }

        if (chapterPlan.getChapterStartingScene() != null && !chapterPlan.getChapterStartingScene().isEmpty()) {
            sb.append("起点: ").append(chapterPlan.getChapterStartingScene()).append("\n");
        }

        if (chapterPlan.getChapterEndingScene() != null && !chapterPlan.getChapterEndingScene().isEmpty()) {
            sb.append("终点: ").append(chapterPlan.getChapterEndingScene()).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 构建连贯性上下文（前置章节）
     */
    private String buildContinuityContext(List<Chapter> recentChapters) {
        StringBuilder sb = new StringBuilder();
        sb.append("【前置章节】\n");

        for (Chapter chapter : recentChapters) {
            sb.append(String.format("第%d章 %s: ", chapter.getChapterNumber(), chapter.getTitle()));

            if (chapter.getPlotSummary() != null && !chapter.getPlotSummary().isEmpty()) {
                sb.append(chapter.getPlotSummary());
            }

            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 添加XML返回格式要求
     */
    private void appendXmlFormatRequirements(StringBuilder prompt, NovelChapterPlan chapterPlan) {
        int targetWordCount = chapterPlan.getWordCountTarget() != null ?
            chapterPlan.getWordCountTarget() : 3000;
        int maxWords = (int)(targetWordCount * 1.05);

        prompt.append("【输出格式 - MUST STRICTLY FOLLOW】\n");
        prompt.append("仅返回XML，无其他内容：\n\n");
        prompt.append("<chap>\n");
        prompt.append("  <t><![CDATA[章节标题]]></t>\n");
        prompt.append("  <c><![CDATA[章节正文（MUST: ")
              .append(targetWordCount).append("-").append(maxWords)
              .append("字，有完整结尾，否则被截断）]]></c>\n");
        prompt.append("  <p><![CDATA[剧情概要（100-200字）]]></p>\n");
        prompt.append("  <w>").append(targetWordCount).append("</w>\n");
        prompt.append("</chap>\n");
    }

    /**
     * 获取情节阶段描述（精简版）
     */
    private String getStageDescription(String plotStage) {
        return switch (plotStage) {
            case "introduction" -> """
                    【情节阶段: 起始】
                    建立世界观、主角登场、铺垫矛盾
                    """;

            case "development" -> """
                    【情节阶段: 发展】
                    矛盾升级、主角成长、埋设伏笔
                    """;

            case "climax" -> """
                    【情节阶段: 高潮】
                    矛盾爆发、伏笔回收、主角蜕变
                    """;

            case "conclusion" -> """
                    【情节阶段: 结局】
                    解决矛盾、回收伏笔、角色成长
                    """;

            default -> "";
        };
    }
}
