package com.aifactory.service.chapter.prompt;

import com.aifactory.dto.CharacterPromptInfo;
import com.aifactory.entity.*;
import com.aifactory.service.*;
import com.aifactory.service.prompt.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PromptTemplateBuilder 单元测试
 * 验证规划角色约束注入逻辑 (CG-01, CG-02)
 */
@ExtendWith(MockitoExtension.class)
class PromptTemplateBuilderTest {

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private ProjectBasicSettingsService projectBasicSettingsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NovelCharacterService novelCharacterService;

    @Mock
    private NovelCharacterChapterService novelCharacterChapterService;

    @Mock
    private PowerSystemService powerSystemService;

    @Mock
    private ContinentRegionService continentRegionService;

    @Mock
    private FactionService factionService;

    @InjectMocks
    private PromptTemplateBuilder promptTemplateBuilder;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        // Mock template service to return the resolved template (passes variables through)
        when(promptTemplateService.executeTemplate(anyString(), anyMap()))
            .thenAnswer(invocation -> {
                Map<String, Object> vars = invocation.getArgument(1);
                // Return the characterInfo value so we can verify it
                Object characterInfo = vars.get("characterInfo");
                return characterInfo != null ? characterInfo.toString() : "";
            });
    }

    // ==================== Helper Methods ====================

    private NovelChapterPlan buildTestChapterPlan(String plannedCharactersJson) {
        NovelChapterPlan plan = new NovelChapterPlan();
        plan.setId(1L);
        plan.setProjectId(PROJECT_ID);
        plan.setChapterNumber(1);
        plan.setChapterTitle("test");
        plan.setWordCountTarget(3000);
        plan.setPlotStage("development");
        plan.setPlannedCharacters(plannedCharactersJson);
        return plan;
    }

    private NovelVolumePlan buildTestVolumePlan() {
        NovelVolumePlan volume = new NovelVolumePlan();
        volume.setId(1L);
        volume.setVolumeNumber(1);
        volume.setVolumeTitle("test volume");
        return volume;
    }

    private NovelWorldview buildTestWorldview() {
        NovelWorldview worldview = new NovelWorldview();
        worldview.setProjectId(PROJECT_ID);
        worldview.setWorldType("fantasy");
        return worldview;
    }

    private void setupFallbackMocks() {
        // Mock project basic settings to return null (triggers default values path)
        when(projectBasicSettingsService.getByProjectId(PROJECT_ID)).thenReturn(null);

        // Mock character service for fallback path
        NovelCharacter fallbackChar = new NovelCharacter();
        fallbackChar.setId(99L);
        fallbackChar.setName("张三");
        fallbackChar.setRoleType("supporting");
        when(novelCharacterService.getNonNpcCharacters(PROJECT_ID))
            .thenReturn(List.of(fallbackChar));

        // Mock character chapter service
        when(novelCharacterChapterService.getPreviousAppearance(anyLong(), anyLong(), anyInt()))
            .thenReturn(null);

        // Mock continent region and faction to do nothing
        doNothing().when(continentRegionService).fillGeography(any(NovelWorldview.class));
        doNothing().when(factionService).fillForces(any(NovelWorldview.class));

        // Mock power system
        when(powerSystemService.buildPowerSystemConstraint(PROJECT_ID)).thenReturn("");
    }

    /**
     * Setup mocks for planned character path tests (no character service needed)
     */
    private void setupPlannedCharacterMocks() {
        // Mock project basic settings to return null (triggers default values path)
        when(projectBasicSettingsService.getByProjectId(PROJECT_ID)).thenReturn(null);

        // Mock continent region and faction to do nothing
        doNothing().when(continentRegionService).fillGeography(any(NovelWorldview.class));
        doNothing().when(factionService).fillForces(any(NovelWorldview.class));

        // Mock power system
        when(powerSystemService.buildPowerSystemConstraint(PROJECT_ID)).thenReturn("");
    }

    // ==================== Tests ====================

    @Test
    @DisplayName("CG-01: 有规划角色时characterInfo仅包含规划角色文本，不包含全量角色列表")
    void testPlannedCharacterInjection() throws Exception {
        // Given: a chapter plan with planned characters
        String plannedJson = "[{\"characterName\":\"李云\",\"roleType\":\"protagonist\",\"roleDescription\":\"发现密室线索并决定深入调查\",\"importance\":\"high\",\"characterId\":42}]";
        NovelChapterPlan plan = buildTestChapterPlan(plannedJson);

        setupPlannedCharacterMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then: verify promptTemplateService.executeTemplate was called
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        String characterInfo = (String) variables.get("characterInfo");

        assertNotNull(characterInfo);
        assertTrue(characterInfo.contains("李云"), "characterInfo should contain planned character name");
        assertFalse(characterInfo.contains("主要角色信息"), "characterInfo should NOT contain full-list header from buildCharacterInfoText");
        // The fallback buildCharacterPromptInfoList should NOT have been called
        verify(novelCharacterService, never()).getNonNpcCharacters(anyLong());
    }

    @Test
    @DisplayName("CG-02a: plannedCharacters=null时走全量角色注入回退路径")
    void testNullPlannedCharactersFallback() {
        // Given: a chapter plan with null plannedCharacters
        NovelChapterPlan plan = buildTestChapterPlan(null);
        setupFallbackMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then: verify fallback path was taken
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        String characterInfo = (String) variables.get("characterInfo");

        // Should contain the full character list header
        assertTrue(characterInfo.contains("主要角色信息"), "Null plannedCharacters should trigger full character list (contains header)");
        // And the character service should have been called
        verify(novelCharacterService).getNonNpcCharacters(PROJECT_ID);
    }

    @Test
    @DisplayName("CG-02b: plannedCharacters=[] 时走全量角色注入回退路径")
    void testEmptyPlannedCharactersFallback() {
        // Given: a chapter plan with empty array plannedCharacters
        NovelChapterPlan plan = buildTestChapterPlan("[]");
        setupFallbackMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then: verify fallback path was taken
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        String characterInfo = (String) variables.get("characterInfo");

        // Should contain the full character list header
        assertTrue(characterInfo.contains("主要角色信息"), "Empty array plannedCharacters should trigger full character list (contains header)");
        // And the character service should have been called
        verify(novelCharacterService).getNonNpcCharacters(PROJECT_ID);
    }

    @Test
    @DisplayName("D-01: 约束语言包含'必须出场'开头约束和'请确认'结尾提醒")
    void testConstraintLanguagePresent() throws Exception {
        // Given
        String plannedJson = "[{\"characterName\":\"李云\",\"roleType\":\"protagonist\",\"roleDescription\":\"调查线索\",\"importance\":\"high\",\"characterId\":42}]";
        NovelChapterPlan plan = buildTestChapterPlan(plannedJson);
        setupPlannedCharacterMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        String characterInfo = (String) variablesCaptor.getValue().get("characterInfo");
        assertNotNull(characterInfo);
        assertTrue(characterInfo.contains("必须出场"), "Constraint text should contain opening constraint '必须出场'");
        assertTrue(characterInfo.contains("请确认"), "Constraint text should contain closing reminder '请确认'");
    }

    @Test
    @DisplayName("D-04: 约束文本包含NPC允许提示")
    void testNpcAllowancePresent() throws Exception {
        // Given
        String plannedJson = "[{\"characterName\":\"李云\",\"roleType\":\"protagonist\",\"roleDescription\":\"调查线索\",\"importance\":\"high\",\"characterId\":42}]";
        NovelChapterPlan plan = buildTestChapterPlan(plannedJson);
        setupPlannedCharacterMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        String characterInfo = (String) variablesCaptor.getValue().get("characterInfo");
        assertNotNull(characterInfo);
        assertTrue(characterInfo.contains("NPC"), "Constraint text should contain NPC allowance with 'NPC'");
    }

    @Test
    @DisplayName("D-05: 角色文本格式为 '角色名 (roleType) - 描述 [importance]'")
    void testPlannedCharacterTextFormat() throws Exception {
        // Given
        String plannedJson = "[{\"characterName\":\"李云\",\"roleType\":\"protagonist\",\"roleDescription\":\"发现密室线索并决定深入调查\",\"importance\":\"high\",\"characterId\":42}]";
        NovelChapterPlan plan = buildTestChapterPlan(plannedJson);
        setupPlannedCharacterMocks();

        // When
        promptTemplateBuilder.buildChapterPrompt(
            PROJECT_ID, plan, buildTestVolumePlan(), buildTestWorldview(), List.of()
        );

        // Then
        verify(promptTemplateService).executeTemplate(eq("llm_chapter_generate_standard"), variablesCaptor.capture());

        String characterInfo = (String) variablesCaptor.getValue().get("characterInfo");
        assertNotNull(characterInfo);
        // Verify the formatted line contains all expected parts
        assertTrue(characterInfo.contains("李云"), "Should contain character name");
        assertTrue(characterInfo.contains("protagonist"), "Should contain raw roleType");
        assertTrue(characterInfo.contains("发现密室线索并决定深入调查"), "Should contain role description");
        assertTrue(characterInfo.contains("high"), "Should contain importance level");
        // Verify format pattern: name (roleType) - description [importance]
        assertTrue(characterInfo.contains("李云 (protagonist)"), "Should have 'name (roleType)' format");
        assertTrue(characterInfo.contains("- 发现密室线索并决定深入调查"), "Should have '- description' format");
        assertTrue(characterInfo.contains("[high]"), "Should have '[importance]' format");
    }
}
