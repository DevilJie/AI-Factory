package com.aifactory.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 角色出场章节VO
 *
 * @Author CaiZy
 * @Date 2025-03-04
 */
@Data
@Builder
public class CharacterChapterVO {

    /**
     * 关联记录ID
     */
    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 章节序号
     */
    private Integer chapterNumber;

    /**
     * 章节标题
     */
    private String chapterTitle;

    /**
     * 本章状态
     */
    private String statusInChapter;

    /**
     * 是否首次出现
     */
    private Boolean isFirstAppearance;

    /**
     * 重要程度
     */
    private String importanceLevel;

    /**
     * 情绪变化
     */
    private String emotionChange;

    /**
     * 关键行为
     */
    private String keyBehavior;

    /**
     * 外貌变化
     */
    private String appearanceChange;

    /**
     * 性格展现
     */
    private String personalityReveal;

    /**
     * 能力展现
     */
    private String abilityShown;

    /**
     * 角色成长
     */
    private String characterDevelopment;

    /**
     * 对话摘要
     */
    private String dialogueSummary;

    /**
     * 修为境界（JSON格式）
     */
    private String cultivationLevel;
}
