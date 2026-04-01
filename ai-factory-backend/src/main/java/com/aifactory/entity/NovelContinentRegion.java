package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("novel_continent_region")
public class NovelContinentRegion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private Integer deep;

    private Integer sortOrder;

    private Long projectId;

    private String name;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 子区域（非数据库字段） */
    @TableField(exist = false)
    private List<NovelContinentRegion> children;
}
