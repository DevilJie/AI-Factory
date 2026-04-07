# Stack Research - Chapter Character Planning (v1.0.5)

**Domain:** Chapter character planning system for AI novel generation
**Researched:** 2026-04-07
**Confidence:** HIGH (entirely based on codebase analysis, zero new external dependencies)

## Executive Summary

This milestone requires **zero new dependencies**. All changes are extensions of existing patterns: adding a JSON column to `novel_chapter_plan`, mapping an existing unmapped JSON column, updating two AI prompt templates, extending regex-based XML parsing, and adding a display section to an existing drawer component. The entire implementation fits within the validated Spring Boot 3.2 + MyBatis-Plus + Vue 3 stack.

The key insight is that the database already has a `character_arcs` JSON column on `novel_chapter_plan` that the Java entity does not map. We need one new column (`planned_characters`) for pre-generation character planning, and we map the existing `character_arcs` for post-generation arc tracking. The AI template system (`PromptTemplateService`) and XML parsing (`OutlineTaskStrategy.parseChaptersXml`) are already proven patterns that simply need field additions.

## Recommended Stack (No Changes to Core Stack)

### Core Framework - Unchanged

| Technology | Version | Purpose | Status |
|------------|---------|---------|--------|
| Spring Boot | 3.2.0 | Backend framework | Unchanged |
| MyBatis-Plus | 3.5.5 | ORM | Unchanged |
| Vue 3 | 3.5.x | Frontend framework | Unchanged |
| Vite | 7.2.x | Build tool | Unchanged |
| Tailwind CSS | 4.1.x | Styling | Unchanged |
| LangChain4j | 1.11.0 | AI orchestration | Unchanged |
| Jackson ObjectMapper | (Spring Boot managed) | JSON serialization | Unchanged |

### No New Dependencies Required

| Need | Existing Solution | Location |
|------|-------------------|----------|
| JSON field storage | MySQL `JSON` column type + `String` field in entity | `novel_chapter_plan.character_arcs` (DB has it, entity does not map it) |
| JSON serialization | `com.fasterxml.jackson.databind.ObjectMapper` (already injected) | `PromptTemplateBuilder` line 50, `OutlineTaskStrategy` line 30 |
| XML parsing (AI output) | Regex-based `extractXmlFieldCData()` pattern | `OutlineTaskStrategy` lines 1273-1289 |
| AI template system | `PromptTemplateService.executeTemplate()` | Used everywhere for all 3 existing templates |
| Frontend drawer/panel | `ChapterPlanDrawer.vue` pattern | Existing component at line 1 |
| Frontend types | `ChapterPlan` interface in `types/project.ts` | Existing type at line 140 |

---

## Database Changes

### Change 1: Add `planned_characters` Column (NEW)

The table already has `character_arcs JSON DEFAULT NULL` (line 381 of `sql/init.sql`) and `foreshadowing_actions JSON DEFAULT NULL` (line 382), but no `planned_characters` column. Add it.

```sql
-- V5__chapter_plan_planned_characters.sql

ALTER TABLE novel_chapter_plan
  ADD COLUMN planned_characters JSON DEFAULT NULL
  COMMENT '规划登场角色（JSON数组，由AI章节规划阶段生成）'
  AFTER foreshadowing_payoff;
```

**Why a new column instead of reusing `character_arcs`:** `character_arcs` stores post-generation arc data (what actually happened to characters after the chapter is written). `planned_characters` stores pre-generation planning (which characters should appear and what they should do). Separating them enables comparing plan vs. actual and avoids semantic confusion. This follows the existing pattern where `foreshadowing_setup` (planned) and `foreshadowing_payoff` (actual) are separate fields.

### Change 2: Map Existing `character_arcs` Column (No Migration)

The `character_arcs` column already exists in the database (`character_arcs json DEFAULT NULL COMMENT '人物弧光变化'`). It is simply not mapped in `NovelChapterPlan.java`. No SQL change needed -- only a Java entity field addition.

---

## JSON Field Structures

### `planned_characters` - New Column

```json
[
  {
    "characterName": "林动",
    "roleType": "protagonist",
    "plannedRole": "本章核心视角人物",
    "plannedScene": "在天剑宗外门考核中展示新突破的武技",
    "plannedEmotion": "紧张、兴奋、决心",
    "plannedDevelopment": "从怯懦到自信的转变",
    "importance": "primary"
  },
  {
    "characterName": "萧炎",
    "roleType": "supporting",
    "plannedRole": "协助与冲突对象",
    "plannedScene": "作为对手在考核中出现",
    "plannedEmotion": "不屑、惊讶",
    "plannedDevelopment": "对主角实力重新评估",
    "importance": "secondary"
  }
]
```

**Field rationale:**

| Field | Type | Why |
|-------|------|-----|
| `characterName` | string | AI generates by name; IDs resolved at save time via `NovelCharacterService` name lookup (same proven pattern as faction parsing in `WorldviewXmlParser`) |
| `roleType` | enum string | Matches existing `protagonist/supporting/antagonist/npc` enum used in `NovelCharacter.roleType` |
| `plannedRole` | free text | The character's narrative function in this specific chapter (not their global role) |
| `plannedScene` | free text | What the character does -- feed this into chapter generation prompt |
| `plannedEmotion` | free text (optional) | Emotional arc guidance for the writer AI |
| `plannedDevelopment` | free text (optional) | Growth direction for this chapter |
| `importance` | enum: `primary`/`secondary`/`minor` | Controls prompt emphasis; `primary` characters get detailed instructions, `minor` characters may be omitted from generation |

### `character_arcs` - Existing Column (Now Mapped)

```json
[
  {
    "characterName": "林动",
    "arcType": "growth",
    "startState": "紧张不安",
    "endState": "自信坚定",
    "keyMoment": "考核中突破自我极限"
  }
]
```

This field is for post-generation tracking (what actually happened), populated by the chapter generation or character extraction process. Mapping it now enables future features but is not the primary deliverable of this milestone.

---

## Entity Changes

### NovelChapterPlan.java - Add Two Fields

Add two fields to the existing entity class:

```java
/**
 * 规划登场角色（JSON格式）
 * 由AI章节规划阶段生成，包含角色名、戏份、情绪等规划信息
 * 格式: [{"characterName":"...", "roleType":"...", "plannedRole":"...", ...}]
 */
private String plannedCharacters;

/**
 * 人物弧光变化（JSON格式）
 * 已有数据库列（character_arcs），本次补充Java实体映射
 * 格式: [{"characterName":"...", "arcType":"...", "startState":"...", ...}]
 */
private String characterArcs;
```

**Why `String` not a typed JSON field:** Consistent with every other JSON column in the codebase. Evidence:
- `keyEvents` is `String` (line 62)
- `foreshadowingSetup` is `String` (line 98)
- `foreshadowingPayoff` is `String` (line 103)
- `NovelCharacterChapter.cultivationLevel` is `String` (line 107)

MyBatis-Plus handles JSON columns as `String` by default. Jackson `ObjectMapper` is used at the service layer for typed access.

---

## AI Template Changes

### Template 1: `llm_outline_chapter_generate` - Add Character Planning Output

**Current output tags:** `<n>` (chapterNumber), `<v>` (volumeNumber), `<t>` (title), `<p>` (plotOutline), `<e>` (keyEvents), `<g>` (goal), `<w>` (wordCount), `<s>` (startingScene), `<f>` (endingScene).

**New output tag:** `<r>` (roles/characters) -- follows single-letter convention, does not collide with existing tags.

Extended XML output format per chapter:

```xml
<o>
  <n>1</n>
  <v>1</v>
  <t><![CDATA[章节标题]]></t>
  <p><![CDATA[情节大纲]]></p>
  <e><![CDATA[关键事件]]></e>
  <g><![CDATA[本章目标]]></g>
  <w>3000</w>
  <s><![CDATA[起点场景]]></s>
  <f><![CDATA[终点场景]]></f>
  <r><![CDATA[
    [
      {"characterName":"角色名","roleType":"protagonist","plannedRole":"本章角色定位","plannedScene":"计划戏份","plannedEmotion":"情绪走向","importance":"primary"},
      {"characterName":"角色名2","roleType":"supporting","plannedRole":"协助角色","plannedScene":"计划戏份","importance":"secondary"}
    ]
  ]]></r>
</o>
```

**Template variable injection:** The template already receives `characterInfo` variable with the project's existing characters (line 940 of `OutlineTaskStrategy`). No new template variables needed -- the LLM uses the existing character list to plan their chapter appearances.

**Template change scope:** Only the output instruction section of the template needs updating. The input variables and structure remain identical.

### Template 2: `llm_chapter_generate_standard` - Inject Planned Characters

**Current variables:** `role`, `targetWordCount`, `maxWordCount`, `minWordCount`, `volumeNumber`, `chapterNumber`, `chapterTitle`, `chapterOutline`, `startingScene`, `endingScene`, `lastChapterEndingScene`, `volumeTitle`, `volumeTheme`, `volumeInfo`, `plotStage`, `plotStageDescription`, `worldview`, `recentChapters`, `knowledgeContext`, narrative settings, `characterInfo`.

**New template variable:**

| Variable Name | Source | Format | Purpose |
|---------------|--------|--------|---------|
| `plannedCharacters` | `NovelChapterPlan.plannedCharacters` parsed and formatted | Structured instruction text block | Direct the AI to include specific characters with specific roles |

**Example injection text that `buildPlannedCharactersText()` would produce:**

```
【本章角色规划 - 必须严格遵循】
以下角色必须在本章登场，按规划执行戏份：

1. **林动**（主角）- importance: primary
   - 角色定位：本章核心视角人物
   - 计划戏份：在天剑宗外门考核中展示新突破的武技
   - 情绪走向：紧张、兴奋、决心
   - 成长方向：从怯懦到自信的转变

2. **萧炎**（配角）- importance: secondary
   - 角色定位：协助与冲突对象
   - 计划戏份：作为对手在考核中出现
   - 情绪走向：不屑、惊讶

3. 其他已有角色：如剧情自然需要可以登场，但不作为重点
```

**Why a separate variable instead of merging into `characterInfo`:** The planned characters are directives (what the AI MUST do), while `characterInfo` is reference data (who exists and their last state). Mixing them would dilute the directive strength. The template can position `plannedCharacters` as a hard constraint section separate from the reference `characterInfo`.

---

## Backend Service Changes

### OutlineTaskStrategy.java - Extend XML Parsing

**Method: `parseChaptersXml()`** (line ~1182)

Add extraction of the `<r>` tag alongside existing fields:

```java
// After existing field extractions (line ~1210):
extractXmlFieldCData(chapterContent, "r", chapterData);

// In field mapping section (line ~1233):
if (chapterData.containsKey("r")) {
    chapterData.put("plannedCharacters", chapterData.get("r"));
}
```

**Method: `saveVolumeChaptersToDatabase()`** (line ~585) and **`saveChaptersToDatabase()`** (line ~2085)

Add `plannedCharacters` field when creating/updating `NovelChapterPlan`:

```java
// When creating new chapter plan:
chapterPlan.setPlannedCharacters(chapterData.getOrDefault("plannedCharacters", null));

// When updating existing chapter plan:
existingChapter.setPlannedCharacters(chapterData.getOrDefault("plannedCharacters", null));
```

### PromptTemplateBuilder.java - Inject Planned Characters into Chapter Generation

**Method: `buildTemplateVariables()`** (line ~149)

After building `characterInfo` (line ~204-207), add planned characters:

```java
// Inject planned characters from chapter plan (if available)
String plannedCharacters = chapterPlan.getPlannedCharacters();
if (plannedCharacters != null && !plannedCharacters.isEmpty()) {
    variables.put("plannedCharacters", buildPlannedCharactersText(plannedCharacters));
} else {
    variables.put("plannedCharacters", "");
}
```

**New private method `buildPlannedCharactersText(String json)`:**

Parse the JSON array using the already-injected `ObjectMapper`, iterate over items, format as structured instruction text. This follows the exact same pattern as `buildCharacterInfoText()` (lines 651-718).

### ChapterPromptBuilder.java - Also Inject (Legacy Path)

The non-template `ChapterPromptBuilder` is used as a fallback. In `buildChapterPlanInfo()` (line ~175), add a planned characters section after the existing chapter plan fields, similar to how `buildChapterPlanInfo` currently appends plotOutline, startingScene, endingScene.

---

## Frontend Changes

### Types - Extend ChapterPlan Interface

**File:** `ai-factory-frontend/src/types/project.ts` (line ~140)

```typescript
export interface ChapterPlan {
  id: string
  volumeId?: string
  projectId: string
  chapterNumber: number
  title: string
  summary?: string
  keyEvents?: string
  characters?: string[]
  foreshadowing?: string
  plannedCharacters?: PlannedCharacter[]  // NEW
  characterArcs?: CharacterArc[]          // NEW (optional, for future use)
}

// NEW interfaces
export interface PlannedCharacter {
  characterName: string
  roleType: string
  plannedRole: string
  plannedScene: string
  plannedEmotion?: string
  plannedDevelopment?: string
  importance: 'primary' | 'secondary' | 'minor'
}

export interface CharacterArc {
  characterName: string
  arcType: string
  startState: string
  endState: string
  keyMoment: string
}
```

### ChapterPlanDrawer.vue - Display Character Planning

**Approach:** Add a read-only section in the drawer showing planned characters when available.

The component currently shows: chapterNumber, title, summary, keyEvents, characters (comma-separated input), foreshadowing.

Add a section (between keyEvents and characters) that renders when `currentChapterPlan.plannedCharacters?.length > 0`:

```
--- 角色规划 (AI规划) ---
+-------------------------------------------+
| 林动 (主角) - 主要                         |
| 角色定位: 本章核心视角人物                  |
| 计划戏份: 在考核中展示新武技                |
| 情绪走向: 紧张、兴奋、决心                  |
+-------------------------------------------+
| 萧炎 (配角) - 次要                         |
| 角色定位: 协助与冲突对象                    |
| 计划戏份: 作为对手出现                      |
+-------------------------------------------+
```

**Design details:**
- Read-only display (not editable -- the AI generates this, user can regenerate the chapter plan to change it)
- Use Tailwind CSS card style consistent with existing dark/light mode patterns
- `importance` shown as colored badge: primary=blue, secondary=gray, minor=transparent
- Use `v-if` + `v-for` -- no new component needed (data is small, 3-8 characters per chapter)
- The existing `characters` text input remains for manual override

### API Layer - Extend ChapterPlanDto

**File:** `ai-factory-frontend/src/api/outline.ts` (line ~41)

```typescript
export interface ChapterPlanDto {
  id: string
  chapterNumber: number
  title: string
  summary?: string
  targetWordCount?: number
  plotPoints?: string
  generated?: boolean
  plannedCharacters?: PlannedCharacter[]  // NEW
  characterArcs?: CharacterArc[]          // NEW
}
```

---

## Alternatives Considered

| Decision Point | Chosen | Alternative | Why Not |
|----------------|--------|-------------|---------|
| Storage for planned characters | JSON column on `novel_chapter_plan` | Separate `novel_chapter_plan_character` table | JSON is simpler; data is always read/written together; matches existing pattern (`keyEvents`, `foreshadowingSetup` all stored as JSON/TEXT on the same row). A join table would require JOIN queries and cascade delete for a small list (3-8 items). |
| Character reference in JSON | By name | By ID | AI does not know database IDs. Name-matching is the proven pattern used for faction parsing (`WorldviewXmlParser.saveTree -> buildNameToIdMap`) and character extraction (`ChapterCharacterExtractService.isSameCharacter`). |
| Template output format | JSON inside CDATA `<r>` tag | Nested XML `<character><name>...</name></character>` tags | JSON inside CDATA is simpler to parse. The regex-based `extractXmlFieldCData` already handles CDATA extraction. Nested XML would require recursive DOM parsing for minimal benefit. Consistent with how `keyEvents` (E tag) is already sometimes JSON. |
| Frontend display | Inline read-only block in ChapterPlanDrawer | New dedicated component / modal | Data is small (3-8 characters per chapter). Inline keeps UX simple. Extract to component only if complexity grows. |
| `character_arcs` column | Map existing unmapped DB column | Create new column | Column exists in database schema (`sql/init.sql` line 381). Not mapping it is wasted schema. Mapping it is zero-cost (one String field). |
| Tag letter for characters | `<r>` (roles) | `<ch>`, `<a>` (actors) | `<r>` follows the single-letter convention. Does not collide with any existing tag. `<a>` is already used for "age" in character parsing. |

---

## What NOT to Do

| Avoid | Why | Do Instead |
|-------|-----|------------|
| Add a new dependency (JSON library, XML library, UI component library) | Everything needed already exists in the stack | Use existing `ObjectMapper`, `extractXmlFieldCData()`, Tailwind CSS |
| Use `@TableField(typeHandler = JacksonTypeHandler.class)` for JSON columns | Not used anywhere in the existing codebase for JSON fields; all JSON fields are `String` with manual `ObjectMapper` usage | Keep `String` fields, parse with `ObjectMapper` at service layer |
| Create a new REST endpoint for planned characters | The data is part of `NovelChapterPlan` and should travel with the existing chapter plan response | Return `plannedCharacters` field in existing chapter plan API response |
| Make planned characters editable in the frontend drawer | The data is AI-generated guidance; editing individual character plans manually would be fragile and confusing | Display as read-only. User regenerates the chapter plan to get new character planning. |
| Store planned characters in `novel_character_chapter` table | That table tracks post-generation actual data (what happened), not pre-generation planning (what should happen). Mixing them breaks the plan-vs-actual distinction. | Use `plannedCharacters` JSON on `novel_chapter_plan` for planning; `novel_character_chapter` remains for actuals |

---

## Files to Modify (Complete List)

### Backend Java (4 files modified)

| File | Change | LOC Estimate |
|------|--------|-------------|
| `entity/NovelChapterPlan.java` | Add `plannedCharacters` and `characterArcs` String fields | +15 |
| `service/task/impl/OutlineTaskStrategy.java` | Parse `<r>` tag in `parseChaptersXml()`, save `plannedCharacters` in both save methods | +25 |
| `service/chapter/prompt/PromptTemplateBuilder.java` | Add `plannedCharacters` variable in `buildTemplateVariables()`, new `buildPlannedCharactersText()` method | +50 |
| `service/chapter/prompt/ChapterPromptBuilder.java` | Add planned characters section in `buildChapterPlanInfo()` | +20 |

### Database (1 new file)

| File | Change |
|------|--------|
| `sql/V5__chapter_plan_planned_characters.sql` (NEW) | `ALTER TABLE` to add `planned_characters` JSON column |

### AI Templates (2 templates updated)

| Template Code | Change |
|---------------|--------|
| `llm_outline_chapter_generate` | Add `<r>` output tag instruction and character planning guidance to the prompt text |
| `llm_chapter_generate_standard` | Add `${plannedCharacters}` variable injection point in the template body |

### Frontend (3 files modified)

| File | Change | LOC Estimate |
|------|--------|-------------|
| `types/project.ts` | Add `PlannedCharacter`, `CharacterArc` interfaces, extend `ChapterPlan` | +25 |
| `api/outline.ts` | Extend `ChapterPlanDto` with new fields | +5 |
| `views/.../ChapterPlanDrawer.vue` | Add planned characters display section | +40 |

**Total estimated LOC change:** ~180 lines across 8 files (excluding template text changes)

---

## Sources

- **Codebase analysis (HIGH confidence):**
  - `entity/NovelChapterPlan.java` -- Current entity fields, missing `plannedCharacters` and `characterArcs` mappings
  - `sql/init.sql` line 361-386 -- `novel_chapter_plan` table definition with existing `character_arcs` and `foreshadowing_actions` JSON columns
  - `service/task/impl/OutlineTaskStrategy.java` -- XML parsing pattern (`parseChaptersXml`, `extractXmlFieldCData`), DB save methods
  - `service/chapter/prompt/PromptTemplateBuilder.java` -- Template variable building, character info formatting
  - `service/chapter/prompt/ChapterPromptBuilder.java` -- Legacy prompt builder (fallback path)
  - `service/task/impl/ChapterContentGenerateTaskStrategy.java` -- Chapter generation flow
  - `service/ChapterCharacterExtractService.java` -- Post-generation character extraction (confirms plan-vs-actual separation)
  - `dto/CharacterPromptInfo.java` -- Existing character prompt DTO
  - `entity/NovelCharacterChapter.java` -- Post-generation character tracking fields
  - `views/.../ChapterPlanDrawer.vue` -- Current frontend drawer
  - `types/project.ts` -- Frontend type definitions
  - `api/outline.ts` -- Frontend API layer

---
*Stack research for: Chapter Character Planning (v1.0.5)*
*Researched: 2026-04-07*
