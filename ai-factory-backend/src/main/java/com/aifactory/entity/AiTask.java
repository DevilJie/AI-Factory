package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI任务实体
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Data
@TableName("ai_task")
public class AiTask {

    /**
     * 任务ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务配置（JSON格式）
     */
    private String configJson;

    /**
     * 状态: pending/running/completed/failed/cancelled
     */
    private String status;

    /**
     * 进度 0-100
     */
    private Integer progress;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 任务结果（JSON格式）
     */
    private String resultJson;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
}
