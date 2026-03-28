package com.aifactory.service.llm;

import com.aifactory.common.LoggingUtil;
import com.aifactory.common.UserContext;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.AiProvider;
import com.aifactory.enums.AIRole;
import com.aifactory.service.AiInteractionLogService;
import com.aifactory.service.AiProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM提供商工厂
 *
 * 根据用户配置动态创建LLM提供商实例
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Component
public class LLMProviderFactory {

    @Autowired
    private AiProviderService aiProviderService;

    @Autowired
    private AiInteractionLogService aiInteractionLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取用户的默认LLM提供商
     */
    public LLMProvider getDefaultProvider() {
        // 从UserContext获取当前登录用户ID
        Long userId = UserContext.getUserId();
        return getDefaultProvider(userId);
    }

    /**
     * 根据用户ID获取默认LLM提供商（用于异步任务等场景）
     */
    public LLMProvider getDefaultProvider(Long userId) {
        // 获取用户的LLM提供商列表
        List<AiProvider> providers = getProvidersByUserId(userId);

        // 查找默认的提供商
        for (AiProvider provider : providers) {
            if (provider.getIsDefault() == 1 && provider.getEnabled() == 1) {
                return createProvider(provider);
            }
        }

        // 如果没有默认的，返回第一个启用的
        for (AiProvider provider : providers) {
            if (provider.getEnabled() == 1) {
                return createProvider(provider);
            }
        }

        throw new RuntimeException("没有可用的LLM提供商，请先在系统设置中配置API Key");
    }

    /**
     * 根据provider code获取提供商
     * 如果providerCode为null，返回默认提供商
     */
    public LLMProvider getProvider(String providerCode) {
        // 如果没有指定providerCode，返回默认提供商
        if (providerCode == null || providerCode.isEmpty()) {
            return getDefaultProvider();
        }

        Long userId = UserContext.getUserId();
        List<AiProvider> providers = getProvidersByUserId(userId);

        for (AiProvider provider : providers) {
            if (provider.getProviderCode().equals(providerCode) && provider.getEnabled() == 1) {
                return createProvider(provider);
            }
        }

        throw new RuntimeException("未找到可用的LLM提供商: " + providerCode);
    }

    /**
     * 根据用户ID获取提供商列表
     */
    private List<AiProvider> getProvidersByUserId(Long userId) {
        // 如果 userId 为 null，抛出异常
        if (userId == null) {
            throw new IllegalStateException("用户未登录，无法获取AI提供商配置");
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProvider> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(AiProvider::getUserId, userId);
        wrapper.eq(AiProvider::getProviderType, "llm");
        wrapper.orderByAsc(AiProvider::getId);

        List<AiProvider> providers = aiProviderService.getAiProviderMapper().selectList(wrapper);

        // 用户未配置LLM，提示去配置
        if (providers.isEmpty()) {
            throw new RuntimeException("您还未配置大语言模型，请先前往「系统设置 → AI模型」配置您的API Key和模型");
        }

        return providers;
    }

    /**
     * 根据数据库配置创建动态提供商
     */
    private LLMProvider createProvider(AiProvider config) {
        return new LLMProvider() {
            @Override
            public String getProviderName() {
                return config.getProviderCode();
            }

            @Override
            public String getModel() {
                return config.getModel();
            }

            @Override
            public AIGenerateResponse generate(AIGenerateRequest request) {
                long startTime = System.currentTimeMillis();

                // 【强制检查】requestType必须提供，用于数据分析和追踪
                if (request.getRequestType() == null || request.getRequestType().isEmpty()) {
                    throw new IllegalArgumentException(
                        "requestType不能为空！所有AI调用必须指定requestType以便数据分类和追踪。" +
                        "请使用提示词模板编码（如：llm_chapter_generate_standard）" +
                        "或指定明确的业务类型（如：chapter_generate/outline_generate等）"
                    );
                }

                // 生成或获取traceId
                String traceId = request.getTraceId();
                if (traceId == null || traceId.isEmpty()) {
                    traceId = aiInteractionLogService.generateTraceId();
                    request.setTraceId(traceId);
                }

                LoggingUtil.logBusiness("LLM调用", "开始调用LLM",
                    "提供商=" + config.getProviderCode() +
                    ", 模型=" + config.getModel() +
                    ", requestType=" + request.getRequestType() +
                    ", API地址=" + config.getApiEndpoint() +
                    ", traceId=" + traceId);

                AIGenerateResponse response = null;
                Throwable exception = null;
                String responseContent = null;

                try {
                    // 从config_json中解析配置
                    Integer configMaxTokens = null;
                    Double configTemperature = null;
                    if (config.getConfigJson() != null && !config.getConfigJson().isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(config.getConfigJson());
                            if (configNode.has("maxTokens")) {
                                configMaxTokens = configNode.get("maxTokens").asInt();
                            }
                            if (configNode.has("temperature")) {
                                configTemperature = configNode.get("temperature").asDouble();
                            }
                        } catch (Exception e) {
                            LoggingUtil.logError("解析提供商配置", new RuntimeException(
                                "解析提供商配置失败: " + e.getMessage(), e));
                        }
                    }

                    // 使用配置中的temperature，如果没有则使用请求中的，最后使用默认值0.7
                    Double temperature = request.getTemperature() != null ? request.getTemperature() :
                        (configTemperature != null ? configTemperature : 0.7);

                    // 使用配置中的maxTokens，如果没有则使用请求中的，最后使用默认值4096
                    Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() :
                        (configMaxTokens != null ? configMaxTokens : 4096);

                    // 1. 创建ChatModel（使用用户配置）
                    ChatModel model = OpenAiChatModel.builder()
                            .apiKey(config.getApiKey())
                            .baseUrl(config.getApiEndpoint())
                            .modelName(config.getModel())
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .timeout(java.time.Duration.ofMinutes(10))  // 设置超时时间为10分钟
                            .build();

                    // 2. 构建消息列表
                    List<ChatMessage> messages = new ArrayList<>();

                    // 添加系统提示词
                    String systemPrompt = request.getCustomSystemPrompt();
                    if (systemPrompt == null || systemPrompt.isEmpty()) {
                        systemPrompt = buildSystemPrompt(request);
                    }
                    messages.add(new SystemMessage(systemPrompt));

                    // 添加用户任务
                    messages.add(new UserMessage(request.getTask()));

                    LoggingUtil.logBusiness("LLM调用", "发送请求到LLM",
                        "消息数量=" + messages.size() +
                        ", 系统提示词长度=" + systemPrompt.length() +
                        ", 用户任务长度=" + request.getTask().length() +
                        ", traceId=" + traceId);

                    // 3. 调用模型生成（新版 API 使用 chat()）
                    ChatResponse chatResponse = model.chat(messages);
                    responseContent = chatResponse.aiMessage().text();

                    long duration = System.currentTimeMillis() - startTime;
                    LoggingUtil.logBusiness("LLM调用", "LLM调用成功",
                        "提供商=" + config.getProviderCode() +
                        ", 耗时=" + duration + "ms" +
                        ", 响应长度=" + responseContent.length() +
                        ", traceId=" + traceId);

                    // 4. 构建响应
                    response = new AIGenerateResponse();
                    response.setContent(responseContent);
                    response.setModel(config.getModel());
                    response.setTotalTokens(0);
                    response.setPromptTokens(0);
                    response.setCompletionTokens(0);

                    return response;

                } catch (Throwable e) {
                    exception = e;
                    LoggingUtil.logError("LLM调用", new RuntimeException(
                        config.getProviderName() + "调用失败: " + e.getMessage() + ", traceId=" + traceId, e));
                    throw e;

                } finally {
                    long duration = System.currentTimeMillis() - startTime;

                    // 记录AI交互日志
                    try {
                        String requestParamsJson = null;
                        try {
                            requestParamsJson = objectMapper.writeValueAsString(request);
                        } catch (Exception e) {
                            // ignore
                        }

                        String errorMessage = null;
                        if (exception != null) {
                            errorMessage = exception.getMessage();
                            if (errorMessage != null && errorMessage.length() > 1000) {
                                errorMessage = errorMessage.substring(0, 1000);
                            }
                        }

                        aiInteractionLogService.logInteraction(
                            traceId,
                            request.getProjectId(),
                            request.getVolumePlanId(),
                            request.getChapterPlanId(),
                            request.getChapterId(),
                            UserContext.getUserId(),
                            request.getRequestType(),
                            config.getProviderCode(),
                            config.getModel(),
                            request.getTask(),
                            requestParamsJson,
                            responseContent,
                            response != null ? response.getTotalTokens() : null,
                            duration,
                            exception == null,
                            errorMessage
                        );
                    } catch (Exception e) {
                        // 记录失败不影响主流程
                        LoggingUtil.logError("AI交互日志", new RuntimeException(
                            "记录AI交互日志失败，traceId=" + traceId, e));
                    }
                }
            }

            @Override
            public void generateStream(AIGenerateRequest request, StreamCallback callback) {
                long startTime = System.currentTimeMillis();

                // 【强制检查】requestType必须提供，用于数据分析和追踪
                if (request.getRequestType() == null || request.getRequestType().isEmpty()) {
                    throw new IllegalArgumentException(
                        "requestType不能为空！所有AI调用必须指定requestType以便数据分类和追踪。" +
                        "请使用提示词模板编码（如：llm_chapter_generate_standard）" +
                        "或指定明确的业务类型（如：chapter_generate/outline_generate等）"
                    );
                }

                LoggingUtil.logBusiness("LLM流式调用", "开始调用LLM",
                    "提供商=" + config.getProviderCode() +
                    ", 模型=" + config.getModel() +
                    ", requestType=" + request.getRequestType() +
                    ", API地址=" + config.getApiEndpoint());

                try {
                    // 从config_json中解析配置
                    Integer configMaxTokens = null;
                    Double configTemperature = null;
                    if (config.getConfigJson() != null && !config.getConfigJson().isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(config.getConfigJson());
                            if (configNode.has("maxTokens")) {
                                configMaxTokens = configNode.get("maxTokens").asInt();
                            }
                            if (configNode.has("temperature")) {
                                configTemperature = configNode.get("temperature").asDouble();
                            }
                        } catch (Exception e) {
                            LoggingUtil.logError("解析提供商配置", new RuntimeException(
                                "解析提供商配置失败: " + e.getMessage(), e));
                        }
                    }

                    // 使用配置中的temperature，如果没有则使用请求中的，最后使用默认值0.7
                    Double temperature = request.getTemperature() != null ? request.getTemperature() :
                        (configTemperature != null ? configTemperature : 0.7);

                    // 使用配置中的maxTokens，如果没有则使用请求中的，最后使用默认值4096
                    Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() :
                        (configMaxTokens != null ? configMaxTokens : 4096);

                    // 1. 创建StreamingChatModel（使用用户配置）
                    dev.langchain4j.model.openai.OpenAiStreamingChatModel streamingModel =
                        dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                            .apiKey(config.getApiKey())
                            .baseUrl(config.getApiEndpoint())
                            .modelName(config.getModel())
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .timeout(java.time.Duration.ofMinutes(10))
                            .build();

                    // 2. 构建消息列表
                    List<ChatMessage> messages = new ArrayList<>();

                    // 添加系统提示词
                    String systemPrompt = request.getCustomSystemPrompt();
                    if (systemPrompt == null || systemPrompt.isEmpty()) {
                        systemPrompt = buildSystemPrompt(request);
                    }
                    messages.add(new SystemMessage(systemPrompt));

                    // 添加用户任务
                    messages.add(new UserMessage(request.getTask()));

                    LoggingUtil.logBusiness("LLM流式调用", "发送请求到LLM",
                        "消息数量=" + messages.size() +
                        ", 系统提示词长度=" + systemPrompt.length() +
                        ", 用户任务长度=" + request.getTask().length());

                    // 3. 使用StreamingHelper进行流式生成
                    StreamingHelper.streamGenerate(streamingModel, messages, callback);

                    long duration = System.currentTimeMillis() - startTime;
                    LoggingUtil.logBusiness("LLM流式调用", "LLM流式调用完成",
                        "提供商=" + config.getProviderCode() +
                        ", 耗时=" + duration + "ms");

                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    LoggingUtil.logError("LLM流式调用", new RuntimeException(
                        config.getProviderName() + "流式调用失败，耗时" + duration + "ms: " + e.getMessage(), e));
                    callback.onError(config.getProviderName() + "流式生成失败: " + e.getMessage());
                }
            }

            /**
             * 构建系统提示词
             */
            private String buildSystemPrompt(AIGenerateRequest request) {
                if (request.getRole() == null) {
                    return "你是一个有用的AI助手。";
                }

                return String.format("""
                        # 角色设定
                        你是一名%s。

                        %s

                        # 工作要求
                        1. 严格按照你的角色专业背景来回答问题
                        2. 提供专业、准确、有深度的内容
                        3. 保持逻辑清晰，结构合理
                        4. 如果涉及创作，请注意创新性和吸引力
                        """,
                        request.getRole().getName(),
                        request.getRole().getDescription()
                );
            }
        };
    }
}
