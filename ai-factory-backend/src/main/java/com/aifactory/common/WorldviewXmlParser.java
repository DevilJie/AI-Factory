package com.aifactory.common;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.entity.NovelPowerSystemLevel;
import com.aifactory.entity.NovelPowerSystemLevelStep;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.PowerSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Worldview XML parsing utility — shared DOM parsing for geography, factions, and name matching.
 * <p>
 * Consolidates DOM parsing logic previously duplicated in WorldviewTaskStrategy,
 * GeographyTaskStrategy, and FactionTaskStrategy. Uses DOM instead of Jackson XML
 * because nested same-name tags (e.g., nested {@code <r>} or {@code <faction>}) cannot
 * be correctly handled by Jackson.
 * <p>
 * Design decisions (per D-08):
 * - Made a @Component (not a pure utility) because findRegionIdByName and findPowerSystemIdByName
 *   need DB access through service beans.
 * - parseFactionXml returns a ParsedFactions record instead of doing DB insertion.
 *   The caller handles saveTree + buildNameToIdMap + Pass 2 associations.
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@Slf4j
@Component
public class WorldviewXmlParser {

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private PowerSystemService powerSystemService;

    // ======================== Public Record Types ========================

    /**
     * Parsed faction result: root factions + pending associations for Pass 2
     */
    public record ParsedFactions(
        List<NovelFaction> rootFactions,
        List<PendingAssociation> pendingAssociations
    ) {}

    /**
     * Pending association for a faction: regions and relations to resolve after saveTree
     */
    public record PendingAssociation(
        String factionName,
        List<String> regionNames,
        List<PendingRelation> relations
    ) {}

    /**
     * Pending inter-faction relation
     */
    public record PendingRelation(
        String targetName,
        String type
    ) {}

    /**
     * Parsed power system result: list of NovelPowerSystem with nested levels and steps
     */
    public record ParsedPowerSystems(
        List<NovelPowerSystem> systems
    ) {}

    // ======================== Geography Parsing ========================

    /**
     * Parse geography XML from AI response.
     * Extracts {@code <g>...</g>} fragment, wraps in {@code <root>}, DOM parses,
     * and returns a list of NovelContinentRegion tree nodes.
     *
     * @param aiResponse the full AI response containing {@code <g>...</g>} tags
     * @param projectId  the project ID to set on each region
     * @return list of root NovelContinentRegion nodes (may be empty)
     */
    public List<NovelContinentRegion> parseGeographyXml(String aiResponse, Long projectId) {
        try {
            // Extract <g>...</g> fragment
            int start = aiResponse.indexOf("<g>");
            int end = aiResponse.indexOf("</g>");
            if (start < 0 || end < 0) {
                log.info("未找到 <g> 地理区域标签，跳过入库");
                return Collections.emptyList();
            }
            String geographyXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

            // Sanitize XML to fix common LLM output issues before DOM parsing
            geographyXml = sanitizeXmlForDomParsing(geographyXml, new String[]{"r"});

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(geographyXml)));

            Element root = doc.getDocumentElement();
            NodeList gNodes = root.getElementsByTagName("g");
            if (gNodes.getLength() == 0) {
                log.info("<g> 标签内无内容，跳过入库");
                return Collections.emptyList();
            }

            Element gElement = (Element) gNodes.item(0);
            List<NovelContinentRegion> rootNodes = parseRegionNodes(gElement, projectId);

            if (rootNodes.isEmpty()) {
                log.info("地理区域解析结果为空，跳过入库");
            }

            return rootNodes;

        } catch (Exception e) {
            log.error("解析地理区域失败, projectId={}", projectId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Recursively parse {@code <r>} nodes into NovelContinentRegion list.
     * <p>
     * XML format: {@code <r><n>Name</n><d><![CDATA[Desc]]></d><r>Child</r></r>}
     */
    private List<NovelContinentRegion> parseRegionNodes(Element parent, Long projectId) {
        List<NovelContinentRegion> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "r".equals(node.getNodeName())) {
                result.add(parseSingleRegion((Element) node, projectId));
            }
        }
        return result;
    }

    /**
     * Parse a single {@code <r>} region node: read {@code <n>} (name) and {@code <d>} (description),
     * recursively parse direct child {@code <r>} nodes.
     */
    private NovelContinentRegion parseSingleRegion(Element rElement, Long projectId) {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setProjectId(projectId);

        // Read name from <n> sub-tag (fallback to name attribute for legacy format)
        NodeList nNodes = rElement.getElementsByTagName("n");
        if (nNodes.getLength() > 0) {
            region.setName(nNodes.item(0).getTextContent().trim());
        } else {
            region.setName(rElement.getAttribute("name"));
        }

        // Read description from <d> sub-tag
        NodeList dNodes = rElement.getElementsByTagName("d");
        if (dNodes.getLength() > 0) {
            region.setDescription(dNodes.item(0).getTextContent().trim());
        }

        // Recursively parse direct child <r> nodes (avoid accidentally taking grandchild nodes)
        List<NovelContinentRegion> childRegions = new ArrayList<>();
        NodeList children = rElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "r".equals(child.getNodeName())) {
                childRegions.add(parseSingleRegion((Element) child, projectId));
            }
        }
        if (!childRegions.isEmpty()) {
            region.setChildren(childRegions);
        }

        return region;
    }

    // ======================== Power System Parsing ========================

    public static void main(String[] args) {
        String ret = "<p>\n" +
                "  <ss>\n" +
                "    <name>古神道</name>\n" +
                "    <sf>血脉之力、混沌之气、众生愿力（情感与生命力）</sf>\n" +
                "    <cr>古神精血、混沌奇物、蕴含强烈情感的物品或生灵</cr>\n" +
                "    <cm>唤醒并纯化体内古神血脉，吞噬混沌之气，或吸收众生愿力（正面负面皆可）以强化己身</cm>\n" +
                "    <d><![CDATA[此体系为被污蔑为“魔”的古神后裔所传承。不依赖外在天道，而是挖掘自身血脉深处来自混沌古神的力量，或从世界本源（混沌）及众生情感中汲取能量。主修肉身与神魂本质，追求肉身成圣、意志不朽。战斗方式狂暴直接，近战无敌，神通多与血肉、灵魂、混沌相关，伴有精神污染特性。修炼越深，肉身越接近古神，会呈现非人特征，并承受来自血脉源头的混沌记忆冲击，但意识保持自我独立。]]></d>\n" +
                "    <ll>\n" +
                "      <ln>觉醒境</ln>\n" +
                "      <dd><![CDATA[感应到体内稀薄的古神血脉，初步唤醒，身体素质全方位超越凡人，获得基础超凡特性。]]></dd>\n" +
                "      <bc><![CDATA[经历生死危机或强烈情绪刺激，引动血脉共鸣。]]></bc>\n" +
                "      <lsp>约150年</lsp>\n" +
                "      <pr><![CDATA[力能扛鼎，迅如猎豹，恢复力惊人，不惧普通刀剑。]]></pr>\n" +
                "      <la>血脉沸腾（临时强化）</la>\n" +
                "      <step>血脉微动</step>\n" +
                "      <step>血脉显化</step>\n" +
                "      <step>血脉初醒</step>\n" +
                "    </ll>\n" +
                "    <ll>\n" +
                "      <ln>蜕变境</ln>\n" +
                "      <dd><![CDATA[血脉之力改造全身，完成第一次生命跃迁，可局部躯体异化（如鳞片、利爪），获得天赋神通雏形。]]></dd>\n" +
                "      <bc><![CDATA[吸收足够能量（混沌气或愿力），引导血脉之力洗练周身骨骼、内脏、骨髓。]]></bc>\n" +
                "      <lsp>约300年</lsp>\n" +
                "      <pr><![CDATA[肉身硬抗法器，徒手拆楼，天赋神通初显威能。]]></pr>\n" +
                "      <la>躯体异化，天赋神通雏形</la>\n" +
                "      <step>皮膜蜕变</step>\n" +
                "      <step>筋骨蜕变</step>\n" +
                "      <step>脏腑蜕变</step>\n" +
                "    </ll>\n" +
                "    <ll>\n" +
                "      <ln>真形境</ln>\n" +
                "      <dd><![CDATA[凝聚血脉真形，可在身后显化古神虚影，大幅提升战力，并能初步运用混沌之力。]]></dd>\n" +
                "      <bc><![CDATA[将觉醒的血脉之力凝聚、观想，形成稳定的本源真形，并成功引动一丝混沌之气入体。]]></bc>\n" +
                "      <lsp>约800年</lsp>\n" +
                "      <pr><![CDATA[真形加持下，战力飙升，可正面击破金丹修士的护体灵光。]]></pr>\n" +
                "      <la>血脉真形显化，混沌之力加持</la>\n" +
                "      <step>真形凝影</step>\n" +
                "      <step>真形凝实</step>\n" +
                "      <step>真形合一</step>\n" +
                "    </ll>\n" +
                "    <ll>\n" +
                "      <ln>法身境</ln>\n" +
                "      <dd><![CDATA[将血脉真形与肉身彻底融合，铸就古神法身，肉身强度堪比法宝，断肢重生，并开始接触灵魂层面的力量。]]></dd>\n" +
                "      <bc><![CDATA[以混沌之气为火，血脉为材，将真形烙印进每一寸血肉灵魂，完成法身铸造。]]></bc>\n" +
                "      <lsp>约2000年</lsp>\n" +
                "      <pr><![CDATA[法身不坏，硬抗元婴法术，近身可撕碎同阶修士。]]></pr>\n" +
                "      <la>法身不坏，断肢重生</la>\n" +
                "      <step>法身初铸</step>\n" +
                "      <step>法身小成</step>\n" +
                "      <step>法身圆满</step>\n" +
                "    </ll>\n" +
                "    <ll>\n" +
                "      <ln>混沌境</ln>\n" +
                "      <dd><![CDATA[肉身逐步向混沌形态转化，可在一定程度上化身混沌，免疫大多数物理和法则攻击，并能从虚空直接汲取混沌之气。此境界需直面血脉源头古神的混乱本质，保持自我清醒。]]></dd>\n" +
                "      <bc><![CDATA[将法身、魂火与混沌之气深度融合，使自身生命形态向混沌过渡，并成功在体内开辟混沌旋涡。]]></bc>\n" +
                "      <lsp>约10000年</lsp>\n" +
                "      <pr><![CDATA[化身混沌，侵蚀现实，战力对标炼虚、合体，对仙道法则有极强抗性。]]></pr>\n" +
                "      <la>混沌化身，法则免疫（部分）</la>\n" +
                "      <step>混沌侵染</step>\n" +
                "      <step>混沌共生</step>\n" +
                "      <step>混沌一体</step>\n" +
                "    </ll>\n" +
                "  </ss>\n" +
                "</p>";
        WorldviewXmlParser p = new WorldviewXmlParser();
        ParsedPowerSystems rr = p.parsePowerSystemXml(ret, 5L);
        System.out.println(rr.systems().size());
    }

    /**
     * Parse power system XML from AI response.
     * Extracts {@code <p>...</p>} fragment, wraps in {@code <root>}, DOM parses,
     * and returns a ParsedPowerSystems record containing NovelPowerSystem entities
     * with nested levels and steps.
     * <p>
     * Uses DOM parsing (like geography) because Jackson XmlParser cannot handle
     * the root element mismatch: AI returns {@code <p>} but WorldSettingXmlDto
     * expects {@code <w>}.
     *
     * @param aiResponse the full AI response containing {@code <p>...</p>} tags
     * @param projectId  the project ID to set on each power system
     * @return ParsedPowerSystems record (systems list may be empty)
     */
    public ParsedPowerSystems parsePowerSystemXml(String aiResponse, Long projectId) {
        try {
            // Extract <p>...</p> fragment
            int start = aiResponse.indexOf("<p>");
            int end = aiResponse.indexOf("</p>");
            if (start < 0 || end < 0) {
                log.info("未找到 <p> 力量体系标签，跳过入库");
                return new ParsedPowerSystems(Collections.emptyList());
            }
            String powerSystemXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

            // Sanitize XML to fix common LLM output issues before DOM parsing
            powerSystemXml = sanitizeXmlForDomParsing(powerSystemXml, new String[]{"ss", "ll"});

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(powerSystemXml)));

            Element root = doc.getDocumentElement();

            // Find the <p> element
            Element pElement = null;
            NodeList rootChildren = root.getChildNodes();
            for (int i = 0; i < rootChildren.getLength(); i++) {
                Node node = rootChildren.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "p".equals(node.getNodeName())) {
                    pElement = (Element) node;
                    break;
                }
            }
            if (pElement == null) {
                log.info("<p> 标签内无内容，跳过入库");
                return new ParsedPowerSystems(Collections.emptyList());
            }

            List<NovelPowerSystem> systems = new ArrayList<>();

            // Parse each <ss> (cultivation system) under <p>
            NodeList ssNodes = pElement.getChildNodes();
            for (int i = 0; i < ssNodes.getLength(); i++) {
                Node ssNode = ssNodes.item(i);
                if (ssNode.getNodeType() == Node.ELEMENT_NODE && "ss".equals(ssNode.getNodeName())) {
                    try {
                        systems.add(parseSingleSystem((Element) ssNode, projectId));
                    } catch (Exception e) {
                        log.warn("解析力量体系节点失败，跳过: {}", e.getMessage());
                    }
                }
            }

            if (systems.isEmpty()) {
                log.info("力量体系解析结果为空，跳过入库");
            }

            return new ParsedPowerSystems(systems);

        } catch (Exception e) {
            log.error("解析力量体系失败, projectId={}", projectId, e);
            return new ParsedPowerSystems(Collections.emptyList());
        }
    }

    /**
     * Parse a single {@code <ss>} system node into a NovelPowerSystem entity
     * with nested levels and steps.
     */
    private NovelPowerSystem parseSingleSystem(Element ssElement, Long projectId) {
        NovelPowerSystem system = new NovelPowerSystem();
        system.setProjectId(projectId);

        List<NovelPowerSystemLevel> levels = new ArrayList<>();
        NodeList children = ssElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();

            switch (tag) {
                case "name" -> system.setName(child.getTextContent().trim());
                case "sf" -> system.setSourceFrom(child.getTextContent().trim());
                case "cr" -> system.setCoreResource(child.getTextContent().trim());
                case "cm" -> system.setCultivationMethod(child.getTextContent().trim());
                case "d" -> system.setDescription(child.getTextContent().trim());
                case "lls" -> {
                    // Parse levels (legacy format: <lls> wrapping <ll> nodes)
                    NodeList llNodes = child.getChildNodes();
                    int levelIndex = 1;
                    for (int j = 0; j < llNodes.getLength(); j++) {
                        Node llNode = llNodes.item(j);
                        if (llNode.getNodeType() == Node.ELEMENT_NODE && "ll".equals(llNode.getNodeName())) {
                            try {
                                levels.add(parseSingleLevel((Element) llNode, levelIndex++));
                            } catch (Exception e) {
                                log.warn("解析境界节点失败，跳过: {}", e.getMessage());
                            }
                        }
                    }
                }
                case "ll" -> {
                    // Parse single level (new format: <ll> directly under <ss>)
                    try {
                        levels.add(parseSingleLevel((Element) child, levels.size() + 1));
                    } catch (Exception e) {
                        log.warn("解析境界节点失败，跳过: {}", e.getMessage());
                    }
                }
            }
        }

        if (!levels.isEmpty()) {
            system.setLevels(levels);
        }

        return system;
    }

    /**
     * Parse a single {@code <ll>} level node into a NovelPowerSystemLevel entity
     * with nested steps.
     */
    private NovelPowerSystemLevel parseSingleLevel(Element llElement, int levelIndex) {
        NovelPowerSystemLevel level = new NovelPowerSystemLevel();
        level.setLevel(levelIndex);

        List<NovelPowerSystemLevelStep> steps = new ArrayList<>();
        NodeList children = llElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();

            switch (tag) {
                case "ln" -> level.setLevelName(child.getTextContent().trim());
                case "dd" -> level.setDescription(child.getTextContent().trim());
                case "bc" -> level.setBreakthroughCondition(child.getTextContent().trim());
                case "lsp" -> level.setLifespan(child.getTextContent().trim());
                case "pr" -> level.setPowerRange(child.getTextContent().trim());
                case "la" -> level.setLandmarkAbility(child.getTextContent().trim());
                case "steps" -> {
                    // Parse step list (legacy format: <steps> wrapping <step> nodes)
                    NodeList stepNodes = child.getChildNodes();
                    int stepIndex = 1;
                    for (int j = 0; j < stepNodes.getLength(); j++) {
                        Node stepNode = stepNodes.item(j);
                        if (stepNode.getNodeType() == Node.ELEMENT_NODE && "step".equals(stepNode.getNodeName())) {
                            NovelPowerSystemLevelStep stepEntity = new NovelPowerSystemLevelStep();
                            stepEntity.setLevel(stepIndex++);
                            stepEntity.setLevelName(stepNode.getTextContent().trim());
                            steps.add(stepEntity);
                        }
                    }
                }
                case "step" -> {
                    // Parse single step (new format: <step> directly under <ll>)
                    NovelPowerSystemLevelStep stepEntity = new NovelPowerSystemLevelStep();
                    stepEntity.setLevel(steps.size() + 1);
                    stepEntity.setLevelName(child.getTextContent().trim());
                    steps.add(stepEntity);
                }
            }
        }

        if (!steps.isEmpty()) {
            level.setSteps(steps);
        }

        return level;
    }

    // ======================== Faction Parsing ========================

    /**
     * Parse faction XML from AI response.
     * Extracts {@code <f>...</f>} fragment, wraps in {@code <root>}, DOM parses,
     * and returns a ParsedFactions record containing root factions and pending associations.
     * <p>
     * The caller is responsible for:
     * 1. Calling factionService.saveTree(projectId, parsed.rootFactions())
     * 2. Calling buildNameToIdMap(parsed.rootFactions(), nameToIdMap)
     * 3. Processing parsed.pendingAssociations() for Pass 2
     *
     * @param aiResponse the full AI response containing {@code <f>...</f>} tags
     * @param projectId  the project ID to set on each faction
     * @return ParsedFactions record (rootFactions may be empty)
     */
    public ParsedFactions parseFactionXml(String aiResponse, Long projectId) {
        try {
            // Extract <f>...</f> fragment
            int start = aiResponse.indexOf("<f>");
            int end = aiResponse.indexOf("</f>");
            if (start < 0 || end < 0) {
                log.info("未找到 <f> 势力标签，跳过入库");
                return new ParsedFactions(Collections.emptyList(), Collections.emptyList());
            }
            String factionXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(factionXml)));

            Element root = doc.getDocumentElement();

            // Use getChildNodes() to find the <f> element (NOT getElementsByTagName)
            Element fElement = null;
            NodeList rootChildren = root.getChildNodes();
            for (int i = 0; i < rootChildren.getLength(); i++) {
                Node node = rootChildren.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "f".equals(node.getNodeName())) {
                    fElement = (Element) node;
                    break;
                }
            }
            if (fElement == null) {
                log.info("<f> 标签内无内容，跳过入库");
                return new ParsedFactions(Collections.emptyList(), Collections.emptyList());
            }

            List<PendingAssociation> pendingAssociations = new ArrayList<>();
            List<NovelFaction> rootFactions = parseFactionNodes(fElement, projectId, pendingAssociations);

            return new ParsedFactions(rootFactions, pendingAssociations);

        } catch (Exception e) {
            log.error("解析势力失败, projectId={}", projectId, e);
            return new ParsedFactions(Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Parse direct child {@code <faction>} nodes under a parent element.
     */
    private List<NovelFaction> parseFactionNodes(Element parent, Long projectId,
                                                  List<PendingAssociation> pendingAssociations) {
        List<NovelFaction> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "faction".equals(node.getNodeName())) {
                try {
                    result.add(parseFactionNode((Element) node, projectId, pendingAssociations));
                } catch (Exception e) {
                    log.warn("解析势力节点失败，跳过: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Recursively parse a single {@code <faction>} node.
     * <p>
     * Uses getChildNodes() for direct child iteration (per D-08), NOT getElementsByTagName.
     * Calls findPowerSystemIdByName during parsing because the faction entity needs the
     * power system ID before saveTree.
     */
    private NovelFaction parseFactionNode(Element factionElement, Long projectId,
                                           List<PendingAssociation> pendingAssociations) {
        NovelFaction faction = new NovelFaction();
        faction.setProjectId(projectId);

        NodeList children = factionElement.getChildNodes();
        List<String> regionNames = new ArrayList<>();
        List<PendingRelation> relations = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();

            switch (tag) {
                case "n" -> faction.setName(child.getTextContent().trim());
                case "d" -> faction.setDescription(child.getTextContent().trim());
                case "type" -> faction.setType(mapFactionType(child.getTextContent().trim()));
                case "power" -> {
                    String powerName = child.getTextContent().trim();
                    Long powerId = findPowerSystemIdByName(projectId, powerName);
                    if (powerId != null) {
                        faction.setCorePowerSystem(powerId);
                    } else {
                        log.warn("未匹配到力量体系: {}", powerName);
                    }
                }
                case "regions" -> {
                    String regionsText = child.getTextContent().trim();
                    regionNames = Arrays.stream(regionsText.split("[,，]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                }
                case "relation" -> {
                    PendingRelation rel = parseRelationElement((Element) child);
                    if (rel != null) relations.add(rel);
                }
            }
        }

        // Parse nested child factions using getChildNodes()
        List<NovelFaction> childFactions = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "faction".equals(child.getNodeName())) {
                try {
                    childFactions.add(parseFactionNode((Element) child, projectId, pendingAssociations));
                } catch (Exception e) {
                    log.warn("解析子势力节点失败，跳过: {}", e.getMessage());
                }
            }
        }
        if (!childFactions.isEmpty()) {
            faction.setChildren(childFactions);
        }

        // Register pending associations for Pass 2
        String factionName = faction.getName();
        if (factionName != null && (!regionNames.isEmpty() || !relations.isEmpty())) {
            pendingAssociations.add(new PendingAssociation(factionName, regionNames, relations));
        }

        return faction;
    }

    /**
     * Parse a {@code <relation>} element.
     */
    private PendingRelation parseRelationElement(Element relationElement) {
        String target = null;
        String type = null;
        NodeList children = relationElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();
            if ("target".equals(tag)) {
                target = child.getTextContent().trim();
            } else if ("type".equals(tag)) {
                type = child.getTextContent().trim();
            }
        }
        if (target != null && type != null) {
            return new PendingRelation(target, type);
        }
        return null;
    }

    // ======================== Type Mapping ========================

    /**
     * Chinese faction type label -> English DB value.
     * 正派 -> ally, 反派 -> hostile, 中立 -> neutral
     */
    public String mapFactionType(String chineseType) {
        if (chineseType == null) return null;
        return switch (chineseType) {
            case "正派" -> "ally";
            case "反派" -> "hostile";
            case "中立" -> "neutral";
            default -> chineseType;
        };
    }

    /**
     * Chinese relation type label -> English DB value.
     * 盟友 -> ally, 敌对 -> hostile, 中立 -> neutral
     */
    public String mapRelationType(String chineseType) {
        if (chineseType == null) return null;
        return switch (chineseType) {
            case "盟友" -> "ally";
            case "敌对" -> "hostile";
            case "中立" -> "neutral";
            default -> chineseType;
        };
    }

    // ======================== Name Matching ========================

    /**
     * Three-tier Chinese name matching for regions.
     * Tier 1: Exact match
     * Tier 2: Strip common suffix (宗/派/门/殿/阁/会/帮/谷/山/城/族/教/院/宫/楼/庄/寨/盟) then compare
     * Tier 3: Contains match (either direction)
     *
     * @param projectId the project ID
     * @param name      the region name to search for
     * @return the matching region ID, or null if no match
     */
    public Long findRegionIdByName(Long projectId, String name) {
        if (name == null || name.isEmpty()) return null;

        List<NovelContinentRegion> regions = continentRegionService.listByProjectId(projectId);

        // Tier 1: Exact match
        for (NovelContinentRegion r : regions) {
            if (name.equals(r.getName())) return r.getId();
        }

        // Tier 2: Strip common suffixes and compare
        String stripped = name.replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
        for (NovelContinentRegion r : regions) {
            String rStripped = r.getName().replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
            if (!stripped.isEmpty() && stripped.equals(rStripped)) return r.getId();
        }

        // Tier 3: Contains match
        for (NovelContinentRegion r : regions) {
            if (r.getName().contains(name) || name.contains(r.getName())) return r.getId();
        }

        log.warn("三级匹配均失败，未找到地区: {}", name);
        return null;
    }

    /**
     * Three-tier Chinese name matching for power systems (same strategy as regions).
     *
     * @param projectId the project ID
     * @param name      the power system name to search for
     * @return the matching power system ID, or null if no match
     */
    public Long findPowerSystemIdByName(Long projectId, String name) {
        if (name == null || name.isEmpty()) return null;

        List<NovelPowerSystem> systems = powerSystemService.listByProjectId(projectId);

        // Tier 1: Exact match
        for (NovelPowerSystem ps : systems) {
            if (name.equals(ps.getName())) return ps.getId();
        }

        // Tier 2: Strip suffixes and compare
        String stripped = name.replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
        for (NovelPowerSystem ps : systems) {
            String psStripped = ps.getName().replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
            if (!stripped.isEmpty() && stripped.equals(psStripped)) return ps.getId();
        }

        // Tier 3: Contains match
        for (NovelPowerSystem ps : systems) {
            if (ps.getName().contains(name) || name.contains(ps.getName())) return ps.getId();
        }

        log.warn("三级匹配均失败，未找到力量体系: {}", name);
        return null;
    }

    // ======================== XML Sanitization ========================

    /**
     * Sanitize LLM-generated XML to fix common structural issues before DOM parsing.
     * <p>
     * Common LLM issues:
     * 1. Extra closing tags (e.g., {@code </ll>} without matching {@code <ll>} opening)
     * 2. Missing closing tags (e.g., {@code <ll>} without {@code </ll>})
     * 3. Raw {@code &} or {@code <} in non-CDATA text content
     * <p>
     * Strategy:
     * 1. Escape illegal XML characters in text nodes (not inside tags or CDATA)
     * 2. For each container tag, count opens vs closes and fix imbalances:
     *    - Extra closes: remove the last N excess closing tags
     *    - Missing closes: append closing tags at the end of the XML
     *
     * @param xml           the XML string to sanitize
     * @param containerTags tag names that are commonly repeated and may have balance issues
     * @return sanitized XML string
     */
    private String sanitizeXmlForDomParsing(String xml, String[] containerTags) {
        // Step 1: Escape illegal XML characters in text nodes
        xml = escapeIllegalXmlChars(xml);

        // Step 2: Fix tag balance
        for (String tag : containerTags) {
            String openTag = "<" + tag + ">";
            String closeTag = "</" + tag + ">";

            int openCount = countTagOccurrences(xml, openTag);
            int closeCount = countTagOccurrences(xml, closeTag);

            if (closeCount == openCount) continue; // already balanced

            if (closeCount > openCount) {
                // Remove stray closing tags using a stack-based approach.
                // Walk through the XML tracking opens vs closes; any close without
                // a matching open (depth=0) is a stray and should be removed.
                // This correctly identifies the stray tag regardless of its position,
                // unlike naive indexOf/lastIndexOf which removes the wrong tag.
                List<int[]> strayPositions = findStrayClosingPositions(xml, openTag, closeTag);
                // Remove strays from end to start to preserve earlier positions
                for (int j = strayPositions.size() - 1; j >= 0; j--) {
                    int pos = strayPositions.get(j)[0];
                    xml = xml.substring(0, pos) + xml.substring(pos + closeTag.length());
                }
                log.debug("XML sanitizer: removed {} stray </{}> tags", strayPositions.size(), tag);
            } else {
                // Add missing closing tags at the end
                int missing = openCount - closeCount;
                StringBuilder suffix = new StringBuilder();
                for (int i = 0; i < missing; i++) {
                    suffix.append(closeTag);
                }
                xml = xml + suffix;
                log.debug("XML sanitizer: added {} missing </{}> tags", missing, tag);
            }
        }
        return xml;
    }

    /**
     * Escape illegal XML characters in text nodes (outside tags and CDATA sections).
     * <p>
     * LLM output may contain raw {@code &} or {@code <} in non-CDATA text content,
     * which causes SAXParseException: "The content of elements must consist of
     * well-formed character data or markup."
     * <p>
     * Strategy: Replace CDATA sections with placeholders, escape {@code &} and {@code <}
     * in remaining text nodes, then restore CDATA sections.
     *
     * @param xml the XML string to escape
     * @return XML string with illegal characters properly escaped
     */
    private String escapeIllegalXmlChars(String xml) {
        // Phase 1: Extract and replace CDATA sections with placeholders
        // CDATA content is already safe and must not be modified
        List<String> cdataSections = new ArrayList<>();
        StringBuilder buffer = new StringBuilder(xml);
        String cdataOpen = "<![CDATA[";
        String cdataClose = "]]>";

        int searchStart = 0;
        while (searchStart < buffer.length()) {
            int cStart = buffer.indexOf(cdataOpen, searchStart);
            if (cStart < 0) break;

            int cEnd = buffer.indexOf(cdataClose, cStart + cdataOpen.length());
            if (cEnd < 0) break;

            // Extract the full CDATA section including delimiters
            String cdata = buffer.substring(cStart, cEnd + cdataClose.length());
            cdataSections.add(cdata);

            // Replace with placeholder
            String placeholder = "\u0000CDATA" + (cdataSections.size() - 1) + "\u0000";
            buffer.replace(cStart, cEnd + cdataClose.length(), placeholder);
            searchStart = cStart + placeholder.length();
        }

        // Phase 2: Escape illegal chars in remaining text
        // Text nodes are content between > and < that doesn't start with </ (closing tag)
        String result = buffer.toString();
        // Escape & first (before <, since &amp; contains &)
        // Only replace & that are NOT already part of an entity reference
        result = result.replaceAll("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)", "&amp;");
        // Escape < that are NOT part of tags (opening, closing, or self-closing)
        // A < that is NOT followed by / or a letter (tag name) is illegal
        // But we need to be careful: < followed by anything that could be a tag should remain
        // Safer approach: replace < that is NOT followed by /, !, or a letter (i.e., not a tag)
        // Actually, simplest safe approach: only escape < in clearly text positions
        // Since CDATA is already removed, any < in text nodes IS illegal
        // But we can't easily distinguish text < from tag < without a full parser
        // The safest approach: < that appears between > and a closing context is likely text
        // For now, we use a pragmatic regex: < not followed by / or a letter is illegal text
        result = result.replaceAll("<(?![/!a-zA-Z])", "&lt;");

        // Phase 3: Restore CDATA sections
        for (int i = 0; i < cdataSections.size(); i++) {
            String placeholder = "\u0000CDATA" + i + "\u0000";
            result = result.replace(placeholder, cdataSections.get(i));
        }

        return result;
    }

    /**
     * Count non-overlapping occurrences of a substring.
     */
    private int countTagOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Find positions of stray closing tags (closing tags with no matching opening tag)
     * using a stack-based approach. Walks through the XML tracking open/close depth;
     * any close tag encountered when depth is 0 is a stray.
     *
     * @param xml      the XML string to scan
     * @param openTag  the opening tag to match (e.g., "&lt;ll&gt;")
     * @param closeTag the closing tag to match (e.g., "&lt;/ll&gt;")
     * @return list of [position, type] pairs where type=1 means stray close; positions are in ascending order
     */
    private List<int[]> findStrayClosingPositions(String xml, String openTag, String closeTag) {
        List<int[]> positions = new ArrayList<>();

        // Collect all open and close tag positions in order
        List<int[]> allTags = new ArrayList<>(); // [position, type] where type=0 for open, type=1 for close
        int i = 0;
        while (i < xml.length()) {
            if (xml.startsWith(openTag, i)) {
                allTags.add(new int[]{i, 0});
                i += openTag.length();
            } else if (xml.startsWith(closeTag, i)) {
                allTags.add(new int[]{i, 1});
                i += closeTag.length();
            } else {
                i++;
            }
        }

        // Use a stack to find stray closes: any close tag when stack is empty is stray
        int depth = 0;
        for (int[] tagInfo : allTags) {
            if (tagInfo[1] == 0) { // open tag
                depth++;
            } else { // close tag
                if (depth > 0) {
                    depth--; // matched with an open
                } else {
                    positions.add(tagInfo); // stray close with no matching open
                }
            }
        }

        return positions;
    }

    // ======================== Utility ========================

    /**
     * Build name->ID map from saved faction tree (recursive).
     *
     * @param factions    the faction tree (after saveTree, so IDs are populated)
     * @param nameToIdMap the map to populate
     */
    public void buildNameToIdMap(List<NovelFaction> factions, Map<String, Long> nameToIdMap) {
        if (factions == null) return;
        for (NovelFaction f : factions) {
            if (f.getName() != null && f.getId() != null) {
                nameToIdMap.put(f.getName(), f.getId());
            }
            if (f.getChildren() != null) {
                buildNameToIdMap(f.getChildren(), nameToIdMap);
            }
        }
    }
}
