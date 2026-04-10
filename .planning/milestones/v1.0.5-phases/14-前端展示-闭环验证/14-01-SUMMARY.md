---
phase: 14-前端展示-闭环验证
plan: 01
subsystem: api
tags: [spring-boot, rest, mybatis-plus, vue3, typescript, chapter-characters]

# Dependency graph
requires:
  - phase: 12
    provides: "NovelCharacterChapterService.getCharactersByChapterId() and novel_character_chapter table"
  - phase: 10
    provides: "NovelCharacter entity with name field, NovelCharacterMapper"
provides:
  - "GET /api/novel/{projectId}/chapters/{chapterId}/characters endpoint"
  - "ChapterCharacterVO with characterId, characterName, roleType, importanceLevel"
  - "Frontend ChapterCharacter interface and getChapterCharacters() API function"
affects: [14-02]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Controller direct mapper injection for character name lookup (follows existing direct mapper pattern)"]

key-files:
  created:
    - ai-factory-backend/src/main/java/com/aifactory/vo/ChapterCharacterVO.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/controller/ChapterController.java
    - ai-factory-frontend/src/api/chapter.ts

key-decisions:
  - "roleType in ChapterCharacterVO mapped from NovelCharacterChapter.importanceLevel (the relation-level role, not NovelCharacter.roleType)"
  - "Direct NovelCharacterMapper injection in controller follows existing direct-mapper pattern used elsewhere in the codebase"

patterns-established:
  - "Chapter character VO pattern: stream relations -> lookup name via mapper -> build VO"

requirements-completed: [FE-02]

# Metrics
duration: 4min
completed: 2026-04-09
---

# Phase 14 Plan 01: Backend Chapter Characters Endpoint + Frontend API Layer Summary

**REST endpoint and frontend API for fetching actual characters that appeared in a generated chapter, enabling plan-vs-actual comparison**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-09T17:22:35Z
- **Completed:** 2026-04-09T17:26:53Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Created ChapterCharacterVO with characterId, characterName, roleType, importanceLevel fields
- Added GET /api/novel/{projectId}/chapters/{chapterId}/characters endpoint reusing existing NovelCharacterChapterService
- Added frontend ChapterCharacter interface and getChapterCharacters() API function with full TypeScript typing

## Task Commits

Each task was committed atomically:

1. **Task 1: Create backend endpoint for chapter actual characters** - `7819b99` (feat)
2. **Task 2: Add frontend API function and TypeScript type for chapter characters** - `9dcbe90` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/vo/ChapterCharacterVO.java` - VO for chapter-characters endpoint response with @Data @Builder
- `ai-factory-backend/src/main/java/com/aifactory/controller/ChapterController.java` - Added getChapterCharacters endpoint, autowired NovelCharacterChapterService and NovelCharacterMapper
- `ai-factory-frontend/src/api/chapter.ts` - Added ChapterCharacter interface and getChapterCharacters API function

## Decisions Made
- **roleType mapped from importanceLevel**: The NovelCharacterChapter relation stores per-chapter role importance in `importanceLevel`. This is used for both `roleType` and `importanceLevel` in the VO, matching how the chapter-character extraction stores the role type.
- **Direct mapper injection in controller**: Follows the existing pattern where controllers inject mappers directly for simple lookups (consistent with codebase conventions).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Pre-existing test failure in `ChapterGenerationTaskStrategyTest.testResolveCharacterIdsRoleType` (Phase 13 test, unrelated to this plan). Expected `supporting` but got `minor` -- this is a pre-existing issue from the Phase 13 commit and is out of scope.

## Deferred Items
- `ChapterGenerationTaskStrategyTest.testResolveCharacterIdsRoleType` failure -- expects `supporting` but gets `minor`. Pre-existing from Phase 13.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Backend endpoint and frontend API layer are ready for Plan 02 to build the comparison UI
- Plan 02 can import `getChapterCharacters` and `ChapterCharacter` from `@/api/chapter`
- Plan 02 needs to pair this with planned character data from the chapter plan endpoint

## Self-Check: PASSED

- FOUND: ChapterCharacterVO.java
- FOUND: chapter.ts
- FOUND: 7819b99 (Task 1 commit)
- FOUND: 9dcbe90 (Task 2 commit)

---
*Phase: 14-前端展示-闭环验证*
*Completed: 2026-04-09*
