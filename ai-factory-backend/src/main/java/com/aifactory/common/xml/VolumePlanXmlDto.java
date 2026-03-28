package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 分卷规划XML DTO
 * 用于AI生成分卷时的XML解析
 *
 * XML结构说明：
 * V: Volumes（分卷列表根元素）
 * v: volume（单个分卷）
 * N: Number（卷号）
 * T: Title（标题）
 * Z: 主题（Theme）
 * F: 矛盾（Conflict）
 * P: 情节（Plot）
 * D: 描述（Description）
 * E: 事件（Events）
 * L: 时间线（Timeline）
 * G: 目标章节数（Goal/Count）
 * B: 备注（Notes）
 * O: 核心目标（Objective）
 * H: 高潮（Climax）
 * R: 收尾（Resolution）
 * A: 人物（Actors/Characters）- JSON数组字符串
 * Y: 伏笔（Yield/Foreshadowing）- JSON数组字符串
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@JacksonXmlRootElement(localName = "V")
public class VolumePlanXmlDto {

    /**
     * 分卷列表
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "v")
    private List<Volume> volumes;

    /**
     * 分卷
     */
    @Data
    public static class Volume {
        /**
         * 卷序号
         */
        @JacksonXmlProperty(localName = "N")
        private Integer number;

        /**
         * 卷标题
         */
        @JacksonXmlProperty(localName = "T")
        private String title;

        /**
         * 主题（Theme）
         */
        @JacksonXmlProperty(localName = "Z")
        private String theme;

        /**
         * 主要矛盾（Conflict）
         */
        @JacksonXmlProperty(localName = "F")
        private String conflict;

        /**
         * 情节走向（Plot）
         */
        @JacksonXmlProperty(localName = "P")
        private String plotArc;

        /**
         * 本卷简介（Description）
         */
        @JacksonXmlProperty(localName = "D")
        private String description;

        /**
         * 关键事件（Events）- 分号分隔的字符串
         */
        @JacksonXmlProperty(localName = "E")
        private String keyEvents;

        /**
         * 时间线设定（Timeline）
         */
        @JacksonXmlProperty(localName = "L")
        private String timeline;

        /**
         * 目标章节数（Goal/Count）
         */
        @JacksonXmlProperty(localName = "G")
        private Integer chapterCount;

        /**
         * 备注信息（Notes）
         */
        @JacksonXmlProperty(localName = "B")
        private String notes;

        /**
         * 核心目标（Objective）
         */
        @JacksonXmlProperty(localName = "O")
        private String objective;

        /**
         * 高潮事件（Climax）
         */
        @JacksonXmlProperty(localName = "H")
        private String climax;

        /**
         * 收尾描述（Resolution）
         */
        @JacksonXmlProperty(localName = "R")
        private String resolution;

        /**
         * 人物列表（Actors/Characters）- JSON数组字符串
         * 例如：["角色1", "角色2"]
         */
        @JacksonXmlProperty(localName = "A")
        private String characters;

        /**
         * 伏笔列表（Yield/Foreshadowing）- JSON数组字符串
         * 例如：["伏笔1：描述", "伏笔2：描述"]
         */
        @JacksonXmlProperty(localName = "Y")
        private String foreshadowings;
    }
}
