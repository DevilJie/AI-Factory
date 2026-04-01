package com.aifactory.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "w")
@Schema(description = "世界观LLM解析xml对象")
public class WorldSettingXmlDto {

    @JacksonXmlProperty(localName = "b")
    private String background; // 背景

    @JacksonXmlProperty(localName = "p")
    private Systems systems; // 修炼体系

    // 地理 <g> 标签由 WorldviewTaskStrategy 手动 DOM 解析入库，
    // 因为 Jackson XML 无法处理嵌套同名 <r> 标签（GitHub Issue #294）

    @JacksonXmlProperty(localName = "f")
    private String forces; // 势力

    @JacksonXmlProperty(localName = "l")
    private String timeline; // 历史

    @JacksonXmlProperty(localName = "r")
    private String rules; // 世界法则

    // ======================== 力量体系结构 ========================

    @Data
    public static class Systems {

        @JacksonXmlProperty(localName = "ss")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CultivationSystem> systemList;

        public Systems() {}
    }

    @Data
    public static class CultivationSystem {

        @JacksonXmlProperty(localName = "name")
        private String name; // 体系名称

        @JacksonXmlProperty(localName = "sf")
        private String sourceFrom; // 力量来源

        @JacksonXmlProperty(localName = "cr")
        private String coreResource; // 核心资源

        @JacksonXmlProperty(localName = "cm")
        private String cultivationMethod; // 修炼方法

        @JacksonXmlProperty(localName = "d")
        private String description; // 描述

        @JacksonXmlProperty(localName = "lls")
        private Levels levels; // 境界等级
    }

    @Data
    public static class Levels {

        @JacksonXmlProperty(localName = "ll")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CultivationLevel> levelList;
    }

    @Data
    public static class CultivationLevel {

        @JacksonXmlProperty(localName = "ln")
        private String levelName; // 境界名称

        @JacksonXmlProperty(localName = "dd")
        private String description; // 描述

        @JacksonXmlProperty(localName = "bc")
        private String breakthroughCondition; // 突破条件

        @JacksonXmlProperty(localName = "lsp")
        private String lifespan; // 寿元

        @JacksonXmlProperty(localName = "pr")
        private String powerRange; // 力量范围

        @JacksonXmlProperty(localName = "la")
        private String landmarkAbility; // 标志能力

        @JacksonXmlProperty(localName = "steps")
        private Steps steps; // 细分阶段
    }

    @Data
    public static class Steps {

        @JacksonXmlProperty(localName = "step")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<String> stepList;
    }
}
