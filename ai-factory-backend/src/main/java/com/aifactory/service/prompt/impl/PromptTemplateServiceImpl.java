package com.aifactory.service.prompt.impl;

import cn.hutool.core.util.StrUtil;
import com.aifactory.common.UserContext;
import com.aifactory.dto.PromptTemplateCreateRequest;
import com.aifactory.dto.PromptTemplateDto;
import com.aifactory.dto.PromptTemplateUpdateRequest;
import com.aifactory.entity.AiPromptTemplate;
import com.aifactory.entity.AiPromptTemplateVersion;
import com.aifactory.exception.PromptTemplateException;
import com.aifactory.mapper.AiPromptTemplateMapper;
import com.aifactory.mapper.AiPromptTemplateVersionMapper;
import com.aifactory.service.prompt.PromptTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务实现
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
@Slf4j
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    @Autowired
    private AiPromptTemplateMapper templateMapper;

    @Autowired
    private AiPromptTemplateVersionMapper versionMapper;

    @Override
    public String executeTemplate(String templateCode, Map<String, Object> variables) {
        log.info("执行提示词模板: {}, 变量数量: {}", templateCode, variables != null ? variables.size() : 0);

        // 1. 查询模板
        AiPromptTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getTemplateCode, templateCode)
                .eq(AiPromptTemplate::getIsActive, true)
        );

        if (template == null) {
            throw new PromptTemplateException("提示词模板不存在或已禁用: " + templateCode, templateCode);
        }

        // 2. 查询当前激活版本
        AiPromptTemplateVersion version = versionMapper.selectOne(
            new LambdaQueryWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getId, template.getCurrentVersionId())
                .eq(AiPromptTemplateVersion::getIsActive, true)
        );

        if (version == null) {
            throw new PromptTemplateException("提示词模板版本不存在: " + templateCode, templateCode);
        }

        // 3. 验证必需变量
        validateRequiredVariables(templateCode, version.getVariableDefinitions(), variables);

        // 4. 使用hutool进行变量替换
        String templateContent = version.getTemplateContent();
        String filledPrompt;

        log.info("模板变量详情: templateCode={}, variables={}, variablesSize={}",
            templateCode, variables, variables != null ? variables.size() : 0);

        try {
            if (variables != null && !variables.isEmpty()) {
                filledPrompt = StrUtil.format(templateContent, variables);
            } else {
                filledPrompt = templateContent;
            }
        } catch (Exception e) {
            log.error("模板变量替换失败: {}, 错误: {}", templateCode, e.getMessage(), e);
            throw new PromptTemplateException("模板变量替换失败: " + e.getMessage(), templateCode, e);
        }

        log.info("提示词模板执行成功: {}, 生成提示词长度: {}", templateCode, filledPrompt.length());

        return filledPrompt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "promptTemplate", key = "#request.templateCode")
    public Long createTemplate(PromptTemplateCreateRequest request) {
        log.info("创建提示词模板: {}", request.getTemplateCode());

        Long userId = UserContext.getUserId();

        // 1. 检查code唯一性
        Long count = templateMapper.selectCount(
            new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getTemplateCode, request.getTemplateCode())
        );

        if (count > 0) {
            throw new PromptTemplateException("模板编码已存在: " + request.getTemplateCode());
        }

        // 2. 创建模板主记录
        AiPromptTemplate template = new AiPromptTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setServiceType(request.getServiceType());
        template.setScenario(request.getScenario());
        template.setDescription(request.getDescription());
        template.setTags(request.getTags());
        template.setIsActive(true);
        template.setIsSystem(false);
        template.setCreatedBy(userId);
        templateMapper.insert(template);

        // 3. 创建v1版本
        AiPromptTemplateVersion version = new AiPromptTemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNumber(1);
        version.setTemplateContent(request.getTemplateContent());
        version.setVariableDefinitions(request.getVariableDefinitions());
        version.setVersionComment("初始版本");
        version.setIsActive(true);
        version.setCreatedBy(userId);
        versionMapper.insert(version);

        // 4. 更新模板的currentVersionId
        template.setCurrentVersionId(version.getId());
        templateMapper.updateById(template);

        log.info("创建模板成功: {}, 版本ID: {}", template.getTemplateCode(), version.getId());

        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "promptTemplate", allEntries = true)
    public Long updateTemplate(Long templateId, PromptTemplateUpdateRequest request) {
        log.info("更新提示词模板: templateId={}", templateId);

        Long userId = UserContext.getUserId();

        AiPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new PromptTemplateException("模板不存在: templateId=" + templateId);
        }

        // 1. 查询当前最大版本号
        Integer maxVersion = versionMapper.selectList(
            new LambdaQueryWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getTemplateId, templateId)
        ).stream()
            .map(AiPromptTemplateVersion::getVersionNumber)
            .max(Integer::compareTo)
            .orElse(0);

        // 2. 创建新版本
        AiPromptTemplateVersion newVersion = new AiPromptTemplateVersion();
        newVersion.setTemplateId(templateId);
        newVersion.setVersionNumber(maxVersion + 1);
        newVersion.setTemplateContent(request.getTemplateContent());
        newVersion.setVariableDefinitions(request.getVariableDefinitions());
        newVersion.setVersionComment(request.getVersionComment());
        newVersion.setIsActive(true);
        newVersion.setCreatedBy(userId);
        versionMapper.insert(newVersion);

        // 3. 旧版本全部设为非激活
        versionMapper.update(null,
            new LambdaUpdateWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getTemplateId, templateId)
                .ne(AiPromptTemplateVersion::getId, newVersion.getId())
                .set(AiPromptTemplateVersion::getIsActive, false)
        );

        // 4. 更新模板的currentVersionId
        template.setCurrentVersionId(newVersion.getId());
        template.setUpdatedBy(userId);
        templateMapper.updateById(template);

        log.info("更新模板成功: {}, 新版本: v{}", template.getTemplateCode(), newVersion.getVersionNumber());

        return newVersion.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "promptTemplate", allEntries = true)
    public void activateVersion(Long templateId, Long versionId) {
        log.info("激活模板版本: templateId={}, versionId={}", templateId, versionId);

        Long userId = UserContext.getUserId();

        AiPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new PromptTemplateException("模板不存在: templateId=" + templateId);
        }

        AiPromptTemplateVersion version = versionMapper.selectOne(
            new LambdaQueryWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getId, versionId)
                .eq(AiPromptTemplateVersion::getTemplateId, templateId)
        );

        if (version == null) {
            throw new PromptTemplateException("版本不存在: versionId=" + versionId);
        }

        // 1. 该模板所有版本设为非激活
        versionMapper.update(null,
            new LambdaUpdateWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getTemplateId, templateId)
                .set(AiPromptTemplateVersion::getIsActive, false)
        );

        // 2. 激活指定版本
        version.setIsActive(true);
        versionMapper.updateById(version);

        // 3. 更新模板的currentVersionId
        template.setCurrentVersionId(versionId);
        template.setUpdatedBy(userId);
        templateMapper.updateById(template);

        log.info("激活版本成功: {}, 版本: v{}", template.getTemplateCode(), version.getVersionNumber());
    }

    @Override
    public PromptTemplateDto getTemplate(String templateCode) {
        AiPromptTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getTemplateCode, templateCode)
        );

        if (template == null) {
            return null;
        }

        PromptTemplateDto dto = new PromptTemplateDto();
        BeanUtils.copyProperties(template, dto);

        // 查询当前版本
        if (template.getCurrentVersionId() != null) {
            AiPromptTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
            dto.setCurrentVersion(version);
        }

        return dto;
    }

    @Override
    public List<AiPromptTemplateVersion> getTemplateVersions(Long templateId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<AiPromptTemplateVersion>()
                .eq(AiPromptTemplateVersion::getTemplateId, templateId)
                .orderByDesc(AiPromptTemplateVersion::getVersionNumber)
        );
    }

    @Override
    public List<PromptTemplateDto> listTemplates(String serviceType, String scenario) {
        LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();

        if (serviceType != null && !serviceType.isEmpty()) {
            wrapper.eq(AiPromptTemplate::getServiceType, serviceType);
        }

        if (scenario != null && !scenario.isEmpty()) {
            wrapper.eq(AiPromptTemplate::getScenario, scenario);
        }

        wrapper.orderByDesc(AiPromptTemplate::getUpdatedTime);

        List<AiPromptTemplate> templates = templateMapper.selectList(wrapper);
        List<PromptTemplateDto> result = new ArrayList<>();

        for (AiPromptTemplate template : templates) {
            PromptTemplateDto dto = new PromptTemplateDto();
            BeanUtils.copyProperties(template, dto);

            // 查询当前版本
            if (template.getCurrentVersionId() != null) {
                AiPromptTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
                dto.setCurrentVersion(version);
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * 验证必需变量
     */
    private void validateRequiredVariables(String templateCode, String variableDefinitions, Map<String, Object> variables) {
        if (variableDefinitions == null || variableDefinitions.isEmpty()) {
            return;
        }

        if (variables == null || variables.isEmpty()) {
            // 如果有变量定义但没有提供变量值，检查是否都是可选变量
            // 这里简单处理，实际可以解析JSON进行详细验证
            log.warn("模板 {} 有变量定义但未提供变量值", templateCode);
            return;
        }

        // TODO: 解析JSON变量定义，验证必需变量是否都提供了值
        // 这里简化处理，实际应该使用JSON库解析并验证
    }
}
