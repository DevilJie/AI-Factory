package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 伏笔创建DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "伏笔创建请求DTO，用于创建新的伏笔")
public class ForeshadowingCreateDto {

    /**
     * 伏笔标题
     */
    @Schema(description = "伏笔标题，简短描述伏笔内容", example = "神秘老人的预言", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "伏笔标题不能为空")
    private String title;

    /**
     * 伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)
     */
    @Schema(description = "伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)",
            allowableValues = {"character", "item", "event", "secret"},
            example = "secret",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "伏笔类型不能为空")
    private String type;

    /**
     * 伏笔描述
     */
    @Schema(description = "伏笔详细描述，说明伏笔的具体内容和作用",
            example = "第5章神秘老人对主角说的预言，暗示主角未来的命运",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "伏笔描述不能为空")
    private String description;

    /**
     * 布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)
     */
    @Schema(description = "布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)。明线表示读者可见的线索，暗线表示隐藏的线索",
            allowableValues = {"bright1", "bright2", "bright3", "dark"},
            example = "dark")
    private String layoutType;

    /**
     * 埋伏笔的章节
     */
    @Schema(description = "埋设伏笔的章节号，必填字段",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "埋伏笔的章节不能为空")
    private Integer plantedChapter;

    /**
     * 埋设伏笔的分卷编号
     */
    @Schema(description = "埋设伏笔的分卷编号", example = "1")
    private Integer plantedVolume;

    /**
     * 计划填坑的章节
     */
    @Schema(description = "计划填坑（揭示伏笔）的章节号，可选",
            example = "50")
    private Integer plannedCallbackChapter;

    /**
     * 计划回收伏笔的分卷编号
     */
    @Schema(description = "计划回收伏笔的分卷编号", example = "2")
    private Integer plannedCallbackVolume;

    /**
     * 优先级，数值越大优先级越高
     */
    @Schema(description = "优先级，数值越大优先级越高，用于排序和提醒",
            example = "5")
    private Integer priority;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息，可记录填坑思路或注意事项",
            example = "需要在高潮前揭示，增强戏剧效果")
    private String notes;
}
