package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 小说大纲实体
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@TableName("novel_outline")
public class NovelOutline {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 整体故事梗概
     */
    private String overallConcept;

    /**
     * 主要主题
     */
    private String mainTheme;

    /**
     * 预计分卷数
     */
    private Integer targetVolumeCount;

    /**
     * 预计章节数
     */
    private Integer targetChapterCount;

    /**
     * 预计总字数
     */
    private Integer targetWordCount;

    /**
     * 故事类型
     */
    private String genre;

    /**
     * 故事基调
     */
    private String tone;

    /**
     * 创作备注
     */
    private String creationNotes;

    /**
     * 状态: draft/confirmed/completed
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
