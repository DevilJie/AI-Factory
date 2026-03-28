package com.aifactory.service;

import com.aifactory.dto.ChapterMemoryXmlDto;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelCharacterChapter;
import com.aifactory.mapper.NovelCharacterChapterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色-章节关联服务
 * 管理角色在各章节的出现记录
 *
 * @Author CaiZy
 * @Date 2025-03-04
 */
@Slf4j
@Service
public class NovelCharacterChapterService {

    @Autowired
    private NovelCharacterChapterMapper characterChapterMapper;

    /**
     * 保存角色-章节关联记录
     *
     * @param characterId      角色ID
     * @param chapterId        章节ID
     * @param projectId        项目ID
     * @param chapterNumber    章节序号
     * @param statusInChapter  本章状态
     * @param isFirstAppearance 是否首次出现
     * @param importanceLevel  重要程度
     */
    @Transactional
    public void saveCharacterChapterRelation(Long characterId, Long chapterId, Long projectId,
                                              Integer chapterNumber, String statusInChapter,
                                              boolean isFirstAppearance, String importanceLevel) {
        log.info("开始保存角色-章节关联：characterId={}, chapterId={}, projectId={}, chapterNumber={}, isFirstAppearance={}, importanceLevel={}",
                characterId, chapterId, projectId, chapterNumber, isFirstAppearance, importanceLevel);

        try {
            // 检查是否已存在关联记录
            NovelCharacterChapter existing = characterChapterMapper.selectByCharacterAndChapter(characterId, chapterId);
            if (existing != null) {
                // 更新现有记录
                log.info("关联记录已存在，更新记录：relationId={}", existing.getId());
                existing.setStatusInChapter(statusInChapter);
                existing.setImportanceLevel(importanceLevel);
                existing.setUpdateTime(LocalDateTime.now());
                int updateResult = characterChapterMapper.updateById(existing);
                log.info("更新角色-章节关联完成，characterId={}, chapterId={}, updateResult={}",
                        characterId, chapterId, updateResult);
            } else {
                // 创建新记录
                log.info("关联记录不存在，创建新记录");
                NovelCharacterChapter relation = new NovelCharacterChapter();
                relation.setCharacterId(characterId);
                relation.setChapterId(chapterId);
                relation.setProjectId(projectId);
                relation.setChapterNumber(chapterNumber);
                relation.setStatusInChapter(statusInChapter);
                relation.setIsFirstAppearance(isFirstAppearance);
                relation.setImportanceLevel(importanceLevel);
                relation.setCreateTime(LocalDateTime.now());
                relation.setUpdateTime(LocalDateTime.now());
                int insertResult = characterChapterMapper.insert(relation);
                log.info("创建角色-章节关联完成，characterId={}, chapterId={}, isFirstAppearance={}, insertResult={}, 新记录ID={}",
                        characterId, chapterId, isFirstAppearance, insertResult, relation.getId());
            }
        } catch (Exception e) {
            log.error("保存角色-章节关联失败，characterId={}, chapterId={}, 错误：{}",
                    characterId, chapterId, e.getMessage(), e);
            throw e; // 重新抛出异常，让调用方知道保存失败
        }
    }

    /**
     * 保存角色详情到章节关联
     *
     * @param characterId   角色ID
     * @param chapter       章节信息
     * @param characterDetail 角色详情（从XML解析）
     * @param isFirstAppearance 是否首次出现
     */
    @Transactional
    public void saveCharacterChapterRelation(Long characterId, Chapter chapter,
                                              ChapterMemoryXmlDto.CharacterDetailDto characterDetail,
                                              boolean isFirstAppearance) {
        saveCharacterChapterRelation(
                characterId,
                chapter.getId(),
                chapter.getProjectId(),
                chapter.getChapterNumber(),
                characterDetail.getStatus(),
                isFirstAppearance,
                characterDetail.getRoleType()
        );
    }

    /**
     * 根据角色ID获取所有出现的章节
     *
     * @param characterId 角色ID
     * @return 章节-角色关联列表
     */
    public List<NovelCharacterChapter> getChaptersByCharacterId(Long characterId) {
        return characterChapterMapper.selectByCharacterId(characterId);
    }

    /**
     * 根据章节ID获取所有相关角色
     *
     * @param chapterId 章节ID
     * @return 章节-角色关联列表
     */
    public List<NovelCharacterChapter> getCharactersByChapterId(Long chapterId) {
        return characterChapterMapper.selectByChapterId(chapterId);
    }

    /**
     * 检查角色是否在项目中首次出现
     *
     * @param characterId 角色ID
     * @param projectId   项目ID
     * @return true-首次出现，false-非首次出现
     */
    public boolean isFirstAppearance(Long characterId, Long projectId) {
        int count = characterChapterMapper.countByCharacterAndProject(characterId, projectId);
        return count == 0;
    }

    /**
     * 根据角色ID和章节ID获取关联记录
     *
     * @param characterId 角色ID
     * @param chapterId   章节ID
     * @return 章节-角色关联
     */
    public NovelCharacterChapter getByCharacterAndChapter(Long characterId, Long chapterId) {
        return characterChapterMapper.selectByCharacterAndChapter(characterId, chapterId);
    }

    /**
     * 删除角色的所有章节关联
     *
     * @param characterId 角色ID
     */
    @Transactional
    public void deleteByCharacterId(Long characterId) {
        LambdaQueryWrapper<NovelCharacterChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacterChapter::getCharacterId, characterId);
        characterChapterMapper.delete(wrapper);
        log.info("删除角色所有章节关联，characterId={}", characterId);
    }

    /**
     * 删除章节的所有角色关联
     *
     * @param chapterId 章节ID
     */
    @Transactional
    public void deleteByChapterId(Long chapterId) {
        LambdaQueryWrapper<NovelCharacterChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacterChapter::getChapterId, chapterId);
        characterChapterMapper.delete(wrapper);
        log.info("删除章节所有角色关联，chapterId={}", chapterId);
    }

    /**
     * 保存角色-章节关联记录（完整版，支持所有新字段）
     *
     * @param characterId        角色ID
     * @param chapterId          章节ID
     * @param projectId          项目ID
     * @param chapterNumber      章节序号
     * @param statusInChapter    本章状态
     * @param isFirstAppearance  是否首次出现
     * @param importanceLevel    重要程度
     * @param appearanceChange   外貌变化
     * @param personalityReveal  性格展现
     * @param abilityShown       能力展现
     * @param characterDevelopment 角色成长
     * @param dialogueSummary    对话摘要
     * @param cultivationLevel   修为境界（JSON格式）
     */
    @Transactional
    public void saveCharacterChapterRelation(Long characterId, Long chapterId, Long projectId,
                                              Integer chapterNumber, String statusInChapter,
                                              boolean isFirstAppearance, String importanceLevel,
                                              String appearanceChange, String personalityReveal,
                                              String abilityShown, String characterDevelopment,
                                              String dialogueSummary, String cultivationLevel) {
        log.info("开始保存角色-章节关联（完整版）：characterId={}, chapterId={}, projectId={}, chapterNumber={}, " +
                        "isFirstAppearance={}, importanceLevel={}, appearanceChange={}, personalityReveal={}, " +
                        "abilityShown={}, characterDevelopment={}, dialogueSummary={}, cultivationLevel={}",
                characterId, chapterId, projectId, chapterNumber, isFirstAppearance, importanceLevel,
                appearanceChange, personalityReveal, abilityShown, characterDevelopment,
                dialogueSummary, cultivationLevel);

        try {
            // 检查是否已存在关联记录
            NovelCharacterChapter existing = characterChapterMapper.selectByCharacterAndChapter(characterId, chapterId);
            if (existing != null) {
                // 更新现有记录
                log.info("关联记录已存在，更新记录：relationId={}", existing.getId());
                existing.setStatusInChapter(statusInChapter);
                existing.setImportanceLevel(importanceLevel);
                existing.setAppearanceChange(appearanceChange);
                existing.setPersonalityReveal(personalityReveal);
                existing.setAbilityShown(abilityShown);
                existing.setCharacterDevelopment(characterDevelopment);
                existing.setDialogueSummary(dialogueSummary);
                existing.setCultivationLevel(cultivationLevel);
                existing.setUpdateTime(LocalDateTime.now());
                int updateResult = characterChapterMapper.updateById(existing);
                log.info("更新角色-章节关联完成（完整版），characterId={}, chapterId={}, updateResult={}",
                        characterId, chapterId, updateResult);
            } else {
                // 创建新记录
                log.info("关联记录不存在，创建新记录");
                NovelCharacterChapter relation = new NovelCharacterChapter();
                relation.setCharacterId(characterId);
                relation.setChapterId(chapterId);
                relation.setProjectId(projectId);
                relation.setChapterNumber(chapterNumber);
                relation.setStatusInChapter(statusInChapter);
                relation.setIsFirstAppearance(isFirstAppearance);
                relation.setImportanceLevel(importanceLevel);
                relation.setAppearanceChange(appearanceChange);
                relation.setPersonalityReveal(personalityReveal);
                relation.setAbilityShown(abilityShown);
                relation.setCharacterDevelopment(characterDevelopment);
                relation.setDialogueSummary(dialogueSummary);
                relation.setCultivationLevel(cultivationLevel);
                relation.setCreateTime(LocalDateTime.now());
                relation.setUpdateTime(LocalDateTime.now());
                int insertResult = characterChapterMapper.insert(relation);
                log.info("创建角色-章节关联完成（完整版），characterId={}, chapterId={}, isFirstAppearance={}, insertResult={}, 新记录ID={}",
                        characterId, chapterId, isFirstAppearance, insertResult, relation.getId());
            }
        } catch (Exception e) {
            log.error("保存角色-章节关联失败（完整版），characterId={}, chapterId={}, 错误：{}",
                    characterId, chapterId, e.getMessage(), e);
            throw e; // 重新抛出异常，让调用方知道保存失败
        }
    }

    /**
     * 批量保存角色-章节关联记录（完整版）
     * 用于一次性保存多个角色在章节中的出现记录
     *
     * @param relations 角色-章节关联列表
     */
    @Transactional
    public void batchSaveCharacterChapterRelations(List<NovelCharacterChapter> relations) {
        if (relations == null || relations.isEmpty()) {
            log.warn("批量保存角色-章节关联：关联列表为空，跳过保存");
            return;
        }

        log.info("开始批量保存角色-章节关联，数量={}", relations.size());
        int successCount = 0;
        int failCount = 0;

        for (NovelCharacterChapter relation : relations) {
            try {
                // 检查是否已存在关联记录
                NovelCharacterChapter existing = characterChapterMapper.selectByCharacterAndChapter(
                        relation.getCharacterId(), relation.getChapterId());

                if (existing != null) {
                    // 更新现有记录
                    updateExistingRelation(existing, relation);
                    characterChapterMapper.updateById(existing);
                    log.debug("更新角色-章节关联：characterId={}, chapterId={}",
                            relation.getCharacterId(), relation.getChapterId());
                } else {
                    // 创建新记录
                    relation.setCreateTime(LocalDateTime.now());
                    relation.setUpdateTime(LocalDateTime.now());
                    characterChapterMapper.insert(relation);
                    log.debug("创建角色-章节关联：characterId={}, chapterId={}, 新ID={}",
                            relation.getCharacterId(), relation.getChapterId(), relation.getId());
                }
                successCount++;
            } catch (Exception e) {
                log.error("批量保存角色-章节关联失败：characterId={}, chapterId={}, 错误：{}",
                        relation.getCharacterId(), relation.getChapterId(), e.getMessage());
                failCount++;
            }
        }

        log.info("批量保存角色-章节关联完成：成功={}, 失败={}", successCount, failCount);

        // 如果全部失败，抛出异常
        if (failCount > 0 && successCount == 0) {
            throw new RuntimeException("批量保存角色-章节关联全部失败");
        }
    }

    /**
     * 更新现有关联记录（内部辅助方法）
     * 将新数据合并到现有记录中
     *
     * @param existing 现有记录
     * @param newData  新数据
     */
    private void updateExistingRelation(NovelCharacterChapter existing, NovelCharacterChapter newData) {
        // 更新基础字段
        if (newData.getStatusInChapter() != null) {
            existing.setStatusInChapter(newData.getStatusInChapter());
        }
        if (newData.getImportanceLevel() != null) {
            existing.setImportanceLevel(newData.getImportanceLevel());
        }

        // 更新扩展字段（只在有新值时更新）
        if (newData.getAppearanceChange() != null) {
            existing.setAppearanceChange(newData.getAppearanceChange());
        }
        if (newData.getPersonalityReveal() != null) {
            existing.setPersonalityReveal(newData.getPersonalityReveal());
        }
        if (newData.getAbilityShown() != null) {
            existing.setAbilityShown(newData.getAbilityShown());
        }
        if (newData.getCharacterDevelopment() != null) {
            existing.setCharacterDevelopment(newData.getCharacterDevelopment());
        }
        if (newData.getDialogueSummary() != null) {
            existing.setDialogueSummary(newData.getDialogueSummary());
        }
        if (newData.getCultivationLevel() != null) {
            existing.setCultivationLevel(newData.getCultivationLevel());
        }

        existing.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 更新角色的修为境界
     * 用于单独更新角色在某章节中的修为境界信息
     *
     * @param characterId     角色ID
     * @param chapterId       章节ID
     * @param cultivationLevel 修为境界（JSON格式）
     * @return 更新是否成功
     */
    @Transactional
    public boolean updateCultivationLevel(Long characterId, Long chapterId, String cultivationLevel) {
        log.info("更新角色修为境界：characterId={}, chapterId={}, cultivationLevel={}",
                characterId, chapterId, cultivationLevel);

        try {
            NovelCharacterChapter existing = characterChapterMapper.selectByCharacterAndChapter(characterId, chapterId);
            if (existing == null) {
                log.warn("更新角色修为境界失败：关联记录不存在，characterId={}, chapterId={}", characterId, chapterId);
                return false;
            }

            existing.setCultivationLevel(cultivationLevel);
            existing.setUpdateTime(LocalDateTime.now());
            int result = characterChapterMapper.updateById(existing);

            log.info("更新角色修为境界完成：characterId={}, chapterId={}, result={}", characterId, chapterId, result);
            return result > 0;
        } catch (Exception e) {
            log.error("更新角色修为境界失败：characterId={}, chapterId={}, 错误：{}",
                    characterId, chapterId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据项目ID获取所有角色-章节关联记录
     *
     * @param projectId 项目ID
     * @return 角色-章节关联列表
     */
    public List<NovelCharacterChapter> getByProjectId(Long projectId) {
        LambdaQueryWrapper<NovelCharacterChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacterChapter::getProjectId, projectId)
                .orderByAsc(NovelCharacterChapter::getChapterNumber);
        return characterChapterMapper.selectList(wrapper);
    }

    /**
     * 获取角色在指定章节之前的最后一次出现记录
     * 用于获取角色的上一章节状态，便于计算成长变化
     *
     * @param characterId   角色ID
     * @param projectId     项目ID
     * @param chapterNumber 当前章节序号
     * @return 上一章节的关联记录，如果没有则返回null
     */
    public NovelCharacterChapter getPreviousAppearance(Long characterId, Long projectId, Integer chapterNumber) {
        LambdaQueryWrapper<NovelCharacterChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacterChapter::getCharacterId, characterId)
                .eq(NovelCharacterChapter::getProjectId, projectId)
                .lt(NovelCharacterChapter::getChapterNumber, chapterNumber)
                .orderByDesc(NovelCharacterChapter::getChapterNumber)
                .last("LIMIT 1");
        return characterChapterMapper.selectOne(wrapper);
    }
}
