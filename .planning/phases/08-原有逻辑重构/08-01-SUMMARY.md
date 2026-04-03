---
phase: 08-原有逻辑重构
plan: 01
subsystem: api
tags: [dom-parsing, xml, refactoring, spring-component, junit5, mockito]

# Dependency graph
requires:
  - phase: 07-api
    provides: GeographyTaskStrategy, FactionTaskStrategy with duplicated DOM parsing code
provides:
  - WorldviewXmlParser shared DOM parsing utility class
  - parseGeographyXml method for <g> XML parsing
  - parseFactionXml method returning ParsedFactions record
  - Three-tier Chinese name matching (findRegionIdByName, findPowerSystemIdByName)
  - 26 unit tests for WorldviewXmlParser
affects: [08-02-PLAN, WorldviewTaskStrategy refactoring]

# Tech tracking
tech-stack:
  added: []
  patterns: [shared-xml-parser-component, parsed-data-record-separating-parsing-from-persistence]

key-files:
  created:
    - ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java
    - ai-factory-backend/src/test/java/com/aifactory/common/WorldviewXmlParserTest.java
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/GeographyTaskStrategy.java
    - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java

key-decisions:
  - "WorldviewXmlParser as @Component (not pure utility) because name matching methods need DB access via service beans"
  - "parseFactionXml returns ParsedFactions record separating parsing from persistence; caller handles saveTree + Pass 2"
  - "parseFactionNode retains inline findPowerSystemIdByName call because faction entity needs power system ID before saveTree"

patterns-established:
  - "Shared XML parsing via @Component with @Autowired services: WorldviewXmlParser pattern for all worldview DOM parsing"
  - "Parsed-data records: returning structured results (ParsedFactions) instead of directly persisting"

requirements-completed: [REFACT-01]

# Metrics
duration: 11min
completed: 2026-04-03
---

# Phase 08 Plan 01: WorldviewXmlParser Extraction Summary

**Extracted ~400 lines of duplicated DOM parsing into WorldviewXmlParser utility, refactored GeographyTaskStrategy and FactionTaskStrategy to delegate all XML parsing and name matching**

## Performance

- **Duration:** 11 min
- **Started:** 2026-04-03T09:52:15Z
- **Completed:** 2026-04-03T10:03:30Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Created WorldviewXmlParser as @Component with all shared DOM parsing, name matching, and type mapping methods
- GeographyTaskStrategy reduced from ~301 to ~153 lines (removed all DOM parsing methods)
- FactionTaskStrategy reduced from ~542 to ~241 lines (removed all DOM parsing, name matching, type mapping, record definitions)
- 26 unit tests covering parseGeographyXml, parseFactionXml, mapFactionType, mapRelationType, findRegionIdByName, findPowerSystemIdByName, buildNameToIdMap

## Task Commits

Each task was committed atomically:

1. **Task 1: Create WorldviewXmlParser utility class** - `8b244fd` (feat)
2. **Task 2: Refactor GeographyTaskStrategy and FactionTaskStrategy** - `57d2ae9` (refactor)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java` - Shared DOM parsing utility with geography/faction parsing, three-tier name matching, type mapping
- `ai-factory-backend/src/test/java/com/aifactory/common/WorldviewXmlParserTest.java` - 26 unit tests for WorldviewXmlParser
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/GeographyTaskStrategy.java` - Simplified to delegate XML parsing to WorldviewXmlParser
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java` - Simplified to delegate XML parsing and name matching to WorldviewXmlParser

## Decisions Made
- **WorldviewXmlParser as @Component**: Not a pure utility class because findRegionIdByName and findPowerSystemIdByName need DB access through continentRegionService and powerSystemService
- **parseFactionXml returns ParsedFactions record**: Separates parsing from persistence so the caller controls saveTree and Pass 2 association logic
- **parseFactionNode retains power system lookup**: Calls findPowerSystemIdByName during parsing because NovelFaction.corePowerSystem must be set before saveTree

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- WorldviewXmlParser ready for WorldviewTaskStrategy refactoring in Plan 02
- All DOM parsing logic consolidated; WorldviewTaskStrategy still contains its own copy of the same methods

## Self-Check: PASSED

All 4 files verified present. Both commits (8b244fd, 57d2ae9) verified in git history.

---
*Phase: 08-原有逻辑重构*
*Completed: 2026-04-03*
