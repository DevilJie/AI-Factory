# Phase 17: AI Generation Constraints - Research

**Researched:** 2026-04-11
**Domain:** Backend prompt engineering / foreshadowing lifecycle management
**Confidence:** HIGH

## Summary

This phase injects foreshadowing constraints into the chapter generation prompt so the LLM plants or resolves foreshadowing per the plan, and then automatically updates foreshadowing status after successful generation. The implementation directly mirrors the Phase 13 character constraint pattern already established in `PromptTemplateBuilder`.

Two generation codepaths exist (streaming via `ChapterService.generateChapterByPlan()` and synchronous via `ChapterContentGenerateTaskStrategy.generateContent()`), both calling `PromptTemplateBuilder.buildChapterPrompt()`. The constraint injection must happen inside `buildTemplateVariables()` in `PromptTemplateBuilder`, which is the single shared entry point.

For status updates, both codepaths have post-generation blocks (after `chapterMapper.updateById`) where the foreshadowing status batch update should be inserted. The `ForeshadowingMapper` already supports `BaseMapper` operations, and `ForeshadowingService` has the `ForeshadowingMapper` autowired, so adding a batch status update method is straightforward.

**Primary recommendation:** Add `hasForeshadowingConstraints()` and `buildForeshadowingConstraintText()` methods to `PromptTemplateBuilder` (mirroring `hasPlannedCharacters`/`buildPlannedCharacterInfoText`), inject the result as a new `foreshadowingConstraint` template variable, and add a `batchUpdateForeshadowingStatus()` method to `ForeshadowingService` called from both generation paths after content is saved.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Constraint paragraphs use directive language style -- "本章节必须埋设的伏笔" and "本章节必须回收的伏笔" as section headers, with "请务必自然地融入情节" instructions. Consistent with Phase 13 character constraint "必须严格遵循" style.
- **D-02:** Each foreshadowing constraint item contains title + description only, not type or layout line. Minimizes token usage; generation stage does not need planning stage's full metadata.
- **D-03:** Constraints inject only current-chapter-relevant foreshadowing -- `plantedChapter == currentGlobalChapter` with `status == pending` for "must plant", `plannedCallbackChapter == currentGlobalChapter` with `status == in_progress` for "must resolve".
- **D-04:** After successful chapter generation, auto-update foreshadowing status: pending -> in_progress for planted, in_progress -> completed for resolved.
- **D-05:** Chapter re-generation does NOT roll back foreshadowing status. Users regenerate for content changes, not foreshadowing changes.
- **D-06:** Mirror the Phase 13 character constraint injection pattern -- add foreshadowing constraint build methods in `PromptTemplateBuilder` (like `buildPlannedCharacterInfoText`), inject into `buildChapterPrompt()` variables.
- **D-07:** Also update the `llm_chapter_generate_standard` template to add foreshadowing constraint variable placeholder.

### Claude's Discretion
- Exact wording and layout of constraint text
- Whether to show empty prompt or omit entirely when no foreshadowing constraints exist
- Timing of status updates (immediately after generation vs during generation)
- Batch status update optimization strategy

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AIC-01 | Inject foreshadowing constraints into chapter generation prompt -- "本章节需埋设的伏笔" and "本章节需回收的伏笔" as mandatory creative directives | D-01/D-06 pattern mirrors Phase 13; inject via `buildTemplateVariables()` in `PromptTemplateBuilder`; query `ForeshadowingService` by `plantedChapter`/`plannedCallbackChapter` matching current chapter |
| AIC-02 | Constraint text uses concise format, only current-chapter items injected, avoiding context window overflow | D-02 limits each item to title + description; D-03 queries only exact-chapter matches; estimated ~50-100 tokens per constraint even with 5 items |
</phase_requirements>

## Architecture Patterns

### Existing Pattern to Mirror: Character Constraint Injection (Phase 13)

The established pattern in `PromptTemplateBuilder` for injecting chapter-specific constraints:

1. **Detection method**: `hasPlannedCharacters(NovelChapterPlan)` -- checks if constraint data exists
2. **Build method**: `buildPlannedCharacterInfoText(String)` -- formats the constraint text
3. **Injection point**: Inside `buildTemplateVariables()`, result placed into `variables.put("characterInfo", ...)`
4. **Template variable**: Database template references `{characterInfo}` placeholder via Hutool `StrUtil.format()`

This exact pattern should be replicated for foreshadowing constraints with `hasForeshadowingConstraints()` and `buildForeshadowingConstraintText()`.

### Two Generation Codepaths (BOTH must be updated)

**Codepath 1: Streaming** -- `ChapterService.generateChapterByPlan()`
- Called from frontend SSE endpoint
- Builds prompt via `buildPromptFromTemplate()` -> `promptTemplateBuilder.buildChapterPrompt()`
- Post-generation: saves chapter, generates plot summary, saves plot memory
- Status update insertion point: after `chapterMapper.updateById(chapter)` at line ~929

**Codepath 2: Synchronous** -- `ChapterContentGenerateTaskStrategy.generateContent()`
- Called from task-based generation
- Also calls `promptTemplateBuilder.buildChapterPrompt()`
- Post-generation: saves chapter, rebuilds memory, extracts characters
- Status update insertion point: after `chapterMapper.updateById(chapter)` at line ~252

### Template Variable System

Template content stored in `ai_prompt_template_version.template_content`. Variables use Hutool `StrUtil.format()` with `{variableName}` syntax (not `${}` or `#{}`). The template code is `llm_chapter_generate_standard`. A new variable `{foreshadowingConstraint}` must be added to the template content in the database.

### Foreshadowing Query Gap

`ForeshadowingQueryDto` currently supports filtering by `plantedVolume`, `plannedCallbackVolume`, `status`, `projectId`, but does NOT have `plantedChapter` or `plannedCallbackChapter` fields. The `getForeshadowingList()` method also lacks these filters.

**Two options:**
1. Add `plantedChapter`/`plannedCallbackChapter` fields to `ForeshadowingQueryDto` and add corresponding `eq()` conditions in `getForeshadowingList()` (consistent with existing pattern)
2. Add a dedicated query method in `ForeshadowingService` that uses `LambdaQueryWrapper` directly (simpler, no DTO change needed)

**Recommendation:** Option 1 (add to DTO) for consistency. The `getForeshadowingList()` method is already the standard query pattern, and adding two more `eq()` conditions is trivial.

### Status Update Pattern

`ForeshadowingMapper` extends `BaseMapper<Foreshadowing>`, so batch updates can use MyBatis-Plus `LambdaUpdateWrapper`:

```java
// pending -> in_progress for foreshadowing planted this chapter
LambdaUpdateWrapper<Foreshadowing> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(Foreshadowing::getProjectId, projectId)
       .eq(Foreshadowing::getPlantedChapter, chapterNumber)
       .eq(Foreshadowing::getStatus, "pending")
       .set(Foreshadowing::getStatus, "in_progress")
       .set(Foreshadowing::getUpdateTime, LocalDateTime.now());
foreshadowingMapper.update(null, wrapper);
```

## Code Examples

### Example: Character Constraint Pattern to Mirror (PromptTemplateBuilder.java lines 656-721)

```java
// Detection method
private boolean hasPlannedCharacters(NovelChapterPlan chapterPlan) {
    if (chapterPlan == null) return false;
    String pc = chapterPlan.getPlannedCharacters();
    if (pc == null || pc.trim().isEmpty()) return false;
    return !pc.trim().equals("[]");
}

// Build method -- directive constraint text
private String buildPlannedCharacterInfoText(String plannedCharactersJson) {
    // ... parse JSON ...
    StringBuilder sb = new StringBuilder();
    sb.append("【角色约束 - 必须严格遵循】\n");
    sb.append("以下是本章必须出场的角色，请严格按照此列表安排角色出场：\n\n");
    // ... format each character ...
    sb.append("请确认你的章节内容包含了上述所有必须出场的角色。\n");
    return sb.toString();
}

// Injection in buildTemplateVariables()
String characterInfo;
if (hasPlannedCharacters(chapterPlan)) {
    characterInfo = buildPlannedCharacterInfoText(chapterPlan.getPlannedCharacters());
} else {
    characterInfo = null; // triggers fallback
}
variables.put("characterInfo", characterInfo);
```

### Example: Active Foreshadowing Context (OutlineTaskStrategy.java lines 1421-1477)

This is the Phase 16 planning-stage foreshadowing context. Generation-stage constraints need a DIFFERENT, more concise format per D-02:

```java
// Phase 16 format (for planning -- too verbose for generation)
sb.append(i + 1).append(". ").append(fs.getTitle());
sb.append(" | 类型: ").append(fs.getType());
sb.append(" | 布局线: ").append(fs.getLayoutType());
sb.append(" | 状态: ").append(fs.getStatus());

// Phase 17 format (for generation -- concise per D-02)
sb.append(i + 1).append(". ").append(fs.getTitle());
sb.append(" -- ").append(fs.getDescription());
```

### Example: Foreshadowing Constraint Text (per D-01, D-02)

```
【本章节必须埋设的伏笔】
以下是本章需要埋设的伏笔，请务必自然地融入情节，不可生硬：
1. 神秘钥匙 -- 主角在废弃矿洞中发现一把古钥匙
2. 暗夜低语 -- 夜间听到奇怪的低语声，来源不明

【本章节必须回收的伏笔】
以下是前文埋设的伏笔，需要在本章自然地揭示或解决：
1. 古老预言 -- 第一章老人说的预言，暗示主角的命运
```

### Example: Batch Status Update Method (ForeshadowingService)

```java
/**
 * 批量更新伏笔状态（章节生成成功后调用）
 * Per D-04: pending -> in_progress for planted, in_progress -> completed for resolved
 * Per D-05: re-generation does NOT roll back status
 */
@Transactional
public int batchUpdateStatusForChapter(Long projectId, int chapterNumber) {
    int updated = 0;

    // pending -> in_progress (planted this chapter)
    LambdaUpdateWrapper<Foreshadowing> plantWrapper = new LambdaUpdateWrapper<>();
    plantWrapper.eq(Foreshadowing::getProjectId, projectId)
                .eq(Foreshadowing::getPlantedChapter, chapterNumber)
                .eq(Foreshadowing::getStatus, "pending")
                .set(Foreshadowing::getStatus, "in_progress")
                .set(Foreshadowing::getUpdateTime, LocalDateTime.now());
    updated += foreshadowingMapper.update(null, plantWrapper);

    // in_progress -> completed (resolved this chapter)
    LambdaUpdateWrapper<Foreshadowing> resolveWrapper = new LambdaUpdateWrapper<>();
    resolveWrapper.eq(Foreshadowing::getProjectId, projectId)
                  .eq(Foreshadowing::getPlannedCallbackChapter, chapterNumber)
                  .eq(Foreshadowing::getStatus, "in_progress")
                  .set(Foreshadowing::getStatus, "completed")
                  .set(Foreshadowing::getActualCallbackChapter, chapterNumber)
                  .set(Foreshadowing::getUpdateTime, LocalDateTime.now());
    updated += foreshadowingMapper.update(null, resolveWrapper);

    log.info("批量更新伏笔状态，projectId={}, chapterNumber={}, updatedCount={}",
             projectId, chapterNumber, updated);
    return updated;
}
```

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Foreshadowing query by chapter | Custom SQL or stream filter | `ForeshadowingMapper` with `LambdaQueryWrapper` (or extend `ForeshadowingQueryDto`) | MyBatis-Plus handles SQL generation, parameterized queries, and null safety |
| Batch status update | Loop + individual `updateById` | `LambdaUpdateWrapper` batch update | Single SQL statement, transactional consistency |
| Template variable substitution | Custom string replacement | Hutool `StrUtil.format()` via `PromptTemplateService.executeTemplate()` | Consistent with existing template system, handles missing variables gracefully |

## Common Pitfalls

### Pitfall 1: Forgetting to Update Both Generation Paths
**What goes wrong:** Only updating `ChapterContentGenerateTaskStrategy` but not `ChapterService.generateChapterByPlan()`, or vice versa. One codepath will lack foreshadowing constraints/status updates.
**Why it happens:** Two separate classes handle generation, easy to overlook one.
**How to avoid:** Both paths funnel through `PromptTemplateBuilder.buildChapterPrompt()`, so constraint injection is centralized. But status updates must be added to BOTH post-generation blocks explicitly. Plan should list both files.
**Warning signs:** Check that `ChapterService.generateChapterByPlan()` AND `ChapterContentGenerateTaskStrategy.generateContent()` both get the status update call.

### Pitfall 2: ForeshadowingQueryDto Missing Chapter-Level Filters
**What goes wrong:** Querying foreshadowing by `plantedChapter` requires a filter that does not exist in `ForeshadowingQueryDto` or `getForeshadowingList()`.
**Why it happens:** The DTO was designed for volume-level filtering, not chapter-level.
**How to avoid:** Add `plantedChapter` and `plannedCallbackChapter` fields to `ForeshadowingQueryDto` and corresponding `eq()` conditions in `getForeshadowingList()`.
**Warning signs:** Compilation error or empty results when querying by chapter.

### Pitfall 3: Template Content Update Requires Database Migration
**What goes wrong:** Adding `{foreshadowingConstraint}` to the template content requires updating the `ai_prompt_template_version` row in the database. Without this, the variable placeholder is never replaced and appears literally in the prompt.
**Why it happens:** Template content lives in MySQL, not in code files.
**How to avoid:** Plan must include a database update step: either SQL migration or API call to update the template version with the new placeholder.
**Warning signs:** Generated prompt contains literal `{foreshadowingConstraint}` text.

### Pitfall 4: Status Update Timing With Streaming Path
**What goes wrong:** In the streaming path (`ChapterService.generateChapterByPlan()`), the status update could be called before content is fully saved if placed in the wrong location.
**Why it happens:** The streaming path has complex async logic with buffer flushing.
**How to avoid:** Place status update AFTER `chapterMapper.updateById(chapter)` at line ~929, in the same try block as plot summary generation. Same pattern as the synchronous path.
**Warning signs:** Foreshadowing status updates but chapter content is empty or partial.

### Pitfall 5: No Foreshadowing for Chapter 1
**What goes wrong:** The first chapter may have no foreshadowing to plant or resolve, resulting in an empty constraint section.
**Why it happens:** Foreshadowing is planned from Phase 16; early chapters may have no entries.
**How to avoid:** Per D-02 (Claude's discretion), omit the constraint section entirely when no foreshadowing exists for the current chapter. Return empty string, and the template placeholder will be replaced with empty text.
**Warning signs:** Empty section headers like "【本章节必须埋设的伏笔】\n\n" with no content.

## Recommended Project Structure

```
ai-factory-backend/src/main/java/com/aifactory/
  service/chapter/prompt/PromptTemplateBuilder.java  -- ADD: hasForeshadowingConstraints(), buildForeshadowingConstraintText(), inject into buildTemplateVariables()
  service/ForeshadowingService.java                  -- ADD: batchUpdateStatusForChapter(), query methods for chapter-level filtering
  dto/ForeshadowingQueryDto.java                     -- ADD: plantedChapter, plannedCallbackChapter fields
  service/ChapterService.java                        -- ADD: foreshadowingService.batchUpdateStatusForChapter() call in generateChapterByPlan()
  service/task/impl/ChapterContentGenerateTaskStrategy.java -- ADD: foreshadowingService.batchUpdateStatusForChapter() call in generateContent()
```

## Key Files to Modify

| File | Change Type | Description |
|------|-------------|-------------|
| `PromptTemplateBuilder.java` | ADD methods | `hasForeshadowingConstraints()`, `buildForeshadowingConstraintText()`, modify `buildTemplateVariables()` |
| `ForeshadowingService.java` | ADD method | `batchUpdateStatusForChapter(Long projectId, int chapterNumber)` |
| `ForeshadowingQueryDto.java` | ADD fields | `plantedChapter`, `plannedCallbackChapter` with getter/setter |
| `ForeshadowingService.getForeshadowingList()` | ADD conditions | Filter by `plantedChapter` and `plannedCallbackChapter` |
| `ChapterService.java` | ADD call | `foreshadowingService.batchUpdateStatusForChapter()` after chapter save (~line 929) |
| `ChapterContentGenerateTaskStrategy.java` | ADD autowire + call | Inject `ForeshadowingService`, call `batchUpdateStatusForChapter()` after chapter save (~line 252) |
| Database: `ai_prompt_template_version` | UPDATE content | Add `{foreshadowingConstraint}` placeholder to `llm_chapter_generate_standard` template |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ChapterPlotMemory text-based foreshadowing | Structured `novel_foreshadowing` table | Phase 15 (v1.0.6) | Can now query by exact chapter, filter by status, batch update |
| Planning-stage foreshadowing only | Generation-stage foreshadowing constraints | This phase | LLM now receives explicit planting/resolving instructions |
| Manual foreshadowing status management | Auto-status-update after generation | This phase | Eliminates manual bookkeeping |

**Deprecated/outdated:**
- `ForeshadowingService.buildPendingForeshadowingText()` -- uses old `List<String>` format from `ChapterPlotMemory`, NOT the structured foreshadowing table. Do NOT use this method for Phase 17.
- `ForeshadowingService.getLatestPendingForeshadowing()` -- same, queries memory table, not foreshadowing table.

## Open Questions

1. **Template content update mechanism**
   - What we know: Templates live in `ai_prompt_template_version` table, editable via API (`PromptTemplateServiceImpl.updateTemplate()`).
   - What's unclear: Whether to use SQL migration script or runtime API call to add the `{foreshadowingConstraint}` placeholder.
   - Recommendation: Use a SQL migration script (consistent with other DDL changes in this project). The template code is `llm_chapter_generate_standard`. Need to query the current active version's `template_content`, append the placeholder, and create a new version.

2. **Global chapter number resolution**
   - What we know: Phase 15 D-04 decided chapter numbers are global sequences. `chapterPlan.getChapterNumber()` returns the global number.
   - What's unclear: Whether `plantedChapter` and `plannedCallbackChapter` in `novel_foreshadowing` store global or volume-local numbers.
   - Recommendation: Per Phase 15 D-04, they store global numbers. Direct equality comparison `plantedChapter == chapterPlan.getChapterNumber()` is correct.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | none (annotation-based via `@ExtendWith(MockitoExtension.class)`) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=PromptTemplateBuilderTest -DfailIfNoTests=false` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AIC-01 | Foreshadowing constraint text injected into prompt when foreshadowing exists for current chapter | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testForeshadowingConstraintInjection` | Wave 0 |
| AIC-01 | Constraint text uses directive language ("必须埋设"/"必须回收") | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintLanguageStyle` | Wave 0 |
| AIC-01 | No constraint text when no foreshadowing for current chapter | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNoForeshadowingConstraint` | Wave 0 |
| AIC-02 | Constraint text contains only title + description (no type/layout) | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintItemFormat` | Wave 0 |
| AIC-02 | Only current chapter foreshadowing injected (not all project foreshadowing) | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testOnlyCurrentChapterForeshadowing` | Wave 0 |
| D-04 | Status batch update: pending -> in_progress for planted foreshadowing | unit | `mvn test -Dtest=ForeshadowingServiceTest#testBatchUpdatePlantedStatus` | Wave 0 |
| D-04 | Status batch update: in_progress -> completed for resolved foreshadowing | unit | `mvn test -Dtest=ForeshadowingServiceTest#testBatchUpdateResolvedStatus` | Wave 0 |
| D-05 | Re-generation does not roll back status | unit | `mvn test -Dtest=ForeshadowingServiceTest#testNoRollbackOnRegeneration` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=PromptTemplateBuilderTest -DfailIfNoTests=false`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `PromptTemplateBuilderTest` -- add foreshadowing constraint tests (AIC-01, AIC-02)
- [ ] `ForeshadowingServiceTest` -- add batch status update tests (D-04, D-05)
- [ ] Both test files may need creation if `ForeshadowingServiceTest` does not exist

## Sources

### Primary (HIGH confidence)
- Direct source code reading of `PromptTemplateBuilder.java` -- confirmed character constraint injection pattern, variable injection mechanism, `buildTemplateVariables()` structure
- Direct source code reading of `ChapterContentGenerateTaskStrategy.java` -- confirmed synchronous generation flow, post-generation insertion point
- Direct source code reading of `ChapterService.java` -- confirmed streaming generation flow, `generateChapterByPlan()` structure, post-save insertion point
- Direct source code reading of `ForeshadowingService.java` -- confirmed CRUD methods, existing query patterns, `LambdaUpdateWrapper` usage
- Direct source code reading of `Foreshadowing.java` entity -- confirmed fields including `plantedChapter`, `plannedCallbackChapter`, `status`, `actualCallbackChapter`
- Direct source code reading of `ForeshadowingQueryDto.java` -- confirmed missing chapter-level filter fields
- Direct source code reading of `OutlineTaskStrategy.java` -- confirmed `buildActiveForeshadowingContext()` planning-stage format
- Direct source code reading of `PromptTemplateServiceImpl.java` -- confirmed `StrUtil.format()` variable substitution mechanism, `{variableName}` syntax

### Secondary (MEDIUM confidence)
- Phase 17 CONTEXT.md decisions -- locked by user discussion
- Phase 16 CONTEXT.md decisions -- foreshadowing XML parsing patterns established

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all code is in the existing project, no new libraries needed
- Architecture: HIGH -- directly mirroring established Phase 13 pattern, all insertion points verified in code
- Pitfalls: HIGH -- identified from code analysis, two codepaths confirmed, DTO gap confirmed
- Template update: MEDIUM -- mechanism is clear (SQL update to `ai_prompt_template_version`) but exact template content not visible in codebase (lives in DB)

**Research date:** 2026-04-11
**Valid until:** 2026-05-11 (stable, no fast-moving dependencies)
