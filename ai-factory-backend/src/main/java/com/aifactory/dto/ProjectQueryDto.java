package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目查询DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "项目查询请求DTO，用于分页查询项目列表，支持按类型、状态筛选和关键词搜索")
public class ProjectQueryDto {

    /**
     * 项目类型：video/novel
     */
    @Schema(description = "项目类型筛选。可选值：video(视频项目)、novel(小说项目)。不传则查询所有类型", example = "novel", allowableValues = {"video", "novel"})
    private String projectType;

    /**
     * 状态：draft/in_progress/completed/archived
     */
    @Schema(description = "项目状态筛选。可选值：draft(草稿)、in_progress(进行中)、completed(已完成)、archived(已归档)。不传则查询所有状态", example = "in_progress", allowableValues = {"draft", "in_progress", "completed", "archived"})
    private String status;

    /**
     * 搜索关键词（项目名称）
     */
    @Schema(description = "搜索关键词，用于模糊匹配项目名称", example = "玄幻")
    private String keyword;

    /**
     * 页码
     */
    @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Schema(description = "每页返回的记录数量，最大100", example = "10", defaultValue = "10", maximum = "100")
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段。可选值：createTime(创建时间)、updateTime(更新时间)。不传则默认按更新时间排序",
            example = "updateTime",
            allowableValues = {"createTime", "updateTime"},
            defaultValue = "updateTime")
    private String sortBy;

    /**
     * 排序方向
     */
    @Schema(description = "排序方向。可选值：asc(升序)、desc(降序)。不传则默认降序",
            example = "desc",
            allowableValues = {"asc", "desc"},
            defaultValue = "desc")
    private String sortOrder;
}
