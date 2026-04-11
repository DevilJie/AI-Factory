package com.aifactory.service.task.impl;

import com.aifactory.dto.ForeshadowingDto;
import com.aifactory.dto.ForeshadowingQueryDto;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.mapper.NovelOutlineMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.ForeshadowingService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptContextBuilder;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.chapter.prompt.PromptTemplateBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OutlineTaskStrategy -- foreshadowing context injection into chapter planning prompts,
 * and foreshadowing tag parsing in chapter XML.
 *
 * Tests use reflection to invoke private methods since they are implementation details
 * not exposed via public API.
 *
 * @Author AI Factory
 * @Date 2026-04-11
 */
@ExtendWith(MockitoExtension.class)
class OutlineTaskStrategyTest {

    @InjectMocks
    private OutlineTaskStrategy strategy;

    @Mock
    private NovelOutlineMapper outlineMapper;

    @Mock
    private NovelVolumePlanMapper volumePlanMapper;

    @Mock
    private NovelChapterPlanMapper chapterPlanMapper;

    @Mock
    private NovelCharacterMapper characterMapper;

    @Mock
    private NovelWorldviewMapper worldviewMapper;

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private PromptContextBuilder promptContextBuilder;

    @Mock
    private PromptTemplateBuilder promptTemplateBuilder;

    @Mock
    private PowerSystemService powerSystemService;

    @Mock
    private ContinentRegionService continentRegionService;

    @Mock
    private FactionService factionService;

    @Mock
    private ForeshadowingService foreshadowingService;

    // ======================== Helper: invoke buildActiveForeshadowingContext via reflection ========================

    private String invokeBuildActiveForeshadowingContext(Long projectId, int volumeNumber) throws Exception {
        Method method = OutlineTaskStrategy.class.getDeclaredMethod(
                "buildActiveForeshadowingContext", Long.class, int.class);
        method.setAccessible(true);
        return (String) method.invoke(strategy, projectId, volumeNumber);
    }

    // ======================== Helper: invoke parseChaptersXml via reflection ========================

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> invokeParseChaptersXml(String xml) throws Exception {
        Method method = OutlineTaskStrategy.class.getDeclaredMethod("parseChaptersXml", String.class);
        method.setAccessible(true);
        return (List<Map<String, String>>) method.invoke(strategy, xml);
    }

    // ======================== Helper: build test ForeshadowingDto ========================

    private ForeshadowingDto makeForeshadowingDto(Long id, String title, String type, String layoutType,
                                                   String status, Integer plantedVolume, Integer plantedChapter,
                                                   Integer plannedCallbackVolume, Integer plannedCallbackChapter) {
        ForeshadowingDto dto = new ForeshadowingDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setType(type);
        dto.setLayoutType(layoutType);
        dto.setStatus(status);
        dto.setPlantedVolume(plantedVolume);
        dto.setPlantedChapter(plantedChapter);
        dto.setPlannedCallbackVolume(plannedCallbackVolume);
        dto.setPlannedCallbackChapter(plannedCallbackChapter);
        return dto;
    }

    // ======================== Test XML samples ========================

    private static final String XML_CHAPTER_WITH_FS = """
            <c>
              <o>
                <n>1</n>
                <t>Test Chapter</t>
                <p>Plot</p>
                <e>Event</e>
                <g>Goal</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
                <fs><ft>Mystery Key</ft><fy>item</fy><fl>bright1</fl><fd>Hero finds ancient key</fd><fc>2</fc><fr>5</fr></fs>
              </o>
            </c>
            """;

    private static final String XML_CHAPTER_WITH_FP = """
            <c>
              <o>
                <n>1</n>
                <t>Callback Chapter</t>
                <p>Plot</p>
                <e>Event</e>
                <g>Goal</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
                <fp><ft>Mystery Key</ft><fd>Hero uses the key to open the door</fd></fp>
              </o>
            </c>
            """;

    private static final String XML_CHAPTER_NO_FORESHADOWING = """
            <c>
              <o>
                <n>1</n>
                <t>Normal Chapter</t>
                <p>Plot</p>
                <e>Event</e>
                <g>Goal</g>
                <w>3000</w>
                <s>Start</s>
                <f>End</f>
              </o>
            </c>
            """;

    // ======================== buildActiveForeshadowingContext tests ========================

    @Test
    @DisplayName("buildActiveForeshadowingContext with no active foreshadowing returns empty string")
    void testBuildActiveForeshadowingContext_noActiveForeshadowing_returnsEmptyString() throws Exception {
        // Mock both queries to return empty lists
        when(foreshadowingService.getForeshadowingList(any(ForeshadowingQueryDto.class)))
                .thenReturn(Collections.emptyList());

        String result = invokeBuildActiveForeshadowingContext(1L, 1);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty string when no active foreshadowing");
    }

    @Test
    @DisplayName("buildActiveForeshadowingContext with pending plants contains foreshadowing info")
    void testBuildActiveForeshadowingContext_withPendingPlants_containsForeshadowingInfo() throws Exception {
        // Create test data
        ForeshadowingDto fs1 = makeForeshadowingDto(1L, "神秘钥匙", "item", "bright1",
                "pending", 1, null, 2, 5);
        ForeshadowingDto fs2 = makeForeshadowingDto(2L, "老人预言", "secret", "dark",
                "pending", 1, null, 3, 10);
        List<ForeshadowingDto> pendingPlants = List.of(fs1, fs2);

        // Use Answer to differentiate between query types
        when(foreshadowingService.getForeshadowingList(any(ForeshadowingQueryDto.class)))
                .thenAnswer(invocation -> {
                    ForeshadowingQueryDto query = invocation.getArgument(0);
                    if ("pending".equals(query.getStatus())) {
                        return pendingPlants;
                    }
                    return Collections.emptyList(); // in_progress query returns empty
                });

        String result = invokeBuildActiveForeshadowingContext(1L, 1);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty when there are pending plants");
        assertTrue(result.contains("神秘钥匙"), "Result should contain foreshadowing title");
        assertTrue(result.contains("item"), "Result should contain foreshadowing type");
        assertTrue(result.contains("待埋设伏笔"), "Result should contain pending plant section header");
        assertTrue(result.contains("bright1"), "Result should contain layout type");
    }

    @Test
    @DisplayName("buildActiveForeshadowingContext with pending callbacks contains callback info")
    void testBuildActiveForeshadowingContext_withPendingCallbacks_containsCallbackInfo() throws Exception {
        // Create test data
        ForeshadowingDto callback = makeForeshadowingDto(3L, "暗夜信使", "character", "bright2",
                "in_progress", 1, 3, 2, 5);
        List<ForeshadowingDto> pendingCallbacks = List.of(callback);

        when(foreshadowingService.getForeshadowingList(any(ForeshadowingQueryDto.class)))
                .thenAnswer(invocation -> {
                    ForeshadowingQueryDto query = invocation.getArgument(0);
                    if ("in_progress".equals(query.getStatus())) {
                        return pendingCallbacks;
                    }
                    return Collections.emptyList(); // pending query returns empty
                });

        String result = invokeBuildActiveForeshadowingContext(1L, 2);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty when there are pending callbacks");
        assertTrue(result.contains("待回收伏笔"), "Result should contain callback section header");
        assertTrue(result.contains("暗夜信使"), "Result should contain foreshadowing title");
        assertTrue(result.contains("埋设"), "Result should contain plant location text");
    }

    // ======================== parseChaptersXml with foreshadowing tags tests ========================
    // NOTE: These tests will FAIL until Plan 02 Task 1 implements the fs/fp extraction logic.
    // They are Wave 0 stubs that serve as the RED phase.

    @Test
    @DisplayName("parseChaptersXml with <fs> tag extracts foreshadowing plant data")
    void testParseChaptersXml_withFsTag_extractsForeshadowingPlantData() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_CHAPTER_WITH_FS);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should parse 1 chapter");

        Map<String, String> chapterData = result.get(0);
        assertEquals("1", chapterData.get("chapterNumber"), "chapterNumber should be 1");
        assertEquals("Test Chapter", chapterData.get("chapterTitle"), "chapterTitle should match");

        // Check foreshadowing plant extraction
        assertEquals("1", chapterData.get("_foreshadowingPlants_count"),
                "Should have 1 foreshadowing plant");
        assertTrue(chapterData.containsKey("_fs_0_ft"),
                "Should have _fs_0_ft key for first foreshadowing plant title");
    }

    @Test
    @DisplayName("parseChaptersXml with <fp> tag extracts foreshadowing payoff data")
    void testParseChaptersXml_withFpTag_extractsForeshadowingPayoffData() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_CHAPTER_WITH_FP);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should parse 1 chapter");

        Map<String, String> chapterData = result.get(0);
        assertEquals("1", chapterData.get("chapterNumber"), "chapterNumber should be 1");

        // Check foreshadowing payoff extraction
        assertEquals("1", chapterData.get("_foreshadowingPayoffs_count"),
                "Should have 1 foreshadowing payoff");
        assertTrue(chapterData.containsKey("_fp_0_ft"),
                "Should have _fp_0_ft key for first foreshadowing payoff title");
    }

    @Test
    @DisplayName("parseChaptersXml without foreshadowing tags has no foreshadowing keys")
    void testParseChaptersXml_withoutForeshadowingTags_noForeshadowingKeys() throws Exception {
        List<Map<String, String>> result = invokeParseChaptersXml(XML_CHAPTER_NO_FORESHADOWING);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should parse 1 chapter");

        Map<String, String> chapterData = result.get(0);
        assertFalse(chapterData.containsKey("_foreshadowingPlants_count"),
                "Should not have _foreshadowingPlants_count key");
        assertFalse(chapterData.containsKey("_foreshadowingPayoffs_count"),
                "Should not have _foreshadowingPayoffs_count key");
    }
}
