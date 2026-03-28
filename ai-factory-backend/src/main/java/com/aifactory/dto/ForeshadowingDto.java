package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伏笔DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "伏笔信息DTO，包含伏笔的完整信息")
public class ForeshadowingDto {

    /**
     * 伏笔ID
     */
    @Schema(description = "伏笔ID", example = "100")
    private Long id;

    /**
     * 项目ID
     */
    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    /**
     * 伏笔标题
     */
    @Schema(description = "伏笔标题，简短描述伏笔内容", example = "神秘老人的预言")
    private String title;

    /**
     * 伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)
     */
    @Schema(description = "伏笔类型",
            allowableValues = {"character", "item", "event", "secret"},
            example = "secret")
    private String type;

    /**
     * 伏笔描述
     */
    @Schema(description = "伏笔详细描述", example = "第5章神秘老人对主角说的预言，暗示主角未来的命运")
    private String description;

    /**
     * 布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)
     */
    @Schema(description = "布局类型，明线表示读者可见的线索，暗线表示隐藏的线索",
            allowableValues = {"bright1", "bright2", "bright3", "dark"},
            example = "dark")
    private String layoutType;

    /**
     * 埋伏笔的章节
     */
    @Schema(description = "埋设伏笔的章节号", example = "5")
    private Integer plantedChapter;

    /**
     * 计划填坑的章节
     */
    @Schema(description = "计划填坑（揭示伏笔）的章节号", example = "50")
    private Integer plannedCallbackChapter;

    /**
     * 实际填坑的章节
     */
    @Schema(description = "实际填坑（揭示伏笔）的章节号，完成后填写", example = "52")
    private Integer actualCallbackChapter;

    /**
     * 状态：pending(未填回)/in_progress(进行中)/completed(已填回)
     */
    @Schema(description = "伏笔状态",
            allowableValues = {"pending", "in_progress", "completed"},
            example = "pending")
    private String status;

    /**
     * 优先级，数值越大优先级越高
     */
    @Schema(description = "优先级，数值越大优先级越高，用于排序", example = "5")
    private Integer priority;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息", example = "需要在高潮前揭示，增强戏剧效果")
    private String notes;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-01-22T10:30:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2025-01-22T15:45:00")
    private LocalDateTime updateTime;
}
