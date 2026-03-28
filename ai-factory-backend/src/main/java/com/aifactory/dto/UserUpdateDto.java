package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户更新信息DTO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户信息更新请求参数")
public class UserUpdateDto {

    /**
     * 昵称
     */
    @Schema(description = "用户昵称/显示名称，长度不超过30位", example = "小张同学", maxLength = 30)
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号码，11位中国大陆手机号，格式：1[3-9]开头的11位数字", example = "13900139000", pattern = "^1[3-9]\\d{9}$")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址，用于接收系统通知和找回密码", example = "newemail@example.com")
    private String email;
}
