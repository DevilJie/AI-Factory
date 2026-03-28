package com.aifactory.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Getter
public enum TaskStatus {

    /**
     * 等待中
     */
    PENDING("pending", "等待中"),

    /**
     * 运行中
     */
    RUNNING("running", "运行中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 失败
     */
    FAILED("failed", "失败"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String name;

    TaskStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskStatus getByCode(String code) {
        for (TaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
