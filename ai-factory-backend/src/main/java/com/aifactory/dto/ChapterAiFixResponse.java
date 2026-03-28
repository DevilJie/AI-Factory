package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI剧情修复响应
 */
@Data
@Schema(description = "AI剧情修复响应对象，包含修复后的内容和修复报告")
public class ChapterAiFixResponse {

    /**
     * 修复后的内容
     */
    @Schema(description = "修复后的章节内容", example = "第一章 风云际会\n\n那是一个风雨交加的夜晚，李云站在城门前...")
    private String fixedContent;

    /**
     * 修复报告
     */
    @Schema(description = "修复报告列表，详细记录每一处修改")
    private List<FixReportItem> fixReport;

    /**
     * 修复总数
     */
    @Schema(description = "修复问题总数", example = "5")
    private Integer totalFixes;

    /**
     * 修复说明摘要
     */
    @Schema(description = "修复说明摘要，简要描述本次修复的主要内容", example = "共修复5处问题：修复2处逻辑错误，删除1处重复内容，统一3处人物称呼")
    private String fixSummary;

    /**
     * 修复报告项
     */
    @Data
    @Schema(description = "单个修复报告项，记录一处具体修改")
    public static class FixReportItem {
        /**
         * 问题类型
         */
        @Schema(description = "问题类型",
                example = "logic_error",
                allowableValues = {"logic_error", "redundancy", "character_consistency", "timeline_error", "setting_conflict", "plot_hole"})
        private String type;

        /**
         * 原文
         */
        @Schema(description = "原文内容片段", example = "李云拔出长剑，但他明明手中只有一把短刀。")
        private String original;

        /**
         * 修改后
         */
        @Schema(description = "修改后的内容", example = "李云拔出短刀，刀锋在月光下闪着寒光。")
        private String fixed;

        /**
         * 修改原因
         */
        @Schema(description = "修改原因说明", example = "与前文描述的武器类型不一致")
        private String reason;

        /**
         * 严重程度：high/medium/low
         */
        @Schema(description = "问题严重程度。high(严重): 影响剧情逻辑；medium(中等): 影响阅读体验；low(轻微): 小问题",
                example = "high",
                allowableValues = {"high", "medium", "low"})
        private String severity;
    }
}
