package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI问题检查响应（第一阶段：只检查，不修改）
 */
@Data
@Schema(description = "AI问题检查响应对象，用于返回检查发现的问题列表，不进行修改")
public class ChapterIssueCheckResponse {

    /**
     * 发现的问题列表
     */
    @Schema(description = "发现的问题列表，包含所有检测到的问题详情")
    private List<IssueItem> issues;

    /**
     * 检查摘要
     */
    @Schema(description = "检查摘要，简要描述本次检查的结果概况",
            example = "共发现5个问题：2个严重问题需要关注，3个轻微问题可选择性修复")
    private String checkSummary;

    /**
     * 问题总数
     */
    @Schema(description = "问题总数", example = "5")
    private Integer totalIssues;

    /**
     * 是否有严重问题
     */
    @Schema(description = "是否存在严重问题（severity=high）。true表示有严重问题需要优先处理",
            example = "true")
    private Boolean hasSevereIssues;

    /**
     * 问题项
     */
    @Data
    @Schema(description = "单个问题项，描述一个具体的问题")
    public static class IssueItem {
        /**
         * 问题类型
         */
        @Schema(description = "问题类型",
                example = "logic_error",
                allowableValues = {"logic_error", "redundancy", "character_consistency",
                        "timeline_error", "setting_conflict", "plot_hole", "dialogue_issue", "pacing_issue"})
        private String type;

        /**
         * 严重程度：high/medium/low
         */
        @Schema(description = "问题严重程度。" +
                "high(严重): 影响剧情逻辑或造成明显矛盾，必须修复；" +
                "medium(中等): 影响阅读体验，建议修复；" +
                "low(轻微): 小问题，可选择性修复",
                example = "high",
                allowableValues = {"high", "medium", "low"})
        private String severity;

        /**
         * 问题位置描述（如"第3段"、"第5段第2句"）
         */
        @Schema(description = "问题位置描述，方便定位问题所在",
                example = "第3段第2句")
        private String location;

        /**
         * 发现的问题原文片段
         */
        @Schema(description = "问题原文片段，截取包含问题的文本",
                example = "李云拔出长剑，但他明明手中只有一把短刀。")
        private String originalText;

        /**
         * 问题描述
         */
        @Schema(description = "问题描述，详细说明问题所在",
                example = "武器类型与前文不一致，第一章明确提到主角使用的是短刀")
        private String description;

        /**
         * 建议的修改方向（不要求具体文本，只是方向）
         */
        @Schema(description = "建议的修改方向，提供修复思路而非具体文本",
                example = "将【长剑】改为【短刀】，或修改相关动作描写以匹配短刀的特性")
        private String suggestion;
    }
}
