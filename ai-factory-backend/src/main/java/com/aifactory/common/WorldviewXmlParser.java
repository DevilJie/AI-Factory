package com.aifactory.common;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelPowerSystem;
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
