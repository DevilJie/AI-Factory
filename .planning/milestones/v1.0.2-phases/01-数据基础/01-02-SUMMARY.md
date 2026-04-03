---
phase: 01-数据基础
plan: 02
subsystem: database
tags: [mybatis-plus, entity, mapper, transient-field, mysql]

# Dependency graph
requires:
  - phase: 01-数据基础/01-01
    provides: novel_faction tree table entity and mapper
provides:
  - NovelFactionRegion entity (faction-region many-to-many)
  - NovelFactionCharacter entity with role field (faction-character)
  - NovelFactionRelation entity with relationType (faction-faction)
  - 3 mapper interfaces for association tables
  - NovelWorldview.forces field marked as @TableField(exist=false)
affects: [02-后端服务, 03-AI生成, 04-前端]

# Tech tracking
tech-stack:
  added: []
  patterns: [association-table-entity, transient-field-annotation]

key-files:
  created:
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRegion.java
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionCharacter.java
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRelation.java
    - ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRegionMapper.java
    - ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionCharacterMapper.java
    - ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRelationMapper.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java

key-decisions:
  - "Forces field kept as String type with @TableField(exist=false), matching geography field precedent"
  - "Association entities follow minimal field pattern from NovelWorldviewPowerSystem reference"

patterns-established:
  - "Association table entity: @Data + @TableName + @TableId(AUTO) + foreign key Long fields"
  - "Mapper interface: @Mapper + extends BaseMapper<Entity> with no custom methods"

requirements-completed: [DB-03, DB-04, DB-05, DB-06]

# Metrics
duration: 3min
completed: 2026-04-01
---

# Phase 1 Plan 2: Faction Association Entities and Mappers Summary

**3 association table entities (FactionRegion, FactionCharacter, FactionRelation) with mappers, plus NovelWorldview.forces marked transient**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-01T14:44:01Z
- **Completed:** 2026-04-01T14:47:30Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Created NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation entities mapping to their respective association tables
- Created 3 Mapper interfaces (NovelFactionRegionMapper, NovelFactionCharacterMapper, NovelFactionRelationMapper) extending BaseMapper
- Marked NovelWorldview.forces as @TableField(exist=false) to dissociate from DB column, matching geography field precedent

## Task Commits

Each task was committed atomically:

1. **Task 1: Create 3 association entities and 3 mapper interfaces** - `23edb74` (feat)
2. **Task 2: Mark NovelWorldview.forces as transient** - `32f88d7` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRegion.java` - Faction-region many-to-many association entity (id, factionId, regionId)
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionCharacter.java` - Faction-character association entity with role field
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRelation.java` - Faction-faction relation entity with relationType and description
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRegionMapper.java` - Mapper for faction-region table
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionCharacterMapper.java` - Mapper for faction-character table
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRelationMapper.java` - Mapper for faction-relation table
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java` - Added @TableField(exist=false) to forces field

## Decisions Made
- Kept forces field as String type with transient annotation rather than removing it, maintaining backward compatibility with code that reads this field
- Followed exact pattern from NovelWorldviewPowerSystem.java for association entities and NovelContinentRegionMapper.java for mappers

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 3 association table entities and mappers ready for service layer (FactionService) to use in Phase 2
- NovelWorldview.forces is now transient, enabling Phase 3 AI generation to populate forces from structured tables
- Requires 01-01 (NovelFaction entity) to be completed before FactionService can be built

## Self-Check: PASSED

- All 7 created/modified files found on disk
- Both task commits (23edb74, 32f88d7) found in git log

---
*Phase: 01-数据基础*
*Completed: 2026-04-01*
