package com.aifactory.service.impl;

import com.aifactory.dto.UpdateVolumeRequest;
import com.aifactory.dto.VolumePlanDto;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.service.VolumeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

/**
 * 分卷服务实现
 *
 * @Author CaiZy
 * @Date 2025-02-08
 */
@Slf4j
@Service
public class VolumeServiceImpl implements VolumeService {

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VolumePlanDto updateVolume(Long projectId, String volumeId, UpdateVolumeRequest request) {
        log.info("更新分卷规划: projectId={}, volumeId={}", projectId, volumeId);

        // 查询分卷是否存在
        LambdaQueryWrapper<NovelVolumePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelVolumePlan::getProjectId, projectId)
                .eq(NovelVolumePlan::getId, Long.valueOf(volumeId));

        NovelVolumePlan volumePlan = volumePlanMapper.selectOne(queryWrapper);
        if (volumePlan == null) {
            throw new RuntimeException("分卷不存在");
        }

        // 更新非空字段
        if (StringUtils.hasText(request.getVolumeTitle())) {
            volumePlan.setVolumeTitle(request.getVolumeTitle());
        }
        if (StringUtils.hasText(request.getVolumeTheme())) {
            volumePlan.setVolumeTheme(request.getVolumeTheme());
        }
        if (StringUtils.hasText(request.getMainConflict())) {
            volumePlan.setMainConflict(request.getMainConflict());
        }
        if (StringUtils.hasText(request.getPlotArc())) {
            volumePlan.setPlotArc(request.getPlotArc());
        }
        if (StringUtils.hasText(request.getVolumeDescription())) {
            volumePlan.setVolumeDescription(request.getVolumeDescription());
        }
        if (StringUtils.hasText(request.getKeyEvents())) {
            volumePlan.setKeyEvents(request.getKeyEvents());
        }
        if (StringUtils.hasText(request.getTimelineSetting())) {
            volumePlan.setTimelineSetting(request.getTimelineSetting());
        }
        if (request.getTargetChapterCount() != null) {
            volumePlan.setTargetChapterCount(request.getTargetChapterCount());
        }
        if (StringUtils.hasText(request.getVolumeNotes())) {
            volumePlan.setVolumeNotes(request.getVolumeNotes());
        }
        if (StringUtils.hasText(request.getCoreGoal())) {
            volumePlan.setCoreGoal(request.getCoreGoal());
        }
        if (StringUtils.hasText(request.getClimax())) {
            volumePlan.setClimax(request.getClimax());
        }
        if (StringUtils.hasText(request.getEnding())) {
            volumePlan.setEnding(request.getEnding());
        }
        if (request.getNewCharacters() != null && !request.getNewCharacters().isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(request.getNewCharacters());
                volumePlan.setNewCharacters(json);
            } catch (Exception e) {
                log.error("转换newCharacters为JSON失败", e);
            }
        }
        if (request.getStageForeshadowings() != null && !request.getStageForeshadowings().isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(request.getStageForeshadowings());
                volumePlan.setStageForeshadowings(json);
            } catch (Exception e) {
                log.error("转换stageForeshadowings为JSON失败", e);
            }
        }
        if (StringUtils.hasText(request.getStatus())) {
            volumePlan.setStatus(request.getStatus());
        }
        if (request.getVolumeCompleted() != null) {
            volumePlan.setVolumeCompleted(request.getVolumeCompleted());
        }

        // 保存更新
        volumePlanMapper.updateById(volumePlan);
        log.info("分卷规划更新成功: volumeId={}", volumeId);

        // 返回更新后的数据
        return convertToDto(volumePlan);
    }

    private VolumePlanDto convertToDto(NovelVolumePlan entity) {
        VolumePlanDto dto = new VolumePlanDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
