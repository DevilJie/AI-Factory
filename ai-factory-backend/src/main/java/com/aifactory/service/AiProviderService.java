package com.aifactory.service;

import cn.hutool.core.util.StrUtil;
import com.aifactory.common.LoggingUtil;
import com.aifactory.common.TokenUtil;
import com.aifactory.controller.AiProviderController.TestConnectionRequest;
import com.aifactory.controller.AiProviderController.TestConnectionResult;
import com.aifactory.dto.AiProviderDto;
import com.aifactory.entity.AiProvider;
import com.aifactory.entity.AiProviderTemplate;
import com.aifactory.mapper.AiProviderMapper;
import com.aifactory.mapper.AiProviderTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI服务提供商配置Service
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class AiProviderService {

    @Autowired
    private AiProviderMapper aiProviderMapper;

    @Autowired
    private AiProviderTemplateMapper aiProviderTemplateMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取Mapper（供其他Service使用）
     */
    public AiProviderMapper getAiProviderMapper() {
        return aiProviderMapper;
    }

    /**
     * 获取用户的所有AI服务提供商配置
     */
    public List<AiProvider> getProviders(String token, String providerType) {
        Long userId = TokenUtil.getUserId(token);

        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProvider::getUserId, userId);
        if (StrUtil.isNotBlank(providerType)) {
            wrapper.eq(AiProvider::getProviderType, providerType);
        }
        wrapper.orderByAsc(AiProvider::getId);

        List<AiProvider> providers = aiProviderMapper.selectList(wrapper);

        // 对 apiKey 进行脱敏处理
        for (AiProvider provider : providers) {
            provider.setApiKey(maskApiKey(provider.getApiKey()));
        }

        return providers;
    }

    /**
     * API Key 脱敏处理
     * 保留前4位和后4位，中间用 **** 替代
     * 例如：sk-abc123xyz789 -> sk-a****z789
     */
    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return apiKey;
        }
        if (apiKey.length() <= 8) {
            // 太短的密钥，只显示前2位
            return apiKey.substring(0, Math.min(2, apiKey.length())) + "****";
        }
        // 保留前4位和后4位
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 初始化默认的AI服务提供商配置
     * @deprecated 已废弃，用户应从模板市场自行添加配置
     */
    @Deprecated
    public void initializeDefaultProviders(Long userId, String providerType) {
        String now = DATE_FORMATTER.format(LocalDateTime.now());

        switch (providerType) {
            case "llm":
                // OpenAI
                createDefaultProvider(userId, "llm", "openai", "OpenAI",
                    "https://api.openai.com/v1", "gpt-4-turbo-preview",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 1, now);
                // Anthropic Claude
                createDefaultProvider(userId, "llm", "claude", "Anthropic",
                    "https://api.anthropic.com/v1", "claude-3-opus-20240229",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // DeepSeek
                createDefaultProvider(userId, "llm", "deepseek", "DeepSeek",
                    "https://api.deepseek.com/v1", "deepseek-chat",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 智谱AI
                createDefaultProvider(userId, "llm", "zhipu", "智谱AI",
                    "https://open.bigmodel.cn/api/paas/v4", "glm-4",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 百川智能
                createDefaultProvider(userId, "llm", "baichuan", "百川智能",
                    "https://api.baichuan-ai.com/v1", "Baichuan2-Turbo",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 通义千问
                createDefaultProvider(userId, "llm", "tongyi", "通义千问",
                    "https://dashscope.aliyuncs.com/api/v1", "qwen-turbo",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 文心一言
                createDefaultProvider(userId, "llm", "wenxin", "文心一言",
                    "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop",
                    "ernie-bot-4",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 月之暗面 Kimi
                createDefaultProvider(userId, "llm", "kimi", "Kimi",
                    "https://api.moonshot.cn/v1", "moonshot-v1-8k",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 零一万物 Yi
                createDefaultProvider(userId, "llm", "lingyi", "零一万物",
                    "https://api.lingyiwanwu.com/v1", "yi-34b-chat",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // Minimax
                createDefaultProvider(userId, "llm", "minimax", "MiniMax",
                    "https://api.minimax.chat/v1", "abab6.5s-chat",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                // 昆仑万维 Tiange
                createDefaultProvider(userId, "llm", "tiange", "天工",
                    "https://api.tiangong.cn/v1", "tiangong-chat",
                    "{\"temperature\":0.7,\"maxTokens\":4096}", 0, now);
                break;

            case "image":
                // Midjourney
                createDefaultProvider(userId, "image", "midjourney", "Midjourney",
                    "https://api.mindjourney.com/v1", "v6",
                    "{\"imageSize\":\"1024x1024\"}", 1, now);
                // Stable Diffusion API
                createDefaultProvider(userId, "image", "sd", "Stable Diffusion",
                    "https://api.stability.ai/v1", "stable-diffusion-xl-1024-v1-0",
                    "{\"imageSize\":\"1024x1024\",\"steps\":30,\"cfgScale\":7.5}", 0, now);
                // DALL-E 3
                createDefaultProvider(userId, "image", "dalle", "DALL-E 3",
                    "https://api.openai.com/v1", "dall-e-3",
                    "{\"imageSize\":\"1024x1024\"}", 0, now);
                // ComfyUI
                createDefaultProvider(userId, "image", "comfyui", "ComfyUI",
                    "http://localhost:8188", "sd_xl_base_1.0",
                    "{\"imageSize\":\"1024x1024\",\"steps\":30,\"cfgScale\":7.5}", 0, now);
                // Stable Diffusion WebUI
                createDefaultProvider(userId, "image", "sdwebui", "Stable Diffusion WebUI",
                    "http://localhost:7860", "sd_xl_base_1.0",
                    "{\"imageSize\":\"1024x1024\",\"steps\":30,\"cfgScale\":7.5}", 0, now);
                break;

            case "tts":
                // Azure TTS
                createDefaultProvider(userId, "tts", "azure", "Azure TTS",
                    "https://eastus.tts.speech.microsoft.com/cognitiveservices/v1", "",
                    "{\"voice\":\"zh-CN-XiaoxiaoNeural\",\"speed\":1,\"pitch\":1}", 1, now);
                // Google TTS
                createDefaultProvider(userId, "tts", "google", "Google TTS",
                    "https://texttospeech.googleapis.com/v1", "",
                    "{\"voice\":\"zh-CN-Wavenet-A\",\"speed\":1,\"pitch\":1}", 0, now);
                // 阿里云 TTS
                createDefaultProvider(userId, "tts", "aliyun", "阿里云 TTS",
                    "https://nls-meta.cn-shanghai.aliyuncs.com", "",
                    "{\"voice\":\"xiaoyun\",\"speed\":1,\"pitch\":1}", 0, now);
                // 腾讯云 TTS
                createDefaultProvider(userId, "tts", "tencent", "腾讯云 TTS",
                    "https://tts.cloud.tencent.com/stream", "",
                    "{\"voice\":\"101009\",\"speed\":1,\"pitch\":1}", 0, now);
                // ElevenLabs
                createDefaultProvider(userId, "tts", "elevenlabs", "ElevenLabs",
                    "https://api.elevenlabs.io/v1", "",
                    "{\"voice\":\"\",\"speed\":1,\"pitch\":1}", 0, now);
                // 豆包 TTS
                createDefaultProvider(userId, "tts", "doubao", "豆包",
                    "https://openspeech.bytedance.com/api/v1", "",
                    "{\"voice\":\"zh_female_shuangkuaisisi_moon_bigtts\",\"speed\":1,\"pitch\":1}", 0, now);
                break;

            case "video":
                // Runway
                createDefaultProvider(userId, "video", "runway", "Runway",
                    "", "",
                    "{}", 1, now);
                // Pika
                createDefaultProvider(userId, "video", "pika", "Pika",
                    "", "",
                    "{}", 0, now);
                break;
        }
    }

    /**
     * 创建默认提供商配置
     * @deprecated 已废弃，用户应从模板市场自行添加配置
     */
    @Deprecated
    private void createDefaultProvider(Long userId, String providerType, String providerCode,
                                       String providerName, String apiEndpoint, String model,
                                       String configJson, int isDefault, String now) {
        AiProvider provider = new AiProvider();
        provider.setUserId(userId);
        provider.setProviderType(providerType);
        provider.setProviderCode(providerCode);
        provider.setProviderName(providerName);
        provider.setApiEndpoint(apiEndpoint);
        provider.setModel(model);
        provider.setIsDefault(isDefault);
        provider.setEnabled(1);
        provider.setConfigJson(configJson);
        provider.setCreateTime(now);
        provider.setUpdateTime(now);

        // 使用 null 让 SQLite 自动生成 ID
        provider.setId(null);
        aiProviderMapper.insert(provider);
    }

    /**
     * 保存或更新AI服务提供商配置
     */
    public void saveProvider(String token, AiProviderDto dto) {
        Long userId = TokenUtil.getUserId(token);

        // 检查是否已存在该类型的该服务商配置
        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProvider::getUserId, userId);
        wrapper.eq(AiProvider::getProviderType, dto.getProviderType());
        wrapper.eq(AiProvider::getProviderCode, dto.getProviderCode());

        AiProvider existing = aiProviderMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setProviderName(dto.getProviderName());
            existing.setIconUrl(dto.getIconUrl());
            if (StrUtil.isNotBlank(dto.getApiKey())) {
                existing.setApiKey(dto.getApiKey());
            }
            if (StrUtil.isNotBlank(dto.getApiEndpoint())) {
                existing.setApiEndpoint(dto.getApiEndpoint());
            }
            if (StrUtil.isNotBlank(dto.getModel())) {
                existing.setModel(dto.getModel());
            }
            if (dto.getIsDefault() != null) {
                // 如果设置为默认，先将同类型的其他配置设为非默认
                if (dto.getIsDefault() == 1) {
                    clearDefaultProvider(userId, dto.getProviderType());
                }
                existing.setIsDefault(dto.getIsDefault());
            }
            if (dto.getEnabled() != null) {
                existing.setEnabled(dto.getEnabled());
            }
            if (StrUtil.isNotBlank(dto.getConfigJson())) {
                existing.setConfigJson(dto.getConfigJson());
            }
            existing.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));
            aiProviderMapper.updateById(existing);
        } else {
            // 新增
            AiProvider provider = new AiProvider();
            provider.setUserId(userId);
            provider.setProviderType(dto.getProviderType());
            provider.setProviderCode(dto.getProviderCode());
            provider.setProviderName(dto.getProviderName());
            provider.setIconUrl(dto.getIconUrl());
            provider.setApiKey(dto.getApiKey());
            provider.setApiEndpoint(dto.getApiEndpoint());
            provider.setModel(dto.getModel());
            provider.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
            provider.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
            provider.setConfigJson(dto.getConfigJson());
            provider.setCreateTime(DATE_FORMATTER.format(LocalDateTime.now()));
            provider.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));

            // 如果设置为默认，先将同类型的其他配置设为非默认
            if (provider.getIsDefault() == 1) {
                clearDefaultProvider(userId, dto.getProviderType());
            }

            aiProviderMapper.insert(provider);
        }
    }

    /**
     * 删除AI服务提供商配置
     */
    public void deleteProvider(String token, Long providerId) {
        Long userId = TokenUtil.getUserId(token);

        AiProvider provider = aiProviderMapper.selectById(providerId);
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new RuntimeException("配置不存在或无权删除");
        }

        aiProviderMapper.deleteById(providerId);
    }

    /**
     * 清除某类型的默认配置
     */
    private void clearDefaultProvider(Long userId, String providerType) {
        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProvider::getUserId, userId);
        wrapper.eq(AiProvider::getProviderType, providerType);
        wrapper.eq(AiProvider::getIsDefault, 1);

        AiProvider update = new AiProvider();
        update.setIsDefault(0);
        aiProviderMapper.update(update, wrapper);
    }

    /**
     * 通过配置ID测试AI服务提供商连接
     * 后端查询真实的apiKey进行测试
     */
    public TestConnectionResult testConnectionById(String token, Long providerId) {
        Long userId = TokenUtil.getUserId(token);

        // 查询配置
        AiProvider provider = aiProviderMapper.selectById(providerId);
        if (provider == null || !provider.getUserId().equals(userId)) {
            return new TestConnectionResult(false, "配置不存在或无权访问");
        }

        // 构建测试请求
        TestConnectionRequest request = new TestConnectionRequest();
        request.setProviderType(provider.getProviderType());
        request.setProviderCode(provider.getProviderCode());
        request.setApiKey(provider.getApiKey());  // 使用真实的apiKey
        request.setApiEndpoint(provider.getApiEndpoint());
        request.setModel(provider.getModel());

        return testConnection(request);
    }

    /**
     * 测试AI服务提供商连接
     */
    public TestConnectionResult testConnection(TestConnectionRequest request) {
        LoggingUtil.logBusiness("测试连接", "开始测试AI服务提供商连接",
            "类型=" + request.getProviderType() + ", 提供商=" + request.getProviderCode());

        try {
            // 验证必填字段
            if (StrUtil.isBlank(request.getApiKey())) {
                return new TestConnectionResult(false, "API Key不能为空");
            }

            // 如果 apiEndpoint 为空，尝试从模板中获取默认值
            String apiEndpoint = request.getApiEndpoint();
            if (StrUtil.isBlank(apiEndpoint)) {
                LambdaQueryWrapper<AiProviderTemplate> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AiProviderTemplate::getTemplateCode, request.getProviderCode());
                AiProviderTemplate template = aiProviderTemplateMapper.selectOne(wrapper);
                if (template != null && StrUtil.isNotBlank(template.getDefaultEndpoint())) {
                    apiEndpoint = template.getDefaultEndpoint();
                } else {
                    return new TestConnectionResult(false, "API端点不能为空且未找到默认配置");
                }
            }

            if (StrUtil.isBlank(request.getModel())) {
                return new TestConnectionResult(false, "模型名称不能为空");
            }

            // 对于LLM类型，进行真实的API调用测试
            if ("llm".equals(request.getProviderType())) {
                return testLLMConnection(request, apiEndpoint);
            }

            // 对于其他类型，暂时只验证配置完整性
            LoggingUtil.logBusiness("测试连接", "连接测试成功",
                "类型=" + request.getProviderType() + ", 提供商=" + request.getProviderCode());
            return new TestConnectionResult(true, "测试成功");

        } catch (Exception e) {
            LoggingUtil.logError("测试连接", e);
            return new TestConnectionResult(false, "测试失败");
        }
    }

    /**
     * 测试LLM连接
     */
    private TestConnectionResult testLLMConnection(TestConnectionRequest request, String apiEndpoint) {
        try {
            // 创建ChatModel
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(request.getApiKey())
                    .baseUrl(apiEndpoint)
                    .modelName(request.getModel())
                    .temperature(0.7)
                    .maxTokens(100)
                    .timeout(java.time.Duration.ofMinutes(5))  // 测试连接超时5分钟
                    .build();

            // 构建测试消息
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个AI助手。"));
            messages.add(new UserMessage("请回复'测试成功'"));

            // 调用模型（新版 API 使用 chat()）
            ChatResponse chatResponse = model.chat(messages);
            String response = chatResponse.aiMessage().text();

            LoggingUtil.logBusiness("测试连接", "LLM调用成功",
                "提供商=" + request.getProviderCode() + ", 响应=" + response);

            return new TestConnectionResult(true, "测试成功");

        } catch (Exception e) {
            LoggingUtil.logError("测试连接", new RuntimeException(
                "LLM连接测试失败: " + e.getMessage(), e));
            return new TestConnectionResult(false, "测试失败");
        }
    }
}
