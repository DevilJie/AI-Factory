package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 验证码VO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "验证码响应")
public class CaptchaVo {

    /**
     * 验证码Base64图片
     */
    @Schema(description = "验证码图片的Base64编码字符串，可直接在img标签的src属性中使用，格式：data:image/png;base64,{base64String}", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
    private String captchaBase64Image;

    /**
     * 验证码UUID
     */
    @Schema(description = "验证码的唯一标识UUID，登录或注册时需要携带此UUID进行验证", example = "550e8400-e29b-41d4-a716-446655440000")
    private String captchaUuid;

    /**
     * 过期时间（秒）
     */
    @Schema(description = "验证码有效期，单位：秒。超过此时间验证码将失效，需要重新获取", example = "300")
    private Integer expireSeconds;
}
