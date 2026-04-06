---
phase: 10-角色体系关联与提取增强
plan: 02
subsystem: ai-prompt
tags: [prompt-template, roleType, faction-extraction, xml, langchain4j]

# Dependency graph
requires:
  - phase: 10-01
    provides: "ChapterCharacterExtractService with novelFactionMapper/resolveAndSaveFactionAssociations already injected"
provides:
  - "Prompt template v2 with Chinese roleType definitions and FC tag format"
  - "buildExistingRoleDistribution() for roleType context injection"
  - "buildFactionList() for faction name list injection"
  - "existingRoleDistribution template variable in v2 prompt"
affects: [10-03, character-extraction, prompt-template]

# Tech tracking
tech-stack:
  added: []
  patterns: [roleType-distribution-injection, faction-list-injection]

key-files:
  created: []
  modified:
    - "sql/init.sql — prompt template v2 INSERT + v1 deactivation UPDATE"
    - "ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java — roleType definitions, distribution injection, faction list injection"

key-decisions:
  - "Injected roleType definitions as static text in chapterInfo StringBuilder (not separate template variable) for ChapterCharacterExtractService consumer"
  - "Passed existingRoleDistribution as separate template variable for v2 template placeholder"
  - "Injected FC tag instructions and faction list directly into chapterInfo StringBuilder"

patterns-established:
  - "RoleType distribution injection: buildExistingRoleDistribution groups by roleType and formats as guidance text"
  - "Faction list injection: buildFactionList queries novelFactionMapper.selectList with projectId for real data"

requirements-completed: [D-07, D-08, D-09, D-13]

# Metrics
duration: 7min
completed: 2026-04-06
---

# Phase 10 Plan 02: Prompt Template V2 + Extraction Context Injection Summary

**Character extraction prompt v2 with Chinese roleType definitions, FC faction tag format, existing role distribution injection, and faction name list reference**

## Performance

- **Duration:** 7 min
- **Started:** 2026-04-06T00:11:57Z
- **Completed:** 2026-04-06T00:19:19Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Prompt template v2 adds Chinese roleType definitions (protagonist=主角, supporting=重要配角, antagonist=反派, npc=过场人物) for accurate classification
- FC tag format (`<FC><N>势力名称</N><R>职位/角色</R></FC>`) added to prompt for faction extraction support
- Existing roleType distribution injected into prompt to prevent re-classification of known characters
- Faction name list injected from database (via novelFactionMapper) for FC tag name reference

## Task Commits

Each task was committed atomically:

1. **Task 1: Create prompt template v2 with roleType definitions and FC tag format** - `71083ff` (feat)
2. **Task 2: Inject existing roleType distribution and faction name list into extraction prompt** - `b5ffc84` (feat)

## Files Created/Modified
- `sql/init.sql` - New template v2 INSERT (id=19, template_id=15, version=2), v1 deactivation UPDATE, current_version_id UPDATE
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java` - Added buildExistingRoleDistribution(), buildFactionList(), roleType definitions in prompt, FC tag instructions

## Decisions Made
- Injected roleType definitions and FC tag instructions directly into chapterInfo StringBuilder rather than as separate template variables, because ChapterCharacterExtractService already bundles all prompt content into chapterInfo
- Passed existingRoleDistribution as a separate template variable (variables.put) because the v2 template has a dedicated {existingRoleDistribution} placeholder between the definitions section and output format section
- buildFactionList uses try-catch with graceful fallback to empty string, matching the defensive pattern of other methods in the class

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Prompt template v2 is active and both ChapterCharacterExtractService and PromptTemplateBuilder will use it
- ChapterCharacterExtractService now injects roleType definitions, distribution, FC tag format, and faction list
- Ready for Plan 03 which builds on the FC tag extraction to process faction associations in the frontend

---
*Phase: 10-角色体系关联与提取增强*
*Completed: 2026-04-06*

## Self-Check: PASSED

- FOUND: sql/init.sql
- FOUND: ChapterCharacterExtractService.java
- FOUND: 10-02-SUMMARY.md
- FOUND: 71083ff (Task 1 commit)
- FOUND: b5ffc84 (Task 2 commit)
