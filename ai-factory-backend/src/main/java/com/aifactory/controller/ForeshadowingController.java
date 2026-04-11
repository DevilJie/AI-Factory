package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.ForeshadowingCreateDto;
import com.aifactory.dto.ForeshadowingDto;
import com.aifactory.dto.ForeshadowingQueryDto;
import com.aifactory.dto.ForeshadowingUpdateDto;
import com.aifactory.entity.Foreshadowing;
import com.aifactory.response.Result;
import com.aifactory.service.ForeshadowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 伏笔控制器
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/foreshadowings")
@Tag(name = "伏笔管理", description = "小说伏笔的增删改查和状态管理接口，支持伏笔的创建、查询、更新、删除以及填坑状态标记")
public class ForeshadowingController {

    @Autowired
    private ForeshadowingService foreshadowingService;

    /**
     * 获取伏笔列表
     */
    @GetMapping
    @Operation(
            summary = "获取伏笔列表",
            description = "获取指定项目的伏笔列表，支持按类型、布局类型、状态和章节进行筛选。" +
                    "伏笔类型(type): character(人物)、item(物品)、event(事件)、secret(秘密)。" +
                    "布局类型(layoutType): bright1(明线1)、bright2(明线2)、bright3(明线3)、dark(暗线)。" +
                    "状态(status): pending(未填回)、in_progress(进行中)、completed(已填回)。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForeshadowingDto.class))),
            @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<List<ForeshadowingDto>> getForeshadowingList(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔类型：character(人物)/item(物品)/event(事件)/secret(秘密)", example = "character")
            @RequestParam(required = false) String type,
            @Parameter(description = "布局类型：bright1(明线1)/bright2(明线2)/bright3(明线3)/dark(暗线)", example = "dark")
            @RequestParam(required = false) String layoutType,
            @Parameter(description = "状态：pending(未填回)/in_progress(进行中)/completed(已填回)", example = "pending")
            @RequestParam(required = false) String status,
            @Parameter(description = "当前章节号，用于查询该章节需要填坑的伏笔", example = "10")
            @RequestParam(required = false) Integer currentChapter,
            @Parameter(description = "埋设伏笔的章节号筛选", example = "5")
            @RequestParam(required = false) Integer plantedChapter,
            @Parameter(description = "计划回收伏笔的章节号筛选", example = "50")
            @RequestParam(required = false) Integer plannedCallbackChapter,
            @Parameter(description = "埋设伏笔的分卷编号筛选", example = "1")
            @RequestParam(required = false) Integer plantedVolume,
            @Parameter(description = "计划回收伏笔的分卷编号筛选", example = "2")
            @RequestParam(required = false) Integer plannedCallbackVolume
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的伏笔列表", userId, projectId);

        ForeshadowingQueryDto queryDto = new ForeshadowingQueryDto();
        queryDto.setProjectId(projectId);
        queryDto.setType(type);
        queryDto.setLayoutType(layoutType);
        queryDto.setStatus(status);
        queryDto.setCurrentChapter(currentChapter);
        queryDto.setPlantedChapter(plantedChapter);
        queryDto.setPlannedCallbackChapter(plannedCallbackChapter);
        queryDto.setPlantedVolume(plantedVolume);
        queryDto.setPlannedCallbackVolume(plannedCallbackVolume);

        List<ForeshadowingDto> foreshadowings = foreshadowingService.getForeshadowingList(queryDto);
        return Result.ok(foreshadowings);
    }

    /**
     * 获取伏笔详情
     */
    @GetMapping("/{foreshadowingId}")
    @Operation(
            summary = "获取伏笔详情",
            description = "根据伏笔ID获取伏笔的详细信息，包括伏笔标题、类型、描述、布局类型、章节关联和状态等。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Foreshadowing.class))),
            @ApiResponse(responseCode = "404", description = "伏笔不存在")
    })
    public Result<Foreshadowing> getForeshadowingDetail(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔ID", required = true, example = "100")
            @PathVariable Long foreshadowingId
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取伏笔 {} 详情", userId, foreshadowingId);

        Foreshadowing foreshadowing = foreshadowingService.getForeshadowingDetail(foreshadowingId);
        return Result.ok(foreshadowing);
    }

    /**
     * 创建伏笔
     */
    @PostMapping
    @Operation(
            summary = "创建伏笔",
            description = "在指定项目中创建新的伏笔。伏笔可以是人物、物品、事件或秘密类型，" +
                    "需要指定埋伏笔的章节，可选设置计划填坑章节、优先级和备注。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功，返回新伏笔的ID"),
            @ApiResponse(responseCode = "400", description = "请求参数验证失败")
    })
    public Result<Long> createForeshadowing(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔创建信息", required = true)
            @Valid @RequestBody ForeshadowingCreateDto createDto
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 在项目 {} 中创建伏笔: {}", userId, projectId, createDto.getTitle());

        Long foreshadowingId = foreshadowingService.createForeshadowing(projectId, createDto);
        return Result.ok(foreshadowingId);
    }

    /**
     * 更新伏笔
     */
    @PutMapping("/{foreshadowingId}")
    @Operation(
            summary = "更新伏笔",
            description = "更新指定伏笔的信息。可更新的字段包括：标题、类型、描述、布局类型、" +
                    "计划填坑章节、实际填坑章节、状态、优先级和备注。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数验证失败"),
            @ApiResponse(responseCode = "404", description = "伏笔不存在")
    })
    public Result<String> updateForeshadowing(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔ID", required = true, example = "100")
            @PathVariable Long foreshadowingId,
            @Parameter(description = "伏笔更新信息", required = true)
            @Valid @RequestBody ForeshadowingUpdateDto updateDto
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新伏笔 {}", userId, foreshadowingId);

        foreshadowingService.updateForeshadowing(foreshadowingId, updateDto);
        return Result.ok("更新成功");
    }

    /**
     * 删除伏笔
     */
    @DeleteMapping("/{foreshadowingId}")
    @Operation(
            summary = "删除伏笔",
            description = "删除指定的伏笔。此操作为逻辑删除，伏笔数据不会被物理删除。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "伏笔不存在")
    })
    public Result<String> deleteForeshadowing(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔ID", required = true, example = "100")
            @PathVariable Long foreshadowingId
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除伏笔 {}", userId, foreshadowingId);

        foreshadowingService.deleteForeshadowing(foreshadowingId);
        return Result.ok("删除成功");
    }

    /**
     * 标记伏笔为已填回
     */
    @PostMapping("/{foreshadowingId}/complete")
    @Operation(
            summary = "标记伏笔为已填回",
            description = "将指定伏笔标记为已完成填坑状态，并记录实际填坑的章节号。" +
                    "此操作会更新伏笔状态为completed，并设置actualCallbackChapter字段。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，缺少填坑章节"),
            @ApiResponse(responseCode = "404", description = "伏笔不存在")
    })
    public Result<String> markAsCompleted(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "伏笔ID", required = true, example = "100")
            @PathVariable Long foreshadowingId,
            @Parameter(description = "实际填坑的章节号", required = true, example = "50")
            @RequestParam Integer actualCallbackChapter
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 标记伏笔 {} 为已填回，填坑章节: {}", userId, foreshadowingId, actualCallbackChapter);

        foreshadowingService.markAsCompleted(foreshadowingId, actualCallbackChapter);
        return Result.ok("标记成功");
    }

    /**
     * 获取填坑统计数据
     */
    @GetMapping("/stats")
    @Operation(
            summary = "获取伏笔统计数据",
            description = "获取指定项目的伏笔统计信息，包括各状态（未填回、进行中、已填回）的伏笔数量、" +
                    "各类型（人物、物品、事件、秘密）的伏笔数量以及填坑率等统计数据。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<ForeshadowingService.ForeshadowingStats> getForeshadowingStats(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的伏笔统计", userId, projectId);

        ForeshadowingService.ForeshadowingStats stats =
                foreshadowingService.getForeshadowingStats(projectId);
        return Result.ok(stats);
    }
}
