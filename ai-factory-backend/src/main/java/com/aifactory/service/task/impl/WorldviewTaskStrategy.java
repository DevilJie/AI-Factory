package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.*;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.*;
import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 世界观生成任务策略
 * <p>
 * 改造要点：
 * 1. 提示词模板化 — 所有 prompt 内容由 DB 模板驱动，不再硬编码 XML 格式要求
 * 2. XML 解析力量体系入库 — AI 返回的 <p><system>...</system></p> 会被解析并写入
 * novel_power_system / novel_power_system_level / novel_power_system_level_step 三张表
 * 3. 关联表 novel_worldview_power_system 在保存时自动建立
 * 4. 移除 novel_worldview.powerSystem 文本字段的一切读写
 *
 * @Author CaiZy
 * @Date 2025-02-02
 */
@Slf4j
@Component
public class WorldviewTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private NovelPowerSystemMapper powerSystemMapper;

    @Autowired
    private NovelPowerSystemLevelMapper levelMapper;

    @Autowired
    private NovelPowerSystemLevelStepMapper stepMapper;

    @Autowired
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Autowired
    private XmlParser xmlParser;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private FactionService factionService;

    @Autowired
    private NovelFactionRegionMapper factionRegionMapper;

    @Autowired
    private NovelFactionRelationMapper factionRelationMapper;

    @Override
    public String getTaskType() {
        return "worldview";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            List<StepConfig> steps = new ArrayList<>();

            // 步骤: 检查是否已有世界观设定（删除旧数据 + 关联）
            steps.add(new StepConfig(1, "检查现有世界观", "check_existing", new HashMap<>()));

            // 步骤: 生成世界观设定
            steps.add(new StepConfig(2, "生成世界观设定", "generate_worldview", new HashMap<>()));

            // 步骤: 保存世界观设定
            steps.add(new StepConfig(3, "保存世界观设定", "save_worldview", new HashMap<>()));

            log.info("创建世界观任务步骤完成，项目ID: {}", task.getProjectId());
            return steps;

        } catch (Exception e) {
            log.error("创建世界观任务步骤失败", e);
            throw new RuntimeException("创建世界观任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "check_existing":
                    return checkExisting(step, context);
                case "generate_worldview":
                    return generateWorldview(step, context);
                case "save_worldview":
                    return saveWorldview(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    // ======================== check_existing ========================

    /**
     * 检查是否已有世界观设定，如有则删除旧世界观 + 关联的力量体系行
     */
    private StepResult checkExisting(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            log.info("检查项目 {} 是否已有世界观设定", projectId);

            NovelWorldview existingWorldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            boolean hadExisting = existingWorldview != null;

            if (hadExisting) {
                log.info("项目 {} 已有世界观设定，ID={}，将删除并重新生成", projectId, existingWorldview.getId());

                // 1. 查找关联的力量体系ID
                List<NovelWorldviewPowerSystem> associations = worldviewPowerSystemMapper.selectList(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existingWorldview.getId())
                );

                // 2. 级联删除力量体系（等级→境界→体系）
                for (NovelWorldviewPowerSystem assoc : associations) {
                    Long psId = assoc.getPowerSystemId();
                    // 删除境界
                    List<NovelPowerSystemLevel> levels = levelMapper.selectList(
                        new LambdaQueryWrapper<NovelPowerSystemLevel>()
                            .eq(NovelPowerSystemLevel::getPowerSystemId, psId)
                    );
                    for (NovelPowerSystemLevel level : levels) {
                        stepMapper.delete(
                            new LambdaQueryWrapper<NovelPowerSystemLevelStep>()
                                .eq(NovelPowerSystemLevelStep::getPowerSystemLevelId, level.getId())
                        );
                    }
                    // 删除等级
                    levelMapper.delete(
                        new LambdaQueryWrapper<NovelPowerSystemLevel>()
                            .eq(NovelPowerSystemLevel::getPowerSystemId, psId)
                    );
                    // 删除力量体系
                    powerSystemMapper.deleteById(psId);
                }

                // 3. 删除关联表行
                worldviewPowerSystemMapper.delete(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existingWorldview.getId())
                );

                // 4. 删除旧世界观关联的地理区域
                continentRegionService.deleteByProjectId(projectId);

                // 4.5. 删除旧势力数据
                factionService.deleteByProjectId(projectId);

                // 5. 删除旧世界观
                worldviewMapper.deleteById(existingWorldview.getId());
            } else {
                log.info("项目 {} 没有世界观设定，需要生成", projectId);
            }

            context.putSharedData("skipGeneration", false);
            return StepResult.success(Map.of("hasExisting", hadExisting), 100);

        } catch (Exception e) {
            log.error("检查现有世界观失败", e);
            return StepResult.failure("检查现有世界观失败: " + e.getMessage());
        }
    }

    // ======================== generate_worldview ========================

    /**
     * 生成世界观设定 — 提示词完全由 DB 模板驱动
     */
    private StepResult generateWorldview(AiTaskStep step, TaskContext context) {
        try {
            Project project = projectMapper.selectById(context.getProjectId());
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String novelType = project.getNovelType();
            String tags = project.getTags();

            // 构建提示词（模板已包含 XML 格式要求，不再手动追加）
            String prompt = buildWorldviewPrompt(projectDescription, storyTone, novelType, tags);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_worldview_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成世界观原始响应:\n{}", responseContent);

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("生成世界观设定失败", e);
            return StepResult.failure("生成世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== save_worldview ========================

    /**
     * 保存世界观设定：解析 XML、存储世界观基本信息 + 结构化力量体系入库 + 建立关联
     */
    private StepResult saveWorldview(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的世界观内容");
            }

            JsonNode config = context.getConfig();
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            // 解析并保存
            NovelWorldview worldview = parseAndSaveWorldview(projectId, aiResponse, storyGenre);

            if (worldview != null) {
                log.info("世界观设定保存成功，ID: {}", worldview.getId());

                updateProjectSetupStage(projectId, "worldview_configured");

                return StepResult.success(Map.of("worldviewId", worldview.getId()), 100);
            } else {
                return StepResult.failure("解析并保存世界观失败");
            }

        } catch (Exception e) {
            log.error("保存世界观设定失败", e);
            return StepResult.failure("保存世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== buildWorldviewPrompt ========================

    /**
     * 构建生成世界观的提示词 — 完全由 DB 模板驱动
     * <p>
     * 模板（llm_worldview_create）已包含完整的 XML 格式要求，
     * 这里只负责准备模板变量和补充 tags 信息。
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String novelType, String tags) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
        variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
        variables.put("novelType", novelType != null && !novelType.isEmpty() ? BasicSettingsDictionary.getNovelType(novelType) : "待补充");

        // tags 是可选的，模板中使用 {tagsSection} 占位
        if (tags != null && !tags.isEmpty()) {
            variables.put("tagsSection", "【标签】" + tags);
        } else {
            variables.put("tagsSection", "");
        }

        String templateCode = "llm_worldview_create";

        return promptTemplateService.executeTemplate(templateCode, variables);
    }

    // ======================== parseAndSaveWorldview ========================

    /**
     * 解析 AI 响应并保存世界观 + 力量体系
     * <p>
     * 流程：
     * 1. 用 XmlParser 解析 <w> 根标签下的 t/b/p/g/f/l/r 字段
     * 2. 将 t/b/g/f/l/r 保存到 novel_worldview（不含 powerSystem）
     * 3. 解析 <p> 下的 <system> 子节点，结构化写入 novel_power_system / level / step
     * 4. 建立 novel_worldview_power_system 关联
     */
    private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {

            WorldSettingXmlDto worldSetting = xmlParser.parse(aiResponse, WorldSettingXmlDto.class);

            // Step 2: 获取项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.error("项目不存在，projectId={}", projectId);
                return null;
            }

            LocalDateTime now = LocalDateTime.now();

            // Step 3: 保存世界观基本信息（不含 powerSystem、geography）
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);
            worldview.setWorldBackground(worldSetting.getBackground());
            worldview.setForces(worldSetting.getForces());
            worldview.setTimeline(worldSetting.getTimeline());
            worldview.setRules(worldSetting.getRules());
            worldview.setCreateTime(now);
            worldview.setUpdateTime(now);

            worldviewMapper.insert(worldview);
            log.info("世界观基本信息保存成功，ID: {}", worldview.getId());

            // Step 4: 手动 DOM 解析 <g> 地理区域并保存（Jackson XML 无法处理嵌套同名 <r> 标签）
            saveGeographyRegionsFromXml(projectId, aiResponse);

            // Step 5: 解析并保存结构化力量体系
            savePowerSystems(projectId, worldview.getId(), worldSetting.getSystems(), now);

            // Step 6: DOM 解析 <f> 势力阵营并保存（在地理和力量体系之后，因为势力引用它们的名称）
            saveFactionsFromXml(projectId, aiResponse);

            return worldview;

        } catch (Exception e) {
            log.error("解析世界观失败", e);
            log.error("AI响应: {}", aiResponse);
            return null;
        }
    }

    // ======================== saveGeographyRegions ========================

    /**
     * 从 AI 响应 XML 中手动 DOM 解析 <g> 地理区域并保存
     * <p>
     * 使用 DOM 而非 Jackson XML 是因为嵌套同名 <r> 标签 Jackson 无法正确处理
     */
    private void saveGeographyRegionsFromXml(Long projectId, String aiResponse) {
        try {
            // 提取 <g>...</g> 片段
            int start = aiResponse.indexOf("<g>");
            int end = aiResponse.indexOf("</g>");
            if (start < 0 || end < 0) {
                log.info("未找到 <g> 地理区域标签，跳过入库");
                return;
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
                return;
            }

            Element gElement = (Element) gNodes.item(0);
            List<NovelContinentRegion> rootNodes = parseRegionNodes(gElement, projectId);

            if (rootNodes.isEmpty()) {
                log.info("地理区域解析结果为空，跳过入库");
                return;
            }

            continentRegionService.saveTree(projectId, rootNodes);
            log.info("地理区域入库完成，projectId={}，根节点数={}", projectId, rootNodes.size());

        } catch (Exception e) {
            log.error("保存地理区域失败，projectId={}", projectId, e);
        }
    }

    /**
     * 递归解析 <r> 节点为 NovelContinentRegion 列表
     * <p>
     * XML格式：<r><n>区域名称</n><d><![CDATA[描述]]></d><r>子区域</r></r>
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
     * 解析单个 <r> 区域节点：读取 <n>（名称）和 <d>（描述），递归解析直接子 <r>
     */
    private NovelContinentRegion parseSingleRegion(Element rElement, Long projectId) {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setProjectId(projectId);

        // 从 <n> 子标签读取名称（兼容旧格式 name 属性）
        NodeList nNodes = rElement.getElementsByTagName("n");
        if (nNodes.getLength() > 0) {
            region.setName(nNodes.item(0).getTextContent().trim());
        } else {
            region.setName(rElement.getAttribute("name"));
        }

        // 从 <d> 子标签读取描述
        NodeList dNodes = rElement.getElementsByTagName("d");
        if (dNodes.getLength() > 0) {
            region.setDescription(dNodes.item(0).getTextContent().trim());
        }

        // 递归解析直接子 <r> 节点（避免误取孙子节点）
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

    // ======================== savePowerSystems ========================

    /**
     * 解析并保存结构化力量体系
     * <p>
     * 流程：
     * 1. 遍历每个 <system>，插入 novel_power_system
     * 2. 遍历 <levels>/<level>，插入 novel_power_system_level
     * 3. 遍历 <steps>/<step>，插入 novel_power_system_level_step
     * 4. 建立 novel_worldview_power_system 关联
     */
    private void savePowerSystems(Long projectId, Long worldviewId, WorldSettingXmlDto.Systems systems, LocalDateTime now) {
        if (systems == null || systems.getSystemList() == null || systems.getSystemList().isEmpty()) {
            log.info("无力量体系数据，跳过入库");
            return;
        }

        for (WorldSettingXmlDto.CultivationSystem cs : systems.getSystemList()) {
            // 1. 插入力量体系主记录
            NovelPowerSystem powerSystem = new NovelPowerSystem();
            powerSystem.setProjectId(projectId);
            powerSystem.setName(cs.getName());
            powerSystem.setSourceFrom(cs.getSourceFrom());
            powerSystem.setCoreResource(cs.getCoreResource());
            powerSystem.setCultivationMethod(cs.getCultivationMethod());
            powerSystem.setDescription(cs.getDescription());
            powerSystem.setCreateTime(now);
            powerSystem.setUpdateTime(now);
            powerSystemMapper.insert(powerSystem);
            log.info("力量体系保存成功，ID={}, name={}", powerSystem.getId(), cs.getName());

            // 2. 插入等级记录
            if (cs.getLevels() != null && cs.getLevels().getLevelList() != null) {
                int levelIndex = 1;
                for (WorldSettingXmlDto.CultivationLevel cl : cs.getLevels().getLevelList()) {
                    NovelPowerSystemLevel level = new NovelPowerSystemLevel();
                    level.setPowerSystemId(powerSystem.getId());
                    level.setLevel(levelIndex++);
                    level.setLevelName(cl.getLevelName());
                    level.setDescription(cl.getDescription());
                    level.setBreakthroughCondition(cl.getBreakthroughCondition());
                    level.setLifespan(cl.getLifespan());
                    level.setPowerRange(cl.getPowerRange());
                    level.setLandmarkAbility(cl.getLandmarkAbility());
                    level.setCreateTime(now);
                    level.setUpdateTime(now);
                    levelMapper.insert(level);
                    log.debug("等级保存成功，ID={}, name={}", level.getId(), cl.getLevelName());

                    // 3. 插入境界记录
                    if (cl.getSteps() != null && cl.getSteps().getStepList() != null) {
                        int stepIndex = 1;
                        for (String stepName : cl.getSteps().getStepList()) {
                            NovelPowerSystemLevelStep step = new NovelPowerSystemLevelStep();
                            step.setPowerSystemLevelId(level.getId());
                            step.setLevel(stepIndex++);
                            step.setLevelName(stepName);
                            step.setCreateTime(now);
                            step.setUpdateTime(now);
                            stepMapper.insert(step);
                        }
                        log.debug("已保存 {} 个境界", cl.getSteps().getStepList().size());
                    }
                }
                log.info("已保存 {} 个等级", cs.getLevels().getLevelList().size());
            }

            // 4. 建立世界观-力量体系关联
            NovelWorldviewPowerSystem association = new NovelWorldviewPowerSystem();
            association.setWorldviewId(worldviewId);
            association.setPowerSystemId(powerSystem.getId());
            worldviewPowerSystemMapper.insert(association);
            log.info("世界观-力量体系关联已建立，worldviewId={}, powerSystemId={}", worldviewId, powerSystem.getId());
        }

        log.info("力量体系入库完成，共 {} 套体系", systems.getSystemList().size());
    }

    // ======================== saveFactions ========================

    /**
     * 从 AI 响应 XML 中手动 DOM 解析 <f> 势力阵营并保存
     * <p>
     * 使用 DOM 而非 Jackson XML 是因为嵌套同名 <faction> 标签 Jackson 无法正确处理。
     * 两遍插入策略：第一遍存所有势力构建名称→ID映射，第二遍建立势力-地区和势力-势力关联。
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

            // Per D-08: Use getChildNodes() to find the <f> element, NOT getElementsByTagName
            // <f> is the only element child of our synthetic <root> wrapper
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
     * 使用 getChildNodes() 直接子元素迭代（per D-08），不用 getElementsByTagName
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

        // Parse nested child factions using getChildNodes() (NOT getElementsByTagName)
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
            default -> chineseType; // fallback: store as-is
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
     * Three-tier name matching for regions (per D-06):
     * Uses continentRegionService.listByProjectId() to query regions (service-oriented pattern).
     * Tier 1: Exact match
     * Tier 2: Strip suffix (宗/派/门/殿/阁/会/帮/谷/山/城/族/教/院/宫/楼/庄/寨/盟) then compare
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
     * Three-tier name matching for power systems (same strategy as regions).
     * Uses powerSystemService.listByProjectId() for service-oriented consistency.
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

    // ======================== updateProjectSetupStage ========================

    /**
     * 更新项目的 setup_stage 状态
     */
    private void updateProjectSetupStage(Long projectId, String stage) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                project.setSetupStage(stage);
                projectMapper.updateById(project);
                log.info("项目状态已更新，projectId={}, setupStage={}", projectId, stage);
            }
        } catch (Exception e) {
            log.error("更新项目状态失败，projectId={}, stage={}", projectId, stage, e);
        }
    }
}
