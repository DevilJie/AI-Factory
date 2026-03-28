package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.dto.TaskDto;
import com.aifactory.response.Result;
import com.aifactory.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 异步任务控制器
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/api/task")
@Tag(name = "任务管理", description = "异步任务的创建、查询、取消等操作接口。支持章节生成、大纲生成等长时间运行的任务，可通过轮询获取任务进度和状态。")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping("/create")
    @Operation(summary = "创建异步任务", description = "创建一个新的异步任务。任务创建后进入待执行状态(pending)，由后台任务调度器执行。支持的任务类型包括：chapter_generate(章节生成)、outline_generate(大纲生成)、storyboard_generate(分镜生成)等。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "任务创建成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TaskDto> createTask(
            @Parameter(description = "创建任务请求参数，包含项目ID、任务类型、任务名称和配置信息", required = true)
            @RequestBody CreateTaskRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求创建任务: {}", userId, request.getTaskName());

        return taskService.createTask(request);
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/{taskId}/status")
    @Operation(summary = "获取任务状态", description = "根据任务ID查询任务的详细信息和当前状态。返回任务的基本信息、进度百分比、当前执行步骤、步骤列表等详细信息。可用于轮询获取任务执行进度。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))),
            @ApiResponse(responseCode = "404", description = "任务不存在"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TaskDto> getTaskStatus(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 查询任务状态: {}", userId, taskId);

        return taskService.getTaskStatus(taskId);
    }

    /**
     * 获取项目任务列表
     */
    @GetMapping("/project/{projectId}/list")
    @Operation(summary = "获取项目任务列表", description = "获取指定项目下的所有异步任务列表。返回任务按创建时间倒序排列，包含任务的基本信息和状态。可用于查看项目下所有的生成任务历史记录。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TaskDto.class)))),
            @ApiResponse(responseCode = "404", description = "项目不存在"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TaskDto>> getProjectTasks(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 查询项目 {} 的任务列表", userId, projectId);

        return taskService.getProjectTasks(projectId);
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "取消任务", description = "取消正在执行或等待中的任务。只能取消状态为pending或running的任务，已完成(completed)、已失败(failed)或已取消(cancelled)的任务无法再次取消。取消后任务状态将变为cancelled。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "任务状态不允许取消"),
            @ApiResponse(responseCode = "404", description = "任务不存在"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> cancelTask(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求取消任务: {}", userId, taskId);

        return taskService.cancelTask(taskId);
    }
}
