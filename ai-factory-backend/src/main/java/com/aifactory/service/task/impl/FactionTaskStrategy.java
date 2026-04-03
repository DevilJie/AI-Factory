package com.aifactory.service.task.impl;

import com.aifactory.common.WorldviewXmlParser;
import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelFactionRegionMapper;
import com.aifactory.mapper.NovelFactionRelationMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.FactionService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 阵营势力独立生成任务策略
 * <p>
 * 独立生成阵营势力设定，需要已生成的地理环境和力量体系数据作为上下文。
 * 使用 llm_faction_create 提示词模板，输出 {@code <f>} 标签格式 XML。
 * <p>
 * 三步骤流程：
 * 1. clean_faction — 清理旧阵营势力数据
 * 2. generate_faction — AI 生成阵营势力（含地理环境和力量体系上下文注入）
 * 3. save_faction — 委托 WorldviewXmlParser 解析 XML，两遍插入（势力入库 + 关联建立）
 *
 * @Author AI Factory
 * @Date 2026-04-03
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
    private NovelFactionRegionMapper factionRegionMapper;

    @Autowired
    private NovelFactionRelationMapper factionRelationMapper;

    @Autowired
    private WorldviewXmlParser worldviewXmlParser;

    @Override
    public String getTaskType() {
        return "faction";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧阵营势力数据", "clean_faction", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成阵营势力", "generate_faction", new HashMap<>()));
        steps.add(new StepConfig(3, "保存阵营势力", "save_faction", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
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
    private StepResult cleanFaction(AiTaskStep step, TaskContext context) {
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
    private StepResult generateFaction(AiTaskStep step, TaskContext context) {
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
     * 保存阵营势力 — 委托 WorldviewXmlParser 解析 {@code <f>} XML，
     * 两遍插入（势力入库 + 关联建立）
     */
    private StepResult saveFaction(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("aiResponse");

            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的阵营势力内容");
            }

            WorldviewXmlParser.ParsedFactions parsed = worldviewXmlParser.parseFactionXml(aiResponse, projectId);
            if (parsed.rootFactions().isEmpty()) {
                return StepResult.success(Map.of("saved", true, "count", 0), 100);
            }

            // Pass 1: Insert all factions via saveTree
            factionService.saveTree(projectId, parsed.rootFactions());

            // Build name->ID map from inserted factions
            Map<String, Long> nameToIdMap = new LinkedHashMap<>();
            worldviewXmlParser.buildNameToIdMap(parsed.rootFactions(), nameToIdMap);

            // Pass 2: Create associations
            for (WorldviewXmlParser.PendingAssociation pending : parsed.pendingAssociations()) {
                Long factionId = nameToIdMap.get(pending.factionName());
                if (factionId == null) {
                    log.warn("未找到势力ID，跳过关联，factionName={}", pending.factionName());
                    continue;
                }

                // Region associations
                for (String regionName : pending.regionNames()) {
                    Long regionId = worldviewXmlParser.findRegionIdByName(projectId, regionName);
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
                for (WorldviewXmlParser.PendingRelation rel : pending.relations()) {
                    Long targetId = nameToIdMap.get(rel.targetName());
                    if (targetId != null) {
                        NovelFactionRelation relation = new NovelFactionRelation();
                        relation.setFactionId(factionId);
                        relation.setTargetFactionId(targetId);
                        relation.setRelationType(worldviewXmlParser.mapRelationType(rel.type()));
                        factionRelationMapper.insert(relation);
                    } else {
                        log.warn("未匹配到势力名称: {}，跳过关系", rel.targetName());
                    }
                }
            }

            log.info("势力入库完成，projectId={}，根节点数={}", projectId, parsed.rootFactions().size());
            return StepResult.success(Map.of("saved", true), 100);

        } catch (Exception e) {
            log.error("保存阵营势力失败", e);
            return StepResult.failure("保存阵营势力失败: " + e.getMessage());
        }
    }
}
