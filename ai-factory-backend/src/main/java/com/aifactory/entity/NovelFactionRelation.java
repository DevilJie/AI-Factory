package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_faction_relation")
public class NovelFactionRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factionId;

    private Long targetFactionId;

    /** 关系类型（ally/hostile/neutral，双向存储 per D-06） */
    private String relationType;

    /** 关系描述 */
    private String description;
}
