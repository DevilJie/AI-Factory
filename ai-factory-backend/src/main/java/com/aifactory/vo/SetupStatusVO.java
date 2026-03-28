package com.aifactory.vo;

import com.aifactory.entity.Project;
import lombok.Data;
import lombok.Builder;

/**
 * 项目设置状态VO
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@Builder
public class SetupStatusVO {

    /**
     * 设置阶段
     */
    private String setupStage;

    /**
     * 是否可以访问创作中心
     */
    private Boolean canAccessCreationCenter;

    /**
     * 世界观是否锁定
     */
    private Boolean worldviewLocked;

    /**
     * 基础设置是否锁定
     */
    private Boolean basicSettingsLocked;

    /**
     * 项目信息
     */
    private Project project;
}
