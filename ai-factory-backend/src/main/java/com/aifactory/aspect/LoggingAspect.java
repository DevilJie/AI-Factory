package com.aifactory.aspect;

import com.aifactory.common.LoggingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

/**
 * 请求响应日志切面
 *
 * 自动记录所有Controller的请求和响应
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Aspect
@Component
public class LoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定义切点：所有Controller
     */
    @Pointcut("execution(* com.aifactory.controller..*.*(..))")
    public void controllerPointcut() {}

    /**
     * 环绕通知：记录请求和响应
     */
    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            LoggingUtil.setTraceInfo(request);
        }

        // 记录请求
        Object[] args = joinPoint.getArgs();
        Object requestBody = null;
        if (args != null && args.length > 0) {
            // 过滤掉不能序列化的参数（如HttpServletRequest、HttpServletResponse）
            Optional<Object> firstArg = Arrays.stream(args)
                    .filter(arg -> arg != null &&
                                   !(arg instanceof HttpServletRequest) &&
                                   !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                    .findFirst();
            if (firstArg.isPresent()) {
                requestBody = firstArg.get();
            }
        }

        LoggingUtil.logRequest(
            attributes != null ? attributes.getRequest() : null,
            requestBody
        );

        Object result = null;
        try {
            // 执行方法
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            // 记录异常
            LoggingUtil.logError(joinPoint.getSignature().toShortString(), e);
            throw e;
        } finally {
            // 记录响应
            long duration = System.currentTimeMillis() - startTime;
            LoggingUtil.logResponse(result, duration);

            // 清除MDC
            LoggingUtil.clearTraceInfo();
        }
    }
}
