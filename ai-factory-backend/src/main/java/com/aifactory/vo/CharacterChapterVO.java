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
}
