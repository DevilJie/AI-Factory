# Project Research Summary

**Project:** AI Factory -- Chapter Character Planning (v1.0.5)
**Domain:** Chapter-level character planning and enforcement for AI novel generation (Spring Boot + Vue.js)
**Researched:** 2026-04-07
**Confidence:** HIGH

## Executive Summary

This milestone adds a closed-loop character management system to the AI novel generation pipeline. Currently, the pipeline operates open-loop for characters: chapter plans have no character assignments, chapter generation dumps ALL non-NPC characters into the prompt regardless of relevance, and post-generation extraction has no plan to validate against. The result is inconsistent character screen time, wasted token budget on irrelevant characters, and no user visibility into which characters appear where.

The fix threads a `planned_characters` JSON field through the existing pipeline: the chapter planning LLM decides which characters appear and what they do, the data is persisted to `novel_chapter_plan`, and the chapter generation prompt uses ONLY those planned characters (with their roles and arcs) instead of the full project roster. This requires zero new dependencies. Every change extends an existing pattern: the database already has an unused `character_arcs` JSON column, the XML parsing regex in `OutlineTaskStrategy` already handles CDATA fields, the `PromptTemplateBuilder` already formats character data for prompts, and `ChapterPlanDrawer.vue` already displays chapter plan details. The total footprint is approximately 180 lines across 8 code files, 1 SQL migration, and 2 prompt template text updates.

The key risks are: (1) the LLM may ignore planned characters during generation -- mitigated by injecting them as a MUST-FOLLOW constraint block rather than background information, and by replacing the full character list with the planned subset to avoid contradictory signals; (2) AI-generated character names may not exactly match database names -- mitigated by reusing the proven three-tier fuzzy matching pattern from `WorldviewXmlParser` and by injecting the existing character name list into the planning prompt; (3) two duplicate `ChapterPlanXmlDto` classes exist in different packages -- both must be updated or consolidated before adding character fields.

## Key Findings

### Recommended Stack

No new dependencies. All changes are extensions of the existing Spring Boot 3.2.0 + MyBatis-Plus 3.5.5 + Vue 3.5.x stack. The research confirms that every technical need has a proven solution already in the codebase: Jackson `ObjectMapper` for JSON serialization, regex-based `extractXmlFieldCData()` for XML parsing, `PromptTemplateService.executeTemplate()` for AI template execution, and `ChapterPlanDrawer.vue` for frontend display. The `planned_characters` field follows the same `String`-typed JSON column pattern used by `keyEvents`, `foreshadowingSetup`, and `cultivationLevel`.

**Core technologies (unchanged):**
- Spring Boot 3.2.0 + MyBatis-Plus 3.5.5: Backend -- entity field addition, service method extension
- Vue 3.5.x + Tailwind CSS 4.1.x: Frontend -- read-only character card display in existing drawer
- Jackson ObjectMapper (Spring-managed): JSON parsing -- already injected in `PromptTemplateBuilder` and `OutlineTaskStrategy`
- Regex-based XML parsing (`extractXmlFieldCData`): AI output extraction -- proven pattern in `OutlineTaskStrategy` lines 1273-1289

### Expected Features

**Must have (table stakes):**
- Planned characters field on `novel_chapter_plan` (T-01) -- JSON column storing per-chapter character assignments with name, role, scene, and importance
- AI generates character assignments during chapter planning (T-02) -- extend `llm_outline_chapter_generate` template with `<r>` CDATA output tag
- Parse character planning from AI XML output (T-03) -- extend `parseChaptersXml()` and both save methods in `OutlineTaskStrategy`
- Inject planned characters into chapter generation prompt (T-04) -- modify `PromptTemplateBuilder` to add constraint block and filter `characterInfo` to planned subset only
- Frontend display in `ChapterPlanDrawer.vue` (T-05) -- read-only card layout showing planned characters with role badges
- Cross-reference extracted characters against plan (T-06) -- post-generation warning log comparing planned vs. actual

**Should have (competitive, defer to follow-up):**
- Manual character planning override (D-01) -- character picker UI for user edits before generation
- Character arc tracking per chapter (D-02) -- map the existing `character_arcs` column for structured arc data
- Character appearance density analysis (D-03) -- aggregate query comparing planned vs. actual appearances across chapters

**Defer (v2+):**
- Automatic re-generation on plan-vs-actual mismatch
- Character scheduling timeline view
- Per-character word count quotas

### Architecture Approach

The integration follows the existing four-stage pipeline (VolumePlan -> ChapterPlan -> ChapterGenerate -> CharacterExtract) and inserts character data at stages 2 and 3. The data flows as: AI outputs character planning inside `<r>` CDATA tags within each chapter's `<o>` block during planning, the backend extracts and stores it as JSON in `novel_chapter_plan.planned_characters`, and the chapter generation prompt reads it to build a focused character constraint block while filtering out unrelated characters. No new tables, services, or architectural components are introduced.

**Major integration points:**
1. **NovelChapterPlan entity + SQL migration** -- Add `planned_characters` JSON column and `characterArcs` field mapping (the column exists in DB but is not mapped in the entity)
2. **OutlineTaskStrategy** -- Extend `parseChaptersXml()` to extract `<r>` tag data, extend both `saveVolumeChaptersToDatabase()` and `saveChaptersToDatabase()` to persist it
3. **PromptTemplateBuilder** -- Add `buildPlannedCharactersText()` method, modify `buildTemplateVariables()` to inject planned characters and filter `characterInfo`
4. **ChapterPlanDrawer.vue** -- Add read-only planned characters display section with Tailwind card styling

### Critical Pitfalls

1. **LLM ignores planned characters during generation (Plan Drift)** -- The generation prompt currently injects ALL non-NPC characters as context. Adding planned characters alongside the full list creates contradictory signals. Prevention: when `planned_characters` exists, REPLACE `characterInfo` with only the planned subset plus strong MUST-FOLLOW constraint language. Do not show both lists simultaneously.

2. **AI-generated character names do not match database names** -- The chapter plan LLM currently receives `characterInfo` hardcoded to "No appearing characters yet" (`ChapterGenerationTaskStrategy` line 435), giving it no name reference. The extraction service only does exact name matching. Prevention: inject a simplified character name list into the plan prompt, and reuse the three-tier fuzzy matching pattern from `WorldviewXmlParser` (exact -> strip suffix -> contains).

3. **Duplicate ChapterPlanXmlDto classes** -- Two versions exist in `com.aifactory.common.xml` and `com.aifactory.dto`. Updating one but not the other causes silent parsing failures. Prevention: consolidate to a single DTO or update both simultaneously. Grep all imports before starting.

4. **JSON schema too rigid causes parse failures** -- If `planned_characters` requires many fields, the LLM will produce malformed JSON. Prevention: keep the schema minimal (characterName required, roleType required, everything optional), store as XML during AI generation and convert to JSON for database, use lenient parsing with `FAIL_ON_UNKNOWN_PROPERTIES = false`.

5. **Token budget exceeded in chapter plan prompt** -- Adding character planning XML to the already-large chapter plan output may push the LLM past its output token limit for 5-chapter batches. Prevention: reduce batch size to 3 chapters, use concise single-letter tags (`<r>` for roles), and monitor for truncated XML.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Database + Entity Foundation
**Rationale:** Every subsequent phase depends on the `planned_characters` column existing in the database and being mapped in the Java entity. This phase has zero risk -- adding a nullable JSON column does not affect any existing code.
**Delivers:** SQL migration (ALTER TABLE), NovelChapterPlan entity field additions (`plannedCharacters`, `characterArcs`), frontend type definitions (`PlannedCharacter`, `CharacterArc` interfaces)
**Addresses:** T-01 (planned characters field)
**Avoids:** Pitfall 4 (duplicate DTOs -- consolidate before extending), Pitfall 8 (`character_arcs` vs `planned_characters` column overlap -- decide explicitly that they serve different purposes)

### Phase 2: AI Planning Output + XML Parsing
**Rationale:** The AI must produce character assignments during chapter planning before anything else can consume them. This phase modifies the chapter planning prompt template and extends the XML parser to extract character data. It requires the entity fields from Phase 1 but nothing else.
**Delivers:** Updated `llm_outline_chapter_generate` template with `<r>` output tag, extended `parseChaptersXml()` and both save methods in `OutlineTaskStrategy`, character name injection in plan prompt (replacing hardcoded "No appearing characters yet")
**Addresses:** T-02 (AI generates character assignments), T-03 (parse and persist)
**Avoids:** Pitfall 2 (name mismatch -- inject name list into prompt), Pitfall 3 (minimal JSON schema -- parse from XML, not from JSON), Pitfall 9 (token budget -- use concise tags, consider reducing batch size)

### Phase 3: Generation Enforcement
**Rationale:** The most architecturally significant change. This phase modifies how the chapter generation prompt is constructed, replacing the current "dump all characters" approach with a focused "planned characters only" approach. Depends on Phase 2 producing actual planned character data in the database.
**Delivers:** `buildPlannedCharactersText()` and `buildFocusedCharacterInfo()` methods in `PromptTemplateBuilder`, updated `llm_chapter_generate_standard` template with `${plannedCharacters}` variable, graceful fallback for plans without character data
**Addresses:** T-04 (inject planned characters into generation prompt)
**Avoids:** Pitfall 1 (plan drift -- MUST-FOLLOW constraint language), Pitfall 5 (contradictory signals -- replace full list with planned subset)
**Research flag:** This phase needs validation. Test with 2-3 real chapter generations to verify the LLM actually follows the character plan. The prompt constraint language may need iteration.

### Phase 4: Frontend Display + Verification
**Rationale:** Purely presentation and verification layer. Depends on data flowing through the backend pipeline (Phases 1-3) but can be developed in parallel with Phase 3 since it only needs the API response to include the field.
**Delivers:** Planned characters card display in `ChapterPlanDrawer.vue`, API DTO extension (`ChapterPlanDto`), post-extraction comparison logging (WARN for missing planned characters)
**Addresses:** T-05 (frontend display), T-06 (cross-reference verification)
**Avoids:** Pitfall 10 (false negatives from extraction -- use name-contains check as fallback, treat misses as soft warnings)

### Phase Ordering Rationale

- Phase 1 is foundational: the database column and entity field are prerequisites for every other change.
- Phase 2 before Phase 3: generation enforcement needs planned character data to exist, which requires the planning prompt and parser to work first.
- Phase 4 can overlap with Phase 3: frontend only needs the entity field and API DTO, not the generation prompt changes.
- Phase 3 is the highest-risk phase: it changes prompt construction logic that affects generation quality. Must be tested end-to-end with real LLM calls.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2:** The `llm_outline_chapter_generate` prompt template is large and complex. Adding character planning instructions without exceeding the instruction budget or confusing the LLM about output format requires careful prompt engineering. Test with actual LLM output before committing to the template text.
- **Phase 3:** The "MUST-FOLLOW" constraint language for planned characters is untested in this codebase. The specific wording, positioning in the prompt, and enforcement strength need experimentation. Consider running A/B tests: generate chapters with and without character plans, compare adherence.

Phases with standard patterns (skip research-phase):
- **Phase 1:** Direct mirror of existing JSON column additions (`keyEvents`, `foreshadowingSetup`). Zero ambiguity.
- **Phase 4:** Read-only display in existing drawer component. Straightforward Vue 3 + Tailwind work.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Zero new dependencies. All patterns verified in production codebase. Every technical need maps to an existing solution. |
| Features | HIGH | Feature set derived from thorough codebase analysis of the existing chapter planning, generation, and extraction pipelines. Dependency graph fully traced with line-number references. |
| Architecture | HIGH | Integration touches 8 files with clear, minimal changes. No new tables, services, or components. All patterns (JSON column, XML parsing, template variables, drawer display) are proven in the codebase. |
| Pitfalls | HIGH | All pitfalls derived from direct codebase analysis with specific line-number references. The plan drift problem (Pitfall 1) is well-understood from the existing behavior of dumping all characters into the prompt. |

**Overall confidence:** HIGH

### Gaps to Address

- **Prompt template output quality:** The `<r>` CDATA tag format for character planning is designed but untested with actual LLM output. The LLM may produce malformed JSON inside CDATA, omit the `<r>` tag entirely, or generate characters not in the project roster. The template text must be validated with test generations during Phase 2.
- **Fuzzy name matching rules for characters:** The three-tier matching from `WorldviewXmlParser` works for faction names, but Chinese character names have different ambiguity patterns (e.g., "Lin Dong" vs. "Lin Dongxiao" vs. "Little Lin Dong"). The specific normalization rules need testing against real AI output during Phase 2.
- **Batch size impact:** Reducing from 5 to 3 chapters per batch to accommodate character planning XML has not been tested. The impact on total generation time for a volume with 30+ chapters needs estimation during Phase 2 planning.
- **Duplicate DTO resolution strategy:** The two `ChapterPlanXmlDto` classes need either consolidation or synchronized updates. The decision (consolidate vs. dual-update) should be made during Phase 1 planning based on import analysis.

## Sources

### Primary (HIGH confidence)
- **Codebase analysis -- Entity layer:** `NovelChapterPlan.java` (entity fields, missing mappings), `sql/init.sql` line 361-386 (table schema with existing `character_arcs` JSON column)
- **Codebase analysis -- AI pipeline:** `OutlineTaskStrategy.java` (XML parsing at `parseChaptersXml`, save methods at `saveVolumeChaptersToDatabase` and `saveChaptersToDatabase`, batch generation at line 219)
- **Codebase analysis -- Prompt building:** `PromptTemplateBuilder.java` (template variable building at `buildTemplateVariables` line 149, character info at `buildCharacterInfoText` lines 651-718, character list at `buildCharacterPromptInfoList` line 590)
- **Codebase analysis -- Chapter generation:** `ChapterContentGenerateTaskStrategy.java` (generation flow, character extraction call at line 267-268), `ChapterPromptBuilder.java` (legacy prompt builder, `buildChapterPlanInfo` at line 175)
- **Codebase analysis -- Character extraction:** `ChapterCharacterExtractService.java` (extraction flow, name matching at `isSameCharacter` line 489), `CharacterPromptInfo.java` (character prompt DTO)
- **Codebase analysis -- Frontend:** `ChapterPlanDrawer.vue` (drawer component), `types/project.ts` (type definitions, ChapterPlan at line 140), `api/outline.ts` (API layer)
- **Codebase analysis -- Proven patterns:** `WorldviewXmlParser.java` (three-tier name matching at lines 603-660), `XmlParser.java` (Jackson XML limitations)

### Secondary (MEDIUM confidence)
- **Existing architectural decisions** from PROJECT.md: AI name matching convention, DOM XML parsing choice, template versioning workflow
- **Faction/force pitfalls (PITFALLS.md):** Transferable patterns from the v1.0.4 faction refactoring -- name matching, DOM parsing, cascade delete, transient field migration
- **LLM prompt engineering best practices:** Constraint language effectiveness, token budget management, output format compliance

---
*Research completed: 2026-04-07*
*Ready for roadmap: yes*
