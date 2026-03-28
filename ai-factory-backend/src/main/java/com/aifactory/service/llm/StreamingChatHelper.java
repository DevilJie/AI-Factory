package com.aifactory.service.llm;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 流式生成辅助类（langchain4j 1.11.0+ 新API）
 *
 * @Author CaiZy
 * @Date 2025-02-07
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class StreamingChatHelper {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatHelper.class);

    /**
     * 使用新版 StreamingChatResponseHandler API 进行流式生成
     * 支持 DeepSeek-Reasoner 等思考模型
     *
     * @param model StreamingChatModel (langchain4j 1.11.0+)
     * @param messages 消息列表
     * @param callback 回调接口
     */
    public static void streamChat(StreamingChatModel model, List<ChatMessage> messages,
                                   LLMProvider.StreamCallback callback) {
        log.info("使用新版流式生成（支持思考模型）");

        try {
            final int[] tokenCount = {0};
            final int[] thinkingTokenCount = {0};

            model.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null && !partialResponse.isEmpty()) {
                        tokenCount[0] += partialResponse.length();
                        if (tokenCount[0] % 100 == 0) {
                            log.info("已接收 {} 个内容字符", tokenCount[0]);
                        }
                        try {
                            callback.onText(partialResponse, false);
                        } catch (Exception e) {
                            log.error("回调处理失败", e);
                        }
                    }
                }

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    if (partialThinking != null && partialThinking.text() != null) {
                        thinkingTokenCount[0] += partialThinking.text().length();
                        log.debug("思考中... 已接收 {} 个思考字符", thinkingTokenCount[0]);
                        try {
                            callback.onThinking(partialThinking.text(), false);
                        } catch (Exception e) {
                            log.debug("思考回调失败（可能不支持）: {}", e.getMessage());
                        }
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("流式生成完成，总共接收 {} 个内容字符, {} 个思考字符",
                             tokenCount[0], thinkingTokenCount[0]);
                    try {
                        callback.onText("", true);
                    } catch (Exception e) {
                        log.error("完成回调失败", e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式生成失败，已接收 {} 个内容字符, {} 个思考字符",
                              tokenCount[0], thinkingTokenCount[0], error);
                    callback.onError("流式生成失败: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("流式生成异常", e);
            callback.onError("流式生成异常: " + e.getMessage());
        }
    }
}
