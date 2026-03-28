package com.aifactory.vo;

import lombok.Data;
import lombok.Builder;

/**
 * 人物弧光阶段VO
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@Builder
public class ArcStageVO {

    /**
     * 阶段ID
     */
    private String stageId;

    /**
     * 阶段序号
     */
    private Integer stageOrder;

    /**
     * 卷号
     */
    private Integer volumeNumber;

    /**
     * 阶段描述
     */
    private String description;

    /**
     * 内在变化
     */
    private String innerChange;

    /**
     * 关键时刻
     */
    private String keyMoment;

    /**
     * 关键章节ID
     */
    private Long keyChapterId;

    /**
     * 关键章节标题
     */
    private String keyChapterTitle;

    /**
     * 状态：completed/in_progress/planned
     */
    private String status;

    /**
     * 实际描述（AI分析后）
     */
    private String actualDescription;

    /**
     * 最后更新时间
     */
    private String lastUpdated;
}
