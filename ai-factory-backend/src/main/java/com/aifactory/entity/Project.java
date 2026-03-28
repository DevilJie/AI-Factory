package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目实体
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("t_project")
public class Project {

    /**
     * 项目ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 项目类型：video(视频项目)/novel(小说项目)
     */
    private String projectType;

    /**
     * 故事基调：adventure(冒险)/romance(恋爱)/suspense(悬疑)/fantasy(奇幻)/daily(日常)/scifi(科幻)/mystery(解谜)/action(动作)
     */
    private String storyTone;

    /**
     * 故事类型
     */
    private String storyGenre;

    /**
     * 小说类型：fantasy(玄幻)/urban(都市)/scifi(科幻)/history(历史)/military(军事)/mystery(悬疑)/romance(言情)/gaming(游戏)
     */
    private String novelType;

    /**
     * 目标长度：short(短篇)/medium(中篇)/long(长篇)
     */
    private String targetLength;

    /**
     * 视觉风格（视频项目专用）
     */
    private String visualStyle;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 状态：draft(草稿)/in_progress(进行中)/completed(已完成)/archived(已归档)
     */
    private String status;

    /**
     * 标签（JSON数组字符串）
     */
    private String tags;

    /**
     * 章节数量
     */
    private Integer chapterCount;

    /**
     * 总字数（小说项目）
     */
    private Integer totalWordCount;

    /**
     * 总时长（视频项目，秒）
     */
    private Integer totalDuration;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * Dify 知识库 ID
     */
    private String difyDatasetId;

    /**
     * Dify 知识库名称
     */
    private String difyDatasetName;

    /**
     * 设置阶段：project_created/worldview_configured/basic_settings_configured/volume_generation_started
     */
    private String setupStage;
}
