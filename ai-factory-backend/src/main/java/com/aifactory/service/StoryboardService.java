package com.aifactory.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.GenerateStoryboardRequest;
import com.aifactory.dto.StoryboardVo;
import com.aifactory.entity.Chapter;
import com.aifactory.enums.AIRole;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelStoryboard;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.mapper.NovelStoryboardMapper;
import com.aifactory.service.llm.LLMProviderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分镜服务
 *
 * @Author CaiZy
 * @Date 2025-01-30
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@Service
public class StoryboardService {

    @Autowired
    private NovelStoryboardMapper storyboardMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private NovelCharacterMapper characterMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为章节生成分镜
     */
    @Transactional
    public List<StoryboardVo> generateStoryboard(GenerateStoryboardRequest request) {
        Long chapterId = request.getChapterId();
        Long projectId = request.getProjectId();

        // 获取章节内容
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }

        // 如果不是强制重新生成，检查是否已有分镜数据
        if (!request.getForceRegenerate()) {
            var existingWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelStoryboard>()
                .eq(NovelStoryboard::getChapterId, chapterId);
            Long count = storyboardMapper.selectCount(existingWrapper);
            if (count > 0) {
                log.info("章节{}已有分镜数据，跳过生成", chapterId);
                return getStoryboardList(chapterId);
            }
        } else {
            // 删除已有分镜数据
            var deleteWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelStoryboard>()
                .eq(NovelStoryboard::getChapterId, chapterId);
            storyboardMapper.delete(deleteWrapper);
        }

        // 获取项目角色列表
        var characterQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelCharacter>()
            .eq(NovelCharacter::getProjectId, projectId);
        List<NovelCharacter> characters = characterMapper.selectList(characterQuery);

        // 构建AI提示词
        String prompt = buildStoryboardPrompt(chapter, characters);

        // 调用AI生成分镜
        try {
            var aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_storyboard_create");
            aiRequest.setRole(AIRole.VIDEO_SCRIPTWRITER);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(8000);

            var aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成分镜原始响应:\n{}", responseContent);

            // 解析并保存分镜数据
            List<StoryboardVo> storyboards = parseAndSaveStoryboards(
                chapterId, projectId, responseContent, characters);

            log.info("章节{}分镜生成完成，共{}个镜头", chapterId, storyboards.size());
            return storyboards;

        } catch (Exception e) {
            log.error("生成分镜失败", e);
            throw new RuntimeException("生成分镜失败: " + e.getMessage());
        }
    }

    /**
     * 构建分镜生成提示词
     */
    private String buildStoryboardPrompt(Chapter chapter, List<NovelCharacter> characters) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("【任务目标】\n");
        prompt.append("你是一位专业的动画导演，需要将小说章节转化为详细的分镜脚本。\n\n");

        prompt.append("【章节信息】\n");
        prompt.append("章节标题：").append(chapter.getTitle()).append("\n");
        prompt.append("章节内容：\n").append(chapter.getContent()).append("\n\n");

        // 添加角色信息
        if (!characters.isEmpty()) {
            prompt.append("【角色列表】\n");
            for (NovelCharacter character : characters) {
                prompt.append("- ").append(character.getName())
                    .append("（").append(character.getRole()).append("）\n");
                if (character.getAppearancePrompt() != null && !character.getAppearancePrompt().isEmpty()) {
                    prompt.append("  图像描述：").append(character.getAppearancePrompt()).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("【分镜要求】\n");
        prompt.append("1. 每个分镜包含以下信息：\n");
        prompt.append("   - shotNumber: 镜头编号（从1开始）\n");
        prompt.append("   - shotType: 景别（extreme_long/full/medium/close_up/extreme_close_up）\n");
        prompt.append("   - cameraAngle: 拍摄角度（birds_eye/high_angle/eye_level/low_angle/dutch_angle）\n");
        prompt.append("   - cameraMovement: 运镜方式（static/dolly_in/dolly_out/pan/tracking/handheld）\n");
        prompt.append("   - description: 镜头描述（中文，详细说明画面内容）\n");
        prompt.append("   - visualPrompt: 画面生成提示词（英文，3D动漫风格，用于AI图像生成）\n");
        prompt.append("   - duration: 镜头时长（秒）\n");
        prompt.append("   - characterNames: 出场角色名称列表（逗号分隔）\n");
        prompt.append("   - dialogue: 台词（如果有）\n");
        prompt.append("   - action: 动作描述\n\n");

        prompt.append("2. visualPrompt 格式要求：\n");
        prompt.append("   - 必须使用英文关键词，逗号分隔\n");
        prompt.append("   - 风格限定：3D anime style, cel shading, Unity engine render, high quality\n");
        prompt.append("   - 包含要素：场景、角色、动作、表情、灯光、氛围\n");
        prompt.append("   - 示例：\"3D anime style, cel shading, indoor scene, young male protagonist sitting at desk, focused expression, warm lamp lighting, books and papers scattered, cozy atmosphere, Unity engine, 8k resolution\"\n\n");

        prompt.append("3. 分镜设计原则：\n");
        prompt.append("   - 场景开场使用远景或全景建立空间感\n");
        prompt.append("   - 对话使用中景或近景\n");
        prompt.append("   - 情感表达使用特写\n");
        prompt.append("   - 动作场面使用动态运镜（tracking, dolly）\n");
        prompt.append("   - 根据场景内容合理选择景别和角度\n\n");

        prompt.append("【重要】请严格按照以下JSON格式返回分镜数据（必须是纯JSON，不要有任何其他文字）：\n");
        prompt.append("{\n");
        prompt.append("  \"storyboards\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"shotNumber\": 1,\n");
        prompt.append("      \"shotType\": \"medium\",\n");
        prompt.append("      \"cameraAngle\": \"eye_level\",\n");
        prompt.append("      \"cameraMovement\": \"static\",\n");
        prompt.append("      \"description\": \"镜头描述\",\n");
        prompt.append("      \"visualPrompt\": \"画面生成提示词\",\n");
        prompt.append("      \"duration\": 5,\n");
        prompt.append("      \"characterNames\": \"角色名1,角色名2\",\n");
        prompt.append("      \"dialogue\": \"台词\",\n");
        prompt.append("      \"action\": \"动作描述\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * 解析并保存分镜数据
     */
    private List<StoryboardVo> parseAndSaveStoryboards(
            Long chapterId, Long projectId, String aiResponse,
            List<NovelCharacter> characters) {
        try {
            // 提取JSON
            String jsonStr = extractJson(aiResponse);
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            JsonNode storyboardsArray = jsonNode.get("storyboards");

            if (storyboardsArray == null || !storyboardsArray.isArray()) {
                log.warn("分镜数据格式错误");
                return new ArrayList<>();
            }

            List<StoryboardVo> result = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // 构建角色名称到ID的映射
            var characterNameToId = new java.util.HashMap<String, String>();
            for (NovelCharacter character : characters) {
                characterNameToId.put(character.getName(), character.getId().toString());
            }

            for (JsonNode storyboardNode : storyboardsArray) {
                NovelStoryboard storyboard = new NovelStoryboard();
                storyboard.setChapterId(chapterId);
                storyboard.setProjectId(projectId);
                storyboard.setShotNumber(storyboardNode.has("shotNumber") ?
                    storyboardNode.get("shotNumber").asInt() : 0);
                storyboard.setShotType(storyboardNode.has("shotType") ?
                    storyboardNode.get("shotType").asText() : "medium");
                storyboard.setCameraAngle(storyboardNode.has("cameraAngle") ?
                    storyboardNode.get("cameraAngle").asText() : "eye_level");
                storyboard.setCameraMovement(storyboardNode.has("cameraMovement") ?
                    storyboardNode.get("cameraMovement").asText() : "static");
                storyboard.setDescription(storyboardNode.has("description") ?
                    storyboardNode.get("description").asText() : "");
                storyboard.setVisualPrompt(storyboardNode.has("visualPrompt") ?
                    storyboardNode.get("visualPrompt").asText() : "");
                storyboard.setDuration(storyboardNode.has("duration") ?
                    storyboardNode.get("duration").asInt() : 5);
                storyboard.setDialogue(storyboardNode.has("dialogue") ?
                    storyboardNode.get("dialogue").asText() : "");
                storyboard.setAction(storyboardNode.has("action") ?
                    storyboardNode.get("action").asText() : "");
                storyboard.setStatus("pending");
                storyboard.setSortOrder(storyboardNode.has("shotNumber") ?
                    storyboardNode.get("shotNumber").asInt() : 0);
                storyboard.setCreateTime(now);
                storyboard.setUpdateTime(now);

                // 处理角色名称到ID的转换
                if (storyboardNode.has("characterNames")) {
                    String characterNames = storyboardNode.get("characterNames").asText();
                    String[] names = characterNames.split(",");
                    StringBuilder characterIds = new StringBuilder();
                    for (String name : names) {
                        String trimmedName = name.trim();
                        if (characterNameToId.containsKey(trimmedName)) {
                            if (characterIds.length() > 0) {
                                characterIds.append(",");
                            }
                            characterIds.append(characterNameToId.get(trimmedName));
                        }
                    }
                    storyboard.setCharacterIds(characterIds.toString());
                }

                storyboardMapper.insert(storyboard);

                // 转换为VO
                StoryboardVo vo = convertToVo(storyboard);
                result.add(vo);

                log.info("保存分镜：镜头{}, {}", storyboard.getShotNumber(), storyboard.getDescription());
            }

            return result;

        } catch (Exception e) {
            log.error("解析分镜失败", e);
            log.error("AI响应: {}", aiResponse);
            return new ArrayList<>();
        }
    }

    /**
     * 提取JSON字符串
     */
    private String extractJson(String response) {
        // 去除可能的markdown代码块标记
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*", "");
        return response.trim();
    }

    /**
     * 获取章节的分镜列表
     */
    public List<StoryboardVo> getStoryboardList(Long chapterId) {
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelStoryboard>()
            .eq(NovelStoryboard::getChapterId, chapterId)
            .orderByAsc(NovelStoryboard::getSortOrder);

        List<NovelStoryboard> storyboards = storyboardMapper.selectList(queryWrapper);
        return storyboards.stream()
            .map(this::convertToVo)
            .toList();
    }

    /**
     * 转换为VO
     */
    private StoryboardVo convertToVo(NovelStoryboard entity) {
        StoryboardVo vo = new StoryboardVo();
        vo.setId(entity.getId());
        vo.setChapterId(entity.getChapterId());
        vo.setProjectId(entity.getProjectId());
        vo.setShotNumber(entity.getShotNumber());
        vo.setShotType(entity.getShotType());
        vo.setCameraAngle(entity.getCameraAngle());
        vo.setCameraMovement(entity.getCameraMovement());
        vo.setDescription(entity.getDescription());
        vo.setVisualPrompt(entity.getVisualPrompt());
        vo.setDuration(entity.getDuration());
        vo.setCharacterIds(entity.getCharacterIds());
        vo.setDialogue(entity.getDialogue());
        vo.setAction(entity.getAction());
        vo.setNotes(entity.getNotes());
        vo.setStatus(entity.getStatus());
        vo.setImageUrl(entity.getImageUrl());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }
}
