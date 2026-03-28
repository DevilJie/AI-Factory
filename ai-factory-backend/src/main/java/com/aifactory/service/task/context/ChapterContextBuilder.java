package com.aifactory.service.task.context;

import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 章节连贯性上下文构建器
 *
 * 职责：
 * 1. 构建最近章节的连贯性上下文
 * 2. 格式化章节摘要信息
 * 3. 为AI生成提供上下文参考
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component
public class ChapterContextBuilder {

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    /**
     * 构建最近章节的连贯性上下文
     *
     * @param volumeId 分卷ID
     * @param contextSize 上下文章节数量
     * @return 上下文字符串
     */
    public String buildRecentChaptersContext(Long volumeId, int contextSize) {
        // 查询最近的N章（按章节号倒序）
        List<NovelChapterPlan> recentChapters = chapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .orderByDesc(NovelChapterPlan::getChapterNumber)
                        .last("LIMIT " + contextSize)
        );

        if (recentChapters.isEmpty()) {
            log.info("分卷 {} 暂无章节，不构建上下文", volumeId);
            return "";
        }

        // 反转列表，使章节号按升序排列
        java.util.Collections.reverse(recentChapters);

        StringBuilder context = new StringBuilder();
        context.append("\n【最近章节摘要（用于保持连贯性）】\n");

        for (NovelChapterPlan chapter : recentChapters) {
            context.append(formatChapterSummary(chapter));
        }

        context.append("\n请基于以上章节继续生成，确保情节自然衔接，");
        context.append("保持人物性格和世界观设定的一致性。\n");

        log.info("为分卷 {} 构建了 {} 章的上下文", volumeId, recentChapters.size());
        return context.toString();
    }

    /**
     * 构建最近章节的连贯性上下文（默认使用5章）
     *
     * @param volumeId 分卷ID
     * @return 上下文字符串
     */
    public String buildRecentChaptersContext(Long volumeId) {
        return buildRecentChaptersContext(volumeId, 5);
    }

    /**
     * 格式化单章摘要
     *
     * @param chapter 章节实体
     * @return 格式化后的摘要字符串
     */
    private String formatChapterSummary(NovelChapterPlan chapter) {
        StringBuilder summary = new StringBuilder();

        summary.append(String.format("第%d章：%s\n", chapter.getChapterNumber(), chapter.getChapterTitle()));

        if (chapter.getPlotOutline() != null && !chapter.getPlotOutline().isEmpty()) {
            summary.append(chapter.getPlotOutline()).append("\n");
        }

        if (chapter.getKeyEvents() != null && !chapter.getKeyEvents().isEmpty()) {
            summary.append("关键事件：").append(chapter.getKeyEvents()).append("\n");
        }

        if (chapter.getChapterGoal() != null && !chapter.getChapterGoal().isEmpty()) {
            summary.append("章节目标：").append(chapter.getChapterGoal()).append("\n");
        }

        // 添加章节终点场景，帮助衔接
        if (chapter.getChapterEndingScene() != null && !chapter.getChapterEndingScene().isEmpty()) {
            summary.append("终点场景：").append(chapter.getChapterEndingScene()).append("\n");
        }

        summary.append("\n");

        return summary.toString();
    }

    /**
     * 获取该分卷的章节数量
     *
     * @param volumeId 分卷ID
     * @return 章节数量
     */
    public int getChapterCount(Long volumeId) {
        Long count = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
        );
        return count.intValue();
    }

    /**
     * 获取该分卷的最后N章
     *
     * @param volumeId 分卷ID
     * @param count 章节数量
     * @return 章节列表（按章节号升序）
     */
    public List<NovelChapterPlan> getRecentChapters(Long volumeId, int count) {
        List<NovelChapterPlan> chapters = chapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .orderByDesc(NovelChapterPlan::getChapterNumber)
                        .last("LIMIT " + count)
        );

        // 反转为升序
        java.util.Collections.reverse(chapters);
        return chapters;
    }
}
