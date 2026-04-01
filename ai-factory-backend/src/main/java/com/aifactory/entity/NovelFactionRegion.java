package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_faction_region")
public class NovelFactionRegion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factionId;

    private Long regionId;
}
