---
phase: 17-ai-generation-constraints
verified: 2026-04-11T09:19:45Z
status: passed
score: 5/5 must-haves verified
re_verification: false

human_verification:
  - test: "Generate a chapter with pending foreshadowing for that chapter, verify constraint text appears in the prompt"
    expected: "Prompt contains directive-style foreshadowing constraint text with correct headers"
    why_human: "Requires running the application with test data and inspecting the rendered prompt"
  - test: "Verify LLM actually follows foreshadowing constraint directives in generated chapter text"
    expected: "Generated chapter content naturally includes the specified foreshadowing elements"
    why_human: "Qualitative assessment of LLM output quality requires human judgment"
---

# Phase 17: AI Generation Constraints Verification Report

**Phase Goal:** Chapter generation prompts inject mandatory foreshadowing creative directives, ensuring the LLM plants or resolves foreshadowing per the plan
**Verified:** 2026-04-11T09:19:45Z
**Status:** PASSED
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Chapter generation prompts include "must plant" and "must resolve" foreshadowing constraint sections | VERIFIED | `buildForeshadowingConstraintText()` (lines 705-751) emits section headers matching exact pattern; injected via `variables.put("foreshadowingConstraint", ...)` at line 229 |
| 2 | Constraint text contains only title+description, no type/layout, no non-current-chapter foreshadowing | VERIFIED | Each item appends only `getTitle()` + optional `getDescription()` (lines 720-723); queries filter by `plantedChapter==current` (pending) and `plannedCallbackChapter==current` (in_progress) |
| 3 | No foreshadowing constraints omitted entirely when none exist, no empty headers output | VERIFIED | `buildForeshadowingConstraintText()` returns empty StringBuilder when both lists are empty (line 750 returns `""`); `hasForeshadowingConstraints()` guards the call (line 226) |
| 4 | After successful chapter generation, pending foreshadowing auto-transitions to in_progress, in_progress to completed | VERIFIED | `batchUpdateStatusForChapter()` (ForeshadowingService lines 344-370) performs two LambdaUpdateWrapper batch updates; called from both ChapterService (line 952) and ChapterContentGenerateTaskStrategy (line 280) |
| 5 | Chapter re-generation does not roll back foreshadowing status | VERIFIED | Status updates only escalate forward (pending->in_progress, in_progress->completed); no rollback logic exists anywhere in `batchUpdateStatusForChapter()`; wrapped in try-catch that logs but does not fail generation (lines 951-961, 278-289) |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `PromptTemplateBuilder.java` | hasForeshadowingConstraints(), buildForeshadowingConstraintText(), injection in buildTemplateVariables() | VERIFIED | All three methods present (lines 679-751); injection at line 224-229; @Autowired ForeshadowingService at line 70 |
| `ForeshadowingQueryDto.java` | plantedChapter and plannedCallbackChapter filter fields | VERIFIED | Both fields declared with @Schema annotations (lines 72-78); Lombok @Data generates getters/setters |
| `ForeshadowingService.java` | batchUpdateStatusForChapter() method | VERIFIED | Method at lines 344-370 with @Transactional, LambdaUpdateWrapper for both transitions; getForeshadowingList() also has chapter-level filters (lines 153-160) |
| `ChapterService.java` | foreshadowingService.batchUpdateStatusForChapter() call in generateChapterByPlan() | VERIFIED | Call at line 952 after chapter save and memory save; foreshadowingService already @Autowired at line 110 |
| `ChapterContentGenerateTaskStrategy.java` | ForeshadowingService autowire + batchUpdateStatusForChapter() call | VERIFIED | @Autowired at line 88; call at line 280 after character extraction |
| `sql/foreshadowing_generation_template_update.sql` | SQL migration adding {foreshadowingConstraint} to template | VERIFIED | File exists with 84 lines; documents both manual and REPLACE approaches; references foreshadowingConstraint variable |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| PromptTemplateBuilder.buildTemplateVariables() | ForeshadowingService.getForeshadowingList() | ForeshadowingQueryDto with plantedChapter/plannedCallbackChapter filters | WIRED | Direct call in both hasForeshadowingConstraints() (lines 683-696) and buildForeshadowingConstraintText() (lines 709-734) |
| PromptTemplateBuilder.buildTemplateVariables() | llm_chapter_generate_standard template | variables.put("foreshadowingConstraint", ...) | WIRED | Line 229 puts variable into map; template uses StrUtil.format() substitution |
| ChapterService.generateChapterByPlan() | ForeshadowingService.batchUpdateStatusForChapter() | Post-generation status update after chapterMapper.updateById | WIRED | Line 952, after memory save (line 942), in try-catch block (lines 951-961) |
| ChapterContentGenerateTaskStrategy.generateContent() | ForeshadowingService.batchUpdateStatusForChapter() | Post-generation status update after chapterMapper.updateById | WIRED | Line 280, after character extraction (line 271), in try-catch block (lines 279-289) |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| PromptTemplateBuilder | foreshadowingConstraint | ForeshadowingService.getForeshadowingList() via ForeshadowingQueryDto | YES -- queries novel_foreshadowing table by projectId + plantedChapter/plannedCallbackChapter + status | FLOWING |
| ForeshadowingService.batchUpdateStatusForChapter() | updated count | LambdaUpdateWrapper on novel_foreshadowing | YES -- updates status field and actualCallbackChapter directly in DB | FLOWING |
| ForeshadowingQueryDto | plantedChapter, plannedCallbackChapter | Passed from PromptTemplateBuilder with chapterPlan.getChapterNumber() | YES -- uses global chapter number from chapter plan entity | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Backend compiles without errors | `cd ai-factory-backend && mvn compile -q` | Exit code 0, no output | PASS |
| plantedChapter field exists in DTO (acceptance criteria >= 3 matches) | grep plantedChapter ForeshadowingQueryDto.java | 4 matches (field, @Schema, javadoc) | PASS |
| batchUpdateStatusForChapter present in ForeshadowingService | grep batchUpdateStatusForChapter ForeshadowingService.java | 1 match (method signature) | PASS |
| foreshadowingConstraint variable injection present | grep foreshadowingConstraint PromptTemplateBuilder.java | 3 matches (variable, put, comment) | PASS |
| SQL migration references foreshadowingConstraint | grep foreshadowingConstraint sql file | Multiple matches | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| AIC-01 | 17-01-PLAN | Chapter generation prompt injects foreshadowing constraints -- "must plant" and "must resolve" directives | SATISFIED | buildForeshadowingConstraintText() generates directive-style text with exact headers; injected via variables.put("foreshadowingConstraint") |
| AIC-02 | 17-01-PLAN | Constraint text uses concise format, only current-chapter items injected | SATISFIED | Each item contains only title + description; queries filter by plantedChapter/plannedCallbackChapter == current chapter number |

No orphaned requirements found. REQUIREMENTS.md maps AIC-01 and AIC-02 to Phase 17, and both are claimed by plan 17-01-PLAN.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | - |

No TODO/FIXME/placeholder comments found in modified files. No empty implementations. No hardcoded empty data flows. No stub return patterns in the new code.

### Human Verification Required

1. **Prompt rendering with foreshadowing data**
   **Test:** Create a project with foreshadowing entries (pending status for chapter N, in_progress status for chapter N callback), then generate chapter N and inspect the prompt sent to the LLM.
   **Expected:** The prompt contains directive-style headers and foreshadowing items with title + description only.
   **Why human:** Requires running the application with test data and inspecting the rendered prompt.

2. **LLM compliance with foreshadowing directives**
   **Test:** Generate a chapter with foreshadowing constraints and verify the output text naturally includes the specified foreshadowing elements.
   **Expected:** Generated chapter content includes the foreshadowing elements without feeling forced or out of place.
   **Why human:** Qualitative assessment of LLM output quality requires human judgment.

3. **Database template migration**
   **Test:** Apply the SQL migration to add {foreshadowingConstraint} to the llm_chapter_generate_standard template, then generate a chapter.
   **Expected:** The template placeholder is correctly replaced and does not appear literally in the prompt.
   **Why human:** The SQL migration is a reference document requiring manual database update; its correct application cannot be verified programmatically.

### Gaps Summary

No gaps found. All 5 observable truths verified, all 6 artifacts exist with substantive implementations, all 4 key links are wired correctly, and data flows from database queries through to template variables and status updates.

The SQL migration file is a reference document (not executable SQL) recommending manual database update. This is a deliberate design choice documented in D-07 and acknowledged in the plan. The code compiles and all wiring is correct -- the template variable will work once the database is updated.

---

_Verified: 2026-04-11T09:19:45Z_
_Verifier: Claude (gsd-verifier)_
