---
phase: 03-ai
plan: 02
subsystem: ai
tags: [dom-parsing, xml, mybatis-plus, langchain4j, two-pass-insert, name-matching]

# Dependency graph
requires:
  - phase: 01-data-foundation
    provides: "novel_faction/novel_faction_region/novel_faction_relation tables, NovelFaction entity, NovelFactionRegionMapper, NovelFactionRelationMapper"
  - phase: 02-api
    provides: "FactionService.saveTree(), FactionService.deleteByProjectId(), FactionService.fillForces()"
provides:
  - "saveFactionsFromXml() method in WorldviewTaskStrategy for DOM parsing <f> XML"
  - "Two-pass insert pattern: saveTree then region/relation associations"
  - "Three-tier name matching (exact -> strip suffix -> contains) for regions and power systems"
  - "checkExisting faction cleanup before worldview regeneration"
affects: [03-03-prompt-migration, frontend-faction-tree]

# Tech tracking
tech-stack:
  added: []
  patterns: [dom-parsing-with-getChildNodes, two-pass-insert-name-to-id, three-tier-name-matching, chinese-to-english-type-mapping]

key-files:
  created: []
  modified:
    - "ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java"

key-decisions:
  - "Used getChildNodes() exclusively for all new DOM operations per D-08, avoiding getElementsByTagName"
  - "Used continentRegionService.listByProjectId() and powerSystemService.listByProjectId() for name matching (service-oriented, no mapper)"
  - "Chinese type labels mapped at parse time: zhengpai->ally, fanpai->hostile, zhongli->neutral"
  - "Two-pass insert: saveTree populates IDs, then buildNameToIdMap collects them for Pass 2 association inserts"

patterns-established:
  - "Two-pass insert: parse tree -> saveTree -> buildNameToIdMap -> insert associations with IDs"
  - "Three-tier name matching: exact match -> strip common suffixes -> contains match"
  - "getChildNodes() iteration with node type and name check for direct child access"

requirements-completed: [AI-03, AI-04, AI-05, AI-06, AI-07, AI-08]

# Metrics
duration: 5min
completed: 2026-04-02
---

# Phase 3 Plan 2: AI Faction XML Parsing Summary

**saveFactionsFromXml() with two-pass insert, three-tier name matching, and DOM parsing using getChildNodes() exclusively for nested faction XML ingestion**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-02T02:52:45Z
- **Completed:** 2026-04-02T02:57:45Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Added saveFactionsFromXml() to WorldviewTaskStrategy that parses nested <faction> XML from AI responses and inserts into novel_faction/novel_faction_region/novel_faction_relation tables
- Implemented two-pass insert pattern: first pass saves all factions via FactionService.saveTree() and builds name-to-ID map, second pass creates region and inter-faction relation associations using resolved IDs
- Added three-tier name matching (exact -> strip suffix -> contains) for both regions and power systems, querying via service methods (not mappers)
- Updated checkExisting() to delete old faction data before worldview regeneration
- Updated parseAndSaveWorldview() to call saveFactionsFromXml() as Step 6 after geography and power systems

## Task Commits

Each task was committed atomically:

1. **Task 1: Add saveFactionsFromXml, parseFactionNode, name matching, and update checkExisting/parseAndSaveWorldview** - `85b7271` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` - Added saveFactionsFromXml(), parseFactionNode(), parseFactionNodes(), parseRelationElement(), mapFactionType(), mapRelationType(), buildNameToIdMap(), findRegionIdByName(), findPowerSystemIdByName(), plus PendingFactionAssociations and PendingRelation records. Updated checkExisting() and parseAndSaveWorldview().

## Decisions Made
- Used getChildNodes() exclusively for all new DOM operations per D-08, avoiding getElementsByTagName which would return all descendants
- Used continentRegionService.listByProjectId() and powerSystemService.listByProjectId() for name-to-ID matching, maintaining service-oriented consistency
- Chinese type labels mapped at parse time in mapFactionType() (zhengpai->ally) and mapRelationType() (mengyou->ally)
- saveTree() populates IDs via MyBatis-Plus auto-increment, then buildNameToIdMap() traverses the tree recursively to collect all name-to-ID mappings

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Worktree branch was behind feature/v1.0.2 (missing Phase 1/2 commits with FactionService, entities, mappers). Resolved by merging feature/v1.0.2 into the worktree branch before implementation.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- saveFactionsFromXml() is ready for AI integration; the <f> XML parsing pipeline is complete
- Next plan (03-03) handles prompt template migration: updating 6 getForces() call sites to use fillForces(), and updating the AI prompt template to produce structured faction XML

## Self-Check: PASSED

- FOUND: ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java
- FOUND: commit 85b7271
- FOUND: .planning/phases/03-ai/03-02-SUMMARY.md

---
*Phase: 03-ai*
*Completed: 2026-04-02*
