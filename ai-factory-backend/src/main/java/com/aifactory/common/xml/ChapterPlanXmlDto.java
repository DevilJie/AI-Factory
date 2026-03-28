package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 章节规划XML DTO
 * 用于AI生成章节规划时的XML解析
 *
 * XML结构说明：
 * c: chapters（章节列表根元素）
 * o: chapter（单个章节，用o代表one chapter）
 * n: chapterNumber（章节序号）
 * v: volumeNumber（所属卷号）
 * t: chapterTitle（章节标题）
 * p: plotOutline（情节大纲）
 * e: keyEvents（关键事件）
 * g: chapterGoal（章节目标）
 * w: wordCountTarget（目标字数）
 * s: chapterStartingScene（起点场景）
 * f: chapterEndingScene（终点场景）
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@JacksonXmlRootElement(localName = "c")
public class ChapterPlanXmlDto {

    /**
     * 章节列表
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "o")
    private List<Chapter> chapters;

    /**
     * 章节
     */
    @Data
    public static class Chapter {
        /**
         * 章节序号
         */
        @JacksonXmlProperty(localName = "n")
        private Integer chapterNumber;

        /**
         * 所属卷号
         */
        @JacksonXmlProperty(localName = "v")
        private Integer volumeNumber;

        /**
         * 章节标题
         */
        @JacksonXmlProperty(localName = "t")
        private String chapterTitle;

        /**
         * 情节大纲
         */
        @JacksonXmlProperty(localName = "p")
        private String plotOutline;

        /**
         * 关键事件
         */
        @JacksonXmlProperty(localName = "e")
        private String keyEvents;

        /**
         * 章节目标
         */
        @JacksonXmlProperty(localName = "g")
        private String chapterGoal;

        /**
         * 目标字数
         */
        @JacksonXmlProperty(localName = "w")
        private Integer wordCountTarget;

        /**
         * 起点场景
         */
        @JacksonXmlProperty(localName = "s")
        private String chapterStartingScene;

        /**
         * 终点场景
         */
        @JacksonXmlProperty(localName = "f")
        private String chapterEndingScene;
    }
}
