---
phase: 12-AI规划输出-XML解析
plan: 01
subsystem: backend, xml-parsing
tags: [dom, xml, name-matching, character-planning, tdd]

# Dependency graph
requires:
  - phase: 11-数据基础-实体映射
    provides: "NameMatchUtil with NamedEntity interface and CHARACTER_SUFFIXES"
provides:
  - "DOM-based parseChaptersXml that extracts <ch> character tags from chapter XML"
  - "resolveCharacterIds method using NameMatchUtil.matchByName for three-tier name matching"
  - "plannedCharacters JSON persistence to novel_chapter_plan table"
  - "NovelCharacter implements NameMatchUtil.NamedEntity for type-safe matching"
affects: [12-02-PLAN, chapter-generation, character-planning]

# Tech tracking
tech-stack:
  added: [javax.xml.parsers.DocumentBuilder, org.w3c.dom]
  patterns: [DOM parsing for nested same-name XML tags, NameMatchUtil integration for character resolution]

key-files:
  created:
    - "ai-factory-backend/src/test/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategyTest.java"
  modified:
    - "ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java"
    - "ai-factory-backend/src/main/java/com/aifactory/entity/NovelCharacter.java"

key-decisions:
  - "Duplicated sanitizeXmlForDomParsing from WorldviewXmlParser (~80 lines) rather than extracting shared utility -- minimal scope, well-tested in both locations"
  - "parseSingleChapter also accepts <ed> tag (in addition to <f>) for ending scene to handle both prompt format variants"

patterns-established:
  - "DOM parsing with getChildNodes() for nested same-name tags (matches WorldviewXmlParser pattern)"
  - "resolveCharacterIds: deserialize -> NameMatchUtil.matchByName per entry -> re-serialize pipeline"

requirements-completed: [CP-01, CP-02]

# Metrics
duration: 6min
completed: 2026-04-08
---

# Phase 12 Plan 01: Chapter XML DOM Parser with Character Extraction Summary

**DOM-based parseChaptersXml replacing Jackson XML, with <ch> character tag extraction, NameMatchUtil name resolution, and plannedCharacters JSON persistence**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-07T17:28:02Z
- **Completed:** 2026-04-07T17:34:18Z
- **Tasks:** 1 (TDD: RED + GREEN)
- **Files modified:** 3

## Accomplishments
- Replaced Jackson XML parseChaptersXml with DOM parsing that correctly handles nested same-name `<ch>` tags inside `<o>` elements
- Added character extraction pipeline: parse `<ch>` tags -> resolve names via NameMatchUtil.matchByName -> persist plannedCharacters JSON
- NovelCharacter now implements NameMatchUtil.NamedEntity for type-safe matching
- XML sanitization handles raw `&`, unmatched tags without SAXParseException
- All 8 unit tests pass with no regression in existing tests (55 tests total)

## Task Commits

Each task was committed atomically:

1. **RED: Failing tests for DOM parseChaptersXml** - `f30d5de` (test)
2. **GREEN: DOM parseChaptersXml with character extraction** - `0e3a3be` (feat)

_Note: TDD task with test-then-implementation commits_

## Files Created/Modified
- `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategyTest.java` - 8 unit tests for DOM parsing, character extraction, sanitization, and name matching
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` - DOM-based parseChaptersXml, parseSingleChapter, parseCharacterTag, resolveCharacterIds, sanitizeXmlForDomParsing, NovelCharacterService integration
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelCharacter.java` - Added implements NameMatchUtil.NamedEntity

## Decisions Made
- Duplicated sanitizeXmlForDomParsing (~80 lines) from WorldviewXmlParser rather than extracting a shared XmlSanitizer utility class -- keeps scope minimal, method is well-tested in both locations
- parseSingleChapter accepts both `<ed>` and `<f>` tags for ending scene to handle both prompt format variants (prompt example uses `<ed>`, plan spec uses `<f>`)
- resolveCharacterIds defaults unmatched characters to roleType="supporting" per plan spec (D-09)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed test assertion for Jackson NullNode vs Java null**
- **Found during:** Task 1 GREEN phase
- **Issue:** Test used assertNull() on JsonNode.get("characterId") but Jackson returns NullNode (not Java null) for JSON null values
- **Fix:** Changed to assertTrue(chars1.get(0).get("characterId").isNull())
- **Files modified:** ChapterGenerationTaskStrategyTest.java
- **Committed in:** 0e3a3be (GREEN commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor test fix only. No scope creep.

## Issues Encountered
None - implementation followed the proven WorldviewXmlParser DOM pattern

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Chapter XML parsing pipeline complete: DOM parse -> character extraction -> name matching -> persistence
- Ready for Plan 12-02 which builds on this foundation
- NovelCharacterService.getNonNpcCharacters wired into chapter generation flow

---
*Phase: 12-AI规划输出-XML解析*
*Completed: 2026-04-08*

## Self-Check: PASSED

- All 4 key files exist on disk
- Both commits (f30d5de, 0e3a3be) found in git log
- DocumentBuilder present in ChapterGenerationTaskStrategy.java
- NameMatchUtil.matchByName present in ChapterGenerationTaskStrategy.java
- setPlannedCharacters present in ChapterGenerationTaskStrategy.java
- implements NameMatchUtil.NamedEntity present in NovelCharacter.java
