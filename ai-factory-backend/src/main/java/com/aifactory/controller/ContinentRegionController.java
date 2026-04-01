package com.aifactory.controller;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.response.Result;
import com.aifactory.service.ContinentRegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大陆区域 Controller
 * 提供地理区域的树形结构 CRUD 操作
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/continent-region")
@Tag(name = "大陆区域", description = "地理区域树形结构管理API，支持无限层级嵌套的区域管理")
public class ContinentRegionController {

    @Autowired
    private ContinentRegionService continentRegionService;

    @Operation(summary = "获取地理区域树", description = "获取指定项目的地理区域树形结构")
    @GetMapping("/tree")
    public Result<List<NovelContinentRegion>> getTree(@PathVariable Long projectId) {
        try {
            return Result.ok(continentRegionService.getTreeByProjectId(projectId));
        } catch (Exception e) {
            log.error("获取地理区域树失败，projectId={}", projectId, e);
            return Result.error("获取地理区域树失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取地理区域列表", description = "获取指定项目的所有地理区域平铺列表")
    @GetMapping("/list")
    public Result<List<NovelContinentRegion>> list(@PathVariable Long projectId) {
        try {
            return Result.ok(continentRegionService.listByProjectId(projectId));
        } catch (Exception e) {
            log.error("获取地理区域列表失败，projectId={}", projectId, e);
            return Result.error("获取地理区域列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增区域节点", description = "新增一个地理区域节点，自动计算层级和排序")
    @PostMapping("/save")
    public Result<NovelContinentRegion> add(@PathVariable Long projectId,
                                            @RequestBody NovelContinentRegion region) {
        try {
            region.setProjectId(projectId);
            return Result.ok(continentRegionService.addRegion(region));
        } catch (Exception e) {
            log.error("新增区域失败，projectId={}", projectId, e);
            return Result.error("新增区域失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新区域节点", description = "更新地理区域节点的名称、描述等信息")
    @PutMapping("/update")
    public Result<NovelContinentRegion> update(@PathVariable Long projectId,
                                               @RequestBody NovelContinentRegion region) {
        try {
            region.setProjectId(projectId);
            return Result.ok(continentRegionService.updateRegion(region));
        } catch (Exception e) {
            log.error("更新区域失败，projectId={}", projectId, e);
            return Result.error("更新区域失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除区域节点", description = "删除地理区域节点及其所有子节点（级联删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            continentRegionService.deleteRegion(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除区域失败，projectId={}, id={}", projectId, id, e);
            return Result.error("删除区域失败: " + e.getMessage());
        }
    }
}
