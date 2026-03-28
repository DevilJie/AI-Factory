package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜VO
 *
 * @Author CaiZy
 * @Date 2025-01-30
 */
@Data
@Schema(description = "分镜信息，包含单个镜头场景的所有可视化参数")
public class StoryboardVo {

    @Schema(description = "分镜ID，唯一标识", example = "1")
    private Long id;

    @Schema(description = "所属章节ID", example = "1")
    private Long chapterId;

    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    @Schema(description = "镜头编号，标识场景中的镜头顺序", example = "1")
    private Integer shotNumber;

    @Schema(description = "镜头类型，决定画面构图方式",
            example = "medium_shot",
            allowableValues = {"wide_shot", "long_shot", "medium_shot", "close_up", "extreme_close_up", "establishing_shot", "point_of_view", "over_the_shoulder"})
    private String shotType;

    @Schema(description = "摄像机角度，决定拍摄视角",
            example = "eye_level",
            allowableValues = {"eye_level", "high_angle", "low_angle", "birds_eye", "worms_eye", "dutch_angle"})
    private String cameraAngle;

    @Schema(description = "摄像机运动方式，决定镜头移动方式",
            example = "pan",
            allowableValues = {"static", "pan", "tilt", "dolly", "zoom", "crane", "tracking", "handheld"})
    private String cameraMovement;

    @Schema(description = "场景描述，文字描述镜头内容", example = "主角站在悬崖边，眺望远方，风吹动衣角")
    private String description;

    @Schema(description = "视觉提示词，用于AI图像生成的提示词", example = "A young man standing on a cliff edge, looking at distant mountains, wind blowing his clothes, dramatic lighting, cinematic composition")
    private String visualPrompt;

    @Schema(description = "镜头时长，单位为秒", example = "5")
    private Integer duration;

    @Schema(description = "出场角色ID列表，逗号分隔", example = "1,2,3")
    private String characterIds;

    @Schema(description = "对话内容，场景中的台词", example = "这就是传说中的禁地吗...")
    private String dialogue;

    @Schema(description = "动作描述，角色的行为动作", example = "主角缓缓转身，目光坚定")
    private String action;

    @Schema(description = "备注信息，额外的说明或特殊要求", example = "背景音乐渐弱，营造紧张氛围")
    private String notes;

    @Schema(description = "分镜状态",
            example = "draft",
            allowableValues = {"draft", "pending_review", "approved", "completed"})
    private String status;

    @Schema(description = "生成的图像URL，AI生成的场景图片地址", example = "https://example.com/images/storyboard/1.png")
    private String imageUrl;

    @Schema(description = "排序顺序，用于调整分镜顺序", example = "1")
    private Integer sortOrder;
}
