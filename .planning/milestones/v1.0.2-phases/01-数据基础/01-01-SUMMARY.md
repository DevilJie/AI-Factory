---
phase: 01-数据基础
plan: 01
subsystem: database
tags: [mysql, mybatis-plus, ddl, tree-structure, entity]

# Dependency graph
requires: []
provides:
  - "sql/faction_migration.sql: 4 张势力相关表 DDL（novel_faction、novel_faction_region、novel_faction_character、novel_faction_relation）"
  - "NovelFaction.java: 势力树形实体类，字段与 DDL 驼峰映射"
  - "NovelFactionMapper.java: 势力 Mapper 接口，继承 BaseMapper"
affects: [01-02, 02-后端服务与API]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Tree entity pattern: parentId + deep + sortOrder + @TableField(exist=false) children"
    - "Faction type encoding: ally/hostile/neutral (English codes, frontend does i18n)"
    - "Nullable top-level fields: type and corePowerSystem allow NULL for child factions"

key-files:
  created:
    - sql/faction_migration.sql
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java
    - ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionMapper.java
  modified: []

key-decisions:
  - "relation_type uses English codes (ally/hostile/neutral) consistent with project role_type convention"
  - "No FOREIGN KEY constraints on any table, pure logical associations per D-08"

patterns-established:
  - "Faction tree entity mirrors NovelContinentRegion exactly with added type/corePowerSystem fields"
  - "DDL style: SET NAMES utf8mb4, utf8mb4_unicode_ci, InnoDB, Dynamic ROW_FORMAT, no FK constraints"

requirements-completed: [DB-01, DB-02, DB-07]

# Metrics
duration: 3min
completed: 2026-04-01
---

# Phase 1 Plan 01: SQL Migration and Faction Entity Summary

**4-table DDL migration script (novel_faction tree + 3 association tables) and NovelFaction tree entity with MyBatis-Plus BaseMapper**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-01T14:54:54Z
- **Completed:** 2026-04-01T14:58:37Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Created faction_migration.sql with 4 complete CREATE TABLE statements matching geography_migration.sql style
- Created NovelFaction entity with tree structure and nullable type/corePowerSystem fields (top-level only per D-02/D-05)
- Created NovelFactionMapper extending BaseMapper<NovelFaction> with @Mapper annotation
- Backend compiles cleanly with new entity and mapper

## Task Commits

Each task was committed atomically:

1. **Task 1: Create faction_migration.sql** - `b807ca4` (feat)
2. **Task 2: Create NovelFaction entity and NovelFactionMapper** - `760bac2` (feat)

## Files Created/Modified
- `sql/faction_migration.sql` - 4-table DDL migration (novel_faction, novel_faction_region, novel_faction_character, novel_faction_relation)
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java` - Tree entity with type/corePowerSystem nullable fields and transient children
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionMapper.java` - BaseMapper interface for faction CRUD

## Decisions Made
None - followed plan as specified. All DDL and entity patterns match plan exactly.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 01-02 can proceed: 3 association table entities (NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation) and their mappers need creation
- Plan 01-02 will also mark NovelWorldview.forces as @TableField(exist=false) transient
- All DDL tables are ready for Phase 2 FactionService implementation

---
*Phase: 01-数据基础*
*Completed: 2026-04-01*

## Self-Check: PASSED

- FOUND: sql/faction_migration.sql
- FOUND: ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java
- FOUND: ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionMapper.java
- FOUND: commit b807ca4 (Task 1)
- FOUND: commit 760bac2 (Task 2)
