---
phase: 10-角色体系关联与提取增强
plan: 01
subsystem: database, api
tags: [mybatis-plus, spring-boot, mysql, xml-parsing, name-resolution, upsert]

# Dependency graph
requires:
  - phase: 01-09 (v1.0.2 + v1.0.3)
    provides: novel_faction, novel_faction_character, novel_power_system, novel_power_system_level, NovelCharacter entity/mapper
provides:
  - character_power_system join table with unique index on (character_id, power_system_id)
  - CharacterPowerSystem entity + mapper
  - FactionConnectionDto for FC tag XML parsing
  - resolveAndSavePowerSystemAssociations with exact/fuzzy name matching
  - resolveAndSaveFactionAssociations with exact/fuzzy name matching
  - CharacterDetailVO with aggregated power system + faction association data
  - Character list API with cultivationRealm and factionInfo summary fields
  - 4 CRUD endpoints for managing character-power_system and character-faction associations
  - 6 test stub methods for downstream verification
affects: [10-02-prompt-template, 10-03-frontend]

# Tech tracking
tech-stack:
  added: []
  patterns: [name-resolution-exact-fuzzy, batch-query-aggregation, vo-from-entity]

key-files:
  created:
    - ai-factory-backend/src/main/java/com/aifactory/entity/CharacterPowerSystem.java
    - ai-factory-backend/src/main/java/com/aifactory/mapper/CharacterPowerSystemMapper.java
    - ai-factory-backend/src/main/java/com/aifactory/vo/CharacterDetailVO.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/CharacterPowerSystemRequest.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/CharacterFactionRequest.java
    - ai-factory-backend/src/test/java/com/aifactory/service/CharacterPowerSystemServiceTest.java
    - ai-factory-backend/src/test/java/com/aifactory/service/ChapterCharacterExtractServiceTest.java
  modified:
    - sql/init.sql
    - ai-factory-backend/src/main/java/com/aifactory/dto/ChapterCharacterExtractXmlDto.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java
    - ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterService.java
    - ai-factory-backend/src/main/java/com/aifactory/controller/NovelCharacterController.java
    - ai-factory-backend/src/main/java/com/aifactory/dto/CharacterDto.java

key-decisions:
  - "In-memory name matching via list query + loop instead of individual DB queries per character"
  - "Batch query pattern for list API to avoid N+1 (collect IDs, batch fetch, group by characterId)"
  - "CharacterDetailVO.fromCharacter() factory method to copy basic NovelCharacter fields"
  - "Fuzzy match uses String.contains() rather than SQL LIKE for simplicity and consistency with extraction service"

patterns-established:
  - "Batch aggregation pattern: collect all character IDs, batch query associations, group by characterId, resolve names via ID->name maps"
  - "VO with embedded association objects: CharacterDetailVO contains List<PowerSystemAssociation> and List<FactionAssociation>"
  - "Direct mapper injection for simple association CRUD (matches FactionController pattern)"

requirements-completed: [D-01, D-02, D-03, D-04, D-05, D-06, D-12]

# Metrics
duration: 11min
completed: 2026-04-06
---

# Phase 10 Plan 01: Character Association Data Layer Summary

**character_power_system join table, FC tag XML DTO, name-to-ID resolution with upsert during extraction, and aggregated character detail/list APIs with 4 CRUD endpoints**

## Performance

- **Duration:** 11 min
- **Started:** 2026-04-05T23:54:51Z
- **Completed:** 2026-04-06T00:06:49Z
- **Tasks:** 4
- **Files modified:** 13

## Accomplishments
- Created character_power_system join table with unique index preventing duplicate associations
- Extended XML DTO with FactionConnectionDto supporting multiple FC tags per character
- Implemented power system name resolution (exact then fuzzy) with hierarchical level/step matching
- Implemented faction name resolution with upsert into existing novel_faction_character table
- Extended character detail API to return aggregated power system and faction associations
- Extended character list API to return cultivationRealm and factionInfo summary strings
- Added 4 CRUD endpoints for managing associations from frontend

## Task Commits

Each task was committed atomically:

1. **Task 0: Create Wave 0 test stub files** - `6345d60` (test)
2. **Task 1: Create character_power_system table, entity, and mapper** - `058a1ab` (feat)
3. **Task 2: Extend XML DTO with FC tags and add association resolution to extraction service** - `6ede6e7` (feat)
4. **Task 3: Extend character detail AND list APIs to return aggregated association data** - `b70fd84` (feat)

## Files Created/Modified
- `sql/init.sql` - Added character_power_system table DDL with unique index
- `ai-factory-backend/src/main/java/com/aifactory/entity/CharacterPowerSystem.java` - Join entity with characterId, powerSystemId, currentRealmId, currentSubRealmId
- `ai-factory-backend/src/main/java/com/aifactory/mapper/CharacterPowerSystemMapper.java` - BaseMapper for character_power_system
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterCharacterExtractXmlDto.java` - Added FactionConnectionDto inner class and factionConnections list field
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java` - Added resolveAndSavePowerSystemAssociations and resolveAndSaveFactionAssociations methods with name resolution and upsert
- `ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterService.java` - Extended getCharacterDetail with aggregated associations, getCharacterList with batch aggregation, added CRUD methods
- `ai-factory-backend/src/main/java/com/aifactory/controller/NovelCharacterController.java` - Changed detail API to return CharacterDetailVO, added 4 association CRUD endpoints
- `ai-factory-backend/src/main/java/com/aifactory/dto/CharacterDto.java` - Added roleType, cultivationRealm, factionInfo fields
- `ai-factory-backend/src/main/java/com/aifactory/vo/CharacterDetailVO.java` - New VO with PowerSystemAssociation and FactionAssociation inner classes
- `ai-factory-backend/src/main/java/com/aifactory/dto/CharacterPowerSystemRequest.java` - Request DTO for power system association CRUD
- `ai-factory-backend/src/main/java/com/aifactory/dto/CharacterFactionRequest.java` - Request DTO for faction association CRUD
- `ai-factory-backend/src/test/java/com/aifactory/service/CharacterPowerSystemServiceTest.java` - 3 stub tests
- `ai-factory-backend/src/test/java/com/aifactory/service/ChapterCharacterExtractServiceTest.java` - 3 stub tests

## Decisions Made
- Used in-memory name matching (query all systems for project, then loop-match) instead of individual DB queries per character -- consistent with extraction flow pattern and avoids N+1
- Batch query pattern for list API aggregation: collect all character IDs, batch query associations, group by characterId, resolve names via ID-to-name maps built from batch queries
- Fuzzy match uses String.contains() rather than SQL LIKE -- works on in-memory lists after initial project-scoped query, consistent approach across both power system and faction matching
- CharacterDetailVO.fromCharacter() factory method for clean field copying from NovelCharacter entity

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Data layer complete for character-power_system and character-faction associations
- Plan 02 can proceed with prompt template update (add FC tag instructions, roleType Chinese definitions)
- Plan 03 can proceed with frontend Drawer tabs and association management UI
- Test stubs ready for expansion when real test cases are needed

---
*Phase: 10-角色体系关联与提取增强*
*Completed: 2026-04-06*

## Self-Check: PASSED

- All 10 created/modified files verified present
- All 4 task commits verified in git log (6345d60, 058a1ab, 6ede6e7, b70fd84)
- Compilation: `mvn compile -q` exits 0
- Tests: 6/6 pass (CharacterPowerSystemServiceTest + ChapterCharacterExtractServiceTest)
