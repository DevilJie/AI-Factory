package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 章节规划更新请求DTO
 * 用于前端编辑章节规划详情时提交修改
 */
@Data
@Schema(description = "章节规划更新请求对象，支持部分更新，只需传入需要修改的字段")
public class ChapterPlanUpdateRequest {

    @Schema(description = "章节标题", example = "初入江湖", maxLength = 100)
    private String chapterTitle;

    @Schema(description = "情节大纲", example = "主角李云初入江湖...")
    private String plotOutline;

    @Schema(description = "章节起点场景")
    private String chapterStartingScene;

    @Schema(description = "章节终点场景")
    private String chapterEndingScene;

    @Schema(description = "关键事件")
    private String keyEvents;

    @Schema(description = "章节目标")
    private String chapterGoal;

    @Schema(description = "目标字数", example = "3000")
    private Integer wordCountTarget;

    @Schema(description = "章节备注")
    private String chapterNotes;

    @Schema(description = "规划状态", allowableValues = {"pending", "in_progress", "completed", "skipped"})
    private String status;

    @Schema(description = "情节阶段", allowableValues = {"introduction", "development", "climax", "conclusion"})
    private String plotStage;

    @Schema(description = "埋伏笔")
    private String foreshadowingSetup;

    @Schema(description = "填伏笔")
    private String foreshadowingPayoff;

    @Schema(description = "规划角色，JSON格式")
    private String plannedCharacters;

    @Schema(description = "人物弧光变化，JSON格式")
    private String characterArcs;
}
