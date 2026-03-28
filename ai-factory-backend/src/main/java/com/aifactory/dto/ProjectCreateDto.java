package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目创建DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "项目创建请求DTO，用于创建新的小说或视频项目")
public class ProjectCreateDto {

    /**
     * 项目名称
     */
    @Schema(description = "项目名称，必填，最大长度50字符", example = "玄幻大陆", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    @NotBlank(message = "项目名称不能为空")
    private String name;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述/简介，可选，最大长度500字符", example = "一个关于少年修仙成神的玄幻故事", maxLength = 500)
    private String description;

    /**
     * 项目类型：video(视频项目)/novel(小说项目)
     */
    @Schema(description = "项目类型，必填。可选值：video(视频项目)、novel(小说项目)", example = "novel", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"video", "novel"})
    @NotBlank(message = "项目类型不能为空")
    private String projectType;

    /**
     * 故事基调
     */
    @Schema(description = "故事基调/风格，如：热血、温馨、悬疑、搞笑等", example = "热血励志")
    private String storyTone;

    /**
     * 故事类型
     */
    @Schema(description = "故事类型/题材，如：修仙、都市、科幻、历史等", example = "修仙")
    private String storyGenre;

    /**
     * 小说类型
     */
    @Schema(description = "小说类型分类。可选值：fantasy(玄幻)、urban(都市)、scifi(科幻)、history(历史)、military(军事)", example = "fantasy", allowableValues = {"fantasy", "urban", "scifi", "history", "military"})
    private String novelType;

    /**
     * 目标长度
     */
    @Schema(description = "小说目标长度。可选值：short(短篇，10万字以内)、medium(中篇，10-50万字)、long(长篇，50万字以上)", example = "long", allowableValues = {"short", "medium", "long"})
    private String targetLength;

    /**
     * 标签（JSON数组字符串）
     */
    @Schema(description = "项目标签，JSON数组字符串格式", example = "[\"玄幻\", \"修仙\", \"热血\"]")
    private String tags;

    /**
     * 视觉风格（视频项目专用）
     */
    @Schema(description = "视觉风格，仅视频项目使用。如：写实、卡通、水墨、赛博朋克等", example = "写实")
    private String visualStyle;
}
