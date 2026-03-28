package com.aifactory.aspect;

import com.aifactory.common.UserContext;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.service.AiInteractionLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI大模型交互切面
 * 拦截所有与大模型的交互，记录请求和响应
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Slf4j
@Aspect
@Component
public class LLMInteractionAspect {

    @Autowired
    private AiInteractionLogService aiInteractionLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 拦截LLMProvider的generate方法
     * 这个pointcut会拦截所有LLMProvider实现类的generate方法
     */
    @Pointcut("execution(* com.aifactory.service.llm.LLMProvider.generate(..))")
    public void llmGeneratePointcut() {
    }

    /**
     * 环绕通知：记录AI交互
     */
    @Around("llmGeneratePointcut()")
    public Object aroundLLMGenerate(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();

        // 获取请求参数
        AIGenerateRequest request = null;
        String traceId = null;
        Long projectId = null;
        Long volumePlanId = null;
        Long chapterPlanId = null;
        Long chapterId = null;
        Long userId = null;
        String requestType = null;

        if (args.length > 0 && args[0] instanceof AIGenerateRequest) {
            request = (AIGenerateRequest) args[0];
            traceId = request.getTraceId();
            projectId = request.getProjectId();
            volumePlanId = request.getVolumePlanId();
            chapterPlanId = request.getChapterPlanId();
            chapterId = request.getChapterId();
            userId = UserContext.getUserId();
            requestType = request.getRequestType();

            // 如果没有traceId，生成一个新的
            if (traceId == null || traceId.isEmpty()) {
                traceId = aiInteractionLogService.generateTraceId();
                request.setTraceId(traceId);
            }

            // 在日志中打印traceId，便于追踪
            log.info("AI交互开始，traceId={}, projectId={}, userId={}, requestType={}, task={}",
                    traceId, projectId, userId, requestType,
                    request.getTask() != null ? request.getTask().substring(0, Math.min(100, request.getTask().length())) : "null");
        }

        AIGenerateResponse response = null;
        Throwable exception = null;
        String provider = "unknown";
        String model = "unknown";

        // 尝试从目标对象获取provider信息
        Object target = joinPoint.getTarget();
        if (target instanceof com.aifactory.service.llm.LLMProvider) {
            provider = ((com.aifactory.service.llm.LLMProvider) target).getProviderName();
        }

        try {
            // 执行目标方法
            response = (AIGenerateResponse) joinPoint.proceed();

            // 从响应中获取model信息
            if (response != null && response.getModel() != null) {
                model = response.getModel();
            }

            return response;

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 记录日志
            try {
                String requestPrompt = request != null && request.getTask() != null ? request.getTask() : null;
                String requestParamsJson = null;

                // 序列化请求参数
                if (request != null) {
                    try {
                        requestParamsJson = objectMapper.writeValueAsString(request);
                    } catch (Exception e) {
                        log.warn("序列化请求参数失败", e);
                    }
                }

                String responseContent = null;
                Integer responseTokens = null;

                // 从响应中获取信息
                if (response != null) {
                    responseContent = response.getContent();
                    responseTokens = response.getTotalTokens();

                    // 尝试获取model（如果响应中有）
                    if (response.getModel() != null) {
                        model = response.getModel();
                    }
                }

                // 从异常中获取错误信息
                String errorMessage = null;
                if (exception != null) {
                    errorMessage = exception.getMessage();
                    if (errorMessage != null && errorMessage.length() > 1000) {
                        errorMessage = errorMessage.substring(0, 1000);
                    }
                }

                // 保存日志
                aiInteractionLogService.logInteraction(
                        traceId,
                        projectId,
                        volumePlanId,
                        chapterPlanId,
                        chapterId,
                        userId,
                        requestType,
                        provider,
                        model,
                        requestPrompt,
                        requestParamsJson,
                        responseContent,
                        responseTokens,
                        duration,
                        exception == null,
                        errorMessage
                );

                // 在业务日志中打印结果
                if (exception == null) {
                    log.info("AI交互成功，traceId={}, provider={}, model={}, tokens={}, 耗时={}ms",
                            traceId, provider, model, responseTokens, duration);
                } else {
                    log.error("AI交互失败，traceId={}, provider={}, model={}, 耗时={}ms, error={}",
                            traceId, provider, model, duration, errorMessage);
                }

            } catch (Exception e) {
                // 记录日志失败不应影响主流程
                log.error("记录AI交互日志失败，traceId={}", traceId, e);
            }
        }
    }
}
