package com.aifactory.service;

import com.aifactory.common.XmlParser;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.ChapterCharacterExtractXmlDto;
import com.aifactory.dto.ChapterCharacterExtractXmlDto.CharacterExtractDto;
import com.aifactory.dto.ChapterCharacterExtractXmlDto.CultivationSystemDto;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
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

                // 7.3 创建章节关联
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

        // 添加已有角色参考
        String existingCharactersText = buildExistingCharactersText(existingCharacters);
        sb.append(existingCharactersText);

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

        if (worldview != null && worldview.getPowerSystem() != null && !worldview.getPowerSystem().isEmpty()) {
            sb.append("本小说的修炼体系设定如下：\n");
            sb.append(worldview.getPowerSystem()).append("\n");
            sb.append("\n**重要约束：**\n");
            sb.append("1. 角色的修为等级**必须**严格使用上述修炼体系中的等级名称\n");
            sb.append("2. **不得**随意创造新的修炼体系或等级\n");
            sb.append("3. 如果章节中没有明确提到角色的修为等级，则不输出V标签\n");
            sb.append("4. 如果角色修炼多套体系，每套体系分别用一个V标签列出\n");
            sb.append("\n示例（修仙+武道双修）：\n");
            sb.append("```xml\n");
            sb.append("<V>\n");
            sb.append("  <SYS>修仙体系</SYS>\n");
            sb.append("  <LV>筑基初期</LV>\n");
            sb.append("  <CH>无变化</CH>\n");
            sb.append("</V>\n");
            sb.append("<V>\n");
            sb.append("  <SYS>武道体系</SYS>\n");
            sb.append("  <LV>三流武者</LV>\n");
            sb.append("  <CH>从末流突破到三流</CH>\n");
            sb.append("</V>\n");
            sb.append("```\n");
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
}
