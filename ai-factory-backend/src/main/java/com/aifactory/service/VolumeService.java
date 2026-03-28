package com.aifactory.service;

import com.aifactory.dto.UpdateVolumeRequest;
import com.aifactory.dto.VolumePlanDto;

/**
 * 分卷服务接口
 *
 * @Author CaiZy
 * @Date 2025-02-08
 */
public interface VolumeService {

    /**
     * 更新分卷规划
     *
     * @param projectId 项目ID
     * @param volumeId 分卷ID
     * @param request 更新请求
     * @return 更新后的分卷信息
     */
    VolumePlanDto updateVolume(Long projectId, String volumeId, UpdateVolumeRequest request);
}
