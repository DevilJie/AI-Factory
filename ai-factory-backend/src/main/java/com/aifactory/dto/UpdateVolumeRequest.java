package com.aifactory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 更新分卷请求DTO
 *
 * @Author CaiZy
 * @Date 2025-02-08
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "更新分卷请求DTO，用于更新分卷的规划信息。只需要传入需要更新的字段，未传入的字段保持不变。")
public class UpdateVolumeRequest {

    /**
     * 分卷标题
     */
    @Schema(description = "分卷标题", example = "少年崛起")
    private String volumeTitle;

    /**
     * 本卷主旨
     */
    @Schema(description = "本卷主旨/主题", example = "主角从普通少年开始踏上修炼之路")
    private String volumeTheme;

    /**
     * 主要冲突
     */
    @Schema(description = "主要冲突/矛盾", example = "主角与家族敌对势力的初次冲突")
    private String mainConflict;

    /**
     * 情节走向
     */
    @Schema(description = "情节走向描述", example = "从平凡少年到初入修仙界，经历生死考验")
    private String plotArc;

    /**
     * 分卷描述
     */
    @Schema(description = "分卷详细描述", example = "第一卷讲述主角林风意外获得神秘功法...")
    private String volumeDescription;

    /**
     * 关键事件
     */
    @Schema(description = "关键事件列表", example = "获得功法、家族试炼、击退敌袭、进入宗门")
    private String keyEvents;

    /**
     * 时间线设定
     */
    @Schema(description = "时间线设定", example = "故事开始后的第一个月")
    private String timelineSetting;

    /**
     * 目标章节数
     */
    @Schema(description = "目标章节数", example = "30")
    private Integer targetChapterCount;

    /**
     * 分卷备注
     */
    @Schema(description = "分卷备注/创作说明", example = "注意铺垫主角性格")
    private String volumeNotes;

    /**
     * 核心目标
     */
    @Schema(description = "本卷核心目标", example = "完成世界观基础设定")
    private String coreGoal;

    /**
     * 高潮事件
     */
    @Schema(description = "高潮事件描述", example = "主角在家族大比中逆袭")
    private String climax;

    /**
     * 收尾描述
     */
    @Schema(description = "收尾/结局描述", example = "主角踏上前往宗门的旅程")
    private String ending;

    /**
     * 本卷新增人物
     */
    @Schema(description = "本卷新增人物名称列表", example = "[\"林风\", \"林父\", \"苏瑶\"]")
    private List<String> newCharacters;

    /**
     * 阶段伏笔
     */
    @Schema(description = "本卷阶段伏笔列表", example = "[\"神秘功法的来历\", \"主角身世之谜\"]")
    private List<String> stageForeshadowings;

    /**
     * 状态
     */
    @Schema(description = "分卷状态。可选值：planning(规划中)、writing(写作中)、completed(已完成)、paused(暂停)", example = "writing", allowableValues = {"planning", "writing", "completed", "paused"})
    private String status;

    /**
     * 是否完成
     */
    @Schema(description = "是否已完成", example = "false")
    private Boolean volumeCompleted;
}
