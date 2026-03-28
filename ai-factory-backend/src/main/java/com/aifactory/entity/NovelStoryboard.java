package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说章节分镜实体
 *
 * @Author CaiZy
 * @Date 2025-01-30
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("novel_storyboard")
public class NovelStoryboard {

    /**
     * 分镜ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 镜头编号
     */
    private Integer shotNumber;

    /**
     * 景别
     */
    private String shotType;

    /**
     * 拍摄角度
     */
    private String cameraAngle;

    /**
     * 运镜方式
     */
    private String cameraMovement;

    /**
     * 分镜描述
     */
    private String description;

    /**
     * 画面生成提示词
     */
    private String visualPrompt;

    /**
     * 镜头时长（秒）
     */
    private Integer duration;

    /**
     * 出场角色ID列表
     */
    private String characterIds;

    /**
     * 台词
     */
    private String dialogue;

    /**
     * 动作描述
     */
    private String action;

    /**
     * 备注信息
     */
    private String notes;

    /**
     * 状态
     */
    private String status;

    /**
     * 生成的首帧图URL
     */
    private String imageUrl;

    /**
     * 图像生成参数
     */
    private String generationParams;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
