package com.aifactory.dto;

import com.aifactory.common.XmlParser;
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
    
    @JacksonXmlProperty(localName = "g")
    private String geography; // 地理
    
    @JacksonXmlProperty(localName = "f")
    private String forces; // 势力
    
    @JacksonXmlProperty(localName = "l")
    private String timeline; // 历史
    
    @JacksonXmlProperty(localName = "r")
    private String rules; // 世界法则

    @Data
    public static class Systems {

        @JacksonXmlProperty(localName = "system")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CultivationSystem> systemList;

        public Systems() {}

        // Getters and Setters...
    }

    @Data
    public static class CultivationSystem {

        @JacksonXmlProperty(localName = "name")
        private String name; // 体系名称

        @JacksonXmlProperty(localName = "sourceFrom")
        private String sourceFrom; // 力量来源

        @JacksonXmlProperty(localName = "coreResource")
        private String coreResource; // 核心资源

        @JacksonXmlProperty(localName = "cultivationMethod")
        private String cultivationMethod; // 修炼方法

        @JacksonXmlProperty(localName = "description")
        private String description; // 描述

        @JacksonXmlProperty(localName = "levels")
        private Levels levels; // 境界等级
    }
    
    @Data
    public static class Levels {

        @JacksonXmlProperty(localName = "level")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CultivationLevel> levelList;
    }

    @Data
    public static class CultivationLevel {

        @JacksonXmlProperty(localName = "levelName")
        private String levelName; // 境界名称

        @JacksonXmlProperty(localName = "description")
        private String description; // 描述

        @JacksonXmlProperty(localName = "breakthroughCondition")
        private String breakthroughCondition; // 突破条件

        @JacksonXmlProperty(localName = "lifespan")
        private String lifespan; // 寿元

        @JacksonXmlProperty(localName = "powerRange")
        private String powerRange; // 力量范围

        @JacksonXmlProperty(localName = "landmarkAbility")
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

    public static void main(String[] args) throws XmlParser.XmlParseException {
        XmlParser parser = new XmlParser();
        WorldSettingXmlDto parse = parser.parse("<w>\n" +
                "  <t>玄幻/仙侠/克苏鲁融合</t>\n" +
                "  <b><![CDATA[这是一个被谎言与遗忘笼罩的宏大世界。表面遵循着传统的“仙道昌隆，魔道肆虐”叙事，由九大仙宗统治，凡人通过吸纳“灵气”修炼飞升。然而，真相深埋于历史的尘埃与扭曲的封印之下。主角的故乡“青石村”所在的区域，实为“墟界”——一个用以封印远古仙魔战场的时空夹缝。所谓的“仙”，是一群在远古时期背叛了“天道”（实为维护世界平衡的原始意志）、窃取其权柄并篡改历史的胜利者；而被污名化的“魔”，则是试图守护世界本源、抵抗窃取者的古神后裔。随着主角成长与封印松动，世界的真实面貌逐渐显现：越接近力量的顶峰，越能感知到被窃取的天道所发出的、充满疯狂与低语的“呼唤”，修炼本身即是一场与未知恐惧和认知污染的赛跑。]]></b>\n" +
                "  <p>\n" +
                "    <system>\n" +
                "      <name>仙道体系（表象/主流）</name>\n" +
                "      <sourceFrom>天地灵气（实为被稀释、过滤后的天道本源碎片）</sourceFrom>\n" +
                "      <coreResource>灵石、灵丹、天材地宝</coreResource>\n" +
                "      <cultivationMethod>吐纳灵气，淬炼己身，感悟天地法则（被篡改后的伪法则）</cultivationMethod>\n" +
                "      <description><![CDATA[由九大仙宗推广的、统治世界的正统修炼体系。修行者通过吸收“灵气”强化肉身与神魂，逐步掌握移山倒海、长生久视的力量。体系完整，境界分明，是通往“飞升仙界”的公认大道。然而，此体系建立在被窃取和扭曲的“天道”基础上，修行至高深时，会不自觉地受到“真实天道”残留意志的污染，产生幻听、幻视与认知扭曲，被主流解释为“心魔”或“天劫”。]]></description>\n" +
                "      <levels>\n" +
                "        <level>\n" +
                "          <levelName>凡蜕三境</levelName>\n" +
                "          <description><![CDATA[脱离凡胎，奠定道基。可施展基础法术，御使低阶法器，寿元远超常人。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[积累足够灵气，并初步理解一种“伪天道法则”的皮毛。]]></breakthroughCondition>\n" +
                "          <lifespan>150-500年</lifespan>\n" +
                "          <powerRange><![CDATA[从力敌百夫到摧毁小型城镇]]></powerRange>\n" +
                "          <landmarkAbility>灵气外放，御器飞行（后期）</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>炼气期</step>\n" +
                "            <step>筑基期</step>\n" +
                "            <step>金丹期</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>问道三境</levelName>\n" +
                "          <description><![CDATA[触及法则，神魂质变。可引动天地之力，初步涉及空间奥秘，开始频繁遭遇“心魔”（真实污染）。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[深度领悟并驾驭至少一种“伪法则”，神魂经受初步“天道低语”考验而不疯。]]></breakthroughCondition>\n" +
                "          <lifespan>1000-3000年</lifespan>\n" +
                "          <powerRange><![CDATA[足以改变局部地貌，一击覆灭中型宗门]]></powerRange>\n" +
                "          <landmarkAbility>法则领域，元神出窍</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>元婴期</step>\n" +
                "            <step>化神期</step>\n" +
                "            <step>炼虚期</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>登仙三境</levelName>\n" +
                "          <description><![CDATA[近乎伪仙，与窃取的天道权柄共鸣。举手投足引动天象，但也是污染最严重的阶段，需时刻对抗疯狂。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[完全掌控一种以上“伪法则”，并窃取一丝“天道权柄”为己用，在疯狂边缘保持自我。]]></breakthroughCondition>\n" +
                "          <lifespan>5000-10000年（理论上）</lifespan>\n" +
                "          <powerRange><![CDATA[大陆级破坏力，可引发天灾]]></powerRange>\n" +
                "          <landmarkAbility>言出法随（小范围），窥见部分世界真实（伴随巨大风险）</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>合体期</step>\n" +
                "            <step>大乘期</step>\n" +
                "            <step>渡劫期（飞升/陨落/异化）</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "      </levels>\n" +
                "    </system>\n" +
                "    <system>\n" +
                "      <name>古神遗脉体系（真相/隐秘）</name>\n" +
                "      <sourceFrom>墟界逸散的混沌原初之力、古神残留精血、被污染的天道低语（危险）</sourceFrom>\n" +
                "      <coreResource>墟界核心碎片、古神遗物、纯净的信念（极稀有）</coreResource>\n" +
                "      <cultivationMethod>唤醒体内稀薄古神血脉，直面并适应“真实”（污染），于疯狂中夺取力量，守护与牺牲中获得共鸣</cultivationMethod>\n" +
                "      <description><![CDATA[被污蔑为“魔道”的真正守护者之路。修行者不依赖被篡改的“灵气”，而是直接接触世界本源或古神遗留的力量。这条道路极其危险且非主流，进展缓慢且伴随强烈的精神与肉体异变风险，但成长上限极高，且力量本质更接近世界真实。修行者外表或心智常异于常人，被仙道斥为“入魔”。其力量往往表现为扭曲物理规则、操控阴影与混沌、以及强大的肉身再生能力。]]></description>\n" +
                "      <levels>\n" +
                "        <level>\n" +
                "          <levelName>觉醒三阶</levelName>\n" +
                "          <description><![CDATA[血脉初步苏醒，感知到世界的“杂音”与“低语”。身体开始出现非人特征，获得超越常理的力量与恢复力。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[成功承受一次“真实冲击”而不崩溃，并主动接纳一种异变。]]></breakthroughCondition>\n" +
                "          <lifespan>200-800年（但外貌可能加速衰老或停滞）</lifespan>\n" +
                "          <powerRange><![CDATA[肉身媲美法宝，能施展小范围规则扭曲]]></powerRange>\n" +
                "          <landmarkAbility>血脉显化（局部异变），感知“真实”</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>聆秘者</step>\n" +
                "            <step>异变者</step>\n" +
                "            <step>共鸣者</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>统御三阶</levelName>\n" +
                "          <description><![CDATA[初步掌控自身异变与血脉力量，能在一定范围内定义临时的“规则”，对抗仙道“伪法则”。心智与疯狂共存。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[在彻底疯狂的边缘锚定一个强烈的“守护”或“牺牲”信念，并吞噬/融合一种强大的古神残留物。]]></breakthroughCondition>\n" +
                "          <lifespan>1500-5000年</lifespan>\n" +
                "          <powerRange><![CDATA[可正面抗衡元婴至化神修士，能力诡异难防]]></powerRange>\n" +
                "          <landmarkAbility>展开“真实领域”，短暂扭曲局部天道规则</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>掌域者</step>\n" +
                "            <step>噬理者</step>\n" +
                "            <step>守望者</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>近古三阶</levelName>\n" +
                "          <description><![CDATA[血脉返祖，接近远古守护者形态。本身已成为一种“规则异常体”，可有限度地修改现实，但需付出巨大代价。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[完成一次对“窃仙”或其重要造物的成功守护/破坏，获得世界本源（真实天道）的短暂认可与灌注。]]></breakthroughCondition>\n" +
                "          <lifespan>未知（可能永恒，也可能下一秒就因代价而湮灭）</lifespan>\n" +
                "          <powerRange><![CDATA[大陆级威胁，能力无法用常理解释]]></powerRange>\n" +
                "          <landmarkAbility>概念级能力，短暂化身古神投影</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>代行者</step>\n" +
                "            <step>古神裔</step>\n" +
                "            <step>？？？（失落的真名）</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "      </levels>\n" +
                "    </system>\n" +
                "  </p>\n" +
                "  <g><![CDATA[世界主体为“苍玄大陆”，被无尽海包围。大陆中央是灵气最浓郁、被九大仙宗瓜分的“中州”。东西南北四方各有大洲，分布着无数王朝、宗门和妖兽领地。青石村位于大陆西南边陲的“南荒”与“西漠”交界处的无名山脉中，此地灵气稀薄，实为“墟界”入口的天然伪装。墟界内部时空错乱，景象光怪陆离，封印着远古战场的碎片、古神遗骸与破碎的法则，是真相与危险并存之地。无尽海深处则传说有未被污染的海外散修与更古老的秘密。]]></g>\n" +
                "  <f><![CDATA[【统治阵营】：九大仙宗及其附属王朝、世家，组成“仙盟”，垄断正统修仙资源与话语权，是当前世界的既得利益者和真相掩盖者。\n" +
                "【反抗/隐秘阵营】：“墟光会”（由知晓部分真相的古神遗脉、被污染但保持理智的高阶修士、以及追寻历史的异类组成），活动于阴影和墟界周边，目标为揭开真相、瓦解仙盟统治。\n" +
                "【中立/混乱阵营】：海外散修联盟、各大妖兽国度、专注于技术（炼丹、炼器、阵法）的“百工阁”，他们可能对真相一无所知，或选择明哲保身。\n" +
                "【超然存在】：沉睡/被封印于墟界深处的古神残念；高踞九天之上、窃取天道权柄的“真仙”（实为最初的背叛者集团）。]]></f>\n" +
                "  <l><![CDATA[【远古纪元】：古神与原生天道维护世界平衡。“窃仙”集团诞生，发动叛乱，史称“天道之争”。古神败北，大部分被封印或湮灭，天道权柄被窃取篡改。历史被胜利者书写，古神被污为“魔”。\n" +
                "【封印纪元】：窃仙集团将主要战场封印于“墟界”，并设立无数入口监视。他们飞升至自我创建的“仙界”，留下仙宗代理统治。真相逐渐被遗忘。\n" +
                "【当下纪元】：封印历经岁月有所松动，“天道低语”（污染）加剧。主角于青石村（墟界入口之一）踏上修仙路，故事由此开始。]]></l>\n" +
                "  <r><![CDATA[1. 【认知即污染法则】：接触世界真实（如墟界景象、古神遗物、天道低语）会直接污染神魂，轻则产生幻觉，重则疯狂异变。知识本身是危险的。\n" +
                "2. 【代价平衡法则】：获取力量必须付出代价。仙道体系代价是逐渐加剧的污染与最终可能的心智丧失；古神遗脉体系代价是肉体异变与永恒的疯狂风险。没有毫无代价的强大。\n" +
                "3. 【墟界不稳定法则】：墟界内物理与时空法则混乱且可变，依赖于区域内最强的“认知”或“遗物”影响。可能一步沧海，一步桑田。\n" +
                "4. 【天道呼唤不可逆法则】：一旦修为达到“问道境”或“觉醒高阶”，将无法屏蔽来自被窃取天道和真实本源的双重“呼唤”（低语），必须选择一方进行深度共鸣，无法再保持绝对中立。\n" +
                "5. 【历史修正力】：仙盟掌控的“历史”具有微弱的世界规则加持，公然质疑或传播相反历史会遭受气运反噬与规则排斥。]]></r>\n" +
                "</w>", WorldSettingXmlDto.class);
        System.out.println(parse.getBackground());
    }
}