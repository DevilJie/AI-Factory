package com.aifactory.service;

import com.aifactory.dto.*;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelOutline;
import com.aifactory.entity.Project;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelOutlineMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.response.Result;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大纲管理Service
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Slf4j
@Service
public class OutlineService {

    @Autowired
    private NovelOutlineMapper outlineMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ChapterMapper chapterMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AI生成完整大纲（同步方法，已弃用）
     */
    public OutlineDto generateOutline(AIGenerateOutlineRequest request) {
        Long userId = com.aifactory.common.UserContext.getUserId();
        log.info("用户 {} 开始AI生成项目 {} 的大纲", userId, request.getProjectId());

        try {
            // 1. 构建AI提示词
            String prompt = buildOutlinePrompt(request);

            // 2. 构建AI请求
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setRequestType("llm_outline_generate_detailed");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(8000);

            // 3. 调用AI生成
            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider()
                .generate(aiRequest);

            String responseContent = aiResponse.getContent();
            log.info("AI生成大纲响应长度: {}", responseContent.length());

            // 4. 解析AI响应
            AIGenerateOutlineResponse outlineResponse = parseAIResponse(responseContent);

            // 5. 保存到数据库
            OutlineDto result = saveOutline(request.getProjectId(), outlineResponse, request);

            log.info("用户 {} AI生成大纲成功，大纲ID: {}", userId, result.getId());
            return result;

        } catch (Exception e) {
            log.error("AI生成大纲失败", e);
            throw new RuntimeException("AI生成大纲失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建AI生成大纲的提示词（使用模板系统）
     */
    private String buildOutlinePrompt(AIGenerateOutlineRequest request) {
        // 准备模板变量
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("projectDescription", request.getProjectDescription());
        variables.put("storyTone", request.getStoryTone());
        variables.put("storyGenre", request.getStoryGenre());
        variables.put("targetVolumeCount", request.getTargetVolumeCount());
        variables.put("avgWordsPerVolume", request.getAvgWordsPerVolume());
        variables.put("targetWordCount", request.getTargetWordCount());

        String additionalRequirements = request.getAdditionalRequirements();
        if (additionalRequirements != null && !additionalRequirements.isEmpty()) {
            variables.put("additionalRequirements", "【额外要求】\n" + additionalRequirements + "\n\n");
        } else {
            variables.put("additionalRequirements", "");
        }

        // 执行模板
        String templateCode = "llm_outline_generate_detailed";
        String prompt = promptTemplateService.executeTemplate(templateCode, variables);

        return prompt;
    }

    /**
     * 此方法已废弃，XML模板已移至数据库提示词模板系统中
     * @deprecated 请使用数据库提示词模板系统
     */
    @Deprecated
    private String getXmlTemplate() {
        log.warn("getXmlTemplate方法已废弃，请使用数据库提示词模板系统");
        return "";
    }

    /**
     * 解析AI响应
     * 优先尝试XML解析，失败后fallback到JSON解析
     */
    private AIGenerateOutlineResponse parseAIResponse(String aiResponse) throws Exception {
        log.info("开始解析AI响应，响应长度: {}", aiResponse.length());

        // 1. 先尝试XML解析
        try {
            AIGenerateOutlineResponse xmlResult = parseXMLResponse(aiResponse);
            if (xmlResult != null) {
                log.info("XML解析成功");
                return xmlResult;
            }
        } catch (Exception e) {
            log.warn("XML解析失败，将尝试JSON解析: {}", e.getMessage());
        }

        // 2. XML解析失败，fallback到JSON解析
        log.info("尝试JSON解析");
        return parseJSONResponse(aiResponse);
    }

    /**
     * 解析XML格式的AI响应（支持单字母标签）
     */
    private AIGenerateOutlineResponse parseXMLResponse(String aiResponse) {
        try {
            // 提取XML内容
            String xmlContent = extractXMLContent(aiResponse);

            log.info("提取的XML内容长度: {}", xmlContent.length());

            // 手动解析XML（避免引入额外依赖）
            AIGenerateOutlineResponse response = new AIGenerateOutlineResponse();

            // 提取整体概念 C (Concept)
            response.setOverallConcept(extractXMLTag(xmlContent, "C"));

            // 提取主要主题 M (Main Theme)
            response.setMainTheme(extractXMLTag(xmlContent, "M"));

            // 提取分卷 V (Volumes)
            List<AIGenerateOutlineResponse.VolumePlan> volumes = new ArrayList<>();
            String volumesContent = extractXMLBlock(xmlContent, "V");
            if (volumesContent != null) {
                String[] volumeBlocks = extractRepeatedBlocks(volumesContent, "v");
                for (String volumeBlock : volumeBlocks) {
                    AIGenerateOutlineResponse.VolumePlan volume = new AIGenerateOutlineResponse.VolumePlan();
                    volume.setVolumeNumber(Integer.parseInt(extractXMLTag(volumeBlock, "N")));
                    volume.setVolumeTitle(extractXMLTag(volumeBlock, "T"));
                    volume.setVolumeTheme(extractXMLTag(volumeBlock, "Z"));
                    volume.setMainConflict(extractXMLTag(volumeBlock, "F"));
                    volume.setPlotArc(extractXMLTag(volumeBlock, "P"));
                    volume.setVolumeDescription(extractXMLTag(volumeBlock, "D"));
                    volume.setKeyEvents(extractXMLTag(volumeBlock, "E"));

                    volume.setTimelineSetting(extractXMLTag(volumeBlock, "L"));
                    volume.setTargetChapterCount(Integer.parseInt(extractXMLTag(volumeBlock, "G")));
                    volume.setVolumeNotes(extractXMLTag(volumeBlock, "B"));
                    volume.setCoreGoal(extractXMLTag(volumeBlock, "O"));
                    volume.setClimax(extractXMLTag(volumeBlock, "H"));
                    volume.setEnding(extractXMLTag(volumeBlock, "R"));

                    // 解析 JSON 数组字段 A (Actors/Characters)
                    String newCharactersStr = extractXMLTag(volumeBlock, "A");
                    if (newCharactersStr != null && !newCharactersStr.isEmpty()) {
                        try {
                            String[] characters = objectMapper.readValue(newCharactersStr, String[].class);
                            volume.setNewCharacters(java.util.Arrays.asList(characters));
                        } catch (Exception e) {
                            volume.setNewCharacters(new ArrayList<>());
                        }
                    } else {
                        volume.setNewCharacters(new ArrayList<>());
                    }

                    // 解析 JSON 数组字段 Y (Yield/Foreshadowing)
                    String foreshadowingsStr = extractXMLTag(volumeBlock, "Y");
                    if (foreshadowingsStr != null && !foreshadowingsStr.isEmpty()) {
                        try {
                            String[] foreshadowings = objectMapper.readValue(foreshadowingsStr, String[].class);
                            volume.setStageForeshadowings(java.util.Arrays.asList(foreshadowings));
                        } catch (Exception e) {
                            volume.setStageForeshadowings(new ArrayList<>());
                        }
                    } else {
                        volume.setStageForeshadowings(new ArrayList<>());
                    }

                    volumes.add(volume);
                }
            }
            response.setVolumes(volumes);

            // 提取章节 H (Chapters)
            List<AIGenerateOutlineResponse.ChapterPlan> chapters = new ArrayList<>();
            String chaptersContent = extractXMLBlock(xmlContent, "H");
            if (chaptersContent != null) {
                String[] chapterBlocks = extractRepeatedBlocks(chaptersContent, "c");
                for (String chapterBlock : chapterBlocks) {
                    AIGenerateOutlineResponse.ChapterPlan chapter = new AIGenerateOutlineResponse.ChapterPlan();
                    chapter.setChapterNumber(Integer.parseInt(extractXMLTag(chapterBlock, "N")));
                    chapter.setVolumeNumber(Integer.parseInt(extractXMLTag(chapterBlock, "V")));
                    chapter.setChapterTitle(extractXMLTag(chapterBlock, "T"));
                    chapter.setPlotOutline(extractXMLTag(chapterBlock, "P"));
                    chapter.setKeyEvents(extractXMLTag(chapterBlock, "E"));
                    chapter.setChapterGoal(extractXMLTag(chapterBlock, "G"));
                    chapter.setWordCountTarget(Integer.parseInt(extractXMLTag(chapterBlock, "W")));
                    chapter.setChapterStartingScene(extractXMLTag(chapterBlock, "S"));
                    chapter.setChapterEndingScene(extractXMLTag(chapterBlock, "X"));
                    chapters.add(chapter);
                }
            }
            response.setChapters(chapters);

            return response;
        } catch (Exception e) {
            log.error("XML解析异常", e);
            throw new RuntimeException("XML解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析JSON格式的AI响应（fallback方案）
     */
    private AIGenerateOutlineResponse parseJSONResponse(String aiResponse) throws JsonProcessingException {
        // 尝试提取JSON部分
        String jsonStr = aiResponse;

        // 如果响应包含```json标记，提取其中的JSON
        if (aiResponse.contains("```json")) {
            int start = aiResponse.indexOf("```json") + 7;
            int end = aiResponse.indexOf("```", start);
            if (end > start) {
                jsonStr = aiResponse.substring(start, end).trim();
            }
        } else if (aiResponse.contains("```")) {
            int start = aiResponse.indexOf("```") + 3;
            int end = aiResponse.indexOf("```", start);
            if (end > start) {
                jsonStr = aiResponse.substring(start, end).trim();
            }
        }

        // 查找第一个{和最后一个}
        int firstBrace = jsonStr.indexOf("{");
        int lastBrace = jsonStr.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
        }

        return objectMapper.readValue(jsonStr, AIGenerateOutlineResponse.class);
    }

    /**
     * 提取XML内容（处理```xml标记）
     */
    private String extractXMLContent(String response) {
        String content = response;

        // 如果响应包含```xml标记，提取其中的XML
        if (response.contains("```xml")) {
            int start = response.indexOf("```xml") + 6;
            int end = response.indexOf("```", start);
            if (end > start) {
                content = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                content = response.substring(start, end).trim();
            }
        }

        // 查找第一个<O>（单字母标签Outline）和最后一个</O>
        int startTag = content.indexOf("<O>");
        int endTag = content.lastIndexOf("</O>");
        if (startTag >= 0 && endTag > startTag) {
            content = content.substring(startTag, endTag + "</O>".length());
        }

        return content;
    }

    /**
     * 提取XML标签内容
     */
    private String extractXMLTag(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";

        int start = xml.indexOf(openTag);
        if (start == -1) {
            return "";
        }

        int end = xml.indexOf(closeTag, start);
        if (end == -1) {
            return "";
        }

        return xml.substring(start + openTag.length(), end).trim();
    }

    /**
     * 提取XML块内容（不包含外层标签）
     */
    private String extractXMLBlock(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";

        int start = xml.indexOf(openTag);
        if (start == -1) {
            return null;
        }

        int end = xml.indexOf(closeTag, start);
        if (end == -1) {
            return null;
        }

        return xml.substring(start + openTag.length(), end);
    }

    /**
     * 提取重复的XML块（如多个<volume>或<chapter>）
     */
    private String[] extractRepeatedBlocks(String content, String tagName) {
        List<String> blocks = new ArrayList<>();
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";

        int searchFrom = 0;
        while (true) {
            int start = content.indexOf(openTag, searchFrom);
            if (start == -1) {
                break;
            }

            int end = content.indexOf(closeTag, start);
            if (end == -1) {
                break;
            }

            blocks.add(content.substring(start, end + closeTag.length()));
            searchFrom = end + closeTag.length();
        }

        return blocks.toArray(new String[0]);
    }

    /**
     * 保存大纲到数据库（无事务，确保数据立即提交）
     */
    protected OutlineDto saveOutline(Long projectId, AIGenerateOutlineResponse response,
                                    AIGenerateOutlineRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存大纲总览
        NovelOutline outline = new NovelOutline();
        outline.setProjectId(projectId);
        outline.setOverallConcept(response.getOverallConcept());
        outline.setMainTheme(response.getMainTheme());
        outline.setTargetVolumeCount(request.getTargetVolumeCount());
        outline.setTargetChapterCount(0); // 章节规划已独立，此处设为0
        outline.setTargetWordCount(request.getTargetWordCount());
        outline.setGenre(request.getStoryGenre());
        outline.setTone(request.getStoryTone());
        outline.setStatus("draft");
        outline.setCreateTime(now);
        outline.setUpdateTime(now);

        outlineMapper.insert(outline);

        // 2. 保存分卷规划
        List<VolumePlanDto> volumeDtos = new ArrayList<>();
        for (AIGenerateOutlineResponse.VolumePlan vol : response.getVolumes()) {
            NovelVolumePlan volumePlan = new NovelVolumePlan();
            volumePlan.setProjectId(projectId);
            volumePlan.setOutlineId(outline.getId());
            volumePlan.setVolumeNumber(vol.getVolumeNumber());
            volumePlan.setVolumeTitle(vol.getVolumeTitle());
            volumePlan.setVolumeTheme(vol.getVolumeTheme());
            volumePlan.setMainConflict(vol.getMainConflict());
            volumePlan.setPlotArc(vol.getPlotArc());
            volumePlan.setVolumeDescription(vol.getVolumeDescription());
            volumePlan.setKeyEvents(vol.getKeyEvents());
            volumePlan.setTimelineSetting(vol.getTimelineSetting());
            volumePlan.setTargetChapterCount(vol.getTargetChapterCount());
            volumePlan.setVolumeNotes(vol.getVolumeNotes());
            volumePlan.setCoreGoal(vol.getCoreGoal());
            volumePlan.setClimax(vol.getClimax());
            volumePlan.setEnding(vol.getEnding());

            // 保存 JSON 数组字段
            if (vol.getNewCharacters() != null && !vol.getNewCharacters().isEmpty()) {
                try {
                    volumePlan.setNewCharacters(objectMapper.writeValueAsString(vol.getNewCharacters()));
                } catch (Exception e) {
                    log.warn("序列化 newCharacters 失败", e);
                }
            }

            if (vol.getStageForeshadowings() != null && !vol.getStageForeshadowings().isEmpty()) {
                try {
                    volumePlan.setStageForeshadowings(objectMapper.writeValueAsString(vol.getStageForeshadowings()));
                } catch (Exception e) {
                    log.warn("序列化 stageForeshadowings 失败", e);
                }
            }

            volumePlan.setStatus("planned");
            volumePlan.setSortOrder(vol.getVolumeNumber());
            volumePlan.setCreateTime(now);
            volumePlan.setUpdateTime(now);

            volumePlanMapper.insert(volumePlan);

            VolumePlanDto dto = new VolumePlanDto();
            dto.setId(volumePlan.getId());
            dto.setProjectId(volumePlan.getProjectId());
            dto.setOutlineId(volumePlan.getOutlineId());
            dto.setVolumeNumber(volumePlan.getVolumeNumber());
            dto.setVolumeTitle(volumePlan.getVolumeTitle());
            dto.setVolumeTheme(volumePlan.getVolumeTheme());
            dto.setMainConflict(volumePlan.getMainConflict());
            dto.setPlotArc(volumePlan.getPlotArc());
            dto.setVolumeDescription(volumePlan.getVolumeDescription());
            dto.setKeyEvents(volumePlan.getKeyEvents());
            dto.setTimelineSetting(volumePlan.getTimelineSetting());
            dto.setTargetChapterCount(volumePlan.getTargetChapterCount());
            dto.setVolumeNotes(volumePlan.getVolumeNotes());
            dto.setCoreGoal(volumePlan.getCoreGoal());
            dto.setClimax(volumePlan.getClimax());
            dto.setEnding(volumePlan.getEnding());
            dto.setStatus(volumePlan.getStatus());
            dto.setSortOrder(volumePlan.getSortOrder());
            dto.setVolumeCompleted(volumePlan.getVolumeCompleted());
            dto.setCreateTime(volumePlan.getCreateTime());
            dto.setUpdateTime(volumePlan.getUpdateTime());
            volumeDtos.add(dto);
        }

        // 3. 保存章节规划
        List<ChapterPlanDto> chapterDtos = new ArrayList<>();
        for (AIGenerateOutlineResponse.ChapterPlan chapter : response.getChapters()) {
            // 找到对应的分卷
            Long volumePlanId = null;
            for (VolumePlanDto vol : volumeDtos) {
                if (vol.getVolumeNumber().equals(chapter.getVolumeNumber())) {
                    volumePlanId = vol.getId();
                    break;
                }
            }

            NovelChapterPlan chapterPlan = new NovelChapterPlan();
            chapterPlan.setProjectId(projectId);
            chapterPlan.setVolumePlanId(volumePlanId);
            chapterPlan.setChapterNumber(chapter.getChapterNumber());
            chapterPlan.setChapterTitle(chapter.getChapterTitle());
            chapterPlan.setPlotOutline(chapter.getPlotOutline());
            chapterPlan.setKeyEvents(chapter.getKeyEvents());
            chapterPlan.setChapterGoal(chapter.getChapterGoal());
            chapterPlan.setWordCountTarget(chapter.getWordCountTarget());
            chapterPlan.setChapterStartingScene(chapter.getChapterStartingScene() != null ? chapter.getChapterStartingScene() : "");
            chapterPlan.setChapterEndingScene(chapter.getChapterEndingScene() != null ? chapter.getChapterEndingScene() : "");
            chapterPlan.setStatus("planned");
            chapterPlan.setCreateTime(now);
            chapterPlan.setUpdateTime(now);

            chapterPlanMapper.insert(chapterPlan);

            ChapterPlanDto dto = new ChapterPlanDto();
            dto.setId(chapterPlan.getId());
            dto.setChapterNumber(chapterPlan.getChapterNumber());
            dto.setVolumePlanId(volumePlanId);
            dto.setChapterTitle(chapterPlan.getChapterTitle());
            dto.setPlotOutline(chapterPlan.getPlotOutline());
            dto.setKeyEvents(chapterPlan.getKeyEvents());
            dto.setChapterGoal(chapterPlan.getChapterGoal());
            dto.setWordCountTarget(chapterPlan.getWordCountTarget());
            dto.setChapterStartingScene(chapterPlan.getChapterStartingScene());
            dto.setChapterEndingScene(chapterPlan.getChapterEndingScene());
            chapterDtos.add(dto);
        }

        // 4. 构建返回DTO
        OutlineDto result = new OutlineDto();
        result.setId(outline.getId());
        result.setProjectId(projectId);
        result.setOverallConcept(outline.getOverallConcept());
        result.setMainTheme(outline.getMainTheme());
        result.setTargetVolumeCount(outline.getTargetVolumeCount());
        result.setTargetChapterCount(outline.getTargetChapterCount());
        result.setTargetWordCount(outline.getTargetWordCount());
        result.setGenre(outline.getGenre());
        result.setTone(outline.getTone());
        result.setStatus(outline.getStatus());
        result.setCreateTime(outline.getCreateTime());
        result.setUpdateTime(outline.getUpdateTime());
        result.setVolumes(volumeDtos);
        result.setChapters(chapterDtos);

        return result;
    }

    /**
     * 获取项目大纲
     */
    public OutlineDto getOutline(Long projectId) {
        log.info("获取项目 {} 的大纲", projectId);

        // 查询大纲
        NovelOutline outline = outlineMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelOutline>()
                .eq(NovelOutline::getProjectId, projectId)
                .orderByDesc(NovelOutline::getCreateTime)
                .last("LIMIT 1")
        );

        if (outline == null) {
            log.info("项目 {} 暂无大纲数据", projectId);
            return null;
        }

        log.info("找到大纲记录，ID: {}, 分卷数: {}, 章节数: {}",
            outline.getId(), outline.getTargetVolumeCount(), outline.getTargetChapterCount());

        // 查询分卷
        List<NovelVolumePlan> volumePlans = volumePlanMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelVolumePlan>()
                .eq(NovelVolumePlan::getProjectId, projectId)
                .eq(NovelVolumePlan::getOutlineId, outline.getId())
                .orderByAsc(NovelVolumePlan::getVolumeNumber)
        );

        log.info("查询到 {} 个分卷", volumePlans.size());

        // 查询章节规划
        List<NovelChapterPlan> chapterPlans = chapterPlanMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getProjectId, projectId)
                .orderByAsc(NovelChapterPlan::getChapterNumber)
        );

        log.info("查询到 {} 个章节", chapterPlans.size());

        // 构建DTO
        OutlineDto dto = new OutlineDto();
        dto.setId(outline.getId());
        dto.setProjectId(outline.getProjectId());
        dto.setOverallConcept(outline.getOverallConcept());
        dto.setMainTheme(outline.getMainTheme());
        dto.setTargetVolumeCount(outline.getTargetVolumeCount());
        dto.setTargetChapterCount(outline.getTargetChapterCount());
        dto.setTargetWordCount(outline.getTargetWordCount());
        dto.setGenre(outline.getGenre());
        dto.setTone(outline.getTone());
        dto.setStatus(outline.getStatus());
        dto.setCreateTime(outline.getCreateTime());
        dto.setUpdateTime(outline.getUpdateTime());

        // 转换分卷，并关联章节
        List<VolumePlanDto> volumeDtos = new ArrayList<>();
        List<ChapterPlanDto> allChapterDtos = new ArrayList<>();

        for (NovelVolumePlan vp : volumePlans) {
            VolumePlanDto vdto = new VolumePlanDto();
            vdto.setId(vp.getId());
            vdto.setProjectId(vp.getProjectId());
            vdto.setOutlineId(vp.getOutlineId());
            vdto.setVolumeNumber(vp.getVolumeNumber());
            vdto.setVolumeTitle(vp.getVolumeTitle());
            vdto.setVolumeTheme(vp.getVolumeTheme());
            vdto.setMainConflict(vp.getMainConflict());
            vdto.setPlotArc(vp.getPlotArc());
            vdto.setVolumeDescription(vp.getVolumeDescription());
            vdto.setKeyEvents(vp.getKeyEvents());
            vdto.setTimelineSetting(vp.getTimelineSetting());
            vdto.setTargetChapterCount(vp.getTargetChapterCount());
            vdto.setVolumeNotes(vp.getVolumeNotes());
            vdto.setCoreGoal(vp.getCoreGoal());
            vdto.setClimax(vp.getClimax());
            vdto.setEnding(vp.getEnding());
            vdto.setStatus(vp.getStatus());
            vdto.setSortOrder(vp.getSortOrder());
            vdto.setVolumeCompleted(vp.getVolumeCompleted());
            vdto.setCreateTime(vp.getCreateTime());
            vdto.setUpdateTime(vp.getUpdateTime());

            // 解析 JSON 数组字段
            if (vp.getNewCharacters() != null && !vp.getNewCharacters().isEmpty()) {
                try {
                    String[] characters = objectMapper.readValue(vp.getNewCharacters(), String[].class);
                    vdto.setNewCharacters(java.util.Arrays.asList(characters));
                } catch (Exception e) {
                    log.warn("解析 newCharacters 失败, volumeId={}", vp.getId(), e);
                    vdto.setNewCharacters(new ArrayList<>());
                }
            } else {
                vdto.setNewCharacters(new ArrayList<>());
            }

            if (vp.getStageForeshadowings() != null && !vp.getStageForeshadowings().isEmpty()) {
                try {
                    String[] foreshadowings = objectMapper.readValue(vp.getStageForeshadowings(), String[].class);
                    vdto.setStageForeshadowings(java.util.Arrays.asList(foreshadowings));
                } catch (Exception e) {
                    log.warn("解析 stageForeshadowings 失败, volumeId={}", vp.getId(), e);
                    vdto.setStageForeshadowings(new ArrayList<>());
                }
            } else {
                vdto.setStageForeshadowings(new ArrayList<>());
            }

            // 查找属于该分卷的章节
            List<ChapterPlanDto> volumeChapterDtos = new ArrayList<>();
            for (NovelChapterPlan cp : chapterPlans) {
                if (vp.getId().equals(cp.getVolumePlanId())) {
                    ChapterPlanDto cdto = new ChapterPlanDto();
                    cdto.setId(cp.getId());
                    cdto.setProjectId(cp.getProjectId());
                    cdto.setVolumePlanId(cp.getVolumePlanId());
                    cdto.setChapterNumber(cp.getChapterNumber());
                    cdto.setChapterTitle(cp.getChapterTitle());
                    cdto.setPlotOutline(cp.getPlotOutline());
                    cdto.setKeyEvents(cp.getKeyEvents());
                    cdto.setChapterGoal(cp.getChapterGoal());
                    cdto.setWordCountTarget(cp.getWordCountTarget());
                    cdto.setChapterStartingScene(cp.getChapterStartingScene());
                    cdto.setChapterEndingScene(cp.getChapterEndingScene());
                    cdto.setStatus(cp.getStatus());
                    cdto.setCreateTime(cp.getCreateTime());
                    cdto.setUpdateTime(cp.getUpdateTime());

                    // 查询是否已生成章节内容
                    try {
                        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Chapter> chapterQuery =
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Chapter>();
                        chapterQuery.eq(Chapter::getChapterPlanId, cp.getId());
                        Chapter chapter = chapterMapper.selectOne(chapterQuery);

                        if (chapter != null) {
                            cdto.setHasContent(true);
                            cdto.setChapterId(chapter.getId());
                            cdto.setWordCount(chapter.getWordCount());
                        } else {
                            cdto.setHasContent(false);
                        }
                    } catch (Exception e) {
                        log.warn("查询章节内容失败，章节规划ID: {}", cp.getId(), e);
                        cdto.setHasContent(false);
                    }

                    volumeChapterDtos.add(cdto);
                    allChapterDtos.add(cdto);
                }
            }

            vdto.setChapters(volumeChapterDtos);
            volumeDtos.add(vdto);
        }

        dto.setVolumes(volumeDtos);
        dto.setChapters(allChapterDtos);

        log.info("构建大纲DTO完成，分卷数: {}, 章节数: {}",
            volumeDtos.size(), allChapterDtos.size());

        return dto;
    }

    /**
     * AI生成章节计划（异步，重构版：支持情节阶段和分批生成）
     */
    public java.util.Map<String, Object> generateChaptersAsync(GenerateChaptersRequest request) {
        Long projectId = request.getProjectId();
        Long volumeId = request.getVolumeId();
        String plotStage = request.getPlotStage();

        log.info("开始为项目 {} 的分卷 {} 生成{}阶段章节计划", projectId, volumeId, plotStage);

        try {
            // 1. 查询分卷信息
            NovelVolumePlan volumePlan = volumePlanMapper.selectById(volumeId);
            if (volumePlan == null) {
                throw new RuntimeException("分卷不存在: " + volumeId);
            }

            // 2. 查询项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new RuntimeException("项目不存在: " + projectId);
            }

            // 3. 查询大纲信息
            NovelOutline outline = outlineMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelOutline>()
                    .eq(NovelOutline::getProjectId, projectId)
                    .orderByDesc(NovelOutline::getCreateTime)
                    .last("LIMIT 1")
            );

            if (outline == null) {
                throw new RuntimeException("项目暂无大纲，请先生成大纲");
            }

            // 4. 使用分卷的目标章节数
            int targetChapterCount = volumePlan.getTargetChapterCount() != null ? volumePlan.getTargetChapterCount() : 10;

            log.info("目标章节数: {}", targetChapterCount);

            // 5. 构建任务配置
            java.util.Map<String, Object> config = new java.util.HashMap<>();
            config.put("volumeId", volumeId);
            config.put("plotStage", plotStage);
            config.put("volumeNumber", volumePlan.getVolumeNumber());
            config.put("volumeTitle", volumePlan.getVolumeTitle());
            config.put("volumeTheme", volumePlan.getVolumeTheme());
            config.put("mainConflict", volumePlan.getMainConflict());
            config.put("plotArc", volumePlan.getPlotArc());
            config.put("targetChapterCount", targetChapterCount);
            config.put("projectDescription", project.getDescription());
            config.put("storyTone", outline.getTone());
            config.put("storyGenre", outline.getGenre());

            // 6. 创建任务请求
            CreateTaskRequest taskRequest = new CreateTaskRequest();
            taskRequest.setProjectId(projectId);
            taskRequest.setTaskType("generate_chapters");
            taskRequest.setTaskName("AI生成分卷章节计划");
            taskRequest.setConfig(config);

            // 7. 调用任务服务创建任务
            Result<TaskDto> result = taskService.createTask(taskRequest);

            if (result.getOk() == null || !result.getOk()) {
                throw new RuntimeException("创建章节生成任务失败: " + result.getMsg());
            }

            // 8. 返回任务ID
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("taskId", result.getData().getId());
            response.put("message", "章节生成任务已创建，正在后台执行");
            response.put("targetChapterCount", targetChapterCount);
            response.put("plotStage", plotStage);

            log.info("章节生成任务创建成功，taskId: {}", result.getData().getId());

            return response;

        } catch (Exception e) {
            log.error("生成章节计划失败", e);
            throw new RuntimeException("生成章节计划失败: " + e.getMessage(), e);
        }
    }

    @Autowired
    private com.aifactory.service.task.validator.ChapterPlotStageValidator stageValidator;

    @Autowired
    private com.aifactory.service.task.validator.VolumeCompletionValidator volumeCompletionValidator;

    /**
     * 标记阶段为完成
     */
    public void markStageCompleted(MarkStageCompletedRequest request) {
        Long volumeId = request.getVolumeId();
        String plotStage = request.getPlotStage();

        log.info("标记阶段为完成：volumeId={}, plotStage={}", volumeId, plotStage);

        // 验证：检查阶段是否有章节
        com.aifactory.service.task.validator.ChapterPlotStageValidator.ValidationResult validation =
            stageValidator.canMarkStageCompleted(volumeId, plotStage);
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getError());
        }

        // 标记该分卷该阶段的所有章节为已完成
        chapterPlanMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                .eq(NovelChapterPlan::getPlotStage, plotStage)
                .set(NovelChapterPlan::getStageCompleted, true)
        );

        // 检查该分卷是否所有阶段都完成，如果是则标记分卷为完成
        if (volumeCompletionValidator.isVolumeCompleted(volumeId)) {
            NovelVolumePlan volumeUpdate = new NovelVolumePlan();
            volumeUpdate.setId(volumeId);
            volumeUpdate.setVolumeCompleted(true);
            volumePlanMapper.updateById(volumeUpdate);
            log.info("分卷 {} 所有阶段已完成，标记分卷为完成", volumeId);
        }

        log.info("阶段 {} 已标记为完成", plotStage);
    }

    /**
     * 取消阶段完成状态（允许继续编辑）
     */
    public void unmarkStageCompleted(MarkStageCompletedRequest request) {
        Long volumeId = request.getVolumeId();
        String plotStage = request.getPlotStage();

        log.info("取消阶段完成状态：volumeId={}, plotStage={}", volumeId, plotStage);

        // 验证：检查是否可以取消完成（下一阶段或下一分卷没有章节）
        int currentOrdinal = stageValidator.getStageOrdinal(plotStage);

        // 检查下一阶段是否有章节
        if (currentOrdinal < 4) {
            String nextStage = stageValidator.getStageByOrdinal(currentOrdinal + 1);
            Long nextCount = chapterPlanMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                    .eq(NovelChapterPlan::getPlotStage, nextStage)
            );

            if (nextCount > 0) {
                throw new RuntimeException("下一阶段已有章节，不能取消当前阶段的完成状态");
            }
        }

        // 取消完成标记
        chapterPlanMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<NovelChapterPlan>()
                .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                .eq(NovelChapterPlan::getPlotStage, plotStage)
                .set(NovelChapterPlan::getStageCompleted, false)
        );

        // 取消分卷完成标记
        NovelVolumePlan volumeUpdate = new NovelVolumePlan();
        volumeUpdate.setId(volumeId);
        volumeUpdate.setVolumeCompleted(false);
        volumePlanMapper.updateById(volumeUpdate);

        log.info("阶段 {} 已取消完成状态", plotStage);
    }
}
