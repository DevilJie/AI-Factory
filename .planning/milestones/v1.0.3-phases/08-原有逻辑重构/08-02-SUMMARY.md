---
phase: 08-原有逻辑重构
plan: 02
subsystem: api
tags: [orchestrator, strategy-pattern, delegation, xml-parsing, junit5, mockito]

# Dependency graph
requires:
  - phase: 08-01
    provides: WorldviewXmlParser shared DOM parsing, GeographyTaskStrategy and FactionTaskStrategy refactored to delegate
  - phase: 07-api
    provides: GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy independent Strategy beans
provides:
  - WorldviewTaskStrategy 9-step orchestrator (down from 920 to ~250 LOC)
  - Delegation pattern to GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy
  - generate_faction with dependency context injection (geographyContext + powerSystemContext)
  - save_core with worldview_power_system associations and complete result assembly
  - 14 unit tests for 9-step orchestration
affects: [WorldviewController, frontend worldview generation UI]

# Tech tracking
tech-stack:
  added: []
  patterns: [strategy-delegation-orchestrator, step-stub-pattern-for-cross-strategy-calls, dependency-context-injection]

key-files:
  created:
    - ai-factory-backend/src/test/java/com/aifactory/service/task/impl/WorldviewTaskStrategyTest.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java

key-decisions:
  - "generate_geography/generate_power_system combine generate+save into one orchestrator step by calling Strategy executeStep twice with step stubs"
  - "generate_faction builds dependency context (geography + power system) after steps 3+5 have saved data to DB"
  - "createStepStub(String stepType) creates minimal AiTaskStep with only stepType set, because Strategy methods only dispatch on stepType"
  - "XmlParser retained for save_core because slim template only has <t>/<b>/<l>/<r> (no nested same-name tags)"

patterns-established:
  - "Step stub delegation: createStepStub(String) for calling Strategy executeStep from orchestrator"
  - "Dependency context injection: orchestrator builds context from DB after dependent modules are saved, injects into child Strategy config"

requirements-completed: [REFACT-01, REFACT-02]

# Metrics
duration: 5min
completed: 2026-04-03
---

# Phase 08 Plan 02: WorldviewTaskStrategy 9-Step Orchestrator Summary

**Rewrote WorldviewTaskStrategy from 920-line monolith to ~250-line 9-step orchestrator delegating to GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy with dependency context injection and 14 unit tests**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-03T10:18:34Z
- **Completed:** 2026-04-03T10:23:57Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- WorldviewTaskStrategy reduced from ~920 to ~397 lines (including JavaDoc), ~250 LOC of actual code
- Removed all DOM parsing, name matching, type mapping, record definitions (now in WorldviewXmlParser)
- Implemented 9-step pipeline: check_existing, clean/generate geography, clean/generate power_system, clean/generate faction, generate_core, save_core
- check_existing only deletes worldview record + associations (module data cleaning delegated to each Strategy)
- generate_faction builds dependency context (buildGeographyText + buildPowerSystemConstraint) after modules are saved
- save_core creates worldview_power_system associations, re-queries complete data via fillGeography + fillForces
- 14 unit tests covering all 9 steps, delegation patterns, and result assembly

## Task Commits

Each task was committed atomically:

1. **Task 1: Rewrite WorldviewTaskStrategy to 9-step orchestration** - `6798f98` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` - 9-step orchestrator delegating to 3 Strategy beans, core worldview generation via slim template
- `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/WorldviewTaskStrategyTest.java` - 14 unit tests for orchestration, delegation, and result assembly

## Decisions Made
- **Combined generate+save per module**: Each "generate" step in WorldviewTaskStrategy calls Strategy's generate step then save step sequentially, so each module is fully persisted before the next module starts
- **Step stub pattern**: `createStepStub(String)` creates minimal AiTaskStep with only stepType because Strategy methods only dispatch on stepType and use TaskContext for data
- **Dependency context timing**: geographyContext and powerSystemContext are built in generate_faction AFTER steps 3 (geography) and 5 (power_system) have saved data to DB, ensuring context reflects newly generated data
- **XmlParser retained**: Kept for save_core step because the slim llm_worldview_create template only outputs <t>/<b>/<l>/<r> with no nested same-name tags

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- WorldviewTaskStrategy fully refactored, no more DOM parsing in the class
- Phase 08 is complete (both plans 01 and 02 done)
- Front-end can now call the worldview generate-async endpoint and it will internally orchestrate all 9 steps
- Remaining: front-end independent generation buttons (outside Phase 08 scope)

---
*Phase: 08-原有逻辑重构*
*Completed: 2026-04-03*
