package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.CharacterDto;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.response.Result;
import com.aifactory.service.NovelCharacterService;
import com.aifactory.vo.CharacterArcVO;
import com.aifactory.vo.CharacterChapterVO;
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
 * 人物控制器
 * 提供小说人物的创建、查询、更新、删除等功能，以及人物弧光生成和出场章节查询
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/characters")
@Tag(name = "人物管理", description = "小说人物管理API，包括人物的CRUD操作、人物弧光生成、出场章节查询等功能")
public class NovelCharacterController {

    @Autowired
    private NovelCharacterService characterService;

    /**
     * 获取人物列表
     */
    @Operation(
            summary = "获取项目人物列表",
            description = "获取指定项目下的所有人物列表，返回人物的简要信息（ID、名称、头像、角色定位）"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取人物列表",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CharacterDto.class)))),
            @ApiResponse(responseCode = "401", description = "未授权，请先登录")
    })
    @GetMapping
    public Result<List<CharacterDto>> getCharacterList(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取项目 {} 的人物列表", userId, projectId);

        List<CharacterDto> characters = characterService.getCharacterList(projectId);
        return Result.ok(characters);
    }

    /**
     * 获取人物详情
     */
    @Operation(
            summary = "获取人物详情",
            description = "根据人物ID获取人物的完整详细信息，包括基本信息、性格、外貌、背景故事、能力等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取人物详情",
                    content = @Content(schema = @Schema(implementation = NovelCharacter.class))),
            @ApiResponse(responseCode = "404", description = "人物不存在")
    })
    @GetMapping("/{characterId}")
    public Result<NovelCharacter> getCharacterDetail(
            @Parameter(description = "人物ID", required = true, example = "1")
            @PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取人物 {} 详情", userId, characterId);

        NovelCharacter character = characterService.getCharacterDetail(characterId);
        return Result.ok(character);
    }

    /**
     * 创建人物
     */
    @Operation(
            summary = "创建人物",
            description = "在指定项目下创建新人物。需要提供人物的基本信息，如名称、性别、年龄、角色类型等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "人物创建成功，返回新人物ID",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping
    public Result<Long> createCharacter(
            @Parameter(description = "项目ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "人物信息",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NovelCharacter.class))
            )
            @RequestBody NovelCharacter character) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 在项目 {} 中创建人物: {}", userId, projectId, character.getName());

        Long characterId = characterService.createCharacter(projectId, character);
        return Result.ok(characterId);
    }

    /**
     * 更新人物
     */
    @Operation(
            summary = "更新人物信息",
            description = "更新指定人物的详细信息。可以修改人物的基本信息、性格、外貌、背景故事等所有属性"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "人物更新成功"),
            @ApiResponse(responseCode = "404", description = "人物不存在")
    })
    @PutMapping("/{characterId}")
    public Result<String> updateCharacter(
            @Parameter(description = "人物ID", required = true, example = "1")
            @PathVariable Long characterId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "更新后的人物信息",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NovelCharacter.class))
            )
            @RequestBody NovelCharacter character
    ) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新人物 {}", userId, characterId);

        characterService.updateCharacter(characterId, character);
        return Result.ok("更新成功");
    }

    /**
     * 删除人物
     */
    @Operation(
            summary = "删除人物",
            description = "删除指定人物。注意：此操作不可逆，删除后人物的所有相关数据都将被清除"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "人物删除成功"),
            @ApiResponse(responseCode = "404", description = "人物不存在")
    })
    @DeleteMapping("/{characterId}")
    public Result<String> deleteCharacter(
            @Parameter(description = "人物ID", required = true, example = "1")
            @PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除人物 {}", userId, characterId);

        characterService.deleteCharacter(characterId);
        return Result.ok("删除成功");
    }

    // ==================== 人物弧光相关接口 ====================

    /**
     * AI生成人物弧光
     */
    @Operation(
            summary = "AI生成人物弧光",
            description = "使用AI为指定人物生成人物弧光（Character Arc）。人物弧光描述人物在整个故事中的内在变化轨迹，包括初始状态、转折事件、各个阶段的变化和最终状态"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功生成人物弧光",
                    content = @Content(schema = @Schema(implementation = CharacterArcVO.class))),
            @ApiResponse(responseCode = "404", description = "人物不存在")
    })
    @PostMapping("/{characterId}/generate-arc")
    public Result<CharacterArcVO> generateCharacterArc(
            @Parameter(description = "人物ID", required = true, example = "1")
            @PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 为人物 {} 生成弧光", userId, characterId);

        CharacterArcVO arc = characterService.generateCharacterArc(characterId);
        return Result.ok(arc);
    }

    // ==================== 角色出场章节管理 ====================

    /**
     * 获取角色出场的章节列表
     *
     * @param characterId 角色ID
     * @return 角色出场章节列表，包含章节号、标题、状态等信息
     */
    @Operation(
            summary = "获取角色出场章节列表",
            description = "获取指定角色在哪些章节中出场，返回章节列表及角色在各章节中的状态信息，包括是否首次出场、重要程度等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取出场章节列表",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CharacterChapterVO.class)))),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @GetMapping("/{characterId}/chapters")
    public Result<List<CharacterChapterVO>> getCharacterChapters(
            @Parameter(description = "角色ID", required = true, example = "1")
            @PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 获取角色 {} 出场章节列表", userId, characterId);

        List<CharacterChapterVO> chapters = characterService.getCharacterChapters(characterId);
        return Result.ok(chapters);
    }
}
