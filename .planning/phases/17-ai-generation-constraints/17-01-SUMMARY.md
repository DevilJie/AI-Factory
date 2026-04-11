---
phase: 17-ai-generation-constraints
plan: 01
subsystem: ai-integration
tags: [langchain4j, foreshadowing, prompt-template, mybatis-plus, xml-parsing]

# Dependency graph
requires:
  - phase: 16-ai-chapter-planning
    provides: foreshadowing persistence and chapter planning context injection
provides:
  - foreshadowing constraint injection into chapter generation prompts
  - batch foreshadowing status update after chapter generation
  - SQL migration for llm_chapter_generate_standard template
affects: [18-frontend-chapter-foreshadowing]

# Tech tracking
tech-stack:
  added: []
  patterns: [constraint-injection, dual-path-status-update, lambda-update-wrapper]

key-files:
  created:
    - sql/foreshadowing_generation_template_update.sql
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterContentGenerateTaskStrategy.java

key-decisions:
  - "D-01: directive language style for foreshadowing constraints (must plant / must resolve)"
  - "D-02: constraint text contains only title + description, no type/layout info"
  - "D-03: only current chapter foreshadowing queried via chapter-number filters"
  - "D-04: auto status update pending->in_progress and in_progress->completed after generation"
  - "D-05: re-generation does NOT roll back foreshadowing status"
  - "D-06: foreshadowing constraint pattern mirrors character constraint injection"
  - "D-07: SQL template update as reference migration (manual verification recommended)"

patterns-established:
  - "Chapter-level foreshadowing filter: ForeshadowingQueryDto.plantedChapter / plannedCallbackChapter"
  - "Batch status update: ForeshadowingService.batchUpdateStatusForChapter() with LambdaUpdateWrapper"
  - "Dual-path generation: both ChapterService (streaming) and ChapterContentGenerateTaskStrategy (sync) call status update"

requirements-completed: [AIC-01, AIC-02]

# Metrics
duration: 6min
completed: 2026-04-11
---

# Phase 17 Plan 01: AI Generation Constraints Summary

**Foreshadowing constraint injection into chapter generation prompts with directive-style text, plus automatic status transitions (pending->in_progress->completed) via batch update in both streaming and synchronous generation paths**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-11T09:05:09Z
- **Completed:** 2026-04-11T09:11:09Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- PromptTemplateBuilder now injects foreshadowing constraints (plant + resolve directives) into chapter generation prompts via `foreshadowingConstraint` variable
- ForeshadowingService.batchUpdateStatusForChapter() atomically transitions pending->in_progress and in_progress->completed after chapter generation
- Both generation paths (ChapterService streaming, ChapterContentGenerateTaskStrategy synchronous) call the status update with try-catch protection
- SQL reference migration created for adding {foreshadowingConstraint} to llm_chapter_generate_standard template

## Task Commits

Each task was committed atomically:

1. **Task 1: Add foreshadowing constraint builder + query DTO fields + batch status update method** - `b819c2c` (feat)
2. **Task 2: Wire status updates into both generation paths + create SQL migration** - `74b1bd3` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` - Added ForeshadowingService autowire, hasForeshadowingConstraints(), buildForeshadowingConstraintText(), and foreshadowingConstraint variable injection
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java` - Added plantedChapter and plannedCallbackChapter filter fields
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` - Added chapter-level filters in getForeshadowingList(), added batchUpdateStatusForChapter() with LambdaUpdateWrapper
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java` - Added batchUpdateStatusForChapter() call after memory save in generateChapterByPlan()
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterContentGenerateTaskStrategy.java` - Added ForeshadowingService autowire and batchUpdateStatusForChapter() call after character extraction
- `sql/foreshadowing_generation_template_update.sql` - SQL reference migration for adding {foreshadowingConstraint} to chapter generation template

## Decisions Made
- All decisions D-01 through D-07 from Phase 16 research were followed as specified in the plan
- Used LambdaUpdateWrapper for batch status updates (efficient, single SQL statement per transition)
- Status update failures logged but do not fail chapter generation (per D-05 no-rollback principle)
- Empty string used for foreshadowingConstraint when no constraints exist (not null) so template placeholder is cleanly replaced

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Foreshadowing constraint injection and status updates are complete and compile cleanly
- SQL migration for the template must be applied manually to the database before the constraint variable appears in actual prompts
- Ready for Phase 18 (Frontend Chapter Foreshadowing) which will add the ChapterPlanDrawer foreshadowing management UI

---
*Phase: 17-ai-generation-constraints*
*Completed: 2026-04-11*

## Self-Check: PASSED

All 6 files verified present:
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterContentGenerateTaskStrategy.java
- FOUND: sql/foreshadowing_generation_template_update.sql

Commits verified:
- FOUND: b819c2c (Task 1)
- FOUND: 74b1bd3 (Task 2)
- FOUND: dfbd1da (metadata)
