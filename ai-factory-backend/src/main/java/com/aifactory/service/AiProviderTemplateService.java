package com.aifactory.service;

import cn.hutool.core.util.StrUtil;
import com.aifactory.entity.AiProviderTemplate;
import com.aifactory.mapper.AiProviderTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI提供商模板Service
 *
 * @Author AI Assistant
 * @Date 2025-02-05
 */
@Service
public class AiProviderTemplateService {

    @Autowired
    private AiProviderTemplateMapper aiProviderTemplateMapper;

    /**
     * 获取所有启用的模板
     */
    public List<AiProviderTemplate> getAllEnabledTemplates() {
        LambdaQueryWrapper<AiProviderTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderTemplate::getIsEnabled, true);
        wrapper.orderByAsc(AiProviderTemplate::getSortOrder);
        return aiProviderTemplateMapper.selectList(wrapper);
    }

    /**
     * 根据类型获取启用的模板
     */
    public List<AiProviderTemplate> getTemplatesByType(String providerType) {
        LambdaQueryWrapper<AiProviderTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderTemplate::getProviderType, providerType);
        wrapper.eq(AiProviderTemplate::getIsEnabled, true);
        wrapper.orderByAsc(AiProviderTemplate::getSortOrder);
        return aiProviderTemplateMapper.selectList(wrapper);
    }

    /**
     * 根据模板代码获取模板
     */
    public AiProviderTemplate getTemplateByCode(String templateCode) {
        LambdaQueryWrapper<AiProviderTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderTemplate::getTemplateCode, templateCode);
        return aiProviderTemplateMapper.selectOne(wrapper);
    }

    /**
     * 根据ID获取模板
     */
    public AiProviderTemplate getTemplateById(Long id) {
        return aiProviderTemplateMapper.selectById(id);
    }

    /**
     * 创建或更新模板（仅限管理员）
     */
    public void saveTemplate(AiProviderTemplate template) {
        if (template.getId() != null) {
            // 更新
            aiProviderTemplateMapper.updateById(template);
        } else {
            // 新增
            aiProviderTemplateMapper.insert(template);
        }
    }

    /**
     * 删除模板（仅系统内置模板不允许删除）
     */
    public void deleteTemplate(Long templateId) {
        AiProviderTemplate template = getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        if (template.getIsSystem()) {
            throw new RuntimeException("系统内置模板不允许删除");
        }
        aiProviderTemplateMapper.deleteById(templateId);
    }

    /**
     * 初始化系统默认模板（首次启动时调用）
     */
    public void initializeDefaultTemplates() {
        // 检查是否已有模板数据
        Long count = aiProviderTemplateMapper.selectCount(null);
        if (count > 0) {
            return;
        }

        // 这里可以插入默认模板，或者通过SQL脚本初始化
        // 本实现使用SQL脚本 init_ai_provider_templates.sql
    }
}
