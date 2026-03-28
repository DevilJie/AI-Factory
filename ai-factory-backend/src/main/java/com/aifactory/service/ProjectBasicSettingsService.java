package com.aifactory.service;

import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.vo.SetupStatusVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 项目基础设置Service
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
public interface ProjectBasicSettingsService extends IService<ProjectBasicSettings> {

    /**
     * 保存或更新项目基础设置
     *
     * @param settings 设置数据
     */
    void saveOrUpdateSettings(ProjectBasicSettings settings);

    /**
     * 根据项目ID获取基础设置
     *
     * @param projectId 项目ID
     * @return 基础设置
     */
    ProjectBasicSettings getByProjectId(Long projectId);

    /**
     * 检查基础设置是否完成
     *
     * @param projectId 项目ID
     * @return 设置状态
     */
    SetupStatusVO checkSetupStatus(Long projectId);
}
