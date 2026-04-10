# Project Research Summary

**Project:** AI Factory -- Foreshadowing Management System (v1.0.6)
**Domain:** Structured foreshadowing lifecycle management for AI novel generation (Spring Boot + Vue.js)
**Researched:** 2026-04-10
**Confidence:** HIGH

## Executive Summary

This milestone activates an existing but dormant `novel_foreshadowing` table as the primary driver of the foreshadowing lifecycle in the AI Factory novel generation pipeline. The system already has the table with full CRUD (ForeshadowingService, ForeshadowingController, 6 REST endpoints), but it is completely disconnected from chapter planning and generation. The work involves adding volume-level granularity to the table, integrating foreshadowing decisions into the chapter planning LLM prompt (so the AI decides which foreshadowing to plant and resolve), injecting those decisions as hard constraints into chapter content generation, and building frontend management UI. Zero new dependencies are required -- every change extends patterns already proven in v1.0.5 for character planning.

The critical architectural insight is that two independent foreshadowing tracking systems currently exist and must not be merged: the structured `novel_foreshadowing` table (pre-planned intent) and the text-based `ChapterPlotMemory` arrays (post-generation AI analysis). This milestone designates the structured table as the sole source of truth for foreshadowing constraint injection, while the memory system continues serving its existing role for runtime AI recall. The orphaned `foreshadowingSetup`/`foreshadowingPayoff` text fields on `novel_chapter_plan` will be deprecated (entity fields marked `@TableField(exist = false)`) but the database columns will be retained to avoid data loss for existing projects.

The primary risks are: (1) chapter number references becoming stale when volumes are regenerated -- mitigated by storing volume number alongside chapter number and treating callback chapters as hints rather than hard constraints; (2) the XML parser silently ignoring new foreshadowing tags if the `sanitizeXmlForDomParsing` container tags array is not updated; (3) prompt context window overflow if too many foreshadowing items are injected -- mitigated by concise constraint format and volume-scoped filtering with proper database indexes.

## Key Findings

### Recommended Stack

No new dependencies. All work extends the existing stack: Spring Boot 3.2.0, MyBatis-Plus 3.5.5, Vue 3, Vite, Tailwind CSS 4.1.x, LangChain4j 1.11.0, DOM XML parsing (javax.xml), Lucide Vue Next icons. The only infrastructure change is a SQL migration adding two integer columns and three composite indexes to `novel_foreshadowing`.

**Core technologies reused:**
- **MyBatis-Plus BaseMapper:** existing ForeshadowingMapper supports all needed queries without custom SQL
- **DOM XML parsing:** extends the same `parseSingleChapter` switch statement used for character tags in v1.0.5
- **PromptTemplateService:** injects foreshadowing context using the same variable substitution pattern as character constraints
- **Axios + existing request utility:** frontend API client follows the `faction.ts`/`character.ts` pattern

### Expected Features

**Must have (table stakes):**
- **T-01: Volume fields on novel_foreshadowing** -- cross-volume foreshadowing is fundamental to long-form novels; `plantedVolume`/`plannedCallbackVolume` integer columns enable scoping and grouping
- **T-03: AI chapter plan outputs foreshadowing decisions** -- the LLM must decide which foreshadowing to plant and resolve per chapter; new `<fs>`/`<fp>` XML tags in chapter plan output, parsed and persisted to `novel_foreshadowing`
- **T-04: Chapter generation injects foreshadowing constraints** -- structured "plant these" and "resolve these" sections in the generation prompt, queried from the structured table
- **T-05: ChapterPlanDrawer foreshadowing management UI** -- replace orphaned textareas with structured card display showing foreshadowing to plant and resolve for this chapter
- **T-06: Project-level foreshadowing page** -- sidebar navigation to a project-wide foreshadowing ledger with stats, filters, and CRUD

**Should have (competitive):**
- **D-01: Minimum plant-to-payoff distance enforcement** -- configurable validation preventing instant-resolution foreshadowing
- **D-02: Foreshadowing balance score** -- health metric showing planted/resolved ratio, overdue count, average distance

**Defer (v1.0.7+):**
- **T-02: Remove foreshadowingSetup/foreshadowingPayoff columns** -- cleanup after new UI is validated
- **D-03/D-04/D-05:** Volume summary, volume planning suggestions, post-generation verification

### Architecture Approach

The integration follows the v1.0.5 character planning pattern end-to-end: inject context into chapter plan LLM prompts, parse structured XML output from the LLM, inject constraints into chapter content generation prompts, and provide management UI. The key difference is that foreshadowing uses a dedicated database table rather than a JSON field, requiring batch create/update operations instead of simple string persistence. The data flows through three pipeline stages: (1) chapter plan generation queries active foreshadowing, injects context, LLM outputs `<fs>`/`<fp>` tags which are parsed and batch-saved to `novel_foreshadowing`; (2) chapter content generation queries foreshadowing to plant and resolve for the target chapter, builds constraint text, injects into prompt; (3) frontend provides chapter-scoped view (ChapterPlanDrawer tab) and project-scoped view (dedicated page with sidebar nav).

**Major components:**
1. **ForeshadowingService (backend, MODIFY)** -- add 5 methods: `getActiveForeshadowings`, `getForeshadowingsByChapter`, `getForeshadowingsToPlant`, `getForeshadowingsToCallback`, `batchCreateForeshadowings`
2. **ChapterGenerationTaskStrategy (backend, MODIFY)** -- inject foreshadowing context into chapter plan prompt, extend `parseSingleChapter` to handle `<fs>`/`<fp>` tags, batch-save parsed foreshadowing
3. **PromptTemplateBuilder (backend, MODIFY)** -- add `hasForeshadowingConstraints`/`buildForeshadowingConstraintText` mirroring the character constraint pattern
4. **ChapterPlanDrawer.vue (frontend, MODIFY)** -- replace textareas with structured foreshadowing cards (read-only summary + link to dedicated management)
5. **Foreshadowing.vue (frontend, NEW)** -- project-level foreshadowing management page with stats, filters, CRUD

### Critical Pitfalls

1. **Chapter number instability across volumes** -- When volumes are regenerated, global chapter numbers shift, breaking foreshadowing references. Store both volume number and relative chapter number; treat callback chapters as approximate hints, not hard constraints. Add validation for temporal consistency.
2. **Dual foreshadowing source divergence** -- The structured table and ChapterPlotMemory text arrays will diverge after activation. Designate the structured table as sole source for constraint injection. Keep memory system for post-generation analysis only.
3. **XML parser silently ignores new tags** -- New `<fs>`/`<fp>` container tags must be added to `sanitizeXmlForDomParsing` container tags array, and the parser switch statement must handle them. Update parser BEFORE prompt template.
4. **Prompt context window overflow** -- Cumulative context injection (worldview + characters + foreshadowing) may exceed the LLM effective window. Use concise constraint format, filter to current-chapter items only, avoid dumping all pending foreshadowing.
5. **Old field removal breaks existing data** -- Do NOT drop `foreshadowingSetup`/`foreshadowingPayoff` DB columns immediately. Mark entity fields as `@TableField(exist = false)` instead. Retain columns for data safety.

## Implications for Roadmap

Based on research, the following phase structure respects dependency ordering and groups related changes:

### Phase 1: Data Foundation
**Rationale:** All subsequent phases depend on the data model and service methods being available. Must come first.
**Delivers:** Extended entity with volume fields, new service methods, new controller endpoints, SQL migration with indexes, deprecated chapter plan fields
**Addresses:** T-01 (volume fields), partial T-02 (entity-level deprecation)
**Avoids:** Pitfall 1 (chapter number instability -- volume fields from the start), Pitfall 3 (safe field deprecation strategy), Pitfall 5 (query performance -- indexes in migration)

### Phase 2: AI Integration -- Chapter Planning
**Rationale:** The LLM must output foreshadowing XML tags before parsing can be tested. Prompt template and parser must be updated in the same phase.
**Delivers:** Updated chapter plan prompt template with foreshadowing context injection and XML output instructions, extended XML parser with `<fs>`/`<fp>` handling, batch save of foreshadowing records
**Addresses:** T-03 (AI chapter plan outputs foreshadowing decisions)
**Avoids:** Pitfall 4 (parser and prompt updated together), Pitfall 8 (prompt designed for approximate, not exact, callback targets)

### Phase 3: AI Integration -- Chapter Generation Constraints
**Rationale:** Depends on Phase 1 service methods. Can be developed in parallel with Phase 2 but must be tested after Phase 2 produces real foreshadowing data.
**Delivers:** Foreshadowing constraint injection into chapter content generation prompts via PromptTemplateBuilder
**Addresses:** T-04 (chapter generation injects foreshadowing constraints)
**Avoids:** Pitfall 2 (constraint injection uses structured table exclusively, not memory text), Pitfall 6 (concise constraint format to avoid context overflow)

### Phase 4: Frontend -- API Client and Chapter Plan Drawer
**Rationale:** Depends on Phase 1 backend endpoints. Can proceed once API contract is stable.
**Delivers:** `foreshadowing.ts` API client, TypeScript types, rebuilt foreshadowing tab in ChapterPlanDrawer with structured card display
**Addresses:** T-05 (ChapterPlanDrawer foreshadowing UI)
**Avoids:** Pitfall 7 (read-only summary in drawer, not full CRUD -- keeps drawer under control)

### Phase 5: Frontend -- Project-Level Management Page
**Rationale:** Depends on Phase 4 API client and types. Final piece that makes foreshadowing a first-class managed entity.
**Delivers:** Foreshadowing.vue management page, sidebar navigation entry, route registration, optional reusable components
**Addresses:** T-06 (project-level foreshadowing page)
**Avoids:** No specific pitfall -- standard pattern matching Characters.vue

### Phase Ordering Rationale

- Phase 1 is the foundation: without volume fields and service methods, nothing else compiles or works
- Phases 2 and 3 are the AI pipeline core: 2 creates foreshadowing records, 3 uses them for generation
- Phases 4 and 5 are frontend: 4 fixes the immediate drawer experience, 5 adds project-wide visibility
- The ordering allows Phase 3 and Phase 4 to proceed in parallel once Phase 1 and 2 complete, since they depend on different backend outputs (service methods vs. foreshadowing data in the table)

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2:** The exact prompt template wording for foreshadowing context injection and XML output instructions needs careful design. The LLM must produce valid XML with correct tag nesting. Test with real LLM output.
- **Phase 3:** The concise constraint format must be validated against the LLM's actual context window usage. Measure prompt length with 10-20 active foreshadowing items.

Phases with standard patterns (skip research-phase):
- **Phase 1:** Straightforward entity/DTO/service/controller extensions following existing patterns
- **Phase 4:** Follows the character tab pattern from v1.0.5 exactly
- **Phase 5:** Follows the Characters.vue pattern exactly

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Zero new dependencies; entirely based on codebase analysis of existing patterns from v1.0.2--v1.0.5 |
| Features | HIGH | Clear table stakes identified; feature dependency graph is well-defined; competitor analysis validates approach |
| Architecture | HIGH | Direct source code analysis of all 18 integration points; v1.0.5 character planning provides a proven blueprint |
| Pitfalls | HIGH | Derived from codebase analysis of existing failure modes and v1.0.5 precedent; specific code locations identified |

**Overall confidence:** HIGH

### Gaps to Address

- **Prompt template design:** The exact wording for foreshadowing context injection and XML output instructions has not been drafted. The prompt must balance specificity (so the LLM produces valid XML) with flexibility (so it can adapt foreshadowing to the narrative). This needs iteration during Phase 2 implementation.
- **LLM output validation:** How to validate that the LLM's foreshadowing output is temporally consistent (callback after planting) and structurally valid (all required XML child tags present). A validation pass may be needed after XML parsing.
- **Cross-volume reference stability:** The volume + relative-chapter-number approach mitigates but does not fully solve the regeneration problem. A post-regeneration re-resolution step may be needed but is deferred to v1.0.7+.
- **Status auto-update from memory:** Whether to auto-update `novel_foreshadowing.status` based on `ChapterPlotMemory.foreshadowingResolved` is an open design decision. Manual-only is safer for v1.0.6; auto-detection can be added as a differentiator later.

## Sources

### Primary (HIGH confidence)
- Codebase analysis: `Foreshadowing.java`, `ForeshadowingService.java`, `ForeshadowingController.java`, `ForeshadowingMapper.java` -- existing CRUD infrastructure
- Codebase analysis: `ChapterGenerationTaskStrategy.java` -- DOM XML parsing pattern, character tag handling, batch save pattern
- Codebase analysis: `PromptTemplateBuilder.java` -- constraint injection pattern (`hasPlannedCharacters`/`buildPlannedCharacterInfoText`)
- Codebase analysis: `ChapterPlanDrawer.vue` -- current foreshadowing tab with textareas, character tab pattern
- Codebase analysis: `ProjectSidebar.vue`, `router/index.ts` -- navigation and routing patterns
- Codebase analysis: `sql/init.sql` -- `novel_foreshadowing` table schema (lines 340-360)
- Previous milestone research: `.planning/research/STACK.md` (v1.0.5 character planning, confirmed zero-dependency pattern)

### Secondary (MEDIUM confidence)
- AutoNovel project (NousResearch) -- foreshadowing ledger pattern, plant-to-payoff distance enforcement, automated balance evaluation
- Scrivener forum -- manual subplot tracking approaches
- Plottr -- visual story planning patterns

---
*Research completed: 2026-04-10*
*Ready for roadmap: yes*
