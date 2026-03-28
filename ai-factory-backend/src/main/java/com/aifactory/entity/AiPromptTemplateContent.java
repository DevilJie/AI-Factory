package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词模板内容实体
 * 存储模板的具体内容
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@TableName("ai_prompt_template_content")
public class AiPromptTemplateContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID（关联ai_prompt_template表）
     */
    private Long templateId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 变量定义（JSON）
     * 示例：{"variables": [{"name": "projectName", "description": "项目名称", "type": "string"}]}
     */
    private String variables;

    /**
     * 示例数据（JSON）
     */
    private String exampleData;

    /**
     * 是否激活：1激活 0禁用
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
