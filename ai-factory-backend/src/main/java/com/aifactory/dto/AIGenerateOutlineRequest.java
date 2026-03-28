package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI生成大纲请求
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Schema(description = "AI生成大纲请求参数，用于生成小说的整体大纲规划")
@Data
public class AIGenerateOutlineRequest {

    /**
     * 项目ID
     */
    @Schema(description = "项目ID，用于关联具体的项目",
            example = "1",
            required = true)
    private Long projectId;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述，包含故事的核心设定、背景、主要人物等信息",
            example = "一部修仙题材的玄幻小说，讲述一个普通少年意外获得上古传承，从此踏上逆天修仙之路的故事")
    private String projectDescription;

    /**
     * 故事基调
     */
    @Schema(description = "故事基调，决定整体氛围和情感走向。" +
                          "如：热血、轻松、黑暗、温馨、悬疑等",
            example = "热血")
    private String storyTone;

    /**
     * 故事类型
     */
    @Schema(description = "故事类型/题材，如玄幻、都市、科幻、历史、悬疑等",
            example = "玄幻",
            allowableValues = {"玄幻", "都市", "科幻", "历史", "悬疑", "言情", "武侠", "仙侠"})
    private String storyGenre;

    /**
     * 目标分卷数
     */
    @Schema(description = "目标分卷数，计划将小说分为多少卷。" +
                          "每卷通常包含一个完整的故事弧线",
            example = "5",
            minimum = "1",
            maximum = "20")
    private Integer targetVolumeCount;

    /**
     * 目标章节数
     */
    @Schema(description = "目标章节数，全书计划包含的章节总数",
            example = "200",
            minimum = "10",
            maximum = "2000")
    private Integer targetChapterCount;

    /**
     * 每卷平均字数（万字）
     */
    @Schema(description = "每卷平均字数，单位：万字。" +
                          "用于控制每卷的内容体量",
            example = "20",
            minimum = "5",
            maximum = "100")
    private Integer avgWordsPerVolume;

    /**
     * 预计总字数（万字）
     */
    @Schema(description = "预计总字数，单位：万字。" +
                          "用于规划整体篇幅",
            example = "100",
            minimum = "10",
            maximum = "1000")
    private Integer targetWordCount;

    /**
     * 额外要求
     */
    @Schema(description = "额外要求，用于补充特殊需求或限制条件。" +
                          "如：避免某些情节、必须包含某些元素等",
            example = "希望故事节奏紧凑，每章结尾留有悬念；避免过多的感情戏")
    private String additionalRequirements;

    /**
     * 开始卷号（用于继续生成）
     */
    @Schema(description = "开始卷号，用于从指定卷继续生成大纲。" +
                          "默认从第1卷开始。适用于增量生成场景",
            example = "1",
            minimum = "1",
            defaultValue = "1")
    private Integer startVolumeNumber;

    /**
     * 是否重新生成（清空现有大纲）
     */
    @Schema(description = "是否重新生成。true-清空现有大纲重新生成，false-保留现有大纲增量生成",
            example = "false",
            defaultValue = "false")
    private Boolean regenerate;
}
