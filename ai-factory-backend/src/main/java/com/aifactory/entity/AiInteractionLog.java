package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI大模型交互日志实体
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Data
@TableName("ai_interaction_log")
public class AiInteractionLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交互流水号（用于关联请求和响应）
     */
    private String traceId;

    /**
     * 项目ID（如果关联具体项目）
     */
    private Long projectId;

    /**
     * 分卷计划ID
     */
    private Long volumePlanId;

    /**
     * 章节规划ID
     */
    private Long chapterPlanId;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 请求类型（chapter_generate/outline_generate/worldview_generate/character_fix等）
     */
    private String requestType;

    /**
     * AI提供商（zhipu/openai/anthropic等）
     */
    private String provider;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 请求提示词
     */
    private String requestPrompt;

    /**
     * 请求参数（temperature/maxTokens等，JSON格式）
     */
    private String requestParams;

    /**
     * 响应内容
     */
    private String responseContent;

    /**
     * 响应token数
     */
    private Integer responseTokens;

    /**
     * 响应耗时（毫秒）
     */
    private Long responseDuration;

    /**
     * 是否成功（1成功 0失败）
     */
    private Boolean isSuccess;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
