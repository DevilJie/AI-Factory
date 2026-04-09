# Phase 12: AI 规划输出 + XML 解析 - Research

**Researched:** 2026-04-08
**Domain:** XML parsing (DOM), prompt template engineering, name matching
**Confidence:** HIGH

## Summary

Phase 12 transforms the chapter planning pipeline to produce and consume structured character planning data. Currently, `ChapterGenerationTaskStrategy.parseChaptersXml()` uses Jackson XML (`ChapterPlanXmlDto`) to parse chapter plan XML -- but this approach fails on nested same-name tags like multiple `<ch>` elements inside `<o>`. The project already solved this exact problem in `WorldviewXmlParser` using DOM parsing (v1.0.2-v1.0.3). This phase replicates that proven pattern for chapter plan parsing.

The three major modifications are: (1) refactoring `parseChaptersXml()` from Jackson XML to DOM, adding `<ch>` tag extraction alongside existing chapter fields; (2) upgrading prompt template `llm_outline_chapter_generate` (template_id=6) to instruct the AI to output character planning XML tags and to receive a full character list injection; (3) wiring the parsed character data through `NameMatchUtil` for ID resolution and persisting as `planned_characters` JSON.

All building blocks exist: `WorldviewXmlParser` provides the DOM parsing pattern, `NameMatchUtil` handles three-tier Chinese name matching, `NovelChapterPlan.plannedCharacters` field is already mapped (Phase 11), and `NovelCharacterService.getNonNpcCharacters()` fetches non-NPC characters for injection.

**Primary recommendation:** Follow WorldviewXmlParser's DOM parsing pattern exactly. Add a `parseChaptersXml` method in `ChapterGenerationTaskStrategy` that uses `javax.xml.parsers.DocumentBuilder`, extracts both existing chapter fields and new `<ch>` character tags, and returns a data structure compatible with `saveChaptersToDatabase`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Switch to DOM parsing -- follow WorldviewXmlParser pattern, replace current Jackson XML (ChapterPlanXmlDto) parsing. Project constraint confirms Jackson XML cannot handle nested same-name tags.
- **D-02:** DOM parsing extracts chapter fields + character data in one pass. Convert to existing Map format (compatible with saveChaptersToDatabase), with additional plannedCharacters JSON.
- **D-03:** Full character injection -- inject all project characters (name + roleType + faction) into planning template. Reuse existing `{characterInfo}` variable slot.
- **D-04:** Concise instruction + format constraint in template -- add "each chapter must output character list" instruction + character planning XML tag format (name, role description, importance). No excessive planning philosophy guidance.
- **D-05:** Allow new characters -- prompt states "can use existing characters or plan new characters as needed". New character names get characterId=null, characterName preserved.
- **D-06:** Character planning tags inside each `<o>` chapter tag -- `<ch>` wraps each planned character with `<cn>` (name), `<cd>` (description/role), `<ci>` (importance: high/medium/low).
- **D-07:** Character planning tags are optional -- some chapters may have no specific characters. Parse gracefully handles empty/missing.
- **D-08:** Match immediately after parsing -- call NameMatchUtil.matchByName() for each character name. Matched -> characterId set. Failed -> characterId=null.
- **D-09:** Persist as planned_characters JSON per Phase 11 schema: `[{"characterName":"...","roleType":"...","roleDescription":"...","importance":"...","characterId":42}]`. roleType defaults to "supporting" if not inferrable.

### Claude's Discretion
- DOM parsing implementation location (reuse WorldviewXmlParser or create new parsing method)
- Character list injection formatting details (text list vs XML format)
- parseChaptersXml return type after refactoring (keep Map format or new data structure)
- ChapterPlanXmlDto retention (DOM parsing may make it unnecessary)
- roleType field: whether AI outputs it or system defaults

### Deferred Ideas (OUT OF SCOPE)
None.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CP-01 | User generates chapter plan, AI output includes character list (name, role description, importance) | Template upgrade (D-03/D-04/D-06) adds character XML tags to prompt output. DOM parsing (D-01/D-02) extracts `<ch>` tags from AI response. |
| CP-02 | User views chapter plan and sees parsed/persisted planned character info (planned_characters JSON field) | NameMatchUtil matching (D-08) resolves characterId. JSON persistence (D-09) stores to novel_chapter_plan.planned_characters. NovelChapterPlan entity already has field (Phase 11). |
</phase_requirements>

## Standard Stack

### Core (Existing - No New Dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| javax.xml.parsers (JDK built-in) | JDK 21 | DOM XML parsing | Built into JDK, already used in WorldviewXmlParser. No external dependency needed. |
| org.w3c.dom (JDK built-in) | JDK 21 | DOM node traversal | Standard Java XML API, proven in this project for nested same-name tags. |
| NameMatchUtil | Phase 11 | Three-tier Chinese name matching | Already implemented and tested. Direct reuse. |
| Jackson ObjectMapper | 2.x | planned_characters JSON serialization | Already used throughout the project for JSON field handling. |
| PromptTemplateService | Existing | Template variable substitution | Already used in buildChapterPromptUsingTemplate(). |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| NovelCharacterService | Existing | Fetch non-NPC characters for injection | getNonNpcCharacters(projectId) returns List<NovelCharacter> |
| WorldviewXmlParser.sanitizeXmlForDomParsing | Existing | Fix common LLM XML issues | Must use before DOM parsing (handles raw &, unmatched tags) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| DOM parsing inside ChapterGenerationTaskStrategy | Add method to WorldviewXmlParser | WorldviewXmlParser is already a @Component. Adding chapter plan parsing there keeps all DOM parsing centralized. However, chapter plan parsing is fundamentally different (returns Map list, not entity tree), so a dedicated method in ChapterGenerationTaskStrategy keeps concerns separated. RECOMMEND: Keep in ChapterGenerationTaskStrategy but extract sanitization to a shared utility. |
| Keep ChapterPlanXmlDto + add character list field | Full DOM replacement | Jackson XML will fail on multiple `<ch>` inside `<o>` (same bug as nested `<r>` in geography). DOM is the only reliable approach. |

**Installation:**
No new packages needed. All dependencies are existing JDK or project libraries.

## Architecture Patterns

### Recommended Modification Points

```
ChapterGenerationTaskStrategy.java
  +-- parseChaptersXml()          -- REWRITE: Jackson XML -> DOM parsing
  +-- saveChaptersToDatabase()    -- MODIFY: add plannedCharacters field
  +-- buildChapterPromptUsingTemplate()  -- MODIFY: inject character list into characterInfo variable
  +-- generateChaptersForVolume() -- MODIFY: fetch character list for injection

WorldviewXmlParser.java
  +-- sanitizeXmlForDomParsing()  -- CONSIDER: make package-private or extract to shared util
```

### Pattern 1: DOM Parsing for Chapter Plans (Follow WorldviewXmlParser)

**What:** Replace Jackson XML parsing with DOM parsing to handle nested same-name `<ch>` tags.
**When to use:** This is the locked decision (D-01).
**Example:**

```java
// Source: Based on WorldviewXmlParser.parseGeographyXml pattern
private List<Map<String, String>> parseChaptersXml(String xmlStr) {
    try {
        // 1. Extract <c>...</c> fragment
        int start = xmlStr.indexOf("<c>");
        int end = xmlStr.indexOf("</c>");
        if (start < 0 || end < 0) {
            log.error("未找到 <c> 章节规划标签");
            return null;
        }
        String chapterXml = "<root>" + xmlStr.substring(start, end + 4) + "</root>";

        // 2. Sanitize for DOM parsing (handle raw &, unmatched tags)
        chapterXml = sanitizeXmlForDomParsing(chapterXml, new String[]{"o", "ch"});

        // 3. Parse with DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(chapterXml)));

        Element root = doc.getDocumentElement();
        // Find <c> element using getChildNodes() (NOT getElementsByTagName)
        Element cElement = null;
        NodeList rootChildren = root.getChildNodes();
        for (int i = 0; i < rootChildren.getLength(); i++) {
            Node node = rootChildren.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "c".equals(node.getNodeName())) {
                cElement = (Element) node;
                break;
            }
        }
        if (cElement == null) return null;

        // 4. Parse each <o> chapter node
        List<Map<String, String>> chapters = new ArrayList<>();
        NodeList oNodes = cElement.getChildNodes();
        for (int i = 0; i < oNodes.getLength(); i++) {
            Node oNode = oNodes.item(i);
            if (oNode.getNodeType() == Node.ELEMENT_NODE && "o".equals(oNode.getNodeName())) {
                Map<String, String> chapter = parseSingleChapter((Element) oNode);
                if (chapter != null) chapters.add(chapter);
            }
        }
        return chapters;
    } catch (Exception e) {
        log.error("DOM解析章节规划失败", e);
        return null;
    }
}
```

### Pattern 2: Character Tag Extraction Inside Chapter Node

**What:** Extract `<ch>` character tags from each `<o>` chapter element, serialize to JSON string.
**When to use:** After parsing chapter fields, iterate child nodes for `<ch>` tags.

```java
// Source: Based on WorldviewXmlParser.parseSingleRegion pattern
private List<Map<String, Object>> parseCharacterTags(Element oElement) {
    List<Map<String, Object>> characters = new ArrayList<>();
    NodeList children = oElement.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() == Node.ELEMENT_NODE && "ch".equals(child.getNodeName())) {
            Map<String, Object> character = new HashMap<>();
            NodeList chChildren = child.getChildNodes();
            for (int j = 0; j < chChildren.getLength(); j++) {
                Node chChild = chChildren.item(j);
                if (chChild.getNodeType() != Node.ELEMENT_NODE) continue;
                String tag = chChild.getNodeName();
                switch (tag) {
                    case "cn" -> character.put("characterName", chChild.getTextContent().trim());
                    case "cd" -> character.put("roleDescription", chChild.getTextContent().trim());
                    case "ci" -> character.put("importance", chChild.getTextContent().trim());
                }
            }
            if (!character.isEmpty()) {
                characters.add(character);
            }
        }
    }
    return characters;
}
```

### Pattern 3: Name Matching After Parse

**What:** Use NameMatchUtil to resolve character IDs from parsed names.
**When to use:** After extracting character tags, before persisting.

```java
// Key consideration: NovelCharacter does NOT implement NamedEntity.
// Options:
// 1. Add "implements NameMatchUtil.NamedEntity" to NovelCharacter
// 2. Create an adapter/wrapper
// RECOMMENDATION: Option 1 is simplest -- NovelCharacter already has getName() and getId().
// Only change: add "implements NameMatchUtil.NamedEntity" to class declaration.

// Usage pattern:
List<NovelCharacter> allCharacters = novelCharacterService.getNonNpcCharacters(projectId);
for (Map<String, Object> charData : parsedCharacters) {
    String name = (String) charData.get("characterName");
    Long matchedId = NameMatchUtil.matchByName(allCharacters, name, NameMatchUtil.CHARACTER_SUFFIXES);
    charData.put("characterId", matchedId);
    charData.put("roleType", "supporting"); // default per D-09
}
```

### Pattern 4: Character List Injection for Prompt

**What:** Format character data for template injection.
**When to use:** In `buildChapterPromptUsingTemplate()` or `generateChaptersForVolume()`.

```java
// Format: each line has name (roleType, faction info)
// Example: "李云 (protagonist, 青云门弟子)"
// Reuses existing {characterInfo} template variable

List<NovelCharacter> characters = novelCharacterService.getNonNpcCharacters(projectId);
StringBuilder charInfo = new StringBuilder();
charInfo.append("以下为本项目已有角色列表：\n");
for (NovelCharacter c : characters) {
    charInfo.append("- ").append(c.getName());
    charInfo.append(" (").append(c.getRoleType());
    // Faction info could be added from CharacterDto data
    charInfo.append(")\n");
}
charInfo.append("\n你可以使用以上已有角色，也可以根据情节需要安排新角色登场。");
variables.put("characterInfo", charInfo.toString());
```

### Anti-Patterns to Avoid

- **Using getElementsByTagName for nested tags:** This returns ALL descendant elements, not just direct children. When parsing `<o>` nodes, use `getChildNodes()` and filter by node name. This was the key fix in WorldviewXmlParser.
- **Forgetting XML sanitization:** LLM output frequently contains raw `&` and unmatched tags. Always run `sanitizeXmlForDomParsing` before DOM parsing.
- **Not handling optional `<ch>` tags:** Per D-07, chapters may have no characters. Return empty list, not null. The JSON field should be an empty array `[]` or null, not cause a parse failure.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| XML sanitization | Custom regex cleanup | WorldviewXmlParser.sanitizeXmlForDomParsing() | Already handles raw &, unmatched tags, CDATA preservation |
| Chinese name matching | Custom string comparison | NameMatchUtil.matchByName() | Three-tier matching with suffix stripping, proven in Phase 11 |
| JSON serialization of planned_characters | Manual JSON string building | Jackson ObjectMapper.writeValueAsString() | Handles escaping, consistent with project patterns |

**Key insight:** All the hard problems (nested XML, Chinese name matching, XML sanitization) already have proven solutions in this codebase. This phase is primarily wiring them together.

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | ai_prompt_template_version table: template_id=6 currently has version id=6 active | SQL INSERT for new version (id=20+), then UPDATE ai_prompt_template set current_version_id |
| Live service config | None -- template versioning handled through DB, not UI | None |
| OS-registered state | None | None |
| Secrets/env vars | None | None |
| Build artifacts | None -- pure Java source changes | None |

**Nothing found in category "live service config":** Template changes go through SQL init scripts and PromptTemplateService.updateTemplate(). No UI or external config stores templates.

**Nothing found in category "OS-registered state":** Verified -- no task scheduler, systemd, or similar registrations.

## Common Pitfalls

### Pitfall 1: getElementsByTagName vs getChildNodes

**What goes wrong:** Using `getElementsByTagName("ch")` inside an `<o>` element returns ALL `<ch>` tags from ALL chapters if called on a parent, not just the ones in the current chapter.
**Why it happens:** `getElementsByTagName` searches the entire subtree, not just direct children.
**How to avoid:** Always use `getChildNodes()` with node type/name filtering, exactly as WorldviewXmlParser does.
**Warning signs:** Characters from chapter 1 appearing in chapter 3's data.

### Pitfall 2: XML Sanitization Omission

**What goes wrong:** SAXParseException on LLM output containing raw `&` or unmatched tags.
**Why it happens:** LLMs frequently generate XML with minor structural issues (raw ampersands in text, missing closing tags on long outputs).
**How to avoid:** Always call `sanitizeXmlForDomParsing()` before DOM parsing. This method is private in WorldviewXmlParser -- either extract it to a shared utility, or duplicate the essential logic.
**Warning signs:** "The content of elements must consist of well-formed character data" errors in logs.

### Pitfall 3: NovelCharacter Not Implementing NamedEntity

**What goes wrong:** `NameMatchUtil.matchByName()` requires `List<T extends NamedEntity>`. `NovelCharacter` has `getName()` and `getId()` but does not implement the interface.
**Why it happens:** `NovelCharacter` predates `NameMatchUtil` (Phase 11).
**How to avoid:** Add `implements NameMatchUtil.NamedEntity` to `NovelCharacter` class declaration. The methods already exist (Lombok `@Data` generates getName/getId). Only the `implements` clause is needed.
**Warning signs:** Compile error: "inferred type does not conform to upper bound(s)".

### Pitfall 4: Token Budget Exceeded

**What goes wrong:** Adding character list injection + character output instructions increases both input and output token usage, potentially causing output truncation.
**Why it happens:** Chapter planning already generates long XML per chapter. Adding `<ch>` tags for each character multiplies output length.
**How to avoid:** Keep character list injection concise (name + roleType only, no full descriptions). Limit to 1-3 characters per chapter in the template instruction. Consider reducing batch size from 5 to 3-4 if truncation observed.
**Warning signs:** Last chapters in a batch have truncated/incomplete XML.

### Pitfall 5: Template Version Management

**What goes wrong:** New template version created but old version remains active, so AI still uses old prompt without character instructions.
**Why it happens:** Template versioning requires both creating a new version record AND updating the template's current_version_id.
**How to avoid:** Follow the established pattern: INSERT new version into `ai_prompt_template_version`, then UPDATE `ai_prompt_template` SET `current_version_id = <new_id>` WHERE id = 6.
**Warning signs:** AI output has no `<ch>` tags despite code expecting them.

## Code Examples

### Complete DOM Parsing for Single Chapter with Characters

```java
// Source: Adapted from WorldviewXmlParser.parseSingleRegion + parseFactionNode patterns
private Map<String, String> parseSingleChapter(Element oElement) {
    Map<String, String> chapter = new HashMap<>();
    List<Map<String, Object>> characters = new ArrayList<>();

    NodeList children = oElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() != Node.ELEMENT_NODE) continue;
        String tag = child.getNodeName();

        switch (tag) {
            case "n" -> {
                String num = child.getTextContent().trim();
                if (num.isEmpty()) return null; // skip chapters without number
                chapter.put("chapterNumber", num);
            }
            case "t" -> chapter.put("chapterTitle", child.getTextContent().trim());
            case "p" -> chapter.put("plotOutline", child.getTextContent().trim());
            case "e" -> chapter.put("keyEvents", child.getTextContent().trim());
            case "g" -> chapter.put("chapterGoal", child.getTextContent().trim());
            case "w" -> chapter.put("wordCountTarget", child.getTextContent().trim());
            case "s" -> chapter.put("chapterStartingScene", child.getTextContent().trim());
            case "f" -> chapter.put("chapterEndingScene", child.getTextContent().trim());
            case "ch" -> {
                // Character planning tag
                Map<String, Object> ch = parseSingleCharacterTag(child);
                if (ch != null) characters.add(ch);
            }
        }
    }

    if (!chapter.containsKey("chapterNumber")) return null;

    // Serialize character list to JSON string for storage
    if (!characters.isEmpty()) {
        try {
            chapter.put("plannedCharacters", objectMapper.writeValueAsString(characters));
        } catch (Exception e) {
            log.warn("序列化角色规划数据失败", e);
        }
    }

    return chapter;
}
```

### XML Sanitization (Reuse from WorldviewXmlParser)

```java
// WorldviewXmlParser.sanitizeXmlForDomParsing is private.
// Option A: Make it package-private and call from same package (not applicable here -- different packages)
// Option B: Extract to a shared XmlSanitizer utility class
// Option C: Duplicate the method in ChapterGenerationTaskStrategy
// RECOMMENDATION: Option B -- extract to a new XmlSanitizer utility class that both
// WorldviewXmlParser and ChapterGenerationTaskStrategy can use.
// However, since WorldviewXmlParser.sanitizeXmlForDomParsing is ~40 lines and well-tested,
// Option C (duplicate) is pragmatic if minimizing scope.
```

### Template Version Update SQL

```sql
-- 1. Insert new version for template_id=6
INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, version_comment, is_active, created_by, created_time)
VALUES (6, 2, '...updated template content with character XML instructions...',
        '...updated variable definitions adding characterInfo...',
        'v2: add character planning output instructions and XML tags', 1, NULL, NOW());

-- 2. Update template to use new version (replace <new_version_id> with actual ID)
UPDATE ai_prompt_template SET current_version_id = <new_version_id>, updated_time = NOW()
WHERE id = 6;
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Jackson XML (ChapterPlanXmlDto) | DOM parsing (javax.xml.parsers) | v1.0.2-v1.0.3 for WorldviewXmlParser | DOM handles nested same-name tags reliably |
| Per-task hardcoded XML parsing | WorldviewXmlParser centralized DOM | v1.0.3 | Single proven pattern for all worldview XML |
| No character data in chapter plans | planned_characters JSON field | Phase 11 (entity mapping) | DB column ready, this phase fills the data |

**Deprecated/outdated:**
- `ChapterPlanXmlDto`: After DOM migration, this DTO becomes unnecessary. The `common.xml.ChapterPlanXmlDto` was already deleted in Phase 11 (zero references). The `dto.ChapterPlanXmlDto` (active) will become dead code after this phase.

## Open Questions

1. **SanitizeXmlForDomParsing reuse**
   - What we know: The method is private in WorldviewXmlParser (~40 lines, well-tested). ChapterGenerationTaskStrategy is in a different package.
   - What's unclear: Whether to extract to shared utility or duplicate.
   - Recommendation: Extract to a shared `XmlSanitizer` utility class in `com.aifactory.common`. This keeps DRY and makes the sanitization testable independently. Both WorldviewXmlParser and ChapterGenerationTaskStrategy can then call `XmlSanitizer.sanitize()`.

2. **roleType inference**
   - What we know: D-09 says roleType defaults to "supporting". The AI does not output roleType in the `<ch>` tag format (D-06 only has cn/cd/ci).
   - What's unclear: Whether to try inferring roleType from matched character's existing roleType.
   - Recommendation: If NameMatchUtil finds a match, use the matched character's `roleType`. If no match (new character), default to "supporting". This is simple and leverages existing data.

3. **Batch size adjustment**
   - What we know: Current batch size is 5 chapters. Adding character output increases per-chapter XML length.
   - What's unclear: Whether this causes truncation in practice.
   - Recommendation: Keep batch size at 5 for now. Add a log warning if parsed chapters < expected batch size (early truncation detection). Adjust in a follow-up if needed.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | DOM parsing, compilation | Need to verify | -- | -- |
| Maven 3.8+ | Build | Need to verify | -- | -- |
| MySQL 8.0+ | Template version data | Need to verify | -- | -- |
| JUnit 5 | Testing | Verified (pom.xml) | -- | -- |
| Mockito | Testing | Verified (existing tests) | -- | -- |

**Skip detailed probe:** Phase is primarily Java source code changes using existing JDK APIs and project dependencies. No new external tools required. Existing test infrastructure (JUnit 5 + Mockito) is sufficient.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | None (annotations-based) |
| Quick run command | `cd ai-factory-backend && mvn test -Dtest=ChapterGenerationTaskStrategyTest -pl .` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CP-01 | DOM parsing extracts `<ch>` tags from chapter XML | unit | `mvn test -Dtest=ChapterPlanDomParserTest#testParseCharacterTags` | Wave 0 |
| CP-01 | DOM parsing handles chapters without `<ch>` tags gracefully | unit | `mvn test -Dtest=ChapterPlanDomParserTest#testParseNoCharacters` | Wave 0 |
| CP-01 | XML sanitization handles raw & in character descriptions | unit | `mvn test -Dtest=ChapterPlanDomParserTest#testSanitizeRawAmpersand` | Wave 0 |
| CP-02 | NameMatchUtil resolves character IDs from parsed names | unit | `mvn test -Dtest=NameMatchUtilTest` | Already exists |
| CP-02 | plannedCharacters JSON persisted to database | unit | `mvn test -Dtest=ChapterGenerationTaskStrategyTest#testSaveWithPlannedCharacters` | Wave 0 |
| CP-01/CP-02 | Full pipeline: XML -> parse -> match -> persist | integration | `mvn test -Dtest=ChapterGenerationTaskStrategyTest#testFullPipeline` | Wave 0 |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn test -Dtest="ChapterPlanDomParserTest,NameMatchUtilTest" -pl .`
- **Per wave merge:** `cd ai-factory-backend && mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `ChapterPlanDomParserTest.java` -- unit tests for DOM parsing of chapter XML with character tags (covers CP-01 parsing)
- [ ] `ChapterGenerationTaskStrategyTest.java` -- unit tests for saveChaptersToDatabase with plannedCharacters, character injection formatting (covers CP-02 persistence and CP-01 template injection)
- [ ] `XmlSanitizerTest.java` -- if extracting sanitization to shared utility (optional, existing tests in WorldviewXmlParserTest cover the logic)

## Sources

### Primary (HIGH confidence)
- `WorldviewXmlParser.java` -- proven DOM parsing pattern (820 lines, tested)
- `NameMatchUtil.java` -- three-tier matching (118 lines, tested)
- `ChapterGenerationTaskStrategy.java` -- current implementation (738 lines)
- `NovelChapterPlan.java` -- entity with plannedCharacters field (129 lines)
- `NovelCharacter.java` -- entity with getName()/getId() (102 lines)
- `sql/init.sql` -- template_id=6 definition (template version id=6)

### Secondary (MEDIUM confidence)
- `WorldviewXmlParserTest.java` -- test patterns for DOM parsing (511 lines, 20+ tests)
- `NameMatchUtilTest.java` -- test patterns for name matching (234 lines, 15+ tests)
- `PromptTemplateService.java` -- template execution interface

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all dependencies are existing and proven in this codebase
- Architecture: HIGH - WorldviewXmlParser provides exact pattern to follow
- Pitfalls: HIGH - identified from real bugs encountered in v1.0.2-v1.0.3

**Research date:** 2026-04-08
**Valid until:** 2026-05-08 (stable -- all based on existing codebase patterns)
