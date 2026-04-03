package com.aifactory.service.task.impl;

import com.aifactory.common.XmlParser;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.NovelWorldviewPowerSystem;
import com.aifactory.entity.Project;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.NovelWorldviewPowerSystemMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorldviewTaskStrategy 9-step orchestration.
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@ExtendWith(MockitoExtension.class)
class WorldviewTaskStrategyTest {

    @Mock
    private NovelWorldviewMapper worldviewMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Mock
    private PowerSystemService powerSystemService;

    @Mock
    private ContinentRegionService continentRegionService;

    @Mock
    private FactionService factionService;

    @Mock
    private XmlParser xmlParser;

    @Mock
    private GeographyTaskStrategy geographyTaskStrategy;

    @Mock
    private PowerSystemTaskStrategy powerSystemTaskStrategy;

    @Mock
    private FactionTaskStrategy factionTaskStrategy;

    @InjectMocks
    private WorldviewTaskStrategy worldviewTaskStrategy;

    // ======================== createSteps ========================

    @Test
    void testCreateStepsReturns9Steps() {
        AiTask task = new AiTask();
        task.setProjectId(1L);

        List<TaskStrategy.StepConfig> steps = worldviewTaskStrategy.createSteps(task);

        assertEquals(9, steps.size());
        assertEquals("check_existing", steps.get(0).getType());
        assertEquals("clean_geography", steps.get(1).getType());
        assertEquals("generate_geography", steps.get(2).getType());
        assertEquals("clean_power_system", steps.get(3).getType());
        assertEquals("generate_power_system", steps.get(4).getType());
        assertEquals("clean_faction", steps.get(5).getType());
        assertEquals("generate_faction", steps.get(6).getType());
        assertEquals("generate_core", steps.get(7).getType());
        assertEquals("save_core", steps.get(8).getType());
    }

    @Test
    void testCreateStepsCorrectOrder() {
        AiTask task = new AiTask();
        task.setProjectId(1L);

        List<TaskStrategy.StepConfig> steps = worldviewTaskStrategy.createSteps(task);

        for (int i = 0; i < 9; i++) {
            assertEquals(i + 1, steps.get(i).getOrder());
        }
    }

    // ======================== check_existing ========================

    @Test
    void testCheckExistingDeletesOnlyWorldview() {
        AiTaskStep step = createStep("check_existing");
        TaskStrategy.TaskContext context = createContext(1L);

        NovelWorldview existing = new NovelWorldview();
        existing.setId(100L);
        when(worldviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(worldviewPowerSystemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        // Verify worldview-power_system associations were deleted
        verify(worldviewPowerSystemMapper).delete(any(LambdaQueryWrapper.class));
        // Verify worldview record was deleted
        verify(worldviewMapper).deleteById(100L);
        // Verify module data was NOT touched
        verifyNoInteractions(continentRegionService);
        verifyNoInteractions(factionService);
        verifyNoInteractions(powerSystemService);
    }

    @Test
    void testCheckExistingNoExistingWorldview() {
        AiTaskStep step = createStep("check_existing");
        TaskStrategy.TaskContext context = createContext(1L);

        when(worldviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        verify(worldviewMapper, never()).deleteById(anyLong());
        verify(worldviewPowerSystemMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    // ======================== clean_geography delegation ========================

    @Test
    void testCleanGeographyDelegatesToStrategy() {
        AiTaskStep step = createStep("clean_geography");
        TaskStrategy.TaskContext context = createContext(1L);

        when(geographyTaskStrategy.executeStep(any(AiTaskStep.class), eq(context)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of("projectId", 1L), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        verify(geographyTaskStrategy).executeStep(any(AiTaskStep.class), eq(context));
    }

    // ======================== generate_geography delegation ========================

    @Test
    void testGenerateGeographyCallsGenerateAndSave() {
        AiTaskStep step = createStep("generate_geography");
        TaskStrategy.TaskContext context = createContext(1L);

        when(geographyTaskStrategy.executeStep(any(AiTaskStep.class), eq(context)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of(), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        // Should call generate_geography then save_geography
        ArgumentCaptor<AiTaskStep> stepCaptor = ArgumentCaptor.forClass(AiTaskStep.class);
        verify(geographyTaskStrategy, times(2)).executeStep(stepCaptor.capture(), eq(context));

        List<AiTaskStep> capturedSteps = stepCaptor.getAllValues();
        assertEquals("generate_geography", capturedSteps.get(0).getStepType());
        assertEquals("save_geography", capturedSteps.get(1).getStepType());
    }

    // ======================== clean/generate power_system delegation ========================

    @Test
    void testCleanPowerSystemDelegatesToStrategy() {
        AiTaskStep step = createStep("clean_power_system");
        TaskStrategy.TaskContext context = createContext(1L);

        when(powerSystemTaskStrategy.executeStep(any(AiTaskStep.class), eq(context)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of(), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        verify(powerSystemTaskStrategy).executeStep(any(AiTaskStep.class), eq(context));
    }

    @Test
    void testGeneratePowerSystemCallsGenerateAndSave() {
        AiTaskStep step = createStep("generate_power_system");
        TaskStrategy.TaskContext context = createContext(1L);

        when(powerSystemTaskStrategy.executeStep(any(AiTaskStep.class), any(TaskStrategy.TaskContext.class)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of(), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        ArgumentCaptor<AiTaskStep> stepCaptor = ArgumentCaptor.forClass(AiTaskStep.class);
        verify(powerSystemTaskStrategy, times(2)).executeStep(stepCaptor.capture(), any(TaskStrategy.TaskContext.class));

        List<AiTaskStep> capturedSteps = stepCaptor.getAllValues();
        assertEquals("generate_power_system", capturedSteps.get(0).getStepType());
        assertEquals("save_power_system", capturedSteps.get(1).getStepType());
    }

    // ======================== clean/generate faction delegation ========================

    @Test
    void testCleanFactionDelegatesToStrategy() {
        AiTaskStep step = createStep("clean_faction");
        TaskStrategy.TaskContext context = createContext(1L);

        when(factionTaskStrategy.executeStep(any(AiTaskStep.class), any(TaskStrategy.TaskContext.class)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of(), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        verify(factionTaskStrategy).executeStep(any(AiTaskStep.class), any(TaskStrategy.TaskContext.class));
    }

    @Test
    void testGenerateFactionBuildsContext() {
        AiTaskStep step = createStep("generate_faction");
        TaskStrategy.TaskContext context = createContext(1L);

        when(continentRegionService.buildGeographyText(1L)).thenReturn("Geography context text");
        when(powerSystemService.buildPowerSystemConstraint(1L)).thenReturn("Power system context text");
        when(factionTaskStrategy.executeStep(any(AiTaskStep.class), any(TaskStrategy.TaskContext.class)))
            .thenReturn(TaskStrategy.StepResult.success(Map.of(), 100));

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        // Verify dependency context was built
        verify(continentRegionService).buildGeographyText(1L);
        verify(powerSystemService).buildPowerSystemConstraint(1L);
        // Verify faction strategy was called twice (generate + save)
        verify(factionTaskStrategy, times(2)).executeStep(any(AiTaskStep.class), any(TaskStrategy.TaskContext.class));
    }

    // ======================== save_core ========================

    @Test
    void testSaveCoreCreatesAssociations() throws Exception {
        AiTaskStep step = createStep("save_core");
        Map<String, Object> sharedData = new HashMap<>();
        sharedData.put("coreAiResponse", "<w><b>Background</b><l>Timeline</l><r>Rules</r></w>");
        TaskStrategy.TaskContext context = createContextWithSharedData(1L, sharedData);

        Project project = new Project();
        project.setId(1L);
        project.setUserId(10L);
        when(projectMapper.selectById(1L)).thenReturn(project);

        com.aifactory.dto.WorldSettingXmlDto dto = new com.aifactory.dto.WorldSettingXmlDto();
        dto.setBackground("Background");
        dto.setTimeline("Timeline");
        dto.setRules("Rules");
        when(xmlParser.parse(anyString(), eq(com.aifactory.dto.WorldSettingXmlDto.class))).thenReturn(dto);

        NovelPowerSystem ps1 = new NovelPowerSystem();
        ps1.setId(201L);
        NovelPowerSystem ps2 = new NovelPowerSystem();
        ps2.setId(202L);
        when(powerSystemService.listByProjectId(1L)).thenReturn(List.of(ps1, ps2));

        NovelWorldview savedWorldview = new NovelWorldview();
        savedWorldview.setId(300L);
        savedWorldview.setProjectId(1L);
        when(worldviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedWorldview);

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertTrue(result.isSuccess());
        // Verify worldview record was inserted
        verify(worldviewMapper).insert(any(NovelWorldview.class));
        // Verify 2 power_system associations were created
        verify(worldviewPowerSystemMapper, times(2)).insert(any(NovelWorldviewPowerSystem.class));
        // Verify complete data assembly
        verify(continentRegionService).fillGeography(any(NovelWorldview.class));
        verify(factionService).fillForces(any(NovelWorldview.class));
        // Verify project stage update
        verify(projectMapper).updateById(any(Project.class));
    }

    @Test
    void testSaveCoreNoAiResponse() {
        AiTaskStep step = createStep("save_core");
        TaskStrategy.TaskContext context = createContext(1L);

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("未找到核心世界观AI响应"));
    }

    // ======================== executeStep unknown type ========================

    @Test
    void testExecuteStepUnknownType() {
        AiTaskStep step = createStep("unknown_step");
        TaskStrategy.TaskContext context = createContext(1L);

        TaskStrategy.StepResult result = worldviewTaskStrategy.executeStep(step, context);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("未知的步骤类型"));
    }

    // ======================== getTaskType ========================

    @Test
    void testGetTaskType() {
        assertEquals("worldview", worldviewTaskStrategy.getTaskType());
    }

    // ======================== Helper methods ========================

    private AiTaskStep createStep(String stepType) {
        AiTaskStep step = new AiTaskStep();
        step.setStepType(stepType);
        step.setStepName("test-step");
        step.setId(1L);
        return step;
    }

    private TaskStrategy.TaskContext createContext(Long projectId) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode config = mapper.createObjectNode();
        return new TaskStrategy.TaskContext(1L, projectId, "worldview", config, new HashMap<>());
    }

    private TaskStrategy.TaskContext createContextWithSharedData(Long projectId, Map<String, Object> sharedData) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode config = mapper.createObjectNode();
        return new TaskStrategy.TaskContext(1L, projectId, "worldview", config, sharedData);
    }
}
