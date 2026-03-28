package com.aifactory.common;

/**
 * 用户上下文 - ThreadLocal存储当前登录用户信息
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class UserContext {

    /**
     * 使用ThreadLocal存储用户ID，保证线程安全
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 存储用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 清除用户ID（请求结束后调用，防止内存泄漏）
     */
    public static void clear() {
        USER_ID.remove();
    }
}
