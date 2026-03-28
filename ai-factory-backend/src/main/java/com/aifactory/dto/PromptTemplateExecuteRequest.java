package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 执行提示词模板请求DTO
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@Schema(description = "执行提示词模板的请求参数。将变量值注入模板，生成最终的提示词。")
public class PromptTemplateExecuteRequest {

    /**
     * 模板编码
     */
    @Schema(description = "模板编码，指定要执行的模板。" +
            "系统会根据编码查找对应的模板，并将变量注入后返回完整的提示词",
            example = "llm_chapter_generate_standard",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 变量值Map
     * 例如：{"worldview": "玄幻", "chapterTitle": "第一章"}
     */
    @Schema(description = "变量值映射，key为变量名，value为变量值。" +
            "变量名需与模板中定义的变量对应。" +
            "必填变量必须提供值，可选变量可以省略（将使用默认值）",
            example = "{\"worldview\": \"修仙世界，分为凡人界和仙界\", \"character\": \"主角林风，资质平平但意志坚定\", \"plot\": \"林风在山中发现一块神秘石碑，开启修炼之路\"}")
    private Map<String, Object> variables;
}
