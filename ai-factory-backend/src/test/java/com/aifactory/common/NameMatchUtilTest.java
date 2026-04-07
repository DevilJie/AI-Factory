package com.aifactory.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NameMatchUtil — generic three-tier Chinese name matching utility.
 * Pure static utility, no Mockito or Spring context needed.
 *
 * @Author AI Factory
 * @Date 2026-04-07
 */
class NameMatchUtilTest {

    /**
     * Simple test entity implementing NamedEntity for testing.
     */
    record TestEntity(String name, Long id) implements NameMatchUtil.NamedEntity {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    private static final List<String> CHARACTER_SUFFIXES = List.of(
            "大长老", "掌门师", "师叔祖",
            "公子", "小姐", "大哥", "大姐", "师傅", "师叔",
            "长老", "前辈", "道友", "兄弟", "妹妹", "师兄", "师妹",
            "掌门", "堂主", "护法", "使者", "仙子", "圣女",
            "大人", "阁下", "义士", "少侠", "女侠"
    );

    // ======================== Tier 1: Exact Match ========================

    @Test
    @DisplayName("Tier 1: Exact match returns correct ID")
    void testExactMatch() {
        List<TestEntity> candidates = List.of(new TestEntity("李云", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "李云", CHARACTER_SUFFIXES);
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("Tier 1: Exact match with multiple candidates returns correct one")
    void testExactMatchMultipleCandidates() {
        List<TestEntity> candidates = List.of(
                new TestEntity("张三", 1L),
                new TestEntity("李云", 2L)
        );
        Long result = NameMatchUtil.matchByName(candidates, "李云", CHARACTER_SUFFIXES);
        assertEquals(2L, result);
    }

    // ======================== Tier 2: Suffix-Stripped Match ========================

    @Test
    @DisplayName("Tier 2: Suffix stripped from target matches candidate")
    void testSuffixStrippedMatch() {
        List<TestEntity> candidates = List.of(new TestEntity("李云", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "李云公子", List.of("公子", "小姐"));
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("Tier 2: Suffix stripped from candidate matches target")
    void testSuffixStrippedMatchCandidateSide() {
        List<TestEntity> candidates = List.of(new TestEntity("王师傅", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "王", List.of("师傅"));
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("Tier 2: Single-char name with suffix stripped matches")
    void testSuffixStrippedMatchSingleChar() {
        List<TestEntity> candidates = List.of(new TestEntity("李", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "李公子", List.of("公子"));
        assertEquals(1L, result);
    }

    // ======================== Tier 3: Contains Match ========================

    @Test
    @DisplayName("Tier 3: Candidate name contains target name")
    void testContainsMatchCandidateContainsTarget() {
        List<TestEntity> candidates = List.of(new TestEntity("东方天剑城", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "天剑城", CHARACTER_SUFFIXES);
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("Tier 3: Target name contains candidate name")
    void testContainsMatchTargetContainsCandidate() {
        List<TestEntity> candidates = List.of(new TestEntity("天剑城", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "东方天剑城", CHARACTER_SUFFIXES);
        assertEquals(1L, result);
    }

    // ======================== No Match ========================

    @Test
    @DisplayName("No match returns null")
    void testNoMatch() {
        List<TestEntity> candidates = List.of(new TestEntity("张三", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "李四", CHARACTER_SUFFIXES);
        assertNull(result);
    }

    // ======================== Edge Cases: Null/Empty Input ========================

    @Test
    @DisplayName("Empty candidates list returns null")
    void testEmptyCandidates() {
        Long result = NameMatchUtil.matchByName(List.of(), "李云", CHARACTER_SUFFIXES);
        assertNull(result);
    }

    @Test
    @DisplayName("Null target returns null")
    void testNullTarget() {
        List<TestEntity> candidates = List.of(new TestEntity("李云", 1L));
        Long result = NameMatchUtil.matchByName(candidates, null, CHARACTER_SUFFIXES);
        assertNull(result);
    }

    @Test
    @DisplayName("Empty target returns null")
    void testEmptyTarget() {
        List<TestEntity> candidates = List.of(new TestEntity("李云", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "", CHARACTER_SUFFIXES);
        assertNull(result);
    }

    @Test
    @DisplayName("Null candidates returns null")
    void testNullCandidates() {
        Long result = NameMatchUtil.matchByName(null, "李云", CHARACTER_SUFFIXES);
        assertNull(result);
    }

    // ======================== Tier Precedence ========================

    @Test
    @DisplayName("Tier 1 takes precedence over Tier 2 — exact match wins")
    void testTier1TakesPrecedenceOverTier2() {
        List<TestEntity> candidates = List.of(
                new TestEntity("李云", 1L),
                new TestEntity("李云公子", 2L)
        );
        Long result = NameMatchUtil.matchByName(candidates, "李云", List.of("公子"));
        assertEquals(1L, result, "Exact match should win over suffix-stripped match");
    }

    @Test
    @DisplayName("Tier 2 takes precedence over Tier 3 — suffix match wins")
    void testTier2TakesPrecedenceOverTier3() {
        List<TestEntity> candidates = List.of(
                new TestEntity("李云", 1L),
                new TestEntity("李云飞", 2L)
        );
        Long result = NameMatchUtil.matchByName(candidates, "李云公子", List.of("公子"));
        assertEquals(1L, result, "Suffix-stripped match should win over contains match");
    }

    // ======================== Greedy Suffix Matching ========================

    @Test
    @DisplayName("Longer suffix stripped first when multiple suffixes match")
    void testLongSuffixStrippedFirst() {
        List<TestEntity> candidates = List.of(new TestEntity("长老", 1L));
        Long result = NameMatchUtil.matchByName(candidates, "大长老", List.of("长老", "大长老"));
        assertEquals(1L, result, "Strip '大长老' from target leaves empty; " +
                "strip '长老' from candidate '长老' also leaves empty; empty==empty -> match");
    }

    // ======================== stripSuffix Unit Tests ========================

    @Nested
    @DisplayName("stripSuffix tests")
    class StripSuffixTests {

        @Test
        @DisplayName("Strips matching suffix")
        void testStripSuffixMatches() {
            assertEquals("李云", NameMatchUtil.stripSuffix("李云公子", List.of("公子", "小姐")));
        }

        @Test
        @DisplayName("Returns original when no suffix matches")
        void testStripSuffixNoMatch() {
            assertEquals("李云", NameMatchUtil.stripSuffix("李云", List.of("公子", "小姐")));
        }

        @Test
        @DisplayName("Greedy match — strips longest suffix first (may produce empty string)")
        void testStripSuffixGreedy() {
            // "大长老" ends with "大长老" (length 3 = whole string), greedy strips entire string -> ""
            assertEquals("", NameMatchUtil.stripSuffix("大长老", List.of("长老", "大长老")));
        }

        @Test
        @DisplayName("Null name returns empty string")
        void testStripSuffixNull() {
            assertEquals("", NameMatchUtil.stripSuffix(null, List.of("公子")));
        }

        @Test
        @DisplayName("Empty name returns empty string")
        void testStripSuffixEmpty() {
            assertEquals("", NameMatchUtil.stripSuffix("", List.of("公子")));
        }

        @Test
        @DisplayName("Null suffixes returns original name")
        void testStripSuffixNullSuffixes() {
            assertEquals("李云公子", NameMatchUtil.stripSuffix("李云公子", null));
        }

        @Test
        @DisplayName("Empty suffixes returns original name")
        void testStripSuffixEmptySuffixes() {
            assertEquals("李云公子", NameMatchUtil.stripSuffix("李云公子", List.of()));
        }
    }
}
