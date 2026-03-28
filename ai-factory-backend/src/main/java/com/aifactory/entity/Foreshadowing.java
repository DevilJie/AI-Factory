package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伏笔实体
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("novel_foreshadowing")
public class Foreshadowing {

    /**
     * 伏笔ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 伏笔标题
     */
    private String title;

    /**
     * 伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)
     */
    private String type;

    /**
     * 伏笔描述
     */
    private String description;

    /**
     * 布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)
     */
    private String layoutType;

    /**
     * 埋伏笔的章节
     */
    private Integer plantedChapter;

    /**
     * 计划填坑的章节
     */
    private Integer plannedCallbackChapter;

    /**
     * 实际填坑的章节
     */
    private Integer actualCallbackChapter;

    /**
     * 状态：pending(未填回)/in_progress(进行中)/completed(已填回)
     */
    private String status;

    /**
     * 优先级，数值越大优先级越高
     */
    private Integer priority;

    /**
     * 备注信息
     */
    private String notes;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
