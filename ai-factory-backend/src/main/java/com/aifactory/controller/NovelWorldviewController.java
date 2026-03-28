package com.aifactory.controller;

import com.aifactory.entity.NovelWorldview;
import com.aifactory.response.Result;
import com.aifactory.service.NovelWorldviewService;
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

/**
 * 世界观设定Controller
 * 提供世界观设定的基础CRUD操作，支持按项目ID或大纲ID查询
 *
 * @Author CaiZy
 * @Date 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/api/worldview")
@Tag(name = "世界观设定", description = "世界观设定管理API，提供世界观的查询、保存和删除功能，支持按项目ID或大纲ID进行查询")
public class NovelWorldviewController {

    @Autowired
    private NovelWorldviewService worldviewService;

    /**
     * 根据项目ID获取世界观设定
     */
    @Operation(
            summary = "根据项目ID获取世界观设定",
            description = "根据项目ID查询关联的世界观设定信息，包括世界类型、背景描述、力量体系、地理环境、势力分布、时间线和世界规则等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取世界观设定，如果不存在则返回null",
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/project/{projectId}")
    public Result<NovelWorldview> getByProjectId(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        try {
            log.info("查询项目 {} 的世界观设定", projectId);
            NovelWorldview worldview = worldviewService.getByProjectId(projectId);
            // 没有世界观设定是正常情况，返回 ok(null) 而不是 error
            return Result.ok(worldview);
        } catch (Exception e) {
            log.error("查询世界观设定失败", e);
            return Result.error("查询世界观设定失败: " + e.getMessage());
        }
    }

    /**
     * 根据大纲ID获取世界观设定
     */
    @Operation(
            summary = "根据大纲ID获取世界观设定",
            description = "根据大纲ID查询关联的世界观设定信息。某些场景下世界观可能与特定大纲版本关联"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取世界观设定，如果不存在则返回null",
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/outline/{outlineId}")
    public Result<NovelWorldview> getByOutlineId(
            @Parameter(description = "大纲ID", required = true, example = "1")
            @PathVariable Long outlineId) {
        try {
            log.info("查询大纲 {} 的世界观设定", outlineId);
            NovelWorldview worldview = worldviewService.getByOutlineId(outlineId);
            // 没有世界观设定是正常情况，返回 ok(null) 而不是 error
            return Result.ok(worldview);
        } catch (Exception e) {
            log.error("查询世界观设定失败", e);
            return Result.error("查询世界观设定失败: " + e.getMessage());
        }
    }

    /**
     * 保存或更新世界观设定
     */
    @Operation(
            summary = "保存或更新世界观设定",
            description = "保存新的世界观设定或更新已有设定。如果请求中包含有效ID则更新，否则创建新记录。可设置世界类型、背景描述、力量体系、地理环境、势力分布、时间线和世界规则等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "保存成功，返回世界观设定",
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))),
            @ApiResponse(responseCode = "500", description = "保存失败")
    })
    @PostMapping("/save")
    public Result<NovelWorldview> save(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "世界观设定内容",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NovelWorldview.class))
            )
            @RequestBody NovelWorldview worldview) {
        try {
            log.info("保存世界观设定，项目ID: {}", worldview.getProjectId());
            NovelWorldview saved = worldviewService.saveOrUpdate(worldview);
            return Result.ok(saved);
        } catch (Exception e) {
            log.error("保存世界观设定失败", e);
            return Result.error("保存世界观设定失败: " + e.getMessage());
        }
    }

    /**
     * 删除世界观设定
     */
    @Operation(
            summary = "删除世界观设定",
            description = "根据世界观设定ID删除指定记录。注意：此操作不可逆，删除后无法恢复"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public Result<String> delete(
            @Parameter(description = "世界观设定ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            log.info("删除世界观设定，ID: {}", id);
            worldviewService.deleteById(id);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除世界观设定失败", e);
            return Result.error("删除世界观设定失败: " + e.getMessage());
        }
    }
}
