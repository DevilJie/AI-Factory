package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI剧情修复请求
 */
@Data
@Schema(description = "AI剧情修复请求对象，用于指定修复选项和自定义要求")
public class ChapterAiFixRequest {

    /**
     * 当前章节内容
     */
    @Schema(description = "当前章节内容，可选。不传入时自动从数据库获取指定章节的内容", example = "第一章 风云际会\n\n那是一个风雨交加的夜晚...")
    private String content;

    /**
     * 前一章ID（可选）
     */
    @Schema(description = "前一章ID，用于获取上下文进行连贯性检查。不传入则不检查与前章的衔接问题", example = "1")
    private Long prevChapterId;

    /**
     * 后一章ID（可选）
     */
    @Schema(description = "后一章ID，用于获取上下文进行连贯性检查。不传入则不检查与后章的衔接问题", example = "3")
    private Long nextChapterId;

    /**
     * 修复选项
     */
    @Schema(description = "修复选项列表，指定要修复的问题类型。可选值：" +
            "logic_error(逻辑错误)、redundancy(重复内容)、character_consistency(人物设定一致性)、" +
            "timeline_error(时间线错误)、setting_conflict(设定冲突)、plot_hole(剧情漏洞)。" +
            "不传入则检查所有类型",
            example = "[\"logic_error\", \"character_consistency\"]")
    private List<String> fixOptions;

    /**
     * 自定义修复要求
     */
    @Schema(description = "自定义修复要求，用自然语言描述额外的修复需求。AI会根据此要求进行针对性修复",
            example = "注意保持主角冷静理智的性格特点，不要让他说出冲动的话")
    private String customRequirements;
}
