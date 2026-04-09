---
phase: 11-数据基础-实体映射
plan: 01
subsystem: database
tags: [mybatis-plus, mysql, json-column, entity-mapping, dto]

# Dependency graph
requires:
  - phase: 10-角色体系关联与提取增强
    provides: "Character extraction and roleType classification for chapter planning"
provides:
  - "planned_characters JSON column in novel_chapter_plan table"
  - "plannedCharacters + characterArcs fields in NovelChapterPlan entity"
  - "plannedCharacters + characterArcs fields in ChapterPlanDto API response"
  - "Migration script for planned_characters column"
  - "Removed unused common.xml.ChapterPlanXmlDto duplicate"
affects: [12-章节规划XML解析增强, 13-章节生成约束注入, 14-前端章节规划展示]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JSON String fields for complex column data (consistent with keyEvents/foreshadowingSetup pattern)"

key-files:
  created:
    - sql/planned_characters_migration.sql
  modified:
    - sql/init.sql
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java
  deleted:
    - ai-factory-backend/src/main/java/com/aifactory/common/xml/ChapterPlanXmlDto.java

key-decisions:
  - "plannedCharacters uses String type matching existing JSON column pattern (keyEvents, foreshadowingSetup)"
  - "characterArcs mapped from existing DB column that was never in the entity (DB-first field addition)"
  - "Kept dto.ChapterPlanXmlDto (active usage) and deleted common.xml.ChapterPlanXmlDto (zero references)"
  - "volumeNumber field NOT migrated from common.xml version - chapter plans use volumePlanId FK instead"

patterns-established:
  - "JSON String fields for complex column data: consistent with existing keyEvents/foreshadowingSetup/foreshadowingPayoff pattern"

requirements-completed: [CP-03]

# Metrics
duration: 5min
completed: 2026-04-07
---

# Phase 11 Plan 01: Entity Field Mapping Summary

**Added planned_characters JSON column to novel_chapter_plan, mapped plannedCharacters and characterArcs in entity and DTO, deleted unused common.xml.ChapterPlanXmlDto duplicate**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-07T15:33:03Z
- **Completed:** 2026-04-07T15:38:43Z
- **Tasks:** 2
- **Files modified:** 4 (created 1, modified 3, deleted 1)

## Accomplishments
- Database schema extended with planned_characters JSON column for per-chapter character assignments
- NovelChapterPlan entity now maps both plannedCharacters (new column) and characterArcs (previously unmapped column)
- ChapterPlanDto exposes both fields in API responses for frontend consumption
- Removed dead code: unused common.xml.ChapterPlanXmlDto with zero import references

## Task Commits

Each task was committed atomically:

1. **Task 1: Add planned_characters column + entity/DTO field mapping** - `923c3ad` (feat)
2. **Task 2: Delete unused common.xml.ChapterPlanXmlDto duplicate** - `e07e82b` (refactor)

## Files Created/Modified
- `sql/planned_characters_migration.sql` - Migration script adding planned_characters JSON column after foreshadowing_actions
- `sql/init.sql` - Updated CREATE TABLE with planned_characters column definition
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` - Added plannedCharacters and characterArcs String fields with Javadoc
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` - Added plannedCharacters and characterArcs String fields with @Schema annotations
- `ai-factory-backend/src/main/java/com/aifactory/common/xml/ChapterPlanXmlDto.java` - DELETED (zero-reference duplicate)

## Decisions Made
- **plannedCharacters uses String type** matching existing JSON column pattern (keyEvents, foreshadowingSetup, foreshadowingPayoff) for MyBatis-Plus auto-mapping consistency
- **characterArcs mapped from existing DB column** (character_arcs) that existed in the database but was never mapped in the Java entity
- **Kept dto.ChapterPlanXmlDto** (active usage by ChapterGenerationTaskStrategy with @Schema annotations) and **deleted common.xml.ChapterPlanXmlDto** (zero import references, no @Schema annotations)
- **volumeNumber field NOT migrated** from common.xml version because chapter plans use volumePlanId FK relationship instead

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Entity and DTO fields ready for Phase 12 (XML parsing enhancement to populate plannedCharacters)
- Migration script ready for DBA execution before deployment
- characterArcs now mapped, enabling future character arc tracking in chapter generation
- No blockers for next plan

## Self-Check: PASSED

- sql/planned_characters_migration.sql: FOUND
- NovelChapterPlan entity: FOUND
- ChapterPlanDto: FOUND
- common.xml.ChapterPlanXmlDto: CONFIRMED DELETED
- 11-01 commits: FOUND

---
*Phase: 11-数据基础-实体映射*
*Completed: 2026-04-07*
