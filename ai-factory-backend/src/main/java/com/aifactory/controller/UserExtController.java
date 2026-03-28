package com.aifactory.controller;

import com.aifactory.dto.UserExtDto;
import com.aifactory.entity.UserExt;
import com.aifactory.response.Result;
import com.aifactory.service.UserExtService;
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
 * 用户扩展信息控制器
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@RestController
@RequestMapping("/api/user/ext")
@Tag(name = "用户扩展信息管理", description = "用户扩展设置相关接口，包括语言、主题、通知设置、自动保存、个人简介等个性化配置")
public class UserExtController {

    @Autowired
    private UserExtService userExtService;

    /**
     * 获取用户扩展信息
     */
    @GetMapping
    @Operation(summary = "获取用户扩展信息", description = "获取当前用户的扩展信息，包括语言偏好、主题设置、通知配置、自动保存设置和个人简介等。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户扩展信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserExt.class))),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<UserExt> getUserExt(
            @Parameter(description = "JWT认证Token，格式：Bearer {token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authorization) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        Long userId = com.aifactory.common.TokenUtil.getUserId(token);
        UserExt userExt = userExtService.getUserExt(userId);
        return Result.ok(userExt);
    }

    /**
     * 更新用户扩展信息
     */
    @PutMapping
    @Operation(summary = "更新用户扩展信息", description = "更新当前用户的扩展信息设置。可更新语言、主题、各类通知开关、自动保存设置和个人简介等。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数验证失败"),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<String> updateUserExt(
            @Parameter(description = "JWT认证Token，格式：Bearer {token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UserExtDto dto) {
        // 移除 "Bearer " 前缀
        String token = authorization.replace("Bearer ", "").trim();
        userExtService.updateUserExt(token, dto);
        return Result.ok("更新成功");
    }
}
