package com.aifactory.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 章节角色提取XML DTO
 *
 * 使用Jackson XML注解将LLM返回的XML字符串解析为POJO
 * 用于从章节内容中提取详细的角色信息
 *
 * XML格式示例：
 * <pre>
 * {@code
 * <M>
 *   <C>
 *     <N>角色名</N>
 *     <T>角色类型</T>
 *     <G>性别</G>
 *     <A>年龄</A>
 *     <S>本章状态变化</S>
 *     <P>性格特点</P>
 *     <F>外貌特征</F>
 *     <V>
 *       <SYS>修炼体系名称</SYS>
 *       <LV>当前境界等级</LV>
 *       <CH>本章境界变化</CH>
 *     </V>
 *     <AP>外貌/装扮变化</AP>
 *     <PR>性格展现</PR>
 *     <AB>能力展现</AB>
 *     <CD>角色成长</CD>
 *     <DG>核心对话摘要</DG>
 *   </C>
 * </M>
 * }
 * </pre>
 *
 * 标签说明：
 * - M: Main（根元素）
 * - C: Character（角色详情，多个）
 *   - N: Name（角色名称）
 *   - T: Type（角色类型：protagonist/supporting/antagonist/npc）
 *   - G: Gender（性别：male/female/other）
 *   - A: Age（年龄）
 *   - S: Status（本章状态变化）
 *   - P: Personality（性格特点，逗号分隔）
 *   - F: Feature（外貌特征）
 *   - V: cultivation System（修炼体系，多个）
 *     - SYS: System（修炼体系名称）
 *     - LV: Level（当前境界等级）
 *     - CH: Change（本章境界变化）
 *   - AP: Appearance change（外貌/装扮变化）
 *   - PR: Personality reveal（性格展现）
 *   - AB: Ability（能力展现）
 *   - CD: Character Development（角色成长）
 *   - DG: DialoG（核心对话摘要）
 *
 * @Author AI-Factory
 * @Date 2026-03-18
 */
@Data
@JacksonXmlRootElement(localName = "M")
@Schema(description = "章节角色提取XML解析对象，用于解析LLM返回的章节角色详细信息XML数据")
public class ChapterCharacterExtractXmlDto {

    /**
     * 角色详情列表（可能有多个角色）
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "C")
    @Schema(description = "角色详情列表，对应XML中的多个<C>标签，包含完整角色提取信息")
    private List<CharacterExtractDto> characters;

    /**
     * 角色提取详情内部类
     *
     * XML格式示例：
     * <C>
     *   <N>角色名</N>
     *   <T>角色类型</T>
     *   <G>性别</G>
     *   <A>年龄</A>
     *   <S>本章状态变化</S>
     *   <P>性格特点</P>
     *   <F>外貌特征</F>
     *   <V>
     *     <SYS>修炼体系名称</SYS>
     *     <LV>当前境界等级</LV>
     *     <CH>本章境界变化</CH>
     *   </V>
     *   <AP>外貌/装扮变化</AP>
     *   <PR>性格展现</PR>
     *   <AB>能力展现</AB>
     *   <CD>角色成长</CD>
     *   <DG>核心对话摘要</DG>
     * </C>
     */
    @Data
    @Schema(description = "角色提取详情，对应XML中的<C>标签，包含完整的角色提取信息")
    public static class CharacterExtractDto {

        /**
         * 角色名称
         */
        @JacksonXmlProperty(localName = "N")
        @Schema(description = "角色名称，对应XML中的<N>标签", example = "李云")
        private String name;

        /**
         * 角色类型：protagonist(主角)/supporting(配角)/antagonist(反派)/npc(路人)
         */
        @JacksonXmlProperty(localName = "T")
        @Schema(description = "角色类型，对应XML中的<T>标签",
                example = "protagonist",
                allowableValues = {"protagonist", "supporting", "antagonist", "npc"})
        private String roleType;

        /**
         * 性别：male(男)/female(女)/other(其他)
         */
        @JacksonXmlProperty(localName = "G")
        @Schema(description = "性别，对应XML中的<G>标签",
                example = "male",
                allowableValues = {"male", "female", "other"})
        private String gender;

        /**
         * 年龄（可能为空或数字字符串）
         */
        @JacksonXmlProperty(localName = "A")
        @Schema(description = "年龄，对应XML中的<A>标签，可能为空或数字字符串",
                example = "18")
        private String age;

        /**
         * 本章状态变化/角色状态
         */
        @JacksonXmlProperty(localName = "S")
        @Schema(description = "本章状态变化，描述角色在本章中的状态改变，对应XML中的<S>标签",
                example = "从初入江湖的懵懂少年，到经历战斗后的成长")
        private String status;

        /**
         * 性格特点（逗号分隔）
         */
        @JacksonXmlProperty(localName = "P")
        @Schema(description = "性格特点，逗号分隔多个特点，对应XML中的<P>标签",
                example = "冷静,果断,重情重义")
        private String personality;

        /**
         * 外貌特征描述
         */
        @JacksonXmlProperty(localName = "F")
        @Schema(description = "外貌特征描述，对应XML中的<F>标签",
                example = "身材修长，眉目清秀，腰间佩戴一把短刀")
        private String appearance;

        /**
         * 修炼体系列表（支持多个修炼体系）
         */
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "V")
        @Schema(description = "修炼体系列表，对应XML中的多个<V>标签，包含修炼体系详情")
        private List<CultivationSystemDto> cultivationSystems;

        /**
         * 势力关联列表（支持多个势力）
         */
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "FC")
        @Schema(description = "势力关联列表，对应XML中的多个<FC>标签，包含角色所属势力信息")
        private List<FactionConnectionDto> factionConnections;

        /**
         * 外貌/装扮变化
         */
        @JacksonXmlProperty(localName = "AP")
        @Schema(description = "外貌/装扮变化，描述角色在本章中外貌或装扮的改变，对应XML中的<AP>标签",
                example = "换上了一身黑色劲装，腰间多了一块玉佩")
        private String appearanceChange;

        /**
         * 性格展现
         */
        @JacksonXmlProperty(localName = "PR")
        @Schema(description = "性格展现，描述角色在本章中性格的具体表现，对应XML中的<PR>标签",
                example = "面对强敌时临危不乱，展现出超乎年龄的沉稳")
        private String personalityReveal;

        /**
         * 能力展现
         */
        @JacksonXmlProperty(localName = "AB")
        @Schema(description = "能力展现，描述角色在本章中展示的能力或技能，对应XML中的<AB>标签",
                example = "施展了家传剑法【流云剑法】，一剑击退三名刺客")
        private String abilityDisplay;

        /**
         * 角色成长
         */
        @JacksonXmlProperty(localName = "CD")
        @Schema(description = "角色成长，描述角色在本章中的心理或能力成长，对应XML中的<CD>标签",
                example = "从依赖他人到独立面对危机，心智得到极大提升")
        private String characterDevelopment;

        /**
         * 核心对话摘要
         */
        @JacksonXmlProperty(localName = "DG")
        @Schema(description = "核心对话摘要，总结角色在本章中的重要对话内容，对应XML中的<DG>标签",
                example = "与老者的对话中得知父亲失踪的线索，决心踏上寻父之路")
        private String keyDialogue;
    }

    /**
     * 修炼体系内部类
     *
     * XML格式示例：
     * <V>
     *   <SYS>修炼体系名称</SYS>
     *   <LV>当前境界等级</LV>
     *   <CH>本章境界变化</CH>
     * </V>
     */
    @Data
    @Schema(description = "修炼体系详情，对应XML中的<V>标签，包含修炼体系相关信息")
    public static class CultivationSystemDto {

        /**
         * 修炼体系名称
         */
        @JacksonXmlProperty(localName = "SYS")
        @Schema(description = "修炼体系名称，对应XML中的<SYS>标签",
                example = "玄天剑道")
        private String systemName;

        /**
         * 当前境界等级
         */
        @JacksonXmlProperty(localName = "LV")
        @Schema(description = "当前境界等级，对应XML中的<LV>标签",
                example = "筑基初期")
        private String currentLevel;

        /**
         * 本章境界变化
         */
        @JacksonXmlProperty(localName = "CH")
        @Schema(description = "本章境界变化，描述角色在本章中境界的变化情况，对应XML中的<CH>标签",
                example = "从练气圆满突破到筑基初期")
        private String levelChange;
    }

    /**
     * 势力关联内部类
     *
     * XML格式示例：
     * <FC>
     *   <N>势力名称</N>
     *   <R>职位/角色</R>
     * </FC>
     */
    @Data
    @Schema(description = "势力关联信息，对应XML中的<FC>标签，包含角色所属势力的名称和角色职位")
    public static class FactionConnectionDto {

        /**
         * 势力名称
         */
        @JacksonXmlProperty(localName = "N")
        @Schema(description = "势力名称，对应XML中的<N>标签",
                example = "青云门")
        private String factionName;

        /**
         * 在势力中的职位/角色
         */
        @JacksonXmlProperty(localName = "R")
        @Schema(description = "在势力中的职位/角色，对应XML中的<R>标签",
                example = "长老")
        private String role;
    }
}
