---
phase: 15-data-foundation
plan: 02
subsystem: database, api, ui
tags: [mysql, mybatis-plus, vue3, typescript, foreshadowing, schema-cleanup]

# Dependency graph
requires:
  - phase: "15-01"
    provides: "novel_foreshadowing volume fields added, migration file created"
provides:
  - "Complete removal of foreshadowingSetup/foreshadowingPayoff from all layers"
  - "Clean separation between structured foreshadowing table and chapter plan"
  - "Migration SQL for DROP COLUMN"
affects: [16-ai-chapter-planning, 17-ai-generation-constraints, 18-frontend-chapter-foreshadowing]

# Tech tracking
tech-stack:
  added: []
  patterns: [field-removal-across-layers, entity-dto-service-cleanup]

key-files:
  created:
    - sql/foreshadowing_volume_migration.sql
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanUpdateRequest.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java
    - ai-factory-frontend/src/types/project.ts
    - ai-factory-frontend/src/api/chapter.ts
    - ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue
    - ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue
    - ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue
    - sql/init.sql

key-decisions:
  - "DATA-02: Complete hard deletion of foreshadowingSetup/foreshadowingPayoff per D-09, D-10"
  - "Removed foreshadow tab from ChapterPlanDrawer since fields no longer exist"

patterns-established: []

requirements-completed: [DATA-02]

# Metrics
duration: 8min
completed: 2026-04-11
---

# Phase 15 Plan 02: Delete Foreshadowing Text Fields Summary

**Complete removal of redundant foreshadowingSetup/foreshadowingPayoff from Java entity, DTOs, service, frontend types, Vue components, and SQL schema -- zero references remain across entire codebase**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-10T16:20:33Z
- **Completed:** 2026-04-10T16:28:35Z
- **Tasks:** 2
- **Files modified:** 11 (10 modified + 1 created)

## Accomplishments
- Removed foreshadowingSetup and foreshadowingPayoff fields from NovelChapterPlan entity, ChapterPlanDto, and ChapterPlanUpdateRequest
- Cleaned ChapterService of 4 separate field references (plan-to-dto mapping, update handler, second plan-to-dto mapping, prompt builder)
- Removed foreshadowing_setup and foreshadowing_payoff columns from init.sql CREATE TABLE
- Deleted foreshadow fields from all frontend types (Chapter, ChapterPlan, ChapterPlanUpdateRequest)
- Removed foreshadow tab, form fields, and template section from ChapterPlanDrawer.vue
- Cleaned VolumeTree.vue and CreationCenter.vue of field mappings
- Created migration SQL with ALTER TABLE DROP COLUMN statements

## Task Commits

Each task was committed atomically:

1. **Task 1: Delete foreshadowingSetup/foreshadowingPayoff from backend** - `85469b8` (feat)
2. **Task 2: Delete foreshadowingSetup/foreshadowingPayoff from frontend** - `28aca1b` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` - Removed 2 fields (foreshadowingSetup, foreshadowingPayoff)
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` - Removed 2 fields with @Schema annotations
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanUpdateRequest.java` - Removed 2 fields with @Schema annotations
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java` - Removed 4 locations: dto mapping (2x), update handler (2 if-blocks), prompt builder (2 if-blocks)
- `ai-factory-frontend/src/types/project.ts` - Removed from Chapter and ChapterPlan interfaces
- `ai-factory-frontend/src/api/chapter.ts` - Removed from ChapterPlanUpdateRequest interface
- `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue` - Removed foreshadow tab, form fields, mappings, and template section
- `ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue` - Removed 2 field mappings
- `ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue` - Removed 2 field mappings
- `sql/init.sql` - Removed foreshadowing_setup and foreshadowing_payoff columns from novel_chapter_plan CREATE TABLE
- `sql/foreshadowing_volume_migration.sql` - Created with ALTER TABLE DROP COLUMN statements

## Decisions Made
- Removed the "伏笔管理" tab from ChapterPlanDrawer section navigation since the text fields it managed no longer exist. The foreshadowing management will be replaced by structured UI in Phase 18.
- Backend compiles cleanly after removal; all DTO field mappings updated consistently.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- node_modules not installed in worktree (parallel execution environment) -- TypeScript check could not run, but zero grep references confirm clean removal.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All foreshadowingSetup/foreshadowingPayoff references purged from codebase
- Backend compiles, frontend has zero dangling references
- Ready for Phase 16 (AI Chapter Planning) which will use the structured novel_foreshadowing table instead
- Migration SQL ready for DBA execution alongside 15-01's ADD COLUMN migration

## Self-Check: PASSED

All 12 files verified present. Both task commits (85469b8, 28aca1b) verified in git history.

---
*Phase: 15-data-foundation*
*Completed: 2026-04-11*
