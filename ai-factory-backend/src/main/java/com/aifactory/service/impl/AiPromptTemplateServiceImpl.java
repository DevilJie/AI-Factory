package com.aifactory.service.impl;

import com.aifactory.entity.AiPromptTemplate;
import com.aifactory.entity.AiPromptTemplateContent;
import com.aifactory.mapper.AiPromptTemplateContentMapper;
import com.aifactory.mapper.AiPromptTemplateMapper;
import com.aifactory.service.AiPromptTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI提示词模板Service实现
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService {

    private final AiPromptTemplateMapper templateMapper;
    private final AiPromptTemplateContentMapper contentMapper;

    @Override
    public String getTemplateContent(String templateCode) {
        // 1. 获取模板
        LambdaQueryWrapper<AiPromptTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(AiPromptTemplate::getTemplateCode, templateCode)
                     .eq(AiPromptTemplate::getIsActive, true);
        AiPromptTemplate template = templateMapper.selectOne(templateWrapper);

        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateCode);
        }

        // 2. 获取当前激活版本的内容
        LambdaQueryWrapper<AiPromptTemplateContent> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.eq(AiPromptTemplateContent::getTemplateId, template.getCurrentVersionId())
                     .eq(AiPromptTemplateContent::getIsActive, true);
        AiPromptTemplateContent content = contentMapper.selectOne(contentWrapper);

        if (content == null) {
            throw new RuntimeException("模板内容不存在: " + templateCode);
        }

        return content.getContent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTemplateContent(String templateCode, String version, String content) {
        // 1. 获取模板
        LambdaQueryWrapper<AiPromptTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(AiPromptTemplate::getTemplateCode, templateCode);
        AiPromptTemplate template = templateMapper.selectOne(templateWrapper);

        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateCode);
        }

        // 2. 保存新版本内容
        AiPromptTemplateContent templateContent = new AiPromptTemplateContent();
        templateContent.setTemplateId(template.getId());
        templateContent.setVersion(version);
        templateContent.setContent(content);
        templateContent.setIsActive(true);
        contentMapper.insert(templateContent);

        // 3. 更新模板的当前版本
        template.setCurrentVersionId(templateContent.getId());
        templateMapper.updateById(template);

        log.info("保存模板内容成功: templateCode={}, version={}", templateCode, version);
        return templateContent.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initBuiltinTemplates() {
        log.info("开始初始化内置AI提示词模板...");

        // 检查是否已初始化
        LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPromptTemplate::getIsSystem, true)
               .eq(AiPromptTemplate::getTemplateCode, "llm_worldview_generate_standard");
        if (templateMapper.selectCount(wrapper) > 0) {
            log.info("内置模板已存在，跳过初始化");
            return;
        }

        // TODO: 创建6个核心模板
        // 1. llm_worldview_generate_standard
        // 2. llm_character_arc_generate
        // 3. llm_growth_path_generate
        // 4. llm_volume_generate_standard
        // 5. llm_chapter_plan_standard
        // 6. llm_chapter_create_standard

        log.info("内置AI提示词模板初始化完成");
    }
}
