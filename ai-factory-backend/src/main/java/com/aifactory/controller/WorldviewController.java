package com.aifactory.controller;

import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.dto.TaskDto;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.Project;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.response.Result;
import com.aifactory.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private TaskService taskService;

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

}
