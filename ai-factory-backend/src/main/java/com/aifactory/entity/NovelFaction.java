package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("novel_faction")
public class NovelFaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private Integer deep;

    private Integer sortOrder;

    private Long projectId;

    private String name;

    /** 势力类型（ally/hostile/neutral，仅顶级设置） */
    private String type;

    /** 核心力量体系ID（仅顶级设置，下级继承） */
    private Long corePowerSystem;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 子势力（非数据库字段） */
    @TableField(exist = false)
    private List<NovelFaction> children;
}
