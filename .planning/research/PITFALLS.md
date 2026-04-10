# Pitfalls Research

**Domain:** Adding foreshadowing (伏笔) management to existing novel generation system -- cross-volume references, AI constraint injection, frontend integration
**Researched:** 2026-04-10
**Confidence:** HIGH (derived from direct codebase analysis of existing patterns, data model, and v1.0.5 character planning precedent)

---

## Critical Pitfalls

### Pitfall 1: Chapter-Number-Only References Break When Volumes Are Regenerated or Reordered

**What goes wrong:**
The existing `novel_foreshadowing` table uses `plantedChapter` (Integer) and `plannedCallbackChapter` (Integer) to reference where a foreshadowing is planted and when it should be resolved. These are global chapter numbers. The milestone adds `plantedVolume` and `plannedCallbackVolume` fields. But the codebase already has `ChapterGenerationTaskStrategy.saveChaptersToDatabase()` which auto-assigns `chapterNumber` starting from `nextChapterNumber`. If a user regenerates a volume's chapters (which the system supports via per-stage regeneration), the global chapter numbers shift: what was chapter 15 in volume 2 becomes chapter 12 after volume 1 chapters are regenerated with fewer entries. All foreshadowing references with `plantedChapter=15` now point to the wrong chapter.

The new volume fields only partially mitigate this: they tell you WHICH volume a foreshadowing belongs to, but the chapter number within that volume can still shift.

**Why it happens:**
Chapter numbers are relative position identifiers that change when earlier content is regenerated. The system has no stable chapter ID at planning time -- `NovelChapterPlan` rows are only created AFTER AI generation, so foreshadowing planned during outline generation cannot reference a plan ID that does not exist yet.

**How to avoid:**
1. During chapter plan generation (when LLM outputs foreshadowing XML), store the foreshadowing with volume number + relative chapter number within that volume (e.g., "Volume 2, Chapter 3" not "Chapter 23")
2. After chapter plans are saved and have stable IDs, run a post-processing step that resolves `(volumeNumber, relativeChapterNumber)` to an actual `novel_chapter_plan.id`
3. Store BOTH the volume reference AND the chapter plan ID once available. The chapter plan ID is the stable reference
4. When a volume is regenerated, foreshadowing references into that volume must be re-resolved (or the user must be warned that foreshadowing targets changed)
5. The `plantedChapter` (global number) field remains useful for display but should NOT be the primary join key

**Warning signs:**
- Foreshadowing "planted in chapter 15" but chapter 15 content is completely unrelated after a regeneration
- Volume 2 foreshadowing marked as "to be resolved in chapter 30" but chapter 30 is now in volume 3
- `ForeshadowingService.getForeshadowingList(currentChapter=X)` returns foreshadowing meant for a different chapter

**Phase to address:**
Data model phase (first phase). The volume-aware fields must be designed with stable references from the start.

---

### Pitfall 2: Dual Foreshadowing Sources (Structured Table + ChapterPlotMemory Text) Diverge

**What goes wrong:**
The system currently has TWO independent foreshadowing tracking mechanisms:

1. **`novel_foreshadowing` table** -- structured rows with title, type, status, plantedChapter, plannedCallbackChapter (the target of this milestone)
2. **`chapter_plot_memory` table** -- has `foreshadowingPlanted` (JSON array), `foreshadowingResolved` (JSON array), `pendingForeshadowing` (JSON array) as text fields

`ForeshadowingService.getPendingForeshadowingFromMemories()` reads from `chapter_plot_memory` and does set arithmetic (planted minus resolved) to find pending foreshadowing. `ChapterContext.pendingForeshadowing` feeds this text into the prompt via `buildPendingForeshadowingText()`.

After the milestone activates the structured `novel_foreshadowing` table, these two sources will diverge:
- LLM chapter planning creates rows in `novel_foreshadowing` (structured)
- LLM chapter content generation writes to `chapter_plot_memory.foreshadowingPlanted` (text)
- The text in memory may describe the SAME foreshadowing differently than the structured row
- If only one source is queried during chapter generation, foreshadowing context is incomplete
- If both are queried, the AI receives conflicting descriptions of the same foreshadowing

**Why it happens:**
The memory-based system was built as the FIRST foreshadowing mechanism (pre-structured-table). It uses free-text descriptions that the AI writes post-generation. The structured table uses pre-planned titles and descriptions from chapter planning. They serve different lifecycle stages (planning vs. post-generation analysis) but both feed into chapter generation context.

**How to avoid:**
1. Designate the structured `novel_foreshadowing` table as the SINGLE source of truth for foreshadowing status
2. When injecting foreshadowing context into chapter generation prompts, query ONLY from `novel_foreshadowing` (not from `chapter_plot_memory`)
3. Keep `chapter_plot_memory.foreshadowingPlanted/Resolved` for the AI memory summarization feature but do NOT use them for foreshadowing constraint injection
4. After each chapter is generated and memory is rebuilt, cross-check: update the structured table's `status` field based on whether the memory records the foreshadowing as resolved
5. Alternatively, after chapter generation, use the memory's foreshadowing data to update the structured table (sync from memory to structured)

**Warning signs:**
- AI receives foreshadowing instructions that conflict with what actually happened in previous chapters
- Structured table shows a foreshadowing as "pending" but chapter memory shows it was already resolved
- Foreshadowing count in the management UI differs from what the AI believes is pending

**Phase to address:**
Chapter generation constraint injection phase. The constraint injection must exclusively use the structured table.

---

### Pitfall 3: Removing foreshadowingSetup/foreshadowingPayoff Fields Breaks Existing Data

**What goes wrong:**
The milestone calls for removing `foreshadowingSetup` and `foreshadowingPayoff` from `novel_chapter_plan`. These fields currently store free-text foreshadowing data in existing projects. The fields are referenced in:

- `NovelChapterPlan.java` entity (lines 97-103)
- `ChapterPlanUpdateRequest.java` DTO (lines 45-48)
- `ChapterPlanDto.java` DTO (lines 58-61)
- `ChapterPlanDrawer.vue` frontend form (lines 30-31, 250-251, 279, 484-516)
- `project.ts` TypeScript types (lines 132-133, 157-158)

If the database columns are dropped without migrating data, any existing project with foreshadowing data in those fields loses it permanently. The structured `novel_foreshadowing` table will be empty for these projects because the migration from text fields to structured rows has not occurred.

**Why it happens:**
The text fields and the structured table serve different lifecycle stages. The text fields were the ORIGINAL mechanism (manual/AI text in chapter plans). The structured table is the NEW mechanism. There is no automatic migration path from "semicolon-separated text in foreshadowingSetup" to "rows in novel_foreshadowing".

**How to avoid:**
1. Do NOT drop the `foreshadowingSetup` and `foreshadowingPayoff` database columns immediately
2. Keep the columns in the schema but mark them as deprecated in code comments
3. Remove the Java entity fields and DTO fields, but use `@TableField(exist = false)` if needed for backward compatibility during transition
4. Add a migration endpoint or script that: (a) reads existing `foreshadowingSetup` text, (b) splits by semicolons, (c) creates `novel_foreshadowing` rows for each entry
5. Only drop the columns in a future release after confirming all active projects have migrated
6. The frontend `ChapterPlanDrawer` must continue to display old foreshadowing data (read from memory/API) during the transition period

**Warning signs:**
- Existing projects show empty foreshadowing section after deployment
- Chapter generation no longer injects foreshadowing constraints for old projects
- Database still has text in `foreshadowing_setup` column but the entity no longer maps it

**Phase to address:**
Data model phase (first phase). The removal strategy must be defined before any code changes.

---

### Pitfall 4: New XML Tags for Foreshadowing Break Existing Chapter Plan Parsing

**What goes wrong:**
The chapter plan XML format currently uses tags: `<c>`, `<o>`, `<n>`, `<t>`, `<p>`, `<e>`, `<g>`, `<w>`, `<s>`, `<ed>`, `<ch>`, `<cn>`, `<cd>`, `<ci>`. Adding new tags for foreshadowing (e.g., `<fs>` for foreshadowing setup, `<fr>` for foreshadowing resolution) must be done carefully because:

1. `ChapterGenerationTaskStrategy.parseSingleChapter()` uses a switch statement on tag names. New tags that are not in the switch are silently ignored -- no error, no data. If the new tag is accidentally named the same as an existing tag's content abbreviation, it collides.

2. The `sanitizeXmlForDomParsing()` function has `containerTags` parameter `["o", "ch"]` that fixes tag balance. If foreshadowing tags use container semantics (parent with children), they must be added to this array or the sanitizer will not fix their tag mismatches.

3. The LLM prompt explicitly lists the tag order: `n -> t -> p -> e -> g -> w -> s -> ed`. Adding new tags changes this order. The LLM must be told the correct position, and the parser must handle tags appearing in any order (current parser iterates `getChildNodes()` and uses a switch, so order-independent -- but the prompt example suggests a fixed order that the LLM tries to follow).

4. The `parseSingleChapter` method accumulates data into a `Map<String, String>`. Foreshadowing may need structured data (multiple foreshadowing items per chapter), not a single string. The character planning solved this with `<ch>` sub-tags. Foreshadowing needs a similar nested approach.

**Why it happens:**
The XML format is a contract between the LLM prompt and the DOM parser. Changing one side without updating the other causes silent data loss. The LLM may produce the new tags but the parser ignores them, or the parser expects new tags but the LLM prompt was not updated.

**How to avoid:**
1. Design the foreshadowing XML tags following the character planning precedent: use container tags like `<fs>` (foreshadowing setup) with child tags for structured data
2. Add new tags to the `sanitizeXmlForDomParsing` container tags array
3. Update BOTH the prompt template AND the parser in the SAME phase -- never update one without the other
4. Write the parser to handle the new tags before writing the prompt -- this way the parser is ready when the LLM output arrives
5. Keep the prompt's tag order list updated
6. Test with real LLM output -- generate a few chapter plans with the new tags and verify parsing extracts all data

**Warning signs:**
- Chapter plans are created but have no foreshadowing data in the structured table
- XML parsing logs show "unknown tag" warnings (currently there are no such warnings -- unknown tags are just skipped silently)
- The LLM output contains `<fs>` tags but the parser's switch statement falls through to the default case

**Phase to address:**
Chapter plan LLM output phase (XML parsing). Parser must be updated BEFORE prompt template changes.

---

### Pitfall 5: Cross-Volume Foreshadowing Query Performance Degrades with Project Scale

**What goes wrong:**
The milestone adds `plantedVolume` and `plannedCallbackVolume` to support cross-volume foreshadowing. When generating chapter content for chapter N in volume M, the system must query:

1. All foreshadowing planted in volumes 1..M (or earlier in volume M) that are still pending
2. All foreshadowing whose `plannedCallbackVolume` == M and `plannedCallbackChapter` == N (due for resolution in this chapter)
3. The status of each foreshadowing (has it been resolved in a previous chapter?)

The current `ForeshadowingService.getForeshadowingList()` uses a single `LambdaQueryWrapper` with an `eq` on `projectId` and optional filters. The `currentChapter` filter uses a complex OR condition. With cross-volume queries, the conditions become:

```sql
WHERE project_id = ?
AND (
  planned_callback_volume < ?
  OR (planned_callback_volume = ? AND planned_callback_chapter <= ?)
)
AND status IN ('pending', 'in_progress')
```

This query runs for every chapter generation. With a 10-volume, 300-chapter novel with 100+ foreshadowing entries, this becomes a significant query without proper indexes.

**Why it happens:**
The current table has only `idx_project_id` as an index. No composite index exists for `(project_id, status)` or `(project_id, planned_callback_volume, planned_callback_chapter)`. The query optimizer falls back to full table scan filtered by project_id, then applies the complex WHERE conditions in memory.

**How to avoid:**
1. Add composite indexes during the schema migration:
   - `idx_project_status` on `(project_id, status)` -- for "all pending foreshadowing in project X"
   - `idx_callback_target` on `(project_id, planned_callback_volume, planned_callback_chapter)` -- for "foreshadowing due in this chapter"
   - `idx_planted_source` on `(project_id, planted_volume, planted_chapter)` -- for "foreshadowing planted in this volume/chapter"
2. Write the query with proper pagination for large projects (though foreshadowing count is typically < 200)
3. Consider caching the pending foreshadowing list per project, invalidated only when foreshadowing status changes

**Warning signs:**
- Chapter generation takes noticeably longer for volume 5+ compared to volume 1
- Database slow query log shows the foreshadowing query taking > 100ms
- Foreshadowing list API response time increases with project chapter count

**Phase to address:**
Data model phase (first phase). Indexes must be part of the schema migration DDL.

---

### Pitfall 6: Foreshadowing Constraint Injection Overloads the LLM Context Window

**What goes wrong:**
When injecting foreshadowing constraints into chapter generation prompts, the system must include:
1. Foreshadowing to PLANT in this chapter (from chapter plan)
2. Foreshadowing to RESOLVE in this chapter (from chapter plan + structured table)
3. Currently pending foreshadowing that should remain unresolved (context awareness)

The current `PromptTemplateBuilder.buildTemplateVariables()` already injects: worldview (500+ words), volume info (200+ words), recent chapters (500+ words), character info (300+ words with planned characters), basic settings (200+ words). Adding foreshadowing context could add another 200-500 words depending on how many foreshadowing items are active.

If the total prompt exceeds the LLM's effective context window (typically 4K-8K tokens for generation), the model truncates or loses later instructions. Since foreshadowing instructions would be added AFTER character info (last in the variable map), they are the most likely to be truncated.

**Why it happens:**
The prompt template system in `PromptTemplateService.executeTemplate()` does string interpolation without checking total length. There is no token counting or prompt length budget. Each new feature adds more context without considering the cumulative effect.

**How to avoid:**
1. Establish a prompt budget: measure the typical template length, subtract from the LLM's effective context window, and allocate the remaining budget across features
2. For foreshadowing injection, use a concise format:
   ```
   【伏笔约束】
   埋设：1. 主角玉佩的秘密来历
   回收：1. 第一章老者身份揭晓
   注意：以下伏笔仍在进行中，请勿提前回收：龙纹匕首的来历、师门密令
   ```
3. Do NOT inject the FULL list of all pending foreshadowing -- only inject those directly relevant to the current chapter (to plant or to resolve) plus a brief mention of "do not resolve" items
4. Monitor prompt length in development and add a warning log if it exceeds a threshold (e.g., 6000 characters)

**Warning signs:**
- LLM ignores foreshadowing instructions entirely (prompt was too long, instructions truncated)
- LLM resolves foreshadowing that should remain unresolved (context was cut off)
- Generated chapters become shorter or lower quality as prompt grows (model compensating for context pressure)

**Phase to address:**
Chapter generation constraint injection phase. The constraint format must be designed for brevity from the start.

---

### Pitfall 7: ChapterPlanDrawer Foreshadowing Section Overwhelms an Already Complex UI

**What goes wrong:**
The `ChapterPlanDrawer.vue` is already 725 lines with 5 tab sections (basic, plot, scene, foreshadow, character). The existing "foreshadow" tab shows two textareas for `foreshadowingSetup` and `foreshadowingPayoff` (lines 484-516). Replacing these simple textareas with a structured foreshadowing management section (list of foreshadowing items with title, type, status, volume/chapter references, edit/delete buttons) significantly increases complexity.

The character tab already demonstrates this pattern: it went from simple text to an editable list with comparison view, requiring ~200 lines of additional code (editableCharacters, sync logic, comparison matching, role type labels). The foreshadowing section needs similar complexity but with additional dimensions:
- Each foreshadowing item has more fields than a character entry (title, type, layoutType, description, plantedVolume, plantedChapter, plannedCallbackVolume, plannedCallbackChapter, status, priority)
- Cross-volume references require displaying volume context
- Status transitions (pending -> in_progress -> completed) need UI affordances

Adding all this to the existing drawer risks making it unwieldy, slow to render, and confusing for users.

**Why it happens:**
The drawer pattern works well for simple data but does not scale to rich structured data with multiple fields and cross-references. Each new structured data type (characters, now foreshadowing) adds a full CRUD sub-interface inside a tab.

**How to avoid:**
1. Do NOT build a full CRUD interface inside the drawer. Instead, show a READ-ONLY summary of foreshadowing in the drawer tab:
   - List of foreshadowing to plant this chapter (with title and type badge)
   - List of foreshadowing to resolve this chapter (with title and type badge)
   - Link/button to open a dedicated ForeshadowingDrawer for full editing
2. The dedicated ForeshadowingDrawer follows the same Teleport pattern as CharacterDrawer
3. Keep the ChapterPlanDrawer's foreshadow tab under 100 lines
4. Use the same approach as the character comparison view: summary bar + expandable detail
5. CRUD operations (add/edit/delete) live in the project-level foreshadowing management view (sidebar menu), not in the chapter plan drawer

**Warning signs:**
- ChapterPlanDrawer.vue exceeds 900 lines
- The foreshadow tab takes > 2 seconds to render
- Users cannot find the foreshadowing edit controls among all the UI elements
- Foreshadow tab code duplicates CRUD logic that should be in a shared component

**Phase to address:**
Frontend phase. The UI architecture decision (drawer vs. dedicated component) must be made before writing any template code.

---

### Pitfall 8: AI Foreshadowing Plan Output Produces Unrealistic Volume/Chapter References

**What goes wrong:**
When the chapter planning LLM is asked to plan foreshadowing, it must specify where (volume/chapter) each foreshadowing should be planted and resolved. For a novel with 10 volumes and 30+ chapters per volume, the LLM has no accurate model of the future plot. It may:
- Plan a foreshadowing to be resolved in "Volume 3, Chapter 15" but volume 3 does not have 15 chapters in the plan
- Create foreshadowing that spans 5+ volumes but the novel only has 3 volumes planned
- Produce `plannedCallbackChapter` values that are before `plantedChapter` within the same volume (temporal impossibility)

The chapter planning prompt only knows about the CURRENT batch of chapters being generated (typically 5 at a time per `ChapterGenerationTaskStrategy`). It cannot accurately reference chapters that do not exist yet.

**Why it happens:**
The LLM has limited context about the overall chapter structure. During volume 1 planning, it does not know how many chapters volume 3 will have. The foreshadowing plan is aspirational, not precise. The system treats LLM output as precise data, but foreshadowing timing is inherently approximate.

**How to avoid:**
1. Do NOT require the LLM to specify exact chapter numbers for callback. Instead, ask it to specify a RELATIVE position: "resolve in volume N, approximately chapter M" or "resolve approximately 10-15 chapters after planting"
2. Store the LLM's callback estimate as a HINT, not a hard constraint. The actual resolution chapter is determined by the user or by a later planning pass
3. When injecting foreshadowing constraints into chapter generation, use the HINT to determine which foreshadowing are "approximately due" rather than requiring exact chapter matches
4. Consider a two-pass approach: first pass plants foreshadowing, second pass (when later volumes are planned) assigns specific resolution chapters
5. Validate LLM output: if `plannedCallbackChapter < plantedChapter` (within same volume), reject and ask for correction

**Warning signs:**
- Foreshadowing table has entries with `plannedCallbackChapter` values that exceed the volume's actual chapter count
- The "foreshadowing due this chapter" query returns zero results for most chapters because the planned chapter numbers do not match reality
- LLM generates the same callback chapter number for 10+ foreshadowing items (loss of specificity)

**Phase to address:**
Chapter plan LLM output phase. The prompt must be designed to elicit approximate, not exact, resolution targets.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Drop `foreshadowingSetup`/`foreshadowingPayoff` columns immediately | Cleaner schema, no deprecated columns | Existing projects lose foreshadowing data | Never -- must migrate first |
| Reuse `chapter_plot_memory.foreshadowingPlanted` for constraint injection instead of structured table | No new query needed | Dual source of truth diverges over time | Never -- structured table must be sole source |
| Copy-paste editable list pattern from character tab into foreshadow tab | Ship faster, familiar pattern | 200+ lines of duplicated CRUD code in ChapterPlanDrawer | Never -- use read-only summary + dedicated drawer |
| Skip cross-volume foreshadowing in v1, only support within-volume | Simpler query logic, simpler UI | Users with multi-volume novels cannot manage cross-volume arcs | Only if time-constrained, add in v1.1 |
| Use free-text foreshadowing descriptions in XML output instead of structured fields | Simpler XML parsing | Cannot programmatically match planted vs. resolved foreshadowing | Never -- structured fields enable status tracking |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Chapter plan XML parsing | Adding `<fs>` tags but not adding them to `sanitizeXmlForDomParsing` container tags array | Add `"fs"` (or whatever container tag name is chosen) to the `containerTags` parameter so tag balance is fixed automatically |
| Foreshadowing status sync | Only updating `novel_foreshadowing.status` when user manually marks it, not when chapter generation resolves it | After chapter memory is built, check `foreshadowingResolved` against structured table and auto-update status to "completed" |
| ChapterContext.pendingForeshadowing | Leaving the old `ChapterContext.pendingForeshadowing` (List<String>) active while adding new structured injection | Replace `ChapterContext.pendingForeshadowing` population with a query from `novel_foreshadowing` table, keep the field but change the data source |
| Frontend ChapterPlanDrawer save | Saving `foreshadowingSetup`/`foreshadowingPayoff` text fields alongside new structured foreshadowing | Remove text field saving from the update request; only save structured foreshadowing via dedicated API |
| Volume regeneration | Not warning about foreshadowing targets that reference chapters being regenerated | Before volume regeneration, query foreshadowing referencing that volume and warn the user |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Full-table scan for pending foreshadowing per chapter generation | Chapter generation latency increases as project grows | Composite index on `(project_id, status, planned_callback_volume)` | 50+ foreshadowing entries across 5+ volumes |
| Loading ALL foreshadowing for a project when only current-chapter items are needed | API response for "foreshadowing this chapter" is slow for large projects | Query with `WHERE planted_volume <= ? AND status IN ('pending', 'in_progress')` with proper index | 100+ foreshadowing entries |
| Foreshadowing section in ChapterPlanDrawer re-fetches full project list on every tab switch | Frontend feels sluggish when switching to foreshadow tab | Cache foreshadowing list per project, refresh only on explicit action | Noticeable at 30+ items |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Showing all 50+ foreshadowing items in the chapter plan drawer | User cannot find relevant items, cognitive overload | Show ONLY foreshadowing relevant to THIS chapter (to plant + to resolve + summary count of total pending) |
| Requiring manual status updates for foreshadowing resolution | User forgets to mark foreshadowing as completed, system shows stale "pending" status | Auto-detect resolution from chapter content analysis (via memory rebuild), let user confirm/override |
| No visual distinction between "planted this chapter" vs "due for resolution this chapter" vs "background pending" | User cannot quickly understand what action is needed | Use color coding: green = to plant, red = to resolve, gray = background pending |
| Volume number displayed without volume title in foreshadowing references | User does not know which volume "Volume 3" refers to | Display "第3卷 少年游" (volume number + title) wherever volume references appear |
| No foreshadowing timeline or Gantt-like view for cross-volume arcs | User cannot visualize foreshadowing spanning multiple volumes | Start with a simple list view sorted by planted volume/chapter; add timeline view as enhancement |

## "Looks Done But Isn't" Checklist

- [ ] **Volume fields added:** Often `plantedVolume`/`plannedCallbackVolume` are added to entity but NOT to the XML parsing switch statement -- verify new fields are populated from parsed XML
- [ ] **Old fields removed:** Often `foreshadowingSetup`/`foreshadowingPayoff` are removed from entity but still referenced in DTO or frontend types -- verify full removal across all layers
- [ ] **Constraint injection active:** Often the structured table is populated during planning but NOT injected into chapter generation prompts -- verify prompt contains foreshadowing section
- [ ] **Cross-volume query works:** Often foreshadowing with `plannedCallbackVolume > plantedVolume` is not returned by the "pending" query -- verify query handles volume ordering correctly
- [ ] **Status auto-update:** Often foreshadowing status is only updated manually, not automatically after chapter generation -- verify memory rebuild triggers status check
- [ ] **Frontend shows structured data:** Often the drawer still shows old textareas instead of structured foreshadowing list -- verify UI displays rows from `novel_foreshadowing`
- [ ] **Prompt template updated:** Often the template is updated but the old version is cached in Redis -- verify template cache is invalidated after update
- [ ] **Backward compatibility:** Often new foreshadowing injection breaks chapter generation for projects without any structured foreshadowing data -- verify null/empty fallback works

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Dual foreshadowing source divergence | LOW | Switch to single source (structured table). No data loss, just query path change. |
| XML parsing ignores new tags | MEDIUM | Update parser, re-run chapter plan generation for affected volumes. Old chapter plans remain valid without foreshadowing data. |
| Dropped columns with data loss | HIGH | Restore from database backup, add migration script to convert text to structured rows before re-dropping. |
| LLM context window overflow | LOW | Reduce foreshadowing injection to concise format. No data changes needed. |
| Cross-volume query returns wrong results | MEDIUM | Fix query logic, re-index. Foreshadowing data is not corrupt, just query was wrong. |
| ChapterPlanDrawer too complex | MEDIUM | Refactor to read-only summary + dedicated drawer. Functional regression risk during refactor. |
| Unrealistic LLM volume/chapter references | LOW | These are hints, not hard constraints. Add validation, let user override. No data corruption. |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Chapter number instability across volumes | Phase 1: Data model + volume-aware fields | Test: create foreshadowing in volume 1 targeting volume 3, regenerate volume 2, verify reference still resolves |
| Dual foreshadowing source divergence | Phase 2: Constraint injection | Test: chapter generation prompt contains foreshadowing from structured table only, not from memory text |
| Old field removal breaks data | Phase 1: Data model + migration | Test: existing project with text foreshadowing still shows data after migration |
| XML parsing breaks with new tags | Phase 2: Chapter plan XML | Test: generate chapters with new foreshadowing tags, verify all data extracted |
| Cross-volume query performance | Phase 1: Schema migration | Test: EXPLAIN query plan shows index usage, not full table scan |
| LLM context window overflow | Phase 2: Constraint injection | Test: measure prompt length with 20 active foreshadowing items, verify < 6000 chars |
| ChapterPlanDrawer complexity | Phase 3: Frontend | Test: foreshadowing tab renders in < 200ms, drawer < 900 lines total |
| Unrealistic AI foreshadowing targets | Phase 2: Chapter plan prompt | Test: validate LLM output for temporal consistency (callback after planting) |

## Sources

- Direct codebase analysis of `Foreshadowing.java` entity -- field structure showing Integer chapter references without volume context
- Direct codebase analysis of `ForeshadowingService.java` -- `getPendingForeshadowingFromMemories()` showing dual-source foreshadowing tracking via `chapter_plot_memory`
- Direct codebase analysis of `ChapterPlotMemory.java` -- `foreshadowingPlanted`/`foreshadowingResolved`/`pendingForeshadowing` JSON text fields
- Direct codebase analysis of `ChapterGenerationTaskStrategy.java` -- `parseSingleChapter()` switch statement and `sanitizeXmlForDomParsing()` container tags
- Direct codebase analysis of `ChapterPlanDrawer.vue` -- 725-line drawer with 5 tabs, foreshadow tab as simple textareas
- Direct codebase analysis of `PromptTemplateBuilder.java` -- `buildTemplateVariables()` showing cumulative context injection without length budget
- Direct codebase analysis of `ChapterContext.java` -- `pendingForeshadowing` as `List<String>` from memory system
- Direct codebase analysis of `ChapterPlanUpdateRequest.java` and `ChapterPlanDto.java` -- foreshadowingSetup/foreshadowingPayoff fields in DTOs
- v1.0.5 precedent: character planning XML tags (`<ch>`, `<cn>`, `<cd>`, `<ci>`) added to same parser, same pattern for foreshadowing

---
*Pitfalls research for: foreshadowing management integration (v1.0.6)*
*Researched: 2026-04-10*
