package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-章节关联实体
 * 记录角色在各章节的出现情况
 *
 * @Author CaiZy
 * @Date 2025-03-04
 */
@Data
@TableName("novel_character_chapter")
public class NovelCharacterChapter {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    private Long characterId;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 章节序号
     */
    private Integer chapterNumber;

    /**
     * 本章状态变化
     */
    private String statusInChapter;

    /**
     * 情绪变化
     */
    private String emotionChange;

    /**
     * 关键行为
     */
    private String keyBehavior;

    /**
     * 是否首次出现（0-否，1-是）
     */
    private Boolean isFirstAppearance;

    /**
     * 重要程度：protagonist/supporting/antagonist/npc
     */
    private String importanceLevel;

    /**
     * 外貌变化
     * 记录角色在本章中的外貌改变，如受伤、换装、容貌变化等
     */
    private String appearanceChange;

    /**
     * 性格展现
     * 记录角色在本章中展现出的性格特点和行为表现
     */
    private String personalityReveal;

    /**
     * 能力展现
     * 记录角色在本章中展示的技能、武艺、特殊能力等
     */
    private String abilityShown;

    /**
     * 角色成长
     * 记录角色在本章中的成长轨迹，包括心理、能力、认知等方面的进步
     */
    private String characterDevelopment;

    /**
     * 对话摘要
     * 记录角色在本章中的重要对话内容摘要
     */
    private String dialogueSummary;

    /**
     * 修为境界（JSON格式）
     * 存储角色在各修炼体系中的境界信息，支持多套修炼体系
     * 格式示例：{"systemName": "武道", "realm": "筑基期", "level": 3}
     */
    private String cultivationLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
