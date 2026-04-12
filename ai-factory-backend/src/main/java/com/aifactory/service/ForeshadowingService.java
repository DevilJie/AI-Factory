package com.aifactory.service;

import com.aifactory.dto.ForeshadowingCreateDto;
import com.aifactory.dto.ForeshadowingDto;
import com.aifactory.dto.ForeshadowingQueryDto;
import com.aifactory.dto.ForeshadowingUpdateDto;
import com.aifactory.entity.ChapterPlotMemory;
import com.aifactory.entity.Foreshadowing;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.mapper.ChapterPlotMemoryMapper;
import com.aifactory.mapper.ForeshadowingMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 伏笔服务
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@Service
public class ForeshadowingService {

    @Autowired
    private ForeshadowingMapper foreshadowingMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private ChapterPlotMemoryMapper plotMemoryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int MIN_FORESHADOWING_DISTANCE = 3;

    /**
     * 校验伏笔回收距离（明线至少间隔3章，暗线豁免）
     * Per D-06, D-07, D-08
     */
    private void validateForeshadowingDistance(Integer plantedChapter, Integer plannedCallbackChapter, String layoutType) {
        // D-07: dark lines exempt
        if ("dark".equals(layoutType)) {
            return;
        }
        // Only validate when both chapters are specified
        if (plantedChapter == null || plannedCallbackChapter == null) {
            return;
        }
        // D-04: global sequence, simple subtraction
        int distance = plannedCallbackChapter - plantedChapter;
        if (distance < MIN_FORESHADOWING_DISTANCE) {
            throw new RuntimeException(
                "明线伏笔回收距离不足：埋设章节(" + plantedChapter
                + ")与回收章节(" + plannedCallbackChapter
                + ")至少需间隔" + MIN_FORESHADOWING_DISTANCE + "章"
            );
        }
    }

    /**
     * 校验回收章节不超过该卷规划章节数
     * Per D-03: plannedCallbackChapter cannot exceed volume's targetChapterCount
     */
    private void validateCallbackChapterBounds(Long projectId, Integer plannedCallbackVolume, Integer plannedCallbackChapter) {
        if (plannedCallbackVolume == null || plannedCallbackChapter == null) {
            return;
        }
        LambdaQueryWrapper<NovelVolumePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelVolumePlan::getProjectId, projectId)
               .eq(NovelVolumePlan::getVolumeNumber, plannedCallbackVolume);
        NovelVolumePlan volumePlan = volumePlanMapper.selectOne(wrapper);
        // If volume plan not found, skip validation (volume not yet planned)
        if (volumePlan != null && volumePlan.getTargetChapterCount() != null) {
            if (plannedCallbackChapter > volumePlan.getTargetChapterCount()) {
                throw new RuntimeException(
                    "回收章节超出范围：第" + plannedCallbackVolume + "卷规划"
                    + volumePlan.getTargetChapterCount() + "章，无法设置回收章节为"
                    + plannedCallbackChapter
                );
            }
        }
    }

    /**
     * 获取伏笔列表
     */
    public List<ForeshadowingDto> getForeshadowingList(ForeshadowingQueryDto queryDto) {
        LambdaQueryWrapper<Foreshadowing> queryWrapper = new LambdaQueryWrapper<>();

        // 按项目ID过滤
        if (queryDto.getProjectId() != null) {
            queryWrapper.eq(Foreshadowing::getProjectId, queryDto.getProjectId());
        }

        // 按类型过滤
        if (queryDto.getType() != null && !queryDto.getType().isEmpty()) {
            queryWrapper.eq(Foreshadowing::getType, queryDto.getType());
        }

        // 按布局类型过滤
        if (queryDto.getLayoutType() != null && !queryDto.getLayoutType().isEmpty()) {
            queryWrapper.eq(Foreshadowing::getLayoutType, queryDto.getLayoutType());
        }

        // 按状态过滤
        if (queryDto.getStatus() != null && !queryDto.getStatus().isEmpty()) {
            queryWrapper.eq(Foreshadowing::getStatus, queryDto.getStatus());
        }

        // 查询需要在该章节填坑的伏笔（用于编辑提醒）
        if (queryDto.getCurrentChapter() != null) {
            Integer currentChapter = queryDto.getCurrentChapter();
            queryWrapper.and(wrapper -> wrapper
                    // 计划填坑章节等于当前章节
                    .eq(Foreshadowing::getPlannedCallbackChapter, currentChapter)
                    // 或计划填坑章节已过期但仍未填回
                    .or(w -> w.lt(Foreshadowing::getPlannedCallbackChapter, currentChapter)
                            .in(Foreshadowing::getStatus, "pending", "in_progress"))
            );
        }

        // 按埋设分卷筛选
        if (queryDto.getPlantedVolume() != null) {
            queryWrapper.eq(Foreshadowing::getPlantedVolume, queryDto.getPlantedVolume());
        }

        // 按回收分卷筛选
        if (queryDto.getPlannedCallbackVolume() != null) {
            queryWrapper.eq(Foreshadowing::getPlannedCallbackVolume, queryDto.getPlannedCallbackVolume());
        }

        // 按埋设章节筛选
        if (queryDto.getPlantedChapter() != null) {
            queryWrapper.eq(Foreshadowing::getPlantedChapter, queryDto.getPlantedChapter());
        }

        // 按计划回收章节筛选
        if (queryDto.getPlannedCallbackChapter() != null) {
            queryWrapper.eq(Foreshadowing::getPlannedCallbackChapter, queryDto.getPlannedCallbackChapter());
        }

        // 按埋设分卷、埋设章节升序排序
        queryWrapper.orderByAsc(Foreshadowing::getPlantedVolume)
                .orderByAsc(Foreshadowing::getPlantedChapter)
                .orderByAsc(Foreshadowing::getCreateTime);

        List<Foreshadowing> foreshadowings = foreshadowingMapper.selectList(queryWrapper);
        return foreshadowings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取伏笔详情
     */
    public Foreshadowing getForeshadowingDetail(Long foreshadowingId) {
        return foreshadowingMapper.selectById(foreshadowingId);
    }

    /**
     * 创建伏笔
     */
    @Transactional
    public Long createForeshadowing(Long projectId, ForeshadowingCreateDto createDto) {
        // Per D-06, D-07: validate foreshadowing distance
        validateForeshadowingDistance(createDto.getPlantedChapter(), createDto.getPlannedCallbackChapter(), createDto.getLayoutType());
        // Per D-03: validate callback chapter bounds
        validateCallbackChapterBounds(projectId, createDto.getPlannedCallbackVolume(), createDto.getPlannedCallbackChapter());

        Foreshadowing foreshadowing = new Foreshadowing();
        BeanUtils.copyProperties(createDto, foreshadowing);

        foreshadowing.setProjectId(projectId);
        foreshadowing.setStatus("pending"); // 默认状态为未填回
        if (foreshadowing.getPriority() == null) {
            foreshadowing.setPriority(0); // 默认优先级为0
        }
        foreshadowing.setCreateTime(LocalDateTime.now());
        foreshadowing.setUpdateTime(LocalDateTime.now());

        foreshadowingMapper.insert(foreshadowing);
        log.info("创建伏笔成功，projectId={}, title={}", projectId, createDto.getTitle());

        return foreshadowing.getId();
    }

    /**
     * 更新伏笔
     */
    @Transactional
    public void updateForeshadowing(Long foreshadowingId, ForeshadowingUpdateDto updateDto) {
        Foreshadowing existingForeshadowing = foreshadowingMapper.selectById(foreshadowingId);
        if (existingForeshadowing == null) {
            throw new RuntimeException("伏笔不存在");
        }

        Foreshadowing foreshadowing = new Foreshadowing();
        BeanUtils.copyProperties(updateDto, foreshadowing);

        foreshadowing.setId(foreshadowingId);
        foreshadowing.setProjectId(existingForeshadowing.getProjectId());
        foreshadowing.setPlantedChapter(existingForeshadowing.getPlantedChapter());
        foreshadowing.setUpdateTime(LocalDateTime.now());

        // Per D-06, D-07: validate foreshadowing distance for update
        // plantedChapter comes from existing (immutable), callbackChapter from updateDto
        Integer effectiveCallbackChapter = foreshadowing.getPlannedCallbackChapter();
        String effectiveLayoutType = foreshadowing.getLayoutType();
        Integer effectiveCallbackVolume = updateDto.getPlannedCallbackVolume();
        validateForeshadowingDistance(foreshadowing.getPlantedChapter(), effectiveCallbackChapter, effectiveLayoutType);
        validateCallbackChapterBounds(existingForeshadowing.getProjectId(), effectiveCallbackVolume, effectiveCallbackChapter);

        foreshadowingMapper.updateById(foreshadowing);
        log.info("更新伏笔成功，foreshadowingId={}", foreshadowingId);
    }

    /**
     * 删除伏笔
     */
    @Transactional
    public void deleteForeshadowing(Long foreshadowingId) {
        foreshadowingMapper.deleteById(foreshadowingId);
        log.info("删除伏笔成功，foreshadowingId={}", foreshadowingId);
    }

    /**
     * 删除指定分卷的pending状态伏笔（重新规划时先删后建）
     * Per D-06: 仅删除 plantedVolume 匹配当前卷且 status=pending 的伏笔
     */
    public int deletePendingForeshadowingForVolume(Long projectId, int volumeNumber) {
        LambdaQueryWrapper<Foreshadowing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Foreshadowing::getProjectId, projectId)
               .eq(Foreshadowing::getPlantedVolume, volumeNumber)
               .eq(Foreshadowing::getStatus, "pending");
        int deleted = foreshadowingMapper.delete(wrapper);
        log.info("删除第{}卷pending伏笔 {} 个, projectId={}", volumeNumber, deleted, projectId);
        return deleted;
    }

    /**
     * 标记伏笔为已填回
     */
    @Transactional
    public void markAsCompleted(Long foreshadowingId, Integer actualCallbackChapter) {
        Foreshadowing foreshadowing = foreshadowingMapper.selectById(foreshadowingId);
        if (foreshadowing == null) {
            throw new RuntimeException("伏笔不存在");
        }

        foreshadowing.setStatus("completed");
        foreshadowing.setActualCallbackChapter(actualCallbackChapter);
        foreshadowing.setUpdateTime(LocalDateTime.now());

        foreshadowingMapper.updateById(foreshadowing);
        log.info("标记伏笔为已填回，foreshadowingId={}, actualCallbackChapter={}",
                foreshadowingId, actualCallbackChapter);
    }

    /**
     * 获取填坑统计数据
     */
    public ForeshadowingStats getForeshadowingStats(Long projectId) {
        // 查询所有伏笔
        LambdaQueryWrapper<Foreshadowing> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Foreshadowing::getProjectId, projectId);

        List<Foreshadowing> allForeshadowings = foreshadowingMapper.selectList(queryWrapper);

        // 统计各状态的伏笔数量
        long pendingCount = allForeshadowings.stream()
                .filter(f -> "pending".equals(f.getStatus()))
                .count();
        long inProgressCount = allForeshadowings.stream()
                .filter(f -> "in_progress".equals(f.getStatus()))
                .count();
        long completedCount = allForeshadowings.stream()
                .filter(f -> "completed".equals(f.getStatus()))
                .count();

        // 计算完成率
        int totalCount = allForeshadowings.size();
        double completionRate = totalCount > 0
                ? (double) completedCount / totalCount * 100
                : 0.0;

        return new ForeshadowingStats(totalCount, (int) pendingCount, (int) inProgressCount,
                (int) completedCount, completionRate);
    }

    /**
     * 转换为DTO
     */
    private ForeshadowingDto convertToDto(Foreshadowing foreshadowing) {
        if (foreshadowing == null) {
            return null;
        }

        ForeshadowingDto dto = new ForeshadowingDto();
        BeanUtils.copyProperties(foreshadowing, dto);

        return dto;
    }

    /**
     * 伏笔统计信息
     */
    public record ForeshadowingStats(
            int totalCount,           // 总伏笔数
            int pendingCount,         // 未填回数量
            int inProgressCount,      // 进行中数量
            int completedCount,       // 已填回数量
            double completionRate     // 完成率
    ) {
    }

    /**
     * 批量更新伏笔状态（章节生成成功后调用）
     * Per D-04: pending -> in_progress for planted, in_progress -> completed for resolved
     * Per D-05: re-generation does NOT roll back status
     *
     * @param projectId 项目ID
     * @param chapterNumber 当前章节全局序号
     * @param volumeNumber 当前分卷号（用于精确匹配，避免跨卷误更新）
     * @return 更新的记录数
     */
    @Transactional
    public int batchUpdateStatusForChapter(Long projectId, int chapterNumber, int volumeNumber) {
        int updated = 0;

        // pending -> in_progress (planted this chapter)
        LambdaUpdateWrapper<Foreshadowing> plantWrapper = new LambdaUpdateWrapper<>();
        plantWrapper.eq(Foreshadowing::getProjectId, projectId)
                    .eq(Foreshadowing::getPlantedVolume, volumeNumber)
                    .eq(Foreshadowing::getPlantedChapter, chapterNumber)
                    .eq(Foreshadowing::getStatus, "pending")
                    .set(Foreshadowing::getStatus, "in_progress")
                    .set(Foreshadowing::getUpdateTime, LocalDateTime.now());
        updated += foreshadowingMapper.update(null, plantWrapper);

        // in_progress -> completed (resolved this chapter)
        LambdaUpdateWrapper<Foreshadowing> resolveWrapper = new LambdaUpdateWrapper<>();
        resolveWrapper.eq(Foreshadowing::getProjectId, projectId)
                      .eq(Foreshadowing::getPlannedCallbackVolume, volumeNumber)
                      .eq(Foreshadowing::getPlannedCallbackChapter, chapterNumber)
                      .eq(Foreshadowing::getStatus, "in_progress")
                      .set(Foreshadowing::getStatus, "completed")
                      .set(Foreshadowing::getActualCallbackChapter, chapterNumber)
                      .set(Foreshadowing::getUpdateTime, LocalDateTime.now());
        updated += foreshadowingMapper.update(null, resolveWrapper);

        log.info("批量更新伏笔状态，projectId={}, volumeNumber={}, chapterNumber={}, updatedCount={}",
                 projectId, volumeNumber, chapterNumber, updated);
        return updated;
    }

    // ==================== 章节记忆伏笔管理 ====================

    /**
     * 从章节记忆中获取待回收伏笔列表
     * 累积前面所有章节的待回收伏笔，排除已回收的
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节号（不包含当前章节）
     * @return 待回收伏笔列表
     */
    public List<String> getPendingForeshadowingFromMemories(Long projectId, Integer currentChapterNumber) {
        log.debug("获取待回收伏笔，projectId={}, currentChapterNumber={}", projectId, currentChapterNumber);

        Set<String> plantedForeshadowing = new HashSet<>();
        Set<String> resolvedForeshadowing = new HashSet<>();

        // 查询当前章节之前的所有记忆
        List<ChapterPlotMemory> memories = plotMemoryMapper.selectRecentMemories(
                projectId, currentChapterNumber, currentChapterNumber - 1
        );

        if (memories == null || memories.isEmpty()) {
            log.debug("没有找到章节记忆，返回空伏笔列表");
            return new ArrayList<>();
        }

        // 遍历所有记忆，累积埋设和回收的伏笔
        for (ChapterPlotMemory memory : memories) {
            // 收集埋设的伏笔
            if (memory.getForeshadowingPlanted() != null && !memory.getForeshadowingPlanted().isEmpty()) {
                try {
                    List<String> planted = objectMapper.readValue(
                            memory.getForeshadowingPlanted(),
                            new TypeReference<List<String>>() {}
                    );
                    // 过滤掉"无明显新伏笔"等占位符
                    planted.stream()
                            .filter(f -> f != null && !f.isEmpty()
                                    && !f.contains("无明显新伏笔")
                                    && !f.contains("无新伏笔"))
                            .forEach(plantedForeshadowing::add);
                } catch (Exception e) {
                    log.warn("解析埋设伏笔失败，chapterId={}", memory.getChapterId(), e);
                }
            }

            // 收集回收的伏笔
            if (memory.getForeshadowingResolved() != null && !memory.getForeshadowingResolved().isEmpty()) {
                try {
                    List<String> resolved = objectMapper.readValue(
                            memory.getForeshadowingResolved(),
                            new TypeReference<List<String>>() {}
                    );
                    // 过滤掉"无回收伏笔"等占位符
                    resolved.stream()
                            .filter(f -> f != null && !f.isEmpty()
                                    && !f.contains("无回收伏笔")
                                    && !f.contains("未回收"))
                            .forEach(resolvedForeshadowing::add);
                } catch (Exception e) {
                    log.warn("解析回收伏笔失败，chapterId={}", memory.getChapterId(), e);
                }
            }
        }

        // 计算待回收伏笔 = 埋设的 - 已回收的
        Set<String> pending = new HashSet<>(plantedForeshadowing);
        pending.removeAll(resolvedForeshadowing);

        List<String> result = new ArrayList<>(pending);
        log.debug("待回收伏笔数量: {}", result.size());

        return result;
    }

    /**
     * 获取最近一章的记忆中存储的待回收伏笔
     * 这个是记忆表中已经计算好的待回收伏笔列表
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节号
     * @return 待回收伏笔列表
     */
    public List<String> getLatestPendingForeshadowing(Long projectId, Integer currentChapterNumber) {
        log.debug("获取最新记忆中的待回收伏笔，projectId={}, currentChapterNumber={}", projectId, currentChapterNumber);

        if (currentChapterNumber <= 1) {
            return new ArrayList<>();
        }

        // 获取上一章的记忆
        List<ChapterPlotMemory> memories = plotMemoryMapper.selectRecentMemories(
                projectId, currentChapterNumber, 1
        );

        if (memories == null || memories.isEmpty()) {
            return new ArrayList<>();
        }

        ChapterPlotMemory latestMemory = memories.get(0);
        if (latestMemory.getPendingForeshadowing() != null && !latestMemory.getPendingForeshadowing().isEmpty()) {
            try {
                return objectMapper.readValue(
                        latestMemory.getPendingForeshadowing(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                log.warn("解析待回收伏笔失败，chapterId={}", latestMemory.getChapterId(), e);
            }
        }

        return new ArrayList<>();
    }

    /**
     * 构建待回收伏笔的文本描述（用于提示词）
     *
     * @param pendingForeshadowing 待回收伏笔列表
     * @return 格式化的文本描述
     */
    public String buildPendingForeshadowingText(List<String> pendingForeshadowing) {
        if (pendingForeshadowing == null || pendingForeshadowing.isEmpty()) {
            return "暂无待回收伏笔";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【待回收伏笔提醒】\n");
        sb.append("以下是前文埋下的伏笔，请根据剧情需要考虑回收：\n\n");

        for (int i = 0; i < pendingForeshadowing.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, pendingForeshadowing.get(i)));
        }

        sb.append("\n**注意**：不需要在当前章节强制回收所有伏笔，请根据剧情节奏自然安排。\n");

        return sb.toString();
    }
}
