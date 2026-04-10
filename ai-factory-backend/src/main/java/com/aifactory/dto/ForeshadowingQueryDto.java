package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 伏笔查询DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "伏笔查询条件DTO，用于筛选伏笔列表")
public class ForeshadowingQueryDto {

    /**
     * 项目ID
     */
    @Schema(description = "项目ID，用于限定查询范围",
            example = "1")
    private Long projectId;

    /**
     * 伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)
     */
    @Schema(description = "伏笔类型筛选：character(人物)/item(物品)/event(事件)/secret(秘密)",
            allowableValues = {"character", "item", "event", "secret"},
            example = "secret")
    private String type;

    /**
     * 布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)
     */
    @Schema(description = "布局类型筛选：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)",
            allowableValues = {"bright1", "bright2", "bright3", "dark"},
            example = "dark")
    private String layoutType;

    /**
     * 状态：pending(未填回)/in_progress(进行中)/completed(已填回)
     */
    @Schema(description = "状态筛选：pending(未填回)/in_progress(进行中)/completed(已填回)",
            allowableValues = {"pending", "in_progress", "completed"},
            example = "pending")
    private String status;

    /**
     * 章节号（查询该章节需要填坑的伏笔）
     */
    @Schema(description = "当前章节号，用于查询该章节需要填坑的伏笔（plannedCallbackChapter <= currentChapter 且状态为pending）",
            example = "50")
    private Integer currentChapter;

    /**
     * 埋设伏笔的分卷编号筛选
     */
    @Schema(description = "埋设伏笔的分卷编号筛选", example = "1")
    private Integer plantedVolume;

    /**
     * 计划回收伏笔的分卷编号筛选
     */
    @Schema(description = "计划回收伏笔的分卷编号筛选", example = "2")
    private Integer plannedCallbackVolume;
}
