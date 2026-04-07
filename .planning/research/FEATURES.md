# Feature Research: Chapter Character Planning System

**Domain:** Chapter-level character planning and enforcement for AI novel generation
**Researched:** 2026-04-07
**Confidence:** HIGH (based on thorough codebase analysis of existing planning, generation, and extraction pipelines)

## Executive Summary

The current AI novel generation pipeline operates in an open-loop for character management: the AI generates chapter plans (with no explicit character assignments), then generates chapter content (injecting all non-NPC characters as context), and finally extracts characters post-generation into `novel_character_chapter` records. This creates three problems: (1) the AI decides character appearances ad-hoc with no pre-planning, leading to inconsistent screen time; (2) the generation prompt includes ALL non-NPC characters regardless of relevance, wasting token budget and confusing the model; (3) there is no way for users to control or predict which characters appear in which chapters.

The chapter character planning system closes this loop by adding a "plan characters -> generate by plan -> extract and verify" cycle. The key insight is that the `novel_chapter_plan` table already has an unused `character_arcs` JSON column, the `ChapterGenerationTaskStrategy` already calls `buildCharacterPromptInfoList()` during planning, and the `PromptTemplateBuilder` already has `buildCharacterInfoText()`. The changes are primarily about threading planned character data through existing pipelines rather than building new infrastructure.

## Current State Analysis

### What Exists

| Component | Current Behavior | Gap |
|-----------|------------------|-----|
| `ChapterGenerationTaskStrategy` | Generates chapter plans with XML output containing n/t/p/e/g/w/s/f fields. No character planning fields. | No `<characters>` section in XML output or prompt. |
| `ChapterPlanXmlDto` (two copies: `common.xml` and `dto`) | Parses XML into `ChapterPlanItem` with 8 fields. | No character-related fields in DTO. |
| `NovelChapterPlan` entity | Has `plotOutline`, `keyEvents`, `chapterGoal`, etc. DB already has `character_arcs` JSON column. | `character_arcs` column unused in entity and service. No `planned_characters` field. |
| `ChapterContentGenerateTaskStrategy` | Builds prompt via `PromptTemplateBuilder`, generates chapter, then calls `ChapterCharacterExtractService.extractCharacters()`. | Injects ALL non-NPC characters via `buildCharacterPromptInfoList()`. No filtering by plan. |
| `PromptTemplateBuilder` | `buildCharacterPromptInfoList()` gets all non-NPC characters + last appearance state. `buildCharacterInfoText()` formats them for the prompt. | No awareness of planned characters. Cannot constrain to "only these characters should appear." |
| `ChapterCharacterExtractService` | Post-generation: calls LLM to extract character info from chapter content, matches/creates characters, saves to `novel_character_chapter`. | No cross-reference against planned characters. No validation that planned characters actually appeared. |
| `ChapterPlanDrawer.vue` | Shows chapter plan details in a drawer: chapter number, title, summary, key events, characters (comma-separated text input), foreshadowing. | "Characters" field is a free-text comma-separated string (`newCharacters`), not linked to `novel_character` table. |
| `CreationCenter.vue` | Sets `chapter.newCharacters` as comma-separated string into the editor store. | No structured character display or planning UI. |

### Key Data Points

- `novel_chapter_plan` table has `character_arcs` JSON column (already in DB schema, not used in Java entity).
- `novel_chapter_plan` table does NOT have `planned_characters` -- this needs adding.
- `ChapterPlanXmlDto` exists in two packages: `com.aifactory.common.xml` (used by `ChapterGenerationTaskStrategy`) and `com.aifactory.dto` (appears older/alternative). Only the `common.xml` version is actively used.
- The prompt template code `llm_outline_chapter_generate` controls what the AI outputs for chapter planning.
- The prompt template code `llm_chapter_generate_standard` controls how characters are injected during chapter generation.
- Character extraction template `llm_chapter_character_extract` already handles post-generation extraction.

## Table Stakes (Must Have)

Features that are essential to close the "plan -> generate -> verify" character loop. Missing any of these means the system still operates open-loop.

| # | Feature | Why Expected | Complexity | Trigger (AI vs User) | Notes |
|---|---------|--------------|------------|----------------------|-------|
| T-01 | Planned characters field in chapter plan | The entire feature premise. Each chapter plan must record which characters are expected to appear and what roles they play. Without this, there is nothing to enforce. | LOW | AI (during planning) + User (manual override) | DB: add `planned_characters` JSON column to `novel_chapter_plan`. Entity: add field. DTO: add field. The existing `character_arcs` JSON column can store arc-level info per character. |
| T-02 | AI generates character assignments during chapter planning | The AI already generates plot outlines per chapter. It should also decide which characters appear and what they do, based on the volume's character roster and story needs. | MEDIUM | AI | Modify `llm_outline_chapter_generate` prompt template to request `<characters>` section in XML output. Modify `ChapterGenerationTaskStrategy` to inject existing character list into planning prompt. |
| T-03 | Parse character planning from XML output | The AI's chapter plan XML must be parsed to extract character assignments and persisted to `planned_characters` JSON. | MEDIUM | AI (parsing) | Extend `ChapterPlanXmlDto.Chapter` to include character list. Add parsing in `ChapterGenerationTaskStrategy.saveChaptersToDatabase()`. Must handle AI outputting character names (not IDs) and resolve them. |
| T-04 | Inject planned characters into chapter generation prompt | When generating chapter content, the system should inject ONLY the planned characters (with their planned roles/arcs), not ALL non-NPC characters. This is the enforcement mechanism. | MEDIUM | AI | Modify `ChapterContentGenerateTaskStrategy` to read `planned_characters` from the chapter plan. Modify `PromptTemplateBuilder` to accept planned character constraints. Add a "character plan" section to the prompt telling the AI exactly which characters must appear and what they should do. |
| T-05 | Frontend display of planned characters in chapter plan | Users need to see which characters are planned for each chapter. This is basic visibility. | LOW | User (viewing) | Modify `ChapterPlanDrawer.vue` to show planned character list (read from plan data). Can use simple card/list layout. No editing needed for v1 -- AI plans, user views. |
| T-06 | Cross-reference extracted characters against plan | After chapter generation + extraction, compare extracted characters with planned characters. If a planned character was not extracted, flag it. | LOW | AI (automated) | Post-extraction validation step in `ChapterContentGenerateTaskStrategy`. Compare `extractCharacters()` output with `planned_characters`. Log warnings, do not block. |

## Differentiators (Competitive Advantage)

Features that go beyond the basic loop and provide meaningful user control over character narrative.

| # | Feature | Value Proposition | Complexity | Trigger (AI vs User) | Notes |
|---|---------|-------------------|------------|----------------------|-------|
| D-01 | Manual character planning override | Users can manually edit the planned character list before generation. This gives authors creative control -- they can add/remove characters and adjust arcs without re-running AI planning. | MEDIUM | User | Requires character picker UI (multi-select from project's character list) + arc text input per character. Save back to `planned_characters` JSON. |
| D-02 | Character arc tracking per chapter plan | Each planned character can have a "role in this chapter" description (e.g., "Li Yun discovers the spy's identity" or "Chen Feng trains new technique"). This gives the AI more specific guidance than just a name. | LOW | AI (auto-generate) + User (edit) | Use the existing `character_arcs` JSON column. Schema: `[{"characterId": 1, "characterName": "...", "role": "...", "arc": "..."}]`. |
| D-03 | Character appearance density analysis | Show users how many chapters each character is planned for vs. actually appeared in. Helps identify overused or underused characters. | MEDIUM | User (viewing) | Aggregate query across `planned_characters` (planned) and `novel_character_chapter` (actual). Display as a simple table or chart in a future reporting view. |
| D-04 | Re-planning triggered by character changes | When a user edits a character's fundamental info (name, role type), offer to re-plan affected chapters where that character was planned. | HIGH | User (trigger) | Complex dependency chain. Requires knowing which plans reference which characters, then re-running AI planning for those chapters. Defer to future. |

## Anti-Features (Explicitly Do NOT Build)

Features that seem natural but would overcomplicate the system or contradict the project's AI-first philosophy.

| Anti-Feature | Why It Seems Appealing | Why We Should NOT Build It | What To Do Instead |
|-------------|----------------------|---------------------------|-------------------|
| Hard enforcement (block generation if planned characters not in content) | "Ensure the AI follows the plan exactly" | AI-generated text is probabilistic. Hard enforcement would cause infinite regeneration loops. The plan is guidance, not a contract. | Soft enforcement: inject planned characters strongly in the prompt, log post-generation mismatches, let users decide whether to re-generate. |
| Per-character screen time / word count quotas | "Ensure balanced character appearances" | This is a screenplay/analytics feature, not a novel planning feature. Tracking word count per character in generated text requires NLP parsing and is unreliable. | Let the AI naturally balance appearances based on the planned roles. Provide visibility (D-03) but not quotas. |
| Character relationship graph per chapter | "Visualize character interactions per chapter" | Requires a graph rendering library and complex interaction data extraction. The ROI for writing (vs. TTRPG management) is low. | Simple list display. Relationship data already exists in `novel_faction_relation` and `novel_faction_character` for faction-based relationships. |
| Automatic re-generation when planned vs. actual mismatch | "If a planned character doesn't appear, auto-re-generate" | Creates unpredictable generation loops and cost. A missing character might be intentionally omitted by the AI for narrative reasons. | Warn the user. Let them manually trigger re-generation if desired. |
| Character scheduling across chapters (timeline view) | "Gantt chart of character appearances across chapters" | Over-engineered for current scope. Requires a full timeline/gantt component that doesn't exist in the frontend stack. | Simple table view of planned characters per chapter. Timeline visualization is a v2+ feature. |
| Mandatory character planning before generation | "Users must plan characters before any chapter can be generated" | This would break the existing workflow for users who don't care about character planning. It also blocks users who want to let the AI decide. | Make character planning opt-in: if `planned_characters` is null/empty, fall back to current behavior (inject all non-NPC characters). |

## Feature Dependency Graph

```
[T-01: planned_characters field in DB/entity]
    |
    +---> [T-02: AI generates character assignments during planning]
    |         +--requires--> [Existing character list query] (already exists in ChapterCharacterExtractService)
    |         +--requires--> [Prompt template update: llm_outline_chapter_generate]
    |
    +---> [T-03: Parse character planning from XML]
    |         +--requires--> [T-02] (XML output must contain characters)
    |         +--requires--> [ChapterPlanXmlDto extension]
    |         +--requires--> [Character name-to-ID resolution] (same pattern as faction name matching)
    |
    +---> [T-04: Inject planned characters into generation prompt]
    |         +--requires--> [T-01] (data must exist in chapter plan)
    |         +--requires--> [PromptTemplateBuilder modification]
    |         +--requires--> [Prompt template update: llm_chapter_generate_standard]
    |         +--degrades-gracefully--> [Current behavior: inject all non-NPC characters]
    |
    +---> [T-05: Frontend display]
    |         +--requires--> [T-01] (data in API response)
    |         +--requires--> [ChapterPlanDrawer.vue modification]
    |
    +---> [T-06: Cross-reference extraction vs plan]
    |         +--requires--> [T-01] (plan data)
    |         +--requires--> [Existing extraction pipeline] (ChapterCharacterExtractService)
              +--runs-after--> [Chapter content generation + character extraction]

[D-01: Manual override]
    +--requires--> [T-01]
    +--requires--> [Character picker UI component]

[D-02: Character arc per chapter]
    +--requires--> [T-01]
    +--uses--> [Existing character_arcs JSON column]

[D-03: Appearance density analysis]
    +--requires--> [T-01] (planned data)
    +--requires--> [novel_character_chapter table] (actual data, already exists)
```

### Critical Dependency Notes

1. **T-01 is the foundation.** Everything depends on having `planned_characters` in the database and entity. This is a single ALTER TABLE + entity field addition. The existing `character_arcs` JSON column can store the arc/role details per character.

2. **T-04 degrades gracefully.** If `planned_characters` is null or empty (existing plans, or plans where AI did not output characters), the system falls back to the current behavior of injecting all non-NPC characters. This means the feature is backward-compatible and can be rolled out incrementally.

3. **Name resolution follows the established pattern.** The AI outputs character names (not IDs). The backend must resolve names to IDs using the same approach as faction/region/power system name matching -- exact match first, then fuzzy match. The `NovelCharacterService` already has `getNonNpcCharacters()` which returns all characters for matching.

4. **Two ChapterPlanXmlDto classes exist.** `com.aifactory.common.xml.ChapterPlanXmlDto` (used by `ChapterGenerationTaskStrategy`) and `com.aifactory.dto.ChapterPlanXmlDto` (used elsewhere). Both need updating, or consolidate to one.

5. **The planning prompt needs character context.** Currently `ChapterGenerationTaskStrategy.buildChapterPromptUsingTemplate()` sets `variables.put("characterInfo", "No characters")`. This must change to inject the project's character roster so the AI can assign characters to chapters.

## MVP Recommendation

### Phase 1: Data Foundation (T-01 + T-03 partial)
The bare minimum to make the data flow work end-to-end.

1. Add `planned_characters` JSON column to `novel_chapter_plan` table.
2. Add `plannedCharacters` field to `NovelChapterPlan` entity.
3. Extend `ChapterPlanXmlDto.Chapter` with character list fields.
4. Update `ChapterPlanDto` to include planned characters.

**Rationale:** Cannot do anything without the data layer. This is a 2-3 hour task with minimal risk.

### Phase 2: AI Planning Output (T-02 + T-03)
Make the AI produce character assignments during chapter planning.

1. Update `llm_outline_chapter_generate` prompt template to request character planning.
2. Inject character roster into `ChapterGenerationTaskStrategy`'s planning prompt.
3. Parse character XML and persist to `planned_characters` JSON.
4. Handle name-to-ID resolution with the existing character list.

**Rationale:** This is the core AI integration. The planning prompt must be carefully designed to output character data in a parseable XML format without breaking existing chapter plan output.

### Phase 3: Generation Enforcement (T-04)
Make chapter generation respect planned characters.

1. Modify `ChapterContentGenerateTaskStrategy` to read `planned_characters` from the plan.
2. Modify `PromptTemplateBuilder.buildCharacterInfoText()` to accept planned character constraints.
3. Update `llm_chapter_generate_standard` template to include character plan section.
4. Add fallback logic: if no planned characters, use current behavior.

**Rationale:** This is the enforcement mechanism. The prompt must strongly instruct the AI to include ONLY the planned characters (with their specified roles/arcs) and not introduce unplanned characters.

### Phase 4: Frontend + Verification (T-05 + T-06)
Close the loop with visibility and verification.

1. Update `ChapterPlanDrawer.vue` to display planned characters.
2. Update API responses to include planned character data.
3. Add post-extraction comparison in `ChapterContentGenerateTaskStrategy`.
4. Log warnings when planned characters are missing from extraction results.

**Rationale:** Frontend is straightforward once the data flows. Verification is a logging concern, not a blocking check.

### Phase 5: Manual Override (D-01 + D-02)
Give users control after the automated system works.

1. Add character picker UI to chapter plan drawer.
2. Allow editing planned character roles/arcs.
3. Save manual changes back to `planned_characters` JSON.

**Rationale:** Manual override only makes sense after users can see what the AI planned. Building it first would mean editing empty data.

## Feature Complexity Estimates

| Feature | Backend LOC | Frontend LOC | DB Changes | AI Prompt Changes | Total Effort |
|---------|------------|-------------|------------|-------------------|-------------|
| T-01: DB field + entity | ~20 | 0 | 1 ALTER TABLE | 0 | 0.5 day |
| T-02: AI planning output | ~50 | 0 | 0 | 1 template (moderate) | 1 day |
| T-03: XML parsing + persistence | ~80 | 0 | 0 | 0 | 1 day |
| T-04: Generation enforcement | ~60 | 0 | 0 | 1 template (moderate) | 1 day |
| T-05: Frontend display | ~10 | ~80 | 0 | 0 | 0.5 day |
| T-06: Cross-reference check | ~30 | 0 | 0 | 0 | 0.5 day |
| D-01: Manual override | ~30 | ~120 | 0 | 0 | 1 day |
| D-02: Character arc editing | ~20 | ~60 | 0 | 0 | 0.5 day |
| **Total MVP (T-01 through T-06)** | **~250** | **~80** | **1 migration** | **2 templates** | **~4.5 days** |
| **Total with D-01 + D-02** | **~300** | **~260** | **0** | **0** | **~6 days** |

## Planned Characters Data Schema

The `planned_characters` JSON column should store structured data that serves both AI planning and frontend display:

```json
[
  {
    "characterId": 42,
    "characterName": "Li Yun",
    "roleInChapter": "protagonist",
    "plannedArc": "Discovers the spy within the sect and confronts them",
    "plannedBehavior": "Investigates suspicious activities, confronts Elder Chen",
    "plannedEmotion": "Suspicion -> Anger -> Determination"
  },
  {
    "characterId": 15,
    "characterName": "Chen Feng",
    "roleInChapter": "supporting",
    "plannedArc": "Trains new technique and demonstrates growth",
    "plannedBehavior": "Spars with senior disciple, reveals improved swordsmanship",
    "plannedEmotion": "Frustration -> Breakthrough -> Pride"
  }
]
```

The `characterId` may be null for characters the AI names but that do not yet exist in the database (new characters introduced in this chapter). The `characterName` always serves as the display/lookup key.

## AI Operation vs User Operation Classification

| Operation | Actor | Automated? | Notes |
|-----------|-------|-----------|-------|
| Generate character assignments during chapter planning | AI | Yes | Part of existing `ChapterGenerationTaskStrategy`. Runs automatically when user clicks "generate chapter plans." |
| Parse and persist character planning data | System | Yes | Post-AI-response parsing. Transparent to user. |
| Inject planned characters into generation prompt | System | Yes | Transparent to user. System reads plan, builds prompt section. |
| Post-generation extraction (existing) | AI | Yes | `ChapterCharacterExtractService.extractCharacters()`. Already runs after chapter generation. |
| Cross-reference plan vs extraction | System | Yes | Automated comparison. Logs warnings. User sees nothing unless they check logs. |
| View planned characters in chapter plan | User | No | User opens ChapterPlanDrawer to see what was planned. |
| Override planned characters (D-01) | User | No | User manually adds/removes characters from the plan before generation. |
| Edit character arc in plan (D-02) | User | No | User adjusts the planned arc description for a character. |
| Trigger re-generation | User | No | User decides whether to re-generate a chapter based on mismatch warnings. |

## Sources

- **Codebase analysis:** `NovelChapterPlan.java`, `ChapterPlanXmlDto.java` (both copies), `ChapterGenerationTaskStrategy.java`, `ChapterContentGenerateTaskStrategy.java`, `PromptTemplateBuilder.java`, `ChapterCharacterExtractService.java`, `NovelCharacterChapterService.java`, `ChapterPlanDrawer.vue`, `CreationCenter.vue`, `sql/init.sql`
- **DB schema:** `novel_chapter_plan` table definition (already has `character_arcs` JSON column)
- **PROJECT.md:** Milestone v1.0.5 scope and out-of-scope items
- **Existing patterns:** Faction name-to-ID resolution, geography tree parsing, DOM XML parsing, prompt template system

---
*Feature research for: chapter character planning system in AI novel generation*
*Researched: 2026-04-07*
