package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI生成项目DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Schema(description = "AI生成项目请求参数，用于根据用户想法自动生成项目名称和描述")
@Data
public class AIGenerateProjectDto {

    /**
     * 创作想法
     */
    @Schema(description = "创作想法，用户对小说的初步构思或灵感描述。" +
                          "AI将根据此想法生成项目名称和详细描述",
            example = "我想写一个关于修仙世界的小说，主角是一个普通少年，" +
                     "意外获得上古传承，从此踏上逆天修仙之路。故事要有热血、友情和成长。",
            maxLength = 500,
            required = true,
            accessMode = Schema.AccessMode.READ_WRITE)
    @NotBlank(message = "创作想法不能为空")
    @Size(max = 500, message = "创作想法不能超过500个字符")
    private String idea;
}
