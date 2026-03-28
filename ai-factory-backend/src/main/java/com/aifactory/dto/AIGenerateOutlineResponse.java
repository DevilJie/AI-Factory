package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * AI生成大纲响应
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Schema(description = "AI生成大纲响应结果，包含整体故事规划和分卷/章节规划")
@Data
public class AIGenerateOutlineResponse {

    /**
     * 整体故事梗概
     */
    @Schema(description = "整体故事梗概，概括全书的核心故事线和主题",
            example = "一个普通少年意外获得上古仙人的传承，从此踏上逆天修仙之路...")
    private String overallConcept;

    /**
     * 主要主题
     */
    @Schema(description = "主要主题，故事想要表达的核心思想或价值观",
            example = "努力与机遇并存，天赋与坚持同等重要")
    private String mainTheme;

    /**
     * 分卷规划列表
     */
    @Schema(description = "分卷规划列表，包含每卷的详细规划信息")
    private List<VolumePlan> volumes;

    /**
     * 章节规划列表
     */
    @Schema(description = "章节规划列表，包含每章的详细规划信息")
    private List<ChapterPlan> chapters;

    /**
     * 分卷规划
     */
    @Schema(description = "分卷规划详情")
    @Data
    public static class VolumePlan {
        @Schema(description = "卷号，从1开始", example = "1")
        private Integer volumeNumber;

        @Schema(description = "卷标题", example = "初入仙途")
        private String volumeTitle;

        @Schema(description = "卷主题", example = "主角初识修仙界，开始修炼之路")
        private String volumeTheme;

        @Schema(description = "主要冲突", example = "主角与家族势力的矛盾，以及初入宗门的生存挑战")
        private String mainConflict;

        @Schema(description = "剧情弧线", example = "从普通少年到踏入修仙门槛的成长历程")
        private String plotArc;

        @Schema(description = "卷描述", example = "本卷讲述主角意外获得传承后...")
        private String volumeDescription;

        @Schema(description = "关键事件", example = "获得传承、拜入宗门、初次战斗、突破境界")
        private String keyEvents;

        @Schema(description = "预计字数（万字）", example = "20")
        private Integer estimatedWordCount;

        @Schema(description = "时间线设定", example = "修仙历元年春至秋")
        private String timelineSetting;

        @Schema(description = "目标章节数", example = "40")
        private Integer targetChapterCount;

        @Schema(description = "卷备注", example = "注意控制节奏，不要过于急躁")
        private String volumeNotes;

        @Schema(description = "核心目标", example = "建立世界观基础，让读者了解修仙体系")
        private String coreGoal;

        @Schema(description = "高潮情节", example = "主角在宗门大比中一鸣惊人")
        private String climax;

        @Schema(description = "结尾设定", example = "主角突破筑基，获得参加秘境的资格")
        private String ending;

        @Schema(description = "新登场角色列表", example = "[\"林师父\", \"苏师姐\", \"赵师兄\"]")
        private List<String> newCharacters;

        @Schema(description = "本卷埋下的伏笔", example = "[\"神秘传承的来源\", \"宗门背后的秘密\"]")
        private List<String> stageForeshadowings;
    }

    /**
     * 章节规划
     */
    @Schema(description = "章节规划详情")
    @Data
    public static class ChapterPlan {
        @Schema(description = "章节号", example = "1")
        private Integer chapterNumber;

        @Schema(description = "所属卷号", example = "1")
        private Integer volumeNumber;

        @Schema(description = "章节标题", example = "天降机缘")
        private String chapterTitle;

        @Schema(description = "剧情大纲", example = "主角在山中采药时意外发现一处古洞...")
        private String plotOutline;

        @Schema(description = "关键事件", example = "发现古洞、获得传承、初次感受到灵气")
        private String keyEvents;

        @Schema(description = "章节目标", example = "引入主角，展示其普通但坚韧的性格")
        private String chapterGoal;

        @Schema(description = "目标字数", example = "3000")
        private Integer wordCountTarget;

        @Schema(description = "开篇场景", example = "清晨，主角背着竹篓上山采药")
        private String chapterStartingScene;

        @Schema(description = "结尾场景", example = "主角握着传承玉简，眼神坚定")
        private String chapterEndingScene;
    }
}
