package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI生成响应DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Schema(description = "AI生成响应结果，包含生成的内容和Token消耗统计")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIGenerateResponse {

    /**
     * 生成的内容
     */
    @Schema(description = "AI生成的文本内容，根据请求的任务类型返回相应格式的内容。" +
                          "可能是纯文本、Markdown格式或JSON格式",
            example = "第一章 天降机缘\n\n清晨的阳光透过窗帘的缝隙洒进房间...")
    private String content;

    /**
     * 使用的模型名称
     */
    @Schema(description = "实际使用的AI模型名称，格式为：提供商/模型名",
            example = "openai/gpt-4")
    private String model;

    /**
     * 消耗的Token数
     */
    @Schema(description = "本次请求消耗的总Token数，包含输入和输出",
            example = "2500",
            minimum = "0")
    private Integer totalTokens;

    /**
     * 提示词Token数
     */
    @Schema(description = "输入提示词消耗的Token数",
            example = "500",
            minimum = "0")
    private Integer promptTokens;

    /**
     * 完成Token数
     */
    @Schema(description = "AI生成输出消耗的Token数",
            example = "2000",
            minimum = "0")
    private Integer completionTokens;
}
