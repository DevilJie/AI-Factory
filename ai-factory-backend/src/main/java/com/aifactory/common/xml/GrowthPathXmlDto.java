package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 成长轨迹XML DTO
 * 用于AI生成成长轨迹时的XML解析
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@JacksonXmlRootElement(localName = "Growth")
public class GrowthPathXmlDto {

    /**
     * 能力体系
     */
    @JacksonXmlProperty(localName = "P")
    private String powerSystem;

    /**
     * 成长阶段列表
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S")
    private List<GrowthStage> stages;

    /**
     * 成长阶段
     */
    @Data
    public static class GrowthStage {
        /**
         * 卷号
         */
        @JacksonXmlProperty(localName = "v")
        private Integer volumeNumber;

        /**
         * 章节范围
         */
        @JacksonXmlProperty(localName = "c")
        private String chapterRange;

        /**
         * 能力等级
         */
        @JacksonXmlProperty(localName = "L")
        private String level;

        /**
         * 能力列表
         */
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "A")
        private List<String> abilities;

        /**
         * 里程碑事件
         */
        @JacksonXmlProperty(localName = "M")
        private String milestone;

        /**
         * 关键章节
         */
        @JacksonXmlProperty(localName = "K")
        private String keyChapter;
    }
}
