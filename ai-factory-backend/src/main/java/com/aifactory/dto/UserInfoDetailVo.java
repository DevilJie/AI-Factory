package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户详细信息VO（包含扩展信息）
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户详细信息响应，包含基本资料和扩展设置")
public class UserInfoDetailVo {

    /**
     * 用户ID
     */
    @Schema(description = "用户唯一标识ID", example = "1")
    private Long userId;

    /**
     * 登录名
     */
    @Schema(description = "登录名/用户名", example = "admin")
    private String loginName;

    /**
     * 真实姓名
     */
    @Schema(description = "用户真实姓名", example = "张三")
    private String actualName;

    /**
     * 昵称
     */
    @Schema(description = "用户昵称/显示名称", example = "小张")
    private String nickname;

    /**
     * 头像
     */
    @Schema(description = "用户头像URL地址", example = "https://example.com/avatar/user1.jpg")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    /**
     * 性别
     */
    @Schema(description = "性别：0-未知，1-男，2-女", example = "1", allowableValues = {"0", "1", "2"})
    private Integer gender;

    // ========== 扩展信息 ==========

    /**
     * 语言
     */
    @Schema(description = "界面语言偏好设置", example = "zh-CN", allowableValues = {"zh-CN", "en-US"})
    private String language;

    /**
     * 主题
     */
    @Schema(description = "界面主题设置", example = "light", allowableValues = {"light", "dark", "auto"})
    private String theme;

    /**
     * 邮件通知
     */
    @Schema(description = "邮件通知开关：0-关闭，1-开启", example = "1", allowableValues = {"0", "1"})
    private Integer emailNotification;

    /**
     * 浏览器通知
     */
    @Schema(description = "浏览器推送通知开关：0-关闭，1-开启", example = "1", allowableValues = {"0", "1"})
    private Integer browserNotification;

    /**
     * 项目通知
     */
    @Schema(description = "项目相关通知开关：0-关闭，1-开启", example = "1", allowableValues = {"0", "1"})
    private Integer projectNotification;

    /**
     * 自动保存
     */
    @Schema(description = "自动保存开关：0-关闭，1-开启", example = "1", allowableValues = {"0", "1"})
    private Integer autoSave;

    /**
     * 个人简介
     */
    @Schema(description = "用户个人简介/自我介绍，最多500字", example = "热爱写作的小说作者，擅长科幻题材。", maxLength = 500)
    private String bio;
}
