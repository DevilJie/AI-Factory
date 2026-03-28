package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * AI服务提供商配置实体
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("t_ai_provider")
public class AiProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 服务商类型: llm/image/tts/video
     */
    private String providerType;

    /**
     * 服务商代码: openai/anthropic/midjourney等
     */
    private String providerCode;

    /**
     * 服务商名称
     */
    private String providerName;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API端点
     */
    private String apiEndpoint;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 是否默认
     */
    private Integer isDefault;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 配置JSON（存储额外配置参数）
     */
    private String configJson;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;
}
