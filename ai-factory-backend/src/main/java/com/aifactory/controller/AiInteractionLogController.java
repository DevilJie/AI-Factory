package com.aifactory.controller;

import com.aifactory.dto.AiInteractionLogDTO;
import com.aifactory.service.AiInteractionLogService;
import com.aifactory.response.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * AI交互日志Controller
 *
 * 提供AI交互日志的查询和管理功能，用于追踪和审计所有AI调用记录
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Tag(name = "AI交互日志", description = "AI交互日志管理接口，用于追踪和审计所有AI调用记录，支持日志查询和统计分析")
@Slf4j
@RestController
@RequestMapping("/api/ai-interaction-log")
public class AiInteractionLogController {

    @Autowired
    private AiInteractionLogService aiInteractionLogService;

    /**
     * 根据traceId查询日志详情
     */
    @Operation(
        summary = "根据traceId查询日志详情",
        description = "根据交互流水号(traceId)查询完整的AI交互日志详情，" +
                      "包含请求提示词、响应内容、Token消耗、耗时等完整信息。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AiInteractionLogDTO.class))),
        @ApiResponse(responseCode = "404", description = "日志不存在")
    })
    @GetMapping("/trace/{traceId}")
    public Result<AiInteractionLogDTO> getByTraceId(
            @Parameter(description = "交互流水号，用于唯一标识一次AI交互",
                      required = true,
                      example = "trace_20250203_abc123def")
            @PathVariable String traceId) {
        AiInteractionLogDTO log = aiInteractionLogService.getByTraceId(traceId);
        if (log == null) {
            return Result.error("日志不存在，traceId: " + traceId);
        }
        return Result.ok(log);
    }

    /**
     * 分页查询日志列表
     */
    @Operation(
        summary = "分页查询AI交互日志列表",
        description = "分页查询AI交互日志列表，支持按项目ID、请求类型、提供商、成功状态等条件筛选。" +
                      "返回的日志列表包含请求预览和响应预览（前500字符），不包含完整内容。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public Result<Page<AiInteractionLogDTO>> queryLogs(
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量，默认20条", example = "20")
            @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "项目ID，用于筛选指定项目的日志", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "请求类型，如chapter_generate、outline_generate、worldview_generate等",
                      example = "chapter_generate")
            @RequestParam(required = false) String requestType,
            @Parameter(description = "AI提供商名称，如openai、anthropic、zhipu等",
                      example = "openai")
            @RequestParam(required = false) String provider,
            @Parameter(description = "是否成功：true-成功，false-失败", example = "true")
            @RequestParam(required = false) Boolean isSuccess
    ) {
        Page<AiInteractionLogDTO> page = aiInteractionLogService.queryLogs(
                pageNum, pageSize, projectId, requestType, provider, isSuccess
        );
        return Result.ok(page);
    }

    /**
     * 生成新的traceId
     */
    @Operation(
        summary = "生成新的traceId",
        description = "生成一个新的交互流水号(traceId)，用于在发起AI请求前预生成流水号，" +
                      "便于在客户端进行日志追踪和关联。格式为：trace_yyyyMMdd_UUID。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "string", example = "trace_20250203_abc123def")))
    })
    @GetMapping("/generate-trace-id")
    public Result<String> generateTraceId() {
        String traceId = aiInteractionLogService.generateTraceId();
        return Result.ok(traceId);
    }
}
