package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节剧情记忆
 * 用于记录每章核心剧情，确保故事连贯性
 *
 * @Author AI Assistant
 * @Date 2026-01-28
 */
@Data
@TableName("chapter_plot_memory")
public class ChapterPlotMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

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
     * 剧情摘要（200-300字）
     */
    private String plotSummary;

    /**
     * 章节内容总结（300-500字）
     * 对本章完整内容的总结，用于后续章节回顾
     */
    private String chapterSummary;

    /**
     * 核心事件（JSON数组格式）
     * 如：["主角获得神秘古书","发现修炼秘籍","第一次突破"]
     */
    private String keyEvents;

    /**
     * 人物状态变化（JSON格式）
     * 如：{"主角":"获得能力，等级提升到练气一层","反派":"计划对付主角"}
     */
    private String characterStatus;

    /**
     * 新出现的设定（JSON数组）
     */
    private String newSettings;

    /**
     * 埋下的伏笔（JSON数组）
     */
    private String foreshadowingPlanted;

    /**
     * 回收的伏笔（JSON数组）
     */
    private String foreshadowingResolved;

    /**
     * 待回收伏笔（JSON数组，继承前一章+本章新增）
     */
    private String pendingForeshadowing;

    /**
     * 当前悬念/未解决问题
     */
    private String currentSuspense;

    /**
     * 本章结尾场景（地点、时间、状态、动作）
     * 用于下一章开头承接，确保时间线不倒流
     * 例如："天剑宗山门外，傍晚，刚完成入门考核，正在跟随弟子前往住所"
     */
    private String chapterEndingScene;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
