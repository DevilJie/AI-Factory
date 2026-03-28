package com.aifactory.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 章节记忆XML DTO
 *
 * 使用Jackson XML注解将LLM返回的XML字符串解析为POJO
 *
 * XML格式示例（新格式，支持完整角色信息）：
 * <pre>
 * {@code
 * <M>
 *   <SUM>章节内容总结</SUM>
 *   <E>核心事件1</E>
 *   <E>核心事件2</E>
 *   <C>
 *     <N>角色名</N>
 *     <T>角色类型(protagonist/supporting/antagonist/npc)</T>
 *     <G>性别(male/female/other)</G>
 *     <A>年龄</A>
 *     <S>本章状态变化</S>
 *     <P>性格特点(逗号分隔)</P>
 *     <F>外貌特征</F>
 *   </C>
 *   <T>新设定</T>
 *   <P>新伏笔</P>
 *   <R>回收伏笔</R>
 *   <L>结尾场景</L>
 *   <U>悬念</U>
 * </M>
 * }
 * </pre>
 *
 * 标签说明：
 * - SUM: Summary（章节内容总结，300-500字）
 * - E: Events（核心事件，多个）
 * - C: Character（角色详情，包含完整角色信息）
 *   - N: Name（角色名称）
 *   - T: Type（角色类型：protagonist/supporting/antagonist/npc）
 *   - G: Gender（性别：male/female/other）
 *   - A: Age（年龄）
 *   - S: Status（本章状态变化）
 *   - P: Personality（性格特点，逗号分隔）
 *   - F: Feature（外貌特征）
 * - T: Things（新设定，在角色信息之后）
 * - P: Planting（埋伏笔）
 * - R: Resolve（回收伏笔）
 * - L: Last scene（结尾场景）
 * - U: Unknown/Suspense（悬念）
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Data
@JacksonXmlRootElement(localName = "M")
@Schema(description = "章节记忆XML解析对象，用于解析LLM返回的章节结构化记忆XML数据")
public class ChapterMemoryXmlDto {

    /**
     * 章节内容总结（300-500字）
     */
    @JacksonXmlProperty(localName = "SUM")
    @Schema(description = "章节内容总结，300-500字，对应XML中的<SUM>标签",
            example = "本章讲述主角李云初入江湖的经历。他在酒楼偶遇一位神秘老者...")
    private String chapterSummary;

    /**
     * 核心事件列表（可能有多个<E>标签）
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "E")
    @Schema(description = "核心事件列表，对应XML中的多个<E>标签",
            example = "[\"酒楼偶遇老者\", \"获得藏宝图线索\", \"遭遇刺客袭击\"]")
    private List<String> keyEvents;

    /**
     * 角色详情列表（可能有多个角色）
     * 使用CharacterDetailDto支持完整角色信息
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "C")
    @Schema(description = "角色详情列表，对应XML中的多个<C>标签，包含完整角色信息")
    private List<CharacterDetailDto> characterDetailList;

    /**
     * 新设定（标签为T，避免与<S>冲突）
     */
    @JacksonXmlProperty(localName = "T")
    @Schema(description = "本章新增的世界设定，对应XML中的<T>标签",
            example = "江湖中存在一个神秘组织【暗影堂】，专门收集情报")
    private String newSettings;

    /**
     * 新埋伏笔
     */
    @JacksonXmlProperty(localName = "P")
    @Schema(description = "本章新埋设的伏笔，对应XML中的<P>标签",
            example = "主角腰间的玉佩似乎隐藏着某种秘密")
    private String foreshadowingPlanted;

    /**
     * 回收伏笔
     */
    @JacksonXmlProperty(localName = "R")
    @Schema(description = "本章回收/解答的伏笔，对应XML中的<R>标签",
            example = "第一章提到的神秘老者原来是主角父亲的老友")
    private String foreshadowingResolved;

    /**
     * 结尾场景
     */
    @JacksonXmlProperty(localName = "L")
    @Schema(description = "章节结尾场景描述，用于下一章开头衔接，对应XML中的<L>标签",
            example = "李云站在城墙上，望着远方的灯火，心中充满期待...")
    private String chapterEndingScene;

    /**
     * 当前悬念
     */
    @JacksonXmlProperty(localName = "U")
    @Schema(description = "章节结束时的悬念，吸引读者继续阅读，对应XML中的<U>标签",
            example = "黑暗中，一双眼睛正默默注视着李云...")
    private String currentSuspense;

    /**
     * 角色详情内部类（支持完整角色信息）
     *
     * XML格式示例：
     * <C>
     *   <N>角色名</N>
     *   <T>角色类型(protagonist/supporting/antagonist/npc)</T>
     *   <G>性别(male/female/other)</G>
     *   <A>年龄</A>
     *   <S>本章状态变化</S>
     *   <P>性格特点(逗号分隔)</P>
     *   <F>外貌特征</F>
     * </C>
     */
    @Data
    @Schema(description = "角色详情，对应XML中的<C>标签，包含完整的角色信息")
    public static class CharacterDetailDto {
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
         * 获取角色状态（兼容方法，返回status字段）
         * @return 角色状态
         */
        public String getStatus() {
            return this.status;
        }

        /**
         * 设置角色状态（兼容方法）
         * @param status 角色状态
         */
        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 将角色详情列表转换为Map格式（用于数据库存储）
     * @return 角色名 -> 状态 的Map
     */
    @Schema(description = "将角色详情列表转换为Map格式，键为角色名，值为状态，用于数据库存储")
    public Map<String, String> getCharacterStatusMap() {
        if (characterDetailList == null || characterDetailList.isEmpty()) {
            return null;
        }

        return characterDetailList.stream()
            .filter(c -> c.getName() != null && c.getStatus() != null)
            .collect(java.util.stream.Collectors.toMap(
                CharacterDetailDto::getName,
                CharacterDetailDto::getStatus,
                (existing, replacement) -> existing // 重复key保留第一个
            ));
    }

    /**
     * 获取角色详情列表（兼容旧方法名）
     * @return 角色详情列表
     * @deprecated 使用 getCharacterDetailList() 替代
     */
    @Deprecated
    public List<CharacterDetailDto> getCharacterStatusList() {
        return characterDetailList;
    }
}
