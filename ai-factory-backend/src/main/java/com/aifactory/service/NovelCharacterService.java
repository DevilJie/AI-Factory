package com.aifactory.service;

import com.aifactory.common.XmlParser;
import com.aifactory.common.xml.CharacterArcXmlDto;
import com.aifactory.dto.CharacterDto;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelCharacterChapter;
import com.aifactory.entity.Project;
import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.vo.ArcStageVO;
import com.aifactory.vo.CharacterArcVO;
import com.aifactory.vo.CharacterChapterVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * 获取人物列表
     */
    public List<CharacterDto> getCharacterList(Long projectId) {
        LambdaQueryWrapper<NovelCharacter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelCharacter::getProjectId, projectId)
                .orderByDesc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(queryWrapper);
        return characters.stream()
                .map(this::convertToDto)
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
     * 获取人物详情
     */
    public NovelCharacter getCharacterDetail(Long characterId) {
        return characterMapper.selectById(characterId);
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
}
