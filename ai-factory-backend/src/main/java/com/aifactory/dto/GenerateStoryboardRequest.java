package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 生成分镜请求DTO
 *
 * @Author CaiZy
 * @Date 2025-01-30
 */
@Data
@Schema(description = "为章节生成分镜的请求参数")
public class GenerateStoryboardRequest {

    /**
     * 章节ID
     */
    @Schema(description = "要生成分镜的章节ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long chapterId;

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /**
     * 是否强制重新生成（如果已存在分镜数据）
     */
    @Schema(description = "是否强制重新生成。如果章节已有分镜数据，设置为true将删除旧数据并重新生成；设置为false则直接返回现有数据",
            example = "false",
            defaultValue = "false")
    private Boolean forceRegenerate = false;
}
