package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 章节更新请求DTO
 *
 * @Author CaiZy
 * @Date 2025-01-29
 */
@Data
@Schema(description = "章节更新请求对象，支持部分更新，只需传入需要修改的字段")
public class ChapterUpdateRequest {

    /**
     * 章节标题
     */
    @Schema(description = "章节标题，长度1-100字符。不传入则不更新此字段", example = "风云际会", maxLength = 100, minLength = 1)
    private String title;

    /**
     * 章节内容
     */
    @Schema(description = "章节正文内容，支持富文本格式。不传入则不更新此字段。传入空字符串会清空内容", example = "第一章 风云际会\n\n那是一个风雨交加的夜晚...")
    private String content;

    /**
     * 所属分卷ID
     */
    @Schema(description = "所属分卷ID，关联novel_volume_plan表。不传入则不更新此字段", example = "1")
    private Long volumeId;

    /**
     * 状态：draft/published
     */
    @Schema(description = "章节状态。draft(草稿): 编辑中；published(已发布): 已定稿。不传入则不更新此字段",
            example = "draft", allowableValues = {"draft", "published"})
    private String status;

    /**
     * 是否锁定
     */
    @Schema(description = "是否锁定章节。锁定后不可被AI自动修改。不传入则不更新此字段", example = "false")
    private Boolean locked;
}
