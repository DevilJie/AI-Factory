---
phase: 12-AI规划输出-XML解析
plan: 02
subsystem: backend, prompt-templates, sql
tags: [prompt-template, character-planning, xml-tags, template-versioning]

# Dependency graph
requires:
  - phase: 12-AI规划输出-XML解析/01
    provides: "DOM parseChaptersXml with <ch> character tag extraction, novelCharacterService autowired in ChapterGenerationTaskStrategy"
provides:
  - "Chapter planning prompt template v2 with <ch>/<cn>/<cd>/<ci> character planning XML output instructions"
  - "buildChapterPromptUsingTemplate injects real character data (name + roleType) into {characterInfo} variable"
  - "context.putSharedData('allCharacters') making character list available to prompt builder"
  - "sql/phase12_template_upgrade.sql migration file for existing deployments"
affects: [chapter-generation, character-planning, prompt-templates]

# Tech tracking
tech-stack:
  added: []
  patterns: [context shared data for cross-method character list passing, template version upgrade with is_active flag pattern]

key-files:
  created:
    - "sql/phase12_template_upgrade.sql"
  modified:
    - "ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java"
    - "sql/init.sql"

key-decisions:
  - "Used template_version id=20 instead of plan-specified id=19 because id=19 was already occupied by character extract template v2"
  - "Injected full character list (name + roleType) per character rather than complex format to keep prompt concise"

patterns-established:
  - "Template version upgrade pattern: insert new version with higher id, set is_active=1, deactivate old version, update current_version_id"
  - "context.putSharedData() for passing loaded data between methods that share TaskContext"

requirements-completed: [CP-01]

# Metrics
duration: 9min
completed: 2026-04-08
---

# Phase 12 Plan 02: Chapter Planning Prompt Template Upgrade Summary

**Chapter planning prompt template v2 with <ch> character XML output instructions, real character list injection replacing hardcoded placeholder**

## Performance

- **Duration:** 9 min
- **Started:** 2026-04-07T17:36:53Z
- **Completed:** 2026-04-07T17:45:55Z
- **Tasks:** 1
- **Files modified:** 3

## Accomplishments
- Replaced hardcoded "暂无登场角色" with real character list injection (name + roleType per character)
- New template_version id=20 for template_id=6 with character planning instructions and <ch>/<cn>/<cd>/<ci> XML tags
- Template instructs AI to output 0-3 <ch> tags per chapter with character name, role description, and importance level
- Template allows new characters not in the provided list (characterId=null for unmatched names)
- Migration SQL file for existing deployments that already have the database initialized

## Task Commits

Each task was committed atomically:

1. **Task 1: Inject character list + upgrade prompt template SQL** - `f2dc88c` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` - Added context.putSharedData("allCharacters"), replaced hardcoded characterInfo with conditional injection
- `sql/init.sql` - Added template_version id=20 with character planning XML tags, updated current_version_id to 20, added deactivation UPDATE for old version
- `sql/phase12_template_upgrade.sql` - Migration SQL for existing deployments (INSERT new version, deactivate old, update template)

## Decisions Made
- Used template_version id=20 instead of plan-specified id=19 because id=19 was already occupied by character extract template (template_id=15) v2 -- plan was written before that id was taken
- Injected full character list (name + roleType) per character rather than complex multi-field format to keep prompt concise and avoid token budget concerns
- Added "角色规划要求" section after "登场角色" and before "章节规划要求" to keep related content adjacent

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Reset worktree branch to include Plan 01 commits**
- **Found during:** Task 1 (initial file verification)
- **Issue:** Worktree was created from master branch which lacks Plan 01 commits (DOM parser, NameMatchUtil, novelCharacterService)
- **Fix:** git reset --hard feature/v1.0.4 to include Plan 01 codebase
- **Files modified:** None (branch reset)
- **Committed in:** f2dc88c (foundation for task commit)

**2. [Rule 3 - Blocking] Used template_version id=20 instead of id=19**
- **Found during:** Task 1 (SQL INSERT creation)
- **Issue:** Plan specified id=19 but that id was already used by template_id=15 character extract v2
- **Fix:** Used id=20 instead, updated both init.sql INSERT and migration file
- **Files modified:** sql/init.sql, sql/phase12_template_upgrade.sql
- **Committed in:** f2dc88c

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both were necessary prerequisite adjustments. No scope creep.

## Issues Encountered
None - implementation followed the plan's detailed action steps precisely after the blocking issues were resolved.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Chapter planning prompt template now instructs AI to output <ch> character tags
- buildChapterPromptUsingTemplate injects real character data for AI reference
- Combined with Plan 01's DOM parser, the full pipeline is: inject characters -> AI outputs <ch> tags -> DOM parser extracts them -> NameMatchUtil resolves names -> plannedCharacters JSON persisted
- Phase 12 is complete (both plans executed)

---
*Phase: 12-AI规划输出-XML解析*
*Completed: 2026-04-08*

## Self-Check: PASSED

- All 4 key files exist on disk (ChapterGenerationTaskStrategy.java, init.sql, phase12_template_upgrade.sql, 12-02-SUMMARY.md)
- Commit f2dc88c found in git log
- context.getSharedData().containsKey("allCharacters") present in ChapterGenerationTaskStrategy.java
- context.putSharedData("allCharacters") present in ChapterGenerationTaskStrategy.java
- variables.put("characterInfo", charInfo.toString()) present (real data injection)
- variables.put("characterInfo", "暂无登场角色") present in else branch (fallback)
- template_version id=20 with <ch>/<cn>/<cd>/<ci> tags present in init.sql
- current_version_id=20 for template id=6 in ai_prompt_template INSERT
- sql/phase12_template_upgrade.sql migration file exists
- All 93 tests pass (0 failures)
