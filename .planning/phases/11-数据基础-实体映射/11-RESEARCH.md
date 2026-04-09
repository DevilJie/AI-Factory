# Phase 11: 数据基础 + 实体映射 - Research

**Researched:** 2026-04-07
**Domain:** Database schema extension, MyBatis-Plus entity mapping, DTO consolidation, Chinese name fuzzy matching
**Confidence:** HIGH

## Summary

Phase 11 is a pure backend data-layer phase with four deliverables: (1) add a `planned_characters` JSON column to `novel_chapter_plan`, (2) map both `planned_characters` and the existing-but-unmapped `character_arcs` column to the `NovelChapterPlan` entity, (3) consolidate two duplicate `ChapterPlanXmlDto` classes into one, and (4) implement a reusable `NameMatchUtil` for three-tier Chinese character name matching. Every change follows established patterns already proven in this codebase -- no new dependencies, no new architectural components.

The `planned_characters` column uses the exact same `String`-typed JSON pattern as `keyEvents`, `foreshadowingSetup`, and `foreshadowingPayoff` on the same entity. The `character_arcs` column already exists in the database (`sql/init.sql` line 381) but has never been mapped in the Java entity. The duplicate `ChapterPlanXmlDto` in `common.xml` has zero imports across the entire codebase (verified by grep), making it safe to delete. The three-tier name matching is extracted from the proven `WorldviewXmlParser.findRegionIdByName()` pattern with a parameterized suffix list for character honorifics.

**Primary recommendation:** Follow existing JSON column patterns exactly. No new dependencies. All changes are additive and backward-compatible.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** planned_characters JSON schema -- 5 fields: characterName (required), roleType (required: protagonist/supporting/antagonist/npc), roleDescription (optional), importance (optional: high/medium/low), characterId (nullable, filled after name match)
- **D-02:** JSON column uses String type (consistent with keyEvents, foreshadowingSetup), MyBatis-Plus auto-maps
- **D-03:** characterId null on match failure, characterName preserved
- **D-04:** Keep `com.aifactory.dto.ChapterPlanXmlDto` (has Schema annotations, actively used by ChapterGenerationTaskStrategy), delete `com.aifactory.common.xml.ChapterPlanXmlDto` (zero imports)
- **D-05:** common.xml version's volumeNumber field NOT migrated -- chapter plans use volumePlanId FK instead
- **D-06:** Extract generic NameMatchUtil -- parameterized three-tier matching (input: name list + target name + suffix list), reusable for characters, factions, regions
- **D-07:** Character name suffix list: 公子/小姐/大哥/大姐/师傅/师叔/长老/前辈/道友/兄弟/妹妹 and similar honorifics
- **D-08:** Phase 11 only implements character matching, does NOT refactor WorldviewXmlParser's existing matching logic (can delegate to NameMatchUtil later)
- **D-09:** character_arcs -- only map field in NovelChapterPlan entity (String type, maps to existing DB column)
- **D-10:** character_arcs data population NOT in Phase 11 scope

### Claude's Discretion
- NameMatchUtil package location (common/util or common/xml)
- Complete enumeration of character name suffixes
- JSON serialization/deserialization details for planned_characters
- Whether ChapterPlanDto (response DTO) should also expose plannedCharacters field

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CP-03 | novel_chapter_plan entity maps planned_characters JSON field and existing character_arcs JSON field | Entity follows String-typed JSON pattern (same as keyEvents). planned_characters needs ALTER TABLE + entity field. character_arcs only needs entity field (column exists). |
| CP-04 | System uses three-tier name matching (exact -> strip suffix -> contains) to associate AI output character names with existing DB characters | Extract proven pattern from WorldviewXmlParser.findRegionIdByName() into parameterized NameMatchUtil. Character suffixes differ from region suffixes. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.0 | Backend framework | Already in use |
| MyBatis-Plus | 3.5.5 | ORM, auto-maps DB columns to entity fields | Already in use, handles String-typed JSON columns natively |
| Lombok | (managed) | @Data annotation for entity/DTO boilerplate | Already on all entities and DTOs |
| Jackson ObjectMapper | (managed by Spring Boot) | JSON parse/serialize for planned_characters field | Already injected in service classes |
| JUnit 5 + Mockito | (spring-boot-starter-test) | Unit testing | Already in pom.xml, existing tests prove pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MySQL 8.0+ | 8.0+ | ALTER TABLE for planned_characters column | Migration SQL |
| SpringDoc OpenAPI | 2.3.0 | @Schema annotations on DTO fields | ChapterPlanDto field documentation |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| String type + manual JSON parse | MyBatis-Plus JSON TypeHandler | TypeHandler adds complexity, String is the established pattern in this entity (keyEvents, etc.) |
| New NameMatchUtil class | Static methods in existing service | Static methods can't be mocked for testing; utility class follows common/ package pattern |

**Installation:**
No new dependencies needed. All libraries already in pom.xml.

**Version verification:** Not applicable -- no new packages introduced.

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-backend/src/main/java/com/aifactory/
├── entity/NovelChapterPlan.java          # ADD: plannedCharacters, characterArcs fields
├── dto/ChapterPlanXmlDto.java            # KEEP (active DTO with Schema annotations)
├── dto/ChapterPlanDto.java               # ADD: plannedCharacters, characterArcs fields
├── common/xml/ChapterPlanXmlDto.java     # DELETE (zero references)
├── common/NameMatchUtil.java             # NEW: three-tier name matching utility
└── mapper/NovelChapterPlanMapper.java    # NO CHANGE (standard BaseMapper)

sql/init.sql                              # ADD: planned_characters column to novel_chapter_plan
```

### Pattern 1: String-Typed JSON Column (Established)
**What:** MyBatis-Plus maps MySQL JSON columns to Java `String` fields. Business layer uses `ObjectMapper` to parse.
**When to use:** Every JSON column in this project (keyEvents, foreshadowingSetup, foreshadowingPayoff).
**Example:**
```java
// Entity field -- identical to existing keyEvents pattern
/**
 * 规划角色（JSON格式）
 */
private String plannedCharacters;

/**
 * 人物弧光变化（JSON格式）
 */
private String characterArcs;
```

### Pattern 2: Three-Tier Chinese Name Matching
**What:** Parameterized matching: exact -> strip configurable suffixes -> contains.
**When to use:** Matching AI-generated names to database names where AI may add honorifics.
**Example:**
```java
// Source: Extracted from WorldviewXmlParser lines 603-660
public static Long findMatchByName(List<NameEntry> candidates, String target, List<String> suffixes) {
    if (target == null || target.isEmpty()) return null;

    // Tier 1: Exact match
    for (NameEntry c : candidates) {
        if (target.equals(c.getName())) return c.getId();
    }

    // Tier 2: Strip suffixes and compare
    String strippedTarget = stripSuffixes(target, suffixes);
    for (NameEntry c : candidates) {
        String strippedCandidate = stripSuffixes(c.getName(), suffixes);
        if (!strippedTarget.isEmpty() && strippedTarget.equals(strippedCandidate)) return c.getId();
    }

    // Tier 3: Contains match (either direction)
    for (NameEntry c : candidates) {
        if (c.getName().contains(target) || target.contains(c.getName())) return c.getId();
    }

    return null;
}
```

### Pattern 3: DTO Merge (Delete Unused Duplicate)
**What:** Remove a class with zero imports, no migration needed.
**When to use:** When a duplicate DTO has no references.
**Action:** Delete `common/xml/ChapterPlanXmlDto.java`. No import changes needed (verified: zero references).

### Anti-Patterns to Avoid
- **Adding a TypeHandler for JSON columns:** The project uses String + manual ObjectMapper everywhere. Introducing a TypeHandler creates inconsistency.
- **Matching single-character names via contains:** "李" contains-matching "李云" is valid, but "云" contains-matching "李云" creates false positives. The contains check should only match target-in-candidate or candidate-in-target, not arbitrary substring.
- **Refactoring WorldviewXmlParser in this phase:** D-08 explicitly defers this. Only create NameMatchUtil for new character matching.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON serialization | Custom JSON builder | Jackson ObjectMapper | Already injected, handles edge cases |
| Entity field mapping | Custom ResultSet mapping | MyBatis-Plus auto-mapping | Already handles snake_case -> camelCase |
| DB migration | Manual schema sync | ALTER TABLE in init.sql + migration script | Follows project convention |

**Key insight:** This phase is purely additive. Every change mirrors an existing pattern. The main risk is the name matching suffix list completeness, not architecture.

## Common Pitfalls

### Pitfall 1: Tier 3 Contains Matching False Positives
**What goes wrong:** "李" matches "李云" via contains, but also "张李" via contains, creating wrong associations.
**Why it happens:** Single-character names or very short names match too broadly.
**How to avoid:** (1) Only apply Tier 3 when target length >= 2 or candidate length >= 2. (2) Prefer target-in-candidate over candidate-in-target for short names. (3) Log all Tier 3 matches at WARN level for review.
**Warning signs:** characterId populated but character has a completely different surname.

### Pitfall 2: common.xml ChapterPlanXmlDto Deletion Breaks IDE Caches
**What goes wrong:** IDE still indexes the deleted class; team members see phantom compile errors.
**Why it happens:** IDE caches don't auto-refresh on file deletion in some configurations.
**How to avoid:** After deletion, run `mvn compile` to verify clean build. Document the deletion in commit message.
**Warning signs:** Compile succeeds but IDE shows red squiggles on unrelated files.

### Pitfall 3: character_arcs vs planned_characters Semantic Confusion
**What goes wrong:** Future developers confuse the two fields, populating the wrong one.
**Why it happens:** Both are JSON columns on the same table containing per-character data.
**How to avoid:** Add clear Javadoc to both fields distinguishing them. `plannedCharacters` = "pre-generation planning (who should appear)". `characterArcs` = "post-generation tracking (how characters changed)".
**Warning signs:** Phase 12 or 13 writes to character_arcs when it should write to plannedCharacters.

### Pitfall 4: Suffix List Incomplete for Character Honorifics
**What goes wrong:** AI outputs "李大哥" but "大哥" is not in the suffix list, so Tier 2 fails and falls through to a potentially wrong Tier 3 match.
**Why it happens:** Chinese honorifics are open-ended; the initial list will miss some.
**How to avoid:** (1) Start with a comprehensive list covering common xianxia/wuxia honorifics. (2) Make the suffix list configurable (static final List, easy to extend). (3) Log all Tier 2 matches so missing suffixes can be identified from production data.
**Warning signs:** Many names reaching Tier 3 that should have matched at Tier 2.

### Pitfall 5: ChapterPlanDto Not Updated
**What goes wrong:** Entity gets new fields but response DTO doesn't, so API consumers never see the data.
**Why it happens:** Entity and DTO are separate classes with separate field lists.
**How to avoid:** Add plannedCharacters and characterArcs to ChapterPlanDto simultaneously. Verify with a manual API call that the response includes the fields.
**Warning signs:** Frontend Phase 14 can't access planned_characters despite entity having it.

## Code Examples

### NovelChapterPlan Entity Field Additions
```java
// Source: Following keyEvents/foreshadowingSetup pattern in NovelChapterPlan.java

/**
 * 规划角色（JSON格式）
 * 存储 AI 章节规划中计划登场的角色列表
 * Schema: [{"characterName":"李云","roleType":"protagonist","roleDescription":"...","importance":"high","characterId":42}]
 * characterId 为 null 表示名称匹配失败，角色名保留在 characterName 中
 */
private String plannedCharacters;

/**
 * 人物弧光变化（JSON格式）
 * 已有数据库列（character_arcs），本次补充 Java 实体映射
 * 存储 AI 生成章节后角色的状态/心态转变
 */
private String characterArcs;
```

### ChapterPlanDto Field Additions (Claude's Discretion)
```java
// Source: Following existing pattern in ChapterPlanDto.java

@Schema(description = "规划角色列表，JSON格式，包含 AI 规划中计划登场的角色信息")
private String plannedCharacters;

@Schema(description = "人物弧光变化，JSON格式，记录角色在本章的状态/心态转变")
private String characterArcs;
```

### NameMatchUtil Core Method
```java
// Source: Extracted from WorldviewXmlParser.findRegionIdByName() lines 603-627
// Parameterized for reuse with different entity types and suffix lists

/**
 * Generic three-tier Chinese name matching utility.
 * Tier 1: Exact match
 * Tier 2: Strip suffixes then exact match
 * Tier 3: Contains match (either direction)
 *
 * @param candidates list of NamedEntity (has getName() and getId())
 * @param targetName the name to search for
 * @param suffixes   suffixes to strip for Tier 2 (e.g., ["公子", "小姐", "师傅"])
 * @param <T>        entity type with name and id
 * @return matching entity ID, or null if no match
 */
public static <T> Long matchByName(List<T> candidates, String targetName, List<String> suffixes) {
    // Implementation follows WorldviewXmlParser pattern
}
```

### SQL Migration
```sql
-- Add planned_characters column to novel_chapter_plan
ALTER TABLE novel_chapter_plan
ADD COLUMN planned_characters json DEFAULT NULL COMMENT '规划角色（JSON格式，存储 AI 规划中计划登场的角色列表）'
AFTER foreshadowing_actions;
```

### planned_characters JSON Schema
```json
[
  {
    "characterName": "李云",
    "roleType": "protagonist",
    "roleDescription": "发现密室线索，推动主线剧情",
    "importance": "high",
    "characterId": 42
  },
  {
    "characterName": "神秘老者",
    "roleType": "supporting",
    "roleDescription": "提供关键情报",
    "importance": "medium",
    "characterId": null
  }
]
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Unmapped character_arcs column | Map to entity field | Phase 11 (now) | Zero-cost: column exists, just add field |
| Inline three-tier matching in WorldviewXmlParser | Extract to reusable NameMatchUtil | Phase 11 (now) | WorldviewXmlParser not refactored yet (D-08) |
| Two ChapterPlanXmlDto classes | Single DTO in dto package | Phase 11 (now) | common.xml version deleted, zero references |

**Deprecated/outdated:**
- `com.aifactory.common.xml.ChapterPlanXmlDto`: Zero imports, replaced by `com.aifactory.dto.ChapterPlanXmlDto`. Delete in this phase.

## Open Questions

1. **NameMatchUtil package location (Claude's Discretion)**
   - What we know: `common/` package has utility classes (PasswordUtil, TokenUtil, UserContext, XmlParser). `common/xml/` has XML DTOs.
   - What's unclear: Whether a generic name matching utility belongs in `common/` (with other utilities) or `common/xml/` (near WorldviewXmlParser).
   - Recommendation: Place in `com.aifactory.common.NameMatchUtil` -- it is a general utility not specific to XML parsing. WorldviewXmlParser can delegate to it in a future phase.

2. **Character suffix list completeness (Claude's Discretion)**
   - What we know: WorldviewXmlParser uses region suffixes like 宗/派/门/殿/阁. Character honorifics are a different set: 公子/小姐/大哥/大姐/师傅/师叔/长老/前辈/道友/兄弟/妹妹.
   - What's unclear: The full extent of xianxia/wuxia honorifics the AI might produce.
   - Recommendation: Start with 15-20 common suffixes, make the list a `static final List<String>` for easy extension. Add logging to identify missing suffixes in production.

3. **ChapterPlanDto exposure (Claude's Discretion)**
   - What we know: Success criterion #2 says "API response should include planned_characters and character_arcs fields". The DTO is the API response object.
   - Recommendation: Add both fields to ChapterPlanDto. This is low-risk and directly satisfies the success criterion.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | Backend compilation | Yes | 21.0.10 | -- |
| Maven 3.9 | Build and test | Yes | 3.9.14 | -- |
| MySQL 8.0+ | ALTER TABLE execution | Yes | Running | -- |
| JUnit 5 + Mockito | Unit testing | Yes | spring-boot-starter-test | -- |

**Missing dependencies with no fallback:**
None -- all dependencies available.

**Missing dependencies with fallback:**
None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (via spring-boot-starter-test) |
| Config file | None (annotation-based: @ExtendWith(MockitoExtension.class)) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=NameMatchUtilTest -q` |
| Full suite command | `cd ai-factory-backend && mvn test -q` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CP-03 | NovelChapterPlan has plannedCharacters and characterArcs fields | unit | `mvn test -Dtest=NovelChapterPlanTest -q` | No -- Wave 0 |
| CP-03 | ChapterPlanDto includes plannedCharacters and characterArcs fields | unit | `mvn test -Dtest=ChapterPlanDtoTest -q` | No -- Wave 0 |
| CP-04 | Exact name match returns correct ID | unit | `mvn test -Dtest=NameMatchUtilTest -q` | No -- Wave 0 |
| CP-04 | Suffix-stripped match works for character honorifics | unit | `mvn test -Dtest=NameMatchUtilTest -q` | No -- Wave 0 |
| CP-04 | Contains match works as fallback | unit | `mvn test -Dtest=NameMatchUtilTest -q` | No -- Wave 0 |
| CP-04 | No match returns null | unit | `mvn test -Dtest=NameMatchUtilTest -q` | No -- Wave 0 |
| SC-4 | common.xml.ChapterPlanXmlDto deleted, compile succeeds | compile | `mvn compile -q` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn test -q`
- **Per wave merge:** `cd ai-factory-backend && mvn test -q`
- **Phase gate:** Full suite green + `mvn compile -q` (verify DTO deletion clean compile)

### Wave 0 Gaps
- [ ] `ai-factory-backend/src/test/java/com/aifactory/common/NameMatchUtilTest.java` -- covers CP-04 (three-tier matching: exact, suffix-strip, contains, no-match, null input, single-char edge cases)
- [ ] `ai-factory-backend/src/test/java/com/aifactory/entity/NovelChapterPlanTest.java` -- covers CP-03 (entity field existence and type)
- [ ] SQL migration script for planned_characters column

## Sources

### Primary (HIGH confidence)
- Direct codebase analysis: `NovelChapterPlan.java` -- entity with String-typed JSON fields (keyEvents, foreshadowingSetup, foreshadowingPayoff)
- Direct codebase analysis: `sql/init.sql` lines 360-386 -- novel_chapter_plan table schema, character_arcs already exists at line 381
- Direct codebase analysis: `WorldviewXmlParser.java` lines 591-660 -- proven three-tier matching pattern (findRegionIdByName, findPowerSystemIdByName)
- Direct codebase analysis: `ChapterGenerationTaskStrategy.java` line 4 -- imports `com.aifactory.dto.ChapterPlanXmlDto`, only consumer
- Grep verification: `common.xml.ChapterPlanXmlDto` has zero import references across entire codebase
- Direct codebase analysis: `WorldviewXmlParserTest.java` -- test pattern for name matching tests (exact, suffix, contains, no-match, null)
- Direct codebase analysis: `ChapterPlanDto.java` -- response DTO with Schema annotations pattern
- Direct codebase analysis: `ChapterCharacterExtractService.java` line 489 -- existing isSameCharacter (exact match only)
- Maven build verification: `mvn test -q` passes (5 test classes, 0 failures)

### Secondary (MEDIUM confidence)
- Project research summary: `.planning/research/SUMMARY.md` -- confirmed no new dependencies needed
- Project pitfalls: `.planning/research/PITFALLS-v1.0.5.md` -- Pitfall 8 (character_arcs vs planned_characters) resolved by D-09/D-10

### Tertiary (LOW confidence)
None -- all findings verified against codebase.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all patterns verified in production codebase
- Architecture: HIGH -- every change mirrors an existing pattern on the same entity/table
- Pitfalls: HIGH -- all pitfalls identified from direct codebase analysis with specific line references
- Name matching: HIGH -- proven pattern from WorldviewXmlParser, adapted for character suffixes

**Research date:** 2026-04-07
**Valid until:** 2026-05-07 (stable -- no external dependencies, all patterns internal to codebase)
