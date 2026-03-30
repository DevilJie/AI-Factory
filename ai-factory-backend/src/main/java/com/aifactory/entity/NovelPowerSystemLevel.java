package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("novel_power_system_level")
public class NovelPowerSystemLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long powerSystemId;
    private Integer level;
    private String levelName;
    private String description;
    private String breakthroughCondition;
    private String lifespan;
    private String powerRange;
    private String landmarkAbility;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<NovelPowerSystemLevelStep> steps;
}