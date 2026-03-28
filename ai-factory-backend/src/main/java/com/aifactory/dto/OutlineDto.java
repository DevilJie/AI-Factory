package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 大纲DTO
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Data
@Schema(description = "大纲信息DTO，包含小说整体规划、分卷和章节信息")
public class OutlineDto {

    @Schema(description = "大纲ID", example = "1")
    private Long id;

    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    @Schema(description = "整体概念，小说的核心创意和故事梗概", example = "一个少年意外获得修仙传承，踏上逆天改命之路")
    private String overallConcept;

    @Schema(description = "主题思想，小说想要表达的核心主题", example = "坚持与成长，命运与选择")
    private String mainTheme;

    @Schema(description = "目标分卷数", example = "5")
    private Integer targetVolumeCount;

    @Schema(description = "目标章节数", example = "200")
    private Integer targetChapterCount;

    @Schema(description = "目标总字数（万字）", example = "300")
    private Integer targetWordCount;

    @Schema(description = "小说类型/流派", example = "玄幻")
    private String genre;

    @Schema(description = "故事基调/风格", example = "热血、励志")
    private String tone;

    @Schema(description = "创作备注", example = "注意保持主角性格的一致性")
    private String creationNotes;

    @Schema(description = "大纲状态：draft(草稿)/generating(生成中)/completed(已完成)", example = "completed")
    private String status;

    @Schema(description = "创建时间", example = "2025-01-23T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-01-23T15:45:00")
    private LocalDateTime updateTime;

    /**
     * 分卷列表
     */
    @Schema(description = "分卷规划列表，包含各卷的详细信息")
    private List<VolumePlanDto> volumes;

    /**
     * 章节规划列表
     */
    @Schema(description = "章节规划列表，包含各章节的详细大纲")
    private List<ChapterPlanDto> chapters;
}
