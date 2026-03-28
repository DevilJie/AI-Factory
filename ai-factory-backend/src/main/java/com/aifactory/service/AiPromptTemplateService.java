package com.aifactory.service;

import com.aifactory.entity.AiPromptTemplate;
import com.aifactory.entity.AiPromptTemplateContent;

/**
 * AI提示词模板Service
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
public interface AiPromptTemplateService {

    /**
     * 根据模板编码获取模板内容
     *
     * @param templateCode 模板编码
     * @return 模板内容
     */
    String getTemplateContent(String templateCode);

    /**
     * 保存模板内容
     *
     * @param templateCode 模板编码
     * @param version 版本号
     * @param content 内容
     * @return 模板内容ID
     */
    Long saveTemplateContent(String templateCode, String version, String content);

    /**
     * 初始化内置模板
     */
    void initBuiltinTemplates();
}
