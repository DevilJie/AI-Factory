package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 章节修复检查结果XML DTO
 * 用于解析AI返回的章节问题检查结果
 *
 * XML结构说明：
 * R: Result（检查结果根元素）
 * I: Issue（问题项）
 * T: Type（问题类型代码：logic/continuity/character/worldview/timeline/foreshadow/repetition/setting）
 * S: Severity（严重程度：high/medium/low）
 * L: Location（位置描述）
 * O: Original（原文片段）
 * D: Description（问题描述）
 * G: Suggestion（修改建议）
 * SUM: Summary（检查总结）
 * N: Number（问题总数）
 * H: HasSevereIssues（是否存在严重问题）
 *
 * @Author AI Factory
 * @Date 2026-03-03
 */
@Data
@JacksonXmlRootElement(localName = "R")
public class ChapterFixCheckResultXmlDto {

    /**
     * 问题列表
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "I")
    private List<Issue> issues;

    /**
     * 检查总结
     */
    @JacksonXmlProperty(localName = "SUM")
    private String summary;

    /**
     * 问题总数
     */
    @JacksonXmlProperty(localName = "N")
    private Integer totalIssues;

    /**
     * 是否存在严重问题
     */
    @JacksonXmlProperty(localName = "H")
    private Boolean hasSevereIssues;

    /**
     * 问题项
     */
    @Data
    public static class Issue {
        /**
         * 问题类型代码
         * logic: 剧情逻辑
         * continuity: 连贯性
         * character: 人物一致性
         * worldview: 世界观一致性
         * timeline: 时间线
         * foreshadow: 伏笔管理
         * repetition: 重复内容
         * setting: 设定矛盾
         */
        @JacksonXmlProperty(localName = "T")
        private String type;

        /**
         * 严重程度：high/medium/low
         */
        @JacksonXmlProperty(localName = "S")
        private String severity;

        /**
         * 位置描述
         */
        @JacksonXmlProperty(localName = "L")
        private String location;

        /**
         * 原文片段
         */
        @JacksonXmlProperty(localName = "O")
        private String originalText;

        /**
         * 问题描述
         */
        @JacksonXmlProperty(localName = "D")
        private String description;

        /**
         * 修改建议
         */
        @JacksonXmlProperty(localName = "G")
        private String suggestion;
    }
}
