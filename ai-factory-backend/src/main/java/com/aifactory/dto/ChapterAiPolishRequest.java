package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI润色请求
 */
@Data
@Schema(description = "AI润色请求对象，用于指定润色风格、程度和自定义要求")
public class ChapterAiPolishRequest {

    /**
     * 当前章节内容
     */
    @Schema(description = "当前章节内容，可选。不传入时自动从数据库获取指定章节的内容",
            example = "第一章 风云际会\n\n那是一个风雨交加的夜晚，李云站在城门前...")
    private String content;

    /**
     * 润色风格：vivid-细腻描写型, fast-紧凑节奏型, literary-文学优化型
     */
    @Schema(description = "润色风格。" +
            "vivid(细腻描写型): 增强环境描写、动作细节、感官描写；" +
            "fast(紧凑节奏型): 删减冗余描写，加快叙事节奏，增强紧张感；" +
            "literary(文学优化型): 提升语言文学性，优化修辞手法，增强艺术感染力",
            example = "vivid",
            allowableValues = {"vivid", "fast", "literary"})
    private String style;

    /**
     * 润色程度：light-轻度, medium-中度, heavy-深度
     */
    @Schema(description = "润色程度。" +
            "light(轻度): 仅做小幅调整，保留原文风格，约10%改动；" +
            "medium(中度): 适度优化，平衡原意与提升，约30%改动；" +
            "heavy(深度): 大幅改写，全面优化语言和结构，约50%以上改动",
            example = "medium",
            allowableValues = {"light", "medium", "heavy"})
    private String polishLevel;

    /**
     * 自定义润色要求
     */
    @Schema(description = "自定义润色要求，用自然语言描述额外的润色需求。AI会根据此要求进行针对性润色",
            example = "请增加一些环境描写来烘托紧张气氛，但不要改变原有的对话内容")
    private String customRequirements;
}
