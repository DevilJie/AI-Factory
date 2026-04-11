---
phase: 16-ai-chapter-planning
plan: 01
subsystem: ai-integration
tags: [langchain4j, foreshadowing, prompt-injection, chapter-planning, mybatis-plus]

# Dependency graph
requires:
  - phase: 15-data-foundation
    provides: novel_foreshadowing volume fields (plantedVolume, plannedCallbackVolume) and ForeshadowingService volume query filters
provides:
  - OutlineTaskStrategy.buildActiveForeshadowingContext() method querying active foreshadowing by volume
  - Foreshadowing context injection into both chapter planning prompt paths (template + hardcoded)
  - Wave 0 test stubs for parseChaptersXml fs/fp tag extraction (for Plan 02)
  - SQL reference migration for llm_outline_chapter_generate template update
affects: [16-02, chapter-generation, prompt-template]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Volume-scoped foreshadowing context: query by plantedVolume/plannedCallbackVolume + status, format as structured list"

key-files:
  created:
    - ai-factory-backend/src/test/java/com/aifactory/service/task/impl/OutlineTaskStrategyTest.java
    - sql/foreshadowing_template_update.sql
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java

key-decisions:
  - "D-01: Structured list format for foreshadowing context showing title, type, layout line, status, plant/callback locations"
  - "D-02: Only inject active foreshadowing for current volume (pending plants + in_progress callbacks), not entire project"
  - "D-07: Inject into both buildChapterPromptUsingTemplate() and buildChapterPrompt() for full coverage"

patterns-established:
  - "Volume-scoped foreshadowing injection: ForeshadowingQueryDto with plantedVolume/plannedCallbackVolume + status filters"

requirements-completed: [AIP-01]

# Metrics
duration: 10min
completed: 2026-04-11
---

# Phase 16 Plan 01: Foreshadowing Context Injection Summary

**buildActiveForeshadowingContext() queries ForeshadowingService for volume-scoped active foreshadowing and injects into both chapter planning prompt paths**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-11T04:40:11Z
- **Completed:** 2026-04-11T04:50:30Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- buildActiveForeshadowingContext() method queries pending plants (status=pending, plantedVolume) and pending callbacks (status=in_progress, plannedCallbackVolume) for current volume
- Foreshadowing context injected into buildChapterPromptUsingTemplate() as {foreshadowingContext} template variable and into buildChapterPrompt() as StringBuilder append
- Wave 0 test stubs created: 3 buildActiveForeshadowingContext tests (all GREEN), 3 parseChaptersXml fs/fp tests (RED, for Plan 02)
- SQL reference migration documents how to update llm_outline_chapter_generate template with {foreshadowingContext} variable and fs/fp output instructions

## Task Commits

Each task was committed atomically:

1. **Task 0: Wave 0 test stubs** - `103517c` (test)
2. **Task 1: buildActiveForeshadowingContext + injection** - `e0eee14` (feat)
3. **Task 2: SQL reference migration** - `f67619a` (chore)

## Files Created/Modified
- `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/OutlineTaskStrategyTest.java` - Wave 0 test stubs for buildActiveForeshadowingContext (3 tests) and parseChaptersXml fs/fp tags (3 tests)
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java` - Added ForeshadowingService injection, buildActiveForeshadowingContext() method, and injection into both prompt paths
- `sql/foreshadowing_template_update.sql` - Reference migration for llm_outline_chapter_generate template update with foreshadowingContext and fs/fp instructions

## Decisions Made
- Per D-01: Used structured list format for foreshadowing context with title, type, layout line, status, and plant/callback location details
- Per D-02: Only queries active foreshadowing relevant to current volume (not entire project) to minimize token usage
- Per D-07: Both planning paths covered -- template path (buildChapterPromptUsingTemplate) and hardcoded path (buildChapterPrompt)
- Template variable {foreshadowingContext} uses Hutool StrUtil.format syntax (not Mustache-style {{}})

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Merged feature/v1.0.6 branch to get Phase 15 volume fields**
- **Found during:** Task 0 (preparation)
- **Issue:** Worktree was behind feature/v1.0.6 -- Foreshadowing entity and DTOs lacked plantedVolume/plannedCallbackVolume fields from Phase 15
- **Fix:** Merged feature/v1.0.6 into worktree branch (fast-forward), bringing in all Phase 15 data foundation work
- **Files modified:** 37 files synced from Phase 15
- **Verification:** Foreshadowing.java, ForeshadowingDto.java, ForeshadowingQueryDto.java all contain volume fields; compilation succeeds

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Merge was necessary to access volume fields added in Phase 15. No scope creep.

## Issues Encounted
- parseChaptersXml fs/fp tag extraction tests (2 tests) are intentionally RED -- these are Wave 0 stubs that will pass once Plan 02 implements the extraction logic. This is expected TDD behavior.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- buildActiveForeshadowingContext() is production-ready and tested (3 GREEN tests)
- Both prompt paths inject foreshadowing context -- the hardcoded path works immediately, the template path requires the DB template to be updated with {foreshadowingContext} variable
- SQL reference migration (sql/foreshadowing_template_update.sql) documents the manual DB update needed for the template path to fully work
- Plan 02 can proceed to implement parseChaptersXml fs/fp tag extraction (tests already written as stubs)

---
*Phase: 16-ai-chapter-planning*
*Completed: 2026-04-11*

## Self-Check: PASSED

- FOUND: ai-factory-backend/src/test/java/com/aifactory/service/task/impl/OutlineTaskStrategyTest.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java
- FOUND: sql/foreshadowing_template_update.sql
- FOUND: .planning/phases/16-ai-chapter-planning/16-01-SUMMARY.md
- FOUND: commit 103517c (test stubs)
- FOUND: commit e0eee14 (implementation)
- FOUND: commit f67619a (SQL migration)
