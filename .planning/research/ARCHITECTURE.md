# Architecture Research: 章节角色规划集成

**Domain:** Chapter Character Planning Integration for AI Novel Generation System
**Researched:** 2026-04-07
**Confidence:** HIGH (based on direct source code analysis, no external sources needed)

---

## Executive Summary

This research analyzes how to integrate chapter character planning into the existing AI Factory novel generation pipeline. The system currently follows a four-stage pipeline: VolumePlan -> ChapterPlan -> ChapterGenerate -> CharacterExtract. The new feature inserts character planning at the ChapterPlan stage, reads it at the ChapterGenerate stage, and creates a feedback loop with CharacterExtract.

The integration touches 5 backend files (MODIFY), 1 database table (MODIFY), 1 XML DTO (MODIFY), 1 prompt template (MODIFY), 1 new prompt template (ADD), 1 frontend component (MODIFY), 1 frontend type (MODIFY), and 1 frontend API file (MODIFY). No new tables, services, or major architectural components are needed -- this is primarily a field extension and prompt engineering change.

The key insight is that `novel_chapter_plan` already stores structured data (keyEvents as JSON, foreshadowingSetup/Payoff as text). Adding `planned_characters` as JSON follows an established pattern. The XML parsing in `ChapterPlanXmlDto` already uses Jackson XML annotations with single-letter tags, and adding character planning sub-tags requires only extending the existing `ChapterPlanItem` inner class.

---

## Current Architecture (Baseline)

### System Overview

```
+---------------------------------------------------------------------+
|                     Pipeline: Novel Generation                       |
+---------------------------------------------------------------------+
|                                                                      |
|  1. VolumePlan        VolumeOptimizeTaskStrategy                     |
|     (llm_outline_generate)         |                                 |
|                                                                      |
|  2. ChapterPlan       ChapterGenerationTaskStrategy                  |
|     (llm_outline_chapter_generate)  |                                |
|                                                                      |
|  3. ChapterGenerate   ChapterContentGenerateTaskStrategy             |
|     (llm_chapter_generate_standard)  |                               |
|                                                                      |
|  4. CharacterExtract  ChapterCharacterExtractService                 |
|     (llm_chapter_character_extract)                                  |
|                                                                      |
+---------------------------------------------------------------------+
```

### Component Responsibilities

| Component | Responsibility | Current Role |
|-----------|----------------|--------------|
| `NovelChapterPlan` entity | DB table mapping for chapter plans | Stores plotOutline, keyEvents, chapterGoal, scenes -- NO character fields |
| `ChapterPlanXmlDto` (dto) | Jackson XML DTO for parsing LLM output | Parses `<c><o>` structure with n/t/p/e/g/w/s/ed tags -- NO character tags |
| `ChapterPlanXmlDto` (common.xml) | Alternative XML DTO (volume-level) | Similar structure, used by OutlineTaskStrategy |
| `ChapterGenerationTaskStrategy` | Generates chapter plans via LLM | Calls `llm_outline_chapter_generate` template, parses XML, saves to DB |
| `ChapterContentGenerateTaskStrategy` | Generates chapter content from plans | Calls `PromptTemplateBuilder.buildChapterPrompt()` |
| `PromptTemplateBuilder` | Assembles all prompt variables | `buildCharacterPromptInfoList()` fetches ALL non-NPC characters with last appearance |
| `ChapterCharacterExtractService` | Post-generation character extraction | Parses chapter content, matches/creates characters, saves `novel_character_chapter` |
| `ChapterPlanDrawer.vue` | Frontend chapter plan detail editor | Shows title, summary, keyEvents, characters (comma-separated), foreshadowing |
| `ChapterInfo.vue` | Frontend chapter info sidebar | Shows title, wordCount, status, summary -- "characters" section is placeholder |
| `editor.ts` store | Frontend editor state management | `ChapterPlan` type has `characters?: string[]` (unused for planned characters) |

---

## Target Architecture (After Integration)

### System Overview

```
+----------------------------------------------------------------------+
|                  Pipeline: Novel Generation (Enhanced)                |
+----------------------------------------------------------------------+
|                                                                       |
|  1. VolumePlan        (unchanged)                                     |
|                                                                       |
|  2. ChapterPlan       ChapterGenerationTaskStrategy                   |
|     +----------------------------------------------------------+      |
|     | NEW: Template includes <ch><cn>name</cn><cr>role</cr>...|      |
|     | NEW: XML parser extracts character planning per chapter   |      |
|     | NEW: Saves planned_characters JSON to novel_chapter_plan  |      |
|     +----------------------------------------------------------+      |
|                                                                       |
|  3. ChapterGenerate   ChapterContentGenerateTaskStrategy              |
|     +----------------------------------------------------------+      |
|     | NEW: Reads planned_characters from chapter plan           |      |
|     | NEW: Injects planned characters as strict guidance        |      |
|     | MOD: Filters characterInfo to focus on planned characters |      |
|     +----------------------------------------------------------+      |
|                                                                       |
|  4. CharacterExtract  (unchanged, validates against plan)             |
|                                                                       |
|  5. Frontend          ChapterPlanDrawer / ChapterInfo                  |
|     +----------------------------------------------------------+      |
|     | NEW: Displays planned character list with roles/arcs      |      |
|     | NEW: Shows character badges in VolumeTree chapter items   |      |
|     +----------------------------------------------------------+      |
|                                                                       |
+----------------------------------------------------------------------+
```

### Data Flow: Character Planning

```
[ChapterPlan Stage]
    |
    +-- PromptTemplateService.executeTemplate("llm_outline_chapter_generate")
    |   +-- Template now includes: "For each chapter, plan which characters appear"
    |
    +-- LLM Response: XML with <ch> tags inside each <o>
    |   <o>
    |     <n>1</n><t>title</t><p>outline</p>...
    |     <ch>                          <-- NEW
    |       <cn>Li Yun</cn>            <-- character name
    |       <cr>protagonist</cr>       <-- role in chapter
    |       <ca>Confused youth entering the world</ca> <-- character arc
    |     </ch>                        <-- NEW
    |   </o>
    |
    +-- XmlParser.parse() -> ChapterPlanXmlDto
    |   +-- ChapterPlanItem now has List<PlannedCharacter> plannedCharacters
    |
    +-- ChapterGenerationTaskStrategy.saveChaptersToDatabase()
    |   +-- Serialize plannedCharacters to JSON -> chapterPlan.setPlannedCharacters(json)
    |
    +-- novel_chapter_plan.planned_characters = '[{"name":"...","role":"...","arc":"..."}]'

[ChapterGenerate Stage]
    |
    +-- ChapterContentGenerateTaskStrategy.generateContent()
    |   +-- Reads chapterPlan.getPlannedCharacters()
    |
    +-- PromptTemplateBuilder.buildTemplateVariables()
    |   +-- NEW: variables.put("plannedCharacters", buildPlannedCharactersText(plan))
    |   +-- MOD: variables.put("characterInfo", buildFocusedCharacterInfo(plan, allChars))
    |       +-- If plannedCharacters exists, only include those characters in detail
    |       +-- If plannedCharacters is empty, fall back to current behavior (all chars)
    |
    +-- Template "llm_chapter_generate_standard" uses ${plannedCharacters}
        +-- "STRICTLY follow this character plan: ..."
```

---

## Integration Points (Complete Inventory)

### Backend: MODIFY (5 files)

#### 1. `NovelChapterPlan.java` -- Entity

**File:** `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java`

**Change:** Add 1 field

```java
/**
 * Planned characters for this chapter (JSON format)
 * Format: [{"name":"Li Yun","role":"protagonist","arc":"Confused youth"},{"name":"Wang Gang","role":"supporting","arc":"Guide"}]
 */
private String plannedCharacters;
```

**Rationale:** Follows existing pattern of JSON fields (keyEvents, foreshadowingSetup). MySQL TEXT column is sufficient. No new table needed because this is plan-stage data, not relationship data.

**Impact:** Low. MyBatis-Plus auto-maps this field once the DB column exists.

---

#### 2. `ChapterPlanXmlDto.java` (dto package) -- XML DTO

**File:** `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanXmlDto.java`

**Change:** Add inner class + field to `ChapterPlanItem`

```java
@Data
public static class ChapterPlanItem {
    // ... existing fields ...

    /**
     * Planned appearing characters
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ch")
    private List<PlannedCharacter> plannedCharacters;

    @Data
    public static class PlannedCharacter {
        @JacksonXmlProperty(localName = "cn")
        private String name;       // Character name

        @JacksonXmlProperty(localName = "cr")
        private String role;       // Role/identity in chapter

        @JacksonXmlProperty(localName = "ca")
        private String arc;        // Scene synopsis
    }
}
```

**Rationale:** Uses same Jackson XML pattern as existing code. Single-letter tags (ch/cn/cr/ca) save tokens and follow convention (c=chapters, o=one, n=number, t=title, p=plot, e=events, g=goal, w=wordcount, s=start, f=end).

**Impact:** Low. The `@JacksonXmlElementWrapper(useWrapping = false)` pattern is already proven in `ChapterPlanXmlDto.chapters` and `ChapterCharacterExtractXmlDto`.

**Risk:** If LLM omits `<ch>` tags for some chapters, the list will be null/empty. This is acceptable -- the system already handles null fields gracefully (see `saveChaptersToDatabase` which uses `getOrDefault`).

---

#### 3. `ChapterGenerationTaskStrategy.java` -- Chapter Plan Strategy

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java`

**Change:** Modify `parseChaptersXml()` and `saveChaptersToDatabase()`

In `parseChaptersXml()`, after line 667 (chapterEndingScene extraction):
```java
// NEW: Extract planned characters
if (item.getPlannedCharacters() != null && !item.getPlannedCharacters().isEmpty()) {
    chapter.put("plannedCharacters",
        objectMapper.writeValueAsString(
            item.getPlannedCharacters().stream()
                .map(pc -> Map.of(
                    "name", pc.getName() != null ? pc.getName() : "",
                    "role", pc.getRole() != null ? pc.getRole() : "",
                    "arc", pc.getArc() != null ? pc.getArc() : ""
                ))
                .toList()
        ));
}
```

In `saveChaptersToDatabase()`, after line 727 (chapterEndingScene):
```java
// NEW: Save planned characters
if (chapterData.containsKey("plannedCharacters")) {
    chapterPlan.setPlannedCharacters(chapterData.get("plannedCharacters"));
}
```

**Rationale:** Minimal change. The existing pattern converts XML DTO fields to Map<String, String>, then maps to entity fields. Adding one more field follows this exact pattern.

**Impact:** Low. No structural change, just two small additions.

**Additional required change:** In `buildChapterPromptUsingTemplate()` (line 435):
```java
// OLD: variables.put("characterInfo", "No appearing characters yet");
// NEW: Inject actual character data so LLM knows which characters exist
List<CharacterPromptInfo> chars = promptTemplateBuilder
    .buildCharacterPromptInfoList(projectId, currentChapterNumber);
variables.put("characterInfo", buildSimpleCharacterList(chars));
```

This is critical: the chapter plan template currently hardcodes "No appearing characters yet", which means the LLM has no knowledge of existing characters when planning. Without this change, the LLM cannot produce meaningful character plans.

---

#### 4. `PromptTemplateBuilder.java` -- Prompt Builder

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java`

**Change:** Add 2 methods, modify `buildTemplateVariables()`

Add new method:
```java
/**
 * Build planned characters text from chapter plan
 * This text is injected as STRICT guidance for chapter generation
 */
private String buildPlannedCharactersText(NovelChapterPlan chapterPlan) {
    String json = chapterPlan.getPlannedCharacters();
    if (json == null || json.isEmpty()) return "";

    // Parse JSON and format as structured text
    // Output format:
    // [Planned Characters for This Chapter - STRICTLY Follow]
    // 1. Li Yun (protagonist) - Confused youth entering the martial world...
    // 2. Wang Gang (supporting) - Guide role, mentors protagonist in this chapter...
}
```

Add new method:
```java
/**
 * Build focused character info: only include characters from the plan
 * Falls back to all characters if plan is empty
 */
private String buildFocusedCharacterInfo(String plannedCharsJson,
                                          List<CharacterPromptInfo> allCharacters) {
    // Parse plannedCharsJson to extract names
    // Filter allCharacters to only include matching names (fuzzy match)
    // Return formatted text using existing buildCharacterInfoText()
}
```

Modify `buildTemplateVariables()` (around line 204):
```java
// NEW: Planned characters (if available)
String plannedCharactersText = buildPlannedCharactersText(chapterPlan);
variables.put("plannedCharacters", plannedCharactersText);

// MOD: If plan has characters, focus characterInfo on them
if (!plannedCharactersText.isEmpty()) {
    variables.put("characterInfo",
        buildFocusedCharacterInfo(chapterPlan.getPlannedCharacters(), characterPromptInfoList));
} else {
    variables.put("characterInfo", buildCharacterInfoText(characterPromptInfoList));
}
```

**Rationale:** The key design decision: when `plannedCharacters` exists, the chapter generation prompt should FOCUS on those specific characters, not dump ALL characters. This is the core value of the feature -- AI writes chapters that match the plan.

The `buildFocusedCharacterInfo()` method would:
1. Extract planned character names from the JSON
2. Filter `characterPromptInfoList` to only include matching characters (with fuzzy name matching)
3. Add extra emphasis in the text: "This character MUST appear in this chapter"

**Impact:** Medium. This is the most architecturally significant change because it changes the prompt construction logic. However, it is backward-compatible: if `plannedCharacters` is null/empty, the existing behavior is preserved.

---

#### 5. `ChapterPlanDto.java` -- API DTO

**File:** `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java`

**Change:** Add 1 field

```java
@Schema(description = "Planned characters for this chapter, JSON format")
private String plannedCharacters;
```

**Rationale:** Frontend needs this field to display planned characters. Straightforward addition.

---

### Backend: ADD (1 SQL migration)

#### 6. SQL Migration

```sql
ALTER TABLE novel_chapter_plan
ADD COLUMN planned_characters TEXT COMMENT 'Planned appearing characters (JSON format)' AFTER foreshadowing_payoff;
```

**Rationale:** TEXT column because JSON array of characters can be variable length. Placed after foreshadowing_payoff to keep JSON/text fields together.

---

### Backend: MODIFY (2 prompt templates, managed via DB)

#### 7. `llm_outline_chapter_generate` prompt template

**Change:** Add character planning instructions and XML format example

The template needs to:
1. Instruct the LLM to plan characters for each chapter
2. Add `<ch>` tag format to the XML example
3. Specify what each character planning field should contain

**Critical design decision:** The template should say "plan which EXISTING characters appear" (referencing the character list) rather than "create new characters." This prevents the LLM from inventing characters that do not exist in the system.

**Template variables available at chapter plan time:**
- `${characterInfo}` -- currently hardcoded to "No appearing characters yet" in ChapterGenerationTaskStrategy (line 435). This MUST change to inject actual character data.

---

#### 8. `llm_chapter_generate_standard` prompt template

**Change:** Add `${plannedCharacters}` variable usage

Add a section like:
```
${plannedCharacters}
```

When the variable is non-empty, it will contain:
```
[Character Plan for This Chapter - STRICTLY Follow]
The following characters MUST appear in this chapter, execute strictly as described:
1. Li Yun (protagonist) - Confused youth entering the martial world, curious about everything...
2. Wang Gang (supporting) - Guide role, mentors protagonist in this chapter...

Note: Strictly follow the above character plan, do not omit any planned characters.
```

When empty, the variable renders as empty string (no impact on existing behavior).

---

### Frontend: MODIFY (3 files)

#### 9. `ChapterPlanDrawer.vue`

**File:** `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue`

**Change:** Add planned characters display section

Replace or augment the current "Involved Characters" (comma-separated text input) with a structured display:

```html
<!-- Planned Characters (NEW) -->
<div v-if="plannedCharacters.length > 0">
  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
    Planned Characters
  </label>
  <div class="space-y-2">
    <div v-for="(char, index) in plannedCharacters" :key="index"
         class="flex items-start gap-2 p-2 bg-gray-50 dark:bg-gray-700 rounded-lg">
      <span class="text-sm font-medium text-gray-900 dark:text-white">{{ char.name }}</span>
      <span class="text-xs text-gray-500">{{ char.role }}</span>
      <p class="text-xs text-gray-600 dark:text-gray-400">{{ char.arc }}</p>
    </div>
  </div>
</div>
```

**Rationale:** Read-only display for AI-generated character plans. Users can see the plan but editing is future scope (user can regenerate the chapter plan).

---

#### 10. `project.ts` -- TypeScript Types

**File:** `ai-factory-frontend/src/types/project.ts`

**Change:** Add fields to `ChapterPlan` interface

```typescript
export interface ChapterPlan {
  // ... existing fields ...

  /** Planned appearing characters */
  plannedCharacters?: PlannedCharacter[]
}

export interface PlannedCharacter {
  name: string
  role: string      // protagonist / supporting / antagonist / npc
  arc: string       // Scene synopsis
}
```

Also add to `Chapter` interface:
```typescript
export interface Chapter {
  // ... existing fields ...
  plannedCharacters?: string  // JSON string from API
}
```

---

#### 11. `VolumeTree.vue`

**File:** `ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue`

**Change:** Add character count badge to chapter items (optional, can defer)

Minor visual enhancement: show a small badge with character count next to chapters that have planned characters.

```html
<span v-if="(chapter as any).plannedCharacters" class="text-xs text-purple-500">
  {{ JSON.parse((chapter as any).plannedCharacters).length }}chars
</span>
```

---

## Data Flow Diagrams

### Flow 1: Chapter Plan Generation (Enhanced)

```
User clicks "AI Generate Chapter Plan"
    |
    v
VolumeTree.vue -> generateChapters(projectId, volumeId, plotStage)
    |
    v
ChapterGenerationTaskStrategy.generateChaptersForVolume()
    |
    +-- Builds prompt using template "llm_outline_chapter_generate"
    |   +-- NEW: Injects ${characterInfo} with existing project characters
    |   +-- Template now requests <ch> tags for character planning
    |
    +-- LLM generates XML with <ch> tags
    |
    +-- parseChaptersXml() -> ChapterPlanXmlDto
    |   +-- NEW: Extracts plannedCharacters from <ch> tags
    |
    +-- saveChaptersToDatabase()
        +-- NEW: Sets plannedCharacters JSON on NovelChapterPlan entity
```

### Flow 2: Chapter Content Generation (Enhanced)

```
User clicks "Generate Chapter" for a specific plan
    |
    v
ChapterContentGenerateTaskStrategy.generateContent()
    |
    +-- Loads NovelChapterPlan (now includes plannedCharacters)
    |
    +-- PromptTemplateBuilder.buildChapterPrompt()
    |   +-- buildTemplateVariables()
    |   |   +-- NEW: buildPlannedCharactersText(chapterPlan) -> ${plannedCharacters}
    |   |   +-- MOD: buildFocusedCharacterInfo() -> ${characterInfo} (filtered)
    |   |
    |   +-- Template "llm_chapter_generate_standard" includes character plan
    |
    +-- LLM generates chapter content following character plan
    |
    +-- Saves chapter content to DB
    |
    +-- ChapterCharacterExtractService.extractCharacters()
        +-- (unchanged - validates actual characters against plan implicitly)
```

### Flow 3: Frontend Display (Enhanced)

```
User clicks chapter in VolumeTree
    |
    v
VolumeTree.vue -> handleChapterClick(chapter)
    |
    +-- editorStore.setChapterPlan(plan)  <-- plannedCharacters now included
    |
    +-- editorStore.loadChapterByPlan(planId)
    |
    v
ChapterPlanDrawer.vue
    +-- NEW: Displays planned characters with name/role/arc
```

---

## Build Order (Dependency-Aware)

The build order is determined by what each phase depends on. No phase should break existing functionality.

### Phase 1: Database + Entity (Foundation)

**Files:** SQL migration, `NovelChapterPlan.java`
**Why first:** Everything else depends on the data being stored somewhere.
**Risk:** Zero. Adding a nullable TEXT column does not affect any existing code.
**Test:** Verify MyBatis-Plus maps the new field correctly.

```
ALTER TABLE novel_chapter_plan
ADD COLUMN planned_characters TEXT COMMENT 'Planned appearing characters (JSON format)' AFTER foreshadowing_payoff;
```

```
NovelChapterPlan.java: add `private String plannedCharacters;` field
```

### Phase 2: XML DTO + Parsing (Plan Generation Input)

**Files:** `ChapterPlanXmlDto.java`, `ChapterGenerationTaskStrategy.java`
**Why second:** Needs the entity field to save into, but can be tested independently with unit tests.
**Dependency:** Phase 1 (entity must have the field).

```
ChapterPlanXmlDto.java:
  - Add PlannedCharacter inner class with cn/cr/ca tags
  - Add plannedCharacters field to ChapterPlanItem

ChapterGenerationTaskStrategy.java:
  - Modify parseChaptersXml() to extract plannedCharacters
  - Modify saveChaptersToDatabase() to save plannedCharacters
```

### Phase 3: Prompt Template Update (Plan Generation Output)

**Files:** `llm_outline_chapter_generate` template (DB), `ChapterGenerationTaskStrategy.java`
**Why third:** Needs XML parsing to work so generated data is actually saved.
**Dependency:** Phase 2 (XML parsing must handle new tags).

**Critical change in ChapterGenerationTaskStrategy:**
- Line 435: Replace `variables.put("characterInfo", "No appearing characters yet")` with actual character data injection
- This is necessary so the LLM knows which characters exist and can plan their appearances

```
llm_outline_chapter_generate template:
  - Add character planning instructions
  - Add <ch><cn><cr><ca> XML format example
  - Add instruction: "Reference the existing character list, plan appearing characters for each chapter"

ChapterGenerationTaskStrategy.buildChapterPromptUsingTemplate():
  - Replace hardcoded "No appearing characters yet" with actual character data
```

### Phase 4: Prompt Builder Enhancement (Plan Consumption)

**Files:** `PromptTemplateBuilder.java`, `llm_chapter_generate_standard` template (DB)
**Why fourth:** Needs planned_characters data in DB from Phase 2+3 to test.
**Dependency:** Phase 1+2+3 (data must flow through the pipeline).

```
PromptTemplateBuilder.java:
  - Add buildPlannedCharactersText() method
  - Add buildFocusedCharacterInfo() method
  - Modify buildTemplateVariables() to inject plannedCharacters

llm_chapter_generate_standard template:
  - Add ${plannedCharacters} section for strict character guidance
```

### Phase 5: API DTO + Frontend Display

**Files:** `ChapterPlanDto.java`, `project.ts`, `ChapterPlanDrawer.vue`, `VolumeTree.vue`
**Why last:** Purely presentation layer, depends on data being available from backend.
**Dependency:** Phase 1 (data must be in DB).

```
ChapterPlanDto.java: add plannedCharacters field

project.ts: add PlannedCharacter interface, add field to ChapterPlan

ChapterPlanDrawer.vue: add planned characters display section

VolumeTree.vue: add character count badge (optional, can defer)
```

---

## Architectural Patterns to Follow

### Pattern 1: JSON Field for Semi-Structured Data

**What:** Store `planned_characters` as JSON string in TEXT column, not as a separate table.
**Why:** This is plan-stage data, not relationship data. It is transient metadata about the AI's intent, not a source-of-truth relationship. The actual character-chapter relationships are stored in `novel_character_chapter` after generation.
**Precedent in codebase:** `keyEvents` (JSON), `foreshadowingSetup` (text), `cultivationLevel` (JSON in NovelCharacterChapter).

### Pattern 2: Backward-Compatible Template Variables

**What:** When `plannedCharacters` is null/empty, the prompt template renders the variable as empty string. No error, no special handling.
**Why:** Existing chapter plans will NOT have `plannedCharacters`. The system must handle both old (no character plan) and new (with character plan) plans gracefully.
**Precedent in codebase:** `PromptTemplateBuilder` already handles null fields with fallbacks (line 173-176: `chapterPlan.getPlotOutline() != null ? ... : ""`).

### Pattern 3: Focus Filter, Not Replace

**What:** When plannedCharacters exists, FILTER the existing `characterInfo` to focus on planned characters. Do NOT replace the entire character info system.
**Why:** The existing `buildCharacterPromptInfoList()` provides valuable context (last appearance, status changes, cultivation level). We want to use this data but FOCUS it on the planned characters rather than dumping all characters.
**Precedent in codebase:** The system already does filtering: `novelCharacterService.getNonNpcCharacters()` filters out NPCs.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Creating a Separate `novel_chapter_plan_character` Table

**What people might do:** Normalize the JSON into a proper join table with `plan_id, character_id, role, arc`.
**Why it's wrong here:**
1. The characters referenced in plans might not exist in `novel_character` yet (plan is generated before any chapter is written).
2. AI outputs character NAMES, not IDs. Name-to-ID matching would add complexity with fuzzy matching, error handling, etc.
3. The plan is AI-generated intent, not a source-of-truth relationship. The actual character-chapter link is `novel_character_chapter`.
4. Adding a join table means adding a mapper, service methods, and cascade logic -- massive overhead for data that is essentially prompt metadata.

**Do this instead:** JSON string in `planned_characters` column. Parse on read, serialize on write. Simple, flexible, matches existing patterns.

### Anti-Pattern 2: Modifying `ChapterCharacterExtractService` to Validate Against Plan

**What people might do:** Add validation logic to CharacterExtractService that compares extracted characters against planned characters and warns about mismatches.
**Why it's wrong here:**
1. The extract service runs AFTER generation. At this point, the chapter content is already written -- validation is too late.
2. Adding validation increases complexity and potential failure points in an already-fragile async pipeline.
3. The LLM may legitimately add or remove characters based on narrative flow. Forcing strict 1:1 matching reduces creative flexibility.

**Do this instead:** Let the prompt template enforce character adherence during GENERATION (Phase 4). If the LLM follows the plan, the extract service will naturally find the planned characters. If not, the extract service still works correctly with whatever characters are found.

### Anti-Pattern 3: Injecting ALL Characters into Chapter Plan Template

**What people might do:** Use the full `buildCharacterPromptInfoList()` with all character details (last appearance, cultivation level, etc.) in the chapter plan template.
**Why it's wrong here:**
1. The chapter plan template (`llm_outline_chapter_generate`) already has a LOT of context: volume info, worldview, plot stage, recent chapters, etc.
2. Adding detailed character info for ALL characters will bloat the prompt and confuse the LLM about what to focus on.
3. The plan stage only needs to know WHICH characters exist and their basic role types -- not their last appearance or cultivation level.

**Do this instead:** Inject a simplified character list: name + roleType only. Save detailed character info for the generation stage where it matters.

---

## Component Boundary Summary

| Component | Type | Change | Complexity |
|-----------|------|--------|------------|
| `novel_chapter_plan` table | DB | ADD column | Low |
| `NovelChapterPlan.java` | Entity | ADD field | Low |
| `ChapterPlanXmlDto.java` | DTO | ADD inner class + field | Low |
| `ChapterGenerationTaskStrategy.java` | Strategy | MODIFY 2 methods + character injection | Low-Medium |
| `PromptTemplateBuilder.java` | Builder | ADD 2 methods, MODIFY 1 | Medium |
| `ChapterPlanDto.java` | DTO | ADD field | Low |
| `llm_outline_chapter_generate` | Template | MODIFY content | Medium |
| `llm_chapter_generate_standard` | Template | ADD variable section | Low |
| `ChapterPlanDrawer.vue` | Component | ADD display section | Low |
| `project.ts` | Types | ADD interface + field | Low |
| `VolumeTree.vue` | Component | ADD badge (optional) | Low |

**Total:** 11 files touched, 0 new files, 0 new tables, 0 new services.

---

## Scalability Considerations

| Concern | Impact | Mitigation |
|---------|--------|------------|
| Large character lists in plan (20+ characters) | Prompt token limit | Template should say "List main appearing characters (no more than 5)" |
| JSON field size in DB | Negligible | TEXT column supports up to 64KB |
| Planned characters for old chapters (null) | Backward compat | All code checks for null/empty before processing |
| LLM ignores `<ch>` tags | Data loss | Graceful degradation: null plannedCharacters = no character guidance |
| LLM invents characters not in DB | Mismatch | Template instructs to only use characters from the provided list |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| LLM omits `<ch>` tags entirely | Medium | Low | System works fine without planned characters (backward compatible) |
| LLM puts `<ch>` tags in wrong position | Low | Medium | Jackson XML parser is lenient; `FAIL_ON_UNKNOWN_PROPERTIES` is false |
| Character names in plan don't match DB names | Medium | Low | `buildFocusedCharacterInfo()` uses fuzzy matching (contains) |
| Prompt token budget exceeded | Low | High | Keep character list short (name + role only for plan template) |

---

## Sources

- Direct source code analysis of `ai-factory-backend` and `ai-factory-frontend` (April 7, 2026)
- Existing architectural decisions documented in PROJECT.md Key Decisions table
- Proven patterns: JSON fields (`keyEvents`, `cultivationLevel`), Jackson XML DTOs (`ChapterPlanXmlDto`, `ChapterCharacterExtractXmlDto`), template variable injection (`PromptTemplateBuilder`)

---
*Architecture research for: Chapter Character Planning Integration*
*Researched: 2026-04-07*
