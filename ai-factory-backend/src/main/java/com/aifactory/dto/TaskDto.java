package com.aifactory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务DTO
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Data
@Schema(description = "异步任务信息，包含任务的基本信息、执行状态、进度和步骤列表")
public class TaskDto {

    /**
     * 任务ID
     */
    @Schema(description = "任务ID，唯一标识", example = "1")
    private Long id;

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    /**
     * 任务类型
     */
    @Schema(description = "任务类型，标识任务的业务类型", example = "chapter_generate",
            allowableValues = {"chapter_generate", "outline_generate", "storyboard_generate", "character_analyze", "worldview_build"})
    private String taskType;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称，用于展示的任务标题", example = "生成第一章内容")
    private String taskName;

    /**
     * 状态: pending/running/completed/failed/cancelled
     */
    @Schema(description = "任务状态", example = "running",
            allowableValues = {"pending", "running", "completed", "failed", "cancelled"})
    private String status;

    /**
     * 进度 0-100
     */
    @Schema(description = "任务执行进度百分比，0-100之间的整数", example = "50", minimum = "0", maximum = "100")
    private Integer progress;

    /**
     * 当前步骤描述
     */
    @Schema(description = "当前正在执行的步骤描述", example = "正在分析章节大纲...")
    private String currentStep;

    /**
     * 任务结果（JSON格式）
     */
    @Schema(description = "任务执行结果，JSON格式数据，根据任务类型不同返回不同的结果结构", example = "{\"chapterId\": 1, \"wordCount\": 3000}")
    private Object result;

    /**
     * 错误信息
     */
    @Schema(description = "任务执行失败时的错误信息", example = "AI服务调用超时")
    private String errorMessage;

    /**
     * 创建人ID
     */
    @Schema(description = "创建任务的用户ID", example = "1")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Schema(description = "任务创建时间", example = "2025-01-24 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "任务最后更新时间", example = "2025-01-24 10:35:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 开始时间
     */
    @Schema(description = "任务开始执行时间", example = "2025-01-24 10:30:05")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    @Schema(description = "任务完成时间，包括成功、失败或取消", example = "2025-01-24 10:40:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    /**
     * 步骤列表
     */
    @Schema(description = "任务的执行步骤列表，包含每个步骤的详细状态和进度")
    private List<TaskStepDto> steps;
}
