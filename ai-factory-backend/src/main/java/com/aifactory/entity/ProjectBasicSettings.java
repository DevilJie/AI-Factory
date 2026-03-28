package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目基础设置实体
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@TableName("project_basic_settings")
public class ProjectBasicSettings {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 项目ID
     */
    private Long projectId;

    // ==================== 情节结构属性 ====================

    /**
     * 整体叙事结构：three_act(三幕式)/multi_line(多线)/flashback(倒叙)/interpolation(插叙)
     */
    private String narrativeStructure;

    /**
     * 结局类型：open(开放)/closed(封闭)/semi_open(半开放)
     */
    private String endingType;

    /**
     * 结局基调：tragedy(悲剧)/comedy(喜剧)/serious(正剧)
     */
    private String endingTone;

    // ==================== 叙事风格属性 ====================

    /**
     * 文风调性：realistic(写实)/gorgeous(华丽)/concise(简洁)/humor(幽默)/sharp(犀利)/gentle(温柔)/desolate(苍凉)
     */
    private String writingStyle;

    /**
     * 写作视角：first_person(第一人称)/third_person(第三人称)/omniscient(全知视角)
     */
    private String writingPerspective;

    /**
     * 节奏把控：fast(快)/slow(慢)/mixed(快慢结合)
     */
    private String narrativePace;

    /**
     * 语言体系：archaic(半文半白)/tech(科技感)/urban(生活化)
     */
    private String languageStyle;

    /**
     * 描写侧重：action(动作)/psychology(心理)/environment(环境)/dialogue(对话)
     */
    private String descriptionFocus;

    // ==================== 复杂结构JSON存储 ====================

    /**
     * 阶段节点/卷篇设定（JSON）
     */
    private String plotStages;

    /**
     * 单章创作设定（JSON）
     */
    private String chapterConfig;

    /**
     * 伏笔与埋线配置（JSON）
     */
    private String foreshadowingConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
