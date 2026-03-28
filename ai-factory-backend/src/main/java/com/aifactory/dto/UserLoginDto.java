package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录DTO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户登录请求参数")
public class UserLoginDto {

    /**
     * 登录名
     */
    @NotBlank(message = "登录名不能为空")
    @Schema(description = "登录名/用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码，6-20位字符", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Schema(description = "图形验证码内容，不区分大小写", example = "ABCD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaCode;

    /**
     * 验证码UUID
     */
    @NotBlank(message = "验证码UUID不能为空")
    @Schema(description = "验证码的唯一标识UUID，从获取验证码接口返回", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaUuid;

    /**
     * 登录设备：1-PC
     */
    @Schema(description = "登录设备类型：1-PC端，2-移动端，3-Web端", example = "1", defaultValue = "1", allowableValues = {"1", "2", "3"})
    private Integer loginDevice = 1;
}
