package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标记阶段为完成请求
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Data
@Schema(description = "标记情节阶段为已完成的请求参数")
public class MarkStageCompletedRequest {

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /**
     * 分卷ID
     */
    @Schema(description = "所属分卷ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long volumeId;

    /**
     * 情节阶段：introduction(起始阶段), development(发展阶段),
     * climax(高潮阶段), conclusion(结局阶段)
     */
    @Schema(description = "情节阶段，标识故事发展的不同阶段。" +
            "introduction: 起始阶段，介绍背景和人物；" +
            "development: 发展阶段，情节推进和冲突展开；" +
            "climax: 高潮阶段，故事冲突达到顶点；" +
            "conclusion: 结局阶段，故事收尾和总结",
            example = "development",
            allowableValues = {"introduction", "development", "climax", "conclusion"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String plotStage;
}
