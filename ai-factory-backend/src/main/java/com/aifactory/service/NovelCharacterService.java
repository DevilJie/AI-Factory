package com.aifactory.service;

import com.aifactory.common.XmlParser;
import com.aifactory.common.xml.CharacterArcXmlDto;
import com.aifactory.dto.CharacterDto;
import com.aifactory.entity.*;
import com.aifactory.entity.NovelCharacterPowerSystem;
import com.aifactory.mapper.*;
import com.aifactory.mapper.NovelCharacterPowerSystemMapper;
import com.aifactory.vo.ArcStageVO;
import com.aifactory.vo.CharacterArcVO;
import com.aifactory.vo.CharacterChapterVO;
import com.aifactory.vo.CharacterDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人物服务
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@Service
public class NovelCharacterService {

    @Autowired
    private NovelCharacterMapper characterMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectBasicSettingsService basicSettingsService;

    @Autowired
    private AIGenerateService aiGenerateService;

    @Autowired
    private XmlParser xmlParser;

    @Autowired
    private NovelCharacterChapterService characterChapterService;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private NovelCharacterPowerSystemMapper novelCharacterPowerSystemMapper;

    @Autowired
    private NovelPowerSystemMapper novelPowerSystemMapper;

    @Autowired
    private NovelPowerSystemLevelMapper novelPowerSystemLevelMapper;

    @Autowired
    private NovelPowerSystemLevelStepMapper novelPowerSystemLevelStepMapper;

    @Autowired
    private NovelFactionCharacterMapper novelFactionCharacterMapper;

    @Autowired
    private NovelFactionMapper novelFactionMapper;

    /**
     * 获取人物列表（包含聚合的修炼境界和势力信息）
     */
    public List<CharacterDto> getCharacterList(Long projectId) {
        LambdaQueryWrapper<NovelCharacter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelCharacter::getProjectId, projectId)
                .orderByDesc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(queryWrapper);

        if (characters.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch query: collect all character IDs
        List<Long> characterIds = characters.stream()
                .map(NovelCharacter::getId)
                .collect(Collectors.toList());

        // Batch query power system associations
        List<NovelCharacterPowerSystem> allPowerAssocs = novelCharacterPowerSystemMapper.selectList(
                new LambdaQueryWrapper<NovelCharacterPowerSystem>()
                        .in(NovelCharacterPowerSystem::getCharacterId, characterIds));
        Map<Long, List<NovelCharacterPowerSystem>> powerAssocsByCharId = allPowerAssocs.stream()
                .collect(Collectors.groupingBy(NovelCharacterPowerSystem::getCharacterId));

        // Batch resolve power system names
        Map<Long, String> powerSystemNames = new HashMap<>();
        Map<Long, String> levelNames = new HashMap<>();
        if (!allPowerAssocs.isEmpty()) {
            List<Long> psIds = allPowerAssocs.stream()
                    .map(NovelCharacterPowerSystem::getPowerSystemId)
                    .distinct()
                    .collect(Collectors.toList());
            List<NovelPowerSystem> powerSystems = novelPowerSystemMapper.selectBatchIds(psIds);
            for (NovelPowerSystem ps : powerSystems) {
                powerSystemNames.put(ps.getId(), ps.getName());
            }

            List<Long> levelIds = allPowerAssocs.stream()
                    .map(NovelCharacterPowerSystem::getCurrentRealmId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!levelIds.isEmpty()) {
                List<NovelPowerSystemLevel> levels = novelPowerSystemLevelMapper.selectBatchIds(levelIds);
                for (NovelPowerSystemLevel lvl : levels) {
                    levelNames.put(lvl.getId(), lvl.getLevelName());
                }
            }
        }

        // Batch query faction associations
        List<NovelFactionCharacter> allFactionAssocs = novelFactionCharacterMapper.selectList(
                new LambdaQueryWrapper<NovelFactionCharacter>()
                        .in(NovelFactionCharacter::getCharacterId, characterIds));
        Map<Long, List<NovelFactionCharacter>> factionAssocsByCharId = allFactionAssocs.stream()
                .collect(Collectors.groupingBy(NovelFactionCharacter::getCharacterId));

        // Batch resolve faction names
        Map<Long, String> factionNames = new HashMap<>();
        if (!allFactionAssocs.isEmpty()) {
            List<Long> factionIds = allFactionAssocs.stream()
                    .map(NovelFactionCharacter::getFactionId)
                    .distinct()
                    .collect(Collectors.toList());
            List<NovelFaction> factions = novelFactionMapper.selectBatchIds(factionIds);
            for (NovelFaction f : factions) {
                factionNames.put(f.getId(), f.getName());
            }
        }

        // Build DTOs with aggregated data
        return characters.stream()
                .map(character -> {
                    CharacterDto dto = convertToDto(character);

                    // Build cultivationRealm string
                    List<NovelCharacterPowerSystem> charPowerAssocs = powerAssocsByCharId.getOrDefault(
                            character.getId(), new ArrayList<>());
                    if (!charPowerAssocs.isEmpty()) {
                        List<String> realmNames = new ArrayList<>();
                        for (NovelCharacterPowerSystem psa : charPowerAssocs) {
                            if (psa.getCurrentRealmId() != null) {
                                String lvlName = levelNames.get(psa.getCurrentRealmId());
                                if (lvlName != null) {
                                    realmNames.add(lvlName);
                                }
                            }
                        }
                        if (!realmNames.isEmpty()) {
                            dto.setCultivationRealm(String.join("、", realmNames));
                        }
                    }

                    // Build factionInfo string
                    List<NovelFactionCharacter> charFactionAssocs = factionAssocsByCharId.getOrDefault(
                            character.getId(), new ArrayList<>());
                    if (!charFactionAssocs.isEmpty()) {
                        List<String> factionInfoParts = new ArrayList<>();
                        for (NovelFactionCharacter fca : charFactionAssocs) {
                            String fName = factionNames.get(fca.getFactionId());
                            if (fName != null) {
                                String info = fName;
                                if (fca.getRole() != null && !fca.getRole().isBlank()) {
                                    info = fName + fca.getRole();
                                }
                                factionInfoParts.add(info);
                            }
                        }
                        if (!factionInfoParts.isEmpty()) {
                            dto.setFactionInfo(String.join("、", factionInfoParts));
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取项目的非NPC角色列表
     * 用于章节生成提示词中展示角色基础信息
     *
     * @param projectId 项目ID
     * @return 非NPC角色列表（protagonist, supporting, antagonist）
     */
    public List<NovelCharacter> getNonNpcCharacters(Long projectId) {
        LambdaQueryWrapper<NovelCharacter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelCharacter::getProjectId, projectId)
                .in(NovelCharacter::getRoleType, "protagonist", "supporting", "antagonist")
                .orderByAsc(NovelCharacter::getId);
        return characterMapper.selectList(queryWrapper);
    }

    /**
     * 获取人物详情（包含关联的力量体系和势力信息）
     */
    public CharacterDetailVO getCharacterDetail(Long characterId) {
        NovelCharacter character = characterMapper.selectById(characterId);
        if (character == null) {
            return null;
        }

        CharacterDetailVO vo = CharacterDetailVO.fromCharacter(character);

        // Query power system associations
        List<NovelCharacterPowerSystem> powerSystemAssocs = novelCharacterPowerSystemMapper.selectList(
                new LambdaQueryWrapper<NovelCharacterPowerSystem>()
                        .eq(NovelCharacterPowerSystem::getCharacterId, characterId));

        List<CharacterDetailVO.PowerSystemAssociation> powerSystemVOs = new ArrayList<>();
        for (NovelCharacterPowerSystem assoc : powerSystemAssocs) {
            CharacterDetailVO.PowerSystemAssociation psVO = new CharacterDetailVO.PowerSystemAssociation();
            psVO.setId(assoc.getId());
            psVO.setPowerSystemId(assoc.getPowerSystemId());
            psVO.setCurrentRealmId(assoc.getCurrentRealmId());
            psVO.setCurrentSubRealmId(assoc.getCurrentSubRealmId());

            // Resolve names
            NovelPowerSystem system = novelPowerSystemMapper.selectById(assoc.getPowerSystemId());
            if (system != null) {
                psVO.setPowerSystemName(system.getName());
            }
            if (assoc.getCurrentRealmId() != null) {
                NovelPowerSystemLevel level = novelPowerSystemLevelMapper.selectById(assoc.getCurrentRealmId());
                if (level != null) {
                    psVO.setCurrentRealmName(level.getLevelName());
                }
            }
            if (assoc.getCurrentSubRealmId() != null) {
                NovelPowerSystemLevelStep step = novelPowerSystemLevelStepMapper.selectById(assoc.getCurrentSubRealmId());
                if (step != null) {
                    psVO.setCurrentSubRealmName(step.getLevelName());
                }
            }

            powerSystemVOs.add(psVO);
        }
        vo.setPowerSystemAssociations(powerSystemVOs);

        // Query faction associations
        List<NovelFactionCharacter> factionAssocs = novelFactionCharacterMapper.selectList(
                new LambdaQueryWrapper<NovelFactionCharacter>()
                        .eq(NovelFactionCharacter::getCharacterId, characterId));

        List<CharacterDetailVO.FactionAssociation> factionVOs = new ArrayList<>();
        for (NovelFactionCharacter assoc : factionAssocs) {
            CharacterDetailVO.FactionAssociation fVO = new CharacterDetailVO.FactionAssociation();
            fVO.setId(assoc.getId());
            fVO.setFactionId(assoc.getFactionId());
            fVO.setRole(assoc.getRole());

            // Resolve faction name
            NovelFaction faction = novelFactionMapper.selectById(assoc.getFactionId());
            if (faction != null) {
                fVO.setFactionName(faction.getName());
            }

            factionVOs.add(fVO);
        }
        vo.setFactionAssociations(factionVOs);

        return vo;
    }

    /**
     * 创建人物
     */
    @Transactional
    public Long createCharacter(Long projectId, NovelCharacter character) {
        character.setProjectId(projectId);
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());

        characterMapper.insert(character);
        log.info("创建人物成功，projectId={}, name={}", projectId, character.getName());

        return character.getId();
    }

    /**
     * 更新人物
     */
    @Transactional
    public void updateCharacter(Long characterId, NovelCharacter character) {
        NovelCharacter existingCharacter = characterMapper.selectById(characterId);
        if (existingCharacter == null) {
            throw new RuntimeException("人物不存在");
        }

        character.setId(characterId);
        character.setProjectId(existingCharacter.getProjectId());
        character.setUpdateTime(LocalDateTime.now());

        characterMapper.updateById(character);
        log.info("更新人物成功，characterId={}", characterId);
    }

    /**
     * 删除人物
     */
    @Transactional
    public void deleteCharacter(Long characterId) {
        NovelCharacter character = characterMapper.selectById(characterId);
        if (character == null) {
            log.warn("角色不存在，无需删除，characterId={}", characterId);
            return;
        }

        Long projectId = character.getProjectId();

        characterMapper.deleteById(characterId);
        log.info("删除人物成功，characterId={}", characterId);
    }

    /**
     * 转换为DTO
     */
    private CharacterDto convertToDto(NovelCharacter character) {
        if (character == null) {
            return null;
        }

        CharacterDto dto = new CharacterDto();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setAvatar(character.getAvatar());
        dto.setRole(character.getRole());
        dto.setRoleType(character.getRoleType());

        return dto;
    }

    // ==================== 新增方法：人物弧光和成长轨迹管理 ====================

    /**
     * 获取项目的主角列表
     */
    public List<NovelCharacter> getProtagonists(Long projectId) {
        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId)
               .eq(NovelCharacter::getRoleType, "protagonist");
        return characterMapper.selectList(wrapper);
    }

    /**
     * AI生成人物弧光
     */
    public CharacterArcVO generateCharacterArc(Long characterId) {
        NovelCharacter character = characterMapper.selectById(characterId);
        if (character == null) {
            throw new RuntimeException("人物不存在");
        }

        Project project = projectService.getById(character.getProjectId());
        ProjectBasicSettings settings = basicSettingsService.getByProjectId(project.getId());

        String prompt = buildArcPrompt(character, project, settings);

        // 构建AI请求
        com.aifactory.dto.AIGenerateRequest aiRequest = new com.aifactory.dto.AIGenerateRequest();
        aiRequest.setProjectId(character.getProjectId());
        aiRequest.setRequestType("character_arc_generate");
        aiRequest.setTask(prompt);

        com.aifactory.dto.AIGenerateResponse aiResponse = aiGenerateService.generate(aiRequest);
        String responseContent = aiResponse.getContent();

        try {
            CharacterArcXmlDto dto = xmlParser.parse(responseContent, CharacterArcXmlDto.class);
            return convertToArcVO(dto);
        } catch (Exception e) {
            log.error("解析人物弧光XML失败: characterId={}", characterId, e);
            throw new RuntimeException("AI生成失败");
        }
    }

    /**
     * 构建人物弧光生成提示词
     */
    private String buildArcPrompt(NovelCharacter character, Project project,
                                  ProjectBasicSettings settings) {
        return String.format("""
            你是一位专业的人物弧光设计师。请为以下角色设计完整的人物弧光（内在成长轨迹）。

            【角色基础信息】
            姓名：%s
            角色类型：%s
            核心性格：%s

            【故事信息】
            故事类型：%s
            故事基调：%s

            【输出要求】
            1. 设计3个阶段的弧光变化（对应3卷）
            2. 每个阶段包含：描述、内在变化、关键时刻
            3. 符合人物性格和故事逻辑
            4. 使用XML格式输出，标签使用单字母

            【XML输出格式】
            <Arc>
              <S>{初始状态}</S>
              <C>{转折事件}</C>
              <T>
                <E v="{卷号}">
                  <D>{阶段描述}</D>
                  <I>{内在变化}</I>
                  <K>{关键时刻}</K>
                </E>
              </T>
              <E>{最终状态}</E>
            </Arc>
            """,
            character.getName(),
            character.getRoleType(),
            character.getPersonality(),
            project.getStoryGenre(),
            project.getStoryTone()
        );
    }

    /**
     * 转换为弧光VO
     */
    private CharacterArcVO convertToArcVO(CharacterArcXmlDto dto) {
        List<ArcStageVO> stages = new ArrayList<>();
        if (dto.getStages() != null) {
            for (CharacterArcXmlDto.ArcStage stage : dto.getStages()) {
                stages.add(ArcStageVO.builder()
                        .volumeNumber(stage.getVolumeNumber())
                        .description(stage.getDescription())
                        .innerChange(stage.getInnerChange())
                        .keyMoment(stage.getKeyMoment())
                        .build());
            }
        }

        return CharacterArcVO.builder()
                .startingState(dto.getStartingState())
                .catalystEvent(dto.getCatalystEvent())
                .stages(stages)
                .endState(dto.getEndState())
                .build();
    }

    // ==================== 角色出场章节管理 ====================

    /**
     * 获取角色出场的章节列表
     *
     * @param characterId 角色ID
     * @return 角色出场章节列表
     */
    public List<CharacterChapterVO> getCharacterChapters(Long characterId) {
        // 获取角色-章节关联列表
        List<NovelCharacterChapter> chapterRelations = characterChapterService.getChaptersByCharacterId(characterId);

        if (chapterRelations == null || chapterRelations.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO并补充章节标题
        return chapterRelations.stream()
                .map(relation -> {
                    // 获取章节标题
                    String chapterTitle = getChapterTitle(relation.getChapterId());

                    return CharacterChapterVO.builder()
                            .id(relation.getId())
                            .chapterId(relation.getChapterId())
                            .chapterNumber(relation.getChapterNumber())
                            .chapterTitle(chapterTitle)
                            .statusInChapter(relation.getStatusInChapter())
                            .isFirstAppearance(relation.getIsFirstAppearance())
                            .importanceLevel(relation.getImportanceLevel())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取章节标题
     *
     * @param chapterId 章节ID
     * @return 章节标题
     */
    private String getChapterTitle(Long chapterId) {
        try {
            com.aifactory.entity.Chapter chapter = chapterMapper.selectById(chapterId);
            return chapter != null ? chapter.getTitle() : "未知章节";
        } catch (Exception e) {
            log.warn("获取章节标题失败，chapterId={}", chapterId, e);
            return "未知章节";
        }
    }

    // ==================== 角色关联管理 ====================

    /**
     * 添加/更新角色-力量体系关联
     *
     * @param characterId 角色ID
     * @param powerSystemId 力量体系ID
     * @param currentRealmId 当前境界ID（可选）
     * @param currentSubRealmId 当前子境界ID（可选）
     */
    @Transactional
    public void upsertPowerSystemAssociation(Long characterId, Long powerSystemId,
                                              Long currentRealmId, Long currentSubRealmId) {
        NovelCharacterPowerSystem existing = novelCharacterPowerSystemMapper.selectOne(
                new LambdaQueryWrapper<NovelCharacterPowerSystem>()
                        .eq(NovelCharacterPowerSystem::getCharacterId, characterId)
                        .eq(NovelCharacterPowerSystem::getPowerSystemId, powerSystemId));

        if (existing != null) {
            existing.setCurrentRealmId(currentRealmId);
            existing.setCurrentSubRealmId(currentSubRealmId);
            novelCharacterPowerSystemMapper.updateById(existing);
        } else {
            NovelCharacterPowerSystem newAssoc = new NovelCharacterPowerSystem();
            newAssoc.setCharacterId(characterId);
            newAssoc.setPowerSystemId(powerSystemId);
            newAssoc.setCurrentRealmId(currentRealmId);
            newAssoc.setCurrentSubRealmId(currentSubRealmId);
            novelCharacterPowerSystemMapper.insert(newAssoc);
        }
        log.info("保存角色-力量体系关联: characterId={}, powerSystemId={}", characterId, powerSystemId);
    }

    /**
     * 删除角色-力量体系关联
     *
     * @param associationId 关联记录ID
     */
    @Transactional
    public void deletePowerSystemAssociation(Long associationId) {
        novelCharacterPowerSystemMapper.deleteById(associationId);
        log.info("删除角色-力量体系关联: associationId={}", associationId);
    }

    /**
     * 添加/更新角色-势力关联
     *
     * @param characterId 角色ID
     * @param factionId 势力ID
     * @param role 在势力中的职位/角色（可选）
     */
    @Transactional
    public void upsertFactionAssociation(Long characterId, Long factionId, String role) {
        NovelFactionCharacter existing = novelFactionCharacterMapper.selectOne(
                new LambdaQueryWrapper<NovelFactionCharacter>()
                        .eq(NovelFactionCharacter::getCharacterId, characterId)
                        .eq(NovelFactionCharacter::getFactionId, factionId));

        if (existing != null) {
            existing.setRole(role);
            novelFactionCharacterMapper.updateById(existing);
        } else {
            NovelFactionCharacter newAssoc = new NovelFactionCharacter();
            newAssoc.setCharacterId(characterId);
            newAssoc.setFactionId(factionId);
            newAssoc.setRole(role);
            novelFactionCharacterMapper.insert(newAssoc);
        }
        log.info("保存角色-势力关联: characterId={}, factionId={}", characterId, factionId);
    }

    /**
     * 删除角色-势力关联
     *
     * @param associationId 关联记录ID
     */
    @Transactional
    public void deleteFactionAssociation(Long associationId) {
        novelFactionCharacterMapper.deleteById(associationId);
        log.info("删除角色-势力关联: associationId={}", associationId);
    }
}
