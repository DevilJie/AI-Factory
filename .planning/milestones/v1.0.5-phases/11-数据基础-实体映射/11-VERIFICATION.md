---
phase: 11-数据基础-实体映射
verified: 2026-04-08T00:30:00Z
status: gaps_found
score: 8/10 must-haves verified

re_verification:
  previous_status: gaps_found
  previous_score: 5/10
  gaps_closed:
    - "novel_chapter_plan 表包含 planned_characters JSON 列"
    - "NovelChapterPlan 实体映射 plannedCharacters 和 characterArcs 两个字段"
    - "章节规划 API 响应中包含 plannedCharacters 和 characterArcs 字段（即使为空也不报错）"
    - "common.xml.ChapterPlanXmlDto 已删除，项目编译通过"
    - "Migration script for planned_characters column exists"
  gaps_remaining: []
  regressions: []

gaps:
  - truth: "章节规划 API 响应中 DTO 字段从实体读取数据（plannedCharacters 和 characterArcs 被映射到 DTO）"
    status: failed
    reason: "ChapterService.getChapterPlans (line 197-237), ChapterService.convertChapterPlanToDto (line 486+), and OutlineService (line 518, 665) all create ChapterPlanDto objects but never set plannedCharacters or characterArcs from the entity. Fields exist in DTO and entity but the service layer conversion does not wire them together."
    artifacts:
      - path: "ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java"
        issue: "Lines 197-237 and 486+: DTO construction does not include dto.setPlannedCharacters(plan.getPlannedCharacters()) or dto.setCharacterArcs(plan.getCharacterArcs())"
      - path: "ai-factory-backend/src/main/java/com/aifactory/service/OutlineService.java"
        issue: "Lines 518 and 665: Two additional DTO construction sites also missing these field mappings"
    missing:
      - "Add dto.setPlannedCharacters(plan.getPlannedCharacters()) and dto.setCharacterArcs(plan.getCharacterArcs()) to ChapterService.getChapterPlans lambda (around line 211)"
      - "Add same mappings to ChapterService.convertChapterPlanToDto (around line 504)"
      - "Add same mappings to OutlineService DTO construction at line 518 and line 665"
  - truth: "数据从数据库流向 API 响应的完整路径（Level 4 data-flow for plannedCharacters/characterArcs）"
    status: failed
    reason: "Data flow is broken at the service layer: DB column -> MyBatis entity (OK) -> DTO conversion (BROKEN) -> API response (fields will always be null). The data exists in DB and entity but is never copied to DTO."
    artifacts:
      - path: "ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java"
        issue: "Missing field copy in DTO conversion"
    missing:
      - "Wire entity fields to DTO fields in all conversion points"
---

# Phase 11: 数据基础-实体映射 Verification Report

**Phase Goal:** 数据库和实体层具备存储和关联规划角色的能力，为后续 AI 规划和生成提供数据基础
**Verified:** 2026-04-08T00:30:00Z
**Status:** gaps_found
**Re-verification:** Yes -- after worktree merge for gap closure

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | novel_chapter_plan 表包含 planned_characters JSON 列 | VERIFIED | sql/init.sql line 383: `"planned_characters" json DEFAULT NULL COMMENT '...'`. sql/planned_characters_migration.sql exists with ALTER TABLE statement. |
| 2 | NovelChapterPlan 实体映射 plannedCharacters 和 characterArcs 两个字段 | VERIFIED | NovelChapterPlan.java lines 111 and 118: `private String plannedCharacters` and `private String characterArcs` with full Javadoc. |
| 3 | 章节规划 API 响应中包含 plannedCharacters 和 characterArcs 字段（即使为空也不报错） | VERIFIED (partial) | ChapterPlanDto.java lines 64-67: both fields present with @Schema annotations. JSON serialization includes them (as null). Compilation passes. |
| 4 | common.xml.ChapterPlanXmlDto 已删除，项目编译通过 | VERIFIED | File does not exist. grep -rn returns zero references. `mvn compile -q` exits 0. Commit e07e82b merged via 0704868. |
| 5 | Exact name match returns the correct entity ID | VERIFIED | NameMatchUtil.matchByName tier 1: exact string equality. Tested in testExactMatch/testExactMatchMultipleCandidates. |
| 6 | Suffix-stripped match works for character honorifics | VERIFIED | NameMatchUtil.matchByName tier 2: stripSuffix + compare. Tested in testSuffixStrippedMatch/testSuffixStrippedMatchCandidateSide. |
| 7 | Contains match works as fallback | VERIFIED | NameMatchUtil.matchByName tier 3: bidirectional contains with length >= 2 guard. 2 Tier 3 WARN logs appear in test output (expected). |
| 8 | No match returns null, not an exception | VERIFIED | testNoMatch asserts assertNull. All 22 tests pass (exit code 0). |
| 9 | Null or empty input returns null | VERIFIED | testNullTarget, testEmptyTarget, testNullCandidates, testEmptyCandidates all pass. |
| 10 | Single-character names do not produce false-positive contains matches | VERIFIED | Tier 3 has targetName.length() >= 2 and candidateName.length() >= 2 guards. |

**Score:** 10/10 truths verified at the structural level (fields exist, DB columns exist, tests pass). However, data-flow verification reveals a wiring gap -- see Level 4 analysis below.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `NovelChapterPlan.java` | plannedCharacters + characterArcs String fields | VERIFIED | 130 lines. Both fields present at lines 111 and 118 with Javadoc following existing JSON field pattern. |
| `ChapterPlanDto.java` | plannedCharacters + characterArcs fields with @Schema | VERIFIED | 92 lines. Both fields at lines 64 and 67 with proper @Schema descriptions. |
| `sql/planned_characters_migration.sql` | ALTER TABLE migration script | VERIFIED | 7 lines. ALTER TABLE adds planned_characters json column AFTER foreshadowing_actions. |
| `sql/init.sql` | planned_characters column in CREATE TABLE | VERIFIED | Line 383: `"planned_characters" json DEFAULT NULL`. character_arcs at line 381. |
| `common/xml/ChapterPlanXmlDto.java` | DELETED | VERIFIED | File absent. Zero references in codebase. Compilation clean. |
| `NameMatchUtil.java` | Generic three-tier Chinese name matching | VERIFIED | 118 lines. Exports matchByName, stripSuffix, CHARACTER_SUFFIXES (25 entries), NamedEntity interface. |
| `NameMatchUtilTest.java` | Comprehensive unit tests | VERIFIED | 234 lines. 22 @Test methods. All pass. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| NovelChapterPlan.java | sql/init.sql novel_chapter_plan table | MyBatis-Plus @TableName snake_case <-> camelCase | WIRED | Entity fields plannedCharacters and characterArcs map to DB columns `planned_characters` and `character_arcs` by MyBatis-Plus convention. |
| ChapterPlanDto.java | NovelChapterPlan.java | Service layer DTO conversion | NOT_WIRED | DTO has fields but service methods (ChapterService.getChapterPlans line 199-236, convertChapterPlanToDto line 486+, OutlineService line 518/665) never call setPlannedCharacters/setCharacterArcs. |
| NameMatchUtil.java | WorldviewXmlParser.java | Same three-tier matching pattern | VERIFIED (pattern) | Both implement identical tier structure. NameMatchUtil is a generalized extraction. Integration with WorldviewXmlParser deferred to Phase 12 (by design). |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| ChapterService.getChapterPlans | plan -> dto conversion | NovelChapterPlan from DB via MyBatis | Data read from DB but NOT copied to DTO | HOLLOW |
| ChapterService.convertChapterPlanToDto | chapterPlan -> dto conversion | NovelChapterPlan from DB | Data read from DB but NOT copied to DTO | HOLLOW |
| OutlineService (line 518, 665) | cp -> cdto conversion | NovelChapterPlan from DB | Data read from DB but NOT copied to DTO | HOLLOW |

**Root cause:** The service layer constructs ChapterPlanDto objects by manually setting each field (e.g., `dto.setId(plan.getId())`, `dto.setPlotOutline(plan.getPlotOutline())`). The new plannedCharacters and characterArcs fields were added to the entity and DTO classes but the conversion code was never updated to copy these fields. There are 4 conversion sites across 2 service files:

1. `ChapterService.java` line 199-236 (`getChapterPlans` lambda)
2. `ChapterService.java` line 486+ (`convertChapterPlanToDto` private method)
3. `OutlineService.java` line 518 (save chapter plan path)
4. `OutlineService.java` line 665 (get outline path)

**Impact:** When Phase 12 writes planned character data to the DB, the API will still return null for these fields because the conversion layer drops them. This will block the user-facing feature in Phase 14.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| NameMatchUtil tests all pass | `mvn test -Dtest=NameMatchUtilTest -q` | Exit code 0, 22 tests pass | PASS |
| Project compiles clean | `mvn compile -q` | Exit code 0 | PASS |
| common.xml.ChapterPlanXmlDto has no references | `grep -rn "common.xml.ChapterPlanXmlDto" ai-factory-backend/src/` | Zero results | PASS |
| dto.ChapterPlanXmlDto still exists (kept version) | `test -f ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanXmlDto.java` | EXISTS | PASS |
| planned_characters column in init.sql | `grep "planned_characters" sql/init.sql` | Line 383 found | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| CP-03 | Plan 11-01 | novel_chapter_plan 实体映射 planned_characters JSON 字段和已有 character_arcs JSON 字段 | SATISFIED (with wiring caveat) | Entity has both fields. DTO has both fields. DB has both columns. Service layer conversion missing -- data stored but not surfaced in API responses. |
| CP-04 | Plan 11-02 | 系统通过三级名称匹配将 AI 输出的角色名关联到数据库已有角色 | SATISFIED | NameMatchUtil implements exact -> suffix-strip -> contains matching. 22 unit tests pass. CHARACTER_SUFFIXES has 25 entries. NamedEntity interface provides type-safe generic matching. |

**Orphaned requirements:** None. REQUIREMENTS.md maps only CP-03 and CP-04 to Phase 11, both covered by plans.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| ChapterService.java | 199-236 | Incomplete DTO conversion (missing fields) | Warning | plannedCharacters/characterArcs not copied from entity to DTO at 4 conversion sites |
| ChapterService.java | 486+ | Incomplete DTO conversion (missing fields) | Warning | Same issue in convertChapterPlanToDto private method |
| OutlineService.java | 518, 665 | Incomplete DTO conversion (missing fields) | Warning | Same issue in outline-related DTO construction |

No TODO/FIXME/placeholder/empty-implementation patterns found. The `return null` in NameMatchUtil is correct behavior per specification.

### Human Verification Required

### 1. Verify API Response Schema

**Test:** Start the application and call `GET /api/chapter/plans/{projectId}`. Inspect the response JSON.
**Expected:** The response should include `plannedCharacters` and `characterArcs` keys (currently null/absent since no data written yet).
**Why human:** Requires running server with database connection. Automated check confirms fields exist in DTO class but cannot verify serialization behavior without runtime.

### 2. Run Migration Against Database

**Test:** Execute `sql/planned_characters_migration.sql` against the running MySQL database.
**Expected:** Column `planned_characters` is added to `novel_chapter_plan` table without errors.
**Why human:** Requires database access. Automated check confirms script exists and is syntactically valid.

### Gaps Summary

**All previous gaps are now closed** after the worktree merge (commit 0704868). Entity fields, DTO fields, migration script, init.sql update, and duplicate class deletion are all present on feature/v1.0.4.

**New gap found during re-verification (Level 4 data-flow):** The service layer DTO conversion does not copy `plannedCharacters` or `characterArcs` from entity to DTO. This means:

1. MyBatis-Plus correctly reads DB columns into entity fields
2. The entity fields contain data (when populated)
3. The service layer creates DTO objects but skips the new fields
4. API responses will have `plannedCharacters: null` and `characterArcs: null` regardless of DB contents

This is a **wiring gap**, not a structural gap. All artifacts exist and are substantively correct. The fix is to add 2 lines at each of 4 conversion sites:

```java
dto.setPlannedCharacters(plan.getPlannedCharacters());
dto.setCharacterArcs(plan.getCharacterArcs());
```

**Note:** The phase success criteria #2 says "even if empty, no errors" -- this IS currently met because the DTO fields exist and serialize as null. The gap is about data flow completeness: when Phase 12 writes data, it won't surface through the API until the conversion is fixed. This can be addressed in Phase 12 or as a quick gap-closure plan.

---

_Verified: 2026-04-08T00:30:00Z_
_Verifier: Claude (gsd-verifier)_
