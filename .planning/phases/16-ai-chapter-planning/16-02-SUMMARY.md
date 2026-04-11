---
phase: 16-ai-chapter-planning
plan: 02
subsystem: ai-integration
tags: [langchain4j, foreshadowing, xml-parsing, mybatis-plus, regex]

# Dependency graph
requires:
  - phase: 16-01
    provides: OutlineTaskStrategy.buildActiveForeshadowingContext(), ForeshadowingService, ForeshadowingCreateDto, test stubs for parseChaptersXml fs/fp
provides:
  - OutlineTaskStrategy.extractForeshadowingPlants() and extractForeshadowingPayoffs() regex extraction methods
  - Extended parseChaptersXml() with <fs>/<fp> tag extraction and prefixed key storage in chapterData maps
  - ForeshadowingService.deletePendingForeshadowingForVolume() for replan delete-before-create
  - Foreshadowing persistence in saveVolumeChaptersToDatabase() and saveChaptersToDatabase()
  - parseIntSafe() helper for optional fc/fr integer fields
affects: [16-03, chapter-generation, prompt-template]

# Tech tracking
tech-stack:
  added: []
patterns:
  - "Flat-map foreshadowing storage: _fs_N_field and _fp_N_field prefixed keys in Map<String,String> to avoid return type changes"
  - "Delete-before-create replan pattern: deletePendingForeshadowingForVolume() before iterating parsed <fs> data"

key-files:
  created: []
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java

key-decisions:
  - "D-03: <fs> has sub-tags ft/fy/fl/fd/fc/fr; <fp> has sub-tags ft/fd"
  - "D-04: Multiple <fs> tags per chapter allowed, stored with index-prefixed keys"
  - "D-05: <fp> data parsed but NOT used to update existing foreshadowing status (Phase 17)"
  - "D-06: deletePendingForeshadowingForVolume only filters by plantedVolume + status=pending, NOT plannedCallbackVolume"
  - "Per-Pitfall-6: parseIntSafe() wraps fc/fr parsing to handle LLM omitting optional sub-tags"

patterns-established:
  - "Individual foreshadowing createForeshadowing() wrapped in try-catch per item so one bad entry does not abort chapter save"

requirements-completed: [AIP-02, AIP-03]

# Metrics
duration: 5min
completed: 2026-04-11
---

# Phase 16 Plan 02: Foreshadowing XML Parsing and Persistence Summary

**parseChaptersXml() extracts <fs>/<fp> foreshadowing tags via regex and both save methods persist parsed plant data as novel_foreshadowing records with delete-before-create replan safety**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-11T04:58:27Z
- **Completed:** 2026-04-11T05:04:13Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- extractForeshadowingPlants() and extractForeshadowingPayoffs() regex methods extract <fs>/<fp> tags from chapter XML content
- parseChaptersXml() now calls both methods and stores foreshadowing data in chapterData maps with _fs_N_field / _fp_N_field prefixed keys
- ForeshadowingService.deletePendingForeshadowingForVolume() deletes only pending-status foreshadowing matching the volume (D-06 compliant)
- Both saveVolumeChaptersToDatabase() and saveChaptersToDatabase() delete pending foreshadowing then create new records from parsed <fs> data
- All 6 OutlineTaskStrategyTest tests pass (3 buildActiveForeshadowingContext + 3 parseChaptersXml fs/fp)

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend parseChaptersXml() with <fs>/<fp> extraction and add deletePendingForeshadowingForVolume()** - `7b4e70d` (feat)
2. **Task 2: Add foreshadowing persistence logic to both save methods** - `6ba4b31` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java` - Added extractForeshadowingPlants(), extractForeshadowingPayoffs(), parseIntSafe(); extended parseChaptersXml() with fs/fp extraction; added foreshadowing persistence to both save methods
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` - Added deletePendingForeshadowingForVolume() method

## Decisions Made
- Per D-03: <fs> sub-tags are ft (title), fy (type), fl (layout), fd (description), fc (callback volume), fr (callback chapter); <fp> sub-tags are ft (title), fd (description)
- Per D-04: Multiple <fs> per chapter stored with indexed keys (_fs_0_ft, _fs_1_ft, etc.)
- Per D-05: <fp> data is parsed and stored but NOT used for status updates -- payoff processing deferred to Phase 17
- Per D-06: delete only targets plantedVolume=currentVolume AND status=pending, never plannedCallbackVolume
- Per Pitfall 6: Optional fc/fr integer fields wrapped in parseIntSafe() to handle LLM omitting them
- Individual foreshadowing createForeshadowing() calls wrapped in per-item try-catch to prevent one bad entry from aborting the entire chapter save

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Foreshadowing parsing and persistence fully implemented for both save paths
- <fp> payoff data is parsed but not consumed -- Phase 17 can use it to match against existing foreshadowing by title
- The DB template (llm_outline_chapter_generate) still needs manual update with {foreshadowingContext} variable (reference migration in sql/foreshadowing_template_update.sql)

---
*Phase: 16-ai-chapter-planning*
*Completed: 2026-04-11*

## Self-Check: PASSED

- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java
- FOUND: .planning/phases/16-ai-chapter-planning/16-02-SUMMARY.md
- FOUND: commit 7b4e70d (Task 1)
- FOUND: commit 6ba4b31 (Task 2)
