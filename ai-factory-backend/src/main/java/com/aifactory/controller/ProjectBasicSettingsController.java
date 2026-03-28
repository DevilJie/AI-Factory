package com.aifactory.controller;

import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.response.Result;
import com.aifactory.service.ProjectBasicSettingsService;
import com.aifactory.vo.SetupStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 项目基础设置Controller
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目基础设置", description = "项目基础设置的查询和保存接口，包括情节结构、叙事风格、阶段节点等配置。这些设置影响AI生成内容的质量和风格。")
public class ProjectBasicSettingsController {

    private final ProjectBasicSettingsService basicSettingsService;

    /**
     * 获取项目基础设置
     */
    @GetMapping("/{projectId}/basic-settings")
    @Operation(
        summary = "获取项目基础设置",
        description = "获取指定项目的基础设置信息，包括情节结构（叙事结构、结局类型、结局基调）、叙事风格（文风调性、写作视角、节奏把控、语言体系、描写侧重）以及复杂结构配置（阶段节点、单章创作设定、伏笔配置）。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回项目基础设置",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限访问该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在或基础设置未初始化")
    })
    public Result<ProjectBasicSettings> getBasicSettings(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId) {
        ProjectBasicSettings settings = basicSettingsService.getByProjectId(projectId);
        return Result.ok(settings);
    }

    /**
     * 保存或更新项目基础设置
     */
    @PostMapping("/{projectId}/basic-settings")
    @Operation(
        summary = "保存项目基础设置",
        description = "保存或更新项目的基础设置。如果是首次保存则创建新记录，否则更新已有记录。设置内容包括：情节结构属性（叙事结构、结局类型、结局基调）、叙事风格属性（文风调性、写作视角、节奏把控、语言体系、描写侧重）以及复杂结构JSON配置（阶段节点、单章创作设定、伏笔与埋线配置）。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "保存成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限修改该项目设置"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<Void> saveBasicSettings(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId,
        @Parameter(description = "项目基础设置信息", required = true)
        @RequestBody ProjectBasicSettings settings
    ) {
        settings.setProjectId(projectId);
        basicSettingsService.saveOrUpdateSettings(settings);
        return Result.ok();
    }

    /**
     * 检查项目设置状态
     */
    @GetMapping("/{projectId}/setup-status")
    @Operation(
        summary = "检查项目设置状态",
        description = "检查项目的设置完成状态，包括当前设置阶段、是否可以访问创作中心、世界观是否锁定、基础设置是否锁定等信息。用于前端判断用户应该进入哪个设置流程。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回设置状态信息",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限访问该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<SetupStatusVO> checkSetupStatus(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId) {
        SetupStatusVO status = basicSettingsService.checkSetupStatus(projectId);
        return Result.ok(status);
    }
}
