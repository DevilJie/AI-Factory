package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_worldview_power_system")
public class NovelWorldviewPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long worldviewId;
    private Long powerSystemId;
}