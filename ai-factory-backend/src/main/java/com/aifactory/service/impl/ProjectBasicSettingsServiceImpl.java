package com.aifactory.service.impl;

import com.aifactory.entity.Project;
import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.mapper.ProjectBasicSettingsMapper;
import com.aifactory.service.ProjectBasicSettingsService;
import com.aifactory.service.ProjectService;
import com.aifactory.vo.SetupStatusVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目基础设置Service实现
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectBasicSettingsServiceImpl
        extends ServiceImpl<ProjectBasicSettingsMapper, ProjectBasicSettings>
        implements ProjectBasicSettingsService {

    private final ProjectService projectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateSettings(ProjectBasicSettings settings) {
        // 1. 验证项目状态
        Project project = projectService.getById(settings.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 检查是否已完成世界观设置
        if (!"worldview_configured".equals(project.getSetupStage()) &&
            !"basic_settings_configured".equals(project.getSetupStage()) &&
            !"volume_generation_started".equals(project.getSetupStage())) {
            throw new RuntimeException("请先完成世界观设置");
        }

        // 2. 保存或更新设置
        ProjectBasicSettings existing = getByProjectId(settings.getProjectId());
        if (existing == null) {
            // 新增
            settings.setUserId(project.getUserId());
            save(settings);
        } else {
            // 更新
            settings.setId(existing.getId());
            updateById(settings);
        }

        // 3. 更新项目状态（如果还未完成基础设置）
        if ("worldview_configured".equals(project.getSetupStage())) {
            project.setSetupStage("basic_settings_configured");
            projectService.updateById(project);
            log.info("项目 {} 基础设置完成，状态更新为 basic_settings_configured", settings.getProjectId());
        }
    }

    @Override
    public ProjectBasicSettings getByProjectId(Long projectId) {
        LambdaQueryWrapper<ProjectBasicSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectBasicSettings::getProjectId, projectId);
        return getOne(wrapper);
    }

    @Override
    public SetupStatusVO checkSetupStatus(Long projectId) {
        Project project = projectService.getById(projectId);
        ProjectBasicSettings settings = getByProjectId(projectId);

        return SetupStatusVO.builder()
                .setupStage(project.getSetupStage())
                .canAccessCreationCenter(
                    "basic_settings_configured".equals(project.getSetupStage()) ||
                    "volume_generation_started".equals(project.getSetupStage()) ||
                    "in_progress".equals(project.getSetupStage())
                )
                .worldviewLocked("volume_generation_started".equals(project.getSetupStage()))
                .basicSettingsLocked("volume_generation_started".equals(project.getSetupStage()))
                .project(project)
                .build();
    }
}
