---
phase: 07-api
plan: 02
subsystem: api
tags: [spring-boot, rest-api, task-strategy, async-generation, dependency-injection, dom-parsing, xml]

# Dependency graph
requires:
  - phase: 06-独立提示词模板
    provides: llm_faction_create prompt template with geographyContext/powerSystemContext variables
  - phase: 02-faction-crud (v1.0.2)
    provides: FactionService (saveTree, deleteByProjectId)
  - phase: 03-ai-migration (v1.0.2)
    provides: DOM parsing methods, three-tier name matching
provides:
  - FactionTaskStrategy (3-step clean/generate/save pipeline for independent faction generation)
  - 3 REST endpoints (generate-geography, generate-power-system, generate-faction)
  - Synchronous dependency validation for faction generation (geography + power system)
  - Context injection pattern (controller injects dependency text into task config)
affects: [frontend-independent-buttons, worldview-composition-refactor]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Controller-level dependency validation with context injection into async task config"
    - "Strategy pattern with config-passed context (geographyContext/powerSystemContext from controller, not DB queries in strategy)"

key-files:
  created:
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java

key-decisions:
  - "Controller injects dependency context (geography/power system text) into task config, strategy reads from config - avoids DB queries in strategy"
  - "Faction endpoint validates both geography and power system dependencies synchronously before task creation"
  - "Geography and power system endpoints have no dependency checks (per D-07)"

patterns-established:
  - "Independent generation endpoint pattern: validate project -> build config -> createTask -> return taskId"
  - "Dependency validation pattern: sync check in controller, context injection into config, strategy reads from config"

requirements-completed: [API-01, API-02, API-03, DEP-01, DEP-02, DEP-03]

# Metrics
duration: 9min
completed: 2026-04-03
---

# Phase 07 Plan 02: FactionTaskStrategy and Independent Generation Endpoints Summary

**FactionTaskStrategy with 3-step async pipeline (clean/generate/save) plus 3 REST endpoints for independent geography, power system, and faction generation with dependency validation**

## Performance

- **Duration:** 9 min
- **Started:** 2026-04-03T04:57:07Z
- **Completed:** 2026-04-03T05:06:26Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Created FactionTaskStrategy with full DOM parsing pipeline (copied from WorldviewTaskStrategy), including two-pass insert with faction-region and faction-relation associations
- Added 3 REST endpoints to WorldviewController: generate-geography (no deps), generate-power-system (no deps), generate-faction (with geography + power system dependency validation and context injection)
- Faction endpoint validates dependencies synchronously and injects geographyContext/powerSystemContext into task config for the strategy to consume

## Task Commits

Each task was committed atomically:

1. **Task 1: Create FactionTaskStrategy** - `e5f3d4d` (feat)
2. **Task 2: Add 3 independent generation endpoints to WorldviewController** - `492349e` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java` - FactionTaskStrategy with 3 steps: clean_faction (deleteByProjectId), generate_faction (llm_faction_create template + context from config), save_faction (DOM parse <f> XML, two-pass insert)
- `ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java` - Added ContinentRegionService/PowerSystemService autowiring, 3 new POST endpoints (generate-geography, generate-power-system, generate-faction)

## Decisions Made
- Controller injects dependency context into task config rather than strategy querying DB directly - keeps strategy stateless and testable, matches existing task pattern
- Faction endpoint validates both geography and power system dependencies synchronously (returns error immediately if missing) rather than failing async
- Geography and power system endpoints have no dependency checks per design (D-07) - they are root-level generators

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Worktree was based on an older commit (56634a6) that predated FactionService and mapper files - resolved by merging feature/v1.0.2 into worktree branch via fast-forward

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- FactionTaskStrategy ready for task routing (needs registration with TaskService or auto-discovery via @Component)
- 3 REST endpoints ready for frontend integration (plan 07-03 or later will add UI buttons)
- Geography and power system endpoints depend on their respective TaskStrategies being created (plan 07-01)

## Self-Check: PASSED

- FOUND: FactionTaskStrategy.java (worktree)
- FOUND: WorldviewController.java (worktree)
- FOUND: 07-02-SUMMARY.md (main repo)
- FOUND: commit e5f3d4d (Task 1)
- FOUND: commit 492349e (Task 2)

---
*Phase: 07-api*
*Completed: 2026-04-03*
