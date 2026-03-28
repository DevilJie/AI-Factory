package com.aifactory.service.llm.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.service.llm.LLMProvider;
import com.aifactory.service.llm.StreamingChatHelper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek LLM提供商实现（基于LangChain4j）
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class DeepSeekProvider implements LLMProvider {

    // TODO: 从用户配置中获取
    private static final String DEFAULT_API_KEY = "your-deepseek-api-key";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    @Override
    public String getProviderName() {
        return "deepseek";
    }

    @Override
    public String getModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public AIGenerateResponse generate(AIGenerateRequest request) {
        try {
            // 1. 创建ChatModel（DeepSeek兼容OpenAI API）
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(DEFAULT_API_KEY)
                    .baseUrl(DEFAULT_BASE_URL)
                    .modelName(DEFAULT_MODEL)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                    .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 32768)  // 默认32K token
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

            // 3. 调用模型生成（新版 API）
            ChatResponse chatResponse = model.chat(messages);
            String content = chatResponse.aiMessage().text();

            // 4. 构建响应
            AIGenerateResponse response = new AIGenerateResponse();
            response.setContent(content);
            response.setModel(DEFAULT_MODEL);
            response.setTotalTokens(0);
            response.setPromptTokens(0);
            response.setCompletionTokens(0);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("DeepSeek调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateStream(AIGenerateRequest request, StreamCallback callback) {
        try {
            // 1. 创建StreamingChatModel（DeepSeek兼容OpenAI API）
            dev.langchain4j.model.openai.OpenAiStreamingChatModel streamingModel =
                dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                    .apiKey(DEFAULT_API_KEY)
                    .baseUrl(DEFAULT_BASE_URL)
                    .modelName(DEFAULT_MODEL)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                    .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 32768)  // 默认32K token
                    .returnThinking(true)  // 启用思考模型支持（DeepSeek-Reasoner）
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

            // 3. 使用新版StreamingChatHelper进行流式生成
            StreamingChatHelper.streamChat(streamingModel, messages, callback);

        } catch (Exception e) {
            callback.onError("DeepSeek流式生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AIGenerateRequest request) {
        if (request.getRole() == null) {
            return "你是DeepSeek开发的AI助手，致力于为用户提供专业、准确的回答。";
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
}
