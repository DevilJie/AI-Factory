package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_faction_character")
public class NovelFactionCharacter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factionId;

    private Long characterId;

    /** 职位（如掌门、长老、弟子，自由文本 per D-07） */
    private String role;
}
