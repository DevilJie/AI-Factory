package com.aifactory.dto;

import lombok.Data;

/**
 * 角色提示词信息DTO
 * 用于章节生成提示词中展示角色信息
 */
@Data
public class CharacterPromptInfo {
    // ==================== 基础角色信息（来自 NovelCharacter） ====================

    /**
     * 角色ID
     */
    private Long characterId;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色类型： protagonist/supporting/antagonist
     */
    private String roleType;

    /**
     * 性别（已格式化：男/女/其他）
     */
    private String gender;

    /**
     * 年龄（支持文字描述，如"十六七岁"）
     */
    private String age;

    /**
     * 角色定位
     */
    private String role;

    /**
     * 性格特点
     */
    private String personality;

    /**
     * 外貌描述
     */
    private String appearance;

    /**
     * 背景故事
     */
    private String background;

    /**
     * 能力/技能
     */
    private String abilities;

    // ==================== 上次登场状态（来自 NovelCharacterChapter） ====================

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
     * 修为境界（JSON格式）
     */
    private String cultivationLevel;
}
