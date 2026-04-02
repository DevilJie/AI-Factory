package com.aifactory.controller;

import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelFactionCharacter;
import com.aifactory.entity.NovelFactionRegion;
import com.aifactory.entity.NovelFactionRelation;
import com.aifactory.mapper.NovelFactionCharacterMapper;
import com.aifactory.mapper.NovelFactionRegionMapper;
import com.aifactory.mapper.NovelFactionRelationMapper;
import com.aifactory.response.Result;
import com.aifactory.service.FactionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 势力管理 Controller
 * 提供势力树形结构 CRUD 和关联表管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/faction")
@Tag(name = "势力管理", description = "势力树形结构管理API")
public class FactionController {

    @Autowired
    private FactionService factionService;

    @Autowired
    private NovelFactionRegionMapper factionRegionMapper;

    @Autowired
    private NovelFactionCharacterMapper factionCharacterMapper;

    @Autowired
    private NovelFactionRelationMapper factionRelationMapper;

    // ==================== 势力树 CRUD ====================

    @Operation(summary = "获取势力树", description = "获取指定项目的势力树形结构")
    @GetMapping("/tree")
    public Result<List<NovelFaction>> getTree(@PathVariable Long projectId) {
        try {
            return Result.ok(factionService.getTreeByProjectId(projectId));
        } catch (Exception e) {
            log.error("获取势力树失败，projectId={}", projectId, e);
            return Result.error("获取势力树失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取势力列表", description = "获取指定项目的所有势力平铺列表")
    @GetMapping("/list")
    public Result<List<NovelFaction>> list(@PathVariable Long projectId) {
        try {
            return Result.ok(factionService.listByProjectId(projectId));
        } catch (Exception e) {
            log.error("获取势力列表失败，projectId={}", projectId, e);
            return Result.error("获取势力列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增势力节点", description = "新增一个势力节点，自动计算层级和排序")
    @PostMapping("/save")
    public Result<NovelFaction> add(@PathVariable Long projectId,
                                    @RequestBody NovelFaction faction) {
        try {
            faction.setProjectId(projectId);
            return Result.ok(factionService.addFaction(faction));
        } catch (Exception e) {
            log.error("新增势力失败，projectId={}", projectId, e);
            return Result.error("新增势力失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新势力节点", description = "更新势力节点的名称、描述等信息")
    @PutMapping("/update")
    public Result<NovelFaction> update(@PathVariable Long projectId,
                                       @RequestBody NovelFaction faction) {
        try {
            faction.setProjectId(projectId);
            return Result.ok(factionService.updateFaction(faction));
        } catch (Exception e) {
            log.error("更新势力失败，projectId={}", projectId, e);
            return Result.error("更新势力失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除势力节点", description = "删除势力节点及其所有子节点（级联删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            factionService.deleteFaction(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除势力失败，projectId={}, id={}", projectId, id, e);
            return Result.error("删除势力失败: " + e.getMessage());
        }
    }

    // ==================== 势力-地区关联 ====================

    @Operation(summary = "获取势力关联地区")
    @GetMapping("/{factionId}/regions")
    public Result<List<NovelFactionRegion>> listRegions(@PathVariable Long projectId,
                                                        @PathVariable Long factionId) {
        try {
            List<NovelFactionRegion> list = factionRegionMapper.selectList(
                    new LambdaQueryWrapper<NovelFactionRegion>()
                            .eq(NovelFactionRegion::getFactionId, factionId));
            return Result.ok(list);
        } catch (Exception e) {
            log.error("获取势力关联地区失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("获取势力关联地区失败: " + e.getMessage());
        }
    }

    @Operation(summary = "添加势力地区关联")
    @PostMapping("/{factionId}/regions")
    public Result<NovelFactionRegion> addRegion(@PathVariable Long projectId,
                                                @PathVariable Long factionId,
                                                @RequestBody NovelFactionRegion association) {
        try {
            association.setFactionId(factionId);
            factionRegionMapper.insert(association);
            return Result.ok(association);
        } catch (Exception e) {
            log.error("添加势力地区关联失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("添加势力地区关联失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除势力地区关联")
    @DeleteMapping("/{factionId}/regions/{id}")
    public Result<Void> deleteRegion(@PathVariable Long projectId,
                                     @PathVariable Long factionId,
                                     @PathVariable Long id) {
        try {
            factionRegionMapper.deleteById(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除势力地区关联失败，projectId={}, factionId={}, id={}", projectId, factionId, id, e);
            return Result.error("删除势力地区关联失败: " + e.getMessage());
        }
    }

    // ==================== 势力-人物关联 ====================

    @Operation(summary = "获取势力关联人物")
    @GetMapping("/{factionId}/characters")
    public Result<List<NovelFactionCharacter>> listCharacters(@PathVariable Long projectId,
                                                              @PathVariable Long factionId) {
        try {
            List<NovelFactionCharacter> list = factionCharacterMapper.selectList(
                    new LambdaQueryWrapper<NovelFactionCharacter>()
                            .eq(NovelFactionCharacter::getFactionId, factionId));
            return Result.ok(list);
        } catch (Exception e) {
            log.error("获取势力关联人物失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("获取势力关联人物失败: " + e.getMessage());
        }
    }

    @Operation(summary = "添加势力人物关联")
    @PostMapping("/{factionId}/characters")
    public Result<NovelFactionCharacter> addCharacter(@PathVariable Long projectId,
                                                      @PathVariable Long factionId,
                                                      @RequestBody NovelFactionCharacter association) {
        try {
            association.setFactionId(factionId);
            factionCharacterMapper.insert(association);
            return Result.ok(association);
        } catch (Exception e) {
            log.error("添加势力人物关联失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("添加势力人物关联失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除势力人物关联")
    @DeleteMapping("/{factionId}/characters/{id}")
    public Result<Void> deleteCharacter(@PathVariable Long projectId,
                                        @PathVariable Long factionId,
                                        @PathVariable Long id) {
        try {
            factionCharacterMapper.deleteById(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除势力人物关联失败，projectId={}, factionId={}, id={}", projectId, factionId, id, e);
            return Result.error("删除势力人物关联失败: " + e.getMessage());
        }
    }

    // ==================== 势力关系 ====================

    @Operation(summary = "获取势力关系列表")
    @GetMapping("/{factionId}/relations")
    public Result<List<NovelFactionRelation>> listRelations(@PathVariable Long projectId,
                                                            @PathVariable Long factionId) {
        try {
            List<NovelFactionRelation> list = factionRelationMapper.selectList(
                    new LambdaQueryWrapper<NovelFactionRelation>()
                            .eq(NovelFactionRelation::getFactionId, factionId));
            return Result.ok(list);
        } catch (Exception e) {
            log.error("获取势力关系列表失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("获取势力关系列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "添加势力关系")
    @PostMapping("/{factionId}/relations")
    public Result<NovelFactionRelation> addRelation(@PathVariable Long projectId,
                                                    @PathVariable Long factionId,
                                                    @RequestBody NovelFactionRelation relation) {
        try {
            relation.setFactionId(factionId);
            factionRelationMapper.insert(relation);
            return Result.ok(relation);
        } catch (Exception e) {
            log.error("添加势力关系失败，projectId={}, factionId={}", projectId, factionId, e);
            return Result.error("添加势力关系失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除势力关系")
    @DeleteMapping("/{factionId}/relations/{id}")
    public Result<Void> deleteRelation(@PathVariable Long projectId,
                                       @PathVariable Long factionId,
                                       @PathVariable Long id) {
        try {
            factionRelationMapper.deleteById(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除势力关系失败，projectId={}, factionId={}, id={}", projectId, factionId, id, e);
            return Result.error("删除势力关系失败: " + e.getMessage());
        }
    }
}
