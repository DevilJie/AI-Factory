package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI提示词模板版本实体
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@TableName("ai_prompt_template_version")
public class AiPromptTemplateVersion {

    /**
     * 版本ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID（关联ai_prompt_template.id）
     */
    private Long templateId;

    /**
     * 版本号：1,2,3...
     */
    private Integer versionNumber;

    /**
     * 模板内容（支持{var}占位符）
     * 例如：你好{username}，您的验证码为{code}
     */
    private String templateContent;

    /**
     * 变量定义JSON
     * 例如：[{"name":"worldview","type":"string","desc":"世界观设定","required":true}]
     */
    private String variableDefinitions;

    /**
     * 版本说明
     */
    private String versionComment;

    /**
     * 是否激活：1是 0否（每个模板同时只能有一个激活版本）
     */
    private Boolean isActive;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
