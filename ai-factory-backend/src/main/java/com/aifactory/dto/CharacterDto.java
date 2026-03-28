package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 人物DTO
 * 用于人物列表展示的简化数据传输对象，包含人物的基本信息
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "人物简要信息DTO，用于人物列表展示")
public class CharacterDto {

    /**
     * 人物ID
     */
    @Schema(description = "人物ID，唯一标识", example = "1")
    private Long id;

    /**
     * 人物名称
     */
    @Schema(description = "人物名称", example = "李逍遥")
    private String name;

    /**
     * 头像URL
     */
    @Schema(description = "人物头像图片URL地址", example = "https://example.com/avatar/lixiaoyao.jpg")
    private String avatar;

    /**
     * 角色定位
     */
    @Schema(description = "角色定位/职责，描述人物在故事中的角色，如：主角、反派、导师、助手等",
            example = "主角",
            allowableValues = {"主角", "女主角", "反派", "配角", "导师", "助手", "对手", "路人"})
    private String role;
}
