package com.aifactory.common;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Generic three-tier Chinese name matching utility.
 * Extracted from the proven WorldviewXmlParser matching pattern.
 *
 * @Author AI Factory
 * @Date 2026-04-07
 */
@Slf4j
public class NameMatchUtil {

    /**
     * Interface for entities that can be matched by name.
     * Any entity with a name and ID can implement this.
     */
    public interface NamedEntity {
        String getName();
        Long getId();
    }

    /**
     * Common Chinese character honorifics and title suffixes (xianxia/wuxia context).
     * Ordered with longer suffixes first for greedy matching.
     */
    public static final List<String> CHARACTER_SUFFIXES = List.of(
            "大长老", "掌门师", "师叔祖",
            "公子", "小姐", "大哥", "大姐", "师傅", "师叔",
            "长老", "前辈", "道友", "兄弟", "妹妹", "师兄", "师妹",
            "掌门", "堂主", "护法", "使者", "仙子", "圣女",
            "大人", "阁下", "义士", "少侠", "女侠"
    );

    // Stub: not yet implemented — RED phase
    public static <T extends NamedEntity> Long matchByName(List<T> candidates, String targetName, List<String> suffixes) {
        return null;
    }

    // Stub: not yet implemented — RED phase
    public static String stripSuffix(String name, List<String> suffixes) {
        return "";
    }

    private NameMatchUtil() {
        // Utility class
    }
}
