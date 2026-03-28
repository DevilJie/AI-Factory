package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI提示词模板实体
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码（唯一，语义化，如：chapter_generate_standard）
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 服务类型：llm/image/video/common
     */
    private String serviceType;

    /**
     * 使用场景：chapter_generate/outline_generate/worldview_generate等
     */
    private String scenario;

    /**
     * 当前激活版本ID（关联ai_prompt_template_version表）
     */
    private Long currentVersionId;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 标签，逗号分隔：小说,章节,生成
     */
    private String tags;

    /**
     * 是否启用：1启用 0禁用
     */
    private Boolean isActive;

    /**
     * 是否系统内置：1是 0否
     */
    private Boolean isSystem;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
