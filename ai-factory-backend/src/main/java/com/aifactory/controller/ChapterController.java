package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.ChapterAiFixRequest;
import com.aifactory.dto.ChapterAiFixResponse;
import com.aifactory.dto.ChapterAiPolishRequest;
import com.aifactory.dto.ChapterAiPolishResponse;
import com.aifactory.dto.ChapterDto;
import com.aifactory.dto.ChapterPlanUpdateRequest;
import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.dto.TaskDto;
import com.aifactory.response.Result;
import com.aifactory.service.ChapterService;
import com.aifactory.service.TaskService;
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

import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelCharacterChapter;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.service.NovelCharacterChapterService;
import com.aifactory.vo.ChapterCharacterVO;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 章节控制器
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/chapters")
@Tag(name = "章节管理", description = "章节的增删改查、AI生成、剧情修复、润色等操作接口")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NovelCharacterChapterService characterChapterService;

    @Autowired
    private NovelCharacterMapper characterMapper;

    /**
     * 获取章节列表
     */
    @GetMapping
    @Operation(
        summary = "获取章节列表",
        description = "获取指定项目下的所有章节列表，按章节序号排序返回。返回内容包括章节ID、标题、字数、状态等基本信息。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取章节列表",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<List<ChapterDto>> getChapterList(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的章节列表", userId, projectId);

        List<ChapterDto> chapters = chapterService.getChapterList(projectId);
        return Result.ok(chapters);
    }

    /**
     * 获取章节树（带分卷）
     * 注意：必须放在 /{chapterId} 之前，否则 tree 会被当作 chapterId 解析
     */
    @GetMapping("/tree")
    @Operation(
        summary = "获取章节树",
        description = "获取项目的章节树形结构，包含分卷信息。用于在章节管理页面展示带分卷层级结构的章节列表。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取章节树",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<List<ChapterDto>> getChapterTree(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的章节树", userId, projectId);

        List<ChapterDto> chapters = chapterService.getChapterList(projectId);
        // TODO: 后续可以返回带分卷结构的树形数据
        return Result.ok(chapters);
    }

    /**
     * 获取大纲中的章节规划（带分卷信息）
     * 用于在章节管理页面显示AI规划的章节结构
     */
    @GetMapping("/plans")
    @Operation(
        summary = "获取章节规划列表",
        description = "获取大纲中的章节规划数据，包含分卷信息、章节目标字数、情节大纲等。用于在章节管理页面显示AI规划的章节结构，支持查看哪些规划已生成实际内容。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取章节规划列表",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<List<com.aifactory.dto.ChapterPlanDto>> getChapterPlans(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的章节规划", userId, projectId);

        List<com.aifactory.dto.ChapterPlanDto> plans = chapterService.getChapterPlans(projectId);
        return Result.ok(plans);
    }

    /**
     * 获取章节详情
     */
    @GetMapping("/{chapterId}")
    @Operation(
        summary = "获取章节详情",
        description = "获取指定章节的完整信息，包括章节内容、字数统计、状态、所属分卷等详细信息。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取章节详情",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在")
    })
    public Result<ChapterDto> getChapterDetail(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取章节 {} 详情", userId, chapterId);

        ChapterDto chapter = chapterService.getChapterDetail(chapterId);
        return Result.ok(chapter);
    }

    /**
     * 获取章节实际登场角色列表
     * 用于前端对比视图，展示章节中实际出场的角色
     */
    @GetMapping("/{chapterId}/characters")
    @Operation(
        summary = "获取章节实际登场角色",
        description = "获取指定章节中实际出场的角色列表，包含角色ID、名称和重要程度。用于与规划角色进行对比。"
    )
    public Result<List<ChapterCharacterVO>> getChapterCharacters(
            @PathVariable Long projectId,
            @PathVariable Long chapterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取章节 {} 的实际登场角色列表", userId, chapterId);

        List<NovelCharacterChapter> relations = characterChapterService.getCharactersByChapterId(chapterId);

        List<ChapterCharacterVO> result = relations.stream().map(rel -> {
            NovelCharacter character = characterMapper.selectById(rel.getCharacterId());
            return ChapterCharacterVO.builder()
                .characterId(rel.getCharacterId())
                .characterName(character != null ? character.getName() : "未知角色")
                .roleType(rel.getImportanceLevel())
                .importanceLevel(rel.getImportanceLevel())
                .build();
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    /**
     * 根据章节规划ID获取章节详情
     * 用于前端点击章节规划时获取对应的章节内容
     */
    @GetMapping("/by-plan/{planId}")
    @Operation(
        summary = "根据规划ID获取章节详情",
        description = "根据章节规划ID获取对应的实际章节内容。如果章节尚未生成，返回null。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取章节详情（可能为null表示未生成）",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录")
    })
    public Result<ChapterDto> getChapterByPlanId(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节规划ID，必须为有效的规划主键", required = true, example = "1")
            @PathVariable Long planId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 根据规划ID {} 获取章节详情", userId, planId);

        ChapterDto chapter = chapterService.getChapterByPlanId(projectId, planId);
        return Result.ok(chapter);
    }

    /**
     * 更新章节规划
     */
    @PutMapping("/plan/{planId}")
    @Operation(
        summary = "更新章节规划",
        description = "更新指定章节规划的信息。支持部分更新，只传入需要修改的字段即可。可更新标题、情节大纲、场景、目标字数、伏笔、角色规划等所有规划字段。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功更新章节规划"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节规划不存在")
    })
    public Result<String> updateChapterPlan(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节规划ID，必须为有效的规划主键", required = true, example = "1")
            @PathVariable Long planId,
            @Parameter(description = "章节规划更新请求体", required = true)
            @RequestBody ChapterPlanUpdateRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新章节规划，planId={}", userId, planId);

        chapterService.updateChapterPlan(planId, request);
        return Result.ok("更新成功");
    }

    /**
     * 创建章节
     */
    @PostMapping
    @Operation(
        summary = "创建章节",
        description = "在指定项目下创建新章节。可以指定章节序号，如果不指定则自动分配为当前最大序号+1。创建成功后返回新章节的ID。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功创建章节，返回新章节ID",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "400", description = "参数错误，如标题为空"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "项目不存在")
    })
    public Result<Long> createChapter(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节序号，可选。不指定时自动分配为当前最大序号+1", example = "5")
            @RequestParam(required = false) Integer chapterNumber,
            @Parameter(description = "章节标题，必填，长度1-100字符", required = true, example = "风云际会")
            @RequestParam String title
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 在项目 {} 中创建章节: {}", userId, projectId, title);

        Long chapterId = chapterService.createChapter(projectId, chapterNumber, title);
        return Result.ok(chapterId);
    }

    /**
     * 更新章节
     */
    @PutMapping("/update/{chapterId}")
    @Operation(
        summary = "更新章节",
        description = "更新指定章节的信息，可更新标题、内容、所属分卷、状态、锁定状态等字段。支持部分更新，只传入需要修改的字段即可。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功更新章节"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在")
    })
    public Result<String> updateChapter(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId,
            @Parameter(description = "章节更新请求体", required = true)
            @RequestBody com.aifactory.dto.ChapterUpdateRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新章节 {}", userId, chapterId);

        chapterService.updateChapter(chapterId, request);
        return Result.ok("更新成功");
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/{chapterId}")
    @Operation(
        summary = "删除章节",
        description = "删除指定的章节。执行逻辑删除，数据不会真正从数据库中移除。删除后章节序号不会自动重排。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功删除章节"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在")
    })
    public Result<String> deleteChapter(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除章节 {}", userId, chapterId);

        chapterService.deleteChapter(chapterId);
        return Result.ok("删除成功");
    }

    /**
     * 重新构建章节记忆
     * 手动触发AI提取章节记忆，用于更新或补充剧情记忆数据
     */
    @PostMapping("/{chapterId}/rebuild-memory")
    @Operation(
        summary = "重新构建章节记忆",
        description = "手动触发AI重新提取章节的结构化记忆数据，包括章节摘要、关键事件、角色状态变化、伏笔设置等。用于更新或补充剧情记忆数据，确保后续章节生成时能获取准确的上下文。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功构建章节记忆"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在"),
        @ApiResponse(responseCode = "500", description = "AI服务异常")
    })
    public Result<String> rebuildChapterMemory(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求重新构建章节记忆，chapterId={}", userId, chapterId);

        chapterService.rebuildChapterMemory(chapterId);
        return Result.ok("章节记忆构建成功");
    }

    /**
     * AI生成章节（异步，推荐使用）
     * 避免超时问题，后台异步执行任务
     */
    @PostMapping("/generate-async/{planId}")
    @Operation(
        summary = "AI生成章节（异步）",
        description = "根据章节规划异步生成章节内容。创建后台任务执行生成，避免HTTP请求超时问题。" +
            "返回任务ID，可通过任务管理接口查询执行进度和结果。推荐使用此接口替代SSE流式接口。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功创建生成任务，返回任务ID"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节规划不存在"),
        @ApiResponse(responseCode = "500", description = "任务创建失败")
    })
    public Result<Map<String, Object>> generateChapterByPlanAsync(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节规划ID，指定要生成的章节规划", required = true, example = "1")
            @PathVariable Long planId
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI异步生成章节，projectId={}, planId={}", userId, projectId, planId);

        // 构建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("planId", planId);
        config.put("userId", userId);

        // 创建任务请求
        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("generate_chapter_content");
        taskRequest.setTaskName("章节内容生成 - planId: " + planId);
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);

        if (result.getOk() == null || !result.getOk()) {
            return Result.error("创建章节生成任务失败: " + result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "章节生成任务已创建，正在后台执行");

        log.info("章节生成任务创建成功，taskId: {}", result.getData().getId());

        return Result.ok(response);
    }

    /**
     * AI剧情修复
     * 检查并修复章节中的逻辑错误、重复内容、设定不一致等问题
     */
    @PostMapping("/{chapterId}/ai-fix")
    @Operation(
        summary = "AI剧情修复（同步）",
        description = "使用AI检查并修复章节中的逻辑错误、重复内容、设定不一致等问题。" +
            "支持指定修复选项（如：修复逻辑漏洞、删除重复内容、统一人物设定等）和自定义修复要求。" +
            "返回修复后的内容、修复报告和修复摘要。由于修复过程可能耗时较长，推荐使用异步接口 ai-fix-async。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功完成剧情修复，返回修复结果",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在"),
        @ApiResponse(responseCode = "500", description = "AI服务异常")
    })
    public Result<ChapterAiFixResponse> fixChapterWithAI(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId,
            @Parameter(description = "AI剧情修复请求，包含修复选项和自定义要求", required = true)
            @RequestBody ChapterAiFixRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI剧情修复，chapterId={}", userId, chapterId);

        ChapterAiFixResponse response = chapterService.fixChapterWithAI(chapterId, request);
        return Result.ok(response);
    }

    /**
     * AI剧情修复（异步，推荐使用）
     * 避免超时问题，后台异步执行任务
     */
    @PostMapping("/{chapterId}/ai-fix-async")
    @Operation(
        summary = "AI剧情修复（异步）",
        description = "使用AI异步检查并修复章节中的剧情问题。创建后台任务执行修复，避免HTTP请求超时问题。" +
            "返回任务ID，可通过任务管理接口查询执行进度和结果。推荐使用此接口替代同步接口。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功创建修复任务，返回任务ID"),
        @ApiResponse(responseCode = "400", description = "章节内容不存在"),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在"),
        @ApiResponse(responseCode = "500", description = "任务创建失败")
    })
    public Result<Map<String, Object>> fixChapterWithAIAsync(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId,
            @Parameter(description = "AI剧情修复请求，包含修复选项和自定义要求", required = true)
            @RequestBody ChapterAiFixRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI异步剧情修复，chapterId={}", userId, chapterId);

        // 获取章节内容
        var chapter = chapterService.getChapterDetail(chapterId);
        if (chapter == null || chapter.getContent() == null) {
            return Result.error("章节内容不存在");
        }

        // 构建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("chapterId", chapterId);
        config.put("content", chapter.getContent());
        if (request.getFixOptions() != null && !request.getFixOptions().isEmpty()) {
            config.put("fixOptions", request.getFixOptions());
        }
        if (request.getCustomRequirements() != null && !request.getCustomRequirements().isEmpty()) {
            config.put("customRequirements", request.getCustomRequirements());
        }

        // 创建任务请求
        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("chapter_fix");
        taskRequest.setTaskName("AI剧情修复 - 第" + chapter.getChapterNumber() + "章: " + chapter.getTitle());
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);

        if (result.getOk() == null || !result.getOk()) {
            return Result.error(result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "剧情修复任务已创建，正在后台执行");

        return Result.ok(response);
    }

    /**
     * AI润色
     * 对章节内容进行文学润色
     */
    @PostMapping("/{chapterId}/ai-polish")
    @Operation(
        summary = "AI章节润色",
        description = "使用AI对章节内容进行文学润色，提升文字质量和阅读体验。" +
            "支持三种润色风格：vivid(细腻描写型)、fast(紧凑节奏型)、literary(文学优化型)；" +
            "支持三种润色程度：light(轻度)、medium(中度)、heavy(深度)。" +
            "可附加自定义润色要求。返回润色后的内容、润色报告和润色摘要。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功完成章节润色，返回润色结果",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
        @ApiResponse(responseCode = "404", description = "章节不存在"),
        @ApiResponse(responseCode = "500", description = "AI服务异常")
    })
    public Result<ChapterAiPolishResponse> polishChapterWithAI(
            @Parameter(description = "项目ID，必须为有效的项目主键", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "章节ID，必须为有效的章节主键", required = true, example = "1")
            @PathVariable Long chapterId,
            @Parameter(description = "AI润色请求，包含润色风格、程度和自定义要求", required = true)
            @RequestBody ChapterAiPolishRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 请求AI润色，chapterId={}，style={}", userId, chapterId, request.getStyle());

        ChapterAiPolishResponse response = chapterService.polishChapterWithAI(chapterId, request);
        return Result.ok(response);
    }
}
