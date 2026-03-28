package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分卷规划DTO
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@Schema(description = "分卷规划DTO，包含分卷的完整信息，包括基本信息、情节规划、人物设定和章节列表")
public class VolumePlanDto {

    @Schema(description = "分卷记录ID，数据库主键", example = "1")
    private Long id;

    @Schema(description = "所属项目ID，关联project表", example = "1234567890")
    private Long projectId;

    @Schema(description = "所属大纲ID，关联outline表", example = "100")
    private Long outlineId;

    @Schema(description = "卷序号，从1开始的分卷序号", example = "1")
    private Integer volumeNumber;

    @Schema(description = "分卷标题", example = "第1卷：少年崛起")
    private String volumeTitle;

    @Schema(description = "本卷主旨/主题", example = "主角从普通少年开始踏上修炼之路")
    private String volumeTheme;

    @Schema(description = "主要冲突/矛盾", example = "主角与家族敌对势力的初次冲突")
    private String mainConflict;

    @Schema(description = "情节走向描述", example = "从平凡少年到初入修仙界，经历生死考验")
    private String plotArc;

    @Schema(description = "本卷核心目标", example = "完成世界观基础设定，建立主角与主要配角的关系")
    private String coreGoal;

    @Schema(description = "关键事件列表，用分号分隔", example = "获得功法;家族试炼;击退敌袭;进入宗门")
    private String keyEvents;

    @Schema(description = "分卷详细描述", example = "第一卷讲述主角林风意外获得神秘功法，开始在家族中崭露头角...")
    private String volumeDescription;

    @Schema(description = "高潮事件描述", example = "主角在家族大比中逆袭，震惊所有人")
    private String climax;

    @Schema(description = "收尾/结局描述", example = "主角踏上前往宗门的旅程，第一卷完")
    private String ending;

    @Schema(description = "时间线设定", example = "故事开始后的第一个月")
    private String timelineSetting;

    @Schema(description = "目标章节数", example = "30")
    private Integer targetChapterCount;

    @Schema(description = "分卷备注/创作说明", example = "注意铺垫主角性格，为后续发展埋下伏笔")
    private String volumeNotes;

    @Schema(description = "本卷新增人物名称列表", example = "[\"林风\", \"林父\", \"苏瑶\", \"陈长老\"]")
    private List<String> newCharacters;

    @Schema(description = "本卷阶段伏笔列表", example = "[\"神秘功法的来历\", \"主角身世之谜\"]")
    private List<String> stageForeshadowings;

    @Schema(description = "分卷状态。可选值：planning(规划中)、writing(写作中)、completed(已完成)、paused(暂停)", example = "writing", allowableValues = {"planning", "writing", "completed", "paused"})
    private String status;

    @Schema(description = "排序顺序，数值越小越靠前", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否已完成", example = "false")
    private Boolean volumeCompleted;

    @Schema(description = "创建时间", example = "2025-01-23T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-02-08T15:45:30")
    private LocalDateTime updateTime;

    /**
     * 该卷的章节规划列表
     */
    @Schema(description = "该分卷下的章节规划列表")
    private List<ChapterPlanDto> chapters;
}
