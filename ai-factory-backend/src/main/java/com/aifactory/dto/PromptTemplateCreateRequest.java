package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建提示词模板请求DTO
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@Schema(description = "创建提示词模板的请求参数")
public class PromptTemplateCreateRequest {

    /**
     * 模板编码（唯一，语义化）
     * 格式：{service}_{scenario}_{variant}
     * 例如：llm_chapter_generate_standard
     */
    @Schema(description = "模板编码，唯一的语义化标识符。" +
            "格式：{service}_{scenario}_{variant}。" +
            "service: 服务类型(llm/image/video/common)；" +
            "scenario: 使用场景(chapter_generate/outline_generate等)；" +
            "variant: 变体名称(standard/detailed/simple等)",
            example = "llm_chapter_generate_standard",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称，用于展示的模板标题", example = "章节生成标准模板", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * 服务类型：llm/image/video/common
     */
    @Schema(description = "服务类型，标识模板适用的AI服务。" +
            "llm: 大语言模型服务，用于文本生成；" +
            "image: 图像生成服务，用于AI绘图；" +
            "video: 视频生成服务，用于AI视频；" +
            "common: 通用服务，可跨服务使用",
            example = "llm",
            allowableValues = {"llm", "image", "video", "common"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务类型不能为空")
    private String serviceType;

    /**
     * 使用场景：chapter_generate/outline_generate等
     */
    @Schema(description = "使用场景，标识模板的业务场景。" +
            "chapter_generate: 章节内容生成；" +
            "outline_generate: 大纲生成；" +
            "storyboard_generate: 分镜生成；" +
            "character_analyze: 角色分析；" +
            "worldview_build: 世界观构建；" +
            "summary_generate: 摘要生成",
            example = "chapter_generate",
            allowableValues = {"chapter_generate", "outline_generate", "storyboard_generate", "character_analyze", "worldview_build", "summary_generate"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "使用场景不能为空")
    private String scenario;

    /**
     * 模板内容（支持{var}占位符）
     */
    @Schema(description = "模板内容，支持{var}格式的占位符变量。" +
            "变量将被运行时传入的实际值替换。" +
            "例如：请根据以下设定生成章节内容：\n世界观：{worldview}\n角色：{character}\n情节：{plot}",
            example = "请根据以下设定生成章节内容：\n世界观：{worldview}\n角色：{character}\n情节：{plot}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    /**
     * 变量定义JSON数组
     * 例如：[{"name":"worldview","type":"string","desc":"世界观设定","required":true}]
     */
    @Schema(description = "变量定义，JSON数组格式。每个变量包含：" +
            "name: 变量名，与模板内容中的{var}对应；" +
            "type: 变量类型(string/number/boolean/object/array)；" +
            "desc: 变量描述；" +
            "required: 是否必填；" +
            "defaultValue: 默认值(可选)",
            example = "[{\"name\":\"worldview\",\"type\":\"string\",\"desc\":\"世界观设定\",\"required\":true},{\"name\":\"character\",\"type\":\"string\",\"desc\":\"角色信息\",\"required\":true},{\"name\":\"plot\",\"type\":\"string\",\"desc\":\"情节大纲\",\"required\":false}]")
    private String variableDefinitions;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述，详细说明模板的用途和特点", example = "用于生成小说章节内容的标准模板，支持世界观、角色、情节等变量注入")
    private String description;

    /**
     * 标签，逗号分隔
     */
    @Schema(description = "标签，用于分类和搜索，多个标签用逗号分隔", example = "章节,生成,标准")
    private String tags;
}
