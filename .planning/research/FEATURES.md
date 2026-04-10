# Feature Research: Foreshadowing Management System

**Domain:** Foreshadowing (伏笔) lifecycle management for AI novel generation
**Researched:** 2026-04-10
**Confidence:** HIGH (based on codebase analysis of existing foreshadowing infrastructure, competitor research, and the AutoNovel pipeline pattern)

## Executive Summary

The AI Factory codebase already has a `novel_foreshadowing` table with basic CRUD (ForeshadowingService, ForeshadowingController, 6 REST endpoints) and a text-based foreshadowing tracking system in `ChapterPlotMemory` (foreshadowingPlanted/foreshadowingResolved/pendingForeshadowing JSON arrays). However, these two systems are disconnected: the `novel_foreshadowing` table is never referenced during chapter planning or generation, and the chapter plan's `foreshadowingSetup`/`foreshadowingPayoff` text fields are manually edited but never used by AI. The milestone goal is to unify these into a single structured foreshadowing lifecycle that drives both chapter planning (LLM decides which foreshadowing to plant/resolve) and chapter generation (LLM is constrained to execute those decisions).

The key architectural insight from the [AutoNovel project](https://github.com/NousResearch/autonovel/blob/master/gen_outline.py) is that foreshadowing management requires a persistent **ledger** that flows through the entire pipeline: outline planning sets up foreshadowing entries, chapter drafting plants/resolves them, and evaluation verifies balance. The existing `novel_foreshadowing` table is this ledger, but it lacks volume-level granularity (no `plantedVolume`/`plannedCallbackVolume` fields) and is not integrated into the AI prompt chain.

## Current State Analysis

### What Exists

| Component | Current Behavior | Gap |
|-----------|------------------|-----|
| `Foreshadowing` entity | 12 fields: id, projectId, title, type, description, layoutType, plantedChapter, plannedCallbackChapter, actualCallbackChapter, status, priority, notes. | No volume fields. plantedChapter/plannedCallbackChapter use chapter numbers but no volume context. |
| `ForeshadowingService` | CRUD + `markAsCompleted()` + `getForeshadowingStats()` + `getPendingForeshadowingFromMemories()` + `buildPendingForeshadowingText()`. | `getPendingForeshadowingFromMemories()` works against `ChapterPlotMemory` text arrays, NOT against `novel_foreshadowing` table. The two data sources are never reconciled. |
| `ForeshadowingController` | 6 endpoints: list, detail, create, update, delete, markAsCompleted, stats. | No endpoint for querying foreshadowing by volume. No endpoint for batch-creating foreshadowing from AI plan output. |
| `ChapterPlotMemory` | `foreshadowingPlanted`/`foreshadowingResolved`/`pendingForeshadowing` as JSON text arrays. Populated post-generation by AI memory extraction. | Free-text strings, not linked to `novel_foreshadowing` records. No IDs, no structure. Just narrative descriptions. |
| `NovelChapterPlan` | `foreshadowingSetup` and `foreshadowingPayoff` text fields. Editable in ChapterPlanDrawer. | These fields are never injected into the chapter generation prompt. They are orphaned UI-only fields. |
| `ChapterContext` | `pendingForeshadowing` as `List<String>`. Populated by `ForeshadowingService.getLatestPendingForeshadowing()` which reads from ChapterPlotMemory text arrays. | Injected into generation prompt as free-text reminders, but not linked to structured foreshadowing records. No "plant this foreshadowing" instruction. |
| `PromptTemplateBuilder` | `getPlotStageDescription()` mentions "埋设伏笔" and "回收伏笔" in stage descriptions, but these are generic narrative guidance, not specific foreshadowing instructions. | No mechanism to inject specific foreshadowing records into prompts. No "plant these foreshadowing in this chapter" or "resolve these foreshadowing in this chapter" prompt section. |
| `ChapterPlanDrawer.vue` | Has "伏笔管理" tab with two text areas: `foreshadowingSetup` and `foreshadowingPayoff`. Users can type free text. | No connection to `novel_foreshadowing` table. No structured display. No add/edit/delete of actual foreshadowing records. |
| `ProjectSidebar.vue` | 5 nav items: overview, world-setting, settings, creation, characters. | No foreshadowing management menu. No project-level foreshadowing overview page. |

### Key Data Points

- `novel_foreshadowing` table exists with chapter-number references but no volume references.
- `novel_chapter_plan` has `foreshadowing_actions` JSON column in DB (in addition to `foreshadowing_setup`/`foreshadowing_payoff` text columns). The `foreshadowing_actions` column is not mapped in the Java entity.
- The chapter generation prompt already receives `pendingForeshadowing` text via `ChapterContext`/`ChapterService.buildChapterContextText()`, but only as generic "consider resolving these" reminders.
- The chapter plan generation template (`llm_outline_chapter_generate`) has no foreshadowing-related output instructions.
- The `novel_volume_plan` table has `stage_foreshadowings` JSON column -- a volume-level foreshadowing config field that is not used by any service.
- `ProjectBasicSettings` has `foreshadowingSettings` text field for configuring foreshadowing frequency/payoff cycle preferences.

## Table Stakes (Must Have)

Features essential to make foreshadowing a first-class driver of the AI generation pipeline. Missing any means the system still has foreshadowing as a manual afterthought.

| # | Feature | Why Expected | Complexity | Notes |
|---|---------|--------------|------------|-------|
| T-01 | Volume fields on novel_foreshadowing | Cross-volume foreshadowing (plant in volume 1, resolve in volume 3) is fundamental to long-form novels. Without volume context, the chapter numbers are ambiguous when a novel spans multiple volumes. | LOW | Add `planted_volume` and `planned_callback_volume` integer columns. Migration is straightforward. Update entity, DTOs, query logic. |
| T-02 | Remove foreshadowingSetup/foreshadowingPayoff from chapter plan | These text fields are orphaned -- never used by AI, duplicated purpose with the foreshadowing table. Keeping them creates two sources of truth. | LOW | Remove from `NovelChapterPlan` entity, `ChapterPlanUpdateRequest`, `ChapterPlanDrawer.vue` form. DB columns can stay (no DDL risk). The structured foreshadowing table replaces them. |
| T-03 | AI chapter plan includes foreshadowing decisions | When generating chapter plans, the LLM must decide which foreshadowing to plant and which to resolve in each chapter. This is the core integration point. | MEDIUM | Extend `llm_outline_chapter_generate` template to output foreshadowing XML tags. Extend `ChapterGenerationTaskStrategy.parseSingleChapter()` to parse them. Inject existing foreshadowing context (pending foreshadowing from the table) into the planning prompt. Auto-create `novel_foreshadowing` records from AI output. |
| T-04 | Chapter generation injects foreshadowing constraints | When generating chapter content, the LLM must be told specifically which foreshadowing to plant and which to resolve in this chapter, drawn from the foreshadowing table records linked to this chapter plan. | MEDIUM | Replace the current free-text `pendingForeshadowing` injection with structured "plant these" and "resolve these" sections. Query `novel_foreshadowing` where `plantedChapter == currentChapter` (plant) or `plannedCallbackChapter == currentChapter` (resolve). Modify `ChapterContext` or create a new `ForeshadowingContext` to carry structured data. |
| T-05 | ChapterPlanDrawer foreshadowing management UI | Users need to see, edit, add, and delete foreshadowing records linked to a chapter plan. Replace the current text areas with structured cards. | MEDIUM | Replace the two textareas with: (1) "To Plant" section showing foreshadowing records where `plantedChapter == this chapter`, (2) "To Resolve" section showing records where `plannedCallbackChapter == this chapter`. Each card shows title, type, description. Add/delete buttons. Uses existing foreshadowing API. |
| T-06 | Project-level foreshadowing management page | Users need a project-wide view of all foreshadowing, filterable by status, type, volume. This is the "foreshadowing ledger" view. | MEDIUM | New route (`/project/:id/foreshadowing`), new sidebar nav item, new page component. Table/card list view with filters. Reuses existing `ForeshadowingController` list/stats endpoints. |

## Differentiators (Competitive Advantage)

Features that go beyond basic CRUD and make the AI-driven foreshadowing lifecycle truly intelligent.

| # | Feature | Value Proposition | Complexity | Notes |
|---|---------|-------------------|------------|-------|
| D-01 | Auto-enforce minimum plant-to-payoff distance | The [AutoNovel project](https://github.com/NousResearch/autonovel/blob/master/PIPELINE.md) enforces at least 3 chapters between plant and payoff. This prevents "instant gratification" foreshadowing that lacks narrative tension. | LOW | Validation in `ForeshadowingService.createForeshadowing()`: reject if `plannedCallbackChapter - plantedChapter < 3`. Configurable via `ProjectBasicSettings.foreshadowingSettings`. |
| D-02 | Foreshadowing balance score | Show users a "foreshadowing health" metric: ratio of planted to resolved, average plant-to-payoff distance, overdue unresolved count. The [AutoNovel evaluator](https://github.com/NousResearch/autonovel/blob/master/evaluate.py) weights foreshadowing at 9/10. | LOW | Extend `ForeshadowingService.ForeshadowingStats` record with overdue count, average distance. Display in project-level foreshadowing page. |
| D-03 | Volume-level foreshadowing summary in volume plan | When viewing a volume, show how many foreshadowing are planted/resolved within it, and which cross-volume foreshadowing pass through it. | MEDIUM | Aggregate query by `plantedVolume`/`plannedCallbackVolume`. Display in volume plan view. |
| D-04 | AI suggests foreshadowing during volume planning | When generating volume plans, the LLM could suggest foreshadowing to plant across volumes. This pre-seeds the foreshadowing table before chapter-level planning. | HIGH | Requires modifying `VolumeOptimizeTaskStrategy` to output foreshadowing suggestions. The `novel_volume_plan.stage_foreshadowings` JSON column already exists for this purpose. Defer unless simple. |
| D-05 | Post-generation foreshadowing verification | After chapter generation, verify that the AI actually planted/resolved the planned foreshadowing. Like the character comparison view (v1.0.5), show planned vs actual. | HIGH | Requires LLM to extract planted/resolved foreshadowing from generated text (like character extraction). Match against plan. Display comparison in ChapterPlanDrawer. |

## Anti-Features (Explicitly Do NOT Build)

Features that seem natural but would overcomplicate the system or contradict existing architecture.

| Anti-Feature | Why It Seems Appealing | Why We Should NOT Build It | What To Do Instead |
|-------------|----------------------|---------------------------|-------------------|
| Merge ChapterPlotMemory foreshadowing into novel_foreshadowing | "One source of truth" -- unify text arrays in memory with structured table. | ChapterPlotMemory is a post-hoc AI analysis of what happened. novel_foreshadowing is a pre-planned intent. They serve different purposes (after-the-fact vs before-the-fact). Merging would lose the distinction between "AI noticed it happened" and "user/AI planned it." | Keep both systems. ChapterPlotMemory continues as automated post-generation analysis. novel_foreshadowing is the planning ledger. In the future, cross-reference them for verification (D-05). |
| Hard enforcement (block generation if foreshadowing not resolved) | "Ensure every planted foreshadowing gets resolved" | AI text is probabilistic. Hard enforcement would cause regeneration loops. Some foreshadowing is intentionally left as open threads (series hooks). | Soft guidance: inject foreshadowing constraints strongly in the prompt. Show overdue foreshadowing as warnings. Let users decide. |
| Visual foreshadowing timeline / Gantt chart | "See foreshadowing threads spanning chapters visually" | Requires a timeline/gantt rendering library that does not exist in the frontend stack. High frontend cost for low writing-value ROI. | Table/list view with sort/filter by volume, status, chapter. Sufficient for managing foreshadowing during writing. |
| Foreshadowing dependency graph (A requires B to be resolved first) | "Manage foreshadowing that depends on other foreshadowing" | Adds a graph data model to what is currently a flat table. Over-engineered for the current scope. Most novels have at most dozens of active foreshadowing, manageable without dependency tracking. | Use the `notes` field to document dependencies manually. Priority field handles ordering. |
| Automatic foreshadowing creation from ChapterPlotMemory | "Every foreshadowing the AI memory extraction finds should become a novel_foreshadowing record" | This would create noise. The AI memory extraction is noisy and includes generic observations. Structured foreshadowing should be intentional, not auto-discovered. | Let AI planning (T-03) create foreshadowing intentionally. Users can manually add foreshadowing for things the AI missed. |

## Feature Dependencies

```
[T-01: Volume fields on novel_foreshadowing]
    |
    +---> [T-03: AI chapter plan includes foreshadowing decisions]
    |         +--requires--> [Existing foreshadowing table] (already exists)
    |         +--requires--> [T-01] (volume context for cross-volume foreshadowing)
    |         +--requires--> [Prompt template update: llm_outline_chapter_generate]
    |         +--requires--> [XML parsing extension: ChapterGenerationTaskStrategy]
    |         +--produces--> [novel_foreshadowing records from AI output]
    |
    +---> [T-04: Chapter generation injects foreshadowing constraints]
    |         +--requires--> [T-01] (volume context)
    |         +--requires--> [T-03] (foreshadowing records must exist in table)
    |         +--requires--> [ChapterContext or new ForeshadowingContext]
    |         +--requires--> [PromptTemplateBuilder modification]
    |         +--degrades-gracefully--> [Current behavior: free-text pending reminders from ChapterPlotMemory]
    |
    +---> [T-02: Remove foreshadowingSetup/foreshadowingPayoff]
    |         +--requires--> [T-05] (replacement UI must exist first)
    |         +--cleans-up--> [NovelChapterPlan entity, ChapterPlanDrawer.vue, ChapterPlanUpdateRequest]
    |
    +---> [T-05: ChapterPlanDrawer foreshadowing management]
    |         +--requires--> [T-01] (volume context in data)
    |         +--requires--> [Existing foreshadowing API] (ForeshadowingController)
    |         +--replaces--> [Current textareas for foreshadowingSetup/foreshadowingPayoff]
    |
    +---> [T-06: Project-level foreshadowing page]
              +--requires--> [T-01] (volume-based filtering)
              +--requires--> [Router + sidebar nav update]

[D-01: Min plant-to-payoff distance]
    +--requires--> [T-01] (needs chapter numbers)
    +--standalone--> [Validation logic in ForeshadowingService]

[D-02: Foreshadowing balance score]
    +--requires--> [T-01] (volume context for overdue calculation)
    +--standalone--> [Extends ForeshadowingStats]
```

### Dependency Notes

1. **T-01 is the foundation.** Volume fields must exist before any cross-volume foreshadowing operations work. This is a simple migration: two integer columns, entity update, DTO update.

2. **T-03 and T-04 can be developed in sequence but not parallel.** T-03 (AI planning creates foreshadowing records) must complete before T-04 (generation uses those records) can be tested end-to-end. However, T-04's prompt changes can be developed against manually-created foreshadowing records.

3. **T-02 must come AFTER T-05.** The old text fields must remain functional until the new structured UI replaces them. Removing them first would break the existing ChapterPlanDrawer.

4. **T-06 is independent of T-03/T-04.** The project-level foreshadowing page can be built with just the existing CRUD API + volume fields. It does not need the AI integration to be useful.

5. **The ChapterPlotMemory system remains untouched.** It continues to provide post-generation foreshadowing analysis independently. The new system adds pre-planning intent. In the future (D-05), the two can be cross-referenced.

## MVP Definition

### Launch With (v1.0.6)

The minimum to make foreshadowing a structured, AI-driven part of the pipeline.

- [ ] T-01: Volume fields on novel_foreshadowing -- data foundation, no AI or UI changes
- [ ] T-03: AI chapter plan outputs foreshadowing planting/resolution -- core AI integration
- [ ] T-04: Chapter generation injects structured foreshadowing constraints -- enforcement mechanism
- [ ] T-05: ChapterPlanDrawer foreshadowing management UI -- user visibility and control at chapter level
- [ ] T-06: Project-level foreshadowing page -- project-wide ledger view with sidebar nav

### Add After Validation (v1.0.7)

- [ ] T-02: Remove foreshadowingSetup/foreshadowingPayoff fields -- cleanup after new UI is validated
- [ ] D-01: Minimum plant-to-payoff distance enforcement -- simple validation, low risk
- [ ] D-02: Foreshadowing balance score -- extends existing stats endpoint

### Future Consideration (v1.1+)

- [ ] D-03: Volume-level foreshadowing summary -- aggregate view per volume
- [ ] D-04: AI suggests foreshadowing during volume planning -- pre-seeds table before chapter planning
- [ ] D-05: Post-generation foreshadowing verification -- planned vs actual comparison view
- [ ] Cross-reference ChapterPlotMemory with novel_foreshadowing for automated verification

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| T-01: Volume fields | HIGH | LOW (migration + entity) | P1 |
| T-03: AI plan outputs foreshadowing | HIGH | MEDIUM (prompt + parsing + persistence) | P1 |
| T-04: Generation injects foreshadowing | HIGH | MEDIUM (prompt + context modification) | P1 |
| T-05: ChapterPlanDrawer foreshadowing UI | HIGH | MEDIUM (new component section) | P1 |
| T-06: Project foreshadowing page | MEDIUM | MEDIUM (new route + page + sidebar) | P1 |
| T-02: Remove old text fields | LOW (cleanup) | LOW | P2 |
| D-01: Min distance validation | MEDIUM | LOW | P2 |
| D-02: Balance score | MEDIUM | LOW | P2 |
| D-03: Volume summary | LOW | MEDIUM | P3 |
| D-04: Volume planning suggestions | MEDIUM | HIGH | P3 |
| D-05: Post-gen verification | HIGH | HIGH | P3 |

## Competitor Feature Analysis

| Feature | Scrivener | Plottr | AutoNovel | AI Factory (Our Approach) |
|---------|-----------|--------|-----------|---------------------------|
| Foreshadowing tracking | Manual (keywords, status labels, notes) | Visual timeline + subplot layers | Automated ledger with plant/payoff chapters | Structured table + AI-driven planning + user editing |
| AI-assisted planting | None | None | Full auto (generates ledger during outline) | Semi-auto (AI suggests during chapter planning, user approves/edits) |
| Plant-to-payoff distance | Manual awareness | Visual gap on timeline | Enforced (minimum 3 chapters) | Planned: configurable minimum distance (D-01) |
| Cross-volume foreshadowing | Manual (folder organization) | Series-level tracking | N/A (single novel) | Volume fields on foreshadowing records (T-01) |
| Balance scoring | None | None | Automated evaluation (weight 9/10) | Stats + overdue detection (D-02) |
| Verification (planned vs actual) | None | None | Evaluation script checks every plant has payoff | Future: cross-reference with ChapterPlotMemory (D-05) |

**Competitive advantage:** AI Factory is the only tool that combines AI-driven foreshadowing planning (auto-creating ledger entries during chapter planning), structured persistence, user override, and AI-constrained generation in a single pipeline. Scrivener/Plottr are purely manual. AutoNovel is fully automated with no user intervention.

## AI Integration Architecture for Foreshadowing

### Chapter Planning Phase (T-03)

The LLM chapter plan output currently uses this XML schema:
```xml
<c>
  <o>
    <n>1</n>  <!-- chapterNumber -->
    <t>Title</t>  <!-- chapterTitle -->
    <p>Plot outline</p>
    <e>Key events</e>
    <g>Chapter goal</g>
    <w>3000</w>  <!-- wordCountTarget -->
    <s>Starting scene</s>
    <f>Ending scene</f>
    <ch>  <!-- plannedCharacters (added v1.0.5) -->
      <cn>Name</cn>
      <cd>Description</cd>
      <ci>importance</ci>
    </ch>
  </o>
</c>
```

For foreshadowing, new XML tags should be added:
```xml
    <fs>  <!-- foreshadowing to plant -->
      <fst>Title</fst>
      <fsd>Description of foreshadowing</fsd>
      <fsy>character</fsy>  <!-- type: character/item/event/secret -->
      <fsv>2</fsv>  <!-- plannedCallbackVolume -->
    </fs>
    <fr>  <!-- foreshadowing to resolve -->
      <frt>Existing foreshadowing title</frt>
      <frd>How it resolves</frd>
    </fr>
```

The planning prompt must inject the current list of unresolved foreshadowing (from `novel_foreshadowing` table, not from ChapterPlotMemory) so the AI can decide which to resolve.

### Chapter Generation Phase (T-04)

The current prompt injection via `ChapterContext.pendingForeshadowing` provides generic text like "consider resolving these." This must change to structured instructions:

```
### Foreshadowing Instructions for This Chapter

**Plant (埋设) these foreshadowing:**
1. [Title]: [Description] (Type: [type], Plan to resolve in Volume [N])

**Resolve (回收) these foreshadowing:**
1. [Title]: [Description] (Planted in Chapter [N], Volume [V])

**IMPORTANT:** You MUST plant the items listed above and MUST resolve the items listed above. Integrate them naturally into the narrative.
```

This is analogous to how `hasPlannedCharacters()` injects character constraints -- the same pattern applies here: check if there are foreshadowing records for this chapter, and if so, build a structured instruction section.

## Data Schema for Foreshadowing Table (Post-Migration)

```sql
ALTER TABLE novel_foreshadowing
  ADD COLUMN planted_volume INT NULL COMMENT '埋设伏笔的卷号' AFTER planted_chapter,
  ADD COLUMN planned_callback_volume INT NULL COMMENT '计划填坑的卷号' AFTER planned_callback_chapter;
```

The `planted_volume` and `planned_callback_volume` fields enable:
- Cross-volume foreshadowing queries
- Volume-scoped foreshadowing views
- Overdue detection: foreshadowing where `plannedCallbackVolume < currentVolume` and status != completed

## Sources

- **Codebase analysis:** `Foreshadowing.java`, `ForeshadowingService.java`, `ForeshadowingController.java`, `ChapterPlotMemory.java`, `ChapterContext.java`, `NovelChapterPlan.java`, `ChapterContentGenerateTaskStrategy.java`, `ChapterGenerationTaskStrategy.java`, `PromptTemplateBuilder.java`, `ChapterPlanDrawer.vue`, `ProjectSidebar.vue`, `sql/init.sql`
- **Competitor research:** [Scrivener forum - subplot tracking](https://forum.literatureandlatte.com/t/how-do-you-keep-track-of-sub-plots-in-a-book/140479), [Plottr visual story planning](https://www.draft2digital.com/blog/visual-story-planning-with-plottr-ep122/)
- **Academic reference:** [AutoNovel pipeline (NousResearch)](https://github.com/NousResearch/autonovel/blob/master/PIPELINE.md) -- foreshadowing ledger pattern with plant-to-payoff distance enforcement and automated balance evaluation
- **AutoNovel code:** [gen_outline.py](https://github.com/NousResearch/autonovel/blob/master/gen_outline.py), [evaluate.py](https://github.com/NousResearch/autonovel/blob/master/evaluate.py), [draft_chapter.py](https://github.com/NousResearch/autonovel/blob/master/draft_chapter.py)
- **Existing patterns in AI Factory:** Character planning integration (v1.0.5), faction name resolution, DOM XML parsing, prompt template system

---
*Feature research for: foreshadowing management system in AI novel generation*
*Researched: 2026-04-10*
