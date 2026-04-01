package com.aifactory.service.chapter.prompt;

import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.CharacterPromptInfo;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelCharacterChapter;
import com.aifactory.service.*;
import com.aifactory.mapper.NovelCharacterChapterMapper;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.service.NovelCharacterChapterService;
import com.aifactory.service.prompt.PromptTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于模板系统的章节提示词构建器
 *
 * 职责：
 * - 从章节规划等数据中提取变量
 * - 调用PromptTemplateService执行模板
 * - 返回完整提示词
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Slf4j
@Component
public class PromptTemplateBuilder {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ProjectBasicSettingsService projectBasicSettingsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NovelCharacterService novelCharacterService;

    @Autowired
    private NovelCharacterChapterService novelCharacterChapterService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    /**
     * 构建章节生成提示词
     *
     * @param projectId 项目ID
     * @param chapterPlan 章节规划
     * @param volumePlan 分卷规划
     * @param worldview 世界观设定
     * @param recentChapters 前置章节上下文
     * @return 完整提示词
     */
    public String buildChapterPrompt(
        Long projectId,
        NovelChapterPlan chapterPlan,
        NovelVolumePlan volumePlan,
        NovelWorldview worldview,
        List<Chapter> recentChapters
    ) {
        return buildChapterPrompt(projectId, chapterPlan, volumePlan, worldview, recentChapters, null, null);
    }

    /**
     * 构建章节生成提示词（带知识库上下文）
     *
     * @param projectId 项目ID
     * @param chapterPlan 章节规划
     * @param volumePlan 分卷规划
     * @param worldview 世界观设定
     * @param recentChapters 前置章节上下文
     * @param knowledgeContextText 知识库检索上下文（已格式化的文本）
     * @return 完整提示词
     */
    public String buildChapterPrompt(
        Long projectId,
        NovelChapterPlan chapterPlan,
        NovelVolumePlan volumePlan,
        NovelWorldview worldview,
        List<Chapter> recentChapters,
        String knowledgeContextText
    ) {
        return buildChapterPrompt(projectId, chapterPlan, volumePlan, worldview, recentChapters, knowledgeContextText, null);
    }

    /**
     * 构建章节生成提示词（完整版本）
     *
     * @param projectId 项目ID
     * @param chapterPlan 章节规划
     * @param volumePlan 分卷规划
     * @param worldview 世界观设定
     * @param recentChapters 前置章节上下文
     * @param knowledgeContextText 知识库检索上下文（已格式化的文本）
     * @param lastChapterEndingScene 上一章的结束场景
     * @return 完整提示词
     */
    public String buildChapterPrompt(
        Long projectId,
        NovelChapterPlan chapterPlan,
        NovelVolumePlan volumePlan,
        NovelWorldview worldview,
        List<Chapter> recentChapters,
        String knowledgeContextText,
        String lastChapterEndingScene
    ) {
        log.debug("开始构建章节提示词，章节规划ID: {}", chapterPlan.getId());

        // 1. 准备模板变量
        Map<String, Object> variables = buildTemplateVariables(
            projectId, chapterPlan, volumePlan, worldview, recentChapters, knowledgeContextText, lastChapterEndingScene
        );

        // 2. 执行模板
        String templateCode = "llm_chapter_generate_standard";
        String prompt = promptTemplateService.executeTemplate(templateCode, variables);

        log.debug("章节提示词构建完成，长度: {}", prompt.length());

        return prompt;
    }

    /**
     * 构建模板变量
     */
    private Map<String, Object> buildTemplateVariables(
        Long projectId,
        NovelChapterPlan chapterPlan,
        NovelVolumePlan volumePlan,
        NovelWorldview worldview,
        List<Chapter> recentChapters,
        String knowledgeContextText,
        String lastChapterEndingScene
    ) {
        Map<String, Object> variables = new HashMap<>();

        // 基础变量
        variables.put("role", "资深网文作家");

        // 字数相关
        int targetWordCount = chapterPlan.getWordCountTarget() != null
            ? chapterPlan.getWordCountTarget() : 3000;
        variables.put("targetWordCount", targetWordCount);
        variables.put("maxWordCount", (int)(targetWordCount * 1.05));
        variables.put("minWordCount", (int)(targetWordCount * 0.9));

        // 章节信息
        variables.put("volumeNumber", volumePlan.getVolumeNumber());
        variables.put("chapterNumber", chapterPlan.getChapterNumber());
        variables.put("chapterTitle", chapterPlan.getChapterTitle());
        variables.put("chapterOutline", chapterPlan.getPlotOutline() != null ? chapterPlan.getPlotOutline() : "");
        variables.put("startingScene", chapterPlan.getChapterStartingScene() != null ? chapterPlan.getChapterStartingScene() : "");
        variables.put("endingScene", chapterPlan.getChapterEndingScene() != null ? chapterPlan.getChapterEndingScene() : "");

        // 上一章的结束场景（用于连贯性）
        variables.put("lastChapterEndingScene", lastChapterEndingScene != null ? lastChapterEndingScene : "");

        // 分卷信息
        variables.put("volumeTitle", volumePlan.getVolumeTitle());
        variables.put("volumeTheme", volumePlan.getVolumeTheme());
        // 完整的分卷信息（包含主旨、冲突、情节走向、核心目标、关键事件等）
        variables.put("volumeInfo", buildVolumeInfo(volumePlan));

        // 情节阶段
        variables.put("plotStage", getPlotStageDescription(chapterPlan.getPlotStage()));
        variables.put("plotStageDescription", getPlotStageDescription(chapterPlan.getPlotStage()));

        // 世界观
        variables.put("worldview", buildWorldviewString(worldview));

        // 前置章节（构建为文本块）
        variables.put("recentChapters", buildRecentChaptersText(recentChapters));

        // 知识库上下文（如果有）
        variables.put("knowledgeContext", knowledgeContextText != null ? knowledgeContextText : "");

        // 项目基础设置（新增）
        addBasicSettingsVariables(projectId, variables);

        // 角色信息（新增）
        List<CharacterPromptInfo> characterPromptInfoList = buildCharacterPromptInfoList(
            projectId, chapterPlan.getChapterNumber()
        );
        variables.put("characterInfo", buildCharacterInfoText(characterPromptInfoList));

        return variables;
    }

    /**
     * 添加项目基础设置变量
     */
    private void addBasicSettingsVariables(Long projectId, Map<String, Object> variables) {
        try {
            ProjectBasicSettings settings = projectBasicSettingsService.getByProjectId(projectId);
            if (settings != null) {
                // 使用字典转换为中文描述
                variables.put("narrativeStructure",
                    BasicSettingsDictionary.getNarrativeStructure(settings.getNarrativeStructure()));
                variables.put("endingType",
                    BasicSettingsDictionary.getEndingType(settings.getEndingType()));
                variables.put("endingTone",
                    BasicSettingsDictionary.getEndingTone(settings.getEndingTone()));
                variables.put("writingStyle",
                    BasicSettingsDictionary.getWritingStyle(settings.getWritingStyle()));
                variables.put("writingPerspective",
                    BasicSettingsDictionary.getWritingPerspective(settings.getWritingPerspective()));
                variables.put("narrativePace",
                    BasicSettingsDictionary.getNarrativePace(settings.getNarrativePace()));
                variables.put("languageStyle",
                    BasicSettingsDictionary.getLanguageStyle(settings.getLanguageStyle()));

                // 描写重点是JSON数组，需要解析并转换
                if (settings.getDescriptionFocus() != null && !settings.getDescriptionFocus().isEmpty()) {
                    try {
                        List<String> focusList = objectMapper.readValue(
                            settings.getDescriptionFocus(),
                            new TypeReference<List<String>>() {}
                        );
                        List<String> focusDescriptions = BasicSettingsDictionary.getDescriptionFocusList(focusList);
                        variables.put("descriptionFocus", String.join("、", focusDescriptions));
                    } catch (Exception e) {
                        log.warn("解析描写重点失败: {}", e.getMessage());
                        variables.put("descriptionFocus", "");
                    }
                } else {
                    variables.put("descriptionFocus", "");
                }

                log.debug("成功添加项目基础设置，项目ID: {}", projectId);
            } else {
                // 如果没有基础设置，使用默认值
                variables.put("narrativeStructure", "");
                variables.put("endingType", "");
                variables.put("endingTone", "");
                variables.put("writingStyle", "");
                variables.put("writingPerspective", "第三人称");
                variables.put("narrativePace", "");
                variables.put("languageStyle", "");
                variables.put("descriptionFocus", "");
                log.debug("未找到项目基础设置，使用默认值，项目ID: {}", projectId);
            }
        } catch (Exception e) {
            log.error("获取项目基础设置失败: {}", e.getMessage(), e);
            // 出错时使用默认值
            variables.put("narrativeStructure", "");
            variables.put("endingType", "");
            variables.put("endingTone", "");
            variables.put("writingStyle", "");
            variables.put("writingPerspective", "第三人称");
            variables.put("narrativePace", "");
            variables.put("languageStyle", "");
            variables.put("descriptionFocus", "");
        }
    }

    /**
     * 构建前置章节文本
     */
    private String buildRecentChaptersText(List<Chapter> recentChapters) {
        if (recentChapters == null || recentChapters.isEmpty()) {
            return "无前置章节";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【前置章节信息 - 严格遵循的设定】\n\n");

        // 按章节号排序，确保顺序正确
        List<Chapter> sortedChapters = recentChapters.stream()
            .sorted((a, b) -> Integer.compare(a.getChapterNumber(), b.getChapterNumber()))
            .toList();

        // 提取主要人物（从最近一章的摘要中提取）
        StringBuilder characters = new StringBuilder();
        characters.append("【当前主要人物】\n");

        for (Chapter chapter : sortedChapters) {
            String summary = chapter.getPlotSummary();
            if (summary != null && !summary.isEmpty()) {
                
            }
        }

        // 最近一章（直接前章）提供详细内容
        Chapter lastChapter = sortedChapters.get(sortedChapters.size() - 1);
        sb.append("=== 直接前章（第").append(lastChapter.getChapterNumber()).append("章）详细内容 ===\n");
        sb.append("章节标题：").append(lastChapter.getTitle()).append("\n");
        sb.append("剧情概要：").append(lastChapter.getPlotSummary() != null ? lastChapter.getPlotSummary() : "无").append("\n");
        if (lastChapter.getContent() != null && !lastChapter.getContent().isEmpty()) {
            // 取前结尾500字作为参考
            String content = lastChapter.getContent();
            if (content.length() > 500) {
                content = content.substring(content.length() - 500) ;
            }
            sb.append("上一章结尾剧情：\n").append(content).append("\n");
        }
        sb.append("\n");

        // 其他前置章节只提供摘要
        if (sortedChapters.size() > 1) {
            sb.append("=== 更早的前置章节摘要 ===\n");
            for (int i = 0; i < sortedChapters.size() - 1; i++) {
                Chapter chapter = sortedChapters.get(i);
                sb.append(String.format("第%d章 %s: %s\n",
                    chapter.getChapterNumber(),
                    chapter.getTitle(),
                    chapter.getPlotSummary() != null ? chapter.getPlotSummary() : "无摘要"
                ));
            }
            sb.append("\n");
        }

        sb.append("【人物一致性要求】\n");
        sb.append("1. 本章出现的人物必须是上述章节中已存在的人物\n");
        sb.append("2. 主角的同伴/伙伴姓名必须与前章保持一致\n");
        sb.append("3. 不要创造新角色来替代已有角色\n");
        sb.append("4. 如果前章结尾主角与某人在一起，本章开头必须仍与此人在一起\n");

        return sb.toString();
    }

    /**
     * 获取情节阶段描述
     */
    private String getPlotStageDescription(String plotStage) {
        if (plotStage == null) {
            return "矛盾升级、主角成长、埋设伏笔";
        }

        return switch (plotStage) {
            case "introduction" -> "建立世界观、主角登场、铺垫矛盾";
            case "development" -> "矛盾升级、主角成长、埋设伏笔";
            case "climax" -> "矛盾爆发、伏笔回收、主角蜕变";
            case "conclusion" -> "解决矛盾、回收伏笔、角色成长";
            default -> "矛盾升级、主角成长、埋设伏笔";
        };
    }

    /**
     * 构建世界观字符串（完整版）
     */
    private String buildWorldviewString(NovelWorldview worldview) {
        if (worldview == null) {
            return "暂无世界观设定";
        }

        // 填充 geography 字段（从 novel_continent_region 表构建）
        continentRegionService.fillGeography(worldview);

        StringBuilder sb = new StringBuilder();
        sb.append("- 世界类型：").append(worldview.getWorldType()).append("\n");

        if (worldview.getWorldBackground() != null && !worldview.getWorldBackground().isEmpty()) {
            sb.append("- 世界背景：").append(worldview.getWorldBackground()).append("\n");
        }
        String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
        if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
            sb.append("- 力量体系：").append(powerConstraint).append("\n");
        }
        if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
            sb.append("- 地理环境：").append(worldview.getGeography()).append("\n");
        }
        if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
            sb.append("- 势力分布：").append(worldview.getForces()).append("\n");
        }
        if (worldview.getTimeline() != null && !worldview.getTimeline().isEmpty()) {
            sb.append("- 时间线：").append(worldview.getTimeline()).append("\n");
        }
        if (worldview.getRules() != null && !worldview.getRules().isEmpty()) {
            sb.append("- 世界规则：").append(worldview.getRules()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建角色提取提示词
     *
     * @param chapter 章节信息
     * @param worldview 世界观信息（可能为null）
     * @param existingCharacters 已有角色列表
     * @return 完整的提示词
     */
    public String buildCharacterExtractPrompt(
            Chapter chapter,
            NovelWorldview worldview,
            List<NovelCharacter> existingCharacters
    ) {
        log.debug("开始构建角色提取提示词，章节ID: {}", chapter.getId());

        // 1. 准备模板变量
        Map<String, Object> variables = new HashMap<>();

        // 章节信息
        variables.put("chapterNumber", chapter.getChapterNumber());
        variables.put("chapterTitle", chapter.getTitle() != null ? chapter.getTitle() : "无标题");
        variables.put("chapterContent", chapter.getContent() != null ? chapter.getContent() : "");

        // 修炼体系约束
        variables.put("powerSystemConstraint", buildPowerSystemConstraint(worldview));

        // 已有角色参考
        variables.put("existingCharacters", buildExistingCharactersText(existingCharacters));

        // 2. 执行模板
        String templateCode = "llm_chapter_character_extract";
        String prompt = promptTemplateService.executeTemplate(templateCode, variables);

        log.debug("角色提取提示词构建完成，长度: {}", prompt.length());

        return prompt;
    }

    /**
     * 构建修炼体系约束文本
     *
     * @param worldview 世界观信息
     * @return 修炼体系约束文本
     */
    public String buildPowerSystemConstraint(NovelWorldview worldview) {
        if (worldview == null) {
            return "本小说未设定修炼体系，所有角色的修为字段留空（不输出V标签）。\n";
        }
        return powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
    }

    /**
     * 构建已有角色参考文本
     *
     * @param characters 已有角色列表
     * @return 已有角色参考文本
     */
    public String buildExistingCharactersText(List<NovelCharacter> characters) {
        StringBuilder sb = new StringBuilder();

        if (characters != null && !characters.isEmpty()) {
            sb.append("以下角色已在本项目中存在。如果章节中出现的角色与已有角色同名且特征匹配，视为同一角色：\n\n");

            for (NovelCharacter existing : characters) {
                sb.append("- **").append(existing.getName()).append("**\n");
                sb.append("  - 角色类型：").append(existing.getRoleType() != null ? existing.getRoleType() : "未知").append("\n");

                if (existing.getGender() != null) {
                    sb.append("  - 性别：").append(formatGender(existing.getGender())).append("\n");
                }
                if (existing.getAge() != null) {
                    sb.append("  - 年龄：").append(existing.getAge()).append("岁\n");
                }
                if (existing.getPersonality() != null && !existing.getPersonality().isEmpty()) {
                    sb.append("  - 性格：").append(existing.getPersonality()).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("暂无已有角色。\n");
        }

        return sb.toString();
    }

    /**
     * 格式化性别显示
     */
    private String formatGender(String gender) {
        if (gender == null) return "未知";
        return switch (gender.toLowerCase()) {
            case "male" -> "男";
            case "female" -> "女";
            default -> "其他";
        };
    }

    /**
     * 构建完整的分卷信息字符串
     * 包含所有AI优化生成的字段（公共方法，可被多处调用）
     *
     * @param volume 分卷规划
     * @return 格式化的分卷信息字符串
     */
    public String buildVolumeInfo(NovelVolumePlan volume) {
        if (volume == null) {
            return "暂无分卷信息";
        }

        StringBuilder info = new StringBuilder();
        info.append("【分卷详细信息】\n\n");

        // 基础信息
        info.append("卷号：第").append(volume.getVolumeNumber()).append("卷\n");
        info.append("卷名：").append(volume.getVolumeTitle() != null ? volume.getVolumeTitle() : "").append("\n\n");

        // 核心设定
        if (volume.getVolumeTheme() != null && !volume.getVolumeTheme().isEmpty()) {
            info.append("【本卷主旨】\n").append(volume.getVolumeTheme()).append("\n\n");
        }
        if (volume.getMainConflict() != null && !volume.getMainConflict().isEmpty()) {
            info.append("【主要冲突】\n").append(volume.getMainConflict()).append("\n\n");
        }
        if (volume.getPlotArc() != null && !volume.getPlotArc().isEmpty()) {
            info.append("【情节走向】\n").append(volume.getPlotArc()).append("\n\n");
        }
        if (volume.getCoreGoal() != null && !volume.getCoreGoal().isEmpty()) {
            info.append("【核心目标】\n").append(volume.getCoreGoal()).append("\n\n");
        }

        // 关键事件（5阶段）
        if (volume.getKeyEvents() != null && !volume.getKeyEvents().isEmpty()) {
            info.append("【关键事件（按情节阶段划分）】\n");
            try {
                com.fasterxml.jackson.databind.JsonNode keyEvents = objectMapper.readTree(volume.getKeyEvents());
                String[] stages = {"opening", "development", "turning", "climax", "ending"};
                String[] stageNames = {"开篇阶段", "发展阶段", "转折阶段", "高潮阶段", "收尾阶段"};

                for (int i = 0; i < stages.length; i++) {
                    com.fasterxml.jackson.databind.JsonNode stageEvents = keyEvents.get(stages[i]);
                    if (stageEvents != null && stageEvents.isArray()) {
                        info.append("\n").append(stageNames[i]).append("：\n");
                        for (int j = 0; j < stageEvents.size(); j++) {
                            info.append("  ").append(j + 1).append(". ").append(stageEvents.get(j).asText()).append("\n");
                        }
                    }
                }
                info.append("\n");
            } catch (Exception e) {
                log.warn("解析关键事件JSON失败，使用原始内容: {}", e.getMessage());
                info.append(volume.getKeyEvents()).append("\n\n");
            }
        }

        // 高潮与收尾
        if (volume.getClimax() != null && !volume.getClimax().isEmpty()) {
            info.append("【高潮事件描述】\n").append(volume.getClimax()).append("\n\n");
        }
        if (volume.getEnding() != null && !volume.getEnding().isEmpty()) {
            info.append("【收尾描述】\n").append(volume.getEnding()).append("\n\n");
        }

        // 分卷描述
        if (volume.getVolumeDescription() != null && !volume.getVolumeDescription().isEmpty()) {
            info.append("【分卷综合描述】\n").append(volume.getVolumeDescription()).append("\n\n");
        }

        // 时间线设定
        if (volume.getTimelineSetting() != null && !volume.getTimelineSetting().isEmpty()) {
            info.append("【时间线设定】\n").append(volume.getTimelineSetting()).append("\n\n");
        }

        // 创作备注
        if (volume.getVolumeNotes() != null && !volume.getVolumeNotes().isEmpty()) {
            info.append("【创作注意事项】\n").append(volume.getVolumeNotes()).append("\n");
        }

        return info.toString();
    }

    // ==================== 角色信息构建（新增） ====================

    /**
     * 构建角色提示词信息列表
     * 获取项目中所有非NPC角色，并结合其上次登场状态
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节序号
     * @return 角色提示词信息列表
     */
    public List<CharacterPromptInfo> buildCharacterPromptInfoList(Long projectId, Integer currentChapterNumber) {
        log.debug("开始构建角色提示词信息， projectId={}, currentChapterNumber={}", projectId, currentChapterNumber);

        // 1. 获取所有非NPC角色
        List<NovelCharacter> nonNpcCharacters = novelCharacterService.getNonNpcCharacters(projectId);

        if (nonNpcCharacters == null || nonNpcCharacters.isEmpty()) {
            log.debug("项目中没有非NPC角色, projectId={}", projectId);
            return new ArrayList<>();
        }

        List<CharacterPromptInfo> result = new ArrayList<>();

        // 2. 为每个角色构建提示词信息
        for (NovelCharacter character : nonNpcCharacters) {
            CharacterPromptInfo info = new CharacterPromptInfo();

            // 巻加基础信息
            info.setCharacterId(character.getId());
            info.setName(character.getName());
            info.setRoleType(character.getRoleType());
            info.setGender(formatGender(character.getGender()));
            info.setAge(character.getAge());
            info.setRole(character.getRole());
            info.setPersonality(character.getPersonality());
            info.setAppearance(character.getAppearance());
            info.setBackground(character.getBackground());
            info.setAbilities(character.getAbilities());

            // 3. 获取上次登场状态（当前章节之前）
            NovelCharacterChapter lastAppearance = novelCharacterChapterService
                    .getPreviousAppearance(character.getId(), projectId, currentChapterNumber);

            if (lastAppearance != null) {
                info.setStatusInChapter(lastAppearance.getStatusInChapter());
                info.setEmotionChange(lastAppearance.getEmotionChange());
                info.setKeyBehavior(lastAppearance.getKeyBehavior());
                info.setAppearanceChange(lastAppearance.getAppearanceChange());
                info.setPersonalityReveal(lastAppearance.getPersonalityReveal());
                info.setAbilityShown(lastAppearance.getAbilityShown());
                info.setCharacterDevelopment(lastAppearance.getCharacterDevelopment());
                info.setCultivationLevel(lastAppearance.getCultivationLevel());
                log.debug("角色 {} 有上次登场记录, 章节={}", character.getName(), lastAppearance.getChapterNumber());
            } else {
                log.debug("角色 {} 是首次登场或之前没有记录", character.getName());
            }

            result.add(info);
        }

        log.debug("角色提示词信息构建完成, 共 {} 个角色", result.size());
        return result;
    }

    /**
     * 构建角色信息文本块
     * 将角色提示词信息列表格式化为可读的文本
     *
     * @param characters 角色提示词信息列表
     * @return 格式化的角色信息文本
     */
    private String buildCharacterInfoText(List<CharacterPromptInfo> characters) {
        if (characters == null || characters.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【主要角色信息】\n\n");

        for (CharacterPromptInfo info : characters) {
            sb.append("=== **").append(info.getName()).append("** ===\n");
            sb.append("- 角色类型：").append(formatRoleType(info.getRoleType())).append("\n");

            if (info.getGender() != null && !info.getGender().isEmpty()) {
                sb.append("- 性别：").append(info.getGender()).append("\n");
            }
            if (info.getAge() != null && !info.getAge().isEmpty()) {
                sb.append("- 年龄：").append(info.getAge()).append("\n");
            }
            if (info.getRole() != null && !info.getRole().isEmpty()) {
                sb.append("- 角色定位：").append(info.getRole()).append("\n");
            }
            if (info.getPersonality() != null && !info.getPersonality().isEmpty()) {
                sb.append("- 性格：").append(info.getPersonality()).append("\n");
            }
            if (info.getAppearance() != null && !info.getAppearance().isEmpty()) {
                sb.append("- 外貌：").append(info.getAppearance()).append("\n");
            }
            if (info.getBackground() != null && !info.getBackground().isEmpty()) {
                sb.append("- 背景故事：").append(info.getBackground()).append("\n");
            }
            if (info.getAbilities() != null && !info.getAbilities().isEmpty()) {
                sb.append("- 能力：").append(info.getAbilities()).append("\n");
            }

            // 上次登场状态（如果有）
            if (hasLastAppearanceInfo(info)) {
                sb.append("\n**上次登场状态：**\n");
                if (info.getStatusInChapter() != null && !info.getStatusInChapter().isEmpty()) {
                    sb.append("- 本章状态：").append(info.getStatusInChapter()).append("\n");
                }
                if (info.getEmotionChange() != null && !info.getEmotionChange().isEmpty()) {
                    sb.append("- 情绪变化：").append(info.getEmotionChange()).append("\n");
                }
                if (info.getKeyBehavior() != null && !info.getKeyBehavior().isEmpty()) {
                    sb.append("- 关键行为：").append(info.getKeyBehavior()).append("\n");
                }
                if (info.getAppearanceChange() != null && !info.getAppearanceChange().isEmpty()) {
                    sb.append("- 外貌变化：").append(info.getAppearanceChange()).append("\n");
                }
                if (info.getPersonalityReveal() != null && !info.getPersonalityReveal().isEmpty()) {
                    sb.append("- 性格展现：").append(info.getPersonalityReveal()).append("\n");
                }
                if (info.getAbilityShown() != null && !info.getAbilityShown().isEmpty()) {
                    sb.append("- 能力展现：").append(info.getAbilityShown()).append("\n");
                }
                if (info.getCharacterDevelopment() != null && !info.getCharacterDevelopment().isEmpty()) {
                    sb.append("- 角色成长：").append(info.getCharacterDevelopment()).append("\n");
                }
                if (info.getCultivationLevel() != null && !info.getCultivationLevel().isEmpty()) {
                    sb.append("- 修为境界：").append(info.getCultivationLevel()).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查角色是否有上次登场信息
     */
    private boolean hasLastAppearanceInfo(CharacterPromptInfo info) {
        return info.getStatusInChapter() != null && !info.getStatusInChapter().isEmpty()
            || info.getEmotionChange() != null && !info.getEmotionChange().isEmpty()
            || info.getKeyBehavior() != null && !info.getKeyBehavior().isEmpty()
            || info.getAppearanceChange() != null && !info.getAppearanceChange().isEmpty()
            || info.getPersonalityReveal() != null && !info.getPersonalityReveal().isEmpty()
            || info.getAbilityShown() != null && !info.getAbilityShown().isEmpty()
            || info.getCharacterDevelopment() != null && !info.getCharacterDevelopment().isEmpty()
            || info.getCultivationLevel() != null && !info.getCultivationLevel().isEmpty();
    }

    /**
     * 格式化角色类型显示
     */
    private String formatRoleType(String roleType) {
        if (roleType == null) return "未知";
        return switch (roleType) {
            case "protagonist" -> "主角";
            case "supporting" -> "配角";
            case "antagonist" -> "反派";
            case "npc" -> "NPC";
            default -> "其他";
        };
    }

}
