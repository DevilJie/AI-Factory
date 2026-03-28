package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "章节数据传输对象，用于返回章节的基本信息和内容")
public class ChapterDto {

    /**
     * 章节ID
     */
    @Schema(description = "章节唯一标识ID", example = "1")
    private Long id;

    /**
     * 分卷规划ID
     */
    @Schema(description = "所属分卷规划的ID，关联novel_volume_plan表", example = "1")
    private Long volumePlanId;

    /**
     * 章节规划ID
     */
    @Schema(description = "关联的章节规划ID，关联novel_chapter_plan表。AI生成章节时填充此字段", example = "1")
    private Long chapterPlanId;

    /**
     * 章节序号
     */
    @Schema(description = "章节序号，用于排序显示。从1开始递增", example = "1", minimum = "1")
    private Integer chapterNumber;

    /**
     * 章节标题
     */
    @Schema(description = "章节标题，长度1-100字符", example = "风云际会", maxLength = 100, minLength = 1)
    private String title;

    /**
     * 字数
     */
    @Schema(description = "章节字数统计，根据内容自动计算", example = "3500", minimum = "0")
    private Integer wordCount;

    /**
     * 状态：draft/published
     */
    @Schema(description = "章节状态", example = "draft", allowableValues = {"draft", "published"})
    private String status;

    /**
     * 章节内容
     */
    @Schema(description = "章节正文内容，支持富文本格式。创建时可为空，后续通过更新接口填充", example = "第一章 风云际会\n\n那是一个风雨交加的夜晚...")
    private String content;

    /**
     * 更新时间
     */
    @Schema(description = "最后更新时间，格式：yyyy-MM-dd HH:mm:ss", example = "2025-02-15 14:30:00")
    private String updateTime;
}
