package com.aifactory.config;

import com.aifactory.common.TokenUtil;
import com.aifactory.common.UserContext;
import com.aifactory.common.LoggingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * 从请求头中解析JWT token，将userId存储到UserContext中
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 设置链路追踪信息
            LoggingUtil.setTraceInfo(request);

            // 优先从请求头获取Authorization
            String authHeader = request.getHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 从请求头提取token
                token = authHeader.substring(7);
            } else {
                // 如果请求头没有，尝试从URL参数获取（用于SSE等不支持自定义头的场景）
                token = request.getParameter("token");
            }

            if (token != null) {
                try {
                    // 解析token获取userId
                    Long userId = TokenUtil.getUserId(token);

                    if (userId != null) {
                        // 将userId存储到UserContext中
                        UserContext.setUserId(userId);
                    }
                } catch (Exception e) {
                    // Token解析失败，忽略继续处理请求（允许匿名访问）
                    // 仅记录调试日志，不阻断请求
                }
            }

            // 继续执行filter chain
            filterChain.doFilter(request, response);
        } finally {
            // 清除MDC和UserContext
            LoggingUtil.clearTraceInfo();
            UserContext.clear();
        }
    }
}
