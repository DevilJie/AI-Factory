package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户扩展信息DTO
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@Schema(description = "用户扩展信息更新请求参数")
public class UserExtDto {

    /**
     * 语言
     */
    @Schema(description = "界面语言偏好设置", example = "zh-CN", allowableValues = {"zh-CN", "en-US"})
    private String language;

    /**
     * 主题
     */
    @Schema(description = "界面主题设置：light-浅色主题，dark-深色主题，auto-跟随系统", example = "light", allowableValues = {"light", "dark", "auto"})
    private String theme;

    /**
     * 邮件通知
     */
    @Schema(description = "邮件通知开关：0-关闭，1-开启。开启后将通过邮件接收重要通知", example = "1", allowableValues = {"0", "1"})
    private Integer emailNotification;

    /**
     * 浏览器通知
     */
    @Schema(description = "浏览器推送通知开关：0-关闭，1-开启。开启后将通过浏览器接收实时通知", example = "1", allowableValues = {"0", "1"})
    private Integer browserNotification;

    /**
     * 项目通知
     */
    @Schema(description = "项目相关通知开关：0-关闭，1-开启。开启后将接收项目更新、协作邀请等通知", example = "1", allowableValues = {"0", "1"})
    private Integer projectNotification;

    /**
     * 自动保存
     */
    @Schema(description = "自动保存开关：0-关闭，1-开启。开启后编辑内容将自动保存", example = "1", allowableValues = {"0", "1"})
    private Integer autoSave;

    /**
     * 个人简介
     */
    @Schema(description = "用户个人简介/自我介绍，用于展示个人形象，最多500字", example = "热爱写作的小说作者，擅长科幻题材。", maxLength = 500)
    private String bio;
}
