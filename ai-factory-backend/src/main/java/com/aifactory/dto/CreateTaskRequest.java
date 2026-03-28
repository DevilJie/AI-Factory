package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 创建任务请求DTO
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Data
@Schema(description = "创建异步任务的请求参数")
public class CreateTaskRequest {

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /**
     * 任务类型
     */
    @Schema(description = "任务类型，决定任务的具体执行逻辑",
            example = "chapter_generate",
            allowableValues = {"chapter_generate", "outline_generate", "storyboard_generate", "character_analyze", "worldview_build"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称，用于展示的任务标题", example = "生成第一章内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskName;

    /**
     * 任务配置（JSON格式）
     */
    @Schema(description = "任务配置参数，根据任务类型不同需要不同的配置项。" +
            "chapter_generate需要：volumeId(分卷ID)、chapterId(章节ID)、plotStage(情节阶段)；" +
            "outline_generate需要：volumeId(分卷ID)；" +
            "storyboard_generate需要：chapterId(章节ID)",
            example = "{\"volumeId\": 1, \"chapterId\": 1, \"plotStage\": \"introduction\"}")
    private Map<String, Object> config;
}
