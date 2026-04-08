package com.aifactory.service;

import com.aifactory.common.XmlParser;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.ChapterCharacterExtractXmlDto;
import com.aifactory.dto.ChapterCharacterExtractXmlDto.CharacterExtractDto;
import com.aifactory.dto.ChapterCharacterExtractXmlDto.CultivationSystemDto;
import com.aifactory.dto.ChapterCharacterExtractXmlDto.FactionConnectionDto;
import com.aifactory.entity.*;
import com.aifactory.entity.NovelCharacterPowerSystem;
import com.aifactory.mapper.*;
import com.aifactory.mapper.NovelCharacterPowerSystemMapper;
import com.aifactory.service.llm.LLMProviderFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aifactory.service.prompt.PromptTemplateService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 章节角色提取服务
 *
 * 负责从章节内容中自动提取角色信息，并与已有角色进行匹配或创建新角色。
 * 支持异步执行，不阻塞主流程。
 *
 * 核心功能：
 * - 从章节内容中提取角色信息（名称、类型、性别、年龄、性格、外貌等）
 * - 与已有角色进行智能匹配
 * - 自动创建新角色
 * - 创建角色-章节关联记录
 * - 支持修炼体系解析
 *
 * @Author AI-Factory
 * @Date 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterCharacterExtractService {

    private final LLMProviderFactory llmProviderFactory;
    private final XmlParser xmlParser;
    private final ObjectMapper objectMapper;
    private final NovelCharacterMapper characterMapper;
    private final NovelWorldviewMapper worldviewMapper;
    private final NovelCharacterChapterService characterChapterService;
    private final PromptTemplateService promptTemplateService;
    private final PowerSystemService powerSystemService;
    private final NovelCharacterPowerSystemMapper novelCharacterPowerSystemMapper;
    private final NovelPowerSystemMapper novelPowerSystemMapper;
    private final NovelPowerSystemLevelMapper powerSystemLevelMapper;
    private final NovelPowerSystemLevelStepMapper powerSystemLevelStepMapper;
    private final NovelFactionMapper novelFactionMapper;
    private final NovelFactionCharacterMapper novelFactionCharacterMapper;

    /**
     * 提示词模板编码
     */
    private static final String TEMPLATE_CODE = "llm_chapter_character_extract";

    /**
     * 需要跳过的代词/关键词集合
     */
    private static final Set<String> SKIP_WORDS = Set.of(
            "我", "我们", "咱", "咱们", "主角", "主人公", "他", "她", "它", "他们", "她们", "它们"
    );

    /**
     * 异步提取章节中的角色信息
     *
     * 该方法使用 @Async 注解，提交到异步线程池执行，不阻塞主流程。
     * 适用于章节生成完成后的后台角色提取场景。
     *
     * 调用示例：
     * <pre>
     * {@code
     * // 在章节保存后异步提取角色
     * chapterCharacterExtractService.extractCharactersAsync(chapter, userId);
     * // 主流程继续执行，不受角色提取影响
     * }
     * </pre>
     *
     * @param chapter 章节实体，必须包含 projectId、id、chapterNumber、title、content 等信息
     * @param userId 用户ID，用于获取AI提供商配置
     * @return CompletableFuture<Void> 异步任务结果，可用于后续回调处理
     */
    @Async
    public CompletableFuture<Void> extractCharactersAsync(Chapter chapter, Long userId) {
        log.info("[异步任务] 开始提取章节角色，chapterId={}, chapterNumber={}, title={}, userId={}",
                chapter.getId(), chapter.getChapterNumber(), chapter.getTitle(), userId);

        return CompletableFuture.runAsync(() -> {
            try {
                extractCharacters(chapter, userId);
                log.info("[异步任务] 章节角色提取完成,chapterId={}", chapter.getId());
            } catch (Exception e) {
                log.error("[异步任务] 章节角色提取失败,chapterId={}, error={}",
                        chapter.getId(), e.getMessage(), e);
            }
        });
    }

    /**
     * 同步提取章节中的角色信息
     *
     * 执行完整的角色提取流程：
     * 1. 获取世界观信息（包含修炼体系设定）
     * 2. 获取已有角色列表（用于匹配）
     * 3. 构建提示词并调用LLM
     * 4. 解析XML响应
     * 5. 处理每个角色：匹配/创建角色实体
     * 6. 创建角色-章节关联记录
     *
     * @param chapter 章节实体
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void extractCharacters(Chapter chapter, Long userId) {
        Long projectId = chapter.getProjectId();
        Long chapterId = chapter.getId();

        log.info("开始提取章节角色，projectId={}, chapterId={}, chapterNumber={}",
                projectId, chapterId, chapter.getChapterNumber());

        try {
            // 1. 获取世界观信息（包含修炼体系）
            NovelWorldview worldview = getWorldview(projectId);

            // 2. 获取已有角色列表（用于匹配）
            List<NovelCharacter> existingCharacters = getExistingCharacters(projectId);

            // 3. 构建提示词
            String prompt = buildCharacterExtractPrompt(chapter, worldview, existingCharacters);

            // 4. 构建AI请求
            AIGenerateRequest request = new AIGenerateRequest();
            request.setProjectId(projectId);
            request.setVolumePlanId(chapter.getVolumePlanId());  // 记录所属分卷计划
            request.setChapterPlanId(chapter.getChapterPlanId()); // 记录所属章节规划
            request.setChapterId(chapter.getId()); 
            request.setRequestType("llm_chapter_character_extract");
            request.setTask(prompt);
            request.setTemperature(0.3); // 低温度保证稳定输出

            // 5. 调用LLM（使用传入的userId获取AI提供商配置）
            AIGenerateResponse response = llmProviderFactory.getDefaultProvider(userId).generate(request);
            String xmlContent = response.getContent();

            log.debug("LLM响应内容长度: {}", xmlContent.length());

            // 6. 解析XML响应
            ChapterCharacterExtractXmlDto dto = xmlParser.parse(xmlContent, ChapterCharacterExtractXmlDto.class);

            if (dto == null || dto.getCharacters() == null || dto.getCharacters().isEmpty()) {
                log.info("未从章节中提取到角色信息，chapterId={}", chapterId);
                return;
            }

            log.info("解析成功，提取到 {} 个角色", dto.getCharacters().size());

            // 7. 处理每个角色
            for (CharacterExtractDto charDto : dto.getCharacters()) {
                // 7.1 跳过代词等无效名称
                if (shouldSkip(charDto.getName())) {
                    log.debug("跳过无效角色名称: {}", charDto.getName());
                    continue;
                }

                // 7.2 匹配或创建角色
                NovelCharacter character = matchOrCreateCharacter(projectId, charDto, existingCharacters);

                // 7.3 解析并保存角色-力量体系关联 (per D-01/D-02)
                resolveAndSavePowerSystemAssociations(character.getId(), charDto.getCultivationSystems(), projectId);

                // 7.4 解析并保存角色-势力关联 (per D-04/D-05/D-06)
                resolveAndSaveFactionAssociations(character.getId(), charDto.getFactionConnections(), projectId);

                // 7.5 创建章节关联
                createCharacterChapterRelation(chapter, character, charDto);

                log.info("角色处理完成: characterId={}, name={}", character.getId(), character.getName());
            }

            log.info("章节角色提取完成，chapterId={}, 提取角色数={}", chapterId, dto.getCharacters().size());

        } catch (XmlParser.XmlParseException e) {
            log.error("解析角色提取XML失败，chapterId={}, error={}", chapterId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("章节角色提取失败，chapterId={}, error={}", chapterId, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断是否应跳过该名称（代词过滤）
     *
     * @param name 角色名称
     * @return true-应跳过，false-不跳过
     */
    private boolean shouldSkip(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        return SKIP_WORDS.contains(name.trim());
    }

    /**
     * 获取项目的世界观信息
     *
     * @param projectId 项目ID
     * @return 世界观实体，可能为null
     */
    private NovelWorldview getWorldview(Long projectId) {
        try {
            LambdaQueryWrapper<NovelWorldview> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelWorldview::getProjectId, projectId);
            return worldviewMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("获取世界观信息失败，projectId={}", projectId, e);
            return null;
        }
    }

    /**
     * 获取项目的已有角色列表
     *
     * @param projectId 项目ID
     * @return 已有角色列表
     */
    private List<NovelCharacter> getExistingCharacters(Long projectId) {
        try {
            LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelCharacter::getProjectId, projectId);
            return characterMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("获取已有角色列表失败，projectId={}", projectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建角色提取提示词
     *
     * @param chapter 章节信息
     * @param worldview 世界观信息（可能为null）
     * @param existingCharacters 已有角色列表
     * @return 完整的提示词
     */
    private String buildCharacterExtractPrompt(Chapter chapter,
                                                NovelWorldview worldview,
                                                List<NovelCharacter> existingCharacters) {

        Map<String, Object> variables = new java.util.HashMap<>();

        StringBuilder sb = new StringBuilder();
        // 添加章节信息
        sb.append("\n## 章节信息\n");
        sb.append("- 章节号：第").append(chapter.getChapterNumber()).append("章\n");
        sb.append("- 章节标题：").append(chapter.getTitle() != null ? chapter.getTitle() : "无标题").append("\n");

        // 添加修炼体系约束
        String powerSystemConstraint = buildPowerSystemConstraint(worldview);
        sb.append(powerSystemConstraint);

        // 添加中文角色类型定义（per D-07/D-13）
        sb.append("\n## 角色类型定义（严格遵循）\n");
        sb.append("- protagonist：主角，第一视角人物，故事核心人物。一个故事通常只有1-2个主角\n");
        sb.append("- supporting：重要配角，多次出现、对剧情有推动作用\n");
        sb.append("- antagonist：反派，与主角对抗、制造冲突的人物\n");
        sb.append("- npc：过场人物，只在一两章出现、一两句台词的边缘角色\n\n");

        // 添加已有角色参考
        String existingCharactersText = buildExistingCharactersText(existingCharacters);
        sb.append(existingCharactersText);

        // 注入已有角色类型分布到模板变量（per D-08）
        String roleDistribution = buildExistingRoleDistribution(existingCharacters);
        variables.put("existingRoleDistribution", roleDistribution);

        // 添加FC势力标签格式说明（per D-04/D-13）
        sb.append("\n## 势力关联\n");
        sb.append("如章节中出现角色所属势力/门派/组织，为每个角色添加FC标签：\n");
        sb.append("<FC><N>势力名称</N><R>职位/角色</R></FC>\n");
        sb.append("角色可属于多个势力。势力名称必须严格使用已有势力列表中的名称。\n");

        // 注入势力名称列表（per D-04）
        String factionList = buildFactionList(chapter.getProjectId());
        if (factionList != null && !factionList.isEmpty()) {
            sb.append("\n## 已有势力列表\n");
            sb.append(factionList);
            sb.append("\n");
        }

        // 添加章节内容
        sb.append("\n## 章节内容\n");
        sb.append("```\n");
        sb.append(chapter.getContent() != null ? chapter.getContent() : "");
        sb.append("\n```\n");

        sb.append("\n请仔细阅读章节内容，提取所有角色信息，按XML格式输出。");
        variables.put("chapterInfo", sb.toString());

        return promptTemplateService.executeTemplate(TEMPLATE_CODE, variables);
    }

    /**
     * 构建修炼体系约束文本
     *
     * @param worldview 世界观信息
     * @return 修炼体系约束文本
     */
    private String buildPowerSystemConstraint(NovelWorldview worldview) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 修炼体系设定\n");

        if (worldview != null) {
            sb.append(powerSystemService.buildPowerSystemConstraint(worldview.getProjectId()));
        } else {
            sb.append("本小说未设定修炼体系，所有角色的修为字段留空（不输出V标签）。\n");
        }

        return sb.toString();
    }

    /**
     * 构建已有角色参考文本
     *
     * @param existingCharacters 已有角色列表
     * @return 已有角色参考文本
     */
    private String buildExistingCharactersText(List<NovelCharacter> existingCharacters) {
        StringBuilder sb = new StringBuilder();

        if (existingCharacters != null && !existingCharacters.isEmpty()) {
            sb.append("\n## 已有角色参考\n");
            sb.append("以下角色已在本项目中存在。如果章节中出现的角色与已有角色同名且特征匹配，视为同一角色：\n\n");

            for (NovelCharacter existing : existingCharacters) {
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
        }

        return sb.toString();
    }

    /**
     * 构建已有角色类型分布文本（per D-08）
     *
     * 将已有角色按 roleType 分组，生成分布概览文本，
     * 注入到提示词中帮助 LLM 准确判断新角色类型。
     *
     * @param existingCharacters 已有角色列表
     * @return 角色类型分布文本，空列表返回空字符串
     */
    private String buildExistingRoleDistribution(List<NovelCharacter> existingCharacters) {
        if (existingCharacters == null || existingCharacters.isEmpty()) return "";

        Map<String, List<String>> byType = existingCharacters.stream()
            .collect(Collectors.groupingBy(
                c -> c.getRoleType() != null ? c.getRoleType() : "unknown",
                Collectors.mapping(NovelCharacter::getName, Collectors.toList())
            ));

        StringBuilder sb = new StringBuilder("## 已有角色类型分布\n");
        byType.forEach((type, names) -> {
            sb.append("- ").append(type).append("：").append(String.join("、", names)).append("\n");
        });
        sb.append("注意：新提取的角色不会改变已有角色的类型。\n\n");
        return sb.toString();
    }

    /**
     * 构建势力名称列表文本（per D-04）
     *
     * 从数据库查询项目下所有势力，生成名称列表，
     * 注入到提示词中供 LLM 在 FC 标签中引用。
     *
     * @param projectId 项目ID
     * @return 势力名称列表文本，无势力时返回空字符串
     */
    private String buildFactionList(Long projectId) {
        try {
            List<NovelFaction> factions = novelFactionMapper.selectList(
                new LambdaQueryWrapper<NovelFaction>()
                    .eq(NovelFaction::getProjectId, projectId)
                    .orderByAsc(NovelFaction::getSortOrder)
            );
            if (factions == null || factions.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (NovelFaction faction : factions) {
                sb.append("- ").append(faction.getName());
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to build faction list for projectId={}", projectId, e);
            return "";
        }
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
     * 匹配或创建角色
     *
     * 首先尝试在已有角色中查找匹配的角色，如果找不到则创建新角色。
     *
     * @param projectId 项目ID
     * @param charDto 提取的角色信息
     * @param existingCharacters 已有角色列表
     * @return 匹配或创建的角色实体
     */
    @Transactional
    public NovelCharacter matchOrCreateCharacter(Long projectId,
                                                   CharacterExtractDto charDto,
                                                   List<NovelCharacter> existingCharacters) {
        // 尝试匹配已有角色
        if (existingCharacters != null) {
            for (NovelCharacter existing : existingCharacters) {
                if (isSameCharacter(existing, charDto)) {
                    log.debug("匹配到已有角色: {}, characterId={}", existing.getName(), existing.getId());
                    return existing;
                }
            }
        }

        // 创建新角色
        NovelCharacter newCharacter = convertToNovelCharacter(projectId, charDto);
        characterMapper.insert(newCharacter);

        // 加入列表避免重复创建
        if (existingCharacters != null) {
            existingCharacters.add(newCharacter);
        }

        log.info("创建新角色: {}, roleType={}, characterId={}",
                newCharacter.getName(), newCharacter.getRoleType(), newCharacter.getId());

        return newCharacter;
    }

    /**
     * 判断是否为同一角色
     *
     * 匹配规则：
     * 1. 名称必须完全相同
     * 2. 如果有性别信息，性别必须匹配
     *
     * @param existing 已有角色
     * @param charDto 提取的角色信息
     * @return true-是同一角色，false-不是
     */
    private boolean isSameCharacter(NovelCharacter existing, CharacterExtractDto charDto) {
        // 名称必须匹配
        if (!existing.getName().equals(charDto.getName())) {
            return false;
        }

        // 如果有性别信息，检查是否匹配
        if (existing.getGender() != null && charDto.getGender() != null) {
            String existingGender = existing.getGender().toLowerCase();
            String dtoGender = parseGender(charDto.getGender()).toLowerCase();
            if (!existingGender.equals(dtoGender)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 将DTO转换为NovelCharacter实体并设置基本信息
     *
     * @param projectId 项目ID
     * @param charDto 角色DTO
     * @return NovelCharacter实体
     */
    private NovelCharacter convertToNovelCharacter(Long projectId, CharacterExtractDto charDto) {
        NovelCharacter character = new NovelCharacter();
        character.setProjectId(projectId);
        character.setName(charDto.getName());
        character.setRoleType(charDto.getRoleType() != null ? charDto.getRoleType() : "npc");
        character.setGender(parseGender(charDto.getGender()));
        // 年龄直接保存原文描述（如"十六七岁"），不再尝试解析为数字
        character.setAge(charDto.getAge());
        character.setPersonality(charDto.getPersonality());
        character.setAppearance(charDto.getAppearance());
        // 背景信息不在章节提取中设置，由用户手动编辑或通过其他方式生成
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());

        return character;
    }

    /**
     * 创建角色-章节关联
     *
     * @param chapter 章节信息
     * @param character 角色信息
     * @param charDto 提取的角色详情
     */
    private void createCharacterChapterRelation(Chapter chapter,
                                                 NovelCharacter character,
                                                 CharacterExtractDto charDto) {
        // 解析修炼体系JSON
        String cultivationLevelJson = parseCultivationLevelJson(charDto.getCultivationSystems());

        // 检查是否首次出现
        boolean isFirstAppearance = characterChapterService.isFirstAppearance(
                character.getId(),
                chapter.getProjectId()
        );

        // 保存章节关联（完整版，包含所有扩展字段）
        characterChapterService.saveCharacterChapterRelation(
                character.getId(),
                chapter.getId(),
                chapter.getProjectId(),
                chapter.getChapterNumber(),
                charDto.getStatus(),
                isFirstAppearance,
                charDto.getRoleType(),
                charDto.getAppearanceChange(),      // AP: 外貌/装扮变化
                charDto.getPersonalityReveal(),     // PR: 性格展现
                charDto.getAbilityDisplay(),        // AB: 能力展现
                charDto.getCharacterDevelopment(),  // CD: 角色成长
                charDto.getKeyDialogue(),           // DG: 核心对话摘要
                cultivationLevelJson                // V: 修炼体系（JSON格式）
        );

        log.info("角色-章节关联创建完成: characterId={}, chapterId={}, isFirstAppearance={}",
                character.getId(), chapter.getId(), isFirstAppearance);
    }

    /**
     * 解析性别
     *
     * @param gender 性别字符串
     * @return 标准化的性别值（male/female/other）
     */
    private String parseGender(String gender) {
        if (gender == null || gender.isEmpty()) {
            return "other";
        }

        return switch (gender.toLowerCase()) {
            case "male", "男", "男性", "男生" -> "male";
            case "female", "女", "女性", "女生" -> "female";
            default -> "other";
        };
    }


    /**
     * 解析修炼体系为JSON字符串
     *
     * @param cultivationSystems 修炼体系列表
     * @return JSON字符串，解析失败返回null
     */
    private String parseCultivationLevelJson(List<CultivationSystemDto> cultivationSystems) {
        if (cultivationSystems == null || cultivationSystems.isEmpty()) {
            return null;
        }

        try {
            // 转换为Map列表
            List<Map<String, String>> cultivationList = new ArrayList<>();
            for (CultivationSystemDto system : cultivationSystems) {
                Map<String, String> map = new HashMap<>();
                map.put("systemName", system.getSystemName());
                map.put("realmLevel", system.getEffectiveRealmLevel());
                map.put("subLevel", system.getEffectiveSubLevel());
                // 向后兼容：保留currentLevel字段
                map.put("currentLevel", system.getCurrentLevel());
                map.put("levelChange", system.getLevelChange());
                cultivationList.add(map);
            }

            return objectMapper.writeValueAsString(cultivationList);
        } catch (JsonProcessingException e) {
            log.warn("解析修炼体系JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析并保存角色-力量体系关联
     *
     * 遍历提取到的修炼体系列表，通过名称匹配找到对应的力量体系和境界等级，
     * 然后upsert到character_power_system关联表。
     *
     * 匹配策略（per D-01）：
     * 1. systemName精确匹配 -> powerSystemId
     * 2. realmLevel精确匹配 -> levelId（大境界，在对应力量体系内）
     * 3. subLevel精确匹配 -> stepId（小境界，在对应大境界内）
     * 4. 匹配失败时ID字段为null（per D-03），文本信息保留在cultivation_level JSON中
     *
     * @param characterId 角色ID
     * @param cultivationSystems 修炼体系DTO列表
     * @param projectId 项目ID
     */
    private void resolveAndSavePowerSystemAssociations(Long characterId,
                                                        List<CultivationSystemDto> cultivationSystems,
                                                        Long projectId) {
        if (cultivationSystems == null || cultivationSystems.isEmpty()) {
            return;
        }

        // 获取项目下所有力量体系，用于内存匹配
        List<NovelPowerSystem> allSystems = novelPowerSystemMapper.selectList(
                new LambdaQueryWrapper<NovelPowerSystem>()
                        .eq(NovelPowerSystem::getProjectId, projectId));

        for (CultivationSystemDto csDto : cultivationSystems) {
            if (csDto.getSystemName() == null || csDto.getSystemName().isBlank()) {
                continue;
            }

            // Step 1: 匹配力量体系名称（精确 -> 模糊）
            NovelPowerSystem matchedSystem = null;
            for (NovelPowerSystem sys : allSystems) {
                if (sys.getName() != null && sys.getName().equals(csDto.getSystemName().trim())) {
                    matchedSystem = sys;
                    break;
                }
            }
            if (matchedSystem == null) {
                // Fuzzy match: LIKE '%systemName%'
                for (NovelPowerSystem sys : allSystems) {
                    if (sys.getName() != null && sys.getName().contains(csDto.getSystemName().trim())) {
                        matchedSystem = sys;
                        break;
                    }
                }
            }

            if (matchedSystem == null) {
                log.debug("力量体系名称匹配失败: systemName={}, projectId={}", csDto.getSystemName(), projectId);
                // Per D-03: 文本信息保留在cultivation_level JSON中，不丢弃
                continue;
            }

            // Step 2: 使用realmLevel匹配大境界（在对应力量体系内）
            Long matchedLevelId = null;
            Long matchedStepId = null;
            String effectiveRealmLevel = csDto.getEffectiveRealmLevel();

            if (effectiveRealmLevel != null) {
                List<NovelPowerSystemLevel> levels = powerSystemLevelMapper.selectList(
                        new LambdaQueryWrapper<NovelPowerSystemLevel>()
                                .eq(NovelPowerSystemLevel::getPowerSystemId, matchedSystem.getId()));

                // 先精确匹配levelName
                NovelPowerSystemLevel matchedLevel = null;
                for (NovelPowerSystemLevel lvl : levels) {
                    if (lvl.getLevelName() != null && lvl.getLevelName().equals(effectiveRealmLevel)) {
                        matchedLevel = lvl;
                        break;
                    }
                }
                // Fuzzy match
                if (matchedLevel == null) {
                    for (NovelPowerSystemLevel lvl : levels) {
                        if (lvl.getLevelName() != null && lvl.getLevelName().contains(effectiveRealmLevel)) {
                            matchedLevel = lvl;
                            break;
                        }
                    }
                }
                if (matchedLevel != null) {
                    matchedLevelId = matchedLevel.getId();

                    // Step 3: 使用subLevel匹配小境界（在对应大境界内）
                    String effectiveSubLevel = csDto.getEffectiveSubLevel();
                    if (effectiveSubLevel != null) {
                        List<NovelPowerSystemLevelStep> steps = powerSystemLevelStepMapper.selectList(
                                new LambdaQueryWrapper<NovelPowerSystemLevelStep>()
                                        .eq(NovelPowerSystemLevelStep::getPowerSystemLevelId, matchedLevel.getId()));
                        // 精确匹配step levelName
                        for (NovelPowerSystemLevelStep step : steps) {
                            if (step.getLevelName() != null && step.getLevelName().equals(effectiveSubLevel)) {
                                matchedStepId = step.getId();
                                break;
                            }
                        }
                        // Fuzzy match
                        if (matchedStepId == null) {
                            for (NovelPowerSystemLevelStep step : steps) {
                                if (step.getLevelName() != null && step.getLevelName().contains(effectiveSubLevel)) {
                                    matchedStepId = step.getId();
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Step 4: Upsert novel_character_power_system
            NovelCharacterPowerSystem existing = novelCharacterPowerSystemMapper.selectOne(
                    new LambdaQueryWrapper<NovelCharacterPowerSystem>()
                            .eq(NovelCharacterPowerSystem::getCharacterId, characterId)
                            .eq(NovelCharacterPowerSystem::getPowerSystemId, matchedSystem.getId()));

            if (existing != null) {
                // Update existing record
                existing.setCurrentRealmId(matchedLevelId);
                existing.setCurrentSubRealmId(matchedStepId);
                novelCharacterPowerSystemMapper.updateById(existing);
                log.debug("更新力量体系关联: characterId={}, powerSystemId={}, realmId={}, stepId={}",
                        characterId, matchedSystem.getId(), matchedLevelId, matchedStepId);
            } else {
                // Insert new record
                NovelCharacterPowerSystem newAssoc = new NovelCharacterPowerSystem();
                newAssoc.setCharacterId(characterId);
                newAssoc.setPowerSystemId(matchedSystem.getId());
                newAssoc.setCurrentRealmId(matchedLevelId);
                newAssoc.setCurrentSubRealmId(matchedStepId);
                novelCharacterPowerSystemMapper.insert(newAssoc);
                log.debug("新增力量体系关联: characterId={}, powerSystemId={}, realmId={}, stepId={}",
                        characterId, matchedSystem.getId(), matchedLevelId, matchedStepId);
            }
        }
    }

    /**
     * 解析并保存角色-势力关联
     *
     * 遍历提取到的势力关联列表，通过名称匹配找到对应的势力，
     * 然后upsert到novel_faction_character关联表。
     *
     * 匹配策略（per D-04/D-05/D-06）：
     * 1. factionName精确匹配 -> factionId
     * 2. 角色可属于多个势力，每个势力一条关联记录
     * 3. 匹配失败时跳过（文本信息已在其他地方保留）
     *
     * @param characterId 角色ID
     * @param factionConnections 势力关联DTO列表
     * @param projectId 项目ID
     */
    private void resolveAndSaveFactionAssociations(Long characterId,
                                                    List<FactionConnectionDto> factionConnections,
                                                    Long projectId) {
        if (factionConnections == null || factionConnections.isEmpty()) {
            return;
        }

        // 获取项目下所有势力，用于内存匹配
        List<NovelFaction> allFactions = novelFactionMapper.selectList(
                new LambdaQueryWrapper<NovelFaction>()
                        .eq(NovelFaction::getProjectId, projectId));

        for (FactionConnectionDto fcDto : factionConnections) {
            if (fcDto.getFactionName() == null || fcDto.getFactionName().isBlank()) {
                continue;
            }

            // Step 1: 匹配势力名称（精确 -> 模糊）
            NovelFaction matchedFaction = null;
            for (NovelFaction faction : allFactions) {
                if (faction.getName() != null && faction.getName().equals(fcDto.getFactionName().trim())) {
                    matchedFaction = faction;
                    break;
                }
            }
            if (matchedFaction == null) {
                // Fuzzy match
                for (NovelFaction faction : allFactions) {
                    if (faction.getName() != null && faction.getName().contains(fcDto.getFactionName().trim())) {
                        matchedFaction = faction;
                        break;
                    }
                }
            }

            if (matchedFaction == null) {
                log.debug("势力名称匹配失败: factionName={}, projectId={}", fcDto.getFactionName(), projectId);
                continue;
            }

            // Step 2: Upsert novel_faction_character
            NovelFactionCharacter existing = novelFactionCharacterMapper.selectOne(
                    new LambdaQueryWrapper<NovelFactionCharacter>()
                            .eq(NovelFactionCharacter::getFactionId, matchedFaction.getId())
                            .eq(NovelFactionCharacter::getCharacterId, characterId));

            if (existing != null) {
                // Update role if changed
                if (fcDto.getRole() != null && !fcDto.getRole().equals(existing.getRole())) {
                    existing.setRole(fcDto.getRole());
                    novelFactionCharacterMapper.updateById(existing);
                    log.debug("更新势力关联角色: characterId={}, factionId={}, role={}",
                            characterId, matchedFaction.getId(), fcDto.getRole());
                }
            } else {
                // Insert new record
                NovelFactionCharacter newAssoc = new NovelFactionCharacter();
                newAssoc.setFactionId(matchedFaction.getId());
                newAssoc.setCharacterId(characterId);
                newAssoc.setRole(fcDto.getRole());
                novelFactionCharacterMapper.insert(newAssoc);
                log.debug("新增势力关联: characterId={}, factionId={}, role={}",
                        characterId, matchedFaction.getId(), fcDto.getRole());
            }
        }
    }
}
