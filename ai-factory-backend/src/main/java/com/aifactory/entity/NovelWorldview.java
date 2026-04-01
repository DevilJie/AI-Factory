package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("novel_worldview")
public class NovelWorldview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long projectId;
    private Long outlineId;
    private String worldType;
    private String worldBackground;
    /**
     * 势力分布（非数据库字段，由 FactionService 从 novel_faction 表构建）
     */
    @TableField(exist = false)
    private String forces;
    /** 时间线设定 */
    private String timeline;
    /** 世界规则 */
    private String rules;
    /** 主角ID列表 (JSON) */
    private String protagonistIds;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 地理环境文本（非数据库字段，由 ContinentRegionService 从 novel_continent_region 表构建）
     */
    @TableField(exist = false)
    private String geography;
}
