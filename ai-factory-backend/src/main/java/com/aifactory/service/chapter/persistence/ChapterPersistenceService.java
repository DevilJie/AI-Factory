package com.aifactory.service.chapter.persistence;

import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 章节数据持久化服务
 *
 * 职责：
 * - 章节数据的CRUD操作
 * - 自动保存逻辑（每200字）
 * - 关联章节规划ID
 * - 字数统计更新
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Service
public class ChapterPersistenceService {

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    /**
     * 创建新章节（关联规划ID）
     *
     * @param chapterPlan 章节规划
     * @return 创建的章节实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Chapter createChapterFromPlan(NovelChapterPlan chapterPlan) {
        // 计算全局章节序号 = 前面所有卷的章节数 + 当前卷内序号
        int globalChapterNumber = calculateGlobalChapterNumber(chapterPlan);

        Chapter chapter = new Chapter();
        chapter.setProjectId(chapterPlan.getProjectId());
        chapter.setVolumePlanId(chapterPlan.getVolumePlanId());
        chapter.setChapterPlanId(chapterPlan.getId());
        chapter.setChapterNumber(globalChapterNumber);
        chapter.setTitle(chapterPlan.getChapterTitle());
        chapter.setContent("");
        chapter.setWordCount(0);
        chapter.setStatus("draft");
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());

        chapterMapper.insert(chapter);

        log.info("创建章节成功，规划ID: {}, 章节ID: {}, 全局序号: {}", chapterPlan.getId(), chapter.getId(), globalChapterNumber);

        return chapter;
    }

    /**
     * 计算全局章节序号
     * 全局序号 = 前面所有卷的章节数 + 当前卷内序号
     *
     * @param chapterPlan 章节规划
     * @return 全局章节序号
     */
    private int calculateGlobalChapterNumber(NovelChapterPlan chapterPlan) {
        // 获取当前卷信息
        NovelVolumePlan currentVolume = volumePlanMapper.selectById(chapterPlan.getVolumePlanId());
        if (currentVolume == null) {
            log.warn("未找到卷规划，使用卷内序号作为全局序号, volumePlanId: {}", chapterPlan.getVolumePlanId());
            return chapterPlan.getChapterNumber();
        }

        // 查询当前卷之前的所有卷（volumeNumber < 当前卷的volumeNumber）
        List<NovelVolumePlan> previousVolumes = volumePlanMapper.selectList(
            new LambdaQueryWrapper<NovelVolumePlan>()
                .eq(NovelVolumePlan::getProjectId, chapterPlan.getProjectId())
                .lt(NovelVolumePlan::getVolumeNumber, currentVolume.getVolumeNumber())
        );

        // 计算前面所有卷的章节总数
        int previousChapterCount = 0;
        for (NovelVolumePlan volume : previousVolumes) {
            Long count = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getVolumePlanId, volume.getId())
            );
            previousChapterCount += (count != null ? count.intValue() : 0);
        }

        // 全局序号 = 前面卷的章节总数 + 当前卷内序号
        return previousChapterCount + chapterPlan.getChapterNumber();
    }

    /**
     * 追加保存章节内容（流式生成时调用）
     *
     * @param chapterId 章节ID
     * @param content 新增内容
     * @param totalWordCount 当前总字数
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendContent(Long chapterId, String content, int totalWordCount) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在: " + chapterId);
        }

        chapter.setContent(content);
        chapter.setWordCount(totalWordCount);
        chapter.setUpdateTime(LocalDateTime.now());

        chapterMapper.updateById(chapter);

        log.debug("追加保存章节内容，章节ID: {}, 当前字数: {}", chapterId, totalWordCount);
    }

    /**
     * 完成章节生成（更新概要、最终字数）
     *
     * @param chapterId 章节ID
     * @param plotSummary 剧情概要
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeChapter(Long chapterId, String plotSummary) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在: " + chapterId);
        }

        chapter.setPlotSummary(plotSummary);
        chapter.setUpdateTime(LocalDateTime.now());

        chapterMapper.updateById(chapter);

        log.info("章节生成完成，章节ID: {}, 字数: {}", chapterId, chapter.getWordCount());
    }

    /**
     * 更新章节标题
     *
     * @param chapterId 章节ID
     * @param title 新标题
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTitle(Long chapterId, String title) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setTitle(title);
        chapter.setUpdateTime(LocalDateTime.now());

        chapterMapper.updateById(chapter);

        log.debug("更新章节标题，章节ID: {}", chapterId);
    }

    /**
     * 根据规划ID查找章节
     *
     * @param planId 章节规划ID
     * @return 章节实体，不存在则返回null
     */
    public Chapter findByPlanId(Long planId) {
        return chapterMapper.selectOne(
            new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getChapterPlanId, planId)
                .last("LIMIT 1")
        );
    }

    /**
     * 根据项目ID和章节规划ID查找章节
     *
     * @param projectId 项目ID
     * @param planId 章节规划ID
     * @return 章节实体，不存在则返回null
     */
    public Chapter findByProjectIdAndPlanId(Long projectId, Long planId) {
        return chapterMapper.selectOne(
            new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getChapterPlanId, planId)
                .last("LIMIT 1")
        );
    }

    /**
     * 检查章节是否已生成
     *
     * @param planId 章节规划ID
     * @return true-已生成，false-未生成
     */
    public boolean isChapterGenerated(Long planId) {
        Long count = chapterMapper.selectCount(
            new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getChapterPlanId, planId)
        );

        return count != null && count > 0;
    }

    /**
     * 获取章节详情
     *
     * @param chapterId 章节ID
     * @return 章节实体
     */
    public Chapter getById(Long chapterId) {
        return chapterMapper.selectById(chapterId);
    }

    /**
     * 删除章节
     *
     * @param chapterId 章节ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long chapterId) {
        chapterMapper.deleteById(chapterId);
        log.info("删除章节，章节ID: {}", chapterId);
    }
}
