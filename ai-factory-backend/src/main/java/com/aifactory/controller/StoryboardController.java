package com.aifactory.controller;

import com.aifactory.dto.GenerateStoryboardRequest;
import com.aifactory.dto.StoryboardVo;
import com.aifactory.response.Result;
import com.aifactory.service.StoryboardService;
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
 * 分镜控制器
 *
 * @Author CaiZy
 * @Date 2025-01-30
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@RestController
@RequestMapping("/api/storyboard")
@Tag(name = "分镜管理", description = "分镜的生成和查询接口。分镜是将章节内容拆分为可视化场景的脚本，包含镜头类型、摄像机角度、运动方式、场景描述、视觉提示词、时长、角色、对话、动作等信息。")
public class StoryboardController {

    @Autowired
    private StoryboardService storyboardService;

    /**
     * 为章节生成分镜
     */
    @PostMapping("/generate")
    @Operation(summary = "为章节生成分镜", description = "根据章节内容使用AI生成分镜脚本。系统会分析章节文本，自动拆分为多个镜头场景，并为每个场景生成详细的视觉描述、镜头参数、角色对话等信息。如果章节已有分镜数据，可通过forceRegenerate参数强制重新生成。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = StoryboardVo.class)))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "章节不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误或AI生成失败")
    })
    public Result<List<StoryboardVo>> generateStoryboard(
            @Parameter(description = "生成分镜请求参数，包含章节ID、项目ID和是否强制重新生成标志", required = true)
            @RequestBody GenerateStoryboardRequest request) {
        try {
            log.info("收到生成分镜请求：chapterId={}, projectId={}",
                request.getChapterId(), request.getProjectId());

            List<StoryboardVo> storyboards = storyboardService.generateStoryboard(request);

            return Result.ok(storyboards);
        } catch (Exception e) {
            log.error("生成分镜失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取章节的分镜列表
     */
    @GetMapping("/list/{chapterId}")
    @Operation(summary = "获取章节分镜列表", description = "获取指定章节的所有分镜数据。返回的分镜列表按sortOrder字段排序，包含每个镜头的详细信息：镜头号、镜头类型、摄像机参数、场景描述、视觉提示词、时长、角色、对话、动作、备注等。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = StoryboardVo.class)))),
            @ApiResponse(responseCode = "404", description = "章节不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<StoryboardVo>> getStoryboardList(
            @Parameter(description = "章节ID", required = true, example = "1")
            @PathVariable Long chapterId) {
        try {
            List<StoryboardVo> storyboards = storyboardService.getStoryboardList(chapterId);
            return Result.ok(storyboards);
        } catch (Exception e) {
            log.error("获取分镜列表失败", e);
            return Result.error(e.getMessage());
        }
    }
}
