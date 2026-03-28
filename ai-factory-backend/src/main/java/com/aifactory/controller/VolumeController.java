package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.UpdateVolumeRequest;
import com.aifactory.dto.VolumePlanDto;
import com.aifactory.response.Result;
import com.aifactory.service.ChapterService;
import com.aifactory.service.VolumeService;
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

import java.util.List;

/**
 * 分卷控制器
 *
 * @Author CaiZy
 * @Date 2025-01-23
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/volumes")
@Tag(name = "分卷管理", description = "小说项目分卷的创建、查询、更新、删除等操作接口。分卷是小说的结构单位，用于组织章节和规划情节走向。")
public class VolumeController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private VolumeService volumeService;

    /**
     * 获取分卷列表（从大纲规划中获取）
     */
    @GetMapping
    @Operation(
        summary = "获取分卷列表",
        description = "获取指定项目的所有分卷列表，包含分卷的详细信息和章节规划。分卷信息从大纲规划中提取，包含卷号、标题、主题、冲突、情节走向、预估字数等。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功，返回分卷列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限访问该项目"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<List<VolumePlanDto>> getVolumeList(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的分卷列表", userId, projectId);

        List<VolumePlanDto> volumes = chapterService.getVolumeList(projectId);
        return Result.ok(volumes);
    }

    /**
     * 更新分卷规划
     */
    @PutMapping("/{volumeId}")
    @Operation(
        summary = "更新分卷规划",
        description = "更新指定分卷的规划信息，包括分卷标题、主题、主要冲突、情节走向、预估字数、目标章节数、本卷新增人物、阶段伏笔等。只需要传入需要更新的字段。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功，返回更新后的分卷信息",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限更新该分卷"),
        @ApiResponse(responseCode = "404", description = "分卷不存在")
    })
    public Result<VolumePlanDto> updateVolume(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId,
        @Parameter(description = "分卷ID", required = true, example = "1")
        @PathVariable String volumeId,
        @Parameter(description = "分卷更新信息", required = true)
        @RequestBody UpdateVolumeRequest request) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新项目 {} 的分卷 {}", userId, projectId, volumeId);

        VolumePlanDto updatedVolume = volumeService.updateVolume(projectId, volumeId, request);
        return Result.ok(updatedVolume);
    }

    /**
     * 创建分卷（临时实现）
     * TODO: 实现完整的分卷管理功能
     */
    @PostMapping
    @Operation(
        summary = "创建分卷",
        description = "在指定项目中创建新的分卷。注意：此接口为临时实现，完整功能待后续开发。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限在该项目中创建分卷"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<String> createVolume(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId,
        @Parameter(description = "分卷创建数据（临时）")
        @RequestBody Object data) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 在项目 {} 中创建分卷", userId, projectId);

        // 暂时返回成功，后续实现分卷功能
        return Result.ok("创建成功");
    }

    /**
     * 删除分卷（临时实现）
     * TODO: 实现完整的分卷管理功能
     */
    @DeleteMapping("/{volumeId}")
    @Operation(
        summary = "删除分卷",
        description = "删除指定的分卷。注意：此接口为临时实现，完整功能待后续开发。删除分卷可能会影响关联的章节，请谨慎操作。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限删除该分卷"),
        @ApiResponse(responseCode = "404", description = "分卷不存在")
    })
    public Result<String> deleteVolume(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId,
        @Parameter(description = "分卷ID", required = true, example = "1")
        @PathVariable String volumeId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除项目 {} 的分卷 {}", userId, projectId, volumeId);

        // 暂时返回成功，后续实现分卷功能
        return Result.ok("删除成功");
    }

    /**
     * 更新分卷排序（临时实现）
     * TODO: 实现完整的分卷管理功能
     */
    @PutMapping("/order")
    @Operation(
        summary = "更新分卷排序",
        description = "批量更新分卷的显示顺序。注意：此接口为临时实现，完整功能待后续开发。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "排序更新成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或token无效"),
        @ApiResponse(responseCode = "403", description = "无权限更新该项目的分卷排序"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<String> updateVolumeOrder(
        @Parameter(description = "项目ID", required = true, example = "1234567890")
        @PathVariable Long projectId,
        @Parameter(description = "分卷排序数据（临时）")
        @RequestBody Object data) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新项目 {} 的分卷排序", userId, projectId);

        // 暂时返回成功，后续实现分卷功能
        return Result.ok("排序更新成功");
    }
}
