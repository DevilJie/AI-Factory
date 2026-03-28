package com.aifactory.service.task.validator;

import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 章节情节阶段验证器
 *
 * 职责：
 * 1. 验证请求的阶段是否合法（不能回溯，不能跳跃）
 * 2. 计算下一个章节编号
 * 3. 提供阶段相关的辅助方法
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component
public class ChapterPlotStageValidator {

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    /**
     * 验证请求的阶段是否合法
     *
     * @param volumeId 分卷ID
     * @param requestedStage 请求的阶段
     * @return 验证结果
     */
    public ValidationResult validateStageRequest(Long volumeId, String requestedStage) {
        log.info("验证阶段请求：volumeId={}, requestedStage={}", volumeId, requestedStage);

        // 查询该分卷的已有章节
        List<NovelChapterPlan> existingChapters = chapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .orderByDesc(NovelChapterPlan::getChapterNumber)
        );

        if (existingChapters.isEmpty()) {
            // 首次生成，只允许从introduction开始
            if (!"introduction".equals(requestedStage)) {
                return ValidationResult.failure("首次生成必须从起始阶段(introduction)开始");
            }
            return ValidationResult.success();
        }

        // 确定当前最大情节阶段
        String maxStage = existingChapters.get(0).getPlotStage();
        if (maxStage == null || maxStage.isEmpty()) {
            // 兼容旧数据，如果没有plot_stage，默认为introduction
            maxStage = "introduction";
        }

        int currentOrdinal = getStageOrdinal(maxStage);
        int requestedOrdinal = getStageOrdinal(requestedStage);

        // 验证不能回溯
        if (requestedOrdinal < currentOrdinal) {
            return ValidationResult.failure(
                    String.format("不能回溯生成已完成的阶段，当前最大阶段：%s，请求阶段：%s",
                            getStageDisplayName(maxStage), getStageDisplayName(requestedStage))
            );
        }

        // 验证不能跳跃
        if (requestedOrdinal > currentOrdinal + 1) {
            String nextStageName = getStageDisplayName(getNextStageName(maxStage));
            return ValidationResult.failure(
                    String.format("不能跳过阶段，请先生成：%s", nextStageName)
            );
        }

        log.info("阶段验证通过：current={}, requested={}", maxStage, requestedStage);
        return ValidationResult.success();
    }

    /**
     * 计算下一个章节编号
     *
     * @param volumeId 分卷ID
     * @return 下一个章节编号
     */
    public int calculateNextChapterNumber(Long volumeId) {
        List<NovelChapterPlan> existingChapters = chapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .orderByDesc(NovelChapterPlan::getChapterNumber)
                        .last("LIMIT 1")
        );

        if (existingChapters.isEmpty()) {
            return 1;
        }

        Integer maxChapterNumber = existingChapters.get(0).getChapterNumber();
        return maxChapterNumber + 1;
    }

    /**
     * 获取阶段的序号（用于比较）
     *
     * @param stage 阶段标识
     * @return 阶段序号（1-4）
     */
    public int getStageOrdinal(String stage) {
        return switch (stage) {
            case "introduction" -> 1;
            case "development" -> 2;
            case "climax" -> 3;
            case "conclusion" -> 4;
            default -> 0;
        };
    }

    /**
     * 获取阶段的显示名称
     *
     * @param stage 阶段标识
     * @return 阶段显示名称
     */
    public String getStageDisplayName(String stage) {
        return switch (stage) {
            case "introduction" -> "起始阶段";
            case "development" -> "发展阶段";
            case "climax" -> "高潮阶段";
            case "conclusion" -> "结局阶段";
            default -> "未知阶段";
        };
    }

    /**
     * 获取下一个阶段的名称
     *
     * @param currentStage 当前阶段
     * @return 下一个阶段名称，如果已是最后阶段则返回null
     */
    public String getNextStageName(String currentStage) {
        return switch (currentStage) {
            case "introduction" -> "development";
            case "development" -> "climax";
            case "climax" -> "conclusion";
            case "conclusion" -> null; // 已是最后阶段
            default -> "introduction";
        };
    }

    /**
     * 获取某阶段在该分卷中已生成的章节数
     *
     * @param volumeId 分卷ID
     * @param stage 阶段标识
     * @return 该阶段的章节数
     */
    public int getChapterCountInStage(Long volumeId, String stage) {
        Long count = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .eq(NovelChapterPlan::getPlotStage, stage)
        );
        return count.intValue();
    }

    /**
     * 获取该分卷当前最大阶段
     *
     * @param volumeId 分卷ID
     * @return 当前最大阶段，如果没有章节则返回null
     */
    public String getCurrentMaxStage(Long volumeId) {
        List<NovelChapterPlan> chapters = chapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                        .orderByDesc(NovelChapterPlan::getChapterNumber)
                        .last("LIMIT 1")
        );

        if (chapters.isEmpty()) {
            return null;
        }

        String stage = chapters.get(0).getPlotStage();
        return stage != null && !stage.isEmpty() ? stage : "introduction";
    }

    /**
     * 验证是否可以标记阶段为完成
     *
     * @param volumeId 分卷ID
     * @param stageKey 阶段标识
     * @return 验证结果
     */
    public ValidationResult canMarkStageCompleted(Long volumeId, String stageKey) {
        // 检查该阶段是否有章节
        Long chapterCount = chapterPlanMapper.selectCount(
            new LambdaQueryWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                .eq(NovelChapterPlan::getPlotStage, stageKey)
        );

        if (chapterCount == 0) {
            return ValidationResult.failure("该阶段还没有章节，不能标记为完成");
        }

        // 检查该阶段是否已经完成
        Long completedCount = chapterPlanMapper.selectCount(
            new LambdaQueryWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                .eq(NovelChapterPlan::getPlotStage, stageKey)
                .eq(NovelChapterPlan::getStageCompleted, true)
        );

        if (completedCount > 0) {
            return ValidationResult.failure("该阶段已经标记为完成");
        }

        return ValidationResult.success();
    }

    /**
     * 验证是否可以在已完成阶段生成新章节
     *
     * @param volumeId 分卷ID
     * @param stageKey 阶段标识
     * @return 验证结果
     */
    public ValidationResult canGenerateInCompletedStage(Long volumeId, String stageKey) {
        int currentOrdinal = getStageOrdinal(stageKey);

        // 检查下一阶段是否有章节（如果有，说明已进入下一阶段，不能回溯）
        if (currentOrdinal < 4) { // 不是最后一个阶段
            String nextStage = getStageByOrdinal(currentOrdinal + 1);
            Long nextChapterCount = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                    .eq(NovelChapterPlan::getPlotStage, nextStage)
            );

            if (nextChapterCount > 0) {
                return ValidationResult.failure("下一阶段已有章节，不能回溯编辑当前阶段");
            }
        }

        return ValidationResult.success();
    }

    /**
     * 根据序号获取阶段名称
     *
     * @param ordinal 阶段序号（1-4）
     * @return 阶段名称
     */
    public String getStageByOrdinal(int ordinal) {
        return switch (ordinal) {
            case 1 -> "introduction";
            case 2 -> "development";
            case 3 -> "climax";
            case 4 -> "conclusion";
            default -> "introduction";
        };
    }

    /**
     * 验证结果
     */
    @Data
    public static class ValidationResult {
        private final boolean valid;
        private final String error;

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, error);
        }
    }
}
