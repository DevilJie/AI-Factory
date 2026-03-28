package com.aifactory.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 章节规划XML DTO
 *
 * 使用Jackson XML注解将LLM返回的章节规划XML解析为POJO
 *
 * XML格式示例：
 * <pre>
 * {@code
 * <c>
 *   <o>
 *     <n>1</n>
 *     <t><![CDATA[章节标题]]></t>
 *     <p><![CDATA[本章情节大纲（100-200字）]]></p>
 *     <e><![CDATA[关键事件1；关键事件2；关键事件3]]></e>
 *     <g><![CDATA[本章要达成的目标]]></g>
 *     <w>3000</w>
 *     <s><![CDATA[章节起点场景（地点、时间、状态）]]></s>
 *     <ed><![CDATA[章节终点场景（地点、时间、状态）]]></ed>
 *   </o>
 *   <o>
 *     <n>2</n>
 *     ...
 *   </o>
 * </c>
 * }
 * </pre>
 *
 * 标签说明：
 * - c: chapters（章节列表根元素）
 * - o: one chapter（单个章节）
 * - n: number（章节编号）
 * - t: title（章节标题）
 * - p: plot（情节大纲）
 * - e: events（关键事件）
 * - g: goal（章节目标）
 * - w: word count（目标字数）
 * - s: start（起点场景）
 * - ed: end（终点场景）
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@JacksonXmlRootElement(localName = "c")
@Schema(description = "章节规划XML解析对象，用于解析LLM返回的章节规划XML数据")
public class ChapterPlanXmlDto {

    /**
     * 章节列表（可能有多个<o>标签）
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "o")
    @Schema(description = "章节规划列表，包含一个或多个章节规划项")
    private List<ChapterPlanItem> chapters;

    /**
     * 单个章节规划项
     */
    @Data
    @Schema(description = "单个章节规划项，对应XML中的<o>标签")
    public static class ChapterPlanItem {

        /**
         * 章节编号
         */
        @JacksonXmlProperty(localName = "n")
        @Schema(description = "章节编号，对应XML中的<n>标签", example = "1")
        private Integer chapterNumber;

        /**
         * 章节标题
         */
        @JacksonXmlProperty(localName = "t")
        @Schema(description = "章节标题，对应XML中的<t>标签", example = "初入江湖")
        private String chapterTitle;

        /**
         * 情节大纲（100-200字）
         */
        @JacksonXmlProperty(localName = "p")
        @Schema(description = "情节大纲，100-200字，对应XML中的<p>标签", example = "主角李云初入江湖...")
        private String plotOutline;

        /**
         * 关键事件（用分号分隔）
         */
        @JacksonXmlProperty(localName = "e")
        @Schema(description = "关键事件，用分号分隔多个事件，对应XML中的<e>标签", example = "酒楼偶遇;获得线索;遇袭")
        private String keyEvents;

        /**
         * 章节目标
         */
        @JacksonXmlProperty(localName = "g")
        @Schema(description = "章节目标，本章要达成的叙事目的，对应XML中的<g>标签", example = "建立主角形象")
        private String chapterGoal;

        /**
         * 目标字数
         */
        @JacksonXmlProperty(localName = "w")
        @Schema(description = "目标字数，AI生成时的字数参考，对应XML中的<w>标签", example = "3000")
        private Integer wordCountTarget;

        /**
         * 起点场景
         */
        @JacksonXmlProperty(localName = "s")
        @Schema(description = "章节起点场景，描述章节开始时的场景设置，对应XML中的<s>标签", example = "地点：江南城；时间：傍晚")
        private String chapterStartingScene;

        /**
         * 终点场景
         */
        @JacksonXmlProperty(localName = "f")
        @Schema(description = "章节终点场景，描述章节结束时的场景状态，对应XML中的<f>标签", example = "地点：城外；时间：深夜")
        private String chapterEndingScene;
    }
}
