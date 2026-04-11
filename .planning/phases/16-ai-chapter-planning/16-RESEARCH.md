# Phase 16: AI Chapter Planning - Research

**Researched:** 2026-04-11
**Domain:** LLM chapter planning with foreshadowing awareness
**Confidence:** HIGH

## Summary

Phase 16 extends the existing chapter planning pipeline (`OutlineTaskStrategy.java`) to make the LLM foreshadowing-aware. Three capabilities are added: (1) injecting active foreshadowing context into the chapter planning prompt, (2) defining new `<fs>`/`<fp>` XML output tags for foreshadowing planting and payoffs, and (3) parsing those tags and persisting parsed foreshadowing records to `novel_foreshadowing`.

The implementation follows an established precedent from Phase 12, which added `<ch>/<cn>/<cd>/<ci>` role tags to the chapter planning output. The same regex-based DOM parsing pattern in `parseChaptersXml()` is reused and extended. The `ForeshadowingService` already supports all needed query and create operations. The primary work is in (a) modifying `buildChapterPromptUsingTemplate()` to inject a foreshadowing context variable, (b) updating the `llm_outline_chapter_generate` template to include foreshadowing instructions, (c) extending `parseChaptersXml()` with `<fs>`/`<fp>` regex extraction, and (d) adding foreshadowing persistence after chapter parsing in both `saveVolumeChaptersToDatabase()` and `saveChaptersToDatabase()`.

**Primary recommendation:** Extend `OutlineTaskStrategy` and the `llm_outline_chapter_generate` template using the exact same patterns that exist for character/role tags. Extract a shared method for foreshadowing context building to serve both planning paths (`generateChaptersInVolume` and `generateChaptersForVolume`).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Structured list format for injecting existing foreshadowing context -- each foreshadowing shows title, type, layout line, status, plant/callback locations, so LLM has full context for decisions
- **D-02:** Injection scope limited to active foreshadowing for the current volume only (status=pending or in_progress, where plantedVolume or plannedCallbackVolume matches current volume), to reduce token usage
- **D-03:** `<fs>` (foreshadowing plant) and `<fp>` (foreshadowing payoff) use sub-tag format, consistent with existing `<n>/<t>/<p>` pattern:
  - `<fs>` sub-tags: `<ft>` title, `<fy>` type (character/item/event/secret), `<fl>` layout line (bright1/bright2/bright3/dark), `<fd>` description, `<fc>` callback volume, `<fr>` callback chapter
  - `<fp>` sub-tags: `<ft>` title (matching pending foreshadowing), `<fd>` payoff method description
- **D-04:** Each `<o>` (chapter) may contain zero or more `<fs>` and `<fp>` tags, supporting multiple foreshadowing events per chapter
- **D-05:** LLM output foreshadowing creates new records directly, no name matching with existing foreshadowing. Avoids incorrect matching causing data corruption
- **D-06:** When re-planning a volume, first delete all pending foreshadowing for that volume (plantedVolume matches current volume), then recreate from LLM output. Consistent with saveChaptersToDatabase's delete-then-create pattern
- **D-07:** Both planning paths support foreshadowing injection and parsing -- `generateChaptersInVolume` (batch outline flow) and `generateChaptersForVolume` (single volume replan) both inject context and parse `<fs>`/`<fp>` tags

### Claude's Discretion
- Specific text format of the structured list (how to format so LLM easily understands)
- Template variable name for foreshadowing context injection
- Regex parsing implementation details for `<fs>`/`<fp>`
- Batch insert optimization for foreshadowing creation
- Dark line (dark) foreshadowing special display in injection context

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AIP-01 | Chapter planning prompt injects existing active foreshadowing context (pending plant + pending payoff), letting LLM understand current foreshadowing state | Use `ForeshadowingService.getForeshadowingList()` with `ForeshadowingQueryDto` filtering by projectId, status=pending/in_progress, and plantedVolume/plannedCallbackVolume=current volume. Format results via a new `buildActiveForeshadowingContext()` method. Inject as a template variable into `llm_outline_chapter_generate`. |
| AIP-02 | Chapter planning LLM output adds `<fs>` (foreshadowing plant) and `<fp>` (foreshadowing payoff) XML tags with title, description, type, layout line sub-tags | Define `<fs>` sub-tags: `<ft>`/`<fy>`/`<fl>`/`<fd>`/`<fc>`/`<fr>`. Define `<fp>` sub-tags: `<ft>`/`<fd>`. Add output instruction block to template. Reference CONTEXT.md D-03 for exact tag names. |
| AIP-03 | DOM parser extension to process `<fs>`/`<fp>` tags, parse foreshadowing data, and batch save to novel_foreshadowing table | Extend `parseChaptersXml()` with regex for `<fs>` and `<fp>` extraction within each `<o>` block. Use existing `extractXmlFieldCData()` pattern. Create `ForeshadowingService.batchCreateForeshadowing()` or loop `createForeshadowing()`. Follow delete-then-create pattern per D-06. |
</phase_requirements>

## Standard Stack

### Core (All Existing -- No New Libraries)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.0 | Backend framework | Project standard |
| MyBatis-Plus | 3.5.5 | ORM for foreshadowing CRUD | Already used in ForeshadowingMapper |
| PromptTemplateService | existing | Template engine with variable injection | Already used by buildChapterPromptUsingTemplate |
| java.util.regex | JDK | XML tag parsing (regex DOM pattern) | Established pattern in parseChaptersXml |
| ForeshadowingService | existing | Foreshadowing CRUD + validation | Already supports query by volume/status, create with validation |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| ForeshadowingQueryDto | existing | Query filtering by volume, status | To query active foreshadowing for current volume |
| ForeshadowingCreateDto | existing | Create DTO with validation | To create parsed foreshadowing records |
| BeanUtils.copyProperties | Spring | Entity-DTO conversion | Already used in ForeshadowingService |

### No New Dependencies Required

This phase is purely an extension of existing code. No new libraries or packages are needed.

## Architecture Patterns

### Recommended Code Changes Structure

```
OutlineTaskStrategy.java (modify)
  +-- buildActiveForeshadowingContext(projectId, volumeNumber) [NEW]
  +-- buildChapterPromptUsingTemplate() [MODIFY: add foreshadowingContext variable]
  +-- parseChaptersXml() [MODIFY: extract <fs>/<fp> per chapter]
  +-- parseAndSaveForeshadowingFromChapters() [NEW]
  +-- saveVolumeChaptersToDatabase() [MODIFY: call foreshadowing persistence]
  +-- saveChaptersToDatabase() [MODIFY: call foreshadowing persistence]
  +-- deletePendingForeshadowingForVolume() [NEW or in ForeshadowingService]
  +-- generateChaptersInVolume() [MODIFY: call foreshadowing context injection]
  +-- generateChaptersForVolume() [MODIFY: call foreshadowing context injection]

ForeshadowingService.java (modify)
  +-- deletePendingForeshadowingForVolume(projectId, volumeNumber) [NEW]

llm_outline_chapter_generate template (DB, modify)
  +-- Add {{foreshadowingContext}} injection block
  +-- Add <fs>/<fp> output format instructions
```

### Pattern 1: Foreshadowing Context Injection
**What:** Build a structured text representation of active foreshadowing for the current volume, inject as template variable.
**When to use:** Both `generateChaptersInVolume` and `generateChaptersForVolume` before calling `buildChapterPromptUsingTemplate`.
**Example:**
```java
// In OutlineTaskStrategy -- NEW method
private String buildActiveForeshadowingContext(Long projectId, int volumeNumber) {
    // Query active foreshadowing for this volume (D-02)
    ForeshadowingQueryDto query = new ForeshadowingQueryDto();
    query.setProjectId(projectId);

    // Get foreshadowing planted in this volume
    query.setPlantedVolume(volumeNumber);
    query.setStatus("pending");
    List<ForeshadowingDto> plantedHere = foreshadowingService.getForeshadowingList(query);

    // Get foreshadowing planned to be paid off in this volume
    query.setPlantedVolume(null);
    query.setPlannedCallbackVolume(volumeNumber);
    List<ForeshadowingDto> callbackHere = foreshadowingService.getForeshadowingList(query);

    // Format as structured list (D-01)
    StringBuilder sb = new StringBuilder();
    // ... format each foreshadowing with title, type, layout, status, locations
    return sb.toString();
}
```

### Pattern 2: Regex Parsing of `<fs>`/`<fp>` Tags
**What:** Extend `parseChaptersXml()` to also extract `<fs>` and `<fp>` blocks from within each `<o>` chapter block.
**When to use:** During XML parsing after LLM response.
**Example:**
```java
// Inside the chapterMatcher.find() loop in parseChaptersXml()
// Extract <fs> tags (zero or more per chapter)
List<Map<String, String>> foreshadowingPlants = extractForeshadowingPlants(chapterContent);
chapterData.put("_foreshadowingPlants", foreshadowingPlants); // transient, not a DB field

// Extract <fp> tags (zero or more per chapter)
List<Map<String, String>> foreshadowingPayoffs = extractForeshadowingPayoffs(chapterContent);
chapterData.put("_foreshadowingPayoffs", foreshadowingPayoffs);

// New helper method
private List<Map<String, String>> extractForeshadowingPlants(String content) {
    List<Map<String, String>> plants = new ArrayList<>();
    Pattern fsPattern = Pattern.compile("<fs>\\s*([\\s\\S]*?)\\s*</fs>", Pattern.DOTALL);
    Matcher fsMatcher = fsPattern.matcher(content);
    while (fsMatcher.find()) {
        String fsContent = fsMatcher.group(1);
        Map<String, String> data = new HashMap<>();
        extractXmlFieldCData(fsContent, "ft", data);  // title
        extractXmlField(fsContent, "fy", data);        // type
        extractXmlField(fsContent, "fl", data);         // layout line
        extractXmlFieldCData(fsContent, "fd", data);   // description
        extractXmlField(fsContent, "fc", data);         // callback volume
        extractXmlField(fsContent, "fr", data);         // callback chapter
        if (data.containsKey("ft")) plants.add(data);
    }
    return plants;
}
```

### Pattern 3: Foreshadowing Persistence After Chapter Save
**What:** After saving chapters to the database, iterate parsed foreshadowing data and create `novel_foreshadowing` records.
**When to use:** At the end of both `saveVolumeChaptersToDatabase()` and `saveChaptersToDatabase()`.
**Example:**
```java
// In saveChaptersToDatabase -- after chapter insert loop
deletePendingForeshadowingForVolume(projectId, volumeNumber); // D-06
for (Map<String, String> chapterData : chaptersList) {
    @SuppressWarnings("unchecked")
    List<Map<String, String>> plants = (List<Map<String, String>>) chapterData.get("_foreshadowingPlants");
    if (plants != null) {
        for (Map<String, String> plant : plants) {
            ForeshadowingCreateDto dto = new ForeshadowingCreateDto();
            dto.setTitle(plant.get("ft"));
            dto.setType(plant.get("fy"));
            dto.setLayoutType(plant.get("fl"));
            dto.setDescription(plant.get("fd"));
            dto.setPlantedChapter(Integer.parseInt(chapterData.get("chapterNumber")));
            dto.setPlantedVolume(volumeNumber);
            if (plant.get("fc") != null)
                dto.setPlannedCallbackVolume(Integer.parseInt(plant.get("fc")));
            if (plant.get("fr") != null)
                dto.setPlannedCallbackChapter(Integer.parseInt(plant.get("fr")));
            foreshadowingService.createForeshadowing(projectId, dto);
        }
    }
}
```

### Anti-Patterns to Avoid

- **Name-matching `<fp>` to existing foreshadowing:** D-05 explicitly forbids this. `<fp>` output should only be used as descriptive context for LLM, not to auto-link records. The `<fp>` tag is informational only for this phase -- actual status updates to "completed" will be handled in later phases.
- **Injecting ALL project foreshadowing:** D-02 limits scope to the current volume only. Injecting all foreshadowing wastes tokens and dilutes LLM focus.
- **Parsing `<fs>`/`<fp>` outside `<o>` blocks:** These tags only exist within chapter blocks. The regex should search within each `chapterContent` string, not the full XML.
- **Using a real DOM parser:** The codebase explicitly uses regex parsing (not Jackson XML or javax.xml). This is a known limitation documented in CLAUDE.md. Stick with regex.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Foreshadowing CRUD | Custom SQL inserts | `ForeshadowingService.createForeshadowing()` | Includes distance validation (D-06/D-07), callback bounds validation, and BeanUtils mapping |
| Foreshadowing query by volume/status | Custom query wrapper | `ForeshadowingService.getForeshadowingList(ForeshadowingQueryDto)` | Already supports plantedVolume, plannedCallbackVolume, status filtering with proper indexing |
| Template variable injection | String replacement | `PromptTemplateService.executeTemplate(code, variables)` | Handles variable validation, versioning, caching |
| XML field extraction | Custom XML parsing | `extractXmlField()` / `extractXmlFieldCData()` | Handles CDATA and plain text, established pattern |

**Key insight:** The `ForeshadowingService` already handles all the validation and CRUD that this phase needs. The main work is connecting the pipeline pieces together, not building new data access logic.

## Common Pitfalls

### Pitfall 1: `<fs>`/`<fp>` Tag Name Collisions
**What goes wrong:** The tag `<f>` is already used for `chapterEndingScene` in `parseChaptersXml()`. If `<fs>`/`<fp>` sub-tags collide with existing tags, parsing breaks.
**Why it happens:** The codebase uses single-letter tags for compactness. `<f>` = endingScene, `<s>` = startingScene.
**How to avoid:** The CONTEXT.md explicitly chose `<fs>` and `<fp>` (two letters) as parent tags, with sub-tags `<ft>`/`<fy>`/`<fl>`/`<fd>`/`<fc>`/`<fr>` (two letters). None of these collide with existing single-letter tags (`n`, `v`, `t`, `p`, `e`, `g`, `w`, `s`, `f`). Verify regex patterns match the two-letter tags exactly.
**Warning signs:** Parser returns empty foreshadowing lists, or chapter fields get corrupted.

### Pitfall 2: Forgetting to Handle `<fp>` as Informational-Only
**What goes wrong:** Trying to auto-update existing foreshadowing status to "completed" when `<fp>` appears in LLM output.
**Why it happens:** The natural assumption is that `<fp>` should trigger a status update. But D-05 says no name matching, and the `<fp>` title may not exactly match existing records (LLM may paraphrase).
**How to avoid:** In this phase, `<fp>` tags are parsed and stored but do NOT trigger status updates. They serve as hints for Phase 17 (generation constraints). The actual foreshadowing lifecycle (pending -> in_progress -> completed) is managed manually or in later phases.
**Warning signs:** Code that does `foreshadowing.setStatus("completed")` based on `<fp>` parsing.

### Pitfall 3: Two Planning Paths Diverging
**What goes wrong:** Only one of the two planning paths (`generateChaptersInVolume` vs `generateChaptersForVolume`) gets foreshadowing support, leading to inconsistent behavior.
**Why it happens:** The two methods have slightly different signatures and call chains. `generateChaptersInVolume` calls `buildChapterPromptUsingTemplate()` which uses the template system. `generateChaptersForVolume` calls the hardcoded `buildChapterPrompt()` method.
**How to avoid:** Per D-07, both must support foreshadowing. Extract a shared `buildActiveForeshadowingContext()` method and call it from both paths. For `generateChaptersForVolume`, inject the foreshadowing context into the hardcoded prompt builder OR refactor it to also use the template system (the template path already has a fallback to the hardcoded method).
**Warning signs:** Foreshadowing context appears in batch outline but not in single-volume replan, or vice versa.

### Pitfall 4: Foreshadowing Delete Scope Too Broad
**What goes wrong:** `deletePendingForeshadowingForVolume` deletes foreshadowing that was planted in other volumes but planned to be paid off in the current volume.
**Why it happens:** D-06 says "delete pending foreshadowing where plantedVolume matches current volume". If the query accidentally also matches `plannedCallbackVolume`, it deletes cross-volume foreshadowing that should be preserved.
**How to avoid:** The delete query must filter ONLY by `plantedVolume = currentVolume` AND `status = pending`. Do NOT include `plannedCallbackVolume` in the delete condition.
**Warning signs:** Cross-volume foreshadowing disappears after re-planning one volume.

### Pitfall 5: Template Variable Not Defined in Template
**What goes wrong:** Adding `variables.put("foreshadowingContext", ...)` but the `llm_outline_chapter_generate` template does not have a `{{foreshadowingContext}}` placeholder, causing the variable to be silently ignored.
**Why it happens:** `PromptTemplateServiceImpl` validates required variables but silently ignores extra variables not in the template.
**How to avoid:** The plan must include a step to update the template content in the database. The template code is `llm_outline_chapter_generate`. The updated template must include both the injection block `{{foreshadowingContext}}` and the `<fs>`/`<fp>` output format instructions. This requires a DB migration or manual template update step.
**Warning signs:** LLM output contains no `<fs>`/`<fp>` tags because the prompt never asked for them.

### Pitfall 6: Null Safety in Parsed Foreshadowing Fields
**What goes wrong:** `Integer.parseInt(plant.get("fc"))` throws NumberFormatException when the LLM omits `<fc>` or `<fr>` tags.
**Why it happens:** LLM output is not guaranteed to include all sub-tags. `<fc>` (callback volume) and `<fr>` (callback chapter) are particularly likely to be omitted for long-term foreshadowing where the LLM doesn't yet know the exact payoff location.
**How to avoid:** Wrap all `Integer.parseInt()` calls in try-catch or null-checks. Use helper methods like `parseIntSafe(value, defaultValue)`. `ForeshadowingCreateDto` allows null for `plannedCallbackChapter` and `plannedCallbackVolume`.
**Warning signs:** Foreshadowing parsing fails silently, or the entire chapter parsing aborts due to one bad foreshadowing entry.

## Code Examples

### Foreshadowing Context Text Format (Claude's Discretion Recommendation)

The format should be concise but complete. Recommended structure for LLM consumption:

```
【当前卷活跃伏笔】

待埋设伏笔（需在本卷各章节中埋设）：
1. 神秘钥匙 | 类型: item | 布局线: bright1 | 状态: pending | 计划回收: 第1卷第8章
2. 老人预言 | 类型: secret | 布局线: dark | 状态: pending | 计划回收: 第2卷第5章

待回收伏笔（前文已埋设，需在本卷回收）：
1. 主角身世 | 类型: secret | 布局线: bright1 | 状态: in_progress | 埋设: 第1卷第3章

注：暗线(dark)伏笔为全书级悬念，可跨卷埋设线索，不急于回收。
```

### `<fs>` Output Format in Template

```xml
<o>
  <n>1</n>
  <v>1</v>
  <t><![CDATA[章节标题]]></t>
  <p><![CDATA[情节大纲]]></p>
  <e><![CDATA[关键事件]]></e>
  <g><![CDATA[本章目标]]></g>
  <w>3000</w>
  <s><![CDATA[起点场景]]></s>
  <f><![CDATA[终点场景]]></f>
  <fs>
    <ft>神秘钥匙</ft>
    <fy>item</fy>
    <fl>bright1</fl>
    <fd>主角在废弃矿洞中发现一把古钥匙</fd>
    <fc>1</fc>
    <fr>8</fr>
  </fs>
  <fp>
    <ft>神秘老人预言</ft>
    <fd>本章揭示老人的预言指向主角身世</fd>
  </fp>
</o>
```

### Delete Pending Foreshadowing for Volume

```java
// In ForeshadowingService -- NEW method
public int deletePendingForeshadowingForVolume(Long projectId, int volumeNumber) {
    LambdaQueryWrapper<Foreshadowing> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Foreshadowing::getProjectId, projectId)
           .eq(Foreshadowing::getPlantedVolume, volumeNumber)
           .eq(Foreshadowing::getStatus, "pending");
    int deleted = foreshadowingMapper.delete(wrapper);
    log.info("删除第{}卷pending伏笔 {} 个, projectId={}", volumeNumber, deleted, projectId);
    return deleted;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| buildPendingForeshadowingText (ChapterPlotMemory-based) | Structured query from novel_foreshadowing table | Phase 15 | Foreshadowing now has proper DB persistence with volume fields |
| Hardcoded prompt in buildChapterPrompt() | Template-based via PromptTemplateService | Phase ~10 | All prompts now manageable via DB, version controlled |
| No foreshadowing in chapter planning | Foreshadowing injection + output tags | Phase 16 (this phase) | LLM can now plan foreshadowing during chapter generation |

**Deprecated/outdated:**
- `ForeshadowingService.buildPendingForeshadowingText()`: Uses old ChapterPlotMemory approach. This phase replaces it with a new structured context builder using the novel_foreshadowing table directly.
- `ForeshadowingService.getPendingForeshadowingFromMemories()`: Legacy method based on plot memory, will not be used by Phase 16.

## Open Questions

1. **Template Update Mechanism**
   - What we know: Templates are stored in DB (`ai_prompt_template` + `ai_prompt_template_version` tables), managed by `PromptTemplateService`.
   - What's unclear: Whether we need a SQL migration to update the template content, or if there is an admin UI for template editing.
   - Recommendation: Create a SQL migration script that updates the active version of `llm_outline_chapter_generate` to include the foreshadowing context block and `<fs>`/`<fp>` output instructions. Alternatively, document the manual template update step.

2. **`<fp>` Data Persistence Strategy**
   - What we know: D-05 says no name matching. `<fp>` tags from LLM output describe payoffs.
   - What's unclear: Should `<fp>` parsed data be stored at all in this phase, or just logged for reference?
   - Recommendation: Store `<fp>` data as notes or as separate records with a `payoff_description` field. Since Foreshadowing entity has a `notes` field, we could create records from `<fp>` with type="payoff_plan" (but the entity does not have such a type). More practical: parse `<fp>` and log them, but only persist `<fs>` records. The `<fp>` information will be consumed in Phase 17 (generation constraints) by querying foreshadowing records whose `plannedCallbackVolume`/`plannedCallbackChapter` match the current chapter.

3. **`generateChaptersForVolume` Uses Hardcoded Prompt**
   - What we know: This method calls `buildChapterPrompt()` (hardcoded), not `buildChapterPromptUsingTemplate()` (template-based).
   - What's unclear: Whether to refactor this method to use the template system or inject foreshadowing into the hardcoded prompt.
   - Recommendation: Since the hardcoded method has a fallback relationship with the template method, inject the foreshadowing context variable directly into the hardcoded `buildChapterPrompt()` method's StringBuilder output. This avoids a larger refactor and keeps changes scoped.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | Backend compilation | Likely | -- | -- |
| Maven 3.8+ | Build | Likely | -- | -- |
| MySQL 8.0+ | DB for template & foreshadowing | Likely | -- | -- |
| LLM Provider | Chapter planning API calls | External | -- | -- |

**Note:** This phase modifies existing backend code only (no frontend changes, no new infrastructure). All dependencies are existing project dependencies. No missing dependencies anticipated.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (existing) |
| Config file | None -- tests in `src/test/java/` |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=OutlineTaskStrategyTest -DfailIfNoTests=false` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AIP-01 | Active foreshadowing context built correctly for volume | unit | `mvn test -Dtest=OutlineTaskStrategyTest#testBuildActiveForeshadowingContext` | Wave 0 |
| AIP-02 | `<fs>`/`<fp>` tags parsed from XML with sub-tags | unit | `mvn test -Dtest=OutlineTaskStrategyTest#testParseForeshadowingTags` | Wave 0 |
| AIP-03 | Parsed foreshadowing persisted with correct fields | unit | `mvn test -Dtest=OutlineTaskStrategyTest#testSaveForeshadowingFromChapters` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=OutlineTaskStrategyTest`
- **Per wave merge:** `cd ai-factory-backend && mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `OutlineTaskStrategyTest.java` -- tests for foreshadowing context building, XML parsing of `<fs>`/`<fp>`, and persistence flow
- [ ] Existing test infrastructure covers JUnit 5 + Mockito but no `OutlineTaskStrategyTest` exists yet

## Project Constraints (from CLAUDE.md)

- **Tech Stack**: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS -- no new libraries
- **Database**: MySQL 8.0+, tree table pattern with parent_id + deep (not applicable here, but confirms MySQL)
- **AI Integration**: LangChain4j + DOM XML parsing (regex, not real DOM parser) -- Jackson XML cannot handle nested same-name tags
- **Frontend**: Not in scope for this phase (Phase 18/19)
- **GSD Workflow**: Changes must go through GSD workflow

## Sources

### Primary (HIGH confidence)
- Direct source code reading of `OutlineTaskStrategy.java` (2121 lines) -- parseChaptersXml, buildChapterPromptUsingTemplate, saveVolumeChaptersToDatabase, saveChaptersToDatabase, generateChaptersInVolume, generateChaptersForVolume
- Direct source code reading of `ForeshadowingService.java` (447 lines) -- getForeshadowingList, createForeshadowing, validateForeshadowingDistance
- Direct source code reading of `Foreshadowing.java` entity (103 lines) -- all fields confirmed
- Direct source code reading of `ForeshadowingCreateDto.java` (97 lines) -- required/optional fields confirmed
- Direct source code reading of `ForeshadowingQueryDto.java` (68 lines) -- filter capabilities confirmed
- Direct source code reading of `PromptTemplateBuilder.java` (827 lines) -- buildVolumeInfo, variable injection pattern
- Direct source code reading of `PromptContextBuilder.java` (222 lines) -- context building pattern
- SQL schema in `init.sql` (lines 338-362) -- novel_foreshadowing table structure with planted_volume, planned_callback_volume

### Secondary (MEDIUM confidence)
- Phase 15 CONTEXT.md -- confirmed data foundation decisions, field naming, validation rules
- Phase 16 CONTEXT.md -- confirmed all locked decisions and design constraints

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new libraries, all existing code thoroughly read
- Architecture: HIGH -- patterns established by Phase 12 precedent, same regex DOM approach, same template injection
- Pitfalls: HIGH -- identified from direct code analysis, especially tag collision risks and dual-path consistency

**Research date:** 2026-04-11
**Valid until:** 2026-05-11 (stable -- no external dependencies, all code is project-internal)
