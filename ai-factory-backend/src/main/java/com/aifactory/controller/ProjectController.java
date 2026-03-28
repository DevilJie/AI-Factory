package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.DashboardStatsDto;
import com.aifactory.dto.ProjectCreateDto;
import com.aifactory.dto.ProjectQueryDto;
import com.aifactory.dto.ProjectUpdateDto;
import com.aifactory.dto.ProjectOverviewVO;
import com.aifactory.dto.ProjectVo;
import com.aifactory.response.Result;
import com.aifactory.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 项目控制器
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@RestController
@RequestMapping("/api/project")
@Tag(name = "项目管理", description = "项目的创建、查询、更新、删除等操作接口")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 创建项目
     */
    @PostMapping("/create")
    @Operation(
        summary = "创建项目",
        description = "创建一个新的小说或视频项目。创建时会初始化项目的基础信息，包括名称、描述、类型、故事基调等。返回新创建的项目ID。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功，返回项目ID",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "400", description = "参数验证失败，如项目名称或类型为空"),
        @ApiResponse(responseCode = "401", description = "未登录或token无效")
    })
    public Result<String> createProject(
        @Parameter(description = "项目创建信息", required = true)
        @Valid @RequestBody ProjectCreateDto createDto) {
        String projectId = projectService.createProject(createDto, UserContext.getUserId());
        return Result.ok(projectId);
    }

    /**
     * 更新项目
     */
    @PutMapping("/update")
    @Operation(
        summary = "更新项目",
        description = "更新项目的基本信息，包括名称、描述、故事基调、故事类型、视觉风格、封面图、状态和标签等。只需要传入需要更新的字段。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "400", description = "参数验证失败，如项目ID为空"),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限更新该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<String> updateProject(
        @Parameter(description = "项目更新信息", required = true)
        @Valid @RequestBody ProjectUpdateDto updateDto) {
        projectService.updateProject(updateDto, UserContext.getUserId());
        return Result.ok("更新成功");
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/delete/{projectId}")
    @Operation(
        summary = "删除项目",
        description = "根据项目ID删除项目。这是一个逻辑删除操作，项目数据不会被物理删除。删除前会验证用户是否有权限删除该项目。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限删除该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<String> deleteProject(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable String projectId) {
        projectService.deleteProject(projectId, UserContext.getUserId());
        return Result.ok("删除成功");
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/detail/{projectId}")
    @Operation(
        summary = "获取项目详情",
        description = "根据项目ID获取项目的完整信息，包括基本信息、统计数据（章节数量、总字数/时长）等。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回项目详情",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限访问该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<ProjectVo> getProjectDetail(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable String projectId) {
        ProjectVo projectVo = projectService.getProjectDetail(projectId, UserContext.getUserId());
        return Result.ok(projectVo);
    }

    /**
     * 查询项目列表
     */
    @GetMapping("/list")
    @Operation(
        summary = "查询项目列表",
        description = "分页查询当前用户的项目列表。支持按项目类型、状态筛选，支持按项目名称关键词搜索。返回分页结果，包含项目基本信息和统计数据。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功，返回分页项目列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效")
    })
    public Result<IPage<ProjectVo>> getProjectList(
        @Parameter(description = "项目类型：video(视频项目)/novel(小说项目)", example = "novel")
        @RequestParam(required = false) String projectType,
        @Parameter(description = "项目状态：draft(草稿)/in_progress(进行中)/completed(已完成)/archived(已归档)", example = "in_progress")
        @RequestParam(required = false) String status,
        @Parameter(description = "搜索关键词（项目名称模糊匹配）", example = "玄幻")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "页码，从1开始", example = "1")
        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
        @Parameter(description = "每页数量，默认10条", example = "10")
        @RequestParam(required = false, defaultValue = "10") Integer pageSize,
        @Parameter(description = "排序字段：createTime(创建时间)/updateTime(更新时间)", example = "updateTime")
        @RequestParam(required = false) String sortBy,
        @Parameter(description = "排序方向：asc(升序)/desc(降序)", example = "desc")
        @RequestParam(required = false) String sortOrder) {
        ProjectQueryDto queryDto = new ProjectQueryDto();
        queryDto.setProjectType(projectType);
        queryDto.setStatus(status);
        queryDto.setKeyword(keyword);
        queryDto.setPageNum(pageNum);
        queryDto.setPageSize(pageSize);
        queryDto.setSortBy(sortBy);
        queryDto.setSortOrder(sortOrder);
        IPage<ProjectVo> projectPage = projectService.getProjectList(queryDto, UserContext.getUserId());
        return Result.ok(projectPage);
    }

    /**
     * 获取Dashboard统计数据
     */
    @GetMapping("/stats")
    @Operation(
        summary = "获取Dashboard统计数据",
        description = "获取当前用户的Dashboard统计数据，包括项目总数、章节总数、角色总数。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回统计数据",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效")
    })
    public Result<DashboardStatsDto> getDashboardStats() {
        DashboardStatsDto stats = projectService.getDashboardStats(UserContext.getUserId());
        return Result.ok(stats);
    }

    /**
     * 获取项目概览数据
     */
    @GetMapping("/{projectId}/overview")
    @Operation(
        summary = "获取项目概览数据",
        description = "获取项目的概览信息，包括基础信息、统计数据（章节数、角色数、总字数）和创作进度。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回项目概览数据",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限访问该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<ProjectOverviewVO> getProjectOverview(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable String projectId) {
        ProjectOverviewVO overview = projectService.getProjectOverview(projectId, UserContext.getUserId());
        return Result.ok(overview);
    }
}
