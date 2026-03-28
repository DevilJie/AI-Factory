package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI交互日志查询DTO
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Schema(description = "AI交互日志详情，记录一次完整的AI请求和响应信息")
@Data
public class AiInteractionLogDTO {

    /**
     * 主键ID
     */
    @Schema(description = "日志记录主键ID",
            example = "1")
    private Long id;

    /**
     * 交互流水号
     */
    @Schema(description = "交互流水号，唯一标识一次AI交互。" +
                          "格式：trace_yyyyMMdd_UUID",
            example = "trace_20250203_abc123def")
    private String traceId;

    /**
     * 项目ID
     */
    @Schema(description = "关联的项目ID",
            example = "1")
    private Long projectId;

    /**
     * 用户ID
     */
    @Schema(description = "发起请求的用户ID",
            example = "1")
    private Long userId;

    /**
     * 请求类型
     */
    @Schema(description = "请求类型，标识AI调用的业务场景。" +
                          "chapter_generate-章节生成，outline_generate-大纲生成，" +
                          "worldview_generate-世界观生成，character_fix-角色修复等",
            example = "chapter_generate")
    private String requestType;

    /**
     * AI提供商
     */
    @Schema(description = "AI提供商名称，标识使用的AI服务商",
            example = "openai")
    private String provider;

    /**
     * 模型名称
     */
    @Schema(description = "使用的AI模型名称",
            example = "gpt-4")
    private String model;

    /**
     * 请求提示词（前500字符）
     */
    @Schema(description = "请求提示词预览，仅显示前500字符。完整内容请查看requestPrompt字段",
            example = "请根据以下设定生成第一章的正文内容...")
    private String requestPromptPreview;

    /**
     * 完整请求提示词
     */
    @Schema(description = "完整的请求提示词内容",
            example = "请根据以下设定生成第一章的正文内容：主角是一个普通少年...")
    private String requestPrompt;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数JSON，包含temperature、maxTokens等生成参数",
            example = "{\"temperature\":0.7,\"maxTokens\":4000}")
    private String requestParams;

    /**
     * 响应内容（前500字符）
     */
    @Schema(description = "响应内容预览，仅显示前500字符。完整内容请查看responseContent字段",
            example = "第一章 天降机缘\n\n清晨的阳光透过窗帘...")
    private String responseContentPreview;

    /**
     * 完整响应内容
     */
    @Schema(description = "完整的AI响应内容",
            example = "第一章 天降机缘\n\n清晨的阳光透过窗帘的缝隙洒进房间...")
    private String responseContent;

    /**
     * 响应token数
     */
    @Schema(description = "响应消耗的Token数量",
            example = "2000",
            minimum = "0")
    private Integer responseTokens;

    /**
     * 响应耗时（毫秒）
     */
    @Schema(description = "响应耗时，单位：毫秒。从发起请求到收到完整响应的时间",
            example = "3500",
            minimum = "0")
    private Long responseDuration;

    /**
     * 是否成功
     */
    @Schema(description = "请求是否成功完成",
            example = "true")
    private Boolean isSuccess;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息，当请求失败时记录错误原因",
            example = "API rate limit exceeded")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Schema(description = "日志创建时间",
            example = "2025-02-03T10:30:00")
    private LocalDateTime createdTime;
}
