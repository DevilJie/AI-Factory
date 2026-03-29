package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("novel_power_system")
public class NovelPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String sourceFrom;
    private String coreResource;
    private String cultivationMethod;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}