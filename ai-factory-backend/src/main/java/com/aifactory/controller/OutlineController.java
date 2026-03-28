package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.*;
import com.aifactory.response.Result;
import com.aifactory.service.OutlineService;
import com.aifactory.service.TaskService;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 大纲管理控制器
 *
 * @Author CaiZy
 * @Date 2025-01-23
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/outline")
@Tag(name = "大纲管理", description = "小说大纲的生成、查询和管理接口，包括分卷规划、章节计划以及情节阶段的完成状态管理")
public class OutlineController {

    @Autowired
    private OutlineService outlineService;

    @Autowired
    private TaskService taskService;

    /**
     * AI生成大纲（同步，已弃用，建议使用异步接口）
     */
    @PostMapping("/generate")
    @Operation(
            summary = "AI生成大纲（同步）",
            description = "同步方式调用AI生成小说大纲。此接口已弃用，建议使用异步接口 /generate-async。" +
                    "同步接口可能会因为AI生成时间较长而导致请求超时。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "大纲生成成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OutlineDto.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误或AI生成失败")
    })
    public Result<OutlineDto> generateOutline(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "AI生成大纲请求参数", required = true)
            @RequestBody AIGenerateOutlineRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI生成项目 {} 的大纲", userId, projectId);

        request.setProjectId(projectId);
        OutlineDto outline = outlineService.generateOutline(request);

        return Result.ok(outline);
    }

    /**
     * AI生成大纲（异步，推荐使用）
     */
    @PostMapping("/generate-async")
    @Operation(
            summary = "AI生成大纲（异步）",
            description = "异步方式调用AI生成小说大纲，推荐使用此接口。" +
                    "接口会立即返回一个任务ID，可以通过任务ID查询生成进度和结果。" +
                    "支持继续生成（通过startVolumeNumber指定起始卷号）和重新生成（通过regenerate=true清空现有大纲）。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "任务创建成功，返回任务ID",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "任务创建失败")
    })
    public Result<Map<String, Object>> generateOutlineAsync(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "AI生成大纲请求参数，包含项目描述、故事基调、目标字数等信息", required = true)
            @RequestBody AIGenerateOutlineRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI异步生成项目 {} 的大纲", userId, projectId);

        // 构建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("projectDescription", request.getProjectDescription());
        config.put("storyTone", request.getStoryTone());
        config.put("storyGenre", request.getStoryGenre());
        config.put("targetVolumeCount", request.getTargetVolumeCount());
        config.put("avgWordsPerVolume", request.getAvgWordsPerVolume());
        config.put("targetWordCount", request.getTargetWordCount());
        config.put("additionalRequirements", request.getAdditionalRequirements());

        // 添加继续生成和重新生成参数
        if (request.getStartVolumeNumber() != null && request.getStartVolumeNumber() > 1) {
            config.put("startVolumeNumber", request.getStartVolumeNumber());
            log.info("设置起始卷号: {}", request.getStartVolumeNumber());
        }
        if (request.getRegenerate() != null && request.getRegenerate()) {
            config.put("regenerate", true);
            log.info("设置重新生成模式");
        }

        // 创建任务请求
        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("outline");
        taskRequest.setTaskName("AI生成小说大纲");
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);

        if (result.getOk() == null || !result.getOk()) {
            return Result.error(result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "大纲生成任务已创建，正在后台执行");

        return Result.ok(response);
    }

    /**
     * 获取项目大纲
     */
    @GetMapping
    @Operation(
            summary = "获取项目大纲",
            description = "获取指定项目的大纲信息，包括整体概念、主题、分卷规划和章节规划。" +
                    "如果项目尚未生成大纲，返回null而不是错误。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OutlineDto.class))),
            @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<OutlineDto> getOutline(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的大纲", userId, projectId);

        OutlineDto outline = outlineService.getOutline(projectId);

        // 大纲不存在是正常情况，返回null而不是错误
        return Result.ok(outline);
    }

    /**
     * AI生成章节计划（异步）
     */
    @PostMapping("/generate-chapters")
    @Operation(
            summary = "AI生成章节计划",
            description = "为指定分卷的特定情节阶段生成章节计划。" +
                    "情节阶段包括：introduction(起始阶段)、development(发展阶段)、climax(高潮阶段)、conclusion(结局阶段)。" +
                    "此接口为异步接口，会返回任务ID用于查询进度。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "任务创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "分卷不存在")
    })
    public Result<Map<String, Object>> generateChapters(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节计划生成请求，包含分卷ID和情节阶段", required = true)
            @RequestBody GenerateChaptersRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求为项目 {} 的分卷 {} 生成章节计划", userId, projectId, request.getVolumeId());

        request.setProjectId(projectId);
        Map<String, Object> result = outlineService.generateChaptersAsync(request);

        return Result.ok(result);
    }

    /**
     * 标记阶段为完成
     */
    @PostMapping("/stage/complete")
    @Operation(
            summary = "标记情节阶段为完成",
            description = "将指定分卷的某个情节阶段标记为已完成。" +
                    "情节阶段包括：introduction(起始阶段)、development(发展阶段)、climax(高潮阶段)、conclusion(结局阶段)。" +
                    "标记完成后，系统会记录完成状态，便于追踪写作进度。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "分卷或阶段不存在")
    })
    public Result<Void> markStageCompleted(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "标记完成请求，包含分卷ID和情节阶段", required = true)
            @RequestBody MarkStageCompletedRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求标记项目 {} 的分卷 {} 阶段 {} 为完成",
            userId, projectId, request.getVolumeId(), request.getPlotStage());

        request.setProjectId(projectId);
        outlineService.markStageCompleted(request);

        return Result.ok();
    }

    /**
     * 取消阶段完成状态
     */
    @PostMapping("/stage/uncomplete")
    @Operation(
            summary = "取消情节阶段的完成状态",
            description = "取消指定分卷的某个情节阶段的完成标记，将状态恢复为未完成。" +
                    "情节阶段包括：introduction(起始阶段)、development(发展阶段)、climax(高潮阶段)、conclusion(结局阶段)。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "分卷或阶段不存在")
    })
    public Result<Void> unmarkStageCompleted(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "取消完成请求，包含分卷ID和情节阶段", required = true)
            @RequestBody MarkStageCompletedRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求取消项目 {} 的分卷 {} 阶段 {} 的完成状态",
            userId, projectId, request.getVolumeId(), request.getPlotStage());

        request.setProjectId(projectId);
        outlineService.unmarkStageCompleted(request);

        return Result.ok();
    }

    /**
     * AI优化分卷详情（异步）
     */
    @PostMapping("/volumes/{volumeId}/optimize")
    @Operation(
            summary = "AI优化分卷详情",
            description = "使用AI优化指定分卷的详情信息。" +
                    "系统会参考世界观设定和前面所有卷的摘要来生成分卷内容。" +
                    "注意：如果该分卷已有章节规划，则无法优化。" +
                    "此接口为异步接口，会返回任务ID用于查询进度。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "任务创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或分卷已有章节规划"),
            @ApiResponse(responseCode = "404", description = "分卷不存在")
    })
    public Result<Map<String, Object>> optimizeVolume(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "分卷ID", required = true, example = "1")
            @PathVariable Long volumeId,
            @Parameter(description = "优化请求，包含目标章节数", required = true)
            @RequestBody VolumeOptimizeRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI优化项目 {} 的分卷 {}", userId, projectId, volumeId);

        // 构建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("volumeId", volumeId);
        config.put("targetChapterCount", request.getTargetChapterCount());

        // 创建任务请求
        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("volume_optimize");
        taskRequest.setTaskName("AI优化分卷详情");
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);

        if (result.getOk() == null || !result.getOk()) {
            return Result.error(result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "分卷优化任务已创建，正在后台执行");

        return Result.ok(response);
    }
}
