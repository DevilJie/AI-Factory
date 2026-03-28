package com.aifactory.common.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import java.util.List;

/**
 * 人物弧光XML DTO
 * 用于AI生成人物弧光时的XML解析
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@JacksonXmlRootElement(localName = "Arc")
public class CharacterArcXmlDto {

    /**
     * 初始状态
     */
    @JacksonXmlProperty(localName = "S")
    private String startingState;

    /**
     * 转折事件
     */
    @JacksonXmlProperty(localName = "C")
    private String catalystEvent;

    /**
     * 转化历程（阶段列表）
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "T")
    private List<ArcStage> stages;

    /**
     * 最终状态
     */
    @JacksonXmlProperty(localName = "E")
    private String endState;

    /**
     * 弧光阶段
     */
    @Data
    public static class ArcStage {
        /**
         * 卷号
         */
        @JacksonXmlProperty(localName = "v")
        private Integer volumeNumber;

        /**
         * 阶段描述
         */
        @JacksonXmlProperty(localName = "D")
        private String description;

        /**
         * 内在变化
         */
        @JacksonXmlProperty(localName = "I")
        private String innerChange;

        /**
         * 关键时刻
         */
        @JacksonXmlProperty(localName = "K")
        private String keyMoment;
    }
}
