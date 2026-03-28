package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分卷规划实体
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@TableName("novel_volume_plan")
public class NovelVolumePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联大纲ID
     */
    private Long outlineId;

    /**
     * 卷序号
     */
    private Integer volumeNumber;

    /**
     * 卷标题
     */
    private String volumeTitle;

    /**
     * 本卷主旨
     */
    private String volumeTheme;

    /**
     * 主要冲突
     */
    private String mainConflict;

    /**
     * 情节走向
     */
    private String plotArc;

    /**
     * 本卷简介
     */
    private String volumeDescription;

    /**
     * 关键事件
     */
    private String keyEvents;

    /**
     * 时间线设定
     */
    private String timelineSetting;

    /**
     * 预计章节数
     */
    private Integer targetChapterCount;

    /**
     * 分卷备注
     */
    private String volumeNotes;

    /**
     * 核心目标
     */
    private String coreGoal;

    /**
     * 高潮事件
     */
    private String climax;

    /**
     * 收尾描述
     */
    private String ending;

    /**
     * 本卷新增人物（JSON）
     */
    private String newCharacters;

    /**
     * 阶段伏笔（JSON）
     */
    private String stageForeshadowings;

    /**
     * 状态: planned/in_progress/completed
     */
    private String status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 该分卷是否已完成（所有4个阶段都完成）
     */
    private Boolean volumeCompleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
