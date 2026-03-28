package com.aifactory.controller;

import com.aifactory.common.TokenUtil;
import com.aifactory.dto.AiProviderDto;
import com.aifactory.entity.AiProvider;
import com.aifactory.entity.AiProviderTemplate;
import com.aifactory.response.Result;
import com.aifactory.service.AiProviderService;
import com.aifactory.service.AiProviderTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI服务提供商配置控制器
 *
 * 提供AI服务提供商的配置管理功能，包括LLM（大语言模型）、图像生成、
 * 语音合成(TTS)、视频生成等多种AI服务的配置和管理
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Tag(name = "AI服务提供商管理", description = "AI服务提供商配置管理接口，支持LLM、图像、语音、视频等多种AI服务的配置和测试")
@RestController
@RequestMapping("/api/ai-provider")
public class AiProviderController {

    @Autowired
    private AiProviderService aiProviderService;

    @Autowired
    private AiProviderTemplateService aiProviderTemplateService;

    /**
     * 获取AI服务提供商配置列表
     */
    @Operation(
        summary = "获取AI服务提供商列表",
        description = "获取当前用户配置的所有AI服务提供商。可按服务商类型筛选，" +
                      "支持llm(大语言模型)、image(图像生成)、tts(语音合成)、video(视频生成)四种类型。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录")
    })
    @GetMapping
    public Result<List<AiProvider>> getProviders(
            @Parameter(description = "JWT认证令牌", required = true,
                      example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "服务商类型筛选：llm-大语言模型, image-图像生成, tts-语音合成, video-视频生成",
                      example = "llm",
                      schema = @Schema(allowableValues = {"llm", "image", "tts", "video"}))
            @RequestParam(required = false) String providerType) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        List<AiProvider> providers = aiProviderService.getProviders(token, providerType);
        return Result.ok(providers);
    }

    /**
     * 保存或更新AI服务提供商配置
     */
    @Operation(
        summary = "保存AI服务提供商配置",
        description = "创建新的AI服务提供商配置或更新已有配置。" +
                      "如果dto中包含id则为更新操作，否则为新增操作。" +
                      "支持配置API密钥、端点地址、模型名称、温度参数等。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录")
    })
    @PutMapping
    public Result<String> saveProvider(
            @Parameter(description = "JWT认证令牌", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "AI服务提供商配置信息", required = true)
            @Valid @RequestBody AiProviderDto dto) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        aiProviderService.saveProvider(token, dto);
        return Result.ok("保存成功");
    }

    /**
     * 删除AI服务提供商配置
     */
    @Operation(
        summary = "删除AI服务提供商配置",
        description = "根据ID删除指定的AI服务提供商配置。删除后不可恢复。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @DeleteMapping("/{id}")
    public Result<String> deleteProvider(
            @Parameter(description = "JWT认证令牌", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "AI服务提供商配置ID", required = true, example = "1")
            @PathVariable Long id) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        aiProviderService.deleteProvider(token, id);
        return Result.ok("删除成功");
    }

    /**
     * 测试AI服务提供商连接（通过配置ID）
     */
    @Operation(
        summary = "测试AI服务连接",
        description = "通过已保存的配置ID测试AI服务提供商的连接是否正常。" +
                      "后端会查询真实的API密钥进行测试。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "测试完成",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TestConnectionResult.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @PostMapping("/test/{id}")
    public Result<TestConnectionResult> testConnectionById(
            @Parameter(description = "JWT认证令牌", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "AI服务提供商配置ID", required = true, example = "1")
            @PathVariable Long id) {
        String token = authorization.replace("Bearer ", "").trim();
        TestConnectionResult result = aiProviderService.testConnectionById(token, id);
        return Result.ok(result);
    }

    /**
     * 测试连接请求DTO
     */
    @Schema(description = "测试AI服务连接请求参数")
    public static class TestConnectionRequest {
        @Schema(description = "服务商类型：llm-大语言模型, image-图像生成, tts-语音合成, video-视频生成",
                example = "llm",
                allowableValues = {"llm", "image", "tts", "video"})
        private String providerType;

        @Schema(description = "服务商代码，如openai、anthropic、zhipu等",
                example = "openai")
        private String providerCode;

        @Schema(description = "API密钥，用于身份验证",
                example = "sk-xxxxxxxxxxxxxxxx")
        private String apiKey;

        @Schema(description = "API端点地址",
                example = "https://api.openai.com/v1")
        private String apiEndpoint;

        @Schema(description = "模型名称",
                example = "gpt-4")
        private String model;

        public String getProviderType() {
            return providerType;
        }

        public void setProviderType(String providerType) {
            this.providerType = providerType;
        }

        public String getProviderCode() {
            return providerCode;
        }

        public void setProviderCode(String providerCode) {
            this.providerCode = providerCode;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    /**
     * 测试连接结果DTO
     */
    @Schema(description = "测试AI服务连接结果")
    public static class TestConnectionResult {
        @Schema(description = "连接是否成功", example = "true")
        private boolean success;

        @Schema(description = "结果消息，成功时返回连接成功信息，失败时返回错误原因",
                example = "连接成功，模型响应正常")
        private String message;

        public TestConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // ================================================================
    // 模板相关API（新增）
    // ================================================================

    /**
     * 获取所有启用的AI提供商模板
     */
    @Operation(
        summary = "获取AI提供商模板列表",
        description = "获取系统中所有可用的AI服务提供商模板。模板包含预设的配置信息，" +
                      "用户可以基于模板快速创建AI服务配置。可按服务商类型筛选。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/templates")
    public Result<List<AiProviderTemplate>> getAllTemplates(
            @Parameter(description = "服务商类型筛选：llm-大语言模型, image-图像生成, tts-语音合成, video-视频生成",
                      example = "llm",
                      schema = @Schema(allowableValues = {"llm", "image", "tts", "video"}))
            @RequestParam(required = false) String providerType) {
        List<AiProviderTemplate> templates;
        if (providerType != null && !providerType.isEmpty()) {
            templates = aiProviderTemplateService.getTemplatesByType(providerType);
        } else {
            templates = aiProviderTemplateService.getAllEnabledTemplates();
        }
        return Result.ok(templates);
    }

    /**
     * 根据模板ID获取模板详情
     */
    @Operation(
        summary = "根据ID获取AI提供商模板",
        description = "根据模板ID获取详细的AI服务提供商模板信息，包含默认端点、默认模型等配置。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @GetMapping("/templates/{id}")
    public Result<AiProviderTemplate> getTemplateById(
            @Parameter(description = "模板ID", required = true, example = "1")
            @PathVariable Long id) {
        AiProviderTemplate template = aiProviderTemplateService.getTemplateById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }
        return Result.ok(template);
    }

    /**
     * 根据模板代码获取模板详情
     */
    @Operation(
        summary = "根据代码获取AI提供商模板",
        description = "根据模板代码（如openai、anthropic等）获取详细的AI服务提供商模板信息。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @GetMapping("/templates/code/{templateCode}")
    public Result<AiProviderTemplate> getTemplateByCode(
            @Parameter(description = "模板代码，如openai、anthropic、zhipu等", required = true,
                      example = "openai")
            @PathVariable String templateCode) {
        AiProviderTemplate template = aiProviderTemplateService.getTemplateByCode(templateCode);
        if (template == null) {
            return Result.error("模板不存在");
        }
        return Result.ok(template);
    }

    /**
     * 从模板创建AI提供商配置
     */
    @Operation(
        summary = "从模板创建AI提供商配置",
        description = "基于预设模板快速创建AI服务提供商配置。只需提供模板ID和API密钥，" +
                      "其他配置可使用模板默认值。也可以自定义端点、模型等参数覆盖模板默认值。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @PostMapping("/create-from-template")
    public Result<String> createFromTemplate(
            @Parameter(description = "JWT认证令牌", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "从模板创建请求参数", required = true)
            @RequestBody CreateFromTemplateRequest request) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        Long userId = TokenUtil.getUserId(token);

        // 获取模板
        AiProviderTemplate template = aiProviderTemplateService.getTemplateById(request.getTemplateId());
        if (template == null) {
            return Result.error("模板不存在");
        }

        // 创建配置
        AiProviderDto dto = new AiProviderDto();
        dto.setProviderType(template.getProviderType());
        dto.setProviderCode(template.getTemplateCode());
        dto.setProviderName(template.getDisplayName());
        dto.setIconUrl(template.getIconUrl());
        dto.setApiEndpoint(request.getApiEndpoint() != null ? request.getApiEndpoint() : template.getDefaultEndpoint());
        dto.setModel(request.getModel() != null ? request.getModel() : template.getDefaultModel());
        dto.setApiKey(request.getApiKey());
        dto.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        dto.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);

        // 设置 configJson，优先使用前端传递的值
        if (request.getConfigJson() != null && !request.getConfigJson().isEmpty()) {
            dto.setConfigJson(request.getConfigJson());
        } else {
            // 设置默认的 configJson
            dto.setConfigJson("{\"temperature\":0.7,\"maxTokens\":4096,\"topP\":0.9}");
        }

        aiProviderService.saveProvider(token, dto);

        return Result.ok("创建成功");
    }

    /**
     * 从模板创建请求DTO
     */
    @Schema(description = "从模板创建AI提供商配置请求参数")
    public static class CreateFromTemplateRequest {
        @Schema(description = "模板ID", required = true, example = "1")
        private Long templateId;

        @Schema(description = "API密钥，用于身份验证", required = true,
                example = "sk-xxxxxxxxxxxxxxxx")
        private String apiKey;

        @Schema(description = "API端点地址，为空则使用模板默认值",
                example = "https://api.openai.com/v1")
        private String apiEndpoint;

        @Schema(description = "模型名称，为空则使用模板默认值",
                example = "gpt-4")
        private String model;

        @Schema(description = "额外配置JSON，包含temperature、maxTokens等参数",
                example = "{\"temperature\":0.7,\"maxTokens\":4096,\"topP\":0.9}")
        private String configJson;

        @Schema(description = "是否设为默认服务商：0-否，1-是",
                example = "0",
                allowableValues = {"0", "1"})
        private Integer isDefault;

        @Schema(description = "是否启用：0-禁用，1-启用",
                example = "1",
                allowableValues = {"0", "1"})
        private Integer enabled;

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getConfigJson() {
            return configJson;
        }

        public void setConfigJson(String configJson) {
            this.configJson = configJson;
        }

        public Integer getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Integer isDefault) {
            this.isDefault = isDefault;
        }

        public Integer getEnabled() {
            return enabled;
        }

        public void setEnabled(Integer enabled) {
            this.enabled = enabled;
        }
    }
}
