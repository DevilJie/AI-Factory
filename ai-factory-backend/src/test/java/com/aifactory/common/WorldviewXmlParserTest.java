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
