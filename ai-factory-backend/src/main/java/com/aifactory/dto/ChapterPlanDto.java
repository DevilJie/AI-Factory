package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节规划DTO
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@Schema(description = "章节规划数据传输对象，用于返回AI生成的章节规划信息")
public class ChapterPlanDto {

    @Schema(description = "章节规划唯一标识ID", example = "1")
    private Long id;

    @Schema(description = "所属项目ID，关联project表", example = "1")
    private Long projectId;

    @Schema(description = "所属分卷规划ID，关联novel_volume_plan表", example = "1")
    private Long volumePlanId;

    @Schema(description = "章节序号，在整个项目中按顺序排列", example = "1", minimum = "1")
    private Integer chapterNumber;

    @Schema(description = "章节标题，AI生成或手动设置", example = "初入江湖", maxLength = 100)
    private String chapterTitle;

    @Schema(description = "情节大纲，描述本章主要剧情发展，通常100-200字", example = "主角李云初入江湖，在酒楼偶遇神秘老者，得知武林秘闻...")
    private String plotOutline;

    @Schema(description = "关键事件列表，用分号分隔的多个事件", example = "酒楼偶遇老者;获得藏宝图线索;遭遇刺客袭击")
    private String keyEvents;

    @Schema(description = "章节目标，本章要达成的叙事目的", example = "建立主角初入江湖的懵懂形象，引出主线悬念")
    private String chapterGoal;

    @Schema(description = "目标字数，AI生成时的字数参考", example = "3000", minimum = "500", maximum = "20000")
    private Integer wordCountTarget;

    @Schema(description = "章节起点场景，描述章节开始时的场景设置", example = "地点：江南城醉仙楼；时间：傍晚；状态：主角刚抵达城中")
    private String chapterStartingScene;

    @Schema(description = "章节终点场景，描述章节结束时的场景状态", example = "地点：城外树林；时间：深夜；状态：主角与神秘人对话")
    private String chapterEndingScene;

    @Schema(description = "章节备注，补充说明信息", example = "注意：本章需要埋下主角身世伏笔")
    private String chapterNotes;

    @Schema(description = "规划状态", example = "pending", allowableValues = {"pending", "in_progress", "completed", "skipped"})
    private String status;

    @Schema(description = "待埋设的伏笔，用分号分隔", example = "主角腰间的玉佩;老者的神秘身份")
    private String foreshadowingSetup;

    @Schema(description = "待回收的伏笔，用分号分隔", example = "第一章提到的失踪父亲线索")
    private String foreshadowingPayoff;

    @Schema(description = "创建时间，格式：yyyy-MM-ddTHH:mm:ss", example = "2025-02-15T14:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间，格式：yyyy-MM-ddTHH:mm:ss", example = "2025-02-15T16:45:00")
    private LocalDateTime updateTime;

    // 扩展字段：用于前端显示

    @Schema(description = "分卷标题，来自关联的分卷规划", example = "第一卷 少年游")
    private String volumeTitle;

    @Schema(description = "卷号，来自关联的分卷规划", example = "1")
    private Integer volumeNumber;

    @Schema(description = "是否已生成实际章节内容", example = "true")
    private Boolean hasContent;

    @Schema(description = "实际章节ID，如果已生成内容则关联novel_chapter表", example = "1")
    private Long chapterId;

    @Schema(description = "实际章节字数，如果已生成内容则显示实际字数", example = "3500")
    private Integer wordCount;
}
