---
phase: 15-data-foundation
plan: 01
subsystem: database, api
tags: [mybatis-plus, mysql, validation, foreshadowing, volume]

# Dependency graph
requires: []
provides:
  - "plantedVolume + plannedCallbackVolume fields on Foreshadowing entity and all DTOs"
  - "validateForeshadowingDistance: 3-chapter minimum for bright-line foreshadowing"
  - "validateCallbackChapterBounds: callback chapter cannot exceed volume targetChapterCount"
  - "Volume-based query filters in getForeshadowingList()"
  - "Migration SQL for existing deployments"
affects: [15-02, 16-ai-chapter-planning, 17-ai-generation-constraints]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Volume fields follow existing chapter field naming pattern (plantedVolume mirrors plantedChapter)"
    - "plantedVolume immutable after creation (ForeshadowingUpdateDto only has plannedCallbackVolume)"
    - "Validation methods private in service, called before persistence"

key-files:
  created:
    - sql/foreshadowing_volume_migration.sql
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingCreateDto.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingUpdateDto.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java
    - sql/init.sql

key-decisions:
  - "plantedVolume immutable after creation (only plannedCallbackVolume on UpdateDto) - mirrors plantedChapter pattern"
  - "Dark-line foreshadowing exempt from distance validation per D-07"
  - "Validation skipped when volume plan not found (volume not yet planned)"

patterns-established:
  - "Validation before BeanUtils.copyProperties in createForeshadowing, after field resolution in updateForeshadowing"
  - "Composite indexes on (project_id, volume_column) for query performance"

requirements-completed: [DATA-01, DATA-03]

# Metrics
duration: 5min
completed: 2026-04-11
---

# Phase 15 Plan 01: Foreshadowing Volume Fields Summary

**Added plantedVolume and plannedCallbackVolume fields to foreshadowing entity/DTOs/SQL, with 3-chapter minimum distance validation for bright-line foreshadowing and callback chapter bounds checking against volume plan targetChapterCount.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-10T16:09:30Z
- **Completed:** 2026-04-10T16:14:27Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- Added plantedVolume and plannedCallbackVolume Integer fields to Foreshadowing entity and all 4 DTOs (ForeshadowingDto, ForeshadowingCreateDto, ForeshadowingUpdateDto, ForeshadowingQueryDto)
- Added validateForeshadowingDistance: enforces minimum 3-chapter gap for bright-line foreshadowing, dark-line (layoutType=dark) exempt
- Added validateCallbackChapterBounds: callback chapter cannot exceed the volume's targetChapterCount
- Added volume-based query filters in getForeshadowingList() for plantedVolume and plannedCallbackVolume
- Updated SQL schema with new columns and composite indexes, plus migration SQL file

## Task Commits

Each task was committed atomically:

1. **Task 1: Add volume fields to entity, DTOs, and SQL schema** - `4e5828f` (feat)
2. **Task 2: Add distance validation and volume query filters to ForeshadowingService** - `b0a1234` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java` - Added plantedVolume + plannedCallbackVolume Integer fields
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java` - Added volume fields with @Schema annotations
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingCreateDto.java` - Added both volume fields for creation
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingUpdateDto.java` - Added plannedCallbackVolume only (plantedVolume immutable)
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java` - Added plantedVolume + plannedCallbackVolume filter fields
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` - Added validation methods, NovelVolumePlanMapper injection, volume query filters
- `sql/init.sql` - Added planted_volume, planned_callback_volume columns and 2 composite indexes
- `sql/foreshadowing_volume_migration.sql` - Migration SQL for existing deployments

## Decisions Made
- **plantedVolume immutable after creation**: ForeshadowingUpdateDto only contains plannedCallbackVolume, following the same pattern as plantedChapter which is preserved from existingForeshadowing in updateForeshadowing()
- **Dark-line exemption**: layoutType=dark bypasses distance validation entirely (early return) per D-07
- **Volume plan not found = skip validation**: If no volume plan exists for the specified callback volume, the chapter bounds check is skipped (volume may not be planned yet)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Volume fields ready for use in Phase 15-02 (chapter plan foreshadowing fields removal + LLM foreshadowing output)
- Validation logic ready for Phase 16 (AI chapter planning) and Phase 17 (AI generation constraints)
- Frontend types not yet updated (will be done in Phase 18)

---
*Phase: 15-data-foundation*
*Completed: 2026-04-11*

## Self-Check: PASSED

All 9 files verified present. Both commits (4e5828f, b0a1234) verified in git history. Backend compilation successful.
