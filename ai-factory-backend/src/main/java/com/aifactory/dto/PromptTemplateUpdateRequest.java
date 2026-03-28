package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新提示词模板请求DTO
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@Schema(description = "更新提示词模板的请求参数。更新模板会创建新版本，保留历史版本记录。")
public class PromptTemplateUpdateRequest {

    /**
     * 模板内容（支持{var}占位符）
     */
    @Schema(description = "模板内容，支持{var}格式的占位符变量。" +
            "变量将被运行时传入的实际值替换。" +
            "更新模板内容会自动创建新版本",
            example = "请根据以下设定生成章节内容：\n世界观：{worldview}\n角色：{character}\n情节：{plot}\n风格：{style}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    /**
     * 变量定义JSON数组
     */
    @Schema(description = "变量定义，JSON数组格式。每个变量包含：" +
            "name: 变量名，与模板内容中的{var}对应；" +
            "type: 变量类型(string/number/boolean/object/array)；" +
            "desc: 变量描述；" +
            "required: 是否必填；" +
            "defaultValue: 默认值(可选)",
            example = "[{\"name\":\"worldview\",\"type\":\"string\",\"desc\":\"世界观设定\",\"required\":true},{\"name\":\"character\",\"type\":\"string\",\"desc\":\"角色信息\",\"required\":true},{\"name\":\"plot\",\"type\":\"string\",\"desc\":\"情节大纲\",\"required\":false},{\"name\":\"style\",\"type\":\"string\",\"desc\":\"写作风格\",\"required\":false,\"defaultValue\":\"轻松幽默\"}]")
    private String variableDefinitions;

    /**
     * 版本说明
     */
    @Schema(description = "版本说明，描述本次更新的内容和原因", example = "增加写作风格变量，优化生成效果")
    private String versionComment;
}
