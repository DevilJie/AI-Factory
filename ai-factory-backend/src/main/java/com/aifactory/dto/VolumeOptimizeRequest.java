package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分卷优化请求DTO
 *
 * @Author AI Factory
 * @Date 2026-03-16
 */
@Data
@Schema(description = "AI优化分卷详情请求")
public class VolumeOptimizeRequest {

    @Schema(description = "分卷ID", required = true, example = "1")
    private Long volumeId;

    @Schema(description = "目标章节数", required = true, example = "50")
    private Integer targetChapterCount;
}
