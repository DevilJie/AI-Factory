---
phase: 13-章节生成约束注入
plan: 01
subsystem: ai-prompt
tags: [langchain4j, prompt-engineering, character-constraints, tdd]

# Dependency graph
requires:
  - phase: 11-数据基础-实体映射
    provides: NovelChapterPlan.plannedCharacters JSON field
  - phase: 12-AI规划输出-XML解析
    provides: Planned character data parsed from AI chapter plan output
provides:
  - Conditional character injection in buildTemplateVariables (planned vs full list)
  - buildPlannedCharacterInfoText constraint text generation method
  - hasPlannedCharacters guard method
  - 6 unit tests covering planned character injection and fallback paths
affects: [chapter-generation, prompt-template, character-constraints]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Conditional prompt variable injection: planned data takes priority, fallback to full-list when absent"
    - "Constraint language pattern: opening constraint + character list + NPC allowance + closing reminder"

key-files:
  created:
    - ai-factory-backend/src/test/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilderTest.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java

key-decisions:
  - "parse failure returns null triggering fallback to full character list (graceful degradation)"
  - "Test uses @Spy ObjectMapper for real JSON parsing instead of mocking deserialization"
  - "setupPlannedCharacterMocks separated from setupFallbackMocks to avoid unnecessary mock setup"

patterns-established:
  - "Planned character constraint text: segment header with mandatory language, per-character format 'name (roleType) - description [importance]', NPC allowance footer, closing reminder"

requirements-completed: [CG-01, CG-02]

# Metrics
duration: 2min
completed: 2026-04-09
---

# Phase 13 Plan 01: 章节生成约束注入 Summary

**Conditional planned character constraint injection in chapter prompt with fallback to full character list, constraint language wrapping, and 6 TDD tests**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-09T03:30:25Z
- **Completed:** 2026-04-09T03:32:30Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Chapter prompt now injects planned character constraints when NovelChapterPlan has plannedCharacters JSON, replacing the full character list
- Constraint text includes opening mandatory language, per-character formatted lines, NPC allowance sentence, and closing reminder
- Graceful fallback to full character list when plannedCharacters is null, empty, or JSON parse fails
- All 6 unit tests pass covering injection, fallback (null and empty array), constraint language, NPC allowance, and text format

## Task Commits

Each task was committed atomically:

1. **Task 1: Write tests for planned character injection and constraint language (RED)** - `d44b606` (test)
2. **Task 2: Implement planned character constraint injection in PromptTemplateBuilder (GREEN)** - `1729db9` (feat)

_Note: TDD flow -- RED commit adds failing tests, GREEN commit adds implementation + fixes test mocks to pass_

## Files Created/Modified
- `ai-factory-backend/src/test/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilderTest.java` - 6 unit tests for planned character injection and fallback paths
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` - Added hasPlannedCharacters() guard and buildPlannedCharacterInfoText() constraint method, modified buildTemplateVariables() with conditional branch

## Decisions Made
- Parse failure in buildPlannedCharacterInfoText returns null (not empty string), which triggers the full character list fallback -- this ensures graceful degradation if the JSON schema changes
- Test uses @Spy ObjectMapper with real instance rather than @Mock to enable actual JSON deserialization in tests
- setupPlannedCharacterMocks separated from setupFallbackMocks because planned path tests do not need NovelCharacterService or NovelCharacterChapterService mocks

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Pre-existing test failure in ChapterGenerationTaskStrategyTest.testResolveCharacterIdsRoleType (expects 'supporting' but gets 'minor') -- unrelated to this plan, logged as deferred

## Next Phase Readiness
- PromptTemplateBuilder ready for real LLM testing with planned characters
- Constraint language effectiveness should be validated with actual chapter generation
- Frontend chapter plan display for planned characters is the next planned feature

## Self-Check: PASSED
- PromptTemplateBuilderTest.java: FOUND
- PromptTemplateBuilder.java: FOUND
- 13-01-SUMMARY.md: FOUND
- d44b606 (RED commit): FOUND
- 1729db9 (GREEN commit): FOUND

---
*Phase: 13-章节生成约束注入*
*Completed: 2026-04-09*
