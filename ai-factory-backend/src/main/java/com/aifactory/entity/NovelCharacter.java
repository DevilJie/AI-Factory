package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人物实体
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("novel_character")
public class NovelCharacter {

    /**
     * 人物ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 人物名称
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别：male/female/other
     */
    private String gender;

    /**
     * 年龄（支持文字描述，如"十六七岁"）
     */
    private String age;

    /**
     * 角色类型：protagonist/supporting/antagonist/npc
     */
    private String roleType;

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
     * 图像生成描述词（用于AI文生图）
     */
    private String appearancePrompt;

    /**
     * 背景故事
     */
    private String background;

    /**
     * 能力/技能
     */
    private String abilities;

    /**
     * 标签
     */
    private String tags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
