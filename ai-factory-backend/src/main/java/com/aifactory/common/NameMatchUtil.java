package com.aifactory.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * Generic three-tier Chinese name matching utility.
 * Extracted from the proven WorldviewXmlParser matching pattern.
 *
 * <p>Tier 1: Exact match — targetName equals candidate name.
 * Tier 2: Suffix-stripped match — strip configurable suffixes from both sides, then compare.
 * Tier 3: Contains match — bidirectional substring check with single-char guard.</p>
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
     * Per D-07, covers standard xianxia honorifics.
     */
    public static final List<String> CHARACTER_SUFFIXES = List.of(
            "大长老", "掌门师", "师叔祖",
            "公子", "小姐", "大哥", "大姐", "师傅", "师叔",
            "长老", "前辈", "道友", "兄弟", "妹妹", "师兄", "师妹",
            "掌门", "堂主", "护法", "使者", "仙子", "圣女",
            "大人", "阁下", "义士", "少侠", "女侠"
    );

    /**
     * Generic three-tier name matching.
     *
     * @param candidates list of entities to search
     * @param targetName the name to match against
     * @param suffixes   suffixes to strip for Tier 2 matching (may be null)
     * @param <T>        entity type implementing NamedEntity
     * @return matching entity ID, or null if no match
     */
    public static <T extends NamedEntity> Long matchByName(List<T> candidates, String targetName, List<String> suffixes) {
        if (targetName == null || targetName.isEmpty()) return null;
        if (candidates == null || candidates.isEmpty()) return null;

        // Tier 1: Exact match
        for (T candidate : candidates) {
            if (targetName.equals(candidate.getName())) return candidate.getId();
        }

        // Tier 2: Strip common suffixes and compare
        if (suffixes != null && !suffixes.isEmpty()) {
            String strippedTarget = stripSuffix(targetName, suffixes);
            for (T candidate : candidates) {
                String strippedCandidate = stripSuffix(candidate.getName(), suffixes);
                if (strippedTarget.equals(strippedCandidate)) {
                    return candidate.getId();
                }
            }
        }

        // Tier 3: Contains match (bidirectional, with single-char guard)
        for (T candidate : candidates) {
            String candidateName = candidate.getName();
            boolean candidateContainsTarget = candidateName.contains(targetName) && targetName.length() >= 2;
            boolean targetContainsCandidate = targetName.contains(candidateName) && candidateName.length() >= 2;
            if (candidateContainsTarget || targetContainsCandidate) {
                log.warn("Tier 3 (contains) match: '{}' matched candidate '{}' — lower confidence",
                        targetName, candidateName);
                return candidate.getId();
            }
        }

        log.warn("Three-tier matching all failed, no match found for: {}", targetName);
        return null;
    }

    /**
     * Strip a matching suffix from a name using greedy (longest-first) matching.
     *
     * @param name     the name to strip (may be null)
     * @param suffixes list of suffixes to try, ordered longest-first for greedy matching
     * @return the name with suffix removed, or original name if no suffix matched,
     *         or empty string if name is null/empty
     */
    public static String stripSuffix(String name, List<String> suffixes) {
        if (name == null || name.isEmpty()) return "";
        if (suffixes == null || suffixes.isEmpty()) return name;

        // Sort suffixes by length descending for greedy matching
        List<String> sorted = suffixes.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        for (String suffix : sorted) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }

        return name;
    }

    private NameMatchUtil() {
        // Utility class — prevent instantiation
    }
}
