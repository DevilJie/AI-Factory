package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.common.LoggingUtil;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.enums.AIRole;
import com.aifactory.response.Result;
import com.aifactory.service.AIGenerateService;
import com.aifactory.service.prompt.PromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI生成控制器
 *
 * 提供通用的AI生成接口，支持多种AI角色和任务类型的文本生成
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Tag(name = "AI生成", description = "AI文本生成相关接口，提供通用AI生成、快速生成、项目生成等功能")
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIGenerateService aiGenerateService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    /**
     * 通用AI生成接口
     */
    @Operation(
        summary = "通用AI生成",
        description = "通用的AI文本生成接口，支持指定AI角色、任务类型、温度参数等配置。" +
                      "可用于章节生成、大纲生成、世界观生成、角色修复等多种场景。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AIGenerateResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "AI服务调用失败")
    })
    @PostMapping("/generate")
    public Result<AIGenerateResponse> generate(
            @Parameter(description = "AI生成请求参数", required = true)
            @Valid @RequestBody AIGenerateRequest request) {
        AIGenerateResponse response = aiGenerateService.generate(request);
        return Result.ok(response);
    }

    /**
     * 快速生成接口（使用通用助手）
     */
    @Operation(
        summary = "快速AI生成",
        description = "使用通用AI助手快速生成文本内容。简化版的生成接口，" +
                      "只需提供任务描述即可获得AI生成结果。适用于简单的文本生成场景。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "400", description = "任务描述为空"),
        @ApiResponse(responseCode = "500", description = "AI服务调用失败")
    })
    @PostMapping("/quickGenerate")
    public Result<String> quickGenerate(
            @Parameter(description = "任务描述文本，描述需要AI完成的具体任务", required = true,
                      example = "请帮我生成一个玄幻小说的开头段落")
            @RequestBody String task) {
        String content = aiGenerateService.quickGenerate(task);
        return Result.ok(content);
    }

    /**
     * AI生成项目（专门接口）
     */
    @Operation(
        summary = "AI生成项目",
        description = "根据用户的创作想法，使用AI自动生成项目名称和项目描述。" +
                      "适用于用户只有一个初步想法，需要AI帮助完善项目设定的场景。" +
                      "内部使用NOVEL_WRITER角色和llm_project_name_generate请求类型。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "项目生成成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AIGenerateProjectResult.class))),
        @ApiResponse(responseCode = "400", description = "创作想法为空或格式错误"),
        @ApiResponse(responseCode = "500", description = "AI服务调用失败")
    })
    @PostMapping("/generateProject")
    public Result<AIGenerateProjectResult> generateProject(
            @Parameter(description = "AI生成项目请求参数", required = true)
            @RequestBody AIGenerateProjectRequest request) {
        Long startTime = System.currentTimeMillis();

        // 记录业务日志
        LoggingUtil.logBusiness("AI生成项目", "用户请求AI生成项目", request);

        try {
            // 构建生成请求
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setRequestType("llm_project_name_generate");

            // 使用模板系统构建提示词
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("idea", request.getIdea());
            String prompt = promptTemplateService.executeTemplate("llm_project_name_generate", variables);
            aiRequest.setTask(prompt);

            // 调用AI生成
            AIGenerateResponse response = aiGenerateService.generate(aiRequest);

            // 记录AI调用日志
            LoggingUtil.logAICall(
                response.getModel(),
                response.getModel(),
                String.valueOf(response.getPromptTokens()),
                String.valueOf(response.getCompletionTokens()),
                System.currentTimeMillis() - startTime
            );

            // 解析生成的内容
            AIGenerateProjectResult result = parseProjectResponse(response.getContent());

            // 记录业务成功日志
            LoggingUtil.logBusiness("AI生成项目", "项目生成成功",
                "name=" + result.getName() + ", description长度=" + result.getDescription().length());

            return Result.ok(result);
        } catch (Exception e) {
            // 记录异常日志
            LoggingUtil.logError("AI生成项目", e);
            throw e;
        }
    }

    /**
     * 解析AI生成的项目响应
     */
    private AIGenerateProjectResult parseProjectResponse(String content) {
        try {
            // 1. 清理可能的markdown代码块标记
            String cleanedContent = content;

            // 移除 ```json 和 ``` 标记
            if (content.contains("```json")) {
                cleanedContent = content.replaceAll("```json\\n?", "")
                                            .replaceAll("```", "")
                                            .trim();
            } else if (content.contains("```")) {
                cleanedContent = content.replaceAll("```\\n?", "")
                                            .replaceAll("```", "")
                                            .trim();
            }

            // 2. 尝试解析JSON格式
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cleanedContent, AIGenerateProjectResult.class);
        } catch (Exception e) {
            // 如果解析失败，返回默认内容
            e.printStackTrace();
            AIGenerateProjectResult result = new AIGenerateProjectResult();
            result.setName("AI生成项目");
            result.setDescription(content);
            return result;
        }
    }

    /**
     * AI生成项目请求DTO
     */
    @Schema(description = "AI生成项目请求参数")
    public static class AIGenerateProjectRequest {
        @Schema(description = "创作想法，用户对小说的初步构思或灵感描述",
                example = "我想写一个关于修仙世界的小说，主角是一个普通少年，意外获得上古传承",
                maxLength = 500)
        private String idea;

        public String getIdea() {
            return idea;
        }

        public void setIdea(String idea) {
            this.idea = idea;
        }
    }

    /**
     * AI生成项目结果DTO
     */
    @Schema(description = "AI生成项目结果")
    public static class AIGenerateProjectResult {
        @Schema(description = "生成的项目名称",
                example = "逆天仙途")
        private String name;

        @Schema(description = "生成的项目描述，包含故事背景、主要设定等",
                example = "一个普通少年意外获得上古仙人的传承，从此踏上逆天修仙之路...")
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
