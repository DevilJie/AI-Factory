package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Dashboard统计数据DTO
 *
 * @Author CaiZy
 * @Date 2026-03-15
 */
@Data
@Schema(description = "Dashboard统计数据")
public class DashboardStatsDto {

    @Schema(description = "项目总数", example = "12")
    private Long projectCount;

    @Schema(description = "章节总数", example = "48")
    private Long chapterCount;

    @Schema(description = "角色总数", example = "156")
    private Long characterCount;

    @Schema(description = "总字数（中文字符数）", example = "125000")
    private Long totalWordCount;
}
