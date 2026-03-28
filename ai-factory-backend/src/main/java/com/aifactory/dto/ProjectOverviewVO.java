package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 项目概览视图对象
 */
@Data
@Schema(description = "项目概览视图对象，包含项目基础信息、统计数据和创作进度")
public class ProjectOverviewVO {

    @Schema(description = "项目ID", example = "1234567890")
    private String id;

    @Schema(description = "项目名称", example = "玄幻大陆")
    private String name;

    @Schema(description = "项目描述", example = "一个关于少年修仙的故事")
    private String description;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "项目状态：draft/in_progress/completed/archived")
    private String status;

    @Schema(description = "项目标签列表")
    private List<String> tags;

    @Schema(description = "已完成章节数")
    private Integer chapterCount;

    @Schema(description = "角色数量")
    private Integer characterCount;

    @Schema(description = "总字数")
    private Integer totalWordCount;

    @Schema(description = "目标章节数（分卷规划之和）")
    private Integer targetChapterCount;

    @Schema(description = "创作进度百分比 (0-100)")
    private Integer progress;
}
