package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.*;
import com.aifactory.service.ContinentRegionService;
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

import java.time.LocalDateTime;
import java.util.*;

/**
 * 分卷优化任务策略
 * 用于AI优化单个分卷详情
 *
 * @Author AI Factory
 * @Date 2026-03-16
 */
@Slf4j
@Component
public class VolumeOptimizeTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Override
    public String getTaskType() {
        return "volume_optimize";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            List<StepConfig> steps = new ArrayList<>();

            // 步骤1: 验证分卷状态（无章节规划）
            steps.add(new StepConfig(1, "验证分卷状态", "validate_volume", new HashMap<>()));

            // 步骤2: 构建上下文（前面所有卷 + 世界观）
            steps.add(new StepConfig(2, "构建上下文", "build_context", new HashMap<>()));

            // 步骤3: 调用AI生成分卷详情
            steps.add(new StepConfig(3, "生成分卷详情", "generate_volume", new HashMap<>()));

            // 步骤4: 保存结果
            steps.add(new StepConfig(4, "保存结果", "save_result", new HashMap<>()));

            log.info("创建分卷优化任务步骤完成，任务ID: {}", task.getId());

            return steps;

        } catch (Exception e) {
            log.error("创建分卷优化任务步骤失败", e);
            throw new RuntimeException("创建分卷优化任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "validate_volume":
                    return validateVolume(step, context);
                case "build_context":
                    return buildContext(step, context);
                case "generate_volume":
                    return generateVolume(step, context);
                case "save_result":
                    return saveResult(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    /**
     * 步骤1: 验证分卷状态
     * 检查该分卷是否已有章节规划，如果有则返回错误
     */
    private StepResult validateVolume(AiTaskStep step, TaskContext context) {
        try {
            JsonNode config = context.getConfig();
            Long volumeId = config.has("volumeId") ? config.get("volumeId").asLong() : null;

            if (volumeId == null) {
                return StepResult.failure("缺少分卷ID参数");
            }

            // 查询分卷
            NovelVolumePlan volume = volumePlanMapper.selectById(volumeId);
            if (volume == null) {
                return StepResult.failure("分卷不存在，ID: " + volumeId);
            }

            // 检查是否已有章节规划
            Long chapterCount = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getVolumePlanId, volumeId)
            );

            if (chapterCount != null && chapterCount > 0) {
                return StepResult.failure("该分卷已有章节规划，无法优化。请先删除章节规划后再试。");
            }

            // 保存分卷信息到上下文
            context.putSharedData("volume", volume);
            context.putSharedData("volumeId", volumeId);
            context.putSharedData("projectId", volume.getProjectId());
            context.putSharedData("volumeNumber", volume.getVolumeNumber());

            log.info("分卷验证通过，volumeId={}, volumeNumber={}", volumeId, volume.getVolumeNumber());

            return StepResult.success(Map.of(
                "volumeId", volumeId,
                "volumeNumber", volume.getVolumeNumber()
            ), 100);

        } catch (Exception e) {
            log.error("验证分卷状态失败", e);
            return StepResult.failure("验证分卷状态失败: " + e.getMessage());
        }
    }

    /**
     * 步骤2: 构建上下文
     * 查询前面所有卷的摘要和世界观设定
     */
    private StepResult buildContext(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = (Long) context.getSharedData("projectId");
            Integer volumeNumber = (Integer) context.getSharedData("volumeNumber");

            // 1. 查询前面所有卷的摘要
            String previousVolumesInfo = buildPreviousVolumesInfo(projectId, volumeNumber);
            context.putSharedData("previousVolumesInfo", previousVolumesInfo);

            // 2. 查询世界观设定
            String worldviewInfo = buildWorldviewInfo(projectId);
            context.putSharedData("worldviewInfo", worldviewInfo);

            // 3. 获取项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return StepResult.failure("项目不存在，ID: " + projectId);
            }
            context.putSharedData("project", project);

            log.info("上下文构建完成，projectId={}, volumeNumber={}", projectId, volumeNumber);

            return StepResult.success(Map.of(
                "previousVolumesInfoLength", previousVolumesInfo.length(),
                "hasWorldview", worldviewInfo.length() > 0
            ), 100);

        } catch (Exception e) {
            log.error("构建上下文失败", e);
            return StepResult.failure("构建上下文失败: " + e.getMessage());
        }
    }

    /**
     * 步骤3: 调用AI生成分卷详情
     */
    private StepResult generateVolume(AiTaskStep step, TaskContext context) {
        try {
            JsonNode config = context.getConfig();
            Project project = (Project) context.getSharedData("project");
            Integer volumeNumber = (Integer) context.getSharedData("volumeNumber");
            String previousVolumesInfo = (String) context.getSharedData("previousVolumesInfo");
            String worldviewInfo = (String) context.getSharedData("worldviewInfo");
            Integer targetChapterCount = config.has("targetChapterCount") ? config.get("targetChapterCount").asInt() : 50;

            // 构建提示词
            String prompt = buildOptimizePrompt(project, volumeNumber, targetChapterCount, previousVolumesInfo, worldviewInfo);

            // 调用AI生成
            Long volumeId = (Long) context.getSharedData("volumeId");
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(project.getId());
            aiRequest.setVolumePlanId(volumeId);
            aiRequest.setRequestType("llm_volume_optimize");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(4000);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI优化分卷原始响应:\n{}", responseContent);

            // 保存AI响应到上下文
            context.putSharedData("aiResponse", responseContent);

            return StepResult.success(Map.of("aiResponseLength", responseContent.length()), 100);

        } catch (Exception e) {
            log.error("生成分卷详情失败", e);
            return StepResult.failure("生成分卷详情失败: " + e.getMessage());
        }
    }

    /**
     * 步骤4: 保存结果
     */
    private StepResult saveResult(AiTaskStep step, TaskContext context) {
        try {
            Long volumeId = (Long) context.getSharedData("volumeId");
            NovelVolumePlan volume = (NovelVolumePlan) context.getSharedData("volume");
            String aiResponse = (String) context.getSharedData("aiResponse");
            JsonNode config = context.getConfig();
            Integer targetChapterCount = config.has("targetChapterCount") ? config.get("targetChapterCount").asInt() : 50;

            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的分卷内容");
            }

            // 解析XML响应
            Map<String, String> volumeData = parseVolumeXml(aiResponse);

            if (volumeData.isEmpty()) {
                log.warn("解析分卷XML失败，数据为空");
                return StepResult.failure("解析分卷详情失败");
            }

            // 更新分卷信息
            LocalDateTime now = LocalDateTime.now();
            volume.setVolumeTitle(volumeData.getOrDefault("volumeTitle", volume.getVolumeTitle()));
            volume.setVolumeTheme(volumeData.getOrDefault("volumeTheme", ""));
            volume.setMainConflict(volumeData.getOrDefault("mainConflict", ""));
            volume.setPlotArc(volumeData.getOrDefault("plotArc", ""));
            volume.setCoreGoal(volumeData.getOrDefault("coreGoal", ""));
            volume.setKeyEvents(volumeData.getOrDefault("keyEvents", ""));
            volume.setClimax(volumeData.getOrDefault("climax", ""));
            volume.setEnding(volumeData.getOrDefault("ending", ""));
            volume.setVolumeDescription(volumeData.getOrDefault("volumeDescription", ""));
            volume.setVolumeNotes(volumeData.getOrDefault("volumeNotes", ""));
            volume.setTimelineSetting(volumeData.getOrDefault("timelineSetting", ""));
            volume.setTargetChapterCount(targetChapterCount);
            volume.setUpdateTime(now);

            volumePlanMapper.updateById(volume);

            log.info("分卷优化保存成功，volumeId={}", volumeId);

            return StepResult.success(Map.of(
                "volumeId", volumeId,
                "volumeTitle", volume.getVolumeTitle()
            ), 100);

        } catch (Exception e) {
            log.error("保存分卷结果失败", e);
            return StepResult.failure("保存分卷结果失败: " + e.getMessage());
        }
    }

    /**
     * 构建前面所有卷的摘要信息
     */
    private String buildPreviousVolumesInfo(Long projectId, Integer currentVolumeNumber) {
        StringBuilder info = new StringBuilder();

        if (currentVolumeNumber <= 1) {
            return "【前面分卷】无（当前为第一卷）";
        }

        // 查询前面所有卷
        List<NovelVolumePlan> previousVolumes = volumePlanMapper.selectList(
            new LambdaQueryWrapper<NovelVolumePlan>()
                .eq(NovelVolumePlan::getProjectId, projectId)
                .lt(NovelVolumePlan::getVolumeNumber, currentVolumeNumber)
                .orderByAsc(NovelVolumePlan::getVolumeNumber)
        );

        if (previousVolumes.isEmpty()) {
            return "【前面分卷】无（未找到前面的分卷）";
        }

        info.append("【前面分卷摘要】\n");
        for (NovelVolumePlan vol : previousVolumes) {
            info.append("第").append(vol.getVolumeNumber()).append("卷：").append(vol.getVolumeTitle()).append("\n");
            if (vol.getVolumeTheme() != null && !vol.getVolumeTheme().isEmpty()) {
                info.append("  主旨：").append(vol.getVolumeTheme()).append("\n");
            }
            if (vol.getMainConflict() != null && !vol.getMainConflict().isEmpty()) {
                info.append("  冲突：").append(vol.getMainConflict()).append("\n");
            }
            if (vol.getEnding() != null && !vol.getEnding().isEmpty()) {
                info.append("  收尾：").append(vol.getEnding()).append("\n");
            }
            info.append("\n");
        }

        return info.toString();
    }

    /**
     * 构建世界观信息
     */
    private String buildWorldviewInfo(Long projectId) {
        NovelWorldview worldview = worldviewMapper.selectOne(
            new LambdaQueryWrapper<NovelWorldview>()
                .eq(NovelWorldview::getProjectId, projectId)
        );

        if (worldview == null) {
            return "";
        }

        // 填充地理环境信息
        continentRegionService.fillGeography(worldview);

        StringBuilder info = new StringBuilder();
        info.append("【世界观设定】\n");
        info.append("- 世界类型：").append(worldview.getWorldType()).append("\n");

        if (worldview.getWorldBackground() != null && !worldview.getWorldBackground().isEmpty()) {
            info.append("- 世界背景：").append(worldview.getWorldBackground()).append("\n");
        }
        String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
        if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
            info.append("- 力量体系：").append(powerConstraint).append("\n");
        }
        if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
            info.append("- 地理环境：").append(worldview.getGeography()).append("\n");
        }
        if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
            info.append("- 势力分布：").append(worldview.getForces()).append("\n");
        }

        return info.toString();
    }

    /**
     * 构建优化提示词
     */
    private String buildOptimizePrompt(Project project, Integer volumeNumber, Integer targetChapterCount,
                                        String previousVolumesInfo, String worldviewInfo) {
        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("volumeNumber", volumeNumber);
        variables.put("targetChapterCount", targetChapterCount);
        variables.put("projectDescription", project.getDescription() != null ? project.getDescription() : "待补充");
        variables.put("storyTone", project.getStoryTone() != null ? project.getStoryTone() : "待补充");
        variables.put("storyGenre", project.getStoryGenre() != null ? project.getStoryGenre() : "待补充");
        variables.put("worldviewInfo", worldviewInfo);
        variables.put("previousVolumesInfo", previousVolumesInfo);

        // 执行模板
        return promptTemplateService.executeTemplate("llm_volume_optimize", variables);
    }

    /**
     * 解析单个分卷的XML
     */
    private Map<String, String> parseVolumeXml(String xml) {
        Map<String, String> result = new HashMap<>();
        try {
            // 提取XML内容
            String xmlStr = extractXml(xml);

            // 提取 <v> 标签内容（单个分卷）
            int vStart = xmlStr.indexOf("<v>");
            int vEnd = xmlStr.indexOf("</v>");
            if (vStart == -1 || vEnd == -1) {
                log.warn("未找到 <v> 标签");
                return result;
            }
            String volumeContent = xmlStr.substring(vStart + 3, vEnd);

            // 定义字段映射：标签名 -> 标准字段名
            Map<String, String> fieldMapping = new LinkedHashMap<>();
            fieldMapping.put("T", "volumeTitle");
            fieldMapping.put("Z", "volumeTheme");
            fieldMapping.put("F", "mainConflict");
            fieldMapping.put("P", "plotArc");
            fieldMapping.put("O", "coreGoal");
            fieldMapping.put("H", "climax");
            fieldMapping.put("R", "ending");
            fieldMapping.put("D", "volumeDescription");
            fieldMapping.put("L", "timelineSetting");
            fieldMapping.put("B", "volumeNotes");

            // 提取各个字段（普通CDATA字段）
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String tag = entry.getKey();
                String fieldName = entry.getValue();
                String value = extractCDataContent(volumeContent, tag);
                if (value != null && !value.isEmpty()) {
                    result.put(fieldName, value);
                }
            }

            // 特殊处理：keyEvents使用XML子节点格式解析
            String keyEventsJson = parseKeyEventsFromXml(volumeContent);
            if (keyEventsJson != null) {
                result.put("keyEvents", keyEventsJson);
            }

            log.info("解析分卷XML成功，提取到 {} 个字段", result.size());

        } catch (Exception e) {
            log.error("解析分卷XML失败", e);
        }
        return result;
    }

    /**
     * 从响应中提取XML内容
     */
    private String extractXml(String response) {
        String xmlStr = response;

        // 1. 提取代码块中的XML
        if (response.contains("```xml")) {
            int start = response.indexOf("```xml") + 5;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        }

        // 2. 智能提取最外层的XML对象
        String[] commonRootTags = {"<V>", "<v>"};
        for (String tag : commonRootTags) {
            String closingTag = "</" + tag.substring(1);
            int firstTag = xmlStr.indexOf(tag);
            int lastTag = xmlStr.lastIndexOf(closingTag);

            if (firstTag >= 0 && lastTag > firstTag) {
                xmlStr = xmlStr.substring(firstTag, lastTag + closingTag.length());
                break;
            }
        }

        return xmlStr;
    }

    /**
     * 提取CDATA内容 - 使用字符串操作而非正则，更可靠
     */
    private String extractCDataContent(String content, String tagName) {
        // 查找标签开始：<T>
        String openTag = "<" + tagName + ">";
        String openTagWithSpace = "<" + tagName + " ";
        int tagStart = content.indexOf(openTag);
        if (tagStart == -1) {
            tagStart = content.indexOf(openTagWithSpace);
            if (tagStart == -1) {
                return null;
            }
            // 找到 > 结束
            int gtPos = content.indexOf(">", tagStart);
            if (gtPos == -1) return null;
            tagStart = gtPos;
        } else {
            tagStart += openTag.length();
        }

        // 查找CDATA开始标记
        String cdataStart = "<![CDATA[";
        int cdataStartPos = content.indexOf(cdataStart, tagStart);

        if (cdataStartPos == -1) {
            // 没有CDATA，提取普通内容
            String closeTag = "</" + tagName + ">";
            int closePos = content.indexOf(closeTag, tagStart);
            if (closePos == -1) return null;
            return content.substring(tagStart, closePos).trim();
        }

        // 找到CDATA内容开始位置
        int contentStart = cdataStartPos + cdataStart.length();

        // 查找CDATA结束标记 ]]>
        String cdataEnd = "]]>";
        int cdataEndPos = content.indexOf(cdataEnd, contentStart);
        if (cdataEndPos == -1) {
            log.warn("未找到CDATA结束标记，tagName: {}", tagName);
            return null;
        }

        return content.substring(contentStart, cdataEndPos).trim();
    }

    /**
     * 从XML子节点解析keyEvents
     * 格式：<E><opening><item>事件</item></opening>...</E>
     * 转换为JSON：{"opening":["事件1","事件2"],...}
     */
    private String parseKeyEventsFromXml(String volumeContent) {
        try {
            // 提取<E>标签内容
            int eStart = volumeContent.indexOf("<E>");
            int eEnd = volumeContent.indexOf("</E>");
            if (eStart == -1 || eEnd == -1) {
                log.warn("未找到<E>标签");
                return null;
            }
            String eContent = volumeContent.substring(eStart + 3, eEnd);

            // 使用Jackson构建JSON
            com.fasterxml.jackson.databind.node.ObjectNode resultNode = objectMapper.createObjectNode();

            // 定义5个阶段
            String[] stages = {"opening", "development", "turning", "climax", "ending"};

            for (String stage : stages) {
                String stageStartTag = "<" + stage + ">";
                String stageEndTag = "</" + stage + ">";

                int stageStart = eContent.indexOf(stageStartTag);
                int stageEnd = eContent.indexOf(stageEndTag);

                if (stageStart == -1 || stageEnd == -1) {
                    log.warn("keyEvents缺少阶段: {}", stage);
                    continue;
                }

                String stageContent = eContent.substring(stageStart + stageStartTag.length(), stageEnd);

                // 提取所有<item>内容
                List<String> items = new ArrayList<>();
                int itemStart = 0;
                while ((itemStart = stageContent.indexOf("<item>", itemStart)) != -1) {
                    int itemEnd = stageContent.indexOf("</item>", itemStart);
                    if (itemEnd == -1) break;

                    String itemContent = stageContent.substring(itemStart + 6, itemEnd);

                    // 处理CDATA
                    String cdataStart = "<![CDATA[";
                    String cdataEnd = "]]>";
                    int cStart = itemContent.indexOf(cdataStart);
                    if (cStart != -1) {
                        int cEnd = itemContent.indexOf(cdataEnd, cStart + cdataStart.length());
                        if (cEnd != -1) {
                            itemContent = itemContent.substring(cStart + cdataStart.length(), cEnd);
                        }
                    }

                    items.add(itemContent.trim());
                    itemStart = itemEnd + 7;
                }

                // 添加到JSON数组
                com.fasterxml.jackson.databind.node.ArrayNode arrayNode = resultNode.putArray(stage);
                for (String item : items) {
                    arrayNode.add(item);
                }
            }

            // 返回JSON字符串
            return objectMapper.writeValueAsString(resultNode);

        } catch (Exception e) {
            log.error("解析keyEvents XML失败: {}", e.getMessage());
            return null;
        }
    }
}
