package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录结果VO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户登录结果响应")
public class UserLoginResultVo {

    /**
     * 登录Token
     */
    @Schema(description = "JWT认证Token，后续请求需在Header的Authorization字段中携带：Bearer {token}", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String token;

    /**
     * 用户ID
     */
    @Schema(description = "用户唯一标识ID", example = "1")
    private Long userId;

    /**
     * 登录名
     */
    @Schema(description = "登录名/用户名", example = "admin")
    private String loginName;

    /**
     * 真实姓名
     */
    @Schema(description = "用户真实姓名", example = "张三")
    private String actualName;

    /**
     * 昵称
     */
    @Schema(description = "用户昵称/显示名称", example = "小张")
    private String nickname;

    /**
     * 头像
     */
    @Schema(description = "用户头像URL地址", example = "https://example.com/avatar/user1.jpg")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    /**
     * 性别
     */
    @Schema(description = "性别：0-未知，1-男，2-女", example = "1", allowableValues = {"0", "1", "2"})
    private Integer gender;
}
