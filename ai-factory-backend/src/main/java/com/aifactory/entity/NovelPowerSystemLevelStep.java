package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("novel_power_system_level_step")
public class NovelPowerSystemLevelStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long powerSystemLevelId;
    private Integer level;
    private String levelName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}