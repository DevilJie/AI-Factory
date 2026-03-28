package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 项目视图对象
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "项目详情视图对象，包含项目的完整信息，包括基本信息和统计数据")
public class ProjectVo {

    /**
     * 项目ID
     */
    @Schema(description = "项目唯一标识ID", example = "1234567890")
    private String id;

    /**
     * 项目名称
     */
    @Schema(description = "项目名称", example = "玄幻大陆")
    private String name;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述/简介", example = "一个关于少年修仙成神的玄幻故事")
    private String description;

    /**
     * 项目类型：video/novel
     */
    @Schema(description = "项目类型。可选值：video(视频项目)、novel(小说项目)", example = "novel", allowableValues = {"video", "novel"})
    private String projectType;

    /**
     * 故事基调
     */
    @Schema(description = "故事基调/风格", example = "热血励志")
    private String storyTone;

    /**
     * 故事类型
     */
    @Schema(description = "故事类型/题材", example = "修仙")
    private String storyGenre;

    /**
     * 视觉风格
     */
    @Schema(description = "视觉风格", example = "写实")
    private String visualStyle;

    /**
     * 封面图URL
     */
    @Schema(description = "项目封面图URL地址", example = "https://example.com/covers/novel-cover.jpg")
    private String coverUrl;

    /**
     * 状态：draft/in_progress/completed/archived
     */
    @Schema(description = "项目状态。可选值：draft(草稿)、in_progress(进行中)、completed(已完成)、archived(已归档)", example = "in_progress", allowableValues = {"draft", "in_progress", "completed", "archived"})
    private String status;

    /**
     * 标签列表
     */
    @Schema(description = "项目标签列表", example = "[\"玄幻\", \"修仙\", \"热血\"]")
    private List<String> tags;

    /**
     * 小说类型（novelType）: fantasy/urban/scifi/history/military
     */
    @Schema(description = "小说类型分类。可选值：fantasy(玄幻)、urban(都市)、scifi(科幻)、history(历史)、military(军事)", example = "fantasy", allowableValues = {"fantasy", "urban", "scifi", "history", "military"})
    private String novelType;

    /**
     * 目标长度（targetLength）: short/medium/long
     */
    @Schema(description = "小说目标长度。可选值：short(短篇，10万字以内)、medium(中篇，10-50万字)、long(长篇，50万字以上)", example = "long", allowableValues = {"short", "medium", "long"})
    private String targetLength;

    /**
     * 章节数量
     */
    @Schema(description = "项目已创建的章节总数", example = "50")
    private Integer chapterCount;

    /**
     * 总字数（小说项目）
     */
    @Schema(description = "小说项目的总字数", example = "150000")
    private Integer totalWordCount;

    /**
     * 总时长（视频项目，秒）
     */
    @Schema(description = "视频项目的总时长，单位：秒", example = "3600")
    private Integer totalDuration;

    /**
     * 创建时间
     */
    @Schema(description = "项目创建时间，格式：yyyy-MM-dd HH:mm:ss", example = "2025-01-22 10:30:00")
    private String createTime;

    /**
     * 更新时间
     */
    @Schema(description = "项目最后更新时间，格式：yyyy-MM-dd HH:mm:ss", example = "2025-02-08 15:45:30")
    private String updateTime;
}
