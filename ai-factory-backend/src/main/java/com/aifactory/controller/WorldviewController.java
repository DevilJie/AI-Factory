package com.aifactory.controller;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.dto.TaskDto;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.response.Result;
import com.aifactory.service.TaskService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.util.XmlParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 世界观生成 Controller
 * 提供独立的 AI 生成世界观功能，包括获取、生成、保存和删除世界观设定
 *
 * @Author CaiZy
 * @Date 2025-02-01
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/worldview")
@Tag(name = "世界观生成", description = "世界观AI生成API，提供基于项目信息的AI自动生成世界观设定功能，包括世界类型、背景、力量体系、地理环境、势力分布等")
public class WorldviewController {

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private TaskService taskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取项目的世界观设定
     */
    @Operation(
            summary = "获取项目世界观设定",
            description = "获取指定项目的世界观设定信息，包括世界类型、背景描述、力量体系、地理环境、势力分布、时间线和世界规则等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取世界观设定",
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))),
            @ApiResponse(responseCode = "404", description = "项目不存在或无世界观设定")
    })
    @GetMapping
    public Result<NovelWorldview> getWorldview(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        try {
            NovelWorldview worldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            if (worldview != null) {
                return Result.ok(worldview);
            } else {
                return Result.ok(null);
            }
        } catch (Exception e) {
            log.error("获取世界观设定失败，projectId={}", projectId, e);
            return Result.error("获取世界观设定失败：" + e.getMessage());
        }
    }

    /**
     * AI 生成世界观设定（异步，推荐使用）
     */
    @Operation(
            summary = "AI异步生成世界观设定",
            description = "使用AI异步生成世界观设定。系统会根据项目的描述、故事基调、故事类型和标签自动生成完整的世界观设定，包括世界类型、背景、力量体系、地理环境、势力分布、时间线和规则。返回任务ID，可通过任务接口查询进度"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "任务创建成功，返回任务ID",
                    content = @Content(schema = @Schema(example = "{\"taskId\": 123, \"message\": \"世界观生成任务已创建，正在后台执行\"}"))),
            @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @PostMapping("/generate-async")
    public Result<Map<String, Object>> generateWorldviewAsync(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "请求参数（可选，系统会自动从项目配置获取所需参数）",
                    required = false
            )
            @RequestBody Map<String, String> request
    ) {
        try {
            log.info("开始AI异步生成世界观设定，projectId={}", projectId);

            // 1. 获取项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return Result.error("项目不存在");
            }

            // 2. 检查是否已有世界观设定，如果存在则删除（允许重新生成）
            NovelWorldview existingWorldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            if (existingWorldview != null) {
                log.info("项目已有世界观设定，ID={}，将删除并重新生成", existingWorldview.getId());
                worldviewMapper.deleteById(existingWorldview.getId());
            }

            // 3. 从项目配置获取参数（不需要前端传递，直接使用数据库中的项目信息）
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String storyGenre = project.getStoryGenre();
            String tags = project.getTags();

            // 4. 构建任务配置
            Map<String, Object> config = new HashMap<>();
            config.put("projectDescription", projectDescription);
            config.put("storyTone", storyTone);
            config.put("storyGenre", storyGenre);
            config.put("tags", tags);

            // 5. 创建任务请求
            CreateTaskRequest taskRequest = new CreateTaskRequest();
            taskRequest.setProjectId(projectId);
            taskRequest.setTaskType("worldview");
            taskRequest.setTaskName("AI生成世界观设定");
            taskRequest.setConfig(config);

            Result<TaskDto> result = taskService.createTask(taskRequest);

            if (result.getOk() == null || !result.getOk()) {
                return Result.error(result.getMsg());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", result.getData().getId());
            response.put("message", "世界观生成任务已创建，正在后台执行");

            return Result.ok(response);

        } catch (Exception e) {
            log.error("AI异步生成世界观设定失败，projectId={}", projectId, e);
            return Result.error("生成世界观设定失败：" + e.getMessage());
        }
    }

    /**
     * 保存/更新世界观设定
     */
    @Operation(
            summary = "保存或更新世界观设定",
            description = "手动保存或更新项目的世界观设定。如果项目已有世界观设定则更新，否则创建新的。可修改世界类型、背景描述、力量体系、地理环境、势力分布、时间线和世界规则等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "保存成功，返回世界观设定",
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))),
            @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @PostMapping("/save")
    public Result<NovelWorldview> saveWorldview(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "世界观设定内容",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))
            )
            @RequestBody NovelWorldview worldview
    ) {
        try {
            log.info("保存世界观设定，projectId={}", projectId);

            // 获取项目信息以获取userId
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return Result.error("项目不存在");
            }

            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);
            worldview.setUpdateTime(LocalDateTime.now());

            // 检查是否已存在
            NovelWorldview existing = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            if (existing != null) {
                // 更新
                worldview.setId(existing.getId());
                worldview.setCreateTime(existing.getCreateTime());
                worldviewMapper.updateById(worldview);
                log.info("更新世界观设定成功，ID={}", existing.getId());
            } else {
                // 新增
                worldview.setCreateTime(LocalDateTime.now());
                worldviewMapper.insert(worldview);
                log.info("创建世界观设定成功，ID={}", worldview.getId());
            }

            return Result.ok(worldview);

        } catch (Exception e) {
            log.error("保存世界观设定失败，projectId={}", projectId, e);
            return Result.error("保存世界观设定失败：" + e.getMessage());
        }
    }

    /**
     * 删除世界观设定
     */
    @Operation(
            summary = "删除项目世界观设定",
            description = "删除指定项目的世界观设定。注意：此操作不可逆，删除后无法恢复"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "项目不存在或无世界观设定")
    })
    @DeleteMapping
    public Result<Void> deleteWorldview(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        try {
            int count = worldviewMapper.delete(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            log.info("删除世界观设定成功，projectId={}, 删除数量={}", projectId, count);
            return Result.ok();

        } catch (Exception e) {
            log.error("删除世界观设定失败，projectId={}", projectId, e);
            return Result.error("删除世界观设定失败：" + e.getMessage());
        }
    }

    /**
     * 构建生成世界观的提示词
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String storyGenre, String tags) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的网络小说作家和世界观构建师。\n\n");
        prompt.append("请根据以下故事信息，构建一个完整的世界观设定：\n\n");

        prompt.append("【故事背景】\n").append(projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充").append("\n\n");
        prompt.append("【故事基调】").append(storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充").append("\n");
        prompt.append("【故事类型】").append(storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充").append("\n");

        // 添加标签信息
        if (tags != null && !tags.isEmpty()) {
            prompt.append("【标签】").append(tags).append("\n\n");
        }

        prompt.append("【重要】请严格按照以下XML格式返回世界观设定：\n");
        prompt.append("<worldview>\n");
        prompt.append("  <worldType>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</worldType>\n");
        prompt.append("  <worldBackground><![CDATA[世界背景描述（200-300字）]]></worldBackground>\n");
        prompt.append("  <powerSystem><![CDATA[力量体系或修炼体系（如有），包括等级划分、能力来源等]]></powerSystem>\n");
        prompt.append("  <geography><![CDATA[地理环境描述，包括重要地点、国家、区域等]]></geography>\n");
        prompt.append("  <forces><![CDATA[势力分布，包括主要组织、国家、门派等]]></forces>\n");
        prompt.append("  <timeline><![CDATA[时间线设定（如适用）]]></timeline>\n");
        prompt.append("  <rules><![CDATA[世界的基本规则和限制]]></rules>\n");
        prompt.append("</worldview>\n\n");

        prompt.append("【XML格式要求】\n");
        prompt.append("1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n");
        prompt.append("2. 不要包含markdown代码块标记（```xml），直接返回XML\n");
        prompt.append("3. 不要包含任何解释或说明文字，只返回XML数据\n\n");

        prompt.append("内容要求：\n");
        prompt.append("1. 世界观要符合故事类型和基调\n");
        prompt.append("2. 力量体系要清晰、合理，有可发展性\n");
        prompt.append("3. 各个要素之间要相互关联，形成完整的世界\n");
        prompt.append("4. 返回的必须是纯XML格式，不要有任何其他说明文字\n");

        return prompt.toString();
    }

    /**
     * 解析AI响应并保存世界观设定
     */
    private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {
            // 使用XML工具类解析
            Map<String, String> worldviewData = XmlParser.parseXml(
                aiResponse,
                "worldview",
                "worldType", "worldBackground", "powerSystem", "geography", "forces", "timeline", "rules"
            );

            if (worldviewData.isEmpty()) {
                log.warn("解析世界观XML失败，数据为空");
                return null;
            }

            // 获取项目信息以获取userId
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.error("项目不存在，projectId={}", projectId);
                return null;
            }

            log.info("项目信息: projectId={}, userId={}", project.getId(), project.getUserId());

            LocalDateTime now = LocalDateTime.now();
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);

            worldview.setWorldType(worldviewData.getOrDefault("worldType", storyGenre));
            worldview.setWorldBackground(worldviewData.getOrDefault("worldBackground", ""));
            worldview.setPowerSystem(worldviewData.getOrDefault("powerSystem", ""));
            worldview.setGeography(worldviewData.getOrDefault("geography", ""));
            worldview.setForces(worldviewData.getOrDefault("forces", ""));
            worldview.setTimeline(worldviewData.getOrDefault("timeline", ""));
            worldview.setRules(worldviewData.getOrDefault("rules", ""));
            worldview.setCreateTime(now);
            worldview.setUpdateTime(now);

            worldviewMapper.insert(worldview);
            log.info("世界观设定保存成功，ID: {}", worldview.getId());

            return worldview;

        } catch (Exception e) {
            log.error("解析世界观失败", e);
            log.error("AI响应: {}", aiResponse);
            return null;
        }
    }

    /**
     * 获取值或默认值（处理空字符串情况）
     *
     * @param value 请求中的值
     * @param defaultValue 默认值（通常来自项目配置）
     * @return 如果value为null或空字符串，返回defaultValue；否则返回value
     */
    private String getValueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
