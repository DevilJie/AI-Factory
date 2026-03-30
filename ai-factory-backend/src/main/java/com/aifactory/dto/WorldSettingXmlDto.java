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

        @JacksonXmlProperty(localName = "ss")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CultivationSystem> systemList;

        public Systems() {}

        // Getters and Setters...
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

    public static void main(String[] args) throws XmlParser.XmlParseException {
        XmlParser parser = new XmlParser();
        WorldSettingXmlDto parse = parser.parse("<w>\n" +
                "  <t>仙侠/玄幻/克苏鲁</t>\n" +
                "  <b><![CDATA[这是一个外表遵循传统修仙逻辑，内核却充满颠覆与未知恐惧的世界。天地间充盈着“灵气”，修士们通过修炼飞升成仙，是世人公认的真理。然而，这“真理”本身就是一个巨大的谎言。世界由“表世界”（现世）与“里世界”（墟界）构成。表世界是仙道昌盛的九洲大陆，而里世界则是被封印的远古仙魔战场，充斥着被扭曲的法则与不可名状的“古神遗骸”。主角的故乡，正是连接两界的脆弱节点。随着修为提升，修士接触的“天道”越深，越会感知到世界底层规则的诡异与不协调，产生“道化”风险——即被墟界泄露的“真实”所污染，精神崩溃或异化为怪物。所谓轻松基调，在于主角初期凭借“系统”和“无敌”天赋，能以戏谑心态应对传统修仙界的尔虞我诈，但随着真相揭露，轻松的表象下将涌起深邃的恐怖与悲壮。]]></b>\n" +
                "  <p>\n" +
                "    <system>\n" +
                "      <name>仙道体系（伪）</name>\n" +
                "      <sourceFrom>天地灵气（实为被“天道”过滤与驯化的古神逸散能量）</sourceFrom>\n" +
                "      <coreResource>灵石、灵丹、蕴含道韵的天材地宝</coreResource>\n" +
                "      <cultivationMethod>吐纳灵气，感悟天道，凝结金丹/元婴，渡劫飞升</cultivationMethod>\n" +
                "      <description><![CDATA[九洲大陆主流修炼体系，由“天庭”及各大仙门推广。体系完整，境界分明，旨在引导修士吸收“安全”的灵气，最终飞升至“仙界”（实为背叛者打造的囚笼或堡垒）。修炼此体系越深，与当前“天道”绑定越紧，对墟界“真实”的抵抗力越弱，但表世界战力增长显著。]]></description>\n" +
                "      <levels>\n" +
                "        <level>\n" +
                "          <levelName>炼气期</levelName>\n" +
                "          <description><![CDATA[引气入体，淬炼肉身，初步掌握法术。可施展低阶五行术法，御使符箓、低阶法器。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[灵气积累足够，打通周身主要经脉，凝练第一缕液态真元。]]></breakthroughCondition>\n" +
                "          <lifespan>约120-150年</lifespan>\n" +
                "          <powerRange><![CDATA[超越凡人，可敌百人军队。]]></powerRange>\n" +
                "          <landmarkAbility>灵气外放，施展基础法术</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>一至九层</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>筑基期</levelName>\n" +
                "          <description><![CDATA[筑就道基，真元固化，寿元大增。可长时间御器飞行，修炼本命法器，神识初成。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[真元凝练至极致，感悟一丝天道法则（伪），凝聚道基之台。]]></breakthroughCondition>\n" +
                "          <lifespan>约300年</lifespan>\n" +
                "          <powerRange><![CDATA[开山裂石，初步具备范围杀伤能力。]]></powerRange>\n" +
                "          <landmarkAbility>御器飞行，神识探查</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>初期、中期、后期、大圆满</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>金丹期</levelName>\n" +
                "          <description><![CDATA[凝结金丹，能量核心形成，可初步调用天地之力。神识可覆盖百里，炼制法宝，开宗立派。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[在道基上凝聚全部精气神，经历心魔劫（实为初步接触墟界信息冲击），结成金丹。]]></breakthroughCondition>\n" +
                "          <lifespan>约800年</lifespan>\n" +
                "          <powerRange><![CDATA[移山倒海，一击可毁城池。]]></landmarkAbility>\n" +
                "          <landmarkAbility>丹火炼器，初步领域</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>虚丹、实丹、金丹</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "      </levels>\n" +
                "    </system>\n" +
                "    <system>\n" +
                "      <name>古神道途（真/禁忌）</name>\n" +
                "      <sourceFrom>墟界逸散的“真实源质”（未被过滤的古神本源力量，充满污染与疯狂）</sourceFrom>\n" +
                "      <coreResource>古神遗骸碎片、被污染的灵物、承载古老知识的禁忌物</coreResource>\n" +
                "      <cultivationMethod>直视“真实”，接纳污染，以意志重构自我，唤醒血脉中的古神印记</cultivationMethod>\n" +
                "      <description><![CDATA[被仙道污名为“魔功”的真正古老传承。修炼者需直面世界的“真实”，承受巨大的精神污染与肉身异化风险。力量增长不遵循固定境界，更侧重于对“真实”的理解和自身意志的坚定。战力诡异强大，但极易被表世界天道排斥，并可能丧失人性。主角因出身墟界入口，血脉特殊，可能通过“系统”辅助降低风险，走出一条新路。]]></description>\n" +
                "      <levels>\n" +
                "        <level>\n" +
                "          <levelName>窥真者</levelName>\n" +
                "          <description><![CDATA[初步感知并接触“真实”，精神开始异于常人，能看见常人不可见之物（灵气流动的“污点”、他人的情绪颜色等），肉身开始出现轻微异化特征（如瞳孔变色、体温异常）。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[成功接纳第一缕“真实源质”而不崩溃，并在精神世界构筑第一个“锚点”（通常是强烈的执念或记忆）。]]></breakthroughCondition>\n" +
                "          <lifespan>不确定，可能缩短也可能因异化延长</lifespan>\n" +
                "          <powerRange><![CDATA[拥有超越同阶仙道修士的诡异能力，如精神冲击、污染感知、小范围现实扭曲，但极不稳定。]]></powerRange>\n" +
                "          <landmarkAbility>真实视界，精神污染</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>感知、接触、接纳</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>重构者</levelName>\n" +
                "          <description><![CDATA[意志初步驾驭污染，开始有意识地重构自身肉体与灵魂，形成独特的“真实形态”（可部分收放）。能主动从墟界汲取力量，施展基于“真实”的术法，威力巨大但代价高昂。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[完成一次重大的自我认知重构，明确“我为何是我”，并在体内稳定生成一个“内在墟界”雏形。]]></breakthroughCondition>\n" +
                "          <lifespan>大幅波动，取决于重构方向</lifespan>\n" +
                "          <powerRange><![CDATA[可正面抗衡金丹修士，能力更加系统且可控，能制造大范围的规则异常地带。]]></powerRange>\n" +
                "          <landmarkAbility>形态重构，召唤/驱使低阶墟界生物</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>血肉重构、灵魂重构、形态固化</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "        <level>\n" +
                "          <levelName>行走者</levelName>\n" +
                "          <description><![CDATA[内在墟界基本成型，可在表里世界缝隙有限穿梭。自身成为一个小型污染源与规则异常点，能扭曲局部现实法则。古神血脉或印记深度觉醒，获得传承记忆碎片。]]></description>\n" +
                "          <breakthroughCondition><![CDATA[完全理解并接纳自身“真实形态”的本质，内在墟界与外界产生稳定共鸣，能短暂展开“真实领域”。]]></breakthroughCondition>\n" +
                "          <lifespan>近乎非人，与自身道途绑定</lifespan>\n" +
                "          <powerRange><![CDATA[对标元婴甚至化神，但战斗方式完全不可预测，能引发区域性的“道化”现象。]]></powerRange>\n" +
                "          <landmarkAbility>真实领域，短途界域行走</landmarkAbility>\n" +
                "          <steps>\n" +
                "            <step>初行、漫游、界标</step>\n" +
                "          </steps>\n" +
                "        </level>\n" +
                "      </levels>\n" +
                "    </system>\n" +
                "  </p>\n" +
                "  <g><![CDATA[主要舞台为“九洲大陆”，分为中土神州（灵气最盛，仙门林立）、东极青洲（妖族势力范围）、南荒炎洲（散修与魔道活跃地）、西漠沙洲（资源贫瘠，多上古遗迹）、北原雪洲（苦寒之地，封印众多）。大陆之外是无尽星海与虚空乱流。主角故乡“青岚村”位于南荒炎洲与西漠沙洲交界的“苍梧山脉”深处，山脉地下深处隐藏着通往“墟界”的最大也是最脆弱的入口之一。墟界内部空间错乱，时间流速不定，景象光怪陆离，遍布仙魔古战场遗迹、漂浮的破碎大陆以及不可名状的古神遗骸，环境本身就在不断低语着疯狂的知识。]]></g>\n" +
                "  <f><![CDATA[表世界：1. 天庭（最高统治与谎言维护者，由“仙”组成，掌控飞升通道）。2. 三大仙门（太清宗、玉虚殿、瑶池圣地，天庭在九洲的代言人与管理者）。3. 万妖谷（妖族联盟，对真相有所怀疑但保持中立）。4. 散修联盟与魔道（部分激进派可能无意中触及真相边缘）。里世界/反抗势力：1. 古神遗族（残存的古神后裔，在墟界艰难生存，是真相的守护者，被污为“魔”）。2. 清醒者（极少数发现真相并叛出仙道的修士，在夹缝中求生并试图揭露谎言）。3. 无序孽物（完全被污染失去理智的墟界生物，敌我不分）。主角初期接触表世界仙门，中后期将深入墟界，与古神遗族和清醒者结盟。]]></f>\n" +
                "  <l><![CDATA[远古纪元：古神统治时代，万物和谐。仙古之战爆发（背叛者窃取天道权柄，镇压古神），古神陨落，其残骸与战场被封印为“墟界”。上古纪元：背叛者建立“天庭”，篡改历史，编织仙道修炼体系，过滤世界能量，开启“飞升”骗局。中古纪元：仙道文明繁荣，偶有“魔灾”（墟界泄漏或清醒者反抗）被镇压。近古纪元至今：封印历经岁月逐渐松动，墟界对表世界渗透加剧，“道化”事件频发，但被天庭掩盖。故事开始于封印又一次周期性松动的时代。]]></l>\n" +
                "  <r><![CDATA[1. 认知即污染规则：对世界“真实”的认知本身会带来精神污染与异化风险，知识是力量也是毒药。2. 天道排斥规则：修炼古神道途或携带过多墟界气息，会遭到表世界“天道”（伪）的压制与排斥，雷劫更猛烈。3. 飞升陷阱规则：按仙道体系飞升，并非超脱，而是进入天庭控制的“仙界”，成为维护谎言的一份子或养料。4. 系统辅助规则（主角专属）：主角顾云深觉醒的“系统”实为一件来自墟界深处的、有自我意识的古老遗物，能帮助他量化能力、任务引导，并最关键地——过滤和稳定“真实源质”，降低污染风险，这是他能轻松开局的关键。5. 血脉共鸣规则：出身墟界入口附近的生灵，其血脉可能隐含古神印记，更容易感知和接纳“真实”，沈星遥可能拥有更纯净的古神血脉。]]></r>\n" +
                "</w>", WorldSettingXmlDto.class);
        System.out.println(parse.getBackground());
    }
}