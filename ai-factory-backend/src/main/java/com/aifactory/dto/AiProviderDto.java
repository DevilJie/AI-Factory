package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI服务提供商配置DTO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Schema(description = "AI服务提供商配置参数，用于创建或更新AI服务配置")
@Data
public class AiProviderDto {

    /**
     * 主键ID（编辑时需要）
     */
    @Schema(description = "主键ID，编辑已有配置时必填，新建时不需要",
            example = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Long id;

    /**
     * 服务商类型: llm/image/tts/video
     */
    @Schema(description = "服务商类型。" +
                          "llm-大语言模型（如GPT、Claude），" +
                          "image-图像生成（如DALL-E、Midjourney），" +
                          "tts-语音合成（如Azure TTS），" +
                          "video-视频生成",
            example = "llm",
            required = true,
            allowableValues = {"llm", "image", "tts", "video"},
            accessMode = Schema.AccessMode.READ_WRITE)
    @NotBlank(message = "服务商类型不能为空")
    private String providerType;

    /**
     * 服务商代码
     */
    @Schema(description = "服务商代码，标识具体的AI服务商。" +
                          "常用代码：openai、anthropic、zhipu(智谱)、qwen(通义千问)、" +
                          "moonshot(月之暗面)、deepseek等",
            example = "openai",
            required = true,
            accessMode = Schema.AccessMode.READ_WRITE)
    @NotBlank(message = "服务商代码不能为空")
    private String providerCode;

    /**
     * 服务商名称
     */
    @Schema(description = "服务商显示名称，用于前端展示",
            example = "OpenAI GPT-4",
            required = true,
            accessMode = Schema.AccessMode.READ_WRITE)
    @NotBlank(message = "服务商名称不能为空")
    private String providerName;

    /**
     * 图标URL
     */
    @Schema(description = "服务商图标URL，用于前端展示",
            example = "https://example.com/icons/openai.png",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String iconUrl;

    /**
     * API密钥
     */
    @Schema(description = "API密钥，用于调用AI服务时的身份验证。" +
                          "注意：此字段为敏感信息，请妥善保管",
            example = "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String apiKey;

    /**
     * API端点
     */
    @Schema(description = "API端点地址，AI服务的API调用地址。" +
                          "部分服务商支持自定义端点（如代理地址或私有部署）",
            example = "https://api.openai.com/v1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String apiEndpoint;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称，指定要使用的具体AI模型。" +
                          "OpenAI: gpt-4, gpt-4-turbo, gpt-3.5-turbo；" +
                          "Anthropic: claude-3-opus, claude-3-sonnet；" +
                          "智谱: glm-4, glm-3-turbo",
            example = "gpt-4",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String model;

    /**
     * 是否默认
     */
    @Schema(description = "是否设为默认服务商。同一类型只能有一个默认服务商。" +
                          "0-否，1-是",
            example = "1",
            allowableValues = {"0", "1"},
            defaultValue = "0",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Integer isDefault;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用此服务商配置。禁用后不会在服务选择中显示。" +
                          "0-禁用，1-启用",
            example = "1",
            allowableValues = {"0", "1"},
            defaultValue = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Integer enabled;

    /**
     * 配置JSON
     */
    @Schema(description = "额外配置参数，JSON格式。" +
                          "常用参数：temperature(温度，0-1)、maxTokens(最大Token数)、" +
                          "topP(核采样参数，0-1)、frequencyPenalty(频率惩罚，0-2)、" +
                          "presencePenalty(存在惩罚，0-2)",
            example = "{\"temperature\":0.7,\"maxTokens\":4096,\"topP\":0.9}",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String configJson;
}
