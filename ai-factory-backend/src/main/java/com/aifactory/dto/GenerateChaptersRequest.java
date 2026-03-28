package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI生成章节计划请求
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Data
@Schema(description = "AI生成章节计划的请求参数")
public class GenerateChaptersRequest {

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /**
     * 分卷ID
     */
    @Schema(description = "所属分卷ID，指定要生成章节计划的分卷", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long volumeId;

    /**
     * 情节阶段：introduction(起始阶段), development(发展阶段),
     * climax(高潮阶段), conclusion(结局阶段)
     */
    @Schema(description = "情节阶段，标识要生成的章节所属的故事发展阶段。" +
            "introduction: 起始阶段，介绍背景和人物；" +
            "development: 发展阶段，情节推进和冲突展开；" +
            "climax: 高潮阶段，故事冲突达到顶点；" +
            "conclusion: 结局阶段，故事收尾和总结",
            example = "introduction",
            allowableValues = {"introduction", "development", "climax", "conclusion"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String plotStage;
}
