package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册DTO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户注册请求参数")
public class UserRegisterDto {

    /**
     * 登录名
     */
    @NotBlank(message = "登录名不能为空")
    @Schema(description = "登录名/用户名，用于登录系统，建议使用字母、数字组合，长度4-20位", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 4, maxLength = 20)
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码，建议使用字母、数字、特殊字符组合，长度6-20位", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6, maxLength = 20)
    private String password;

    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "用户真实姓名，长度2-20位", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 20)
    private String actualName;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号码，11位中国大陆手机号，格式：1[3-9]开头的11位数字", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^1[3-9]\\d{9}$")
    private String phone;

    /**
     * 邮箱（选填）
     */
    @Schema(description = "邮箱地址，用于接收系统通知和找回密码", example = "zhangsan@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String email;

    /**
     * 昵称（选填）
     */
    @Schema(description = "用户昵称/显示名称，如不填写则默认使用登录名", example = "小张", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 30)
    private String nickname;
}
