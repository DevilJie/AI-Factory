package com.aifactory.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码：0表示成功，其他表示失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 是否成功
     */
    private Boolean ok;

    public Result() {
    }

    public Result(Integer code, String msg, T data, Boolean ok) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.ok = ok;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(0, "操作成功", null, true);
    }

    /**
     * 成功响应（有数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "操作成功", data, true);
    }

    /**
     * 成功响应（指定消息和数据）
     */
    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(0, msg, data, true);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(-1, msg, null, false);
    }

    /**
     * 失败响应（指定错误码）
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null, false);
    }

    /**
     * 用户参数错误
     */
    public static <T> Result<T> userErrorParam(String msg) {
        return new Result<>(30001, msg, null, false);
    }
}
