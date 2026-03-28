package com.aifactory.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日志工具类
 *
 * 提供统一的日志记录功能，支持链路追踪
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class LoggingUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggingUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 配置ObjectMapper支持Java 8日期时间类型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 链路追踪ID的MDC key
     */
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";

    /**
     * 生成链路追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 设置请求追踪信息到MDC
     */
    public static void setTraceInfo(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(REQUEST_METHOD, request.getMethod());

        // 从UserContext获取用户ID
        Long userId = UserContext.getUserId();
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    /**
     * 清除MDC
     */
    public static void clearTraceInfo() {
        MDC.clear();
    }

    /**
     * 记录请求日志
     */
    public static void logRequest(HttpServletRequest request, Object body) {
        try {
            String traceId = MDC.get(TRACE_ID);
            String userId = MDC.get(USER_ID);
            String uri = MDC.get(REQUEST_URI);
            String method = MDC.get(REQUEST_METHOD);

            String bodyJson = body != null ? objectMapper.writeValueAsString(body) : "{}";

            logger.info("=== 请求开始 === " +
                    "TraceId: {} | " +
                    "UserId: {} | " +
                    "Method: {} {} | " +
                    "Body: {}",
                    traceId, userId, method, uri, bodyJson);

        } catch (Exception e) {
            logger.error("记录请求日志失败", e);
        }
    }

    /**
     * 记录响应日志
     */
    public static void logResponse(Object result, long duration) {
        try {
            String traceId = MDC.get(TRACE_ID);
            String userId = MDC.get(USER_ID);
            String uri = MDC.get(REQUEST_URI);

            logger.info("=== 请求完成 === " +
                    "TraceId: {} | " +
                    "UserId: {} | " +
                    "URI: {} | " +
                    "耗时: {}ms",
                    traceId, userId, uri, duration);

        } catch (Exception e) {
            logger.error("记录响应日志失败", e);
        }
    }

    /**
     * 记录异常日志
     */
    public static void logError(String operation, Exception e) {
        String traceId = MDC.get(TRACE_ID);
        String userId = MDC.get(USER_ID);
        String uri = MDC.get(REQUEST_URI);

        logger.error("=== 业务异常 === " +
                    "TraceId: {} | " +
                    "UserId: {} | " +
                    "URI: {} | " +
                    "操作: {} | " +
                    "异常: {} | " +
                    "堆栈: {}",
                traceId, userId, uri, operation, e.getMessage(), getStackTrace(e));

        // 记录完整的异常堆栈
        logger.error("完整堆栈信息:", e);
    }

    /**
     * 记录业务日志
     */
    public static void logBusiness(String operation, String message) {
        String traceId = MDC.get(TRACE_ID);
        String userId = MDC.get(USER_ID);

        logger.info("=== 业务操作 === " +
                    "TraceId: {} | " +
                    "UserId: {} | " +
                    "操作: {} | " +
                    "消息: {}",
                traceId, userId, operation, message);
    }

    /**
     * 记录业务日志（带参数）
     */
    public static void logBusiness(String operation, String message, Object params) {
        try {
            String traceId = MDC.get(TRACE_ID);
            String userId = MDC.get(USER_ID);
            String paramsJson = params != null ? objectMapper.writeValueAsString(params) : "{}";

            logger.info("=== 业务操作 === " +
                        "TraceId: {} | " +
                        "UserId: {} | " +
                        "操作: {} | " +
                        "消息: {} | " +
                        "参数: {}",
                    traceId, userId, operation, message, paramsJson);

        } catch (Exception e) {
            logger.error("记录业务日志失败", e);
        }
    }

    /**
     * 记录AI调用日志
     */
    public static void logAICall(String provider, String model, String inputTokens, String outputTokens, long duration) {
        String traceId = MDC.get(TRACE_ID);
        String userId = MDC.get(USER_ID);

        logger.info("=== AI调用 === " +
                    "TraceId: {} | " +
                    "UserId: {} | " +
                    "提供商: {} | " +
                    "模型: {} | " +
                    "输入Tokens: {} | " +
                    "输出Tokens: {} | " +
                    "耗时: {}ms",
                traceId, userId, provider, model, inputTokens, outputTokens, duration);
    }

    /**
     * 获取异常堆栈信息
     */
    private static String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取当前TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取当前UserId
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }
}
