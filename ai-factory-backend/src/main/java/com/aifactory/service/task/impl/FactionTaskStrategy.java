package com.aifactory.service.task.impl;

import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelFactionRegionMapper;
import com.aifactory.mapper.NovelFactionRelationMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.fasterxml.jackson.databind.JsonNode;
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
 * 阵营势力独立生成任务策略
 * <p>
 * 独立生成阵营势力设定，需要已生成的地理环境和力量体系数据作为上下文。
 * 使用 llm_faction_create 提示词模板，输出 <f> 标签格式 XML。
 * <p>
 * 三步骤流程：
 * 1. clean_faction — 清理旧阵营势力数据
 * 2. generate_faction — AI 生成阵营势力（含地理环境和力量体系上下文注入）
 * 3. save_faction — DOM 解析 <f> XML，两遍插入（势力入库 + 关联建立）
 */
@Slf4j
@Component
public class FactionTaskStrategy implements TaskStrategy {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private FactionService factionService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private NovelFactionRegionMapper factionRegionMapper;

    @Autowired
    private NovelFactionRelationMapper factionRelationMapper;

    @Override
    public String getTaskType() {
        return "faction";
    }

    @Override
    public List<StepConfig> createSteps(com.aifactory.entity.AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧阵营势力数据", "clean_faction", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成阵营势力", "generate_faction", new HashMap<>()));
        steps.add(new StepConfig(3, "保存阵营势力", "save_faction", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(com.aifactory.entity.AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();
            switch (stepType) {
                case "clean_faction":
                    return cleanFaction(step, context);
                case "generate_faction":
                    return generateFaction(step, context);
                case "save_faction":
                    return saveFaction(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }
        } catch (Exception e) {
            log.error("执行阵营势力步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    // ======================== clean_faction ========================

    /**
     * 清理旧阵营势力数据
     */
    private StepResult cleanFaction(com.aifactory.entity.AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            log.info("开始清理旧阵营势力数据, projectId={}", projectId);

            factionService.deleteByProjectId(projectId);

            log.info("清理旧阵营势力数据完成, projectId={}", projectId);
            return StepResult.success(Map.of("cleaned", true), 100);
        } catch (Exception e) {
            log.error("清理旧阵营势力数据失败", e);
            return StepResult.failure("清理旧阵营势力数据失败: " + e.getMessage());
        }
    }

    // ======================== generate_faction ========================

    /**
     * AI 生成阵营势力 — 提示词由 DB 模板驱动，地理环境和力量体系上下文从 config 注入
     */
    private StepResult generateFaction(com.aifactory.entity.AiTaskStep step, TaskContext context) {
        try {
            Project project = projectMapper.selectById(context.getProjectId());
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String novelType = project.getNovelType();
            String tags = project.getTags();

            JsonNode config = context.getConfig();

            // 从 config 中读取地理环境和力量体系上下文（由 controller 注入）
            String geographyContext = config.has("geographyContext") ? config.get("geographyContext").asText() : "";
            String powerSystemContext = config.has("powerSystemContext") ? config.get("powerSystemContext").asText() : "";

            Map<String, Object> variables = new HashMap<>();
            variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
            variables.put("storyGenre", novelType != null && !novelType.isEmpty() ? BasicSettingsDictionary.getNovelType(novelType) : "待补充");

            if (tags != null && !tags.isEmpty()) {
                variables.put("tagsSection", "【标签】" + tags);
            } else {
                variables.put("tagsSection", "");
            }

            variables.put("geographyContext", geographyContext);
            variables.put("powerSystemContext", powerSystemContext);

            String prompt = promptTemplateService.executeTemplate("llm_faction_create", variables);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_faction_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成阵营势力原始响应:\n{}", responseContent);

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);
        } catch (Exception e) {
            log.error("AI生成阵营势力失败", e);
            return StepResult.failure("AI生成阵营势力失败: " + e.getMessage());
        }
    }

    // ======================== save_faction ========================

    /**
     * 保存阵营势力 — DOM 解析 <f> XML，两遍插入（势力入库 + 关联建立）
     */
    private StepResult saveFaction(com.aifactory.entity.AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("aiResponse");

            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的阵营势力内容");
            }

            saveFactionsFromXml(projectId, aiResponse);

            return StepResult.success(Map.of("saved", true), 100);
        } catch (Exception e) {
            log.error("保存阵营势力失败", e);
            return StepResult.failure("保存阵营势力失败: " + e.getMessage());
        }
    }

    // ======================== DOM Parsing (copied from WorldviewTaskStrategy) ========================

    /**
     * 从 AI 响应 XML 中手动 DOM 解析 <f> 势力阵营并保存
     * <p>
     * 两遍插入策略：第一遍存所有势力构建名称->ID映射，第二遍建立势力-地区和势力-势力关联。
     */
    private void saveFactionsFromXml(Long projectId, String aiResponse) {
        try {
            // 提取 <f>...</f> 片段
            int start = aiResponse.indexOf("<f>");
            int end = aiResponse.indexOf("</f>");
            if (start < 0 || end < 0) {
                log.info("未找到 <f> 势力标签，跳过入库");
                return;
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
                return;
            }

            // Two-pass data structures
            Map<String, Long> nameToIdMap = new LinkedHashMap<>();
            List<PendingFactionAssociations> pendingAssociations = new ArrayList<>();

            // Parse all faction nodes recursively (Pass 1 data collection)
            List<NovelFaction> rootFactions = parseFactionNodes(fElement, projectId, nameToIdMap, pendingAssociations);

            if (rootFactions.isEmpty()) {
                log.info("势力解析结果为空，跳过入库");
                return;
            }

            // Pass 1: Insert all factions via saveTree (builds name->ID map)
            factionService.saveTree(projectId, rootFactions);

            // After saveTree, populate nameToIdMap from the inserted factions
            buildNameToIdMap(rootFactions, nameToIdMap);

            // Pass 2: Create associations using nameToIdMap
            for (PendingFactionAssociations pending : pendingAssociations) {
                Long factionId = nameToIdMap.get(pending.factionName());
                if (factionId == null) {
                    log.warn("未找到势力ID，跳过关联，factionName={}", pending.factionName());
                    continue;
                }

                // Region associations
                for (String regionName : pending.regionNames()) {
                    Long regionId = findRegionIdByName(projectId, regionName);
                    if (regionId != null) {
                        NovelFactionRegion assoc = new NovelFactionRegion();
                        assoc.setFactionId(factionId);
                        assoc.setRegionId(regionId);
                        factionRegionMapper.insert(assoc);
                    } else {
                        log.warn("三级匹配均失败，未找到地区: {}，跳过关联", regionName);
                    }
                }

                // Faction-faction relations
                for (PendingRelation rel : pending.relations()) {
                    Long targetId = nameToIdMap.get(rel.targetName());
                    if (targetId != null) {
                        NovelFactionRelation relation = new NovelFactionRelation();
                        relation.setFactionId(factionId);
                        relation.setTargetFactionId(targetId);
                        relation.setRelationType(mapRelationType(rel.type()));
                        factionRelationMapper.insert(relation);
                    } else {
                        log.warn("未匹配到势力名称: {}，跳过关系", rel.targetName());
                    }
                }
            }

            log.info("势力入库完成，projectId={}，根节点数={}", projectId, rootFactions.size());

        } catch (Exception e) {
            log.error("保存势力失败，projectId={}", projectId, e);
        }
    }

    /**
     * 解析 <f> 下的直接子 <faction> 节点
     */
    private List<NovelFaction> parseFactionNodes(Element parent, Long projectId,
                                                  Map<String, Long> nameToIdMap,
                                                  List<PendingFactionAssociations> pendingAssociations) {
        List<NovelFaction> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "faction".equals(node.getNodeName())) {
                try {
                    result.add(parseFactionNode((Element) node, projectId, nameToIdMap, pendingAssociations));
                } catch (Exception e) {
                    log.warn("解析势力节点失败，跳过: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 递归解析单个 <faction> 节点
     * <p>
     * 使用 getChildNodes() 直接子元素迭代，不用 getElementsByTagName
     */
    private NovelFaction parseFactionNode(Element factionElement, Long projectId,
                                           Map<String, Long> nameToIdMap,
                                           List<PendingFactionAssociations> pendingAssociations) {
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
                    childFactions.add(parseFactionNode((Element) child, projectId, nameToIdMap, pendingAssociations));
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
            pendingAssociations.add(new PendingFactionAssociations(factionName, regionNames, relations));
        }

        return faction;
    }

    /**
     * 解析 <relation> 元素
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

    /**
     * Chinese faction type label -> English DB value
     * 正派->ally, 反派->hostile, 中立->neutral
     */
    private String mapFactionType(String chineseType) {
        if (chineseType == null) return null;
        return switch (chineseType) {
            case "正派" -> "ally";
            case "反派" -> "hostile";
            case "中立" -> "neutral";
            default -> chineseType;
        };
    }

    /**
     * Chinese relation type label -> English DB value
     * 盟友->ally, 敌对->hostile, 中立->neutral
     */
    private String mapRelationType(String chineseType) {
        if (chineseType == null) return null;
        return switch (chineseType) {
            case "盟友" -> "ally";
            case "敌对" -> "hostile";
            case "中立" -> "neutral";
            default -> chineseType;
        };
    }

    /**
     * Build name->ID map from saved faction tree (recursive)
     */
    private void buildNameToIdMap(List<NovelFaction> factions, Map<String, Long> nameToIdMap) {
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

    /**
     * Three-tier name matching for regions:
     * Tier 1: Exact match
     * Tier 2: Strip suffix then compare
     * Tier 3: Contains match (either direction)
     */
    private Long findRegionIdByName(Long projectId, String name) {
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
     * Three-tier name matching for power systems (same strategy as regions)
     */
    private Long findPowerSystemIdByName(Long projectId, String name) {
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

    /**
     * Pending region and relation associations for Pass 2
     */
    private record PendingFactionAssociations(
        String factionName,
        List<String> regionNames,
        List<PendingRelation> relations
    ) {}

    /**
     * Pending inter-faction relation
     */
    private record PendingRelation(
        String targetName,
        String type
    ) {}
}
