package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI任务步骤实体
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Data
@TableName("ai_task_step")
public class AiTaskStep {

    /**
     * 步骤ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 步骤顺序
     */
    private Integer stepOrder;

    /**
     * 步骤名称
     */
    private String stepName;

    /**
     * 步骤类型
     */
    private String stepType;

    /**
     * 步骤配置（JSON格式）
     */
    private String configJson;

    /**
     * 状态: pending/running/completed/failed/skipped
     */
    private String status;

    /**
     * 进度 0-100
     */
    private Integer progress;

    /**
     * 步骤结果（JSON格式）
     */
    private String resultJson;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
