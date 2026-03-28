package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节实体
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("novel_chapter")
public class Chapter {

    /**
     * 章节ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 分卷规划ID（关联到novel_volume_plan表）
     */
    private Long volumePlanId;

    /**
     * 章节规划ID（关联到novel_chapter_plan表）
     */
    private Long chapterPlanId;

    /**
     * 章节序号
     */
    private Integer chapterNumber;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节内容
     */
    private String content;

    /**
     * 字数
     */
    private Integer wordCount;

    /**
     * 剧情概要（本章节的剧情总结，用于后续章节生成时的上下文参考）
     */
    private String plotSummary;

    /**
     * 状态：draft/published
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
