package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 章节规划实体
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@TableName("novel_chapter_plan")
public class NovelChapterPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联分卷规划ID
     */
    private Long volumePlanId;

    /**
     * 章节序号
     */
    private Integer chapterNumber;

    /**
     * 章节标题
     */
    private String chapterTitle;

    /**
     * 情节大纲
     */
    private String plotOutline;

    /**
     * 章节起点场景（地点、时间、状态）
     * 用于确定章节从哪里开始
     */
    private String chapterStartingScene;

    /**
     * 章节终点场景（地点、时间、状态）
     * 用于确定章节到哪里结束
     */
    private String chapterEndingScene;

    /**
     * 关键事件（JSON格式）
     */
    private String keyEvents;

    /**
     * 章节目标
     */
    private String chapterGoal;

    /**
     * 目标字数
     */
    private Integer wordCountTarget;

    /**
     * 章节备注
     */
    private String chapterNotes;

    /**
     * 状态: planned/in_progress/completed
     */
    private String status;

    /**
     * 情节阶段: introduction(起始阶段), development(发展阶段),
     * climax(高潮阶段), conclusion(结局阶段)
     */
    private String plotStage;

    /**
     * 该阶段是否已完成（防止剧情回溯）
     */
    private Boolean stageCompleted;

    /**
     * 埋伏笔（JSON格式）
     */
    private String foreshadowingSetup;

    /**
     * 填伏笔（JSON格式）
     */
    private String foreshadowingPayoff;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
