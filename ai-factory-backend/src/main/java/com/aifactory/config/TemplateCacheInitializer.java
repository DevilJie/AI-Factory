package com.aifactory.config;

import com.aifactory.dto.PromptTemplateCreateRequest;
import com.aifactory.service.prompt.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板缓存初始化器
 *
 * 职责：
 * - 应用启动时预加载所有活跃的提示词模板到缓存
 * - 确保缓存与数据库一致，避免使用过期模板
 *
 * @Author CaiZy
 * @Date 2025-02-07
 */
@Slf4j
@Component
public class TemplateCacheInitializer implements ApplicationRunner {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 需要预加载的模板编码列表
     */
    private static final String[] TEMPLATE_CODES_TO_WARMUP = {
        "llm_chapter_generate_standard",
        "llm_outline_chapter_generate"
    };

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始预加载提示词模板缓存...");

        try {
            // 清空旧缓存（如果有的话）
            var cache = cacheManager.getCache("promptTemplate");
            if (cache != null) {
                cache.clear();
                log.info("已清空旧的模板缓存");
            }

            // 注意：章节压缩模板(llm_chapter_compress)已直接在数据库中初始化
            // 位置：ai_prompt_template表 id=8, ai_prompt_template_version表 id=12

            log.info("提示词模板缓存预加载完成（已清空缓存，模板将在首次使用时加载）");

        } catch (Exception e) {
            log.error("模板缓存初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，允许应用继续启动
        }
    }
}
