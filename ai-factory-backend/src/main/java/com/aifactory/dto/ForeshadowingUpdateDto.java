package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 伏笔更新DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "伏笔更新请求DTO，用于更新现有伏笔的信息")
public class ForeshadowingUpdateDto {

    /**
     * 伏笔标题
     */
    @Schema(description = "伏笔标题，简短描述伏笔内容",
            example = "神秘老人的预言",
            requiredMode = Schema.RequiredMode.REQUIRED)
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
     * 计划填坑的章节
     */
    @Schema(description = "计划填坑（揭示伏笔）的章节号",
            example = "50")
    private Integer plannedCallbackChapter;

    /**
     * 实际填坑的章节
     */
    @Schema(description = "实际填坑（揭示伏笔）的章节号，完成后填写",
            example = "52")
    private Integer actualCallbackChapter;

    /**
     * 状态：pending(未填回)/in_progress(进行中)/completed(已填回)
     */
    @Schema(description = "伏笔状态：pending(未填回)/in_progress(进行中)/completed(已填回)",
            allowableValues = {"pending", "in_progress", "completed"},
            example = "in_progress")
    private String status;

    /**
     * 优先级，数值越大优先级越高
     */
    @Schema(description = "优先级，数值越大优先级越高，用于排序和提醒",
            example = "8")
    private Integer priority;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息，可记录填坑思路或注意事项",
            example = "已在52章揭示，效果良好")
    private String notes;
}
