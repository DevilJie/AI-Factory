package com.aifactory.service.llm.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.service.llm.LLMProvider;
import com.aifactory.service.llm.StreamingHelper;
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
 * 智谱AI LLM提供商实现（基于LangChain4j）
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class ZhipuAIProvider implements LLMProvider {

    // TODO: 从用户配置中获取
    private static final String DEFAULT_API_KEY = "your-zhipu-api-key";
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private static final String DEFAULT_MODEL = "glm-4";

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    @Override
    public String getModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public AIGenerateResponse generate(AIGenerateRequest request) {
        try {
            // 1. 创建ChatModel（智谱AI兼容OpenAI API）
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

            // 3. 调用模型生成（新版 API 使用 chat()）
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
            throw new RuntimeException("智谱AI调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateStream(AIGenerateRequest request, StreamCallback callback) {
        try {
            // 1. 创建StreamingChatModel（智谱AI兼容OpenAI API）
            dev.langchain4j.model.openai.OpenAiStreamingChatModel streamingModel =
                dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
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

            // 3. 使用StreamingHelper进行流式生成
            StreamingHelper.streamGenerate(streamingModel, messages, callback);

        } catch (Exception e) {
            callback.onError("智谱AI流式生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AIGenerateRequest request) {
        if (request.getRole() == null) {
            return "你是由智谱AI训练的AI助手，名为智谱清言。";
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
