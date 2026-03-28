package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 世界观设定实体
 *
 * @Author CaiZy
 * @Date 2025-01-27
 */
@Data
@TableName("novel_worldview")
public class NovelWorldview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 关联大纲ID
     */
    private Long outlineId;

    /**
     * 世界类型：架空/现代/古代/未来/玄幻等
     */
    private String worldType;

    /**
     * 世界背景描述
     */
    private String worldBackground;

    /**
     * 力量体系/修炼体系
     */
    private String powerSystem;

    /**
     * 地理环境
     */
    private String geography;

    /**
     * 势力分布
     */
    private String forces;

    /**
     * 时间线设定
     */
    private String timeline;

    /**
     * 世界规则
     */
    private String rules;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
