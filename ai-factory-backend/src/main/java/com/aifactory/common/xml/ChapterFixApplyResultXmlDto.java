package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 章节修复应用结果XML DTO
 * 用于解析AI返回的章节修复结果
 *
 * XML结构说明：
 * F: Fix（修复结果根元素）
 * C: Content（修复后的完整内容）
 * SUM: Summary（修复说明摘要）
 * N: Number（修复数量）
 * R: Report（修复报告）
 * I: Item（修复项）
 * T: Type（问题类型代码）
 * S: Severity（严重程度）
 * O: Original（修改前的原文片段）
 * F: Fixed（修改后的原文片段）- 在内部类中使用localName="F"会与根元素冲突，改用V（Value）
 * E: Explanation（修改的原因和说明）
 *
 * @Author AI Factory
 * @Date 2026-03-03
 */
@Data
@JacksonXmlRootElement(localName = "F")
public class ChapterFixApplyResultXmlDto {

    /**
     * 修复后的完整章节内容
     */
    @JacksonXmlProperty(localName = "C")
    private String fixedContent;

    /**
     * 修复说明摘要
     */
    @JacksonXmlProperty(localName = "SUM")
    private String summary;

    /**
     * 修复数量
     */
    @JacksonXmlProperty(localName = "N")
    private Integer totalFixes;

    /**
     * 修复报告列表
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "I")
    private List<FixItem> fixReport;

    /**
     * 修复项
     */
    @Data
    public static class FixItem {
        /**
         * 问题类型代码
         */
        @JacksonXmlProperty(localName = "T")
        private String type;

        /**
         * 严重程度
         */
        @JacksonXmlProperty(localName = "S")
        private String severity;

        /**
         * 修改前的原文片段
         */
        @JacksonXmlProperty(localName = "O")
        private String original;

        /**
         * 修改后的原文片段
         * 使用V代表Value/Fixed，避免与根元素F冲突
         */
        @JacksonXmlProperty(localName = "V")
        private String fixed;

        /**
         * 修改的原因和说明
         */
        @JacksonXmlProperty(localName = "E")
        private String explanation;
    }
}
