package com.aifactory.service.task.impl;

import com.aifactory.common.NameMatchUtil;
import com.aifactory.entity.NovelCharacter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChapterGenerationTaskStrategy -- DOM parsing of chapter XML with character tags,
 * name matching, and persistence.
 *
 * Tests use reflection to invoke private methods parseChaptersXml and resolveCharacterIds
 * since they are implementation details not exposed via public API.
 *
 * @Author AI Factory
 * @Date 2026-04-08
 */
class ChapterGenerationTaskStrategyTest {

    private ChapterGenerationTaskStrategy strategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        strategy = new ChapterGenerationTaskStrategy();
    }

    // ======================== Helper: invoke parseChaptersXml via reflection ========================

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> invokeParseChaptersXml(String xml) throws Exception {
        Method method = ChapterGenerationTaskStrategy.class.getDeclaredMethod("parseChaptersXml", String.class);
        method.setAccessible(true);
        return (List<Map<String, String>>) method.invoke(strategy, xml);
    }

    // ======================== Helper: invoke resolveCharacterIds via reflection ========================

    private String invokeResolveCharacterIds(String json, List<NovelCharacter> characters) throws Exception {
        Method method = ChapterGenerationTaskStrategy.class.getDeclaredMethod(
                "resolveCharacterIds", String.class, List.class);
        method.setAccessible(true);
        return (String) method.invoke(strategy, json, characters);
    }

    // ======================== Helper: build test NovelCharacter ========================

    private NovelCharacter makeCharacter(Long id, String name, String roleType) {
        NovelCharacter c = new NovelCharacter();
        c.setId(id);
        c.setName(name);
        c.setRoleType(roleType);
        return c;
    }

    // ======================== Test XML samples ========================

    private static final String XML_TWO_CHAPTERS_WITH_CHARS = """
            <c>
              <o>
                <n>1</n>
                <t>Test Chapter 1</t>
                <p>Plot outline for chapter 1</p>
                <e>Event A; Event B</e>
                <g>Establish protagonist</g>
                <w>3000</w>
                <s>Starting scene 1</s>
                <f>Ending scene 1</f>
                <ch><cn>Li Yun</cn><cd>Protagonist enters the city</cd><ci>high</ci></ch>
                <ch><cn>Zhang San</cn><cd>Supporting character meets protagonist</cd><ci>medium</ci></ch>
              </o>
              <o>
                <n>2</n>
                <t>Test Chapter 2</t>
                <p>Plot outline for chapter 2</p>
                <e>Event C</e>
                <g>Introduce antagonist</g>
                <w>3000</w>
                <s>Starting scene 2</s>
                <f>Ending scene 2</f>
                <ch><cn>Wang Wu</cn><cd>Antagonist appears</cd><ci>high</ci></ch>
              </o>
            </c>
            """;

    private static final String XML_CHAPTERS_NO_CHARS = """
            <c>
              <o>
                <n>1</n>
                <t>No Characters Chapter</t>
                <p>Plot outline without characters</p>
                <e>Event A</e>
                <g>World building</g>
                <w>3000</w>
                <s>Start scene</s>
                <f>End scene</f>
              </o>
              <o>
                <n>2</n>
                <t>Another No Characters</t>
                <p>More plot</p>
                <e>Event B</e>
                <g>Character intro</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
              </o>
            </c>
            """;

    private static final String XML_WITH_RAW_AMPERSAND = """
            <c>
              <o>
                <n>1</n>
                <t>Ampersand Test</t>
                <p>Plot with raw & character</p>
                <e>Tom & Jerry fight</e>
                <g>Goal</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
                <ch><cn>Tom & Jerry</cn><cd>Funny duo</cd><ci>low</ci></ch>
              </o>
            </c>
            """;

    private static final String XML_WITH_UNMATCHED_TAGS = """
            <c>
              <o>
                <n>1</n>
                <t>Unmatched Tag Test</t>
                <p>Plot</p>
                <e>Events</e>
                <g>Goal</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
                <ch><cn>Li Yun</cn><cd>Test desc</cd><ci>high</ci></ch>
              </o>
              </o>
            </c>
            """;

    private static final String XML_NO_C_ROOT = """
            <something>
              <o>
                <n>1</n>
                <t>Wrong Root</t>
              </o>
            </something>
            """;

    // ======================== Test 1: parseChaptersXml with character tags ========================

    @Test
    @DisplayName("Test 1: parseChaptersXml with character tags returns plannedCharacters JSON")
    void testParseChaptersWithCharacterTags() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_TWO_CHAPTERS_WITH_CHARS);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should parse 2 chapters");

        // Chapter 1 should have plannedCharacters with 2 entries
        Map<String, String> chapter1 = result.get(0);
        assertEquals("1", chapter1.get("chapterNumber"));
        assertEquals("Test Chapter 1", chapter1.get("chapterTitle"));
        assertNotNull(chapter1.get("plannedCharacters"), "Chapter 1 should have plannedCharacters");

        JsonNode chars1 = objectMapper.readTree(chapter1.get("plannedCharacters"));
        assertEquals(2, chars1.size(), "Chapter 1 should have 2 characters");
        assertEquals("Li Yun", chars1.get(0).get("characterName").asText());
        assertEquals("Protagonist enters the city", chars1.get(0).get("roleDescription").asText());
        assertEquals("high", chars1.get(0).get("importance").asText());
        assertNull(chars1.get(0).get("characterId"), "characterId should be null before resolution");
        assertEquals("Zhang San", chars1.get(1).get("characterName").asText());

        // Chapter 2 should have plannedCharacters with 1 entry
        Map<String, String> chapter2 = result.get(1);
        assertEquals("2", chapter2.get("chapterNumber"));
        assertNotNull(chapter2.get("plannedCharacters"), "Chapter 2 should have plannedCharacters");

        JsonNode chars2 = objectMapper.readTree(chapter2.get("plannedCharacters"));
        assertEquals(1, chars2.size(), "Chapter 2 should have 1 character");
        assertEquals("Wang Wu", chars2.get(0).get("characterName").asText());
    }

    // ======================== Test 2: parseChaptersXml without character tags ========================

    @Test
    @DisplayName("Test 2: parseChaptersXml without <ch> tags parses successfully, no plannedCharacters key")
    void testParseChaptersWithoutCharacterTags() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_CHAPTERS_NO_CHARS);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should parse 2 chapters");

        for (Map<String, String> chapter : result) {
            assertNull(chapter.get("plannedCharacters"),
                    "Chapters without <ch> tags should have null/absent plannedCharacters");
            assertNotNull(chapter.get("chapterNumber"), "chapterNumber should be present");
            assertNotNull(chapter.get("chapterTitle"), "chapterTitle should be present");
        }
    }

    // ======================== Test 3: parseChaptersXml with raw & ========================

    @Test
    @DisplayName("Test 3: parseChaptersXml with raw & in character description sanitizes successfully")
    void testParseChaptersWithRawAmpersand() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_WITH_RAW_AMPERSAND);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should parse 1 chapter");

        Map<String, String> chapter = result.get(0);
        assertEquals("Ampersand Test", chapter.get("chapterTitle"));

        // The character name should be parsed (may have & escaped)
        assertNotNull(chapter.get("plannedCharacters"), "Should have plannedCharacters");
        JsonNode chars = objectMapper.readTree(chapter.get("plannedCharacters"));
        assertEquals(1, chars.size());
        // Character name contains &, should be preserved (possibly as &amp; in text)
        String charName = chars.get(0).get("characterName").asText();
        assertTrue(charName.contains("Tom") && charName.contains("Jerry"),
                "Character name should contain Tom and Jerry: " + charName);
    }

    // ======================== Test 4: parseChaptersXml with unmatched <o> tags ========================

    @Test
    @DisplayName("Test 4: parseChaptersXml with unmatched </o> tags sanitizes and parses correctly")
    void testParseChaptersWithUnmatchedTags() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_WITH_UNMATCHED_TAGS);

        assertNotNull(result, "Result should not be null after sanitization");
        assertEquals(1, result.size(), "Should parse 1 valid chapter after fixing unmatched tags");

        Map<String, String> chapter = result.get(0);
        assertEquals("1", chapter.get("chapterNumber"));
        assertEquals("Unmatched Tag Test", chapter.get("chapterTitle"));
    }

    // ======================== Test 5: parseChaptersXml returns null for no <c> root ========================

    @Test
    @DisplayName("Test 5: parseChaptersXml returns null for XML without <c> root element")
    void testParseChaptersNoCRoot() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_NO_C_ROOT);

        assertNull(result, "Should return null when <c> root element is missing");
    }

    // ======================== Test 6: resolveCharacterIds matches existing character ========================

    @Test
    @DisplayName("Test 6: resolveCharacterIds matches character by name, sets characterId")
    void testResolveCharacterIdsMatch() throws Exception {
        String plannedCharsJson = "[{\"characterName\":\"Li Yun\",\"roleDescription\":\"Protagonist\",\"importance\":\"high\",\"characterId\":null}]";
        List<NovelCharacter> allCharacters = new ArrayList<>();
        allCharacters.add(makeCharacter(42L, "Li Yun", "protagonist"));
        allCharacters.add(makeCharacter(43L, "Zhang San", "supporting"));

        String result = invokeResolveCharacterIds(plannedCharsJson, allCharacters);

        assertNotNull(result, "Result should not be null");
        JsonNode resultArray = objectMapper.readTree(result);
        assertEquals(1, resultArray.size());
        assertEquals(42L, resultArray.get(0).get("characterId").asLong(),
                "characterId should be set to matched character's ID");
        assertEquals("protagonist", resultArray.get(0).get("roleType").asText(),
                "roleType should be set from matched character");
    }

    // ======================== Test 7: resolveCharacterIds leaves characterId null when no match ========================

    @Test
    @DisplayName("Test 7: resolveCharacterIds leaves characterId=null when no match found")
    void testResolveCharacterIdsNoMatch() throws Exception {
        String plannedCharsJson = "[{\"characterName\":\"Unknown Person\",\"roleDescription\":\"New character\",\"importance\":\"medium\",\"characterId\":null}]";
        List<NovelCharacter> allCharacters = new ArrayList<>();
        allCharacters.add(makeCharacter(42L, "Li Yun", "protagonist"));

        String result = invokeResolveCharacterIds(plannedCharsJson, allCharacters);

        assertNotNull(result);
        JsonNode resultArray = objectMapper.readTree(result);
        assertEquals(1, resultArray.size());
        assertTrue(resultArray.get(0).get("characterId").isNull(),
                "characterId should be null when no match found");
    }

    // ======================== Test 8: resolveCharacterIds uses matched roleType, defaults to "supporting" ========================

    @Test
    @DisplayName("Test 8: resolveCharacterIds uses matched roleType, defaults to supporting when no match")
    void testResolveCharacterIdsRoleType() throws Exception {
        // One character that matches (gets roleType from existing), one that doesn't (gets "supporting")
        String plannedCharsJson = "[{\"characterName\":\"Li Yun\",\"roleDescription\":\"desc\",\"importance\":\"high\",\"characterId\":null}," +
                "{\"characterName\":\"New Guy\",\"roleDescription\":\"desc\",\"importance\":\"low\",\"characterId\":null}]";
        List<NovelCharacter> allCharacters = new ArrayList<>();
        allCharacters.add(makeCharacter(42L, "Li Yun", "protagonist"));

        String result = invokeResolveCharacterIds(plannedCharsJson, allCharacters);

        assertNotNull(result);
        JsonNode resultArray = objectMapper.readTree(result);
        assertEquals(2, resultArray.size());

        // Matched character gets roleType from the existing character
        assertEquals("protagonist", resultArray.get(0).get("roleType").asText(),
                "Matched character should get roleType from existing character");
        assertEquals(42L, resultArray.get(0).get("characterId").asLong());

        // Unmatched character gets default "supporting"
        assertEquals("supporting", resultArray.get(1).get("roleType").asText(),
                "Unmatched character should default to 'supporting'");
        assertTrue(resultArray.get(1).get("characterId").isNull(),
                "Unmatched character should have null characterId");
    }
}
