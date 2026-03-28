package com.aifactory.controller;

import com.aifactory.common.UserContext;
import com.aifactory.dto.CaptchaVo;
import com.aifactory.dto.UserLoginDto;
import com.aifactory.dto.UserLoginResultVo;
import com.aifactory.dto.UserRegisterDto;
import com.aifactory.dto.UserUpdateDto;
import com.aifactory.dto.UserInfoDetailVo;
import com.aifactory.response.Result;
import com.aifactory.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取验证码
     */
    @GetMapping("/getCaptcha")
    @Operation(summary = "获取验证码", description = "生成图形验证码，返回Base64编码的图片和验证码UUID。验证码有效期为返回的expireSeconds秒，用于登录和注册时验证。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取验证码",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaptchaVo.class)))
    })
    public Result<CaptchaVo> getCaptcha() {
        CaptchaVo captcha = com.aifactory.common.CaptchaManager.generateCaptcha();
        return Result.ok(captcha);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册接口。需要提供登录名、密码、真实姓名、手机号等必填信息。注册成功后可直接使用账号登录。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功，返回成功提示信息"),
            @ApiResponse(responseCode = "400", description = "参数验证失败，如手机号格式不正确、必填字段为空等"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误，如登录名已存在")
    })
    public Result<String> register(@Valid @RequestBody UserRegisterDto registerDto) {
        String result = userService.register(registerDto);
        return Result.ok(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口。需要提供登录名、密码以及验证码信息。登录成功后返回JWT Token，后续请求需在Header中携带该Token。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功，返回用户信息和Token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginResultVo.class))),
            @ApiResponse(responseCode = "400", description = "参数验证失败，如必填字段为空"),
            @ApiResponse(responseCode = "401", description = "登录失败，如用户名或密码错误、验证码错误或已过期")
    })
    public Result<UserLoginResultVo> login(@Valid @RequestBody UserLoginDto loginDto) {
        UserLoginResultVo result = userService.login(loginDto);
        return Result.ok(result);
    }

    /**
     * 获取登录用户信息
     */
    @GetMapping("/getLoginInfo")
    @Operation(summary = "获取当前登录用户信息", description = "获取当前已登录用户的基本信息。需要在请求Header中携带有效的JWT Token。返回用户的基本资料，包括ID、登录名、昵称、头像等。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginResultVo.class))),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<UserLoginResultVo> getLoginInfo() {
        UserLoginResultVo result = userService.getLoginInfoById(UserContext.getUserId());
        return Result.ok(result);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的个人信息。可更新昵称、手机号、邮箱等信息。需要在请求Header中携带有效的JWT Token。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数验证失败，如手机号或邮箱格式不正确"),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<String> updateUserInfo(@Valid @RequestBody UserUpdateDto updateDto) {
        userService.updateUserInfoById(UserContext.getUserId(), updateDto);
        return Result.ok("更新成功");
    }

    /**
     * 用户退出
     */
    @GetMapping("/logout")
    @Operation(summary = "用户退出登录", description = "用户退出登录接口。清除服务端存储的用户登录状态。需要在请求Header中携带有效的JWT Token。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "退出成功"),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<String> logout() {
        String result = userService.logout();
        return Result.ok(result);
    }

    /**
     * 获取用户详细信息（包含扩展信息）
     */
    @GetMapping("/detail")
    @Operation(summary = "获取用户详细信息", description = "获取当前登录用户的详细信息，包括基本资料和扩展设置（如语言、主题、通知设置等）。需要在请求Header中携带有效的JWT Token。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户详细信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserInfoDetailVo.class))),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    public Result<UserInfoDetailVo> getUserDetail() {
        UserInfoDetailVo result = userService.getUserDetailInfoById(UserContext.getUserId());
        return Result.ok(result);
    }
}
