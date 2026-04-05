# Phase 8: 原有逻辑重构 - Research

**Researched:** 2026-04-03
**Domain:** WorldviewTaskStrategy 组合调用重构 + DOM 解析公共逻辑提取
**Confidence:** HIGH

## Summary

Phase 8 将 WorldviewTaskStrategy 从"单次 AI 调用 + 全量 XML 解析"重构为"依次调用三个独立 Strategy + 精简模板生成核心世界观"。三个独立 Strategy（GeographyTaskStrategy、PowerSystemTaskStrategy、FactionTaskStrategy）已在 Phase 7 中完整实现，各自封装了 clean/generate/save 三步流程。本 Phase 的核心工作是在 WorldviewTaskStrategy 中编排这 9 个步骤，并提取重复的 DOM 解析逻辑到公共工具类 WorldviewXmlParser。

当前 WorldviewTaskStrategy 约 920 行，其中约 400 行是 DOM 解析和名称匹配方法（与 GeographyTaskStrategy、FactionTaskStrategy 完全重复）。重构后 WorldviewTaskStrategy 的职责将大幅简化：编排 9 个步骤 + 调用精简模板 + 保存核心世界观记录 + 最终结果回查拼装。

**Primary recommendation:** 按两波执行 — Wave 1 提取 WorldviewXmlParser 公共工具类并让三个 Strategy 调用它（消除重复）；Wave 2 重构 WorldviewTaskStrategy 为 9 步骤编排（注入三个 Strategy + 调用精简模板 + 回查拼装结果）。

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 注入 GeographyTaskStrategy / PowerSystemTaskStrategy / FactionTaskStrategy 为 Spring Bean，直接调用其内部方法（非通过 TaskService 创建子任务）
- **D-02:** 为三个独立 Strategy 各添加一个 public 的完整流程方法（如 `executeFullFlow(projectId, config)`），内部编排 clean -> generate -> save 三步，避免 WorldviewTaskStrategy 直接调用 private 方法
- **D-03:** WorldviewTaskStrategy 的 `check_existing` 步骤仅删除旧世界观记录本身（`novel_worldview`）和关联表（`novel_worldview_power_system`），模块数据清理由各独立 Strategy 的 clean 步骤负责
- **D-04:** 9 步骤结构，每模块独立步骤可见于进度报告：check_existing / clean_geography / generate_geography / clean_power_system / generate_power_system / clean_faction / generate_faction / generate_core / save_core
- **D-05:** FactionTaskStrategy 生成时的依赖上下文（geographyContext / powerSystemContext）在 WorldviewTaskStrategy 中构建（调用 ContinentRegionService.buildGeographyText 和 PowerSystemService.buildPowerSystemConstraint），传入 Strategy 方法
- **D-06:** 所有模块入库完成后，从 DB 回查全部数据（世界观 + 地理环境 + 力量体系 + 阵营势力），组装为完整的 `NovelWorldview` 对象作为最终步骤的结果
- **D-07:** 使用现有的 `NovelWorldviewMapper.selectOne` + 各 Service 的 list/fill 方法回查拼装，确保前端收到与 DB 一致的数据
- **D-08:** 提取 `WorldviewXmlParser` 工具类（`com.aifactory.common` 包），包含：地理环境 DOM 解析、势力 DOM 解析、三级名称匹配、中文到英文映射、Record 类型
- **D-09:** WorldviewTaskStrategy、GeographyTaskStrategy、FactionTaskStrategy 均改为调用 WorldviewXmlParser，删除各自的重复 DOM 解析和名称匹配代码
- **D-10:** PowerSystemTaskStrategy 使用 Jackson XmlParser（非 DOM 解析），不受此工具类影响

### Claude's Discretion
- WorldviewXmlParser 的具体方法签名和返回类型设计
- 各 Strategy 的 executeFullFlow 方法是否返回具体结果对象或仅返回成功/失败
- 错误处理细节（某模块生成失败时的回滚策略）

### Deferred Ideas (OUT OF SCOPE)
None - discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REFACT-01 | WorldviewTaskStrategy 整体生成流程重构为：先调用三个独立生成任务，再调用剔除后的世界观提示词生成剩余内容 | D-01/D-02/D-04 决定了组合调用方式；D-03 决定了清理职责划分；9 步骤结构中 steps 2-7 调用三个独立 Strategy，steps 8-9 调用精简模板 |
| REFACT-02 | 整体生成流程中三个模块的生成结果汇总后仍作为完整世界观数据返回给前端 | D-06/D-07 决定了回查拼装方式：所有模块入库后从 DB 回查，使用 NovelWorldviewMapper.selectOne + ContinentRegionService.fillGeography + FactionService.fillForces 组装完整对象 |
</phase_requirements>

## Standard Stack

### Core (existing, no new dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot 3.2 | 3.2.0 | Dependency injection, @Autowired | Project standard for all backend services |
| MyBatis-Plus | 3.5.5 | Database CRUD | Project standard ORM, used in all mappers |
| Java DOM (org.w3c.dom) | JDK built-in | XML parsing for <g>/<f> nested tags | Already used in all three strategies; Jackson XML cannot handle nested same-name tags |
| Jackson XmlMapper | from spring-boot | XML parsing for <p> power system tags | Already used via XmlParser.java; PowerSystemTaskStrategy only |

### No new dependencies required
This phase is purely refactoring existing code. All libraries are already in the project.

## Architecture Patterns

### Current Architecture (before refactoring)
```
WorldviewTaskStrategy (920 lines)
  ├── check_existing: deletes ALL old data (worldview + geography + power_system + faction)
  ├── generate_worldview: single AI call with full template
  └── save_worldview: parses ALL modules from single AI response
      ├── saveGeographyRegionsFromXml() — DOM parsing <g> (duplicate)
      ├── savePowerSystems() — Jackson parsing <p>
      ├── saveFactionsFromXml() — DOM parsing <f> (duplicate)
      ├── findRegionIdByName() — 3-tier matching (duplicate)
      ├── findPowerSystemIdByName() — 3-tier matching (duplicate)
      ├── mapFactionType() — Chinese->English (duplicate)
      ├── mapRelationType() — Chinese->English (duplicate)
      ├── PendingFactionAssociations record (duplicate)
      └── PendingRelation record (duplicate)
```

### Target Architecture (after refactoring)
```
WorldviewXmlParser (new utility, com.aifactory.common)
  ├── parseGeographyXml() — extracts <g>, DOM parses, returns List<NovelContinentRegion>
  ├── parseFactionXml() — extracts <f>, DOM parses, returns ParsedFactions result
  ├── findRegionIdByName() — 3-tier name matching
  ├── findPowerSystemIdByName() — 3-tier name matching
  ├── mapFactionType() — Chinese->English mapping
  ├── mapRelationType() — Chinese->English mapping
  ├── record ParsedFactions(List<NovelFaction>, List<PendingAssociation>)
  ├── record PendingAssociation(String factionName, List<String> regionNames, List<PendingRelation>)
  └── record PendingRelation(String targetName, String type)

GeographyTaskStrategy (simplified)
  ├── clean_geography — calls continentRegionService.deleteByProjectId
  ├── generate_geography — calls llm_geography_create template + LLM
  ├── save_geography — calls WorldviewXmlParser.parseGeographyXml + continentRegionService.saveTree
  └── executeFullFlow(projectId, config) — public method orchestrating clean->generate->save

PowerSystemTaskStrategy (unchanged by WorldviewXmlParser)
  ├── clean/generate/save — unchanged, uses Jackson XmlParser
  └── executeFullFlow(projectId, config) — public method

FactionTaskStrategy (simplified)
  ├── clean_faction — calls factionService.deleteByProjectId
  ├── generate_faction — calls llm_faction_create template + LLM (with context injection)
  ├── save_faction — calls WorldviewXmlParser.parseFactionXml + factionService.saveTree + association pass
  └── executeFullFlow(projectId, config) — public method

WorldviewTaskStrategy (refactored, ~200 lines)
  ├── Step 1: check_existing — delete worldview record + worldview_power_system associations only
  ├── Step 2: clean_geography — call geographyStrategy.cleanGeography(projectId)
  ├── Step 3: generate_geography — call geographyStrategy.generateGeography(projectId, config)
  ├── Step 4: clean_power_system — call powerSystemStrategy.cleanPowerSystem(projectId)
  ├── Step 5: generate_power_system — call powerSystemStrategy.generatePowerSystem(projectId, config)
  ├── Step 6: clean_faction — call factionStrategy.cleanFaction(projectId)
  ├── Step 7: generate_faction — call factionStrategy.generateFaction(projectId, config, geographyCtx, powerSystemCtx)
  ├── Step 8: generate_core — call llm_worldview_create (slim template) for <t>/<b>/<l>/<r>
  └── Step 9: save_core — parse <t>/<b>/<l>/<r>, save worldview record, create power_system associations,
              then query DB to assemble complete NovelWorldview result
```

### Recommended executeFullFlow Pattern
Per D-02, each Strategy gets a public method. Recommended design:

```java
// In GeographyTaskStrategy:
public GeographyFlowResult executeFullFlow(Long projectId, JsonNode config) {
    StepResult clean = cleanGeography(projectId);
    if (!clean.isSuccess()) return GeographyFlowResult.failure(clean.getErrorMessage());
    StepResult generate = generateGeography(projectId, config);
    if (!generate.isSuccess()) return GeographyFlowResult.failure(generate.getErrorMessage());
    StepResult save = saveGeography(projectId);
    if (!save.isSuccess()) return GeographyFlowResult.failure(save.getErrorMessage());
    return GeographyFlowResult.success();
}
```

Alternative (simpler): return StepResult directly, since WorldviewTaskStrategy already handles StepResult. This avoids creating new result types.

```java
// Simpler approach - reuse StepResult:
public StepResult executeFullFlow(Long projectId, JsonNode config) {
    StepResult clean = cleanGeographyInternal(projectId);
    if (!clean.isSuccess()) return clean;
    StepResult gen = generateGeographyInternal(projectId, config);
    if (!gen.isSuccess()) return gen;
    StepResult save = saveGeographyInternal(projectId);
    return save;
}
```

The existing private methods (cleanGeography, generateGeography, saveGeography) each have a signature of `(AiTaskStep, TaskContext)` due to the TaskStrategy interface. The new public executeFullFlow should NOT require AiTaskStep/TaskContext since WorldviewTaskStrategy calls it directly, not through AsyncTaskExecutor.

**Recommendation:** Refactor the three private step methods to extract their core logic into internal methods that take `(Long projectId, JsonNode config)` instead of `(AiTaskStep, TaskContext)`. The executeStep methods call these internal methods, and executeFullFlow also calls them. This avoids creating fake AiTaskStep/TaskContext objects.

### Anti-Patterns to Avoid
- **Calling TaskService.createTask for sub-tasks:** D-01 explicitly forbids this. Inject Strategy beans directly.
- **Passing fake AiTaskStep/TaskContext:** The executeFullFlow method should have its own parameter types, not reuse the TaskStrategy interface types.
- **Deleting module data in check_existing:** D-03 explicitly limits check_existing to worldview record + associations only. Module data cleaning is delegated to each Strategy's clean step.
- **Trying to combine WorldviewXmlParser with existing XmlParser:** They serve different purposes. XmlParser uses Jackson for simple POJO mapping; WorldviewXmlParser uses DOM for complex nested same-name tag parsing. Keep them separate.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Context text building | Custom geography/power_system text serializers | ContinentRegionService.buildGeographyText() + PowerSystemService.buildPowerSystemConstraint() | Already implemented and tested in Phase 7 |
| Tree saving | Custom recursive insert logic | ContinentRegionService.saveTree() + FactionService.saveTree() | Handles parent_id/deep calculation, already used by all strategies |
| Template execution | Manual string replacement | PromptTemplateService.executeTemplate(code, vars) | Hutool StrUtil.format with DB-stored templates |
| Final result assembly | Custom DB query joining all tables | NovelWorldviewMapper.selectOne + ContinentRegionService.fillGeography + FactionService.fillForces | D-07 explicitly requires this pattern |

**Key insight:** This phase is almost entirely about re-wiring existing methods into a new orchestration. Very little new logic is needed.

## Common Pitfalls

### Pitfall 1: Step method signature mismatch
**What goes wrong:** The TaskStrategy interface requires `executeStep(AiTaskStep, TaskContext)` but executeFullFlow needs to call the same logic without AiTaskStep/TaskContext objects. If you try to create fake AiTaskStep objects, you get null pointer exceptions or need to mock complex objects.
**Why it happens:** The current step methods couple their logic to the TaskStrategy interface types.
**How to avoid:** Extract core logic into internal methods with clean signatures like `cleanGeographyInternal(Long projectId)`. Both executeStep and executeFullFlow call the internal method.
**Warning signs:** Code creating new AiTaskStep() with only some fields set, or constructing TaskContext with dummy values.

### Pitfall 2: Forgetting worldview-power_system association in save_core
**What goes wrong:** PowerSystemTaskStrategy intentionally skips creating `novel_worldview_power_system` associations (worldview may not exist during independent generation). If save_core also forgets, the association table stays empty and the frontend won't see power systems linked to worldview.
**Why it happens:** The association was previously created inside savePowerSystems in the old WorldviewTaskStrategy. After refactoring, it must be explicitly created in the save_core step after inserting the worldview record.
**How to avoid:** The save_core step MUST: (1) insert worldview record, (2) query all power systems for project, (3) insert worldview_power_system associations for each.
**Warning signs:** Frontend shows empty power system list after worldview generation.

### Pitfall 3: Context injection timing for faction generation
**What goes wrong:** FactionTaskStrategy needs geographyContext and powerSystemContext. These must be built AFTER geography and power system are generated and saved to DB (steps 3 and 5 complete), not before.
**Why it happens:** Building context before the data is generated yields empty strings, causing faction generation with no reference data.
**How to avoid:** In step 7 (generate_faction), call ContinentRegionService.buildGeographyText(projectId) and PowerSystemService.buildPowerSystemConstraint(projectId) AFTER steps 3 and 5 have completed and data is in DB.
**Warning signs:** AI-generated factions have no region or power system references, or faction-region associations all fail name matching.

### Pitfall 4: Step result not propagated correctly in 9-step flow
**What goes wrong:** If step 3 (generate_geography) fails but step 4 (clean_power_system) still runs, the project ends up with no geography data but also no power system data (cleaned but not regenerated).
**Why it happens:** AsyncTaskExecutor stops on first failure (line 231: returns on step failure), but if a strategy's executeFullFlow method doesn't propagate failures, the outer executor won't know.
**How to avoid:** Each step must return StepResult.failure() on error. The AsyncTaskExecutor already handles stopping the task on failure. Do NOT catch and swallow exceptions in executeFullFlow.
**Warning signs:** Task shows "completed" status but some modules have no data.

### Pitfall 5: WorldviewXmlParser dependencies on Spring beans
**What goes wrong:** If WorldviewXmlParser is made a @Component with @Autowired services, the parse methods need projectId to query DB for name matching. But if it's a pure utility class with no Spring dependency, callers must pass in the required data.
**Why it happens:** The name matching methods (findRegionIdByName, findPowerSystemIdByName) currently call service methods that query the DB.
**How to avoid:** Make WorldviewXmlParser a @Component that @Autowired ContinentRegionService and PowerSystemService. The parse methods take projectId and internally call the services for name matching. This matches the existing pattern where strategies inject services.
**Warning signs:** WorldviewXmlParser cannot resolve names because it has no DB access.

### Pitfall 6: Double-cleaning of module data
**What goes wrong:** Both check_existing (step 1) and the individual clean steps (2, 4, 6) try to delete the same data.
**Why it happens:** The old check_existing deleted everything (worldview + geography + power_system + faction). After refactoring, D-03 says check_existing only deletes worldview + associations, and each Strategy's clean step handles its own module.
**How to avoid:** check_existing must NOT call continentRegionService.deleteByProjectId or factionService.deleteByProjectId or powerSystemService.deleteById. Only delete: (1) worldview record, (2) worldview_power_system association rows.
**Warning signs:** Geography data deleted twice, or module clean steps find nothing to clean.

## Code Examples

### WorldviewXmlParser.parseGeographyXml signature
```java
// Source: extracted from GeographyTaskStrategy.saveGeographyRegionsFromXml
@Component
public class WorldviewXmlParser {
    @Autowired
    private ContinentRegionService continentRegionService;
    @Autowired
    private PowerSystemService powerSystemService;

    /**
     * Parse <g> XML fragment from AI response into tree of NovelContinentRegion
     * @return List of root region nodes with children populated, or empty list if no <g> found
     */
    public List<NovelContinentRegion> parseGeographyXml(String aiResponse, Long projectId) {
        // Extract <g>...</g> fragment
        // DOM parse into NovelContinentRegion tree
        // Return root nodes list
    }

    /**
     * Parse <f> XML fragment from AI response into faction data + pending associations
     * @return ParsedFactions containing root factions and their pending associations
     */
    public ParsedFactions parseFactionXml(String aiResponse, Long projectId) {
        // Extract <f>...</f> fragment
        // DOM parse into NovelFaction tree + collect pending associations
        // Return ParsedFactions record
    }
}
```

### WorldviewTaskStrategy 9-step createSteps
```java
// Source: from D-04 decision
@Override
public List<StepConfig> createSteps(AiTask task) {
    List<StepConfig> steps = new ArrayList<>();
    steps.add(new StepConfig(1, "检查现有世界观", "check_existing", new HashMap<>()));
    steps.add(new StepConfig(2, "清理旧地理环境", "clean_geography", new HashMap<>()));
    steps.add(new StepConfig(3, "生成地理环境", "generate_geography", new HashMap<>()));
    steps.add(new StepConfig(4, "清理旧力量体系", "clean_power_system", new HashMap<>()));
    steps.add(new StepConfig(5, "生成力量体系", "generate_power_system", new HashMap<>()));
    steps.add(new StepConfig(6, "清理旧阵营势力", "clean_faction", new HashMap<>()));
    steps.add(new StepConfig(7, "生成阵营势力", "generate_faction", new HashMap<>()));
    steps.add(new StepConfig(8, "生成核心世界观", "generate_core", new HashMap<>()));
    steps.add(new StepConfig(9, "保存核心世界观", "save_core", new HashMap<>()));
    return steps;
}
```

### Final result assembly (D-06/D-07)
```java
// In save_core step, after all data is in DB:
private StepResult saveCore(AiTaskStep step, TaskContext context) {
    Long projectId = context.getProjectId();
    String aiResponse = (String) context.getSharedData("coreAiResponse");

    // Parse <t>/<b>/<l>/<r> from slim template AI response
    WorldSettingXmlDto dto = xmlParser.parse(aiResponse, WorldSettingXmlDto.class);

    // Save worldview record
    NovelWorldview worldview = new NovelWorldview();
    worldview.setProjectId(projectId);
    worldview.setWorldBackground(dto.getBackground());
    worldview.setTimeline(dto.getTimeline());
    worldview.setRules(dto.getRules());
    worldviewMapper.insert(worldview);

    // Create power system associations
    List<NovelPowerSystem> systems = powerSystemService.listByProjectId(projectId);
    for (NovelPowerSystem ps : systems) {
        NovelWorldviewPowerSystem assoc = new NovelWorldviewPowerSystem();
        assoc.setWorldviewId(worldview.getId());
        assoc.setPowerSystemId(ps.getId());
        worldviewPowerSystemMapper.insert(assoc);
    }

    // Update project setup stage
    updateProjectSetupStage(projectId, "worldview_configured");

    // Re-query complete data for frontend (D-06/D-07)
    NovelWorldview complete = worldviewMapper.selectOne(
        new LambdaQueryWrapper<NovelWorldview>().eq(NovelWorldview::getProjectId, projectId)
    );
    continentRegionService.fillGeography(complete);
    factionService.fillForces(complete);

    return StepResult.success(Map.of("worldviewId", complete.getId()), 100);
}
```

### GeographyTaskStrategy executeFullFlow pattern
```java
// Per D-02: public method for WorldviewTaskStrategy to call directly
public StepResult executeFullFlow(Long projectId, JsonNode config) {
    // Step 1: Clean
    continentRegionService.deleteByProjectId(projectId);

    // Step 2: Generate (AI call)
    Map<String, Object> variables = buildTemplateVariables(projectId, config);
    String prompt = promptTemplateService.executeTemplate("llm_geography_create", variables);
    AIGenerateResponse aiResponse = callLlm(projectId, prompt, "llm_geography_create");

    // Step 3: Parse and save
    List<NovelContinentRegion> regions = worldviewXmlParser.parseGeographyXml(
        aiResponse.getContent(), projectId);
    if (!regions.isEmpty()) {
        continentRegionService.saveTree(projectId, regions);
    }

    return StepResult.success(Map.of("saved", true), 100);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Single AI call for full worldview | 4 AI calls (3 modules + core) | Phase 6/7/8 | Higher total AI cost but better modularity |
| DOM parsing code duplicated in 3 files | Shared WorldviewXmlParser | Phase 8 | Single source of truth for parsing logic |
| WorldviewTaskStrategy handles all parsing | Each Strategy handles own module | Phase 7/8 | WorldviewTaskStrategy becomes orchestrator only |

**Note on approach change:**
- Old WorldviewTaskStrategy: 1 AI call, 1 parse pass, ~920 lines
- New WorldviewTaskStrategy: orchestrator only, ~200 lines
- Three independent Strategies: ~300 lines each (unchanged except WorldviewXmlParser extraction)
- New WorldviewXmlParser: ~250 lines

## Open Questions

1. **executeFullFlow return type (Claude's Discretion)**
   - What we know: D-02 says add public method. Options are StepResult or a custom result type.
   - Recommendation: Use StepResult for simplicity. All callers already understand it. No new types needed.

2. **Error handling on partial failure (Claude's Discretion)**
   - What we know: If step 3 (generate_geography) fails, steps 4-9 won't execute (AsyncTaskExecutor stops on failure). But data from step 2 (clean_geography) already ran, so old geography is deleted.
   - Recommendation: Accept this behavior. It matches the current pattern where check_existing deletes data before generation. Users can retry the entire generation. No partial rollback needed.

3. **Whether executeFullFlow should be a single method or remain as individual step calls**
   - What we know: D-02 says "executeFullFlow(projectId, config)" as one method. But the 9-step design (D-04) has clean/generate as separate steps.
   - Recommendation: Have executeFullFlow in each Strategy for potential future reuse, but WorldviewTaskStrategy's 9 steps should call individual internal methods (not executeFullFlow) so that each step maps to one AsyncTaskExecutor step for progress tracking. This aligns with D-04's requirement that "每模块独立步骤可见于进度报告".

   **REFINEMENT:** WorldviewTaskStrategy should call Strategy methods that correspond to single operations (clean only, or generate only, or save only), not executeFullFlow. The executeFullFlow method exists for the independent generation endpoints (Phase 7) where a single task runs all 3 steps. For WorldviewTaskStrategy's 9-step orchestration, we need finer-grained access.

   **ACTUALLY:** Looking more carefully at the code, GeographyTaskStrategy's current step methods are private. Per D-02 we need to add public methods. But the 9-step design requires calling clean and generate+save separately (as separate steps). So we need either: (a) three public methods per Strategy (cleanX, generateX, saveX), or (b) make executeFullFlow the only public method and have WorldviewTaskStrategy call it in a combined clean+generate step.

   **FINAL RECOMMENDATION:** Provide two public methods per Strategy:
   - `cleanData(Long projectId)` — just the clean operation
   - `generateAndSave(Long projectId, JsonNode config)` — generate + save combined

   This gives WorldviewTaskStrategy 2 callable methods per module, fitting the 9-step structure:
   - Steps 2-3: clean_geography (call cleanData) + generate_geography (call generateAndSave)
   - Steps 4-5: clean_power_system + generate_power_system
   - Steps 6-7: clean_faction + generate_faction

   However, note that the generate step needs the AI response stored somewhere for the save step. Currently this uses context.putSharedData("aiResponse"). For the public method approach, generateAndSave should handle both internally.

   **SIMPLEST APPROACH (recommended):** Keep the 3 internal methods as-is but make them package-private or add a public wrapper. The WorldviewTaskStrategy calls them directly. Since all strategies are in the same package (`com.aifactory.service.task.impl`), package-private access works.

   However, D-02 says "public" methods. So: add public methods that wrap the internal logic. WorldviewTaskStrategy calls these public methods.

## Environment Availability

> Step 2.6: SKIPPED (no external dependencies identified)
> This phase is purely Java code refactoring. All dependencies (Spring Boot, MyBatis-Plus, DOM parser) are already in the project.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | None (standard Maven test structure) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=WorldviewXmlParserTest -q` |
| Full suite command | `cd ai-factory-backend && mvn test -q` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REFACT-01 | WorldviewXmlParser extracts geography from <g> XML | unit | `mvn test -Dtest=WorldviewXmlParserTest#testParseGeographyXml -q` | Wave 0 |
| REFACT-01 | WorldviewXmlParser extracts factions from <f> XML | unit | `mvn test -Dtest=WorldviewXmlParserTest#testParseFactionXml -q` | Wave 0 |
| REFACT-01 | WorldviewXmlParser name matching works (3 tiers) | unit | `mvn test -Dtest=WorldviewXmlParserTest#testFindRegionIdByName -q` | Wave 0 |
| REFACT-01 | WorldviewTaskStrategy creates 9 steps | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCreateSteps -q` | Wave 0 |
| REFACT-01 | WorldviewTaskStrategy check_existing only deletes worldview + associations | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCheckExisting -q` | Wave 0 |
| REFACT-02 | Final result assembly fills all fields from DB | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testSaveCoreAssembly -q` | Wave 0 |
| REFACT-01 | Compilation succeeds after refactor | build | `cd ai-factory-backend && mvn compile -q` | N/A |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn compile -q`
- **Per wave merge:** `cd ai-factory-backend && mvn compile -q`
- **Phase gate:** `cd ai-factory-backend && mvn compile -q` (no existing test suite covers strategies)

### Wave 0 Gaps
- [ ] `WorldviewXmlParserTest.java` — unit tests for DOM parsing extraction
- [ ] `WorldviewTaskStrategyTest.java` — unit tests for 9-step orchestration (mock Strategy dependencies)
- [ ] Note: Existing test infrastructure is minimal (1 test file total). These tests should follow the existing JUnit 5 + Mockito pattern from `NovelCharacterChapterServiceTest.java`.

## Sources

### Primary (HIGH confidence)
- Direct code reading of all 4 TaskStrategy implementations (WorldviewTaskStrategy, GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy)
- Direct code reading of AsyncTaskExecutor, TaskStrategy interface, WorldviewController
- Direct code reading of service interfaces (ContinentRegionService, PowerSystemService, FactionService)
- Direct code reading of entities (NovelWorldview, WorldSettingXmlDto)
- CONTEXT.md decisions (D-01 through D-10) — verified against actual code

### Secondary (MEDIUM confidence)
- Phase 6 CONTEXT.md — template design decisions, verified against V4 migration plan
- Phase 7 CONTEXT.md and PLAN.md — strategy implementation patterns, verified against actual code

### Tertiary (LOW confidence)
- None — all findings verified by direct code reading

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies, all existing code refactoring
- Architecture: HIGH — target architecture fully specified by CONTEXT.md D-01 through D-10
- Pitfalls: HIGH — identified from direct code analysis and cross-referencing decisions with current implementation
- Code examples: HIGH — extracted from actual codebase, verified method signatures and call patterns

**Research date:** 2026-04-03
**Valid until:** 2026-05-03 (stable codebase, no external dependencies)
