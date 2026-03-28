package com.aifactory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务步骤DTO
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Data
@Schema(description = "任务执行步骤信息，记录任务中每个子步骤的执行状态和结果")
public class TaskStepDto {

    /**
     * 步骤ID
     */
    @Schema(description = "步骤ID，唯一标识", example = "1")
    private Long id;

    /**
     * 任务ID
     */
    @Schema(description = "所属任务ID", example = "1")
    private Long taskId;

    /**
     * 步骤顺序
     */
    @Schema(description = "步骤执行顺序，从1开始递增", example = "1")
    private Integer stepOrder;

    /**
     * 步骤名称
     */
    @Schema(description = "步骤名称，用于展示的步骤标题", example = "分析章节大纲")
    private String stepName;

    /**
     * 步骤类型
     */
    @Schema(description = "步骤类型，标识步骤的业务类型", example = "analyze",
            allowableValues = {"analyze", "generate", "validate", "save", "notify"})
    private String stepType;

    /**
     * 状态: pending/running/completed/failed/skipped
     */
    @Schema(description = "步骤执行状态", example = "completed",
            allowableValues = {"pending", "running", "completed", "failed", "skipped"})
    private String status;

    /**
     * 进度 0-100
     */
    @Schema(description = "步骤执行进度百分比，0-100之间的整数", example = "100", minimum = "0", maximum = "100")
    private Integer progress;

    /**
     * 步骤结果（JSON格式）
     */
    @Schema(description = "步骤执行结果，JSON格式数据", example = "{\"outlineNodes\": 5}")
    private Object result;

    /**
     * 错误信息
     */
    @Schema(description = "步骤执行失败时的错误信息", example = "大纲解析失败")
    private String errorMessage;

    /**
     * 开始时间
     */
    @Schema(description = "步骤开始执行时间", example = "2025-01-24 10:30:10")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    @Schema(description = "步骤完成时间", example = "2025-01-24 10:30:30")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    @Schema(description = "步骤记录创建时间", example = "2025-01-24 10:30:05")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "步骤记录最后更新时间", example = "2025-01-24 10:30:30")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
