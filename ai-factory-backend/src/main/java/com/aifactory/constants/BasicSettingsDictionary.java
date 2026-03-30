package com.aifactory.constants;

import java.util.*;

/**
 * 基础设置枚举字典
 * 用于将枚举值转换为中文描述，便于在LLM提示词中使用
 *
 * @Author AI Factory
 * @Date 2025-02-08
 */
public class BasicSettingsDictionary {

    public static final Map<String, String> STORY_TONE = Map.ofEntries(
        Map.entry("relaxed", "轻松"),
        Map.entry("serious", "严肃"),
        Map.entry("suspense", "悬疑"),
        Map.entry("adventure", "冒险")
    );

    // 小说类型选项
    public static final Map<String, String> NOVEL_TYPE = Map.ofEntries(
        Map.entry("fantasy", "玄幻"),
        Map.entry("urban", "都市"),
        Map.entry("scifi", "科幻"),
        Map.entry("history", "历史"),
        Map.entry("military", "军事"),
        Map.entry("mystery", "悬疑"),
        Map.entry("romance", "言情"),
        Map.entry("gaming", "游戏")
    );


    /**
     * 叙事结构字典
     */
    public static final Map<String, String> NARRATIVE_STRUCTURE = Map.ofEntries(
        Map.entry("three_act", "三幕式结构"),
        Map.entry("four_act", "四幕式结构"),
        Map.entry("five_act", "五幕式结构"),
        Map.entry("kishōtenketsu", "起承转合"),
        Map.entry("seven_point", "七点式结构"),
        Map.entry("hero_journey", "英雄之旅"),
        Map.entry("save_the_cat", "救猫咪结构"),
        Map.entry("freytag", "弗雷塔格金字塔"),
        Map.entry("parallel", "平行叙事"),
        Map.entry("non_linear", "非线性叙事")
    );

    /**
     * 结局类型字典
     */
    public static final Map<String, String> ENDING_TYPE = Map.ofEntries(
        Map.entry("open", "开放式结局"),
        Map.entry("closed", "封闭式结局"),
        Map.entry("cliffhanger", "悬念结局"),
        Map.entry("twist", "反转结局"),
        Map.entry("bittersweet", "苦乐参半结局"),
        Map.entry("tragic", "悲剧结局"),
        Map.entry("happy", "喜剧结局")
    );

    /**
     * 结局基调字典
     */
    public static final Map<String, String> ENDING_TONE = Map.ofEntries(
        Map.entry("serious", "严肃"),
        Map.entry("hopeful", "充满希望"),
        Map.entry("melancholic", "忧郁"),
        Map.entry("peaceful", "宁静"),
        Map.entry("mysterious", "神秘"),
        Map.entry("ironic", "讽刺")
    );

    /**
     * 叙事节奏字典
     */
    public static final Map<String, String> NARRATIVE_PACE = Map.ofEntries(
        Map.entry("fast", "快节奏"),
        Map.entry("slow", "慢节奏"),
        Map.entry("mixed", "混合节奏"),
        Map.entry("accelerating", "逐渐加快"),
        Map.entry("decelerating", "逐渐减慢")
    );

    /**
     * 语言风格字典
     */
    public static final Map<String, String> LANGUAGE_STYLE = Map.ofEntries(
        Map.entry("urban", "都市风格"),
        Map.entry("classical", "古典风格"),
        Map.entry("internet", "网络风格"),
        Map.entry("literary", "文学风格"),
        Map.entry("colloquial", "口语化"),
        Map.entry("formal", "正式风格"),
        Map.entry("poetic", "诗意风格"),
        Map.entry("humorous", "幽默风格")
    );

    /**
     * 描写重点字典
     */
    public static final Map<String, String> DESCRIPTION_FOCUS = Map.ofEntries(
        Map.entry("action", "动作描写"),
        Map.entry("psychology", "心理描写"),
        Map.entry("environment", "环境描写"),
        Map.entry("dialogue", "对话描写")
    );

    /**
     * 写作视角字典
     */
    public static final Map<String, String> WRITING_PERSPECTIVE = Map.ofEntries(
        Map.entry("first_person", "第一人称"),
        Map.entry("third_person", "第三人称"),
        Map.entry("omniscient", "全知视角")
    );

    /**
     * 写作风格字典（用户自定义，这里提供一些示例）
     */
    public static final Map<String, String> WRITING_STYLE_EXAMPLES = Map.ofEntries(
        Map.entry("hard_sf", "硬科幻风格"),
        Map.entry("space_opera", "太空歌剧风格"),
        Map.entry("cyberpunk", "赛博朋克风格"),
        Map.entry("noir", "黑色电影风格"),
        Map.entry("romantic", "浪漫主义风格"),
        Map.entry("realist", "现实主义风格"),
        Map.entry("realistic", "现实主义风格"),
        Map.entry("surreal", "超现实主义风格"),
        Map.entry("minimalist", "极简主义风格")
    );

    /**
     * 获取叙事结构中文描述
     */
    public static String getNarrativeStructure(String key) {
        return NARRATIVE_STRUCTURE.getOrDefault(key, key);
    }

    /**
     * 获取小说类型
     * @param key
     * @return
     */
    public static String getNovelType(String key){
        return NOVEL_TYPE.getOrDefault(key, key);
    }

    /**
     * 获取结局类型中文描述
     */
    public static String getEndingType(String key) {
        return ENDING_TYPE.getOrDefault(key, key);
    }

    /**
     * 获取结局基调中文描述
     */
    public static String getEndingTone(String key) {
        return ENDING_TONE.getOrDefault(key, key);
    }

    /**
     * 获取叙事节奏中文描述
     */
    public static String getNarrativePace(String key) {
        return NARRATIVE_PACE.getOrDefault(key, key);
    }

    /**
     * 获取语言风格中文描述
     */
    public static String getLanguageStyle(String key) {
        return LANGUAGE_STYLE.getOrDefault(key, key);
    }

    /**
     * 获取描写重点中文描述
     */
    public static String getDescriptionFocus(String key) {
        return DESCRIPTION_FOCUS.getOrDefault(key, key);
    }

    /**
     * 获取写作视角中文描述
     */
    public static String getWritingPerspective(String key) {
        return WRITING_PERSPECTIVE.getOrDefault(key, key);
    }

    /**
     * 获取故事基调
     */
    public static String getStoryTone(String key){
        return STORY_TONE.getOrDefault(key, key);
    }

    /**
     * 获取写作风格中文描述
     */
    public static String getWritingStyle(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        // 如果在示例字典中，使用转换后的中文；否则返回原值（用户可能自定义了写作风格）
        return WRITING_STYLE_EXAMPLES.getOrDefault(key, key);
    }

    /**
     * 批量转换描写重点数组为中文描述
     */
    public static List<String> getDescriptionFocusList(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            result.add(getDescriptionFocus(key));
        }
        return result;
    }

    /**
     * 将基础设置转换为LLM提示词格式
     *
     * @param narrativeStructure 叙事结构
     * @param endingType 结局类型
     * @param endingTone 结局基调
     * @param writingStyle 写作风格
     * @param writingPerspective 写作视角
     * @param narrativePace 叙事节奏
     * @param languageStyle 语言风格
     * @param descriptionFocus 描写重点列表
     * @return 中文描述字符串
     */
    public static String toPromptFormat(
        String narrativeStructure,
        String endingType,
        String endingTone,
        String writingStyle,
        String writingPerspective,
        String narrativePace,
        String languageStyle,
        List<String> descriptionFocus
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("【叙事结构】").append(getNarrativeStructure(narrativeStructure)).append("\n");
        sb.append("【结局类型】").append(getEndingType(endingType)).append("\n");
        sb.append("【结局基调】").append(getEndingTone(endingTone)).append("\n");
        sb.append("【写作视角】").append(getWritingPerspective(writingPerspective)).append("\n");
        sb.append("【叙事节奏】").append(getNarrativePace(narrativePace)).append("\n");
        sb.append("【语言风格】").append(getLanguageStyle(languageStyle)).append("\n");

        if (writingStyle != null && !writingStyle.isEmpty()) {
            sb.append("【写作风格】").append(writingStyle).append("\n");
        }

        if (descriptionFocus != null && !descriptionFocus.isEmpty()) {
            sb.append("【描写重点】").append(String.join("、", getDescriptionFocusList(descriptionFocus))).append("\n");
        }

        return sb.toString();
    }
}
