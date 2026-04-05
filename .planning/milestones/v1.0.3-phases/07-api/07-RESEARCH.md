# Phase 7: 独立生成 API + 依赖校验 - Research

**Researched:** 2026-04-03
**Domain:** Spring Boot REST API + TaskStrategy pattern + dependency validation
**Confidence:** HIGH

## Summary

Phase 7 creates 3 independent generation REST endpoints (geography, power system, faction) by reusing the existing `TaskStrategy` + `AsyncTaskExecutor` + `TaskService` architecture. Each endpoint maps to a new `TaskStrategy` implementation that calls a Phase 6 prompt template and reuses the XML parsing logic already proven in `WorldviewTaskStrategy`. The faction endpoint adds synchronous pre-validation in the controller to check that geography and power system data exist before dispatching the async task.

**Primary recommendation:** Each new Strategy is a simplified subset of `WorldviewTaskStrategy` — one step to clean existing data, one step to call the LLM with the independent prompt template, one step to parse and persist. Copy the parsing methods directly rather than refactoring them out (Phase 8 will handle that consolidation). The dependency check for faction generation is a simple `count > 0` query in the controller before creating the task.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 独立重新生成时采用"先清后生"策略 — 先删除该项目对应模块的旧数据，再执行 AI 生成，与现有世界观整体生成的 check_existing 行为一致
- **D-02:** 阵营势力重新生成时全部清除（含用户手动添加的势力-人物关联），生成后用户可重新调整。现有级联删除逻辑已覆盖关联表清理
- **D-03:** 3 个独立生成端点扩展 WorldviewController，与现有 `generate-async` 同级，路径为：
  - `POST /api/novel/{projectId}/worldview/generate-geography`
  - `POST /api/novel/{projectId}/worldview/generate-power-system`
  - `POST /api/novel/{projectId}/worldview/generate-faction`
- **D-04:** 复用现有 TaskService + AsyncTaskExecutor + TaskStrategy 架构，创建 3 个新 TaskStrategy 实现类：
  - `GeographyTaskStrategy`（taskType = `"geography"`）— 调用 `llm_geography_create` 模板，解析 `<g>` XML 入库
  - `PowerSystemTaskStrategy`（taskType = `"power_system"`）— 调用 `llm_power_system_create` 模板，解析 `<p>` 数据入库
  - `FactionTaskStrategy`（taskType = `"faction"`）— 调用 `llm_faction_create` 模板，注入依赖上下文，解析 `<f>` XML 入库
- **D-05:** 前端可复用现有 task 轮询逻辑（GET /api/task/{taskId}/status）
- **D-06:** 阵营势力生成的依赖校验放在 Controller 层同步执行，不创建 task：
  - 检查 `novel_continent_region` 是否有数据 -> 无则返回 `Result.error("请先生成地理环境数据")`
  - 检查 `novel_power_system` 是否有数据 -> 无则返回 `Result.error("请先生成力量体系数据")`
  - 两者都有 -> 创建 task 并异步执行
- **D-07:** 地理环境和力量体系独立生成无前置依赖，直接创建 task 执行

### Claude's Discretion
- 3 个 TaskStrategy 的具体步骤划分（createSteps / executeStep）
- 上下文序列化实现细节（从 DB 读取已入库数据并构建 geographyContext/powerSystemContext）
- 错误提示文案的最终措辞
- Strategy 内部是否复用 WorldviewTaskStrategy 中的解析方法还是提取公共工具方法

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| API-01 | 地理环境独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成并入库 | GeographyTaskStrategy + `llm_geography_create` template + `continentRegionService.saveTree()` — all patterns verified in WorldviewTaskStrategy |
| API-02 | 力量体系独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成并入库 | PowerSystemTaskStrategy + `llm_power_system_create` template + `savePowerSystems()` logic — reuses existing pattern |
| API-03 | 阵营势力独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成并入库 | FactionTaskStrategy + `llm_faction_create` template + `saveFactionsFromXml()` logic — two-pass association pattern |
| DEP-01 | 阵营势力生成前置校验：未生成地理环境则拒绝并返回提示 | Controller-level check using `ContinentRegionService.listByProjectId()` — returns `Result.error()` before task creation |
| DEP-02 | 阵营势力生成前置校验：未生成力量体系则拒绝并返回提示 | Controller-level check using `PowerSystemService.listByProjectId()` — returns `Result.error()` before task creation |
| DEP-03 | 阵营势力提示词模板自动注入已生成的地理环境和力量体系结构化数据 | FactionTaskStrategy reads DB data, serializes to XML format, passes as `geographyContext` and `powerSystemContext` template variables |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot 3.2.0 | 3.2.0 | Backend framework | Existing project standard |
| MyBatis-Plus 3.5.5 | 3.5.5 | ORM framework | Existing project standard |
| LangChain4j | 1.11.0 | AI orchestration | Existing project standard |
| Jackson XML | (managed by Spring Boot) | XML serialization | Existing project standard |
| Java DOM Parser | JDK built-in | XML parsing for `<g>` and `<f>` tags | Required for nested same-name tags |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hutool StrUtil | 5.8.24 | Template variable substitution | Used by PromptTemplateService |
| SpringDoc OpenAPI | 2.3.0 | API documentation | Swagger annotations on new endpoints |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Copy parsing methods into each Strategy | Extract shared utility class | Shared utility adds refactoring scope; Phase 8 will consolidate anyway. Copy now, consolidate later |

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-backend/src/main/java/com/aifactory/
├── controller/
│   └── WorldviewController.java          # ADD 3 new endpoints
├── service/task/impl/
│   ├── WorldviewTaskStrategy.java        # EXISTING (reference)
│   ├── GeographyTaskStrategy.java        # NEW
│   ├── PowerSystemTaskStrategy.java      # NEW
│   └── FactionTaskStrategy.java          # NEW
```

### Pattern 1: TaskStrategy Implementation
**What:** Each independent generation strategy implements `TaskStrategy` with `getTaskType()`, `createSteps()`, and `executeStep()`.
**When to use:** Every async generation task in this project.
**Example:**
```java
@Slf4j
@Component
public class GeographyTaskStrategy implements TaskStrategy {

    @Override
    public String getTaskType() {
        return "geography";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧地理环境数据", "clean_geography", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成地理环境", "generate_geography", new HashMap<>()));
        steps.add(new StepConfig(3, "保存地理环境", "save_geography", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        switch (step.getStepType()) {
            case "clean_geography": return cleanGeography(step, context);
            case "generate_geography": return generateGeography(step, context);
            case "save_geography": return saveGeography(step, context);
            default: return StepResult.failure("未知步骤: " + step.getStepType());
        }
    }
}
```

### Pattern 2: Controller Endpoint with Task Creation
**What:** Controller validates project, builds config, creates task, returns taskId.
**When to use:** All async generation endpoints.
**Example:**
```java
@PostMapping("/generate-geography")
public Result<Map<String, Object>> generateGeography(
        @PathVariable Long projectId,
        @RequestBody(required = false) Map<String, String> request) {
    try {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.error("项目不存在");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("projectDescription", project.getDescription());
        config.put("storyTone", project.getStoryTone());
        // ... build config from project fields

        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("geography");
        taskRequest.setTaskName("AI生成地理环境");
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);
        if (result.getOk() == null || !result.getOk()) {
            return Result.error(result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "地理环境生成任务已创建");
        return Result.ok(response);
    } catch (Exception e) {
        log.error("创建地理环境生成任务失败，projectId={}", projectId, e);
        return Result.error("创建任务失败：" + e.getMessage());
    }
}
```

### Pattern 3: Dependency Validation in Controller (Faction only)
**What:** Synchronous check before async task creation.
**When to use:** Only for faction generation which depends on geography and power system data.
**Example:**
```java
@PostMapping("/generate-faction")
public Result<Map<String, Object>> generateFaction(
        @PathVariable Long projectId,
        @RequestBody(required = false) Map<String, String> request) {
    try {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.error("项目不存在");
        }

        // Dependency validation (D-06)
        List<NovelContinentRegion> regions = continentRegionService.listByProjectId(projectId);
        if (regions.isEmpty()) {
            return Result.error("请先生成地理环境数据");
        }

        List<NovelPowerSystem> powerSystems = powerSystemService.listByProjectId(projectId);
        if (powerSystems.isEmpty()) {
            return Result.error("请先生成力量体系数据");
        }

        // Build config including dependency context
        // ...
        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setTaskType("faction");
        // ...
        return Result.ok(response);
    } catch (Exception e) { ... }
}
```

### Pattern 4: Context Serialization for Faction Template
**What:** Read existing structured data from DB, serialize to XML format matching the template's expected `{geographyContext}` and `{powerSystemContext}` variables.
**When to use:** FactionTaskStrategy's generate step.
**Key insight:** The Phase 6 faction template expects raw XML in the same format AI outputs (`<g>...</g>` and `<p>...</p>`). The simplest approach is to use `ContinentRegionService.buildGeographyText()` for a text summary, but the template expects structured XML. Two options exist:
1. **Re-serialize from DB to XML** — read `NovelContinentRegion` tree, build `<g><r><n>name</n><d>desc</d><r>...</r></r></g>` string
2. **Use text summaries** — use existing `buildGeographyText()` and `buildPowerSystemConstraint()` but the template wraps them in `<existing_geography>` tags

**Recommendation:** Use text summaries from existing service methods. The `<existing_geography>` / `<existing_power_systems>` wrapper tags in the template serve as delimiters. The AI understands both formats. This avoids building a new XML serializer and reuses tested code.

### Anti-Patterns to Avoid
- **Do not put dependency validation inside the TaskStrategy.** Per D-06, validation is in the controller. If validation were async (inside Strategy), the user would get a taskId but the task would fail — poor UX.
- **Do not refactor WorldviewTaskStrategy to share methods yet.** Phase 8 handles that. Phase 7 copies the needed parsing logic into each new Strategy.
- **Do not use Jackson XML to parse `<g>` or `<f>` tags.** The nested same-name `<r>` and `<faction>` tags break Jackson XML deserialization. Use DOM parser as WorldviewTaskStrategy already does.
- **Do not forget the `@Component` annotation** on new Strategy classes. Without it, `AsyncTaskExecutor.strategyMap` auto-injection won't pick them up.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Task lifecycle management | Custom async execution | TaskService + AsyncTaskExecutor | Existing infrastructure handles status tracking, step management, cancellation |
| Template variable substitution | Custom string replacement | PromptTemplateService.executeTemplate() | Handles version management, Hutool StrUtil.format, variable validation |
| Geography tree save | Manual insert logic | ContinentRegionService.saveTree() | Handles parent_id, deep, sort_order calculation recursively |
| Power system cascade delete | Manual per-table deletes | PowerSystemService.deleteById() | Handles worldview association + levels + steps cascade |
| Faction cascade delete | Manual per-table deletes | FactionService.deleteByProjectId() | Handles faction-region, faction-relation, and recursive children |

**Key insight:** Every data operation needed already exists as a service method. The new Strategies are orchestration glue, not data layer code.

## Common Pitfalls

### Pitfall 1: AsyncTaskExecutor Strategy Bean Naming
**What goes wrong:** AsyncTaskExecutor resolves strategies by converting taskType to camelCase and appending "TaskStrategy" (e.g., `"geography"` -> `"geographyTaskStrategy"`). Spring's default bean name for `GeographyTaskStrategy` is `"geographyTaskStrategy"` (lowercase first letter of class name). These match. BUT for `"power_system"`, the camelCase conversion produces `"powerSystemTaskStrategy"`, and the class `PowerSystemTaskStrategy` produces bean name `"powerSystemTaskStrategy"`. These also match. No issue, but verify.
**Why it happens:** The `toCamelCase()` method in AsyncTaskExecutor handles snake_case -> camelCase conversion.
**How to avoid:** Verify bean names match by checking `strategyMap.keySet()` in logs after deployment. All three task types (`geography`, `power_system`, `faction`) should resolve correctly with the naming convention.
**Warning signs:** "找不到对应的任务策略" error in logs.

### Pitfall 2: Missing Power System Worldview Association
**What goes wrong:** `WorldviewTaskStrategy.savePowerSystems()` creates `NovelWorldviewPowerSystem` association records. The new `PowerSystemTaskStrategy` must decide whether to create these associations too. However, independent power system generation happens without a worldview record existing.
**Why it happens:** The worldview entity (`novel_worldview`) may not exist when power system is generated independently. The association table requires a `worldviewId`.
**How to avoid:** Do NOT create worldview-power-system associations in `PowerSystemTaskStrategy`. The power system data is stored in `novel_power_system` with `projectId` — that is sufficient for independent generation. Phase 8's `WorldviewTaskStrategy` refactor will handle associations when it orchestrates all three.
**Warning signs:** NullPointerException or foreign key constraint violation on `novel_worldview_power_system.worldview_id`.

### Pitfall 3: Geography Context Serialization Format
**What goes wrong:** The faction template uses `{geographyContext}` inside `<existing_geography>` tags. If the serialized data contains malformed XML or CDATA issues, the LLM prompt will be garbled.
**Why it happens:** DB text fields may contain characters that break XML (quotes, angle brackets, etc).
**How to avoid:** Use `ContinentRegionService.buildGeographyText()` which produces plain text (not XML), placed inside the `<existing_geography>` wrapper that the template already provides. For power systems, use `PowerSystemService.buildPowerSystemConstraint()` which also produces plain text. The template wrapper tags provide enough structure for the LLM to understand the context.
**Warning signs:** LLM generates malformed faction output or ignores the geography context entirely.

### Pitfall 4: Re-generation Race Condition
**What goes wrong:** If a user clicks "generate geography" twice quickly, two tasks may run concurrently — both delete old data then both try to insert new data.
**Why it happens:** The clean step and generate step are separate, and no lock prevents concurrent execution.
**How to avoid:** This is an existing limitation in the architecture (same issue exists for `generate-async`). The frontend should disable the button while a task is running. The task polling mechanism already supports this pattern. Not blocking for Phase 7.
**Warning signs:** Duplicate data in database after rapid clicks.

### Pitfall 5: Config Not Passed to Strategy
**What goes wrong:** The controller builds a config Map with project fields and passes it via `CreateTaskRequest.config`. This gets serialized to `AiTask.configJson`. The Strategy reads it from `TaskContext.getConfig()`. If variable names don't match between controller and strategy, template variables won't be filled.
**Why it happens:** Controller builds config, Strategy reads it — two separate code locations must agree on key names.
**How to avoid:** Use consistent key names: `projectDescription`, `storyTone`, `storyGenre`, `tagsSection` (matching the template variables). For FactionTaskStrategy, add `geographyContext` and `powerSystemContext` to the config in the controller (not the strategy — the controller already has the data from the dependency check).
**Warning signs:** Template returns literal `{projectDescription}` instead of actual values.

## Code Examples

### GeographyTaskStrategy — Generate Step
```java
// Source: Modeled on WorldviewTaskStrategy.generateWorldview()
private StepResult generateGeography(AiTaskStep step, TaskContext context) {
    try {
        JsonNode config = context.getConfig();
        String projectDescription = getTextOrDefault(config, "projectDescription", "待补充");
        String storyTone = getTextOrDefault(config, "storyTone", "待补充");
        String storyGenre = getTextOrDefault(config, "storyGenre", "待补充");
        String tags = config.has("tags") ? config.get("tags").asText() : "";

        Map<String, Object> variables = new HashMap<>();
        variables.put("projectDescription", projectDescription);
        variables.put("storyTone", storyTone);
        variables.put("storyGenre", storyGenre);
        variables.put("tagsSection", tags != null && !tags.isEmpty() ? "【标签】" + tags : "");

        String prompt = promptTemplateService.executeTemplate("llm_geography_create", variables);

        AIGenerateRequest aiRequest = new AIGenerateRequest();
        aiRequest.setProjectId(context.getProjectId());
        aiRequest.setRequestType("llm_geography_create");
        aiRequest.setRole(AIRole.NOVEL_WRITER);
        aiRequest.setTask(prompt);

        AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
        String responseContent = aiResponse.getContent();

        context.putSharedData("aiResponse", responseContent);
        return StepResult.success(Map.of("aiResponse", responseContent), 100);
    } catch (Exception e) {
        log.error("AI生成地理环境失败", e);
        return StepResult.failure("AI生成地理环境失败: " + e.getMessage());
    }
}
```

### GeographyTaskStrategy — Save Step (reusing DOM parsing from WorldviewTaskStrategy)
```java
// Source: Copied from WorldviewTaskStrategy.saveGeographyRegionsFromXml()
private StepResult saveGeography(AiTaskStep step, TaskContext context) {
    try {
        Long projectId = context.getProjectId();
        String aiResponse = (String) context.getSharedData("aiResponse");
        if (aiResponse == null) {
            return StepResult.failure("未找到AI生成的地理环境内容");
        }

        // Parse and save using DOM (same logic as WorldviewTaskStrategy.saveGeographyRegionsFromXml)
        saveGeographyRegionsFromXml(projectId, aiResponse);
        return StepResult.success(Map.of("projectId", projectId), 100);
    } catch (Exception e) {
        log.error("保存地理环境失败", e);
        return StepResult.failure("保存地理环境失败: " + e.getMessage());
    }
}
```

### FactionTaskStrategy — Context Building in Controller
```java
// Source: New pattern for D-06 dependency validation + D-04 context injection
@PostMapping("/generate-faction")
public Result<Map<String, Object>> generateFaction(
        @PathVariable Long projectId,
        @RequestBody(required = false) Map<String, String> request) {
    try {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.error("项目不存在");
        }

        // D-06: Dependency validation
        List<NovelContinentRegion> regions = continentRegionService.listByProjectId(projectId);
        if (regions.isEmpty()) {
            return Result.error("请先生成地理环境数据");
        }
        List<NovelPowerSystem> powerSystems = powerSystemService.listByProjectId(projectId);
        if (powerSystems.isEmpty()) {
            return Result.error("请先生成力量体系数据");
        }

        // Build context data (D-04: serialized for template injection)
        String geographyContext = continentRegionService.buildGeographyText(projectId);
        String powerSystemContext = powerSystemService.buildPowerSystemConstraint(projectId);

        Map<String, Object> config = new HashMap<>();
        config.put("projectDescription", project.getDescription());
        config.put("storyTone", project.getStoryTone());
        config.put("storyGenre", project.getNovelType());
        config.put("tags", project.getTags());
        config.put("geographyContext", geographyContext);
        config.put("powerSystemContext", powerSystemContext);

        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setProjectId(projectId);
        taskRequest.setTaskType("faction");
        taskRequest.setTaskName("AI生成阵营势力");
        taskRequest.setConfig(config);

        Result<TaskDto> result = taskService.createTask(taskRequest);
        if (result.getOk() == null || !result.getOk()) {
            return Result.error(result.getMsg());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getData().getId());
        response.put("message", "阵营势力生成任务已创建");
        return Result.ok(response);
    } catch (Exception e) {
        log.error("创建阵营势力生成任务失败，projectId={}", projectId, e);
        return Result.error("创建任务失败：" + e.getMessage());
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Monolithic worldview generation | Independent module generation | Phase 6-7 (v1.0.3) | Each worldview module can be regenerated independently |

**Deprecated/outdated:**
- `llm_worldview_create` template with geography/power/faction instructions: Updated in V4 migration to only cover world type/background/timeline/rules

## Open Questions

1. **Should the geography context for faction generation use XML re-serialization or text summary?**
   - What we know: `buildGeographyText()` returns plain text with indentation. `buildPowerSystemConstraint()` returns structured text. Both are proven in chapter generation context.
   - What's unclear: Whether the LLM needs to see the same XML format it outputs for accurate name referencing.
   - Recommendation: Start with text summaries (`buildGeographyText` + `buildPowerSystemConstraint`). They are tested and sufficient. The template wraps them in `<existing_geography>` / `<existing_power_systems>` tags for structure. If name matching proves insufficient in testing, switch to XML re-serialization.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | Backend compilation | Verified | 21 | -- |
| Maven 3.8+ | Build | Verified | 3.8+ | -- |
| MySQL 8.0+ | Database | Verified | 8.0 | -- |
| Redis | Caching (disabled in dev) | -- | -- | Not needed for API creation |
| LangChain4j | LLM integration | Verified | 1.11.0 | -- |

**Missing dependencies with no fallback:** None.

**Missing dependencies with fallback:** None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + Spring Boot Test |
| Config file | None detected (Wave 0 gap) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=GeographyTaskStrategyTest -DfailIfNoTests=false` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-01 | Geography independent generation endpoint creates task and returns taskId | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateGeography` | No -- Wave 0 |
| API-02 | Power system independent generation endpoint creates task and returns taskId | unit | `mvn test -Dtest=WorldviewControllerTest#testGeneratePowerSystem` | No -- Wave 0 |
| API-03 | Faction independent generation endpoint creates task and returns taskId | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateFaction` | No -- Wave 0 |
| DEP-01 | Faction endpoint returns error when no geography data | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateFactionNoGeography` | No -- Wave 0 |
| DEP-02 | Faction endpoint returns error when no power system data | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateFactionNoPowerSystem` | No -- Wave 0 |
| DEP-03 | Faction prompt includes geography and power system context | unit | `mvn test -Dtest=FactionTaskStrategyTest#testContextInjection` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn compile`
- **Per wave merge:** `cd ai-factory-backend && mvn test`
- **Phase gate:** Full suite green + manual endpoint verification via Swagger UI

### Wave 0 Gaps
- [ ] `ai-factory-backend/src/test/java/com/aifactory/controller/WorldviewControllerTest.java` -- covers API-01/02/03, DEP-01/02
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/GeographyTaskStrategyTest.java` -- covers API-01 strategy logic
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/FactionTaskStrategyTest.java` -- covers DEP-03 context injection
- [ ] Existing test: `ai-factory-backend/src/test/java/com/aifactory/service/NovelCharacterChapterServiceTest.java` -- unrelated, no conflict

## Sources

### Primary (HIGH confidence)
- `WorldviewTaskStrategy.java` -- Full source code read and analyzed (920 lines). Contains all parsing logic to be reused.
- `AsyncTaskExecutor.java` -- Strategy resolution mechanism confirmed (toCamelCase + "TaskStrategy" bean naming).
- `TaskStrategy.java` -- Interface definition with StepConfig, StepResult, TaskContext inner classes.
- `WorldviewController.java` -- Controller pattern with generate-async endpoint as template for 3 new endpoints.
- `TaskService.java` -- Task creation flow (insert AiTask -> build TaskDto -> dispatch async -> return).
- `V4__independent_prompt_templates.sql` -- All 3 template codes confirmed: `llm_geography_create`, `llm_power_system_create`, `llm_faction_create`. Variable names verified.
- `ContinentRegionServiceImpl.java` -- `buildGeographyText()`, `saveTree()`, `deleteByProjectId()` methods verified.
- `PowerSystemServiceImpl.java` -- `buildPowerSystemConstraint()`, `listByProjectId()` methods verified.
- `Result.java` -- `Result.ok()`, `Result.error()` API response pattern confirmed.

### Secondary (MEDIUM confidence)
- `PromptContextBuilder.java` -- Existing context building patterns for reference.
- `07-CONTEXT.md` -- User decisions locked and verified against code.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- All infrastructure already exists, no new libraries needed
- Architecture: HIGH -- TaskStrategy pattern is proven with WorldviewTaskStrategy, exact same pattern applies
- Pitfalls: HIGH -- Analyzed from actual source code, not theoretical
- Context serialization: MEDIUM -- Using text summaries is the pragmatic choice but XML re-serialization might produce better LLM results for name matching

**Research date:** 2026-04-03
**Valid until:** 2026-05-03 (stable architecture, no framework changes expected)
