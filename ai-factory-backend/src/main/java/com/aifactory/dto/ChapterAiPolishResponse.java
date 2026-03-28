package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * AI润色响应
 */
@Data
@Schema(description = "AI润色响应对象，包含润色后的内容和润色报告")
public class ChapterAiPolishResponse {

    /**
     * 润色后的内容
     */
    @Schema(description = "润色后的章节内容",
            example = "第一章 风云际会\n\n那是一个风雨交加的夜晚，狂风呼啸，电闪雷鸣。李云静静地站在城门前，衣袂飘飘...")
    private String polishedContent;

    /**
     * 润色报告
     */
    @Schema(description = "润色报告，统计各类优化的数量")
    private PolishReport polishReport;

    /**
     * 润色说明摘要
     */
    @Schema(description = "润色说明摘要，简要描述本次润色的主要改进",
            example = "共优化35处：增强环境描写12处，优化对话8处，调整叙事节奏10处，提升用词5处")
    private String polishSummary;

    /**
     * 润色报告
     */
    @Data
    @Schema(description = "润色报告详情，统计各类优化的数量")
    public static class PolishReport {
        /**
         * 描写优化数量
         */
        @Schema(description = "描写优化数量，包括环境描写、动作描写、心理描写等增强",
                example = "12")
        private Integer descriptionImprovements;

        /**
         * 对话优化数量
         */
        @Schema(description = "对话优化数量，包括对话语言优化、对话节奏调整等",
                example = "8")
        private Integer dialogueOptimizations;

        /**
         * 节奏调整数量
         */
        @Schema(description = "节奏调整数量，包括删减冗余、调整叙事节奏等",
                example = "10")
        private Integer pacingAdjustments;

        /**
         * 用词优化数量
         */
        @Schema(description = "用词优化数量，包括替换平淡词汇、增强表达准确性等",
                example = "5")
        private Integer wordOptimizations;

        /**
         * 句式优化数量
         */
        @Schema(description = "句式优化数量，包括调整句式结构、增强语言节奏感等",
                example = "8")
        private Integer sentenceOptimizations;

        /**
         * 总计优化数量
         */
        @Schema(description = "总计优化数量，以上各类优化的总和",
                example = "43")
        private Integer totalOptimizations;
    }
}
