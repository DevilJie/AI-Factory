package com.aifactory.enums;

import lombok.Getter;

/**
 * AI角色枚举
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Getter
public enum AIRole {

    /**
     * 资深网络小说作家
     */
    NOVEL_WRITER("novel_writer", "资深网络小说作家", "拥有10年以上网络小说创作经验，擅长玄幻、都市、科幻等各种类型，精通故事结构、人物塑造、情节设计"),

    /**
     * 视频编剧
     */
    VIDEO_SCRIPTWRITER("video_scriptwriter", "专业视频编剧", "拥有丰富的视频剧本创作经验，擅长分镜设计、情节编排、对白创作"),

    /**
     * 程序员
     */
    PROGRAMMER("programmer", "资深程序员", "拥有10年以上编程经验，精通多种编程语言，擅长系统设计、算法优化、代码重构"),

    /**
     * 产品经理
     */
    PRODUCT_MANAGER("product_manager", "资深产品经理", "拥有丰富的产品设计经验，擅长需求分析、用户研究、产品规划"),

    /**
     * UI设计师
     */
    UI_DESIGNER("ui_designer", "资深UI设计师", "拥有丰富的界面设计经验，擅长用户体验设计、视觉设计、交互设计"),

    /**
     * 数据分析师
     */
    DATA_ANALYST("data_analyst", "数据分析师", "拥有丰富的数据分析经验，擅长数据挖掘、统计分析、可视化呈现"),

    /**
     * 通用助手
     */
    GENERAL_ASSISTANT("general_assistant", "通用AI助手", "全能型AI助手，可以处理各种类型的任务");

    private final String code;
    private final String name;
    private final String description;

    AIRole(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static AIRole getByCode(String code) {
        for (AIRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return GENERAL_ASSISTANT;
    }
}
