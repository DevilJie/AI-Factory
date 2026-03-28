package com.aifactory.service;

import com.aifactory.common.LoggingUtil;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.enums.AIRole;
import com.aifactory.service.llm.LLMProvider;
import com.aifactory.service.llm.LLMProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI生成服务
 *
 * 提供通用的AI生成能力，支持多种角色和场景
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class AIGenerateService {

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    /**
     * 通用AI生成方法
     *
     * @param request 生成请求
     * @return 生成响应
     */
    public AIGenerateResponse generate(AIGenerateRequest request) {
        long startTime = System.currentTimeMillis();

        // 记录业务日志
        LoggingUtil.logBusiness("AI生成", "用户请求AI生成",
            "角色=" + (request.getRole() != null ? request.getRole().getName() : "通用助手") +
            ", 任务长度=" + request.getTask().length());

        try {
            // 1. 构建系统提示词
            String systemPrompt = buildSystemPrompt(request);

            // 2. 获取默认的LLM提供商
            LLMProvider provider = llmProviderFactory.getDefaultProvider();

            LoggingUtil.logBusiness("AI生成", "选择LLM提供商",
                "提供商=" + provider.getProviderName());

            // 3. 如果请求中有自定义系统提示词，则使用自定义的
            if (request.getCustomSystemPrompt() != null && !request.getCustomSystemPrompt().isEmpty()) {
                request.setCustomSystemPrompt(systemPrompt);
            }

            // 4. 调用提供商生成内容
            AIGenerateResponse response = provider.generate(request);

            // 记录AI调用日志
            LoggingUtil.logAICall(
                provider.getProviderName(),
                response.getModel(),
                String.valueOf(response.getPromptTokens()),
                String.valueOf(response.getCompletionTokens()),
                System.currentTimeMillis() - startTime
            );

            // 记录生成成功日志
            LoggingUtil.logBusiness("AI生成", "内容生成成功",
                "内容长度=" + response.getContent().length() + "字符");

            return response;
        } catch (Exception e) {
            // 记录异常日志
            LoggingUtil.logError("AI生成", e);
            throw e;
        }
    }

    /**
     * 使用指定角色生成内容
     *
     * @param role AI角色
     * @param task 任务描述
     * @return 生成内容
     */
    public String generateByRole(AIRole role, String task) {
        AIGenerateRequest request = new AIGenerateRequest();
        request.setRole(role);
        request.setTask(task);
        AIGenerateResponse response = generate(request);
        return response.getContent();
    }

    /**
     * 快速生成（使用通用助手角色）
     *
     * @param task 任务描述
     * @return 生成内容
     */
    public String quickGenerate(String task) {
        return generateByRole(AIRole.GENERAL_ASSISTANT, task);
    }

    /**
     * 构建系统提示词
     *
     * @param request 请求
     * @return 系统提示词
     */
    private String buildSystemPrompt(AIGenerateRequest request) {
        AIRole role = request.getRole() != null ? request.getRole() : AIRole.GENERAL_ASSISTANT;

        StringBuilder prompt = new StringBuilder();
        prompt.append("# 角色设定\n");
        prompt.append("你是一名").append(role.getName()).append("。\n");
        prompt.append(role.getDescription()).append("\n\n");

        prompt.append("# 工作要求\n");
        prompt.append("1. 严格按照你的角色专业背景来回答问题\n");
        prompt.append("2. 提供专业、准确、有深度的内容\n");
        prompt.append("3. 保持逻辑清晰，结构合理\n");
        prompt.append("4. 如果涉及创作，请注意创新性和吸引力\n\n");

        prompt.append("# 任务\n");
        prompt.append(request.getTask());

        return prompt.toString();
    }

    /**
     * 根据名称获取LLM提供商
     *
     * @param providerName 提供商名称
     * @return LLM提供商
     */
    public LLMProvider getProvider(String providerName) {
        return llmProviderFactory.getProvider(providerName);
    }
}
