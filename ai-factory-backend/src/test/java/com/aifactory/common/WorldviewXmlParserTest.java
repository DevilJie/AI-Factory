package com.aifactory.common;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.PowerSystemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WorldviewXmlParser — DOM parsing and name matching logic.
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@ExtendWith(MockitoExtension.class)
class WorldviewXmlParserTest {

    @Mock
    private ContinentRegionService continentRegionService;

    @Mock
    private PowerSystemService powerSystemService;

    @InjectMocks
    private WorldviewXmlParser worldviewXmlParser;

    // ======================== parseGeographyXml ========================

    @Test
    void testParseGeographyXml_singleRegion() {
        String xml = "Some text before <g><r><n>TestRegion</n><d>Test description</d></r></g> some text after";

        List<NovelContinentRegion> result = worldviewXmlParser.parseGeographyXml(xml, 1L);

        assertEquals(1, result.size());
        assertEquals("TestRegion", result.get(0).getName());
        assertEquals("Test description", result.get(0).getDescription());
        assertEquals(1L, result.get(0).getProjectId());
    }

    @Test
    void testParseGeographyXml_nestedRegions() {
        String xml = "<g><r><n>Continent</n><d>A big land</d><r><n>Country</n><d>A nation</d></r></r></g>";

        List<NovelContinentRegion> result = worldviewXmlParser.parseGeographyXml(xml, 1L);

        assertEquals(1, result.size());
        assertEquals("Continent", result.get(0).getName());
        assertNotNull(result.get(0).getChildren());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("Country", result.get(0).getChildren().get(0).getName());
    }

    @Test
    void testParseGeographyXml_noGTag() {
        String xml = "No geography tags here";

        List<NovelContinentRegion> result = worldviewXmlParser.parseGeographyXml(xml, 1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testParseGeographyXml_emptyGTag() {
        String xml = "<g></g>";

        List<NovelContinentRegion> result = worldviewXmlParser.parseGeographyXml(xml, 1L);

        assertTrue(result.isEmpty());
    }

    // ======================== parseFactionXml ========================

    @Test
    void testParseFactionXml_singleFaction() {
        String xml = "Prefix <f><faction><n>TestFaction</n><type>正派</type><d>A good faction</d></faction></f> suffix";

        WorldviewXmlParser.ParsedFactions result = worldviewXmlParser.parseFactionXml(xml, 1L);

        assertEquals(1, result.rootFactions().size());
        NovelFaction faction = result.rootFactions().get(0);
        assertEquals("TestFaction", faction.getName());
        assertEquals("ally", faction.getType());
        assertEquals("A good faction", faction.getDescription());
        assertEquals(1L, faction.getProjectId());
    }

    @Test
    void testParseFactionXml_withPendingAssociations() {
        String xml = "<f><faction><n>TestFaction</n><type>反派</type>" +
            "<regions>Region1, Region2</regions>" +
            "<relation><target>OtherFaction</target><type>盟友</type></relation>" +
            "</faction></f>";

        WorldviewXmlParser.ParsedFactions result = worldviewXmlParser.parseFactionXml(xml, 1L);

        assertEquals(1, result.rootFactions().size());
        assertEquals(1, result.pendingAssociations().size());

        WorldviewXmlParser.PendingAssociation pending = result.pendingAssociations().get(0);
        assertEquals("TestFaction", pending.factionName());
        assertEquals(2, pending.regionNames().size());
        assertTrue(pending.regionNames().contains("Region1"));
        assertTrue(pending.regionNames().contains("Region2"));
        assertEquals(1, pending.relations().size());
        assertEquals("OtherFaction", pending.relations().get(0).targetName());
        assertEquals("盟友", pending.relations().get(0).type());
    }

    @Test
    void testParseFactionXml_noFTag() {
        String xml = "No faction tags here";

        WorldviewXmlParser.ParsedFactions result = worldviewXmlParser.parseFactionXml(xml, 1L);

        assertTrue(result.rootFactions().isEmpty());
        assertTrue(result.pendingAssociations().isEmpty());
    }

    @Test
    void testParseFactionXml_nestedFactions() {
        String xml = "<f><faction><n>Parent</n><type>中立</type>" +
            "<faction><n>Child</n><type>正派</type></faction>" +
            "</faction></f>";

        WorldviewXmlParser.ParsedFactions result = worldviewXmlParser.parseFactionXml(xml, 1L);

        assertEquals(1, result.rootFactions().size());
        assertEquals("Parent", result.rootFactions().get(0).getName());
        assertNotNull(result.rootFactions().get(0).getChildren());
        assertEquals(1, result.rootFactions().get(0).getChildren().size());
        assertEquals("Child", result.rootFactions().get(0).getChildren().get(0).getName());
    }

    // ======================== mapFactionType ========================

    @Test
    void testMapFactionType_ally() {
        assertEquals("ally", worldviewXmlParser.mapFactionType("正派"));
    }

    @Test
    void testMapFactionType_hostile() {
        assertEquals("hostile", worldviewXmlParser.mapFactionType("反派"));
    }

    @Test
    void testMapFactionType_neutral() {
        assertEquals("neutral", worldviewXmlParser.mapFactionType("中立"));
    }

    @Test
    void testMapFactionType_unknown() {
        assertEquals("unknown", worldviewXmlParser.mapFactionType("unknown"));
    }

    @Test
    void testMapFactionType_null() {
        assertNull(worldviewXmlParser.mapFactionType(null));
    }

    // ======================== mapRelationType ========================

    @Test
    void testMapRelationType_ally() {
        assertEquals("ally", worldviewXmlParser.mapRelationType("盟友"));
    }

    @Test
    void testMapRelationType_hostile() {
        assertEquals("hostile", worldviewXmlParser.mapRelationType("敌对"));
    }

    @Test
    void testMapRelationType_neutral() {
        assertEquals("neutral", worldviewXmlParser.mapRelationType("中立"));
    }

    @Test
    void testMapRelationType_unknown() {
        assertEquals("other", worldviewXmlParser.mapRelationType("other"));
    }

    // ======================== findRegionIdByName ========================

    @Test
    void testFindRegionIdByName_exactMatch() {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setId(10L);
        region.setName("天剑宗");

        when(continentRegionService.listByProjectId(1L)).thenReturn(List.of(region));

        Long result = worldviewXmlParser.findRegionIdByName(1L, "天剑宗");
        assertEquals(10L, result);
    }

    @Test
    void testFindRegionIdByName_suffixMatch() {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setId(20L);
        region.setName("天剑宗");

        when(continentRegionService.listByProjectId(1L)).thenReturn(List.of(region));

        Long result = worldviewXmlParser.findRegionIdByName(1L, "天剑");
        assertEquals(20L, result);
    }

    @Test
    void testFindRegionIdByName_containsMatch() {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setId(30L);
        region.setName("东方天剑城");

        when(continentRegionService.listByProjectId(1L)).thenReturn(List.of(region));

        Long result = worldviewXmlParser.findRegionIdByName(1L, "天剑城");
        assertEquals(30L, result);
    }

    @Test
    void testFindRegionIdByName_noMatch() {
        when(continentRegionService.listByProjectId(1L)).thenReturn(Collections.emptyList());

        Long result = worldviewXmlParser.findRegionIdByName(1L, "不存在");
        assertNull(result);
    }

    @Test
    void testFindRegionIdByName_nullName() {
        Long result = worldviewXmlParser.findRegionIdByName(1L, null);
        assertNull(result);
    }

    // ======================== findPowerSystemIdByName ========================

    @Test
    void testFindPowerSystemIdByName_exactMatch() {
        NovelPowerSystem ps = new NovelPowerSystem();
        ps.setId(100L);
        ps.setName("修仙体系");

        when(powerSystemService.listByProjectId(1L)).thenReturn(List.of(ps));

        Long result = worldviewXmlParser.findPowerSystemIdByName(1L, "修仙体系");
        assertEquals(100L, result);
    }

    @Test
    void testFindPowerSystemIdByName_noMatch() {
        when(powerSystemService.listByProjectId(1L)).thenReturn(Collections.emptyList());

        Long result = worldviewXmlParser.findPowerSystemIdByName(1L, "不存在");
        assertNull(result);
    }

    // ======================== parsePowerSystemXml (new format) ========================

    @Test
    void testParsePowerSystemXml_newFormat_noLlsNoStepsWrapper() {
        // New template format: <ll> directly under <ss>, <step> directly under <ll>
        String xml = "<p><ss><name>修仙</name><sf>天地灵气</sf><cr>灵石</cr><cm>打坐冥想</cm>" +
            "<d><![CDATA[修仙体系描述]]></d>" +
            "<ll><ln>练气期</ln><dd><![CDATA[吸收灵气]]></dd><bc><![CDATA[感悟天地]]></bc>" +
            "<lsp>约150年</lsp><pr><![CDATA[战力描述]]></pr><la>灵气外放</la>" +
            "<step>初期</step><step>中期</step><step>后期</step></ll>" +
            "<ll><ln>筑基期</ln><dd><![CDATA[凝聚灵液]]></dd><bc><![CDATA[灵气液化]]></bc>" +
            "<lsp>约300年</lsp><pr><![CDATA[战力描述2]]></pr><la>御剑飞行</la>" +
            "<step>初期</step><step>中期</step><step>后期</step></ll>" +
            "</ss></p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertEquals(1, result.systems().size());
        NovelPowerSystem system = result.systems().get(0);
        assertEquals("修仙", system.getName());
        assertEquals("天地灵气", system.getSourceFrom());
        assertEquals("灵石", system.getCoreResource());
        assertEquals("打坐冥想", system.getCultivationMethod());
        assertEquals(1L, system.getProjectId());

        assertNotNull(system.getLevels());
        assertEquals(2, system.getLevels().size());

        // Level 1: 练气期
        assertEquals("练气期", system.getLevels().get(0).getLevelName());
        assertEquals(1, system.getLevels().get(0).getLevel());
        assertEquals("约150年", system.getLevels().get(0).getLifespan());
        assertEquals("灵气外放", system.getLevels().get(0).getLandmarkAbility());
        assertNotNull(system.getLevels().get(0).getSteps());
        assertEquals(3, system.getLevels().get(0).getSteps().size());
        assertEquals("初期", system.getLevels().get(0).getSteps().get(0).getLevelName());
        assertEquals(1, system.getLevels().get(0).getSteps().get(0).getLevel());
        assertEquals("后期", system.getLevels().get(0).getSteps().get(2).getLevelName());

        // Level 2: 筑基期
        assertEquals("筑基期", system.getLevels().get(1).getLevelName());
        assertEquals(2, system.getLevels().get(1).getLevel());
        assertEquals("御剑飞行", system.getLevels().get(1).getLandmarkAbility());
    }

    @Test
    void testParsePowerSystemXml_legacyFormat_withLlsAndStepsWrapper() {
        // Old format: <lls> wrapping <ll>, <steps> wrapping <step>
        String xml = "<p><ss><name>仙道</name><sf>灵气</sf><cr>灵石</cr><cm>冥想</cm>" +
            "<d>描述</d>" +
            "<lls><ll><ln>练气</ln><dd>描述</dd><bc>条件</bc>" +
            "<steps><step>初</step><step>中</step></steps>" +
            "</ll></lls>" +
            "</ss></p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertEquals(1, result.systems().size());
        NovelPowerSystem system = result.systems().get(0);
        assertEquals("仙道", system.getName());
        assertEquals(1, system.getLevels().size());
        assertEquals("练气", system.getLevels().get(0).getLevelName());
        assertEquals(2, system.getLevels().get(0).getSteps().size());
        assertEquals("初", system.getLevels().get(0).getSteps().get(0).getLevelName());
        assertEquals("中", system.getLevels().get(0).getSteps().get(1).getLevelName());
    }

    @Test
    void testParsePowerSystemXml_noPTag() {
        String xml = "No power system tags here";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertTrue(result.systems().isEmpty());
    }

    @Test
    void testParsePowerSystemXml_rawAmpersandInTextField() {
        // LLM may generate raw & in non-CDATA fields like <cm> or <step>
        String xml = "<p><ss><name>修仙</name><sf>天地灵气</sf><cr>灵石</cr>" +
            "<cm>打坐冥想 & 战斗修炼</cm>" +
            "<d><![CDATA[修仙体系描述]]></d>" +
            "<ll><ln>练气期</ln><dd><![CDATA[吸收灵气]]></dd><bc><![CDATA[感悟天地]]></bc>" +
            "<lsp>约150年</lsp><pr><![CDATA[战力描述]]></pr><la>灵气外放</la>" +
            "<step>初期 & 中期</step></ll>" +
            "</ss></p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertEquals(1, result.systems().size());
        NovelPowerSystem system = result.systems().get(0);
        assertEquals("打坐冥想 & 战斗修炼", system.getCultivationMethod());
        assertEquals(1, system.getLevels().size());
        assertEquals("初期 & 中期", system.getLevels().get(0).getSteps().get(0).getLevelName());
    }

    @Test
    void testParsePowerSystemXml_rawLessThanInTextField() {
        // LLM may generate raw < in non-CDATA fields (e.g., mathematical comparison)
        String xml = "<p><ss><name>修仙</name><sf>灵气</sf><cr>灵石</cr>" +
            "<cm>灵气浓度<50%时冥想</cm>" +
            "<d><![CDATA[描述]]></d>" +
            "<ll><ln>练气期</ln><dd><![CDATA[描述]]></dd><bc><![CDATA[条件]]></bc>" +
            "<lsp>约150年</lsp><pr><![CDATA[战力]]></pr><la>外放</la>" +
            "<step>初期</step></ll>" +
            "</ss></p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertEquals(1, result.systems().size());
        // The raw < should be escaped and the text preserved
        assertNotNull(result.systems().get(0).getCultivationMethod());
    }

    @Test
    void testParsePowerSystemXml_cdataPreserved() {
        // CDATA sections should be preserved as-is, including any special chars inside
        String xml = "<p><ss><name>修仙</name><sf>灵气</sf><cr>灵石</cr><cm>冥想</cm>" +
            "<d><![CDATA[体系描述：使用 A&B 方法，包括 <特殊> 技巧]]></d>" +
            "<ll><ln>练气期</ln><dd><![CDATA[描述 A&B]]></dd><bc><![CDATA[条件]]></bc>" +
            "<lsp>约150年</lsp><pr><![CDATA[战力]]></pr><la>外放</la>" +
            "<step>初期</step></ll>" +
            "</ss></p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        assertEquals(1, result.systems().size());
        assertEquals("体系描述：使用 A&B 方法，包括 <特殊> 技巧", result.systems().get(0).getDescription());
    }

    // ======================== parsePowerSystemXml with real LLM output ========================

    @Test
    void testParsePowerSystemXml_realLlmOutput_demoFile() {
        // Real LLM output from d:/work/ai/需求/demo.txt — 3 power systems, each with 5 levels
        String xml = "<p>\n" +
            "  <ss>\n" +
            "    <name>灵能觉醒</name>\n" +
            "    <sf>灵能潮汐辐射</sf>\n" +
            "    <cr>灵能结晶、高能生物组织</cr>\n" +
            "    <cm>吸收环境中的游离灵能、战斗与极限生存压力下的潜能激发、特定灵能结晶引导</cm>\n" +
            "    <d><![CDATA[灵能潮汐后，人类幸存者中自然觉醒的力量体系。]]></d>\n" +
            "\t\t<ll>\n" +
            "\t\t  <ln>觉醒者</ln>\n" +
            "\t\t  <dd><![CDATA[初步感知并引导体内灵能。]]></dd>\n" +
            "\t\t  <bc><![CDATA[在灵能辐射环境下经历生死危机。]]></bc>\n" +
            "\t\t  <lsp>约100-120年</lsp>\n" +
            "\t\t  <pr><![CDATA[可对抗少量普通变异体。]]></pr>\n" +
            "\t\t  <la>异能雏形显现</la>\n" +
            "\t\t  <step>初醒期、稳固期、显化期</step>\n" +
            "\t\t</ll>\n" +
            "\t\t<ll>\n" +
            "\t\t  <ln>掌控者</ln>\n" +
            "\t\t  <dd><![CDATA[能稳定运用异能。]]></dd>\n" +
            "\t\t  <bc><![CDATA[长期锻炼异能直至如臂使指。]]></bc>\n" +
            "\t\t  <lsp>约120-150年</lsp>\n" +
            "\t\t  <pr><![CDATA[可独立清理小股变异体群。]]></pr>\n" +
            "\t\t  <la>异能稳定外放</la>\n" +
            "\t\t  <step>熟练期、扩展期、精通期</step>\n" +
            "\t\t</ll>\n" +
            "  </ss>\n" +
            "  <ss>\n" +
            "    <name>科技重构</name>\n" +
            "    <sf>灵能（作为特殊能源与催化媒介）、旧时代科技知识</sf>\n" +
            "    <cr>完好的科技造物、精密零件、稀有材料、设计蓝图</cr>\n" +
            "    <cm>学习与理解科技原理，使用【文明火种】类异能解析、修复、改造、优化乃至创造科技装备</cm>\n" +
            "    <d><![CDATA[由极少数拥有技术类异能的觉醒者开创的体系。]]></d>\n" +
            "\t\t<ll>\n" +
            "\t\t  <ln>学徒</ln>\n" +
            "\t\t  <dd><![CDATA[能理解基础原理。]]></dd>\n" +
            "\t\t  <bc><![CDATA[成功独立修复一件复杂机械。]]></bc>\n" +
            "\t\t  <lsp>约100-120年</lsp>\n" +
            "\t\t  <pr><![CDATA[依赖自制武器和陷阱。]]></pr>\n" +
            "\t\t  <la>物品解析（初步）、基础修复</la>\n" +
            "\t\t  <step>认知期、动手期、应用期</step>\n" +
            "\t\t</ll>\n" +
            "  </ss>\n" +
            "</p>";

        WorldviewXmlParser.ParsedPowerSystems result = worldviewXmlParser.parsePowerSystemXml(xml, 1L);

        // Should parse successfully — no SAXParseException
        assertEquals(2, result.systems().size(), "Should parse 2 power systems");

        // First system: 灵能觉醒
        NovelPowerSystem sys1 = result.systems().get(0);
        assertEquals("灵能觉醒", sys1.getName());
        assertEquals("灵能潮汐辐射", sys1.getSourceFrom());
        assertEquals("灵能结晶、高能生物组织", sys1.getCoreResource());
        assertEquals(2, sys1.getLevels().size(), "灵能觉醒 should have 2 levels");
        assertEquals("觉醒者", sys1.getLevels().get(0).getLevelName());
        assertEquals("掌控者", sys1.getLevels().get(1).getLevelName());

        // Second system: 科技重构
        NovelPowerSystem sys2 = result.systems().get(1);
        assertEquals("科技重构", sys2.getName());
        assertEquals(1, sys2.getLevels().size(), "科技重构 should have 1 level");
        assertEquals("学徒", sys2.getLevels().get(0).getLevelName());

        // Verify step parsing — "初醒期、稳固期、显化期" is a single <step> text
        assertEquals(1, sys1.getLevels().get(0).getSteps().size());
        assertEquals("初醒期、稳固期、显化期", sys1.getLevels().get(0).getSteps().get(0).getLevelName());
    }

    // ======================== buildNameToIdMap ========================

    @Test
    void testBuildNameToIdMap_flat() {
        NovelFaction f1 = new NovelFaction();
        f1.setId(1L);
        f1.setName("Faction1");

        NovelFaction f2 = new NovelFaction();
        f2.setId(2L);
        f2.setName("Faction2");

        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        worldviewXmlParser.buildNameToIdMap(List.of(f1, f2), map);

        assertEquals(2, map.size());
        assertEquals(1L, map.get("Faction1"));
        assertEquals(2L, map.get("Faction2"));
    }

    @Test
    void testBuildNameToIdMap_nested() {
        NovelFaction child = new NovelFaction();
        child.setId(10L);
        child.setName("ChildFaction");

        NovelFaction parent = new NovelFaction();
        parent.setId(1L);
        parent.setName("ParentFaction");
        parent.setChildren(List.of(child));

        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        worldviewXmlParser.buildNameToIdMap(List.of(parent), map);

        assertEquals(2, map.size());
        assertEquals(1L, map.get("ParentFaction"));
        assertEquals(10L, map.get("ChildFaction"));
    }
}
