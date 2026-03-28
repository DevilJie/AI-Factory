package com.aifactory.service.prompt;

import com.aifactory.dto.PromptTemplateCreateRequest;
import com.aifactory.dto.PromptTemplateDto;
import com.aifactory.dto.PromptTemplateUpdateRequest;
import com.aifactory.entity.AiPromptTemplateVersion;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务接口
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
public interface PromptTemplateService {

    /**
     * 根据模板编码执行模板（核心方法）
     *
     * @param templateCode 模板编码（如：llm_chapter_generate_standard）
     * @param variables    变量值Map
     * @return 填充后的完整提示词
     */
    String executeTemplate(String templateCode, Map<String, Object> variables);

    /**
     * 创建新模板（初始版本为v1）
     *
     * @param request 创建请求
     * @return 模板ID
     */
    Long createTemplate(PromptTemplateCreateRequest request);

    /**
     * 更新模板（创建新版本）
     *
     * @param templateId 模板ID
     * @param request    更新请求
     * @return 新版本ID
     */
    Long updateTemplate(Long templateId, PromptTemplateUpdateRequest request);

    /**
     * 激活指定版本
     *
     * @param templateId 模板ID
     * @param versionId  版本ID
     */
    void activateVersion(Long templateId, Long versionId);

    /**
     * 查询模板详情（含当前版本）
     *
     * @param templateCode 模板编码
     * @return 模板DTO
     */
    PromptTemplateDto getTemplate(String templateCode);

    /**
     * 查询模板的所有版本
     *
     * @param templateId 模板ID
     * @return 版本列表
     */
    List<AiPromptTemplateVersion> getTemplateVersions(Long templateId);

    /**
     * 按服务类型和场景查询模板列表
     *
     * @param serviceType 服务类型（可选）
     * @param scenario    使用场景（可选）
     * @return 模板列表
     */
    List<PromptTemplateDto> listTemplates(String serviceType, String scenario);
}
