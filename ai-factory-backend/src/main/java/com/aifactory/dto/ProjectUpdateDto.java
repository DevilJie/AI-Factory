package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目更新DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "项目更新请求DTO，用于更新已有项目的基本信息。只需要传入需要更新的字段，未传入的字段保持不变。")
public class ProjectUpdateDto {

    /**
     * 项目ID
     */
    @Schema(description = "项目ID，必填，用于标识要更新的项目", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    private String id;

    /**
     * 项目名称
     */
    @Schema(description = "项目名称，可选，最大长度50字符", example = "玄幻大陆之崛起", maxLength = 50)
    private String name;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述/简介，可选，最大长度500字符", example = "少年林风意外获得神秘功法，踏上修仙之路的故事", maxLength = 500)
    private String description;

    /**
     * 故事基调
     */
    @Schema(description = "故事基调/风格，如：热血、温馨、悬疑、搞笑等", example = "热血")
    private String storyTone;

    /**
     * 故事类型
     */
    @Schema(description = "故事类型/题材，如：修仙、都市、科幻、历史等", example = "修仙")
    private String storyGenre;

    /**
     * 视觉风格
     */
    @Schema(description = "视觉风格，如：写实、卡通、水墨等", example = "写实")
    private String visualStyle;

    /**
     * 封面图URL
     */
    @Schema(description = "项目封面图URL地址", example = "https://example.com/covers/novel-cover.jpg")
    private String coverUrl;

    /**
     * 状态
     */
    @Schema(description = "项目状态。可选值：draft(草稿)、in_progress(进行中)、completed(已完成)、archived(已归档)", example = "in_progress", allowableValues = {"draft", "in_progress", "completed", "archived"})
    private String status;

    /**
     * 标签（JSON数组字符串）
     */
    @Schema(description = "项目标签，JSON数组字符串格式", example = "[\"玄幻\", \"修仙\", \"热血\", \"励志\"]")
    private String tags;
}
