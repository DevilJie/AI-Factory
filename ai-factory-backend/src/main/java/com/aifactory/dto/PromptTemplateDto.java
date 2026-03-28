package com.aifactory.dto;

import com.aifactory.entity.AiPromptTemplateVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词模板DTO
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@Schema(description = "提示词模板信息，用于管理和执行AI提示词模板")
public class PromptTemplateDto {

    /**
     * 模板ID
     */
    @Schema(description = "模板ID，唯一标识", example = "1")
    private Long id;

    /**
     * 模板编码
     */
    @Schema(description = "模板编码，唯一的语义化标识符，格式为{service}_{scenario}_{variant}",
            example = "llm_chapter_generate_standard")
    private String templateCode;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称，用于展示的模板标题", example = "章节生成标准模板")
    private String templateName;

    /**
     * 服务类型
     */
    @Schema(description = "服务类型，标识模板适用的AI服务",
            example = "llm",
            allowableValues = {"llm", "image", "video", "common"})
    private String serviceType;

    /**
     * 使用场景
     */
    @Schema(description = "使用场景，标识模板的业务场景",
            example = "chapter_generate",
            allowableValues = {"chapter_generate", "outline_generate", "storyboard_generate", "character_analyze", "worldview_build", "summary_generate"})
    private String scenario;

    /**
     * 当前版本
     */
    @Schema(description = "当前生效的模板版本信息，包含模板内容和变量定义")
    private AiPromptTemplateVersion currentVersion;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述，详细说明模板的用途和特点", example = "用于生成小说章节内容的标准模板，支持世界观、角色、情节等变量注入")
    private String description;

    /**
     * 标签
     */
    @Schema(description = "标签，用于分类和搜索，多个标签用逗号分隔", example = "章节,生成,标准")
    private String tags;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用，禁用后模板不可使用", example = "true")
    private Boolean isActive;

    /**
     * 是否系统内置
     */
    @Schema(description = "是否系统内置，内置模板不可删除", example = "true")
    private Boolean isSystem;

    /**
     * 创建时间
     */
    @Schema(description = "模板创建时间", example = "2025-02-05T10:30:00")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Schema(description = "模板最后更新时间", example = "2025-02-05T15:20:00")
    private LocalDateTime updatedTime;
}
