package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI提供商模板实体
 * 用于预定义各大AI厂商的配置模板，用户可选择模板后自定义配置
 *
 * @Author AI Assistant
 * @Date 2025-02-05
 */
@Data
@TableName("t_ai_provider_template")
public class AiProviderTemplate {

    /**
     * 主键ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板代码（唯一标识，如：openai_gpt4、claude_opus、deepseek等）
     */
    private String templateCode;

    /**
     * 服务商类型：llm/image/tts/video
     */
    private String providerType;

    /**
     * 提供商名称（英文，如：OpenAI、Anthropic）
     */
    private String providerName;

    /**
     * 显示名称（用户界面显示，如：OpenAI GPT-4、Claude 3 Opus）
     */
    private String displayName;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 默认API端点
     */
    private String defaultEndpoint;

    /**
     * 默认模型名称
     */
    private String defaultModel;

    /**
     * 配置参数JSON Schema
     * 描述该提供商支持的配置参数结构
     */
    private String configSchema;

    /**
     * 必填字段（JSON数组）
     * 例如：["apiKey", "apiEndpoint", "model"]
     */
    private String requiredFields;

    /**
     * 可选字段（JSON数组）
     * 例如：["temperature", "maxTokens", "topP"]
     */
    private String optionalFields;

    /**
     * 是否系统内置（系统内置模板不允许删除）
     */
    private Boolean isSystem;

    /**
     * 是否启用（禁用的模板不会显示给用户）
     */
    private Boolean isEnabled;

    /**
     * 排序（数值越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
