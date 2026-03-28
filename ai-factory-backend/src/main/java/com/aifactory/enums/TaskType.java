package com.aifactory.enums;

import lombok.Getter;

/**
 * AI任务类型枚举
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Getter
public enum TaskType {

    /**
     * AI大纲生成
     */
    OUTLINE_GENERATION("outline", "AI大纲生成"),

    /**
     * AI角色生成
     */
    CHARACTER_GENERATION("character", "AI角色生成"),

    /**
     * AI图片生成
     */
    IMAGE_GENERATION("image", "AI图片生成"),

    /**
     * AI视频生成
     */
    VIDEO_GENERATION("video", "AI视频生成"),

    /**
     * AI配音生成
     */
    TTS_GENERATION("tts", "AI配音生成"),

    /**
     * AI章节续写
     */
    CHAPTER_CONTINUE("chapter_continue", "AI章节续写");

    private final String code;
    private final String name;

    TaskType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskType getByCode(String code) {
        for (TaskType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
