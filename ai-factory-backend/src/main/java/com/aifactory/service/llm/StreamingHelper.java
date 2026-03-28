package com.aifactory.service.llm;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 流式生成辅助类（langchain4j 1.11.0+ 新API）
 *
 * @Author CaiZy
 * @Date 2025-01-28
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class StreamingHelper {

    private static final Logger log = LoggerFactory.getLogger(StreamingHelper.class);

    /**
     * 通用的流式生成方法（langchain4j 1.11.0+ 新API）
     * model.chat()会阻塞直到完成，所以这里本身就是同步的
     *
     * @param model StreamingChatModel
     * @param messages 消息列表
     * @param callback 回调接口
     */
    public static void streamGenerate(StreamingChatModel model, List<ChatMessage> messages,
                                       LLMProvider.StreamCallback callback) {
        log.info("使用真正的流式生成");

        try {
            final int[] tokenCount = {0};

            model.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null && !partialResponse.isEmpty()) {
                        tokenCount[0] += partialResponse.length();
                        if (tokenCount[0] % 100 == 0) {
                            log.info("已接收 {} 个字符", tokenCount[0]);
                        }
                        try {
                            callback.onText(partialResponse, false);
                        } catch (Exception e) {
                            log.error("回调处理失败", e);
                        }
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("流式生成完成，总共接收 {} 个字符", tokenCount[0]);
                    try {
                        callback.onText("", true);
                    } catch (Exception e) {
                        log.error("完成回调失败", e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式生成失败，已接收 {} 个字符", tokenCount[0], error);
                    callback.onError("流式生成失败: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("流式生成异常", e);
            callback.onError("流式生成异常: " + e.getMessage());
        }
    }
}
