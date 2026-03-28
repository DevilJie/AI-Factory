package com.aifactory.service.llm;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;

/**
 * LLM提供商接口
 *
 * 定义所有LLM提供商需要实现的统一接口
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public interface LLMProvider {

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 获取模型名称
     */
    String getModel();

    /**
     * 生成文本（同步）
     *
     * @param request 生成请求
     * @return 生成响应
     */
    AIGenerateResponse generate(AIGenerateRequest request);

    /**
     * 流式生成文本
     *
     * @param request 生成请求
     * @param callback 每生成一个token就回调一次
     */
    void generateStream(AIGenerateRequest request, StreamCallback callback);

    /**
     * 流式输出回调接口
     */
    interface StreamCallback {
        /**
         * 接收到文本片段
         * @param text 文本内容
         * @param isDone 是否完成
         */
        void onText(String text, boolean isDone);

        /**
         * 接收到思考过程（思考模型专用，如 DeepSeek-Reasoner）
         * 默认实现为空，子类可以选择性实现
         *
         * @param thinking 思考内容
         * @param isDone 是否完成
         */
        default void onThinking(String thinking, boolean isDone) {
            // 默认不处理思考过程
        }

        /**
         * 发生错误
         * @param error 错误信息
         */
        void onError(String error);
    }
}
