# Architecture Research: Foreshadowing Management Integration

**Domain:** Novel generation app -- foreshadowing (伏笔) management as core chapter planning/creation driver
**Researched:** 2026-04-10
**Confidence:** HIGH (based on direct source code analysis of all integration points)

---

## Executive Summary

This research analyzes how to integrate foreshadowing management into the existing chapter planning and creation pipeline for the AI Factory novel generation system. The system already has a `novel_foreshadowing` table with CRUD (ForeshadowingService, ForeshadowingController, ForeshadowingMapper), and the chapter plan entity already has `foreshadowingSetup`/`foreshadowingPayoff` text fields. The milestone goal is to activate the foreshadowing table as the primary driver, replacing these redundant text fields with structured records.

The integration follows the proven pattern established in v1.0.5 for character planning: inject context into chapter plan LLM prompts, parse structured XML output from the LLM, inject constraints into chapter content generation prompts, and provide a management UI. The key difference is that foreshadowing has a dedicated table (unlike planned characters which use a JSON field), so the data flow requires batch create/update operations against `novel_foreshadowing` rather than just saving a JSON string.

The work touches 4 backend files (MODIFY), 2 prompt templates (MODIFY), 1 SQL schema (MODIFY), 1 frontend component (MODIFY), 2 new frontend files (ADD), 1 frontend API client (ADD), and 1 sidebar navigation (MODIFY). No new backend services or mappers -- all changes are additive to existing components.

---

## Current Architecture (Baseline)

### System Overview

```
+=====================================================================+
|                     Pipeline: Novel Generation                       |
+=====================================================================+
|                                                                      |
|  1. VolumePlan        OutlineTaskStrategy                            |
|     (llm_outline_volume_generate)                                    |
|     +----------------------------------------------------------+     |
|     | EXISTING: Generates volume plans with stageForeshadowings|     |
|     | (JSON text field on novel_volume_plan, not structured)    |     |
|     +----------------------------------------------------------+     |
|                                                                      |
|  2. ChapterPlan       ChapterGenerationTaskStrategy                  |
|     (llm_outline_chapter_generate)                                   |
|     +----------------------------------------------------------+     |
|     | EXISTING: Generates chapter plans                        |     |
|     | EXISTING: parseChaptersXml extracts <o> elements         |     |
|     | EXISTING: parseSingleChapter extracts <ch> character tags|     |
|     | MISSING: No foreshadowing tags in XML output             |     |
|     | MISSING: No foreshadowing context injected into prompt   |     |
|     +----------------------------------------------------------+     |
|                                                                      |
|  3. ChapterGenerate   (via PromptTemplateBuilder)                    |
|     (llm_chapter_generate_standard)                                  |
|     +----------------------------------------------------------+     |
|     | EXISTING: Injects character constraints (planned chars)  |     |
|     | MISSING: No foreshadowing constraints injected           |     |
|     +----------------------------------------------------------+     |
|                                                                      |
|  4. ChapterPlanDrawer (Frontend)                                     |
|     +----------------------------------------------------------+     |
|     | EXISTING: "伏笔管理" tab with plain textareas            |     |
|     | MISSING: No structured foreshadowing management          |     |
|     +----------------------------------------------------------+     |
|                                                                      |
|  5. ForeshadowingService (Backend)                                   |
|     +----------------------------------------------------------+     |
|     | EXISTING: CRUD operations (create/read/update/delete)    |     |
|     | EXISTING: Chapter memory integration (plot memories)     |     |
|     | MISSING: No LLM output parsing for batch create          |     |
|     | MISSING: No volume-aware queries                         |     |
|     +----------------------------------------------------------+     |
|                                                                      |
+=====================================================================+
```

### Component Responsibilities (Current State)

| Component | Responsibility | Current State | Change Required |
|-----------|----------------|---------------|-----------------|
| `Foreshadowing.java` | Entity for novel_foreshadowing table | EXISTS: CRUD fields, chapter-based (plantedChapter, plannedCallbackChapter, actualCallbackChapter) | MODIFY: add `plantedVolume`, `plannedCallbackVolume` for cross-volume tracking |
| `ForeshadowingService.java` | CRUD + chapter memory queries | EXISTS: CRUD + getPendingForeshadowingFromMemories + buildPendingForeshadowingText | MODIFY: add volume-scoped queries, batch create from LLM output, chapter-scoped queries |
| `ForeshadowingController.java` | REST API at /api/novel/{projectId}/foreshadowings | EXISTS: 7 endpoints (list, detail, create, update, delete, markCompleted, stats) | MODIFY: add batch create endpoint, chapter-scoped query, volume-scoped query |
| `ForeshadowingMapper.java` | DB access (extends BaseMapper) | EXISTS: basic MyBatis-Plus mapper | MINIMAL: may add custom query methods |
| `NovelChapterPlan.java` | Chapter plan entity | EXISTS: has foreshadowingSetup/foreshadowingPayoff as String fields | MODIFY: mark those fields @TableField(exist=false), they become unused |
| `OutlineTaskStrategy.java` | Volume + chapter plan generation | EXISTS: generates volume plans and chapter plans via LLM | MODIFY: inject existing foreshadowing context into chapter plan prompt |
| `ChapterGenerationTaskStrategy.java` | Chapter plan generation (per-volume) | EXISTS: DOM-based parseChaptersXml with <ch> character tag support | MODIFY: parse <fs>/<fp> foreshadowing tags, batch save to novel_foreshadowing |
| `PromptTemplateBuilder.java` | Build chapter content generation prompts | EXISTS: hasPlannedCharacters + buildPlannedCharacterInfoText | MODIFY: add hasForeshadowingConstraints + buildForeshadowingConstraintText |
| `ChapterPlanDrawer.vue` | Chapter plan detail edit drawer | EXISTS: 5 tabs including "伏笔管理" with plain textareas | MODIFY: replace textareas with structured foreshadowing card management |
| `ProjectSidebar.vue` | Project navigation sidebar | EXISTS: 5 nav items (overview, worldview, settings, creation, characters) | MODIFY: add "伏笔管理" menu item |
| `foreshadowing.ts` (frontend API) | HTTP client for foreshadowing endpoints | MISSING | NEW: create API client |
| `Foreshadowing.vue` | Project-level foreshadowing overview | MISSING | NEW: create management page |

---

## Target Architecture (After Integration)

### System Overview

```
+========================================================================+
|                  Pipeline: Novel Generation (v1.0.6)                    |
+========================================================================+
|                                                                         |
|  1. VolumePlan       OutlineTaskStrategy                                |
|     +-----------------------------------------------------------+      |
|     | EXISTING: Generates volume plans with stageForeshadowings |      |
|     +-----------------------------------------------------------+      |
|                                                                         |
|  2. ChapterPlan      ChapterGenerationTaskStrategy                      |
|     +-----------------------------------------------------------+      |
|     | NEW: Query ForeshadowingService for active foreshadowings |      |
|     | NEW: Inject foreshadowingContext into prompt              |      |
|     | NEW: Template instructs LLM to output <fs>/<fp> tags      |      |
|     | NEW: parseSingleChapter extracts <fs> and <fp> elements   |      |
|     | NEW: Batch save foreshadowing records to novel_foreshadowing|     |
|     +-----------------------------------------------------------+      |
|                                                                         |
|  3. ChapterGenerate  (via PromptTemplateBuilder)                        |
|     +-----------------------------------------------------------+      |
|     | EXISTING: Injects character constraints                  |      |
|     | NEW: Query foreshadowings to plant in this chapter       |      |
|     | NEW: Query foreshadowings to callback in this chapter    |      |
|     | NEW: Inject foreshadowingConstraints into prompt         |      |
|     +-----------------------------------------------------------+      |
|                                                                         |
|  4. Frontend                                                            |
|     +-----------------------------------------------------------+      |
|     | ChapterPlanDrawer: structured foreshadowing management   |      |
|     | ForeshadowingView: project-level overview page           |      |
|     | ProjectSidebar: "伏笔管理" menu item                     |      |
|     | foreshadowing.ts: API client                             |      |
|     +-----------------------------------------------------------+      |
|                                                                         |
+========================================================================+
```

---

## Data Flow

### Flow 1: Chapter Plan Generation with Foreshadowing Context

```
[User triggers chapter plan generation for a volume]
    |
    v
[ChapterGenerationTaskStrategy.generateChaptersForVolume]
    |
    |-- Step 1: Load existing foreshadowing context
    |   |
    |   +-- ForeshadowingService.getActiveForeshadowings(projectId)
    |       Returns: foreshadowings with status pending/in_progress
    |       Purpose: Tell LLM what foreshadowings exist and could be referenced
    |
    |-- Step 2: Build foreshadowing context string
    |   |
    |   +-- "以下伏笔尚未回收：\n1. [title] (planted ch.X, type: event)\n..."
    |
    |-- Step 3: Inject into prompt template
    |   |
    |   +-- variables.put("foreshadowingContext", contextText)
    |   +-- Template includes <fs>/<fp> XML output instructions
    |
    |-- Step 4: LLM generates chapters with foreshadowing tags
    |   |
    |   +-- <o>
    |       <n>5</n><t>...</t><p>...</p>...
    |       <fs><ft>标题</ft><fd>描述</fd><fy>event</fy><fl>bright1</fl></fs>
    |       <fp><ft>前文伏笔标题</ft><fd>回收方式</fd></fp>
    |       </o>
    |
    |-- Step 5: parseSingleChapter extracts foreshadowing data
    |   |
    |   +-- Parse <fs> elements -> foreshadowingSetupList
    |   +-- Parse <fp> elements -> foreshadowingPayoffList
    |
    |-- Step 6: Save chapter plans to database
    |   |
    |   +-- Chapter plans saved with planned foreshadowing metadata
    |
    |-- Step 7: Batch create/update foreshadowing records
    |   |
    |   +-- ForeshadowingService.batchCreateFromChapterPlan(projectId, volumeNumber, parsedData)
    |       - For each <fs>: create new novel_foreshadowing record
    |         (title, type, layoutType from XML; plantedChapter from chapter number; plantedVolume from volume)
    |       - For each <fp>: find matching foreshadowing by title, update status
    |         (NameMatchUtil or exact title match)
    |
    v
[Chapter plans saved + foreshadowing records created/updated]
```

### Flow 2: Chapter Creation with Foreshadowing Constraints

```
[User triggers chapter content generation]
    |
    v
[PromptTemplateBuilder.buildChapterPrompt]
    |
    |-- Step 1: Load chapter plan
    |   (includes plannedCharacters, plotOutline, etc.)
    |
    |-- Step 2: Load foreshadowing constraints
    |   |
    |   +-- ForeshadowingService.getForeshadowingsToPlant(projectId, chapterNumber, volumeNumber)
    |       Returns: foreshadowings where plantedChapter == this chapter (status: pending)
    |   +-- ForeshadowingService.getForeshadowingsToCallback(projectId, chapterNumber, volumeNumber)
    |       Returns: foreshadowings where plannedCallbackChapter <= this chapter (status: pending/in_progress)
    |
    |-- Step 3: Build constraint text (mirrors buildPlannedCharacterInfoText)
    |   |
    |   +-- If constraints exist:
    |       "【伏笔约束 - 必须严格遵循】
    |        本章需要埋设以下伏笔：
    |        1. [title] (type) - [description] [layout: bright1/dark]
    |        ...
    |        本章需要回收以下伏笔：
    |        1. [title] - 原文描述: [description]"
    |   +-- If no constraints: empty string (backward compatible)
    |
    |-- Step 4: Inject into template
    |   |
    |   +-- variables.put("foreshadowingConstraints", constraintText)
    |
    v
[LLM generates chapter content following foreshadowing constraints]
```

### Flow 3: Manual Foreshadowing Management

```
[User opens ForeshadowingView from sidebar]
    |
    v
[ForeshadowingView.vue]
    |
    |-- Load all foreshadowings for project
    |   +-- foreshadowing.ts: getForeshadowingList(projectId, {status, volume, type})
    |
    |-- Display: table with columns
    |   +-- Title, Type, Layout, Planted (vol/ch), Planned Callback (vol/ch),
    |       Actual Callback, Status, Priority
    |
    |-- Stats dashboard
    |   +-- getForeshadowingStats(projectId)
    |   +-- Pending / In Progress / Completed counts, completion rate
    |
    |-- CRUD operations
    |   +-- Create: ForeshadowingForm dialog
    |   +-- Edit: ForeshadowingForm dialog (pre-filled)
    |   +-- Delete: confirmation + API call
    |   +-- Mark complete: select actual callback chapter
    |
    v
[User manages foreshadowings across volumes]
```

### Flow 4: Chapter Plan Drawer Foreshadowing Tab

```
[User opens ChapterPlanDrawer for chapter N in volume V]
    |
    v
[Foreshadowing tab activates]
    |
    |-- Step 1: Load foreshadowings for this chapter
    |   +-- foreshadowing.ts: getForeshadowingsByChapter(projectId, volumePlanId, chapterNumber)
    |
    |-- Step 2: Display structured cards
    |   +-- "本章埋设" section: foreshadowings where plantedChapter == N
    |     Each card shows: title, type badge, description, layout badge
    |   +-- "本章回收" section: foreshadowings where actualCallbackChapter == N
    |     Each card shows: title, original description, callback notes
    |
    |-- Step 3: Operations
    |   +-- Add: open ForeshadowingForm with pre-filled plantedChapter=N
    |   +-- Remove: unlink foreshadowing from this chapter (set chapter to null)
    |   +-- Link existing: dropdown to select from project's active foreshadowings
    |   +-- Edit: open ForeshadowingForm pre-filled with foreshadowing data
    |
    v
[User manages chapter-specific foreshadowings]
```

---

## Integration Points (Complete Inventory)

### Backend: MODIFY

#### 1. Foreshadowing.java -- Entity

**File:** `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java`

**Change:** Add 2 fields

```java
/**
 * 埋伏笔的分卷编号
 */
private Integer plantedVolume;

/**
 * 计划填坑的分卷编号
 */
private Integer plannedCallbackVolume;
```

**Rationale:** Current `plantedChapter`/`plannedCallbackChapter` use chapter numbers only, which are per-volume and reset between volumes. Without volume numbers, a foreshadowing planted in volume 1 chapter 5 is indistinguishable from one in volume 2 chapter 5. Volume fields enable cross-volume foreshadowing management.

**Impact:** Low. MyBatis-Plus auto-maps once DB columns exist.

---

#### 2. NovelChapterPlan.java -- Entity

**File:** `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java`

**Change:** Mark 2 fields as transient

```java
/**
 * 埋伏笔 -- DEPRECATED: use novel_foreshadowing table queries instead
 */
@TableField(exist = false)
private String foreshadowingSetup;

/**
 * 填伏笔 -- DEPRECATED: use novel_foreshadowing table queries instead
 */
@TableField(exist = false)
private String foreshadowingPayoff;
```

**Rationale:** Per PROJECT.md Active task. The dedicated `novel_foreshadowing` table replaces these text fields. Marking `@TableField(exist = false)` stops ORM writes without requiring a DB migration to drop columns. Frontend reads from foreshadowing API instead.

**Migration strategy:** Keep DB columns (avoid ALTER TABLE risk). Mark entity fields as non-persistent. Frontend reads from foreshadowing API.

---

#### 3. ForeshadowingService.java -- Service

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java`

**Change:** Add 4 methods

```java
/**
 * Get active foreshadowings for a project (pending + in_progress)
 * Used by OutlineTaskStrategy to inject context into chapter plan prompts
 */
public List<ForeshadowingDto> getActiveForeshadowings(Long projectId);

/**
 * Get foreshadowings relevant to a specific chapter
 * Used by ChapterPlanDrawer and PromptTemplateBuilder
 */
public List<ForeshadowingDto> getForeshadowingsByChapter(Long projectId, Long volumePlanId, Integer chapterNumber);

/**
 * Get foreshadowings to plant in a specific chapter
 * Used by PromptTemplateBuilder for constraint injection
 */
public List<ForeshadowingDto> getForeshadowingsToPlant(Long projectId, Integer volumeNumber, Integer chapterNumber);

/**
 * Get foreshadowings that should be called back by a specific chapter
 * Used by PromptTemplateBuilder for constraint injection
 */
public List<ForeshadowingDto> getForeshadowingsToCallback(Long projectId, Integer volumeNumber, Integer chapterNumber);

/**
 * Batch create foreshadowings from parsed LLM XML output
 * Called by ChapterGenerationTaskStrategy after parsing chapter plan
 */
public List<Long> batchCreateForeshadowings(Long projectId, Integer volumeNumber, List<ForeshadowingCreateDto> items);
```

**Rationale:** These methods bridge the gap between the existing CRUD service and the new LLM integration use case. The `getActiveForeshadowings` method is used by the chapter plan generation flow. The `getForeshadowingsToPlant/Callback` methods are used by the chapter content generation flow. The `batchCreateForeshadowings` method processes the parsed LLM output.

**Impact:** Medium. New methods only. Existing CRUD methods unchanged.

---

#### 4. ForeshadowingController.java -- Controller

**File:** `ai-factory-backend/src/main/java/com/aifactory/controller/ForeshadowingController.java`

**Change:** Add 3 endpoints

```java
/**
 * GET /api/novel/{projectId}/foreshadowings/chapter?volumePlanId=X&chapterNumber=Y
 * Get foreshadowings planted/callback for a specific chapter
 */
@GetMapping("/chapter")
public Result<List<ForeshadowingDto>> getForeshadowingsByChapter(
    @PathVariable Long projectId,
    @RequestParam Long volumePlanId,
    @RequestParam Integer chapterNumber);

/**
 * POST /api/novel/{projectId}/foreshadowings/batch
 * Batch create foreshadowings from LLM parse output
 */
@PostMapping("/batch")
public Result<List<Long>> batchCreateForeshadowings(
    @PathVariable Long projectId,
    @RequestBody List<ForeshadowingCreateDto> items);

/**
 * GET /api/novel/{projectId}/foreshadowings/volume/{volumeNumber}
 * Get all foreshadowings scoped to a volume
 */
@GetMapping("/volume/{volumeNumber}")
public Result<List<ForeshadowingDto>> getForeshadowingsByVolume(
    @PathVariable Long projectId,
    @PathVariable Integer volumeNumber);
```

**Rationale:** The existing 7 endpoints cover single-record CRUD. New endpoints support batch creation from LLM output and chapter/volume-scoped queries needed by the frontend.

**Impact:** Low. Additive only.

---

#### 5. ForeshadowingDto.java + ForeshadowingCreateDto.java -- DTOs

**Files:**
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java`
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingCreateDto.java`

**Change:** Add volume fields to both DTOs

```java
// In ForeshadowingDto
private Integer plantedVolume;
private Integer plannedCallbackVolume;

// In ForeshadowingCreateDto
private Integer plantedVolume;
private Integer plannedCallbackVolume;
```

**Impact:** Low. Field additions only.

---

#### 6. ChapterGenerationTaskStrategy.java -- Chapter Plan Strategy

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java`

**Change:** Modify 3 areas

**Area 1: Inject foreshadowing context into prompt** (in `buildChapterPromptUsingTemplate`)

```java
// Load active foreshadowings for context
List<ForeshadowingDto> activeForeshadowings = foreshadowingService.getActiveForeshadowings(projectId);
String foreshadowingContext = buildForeshadowingContextText(activeForeshadowings);
variables.put("foreshadowingContext", foreshadowingContext);
```

**Area 2: Parse <fs> and <fp> tags** (in `parseSingleChapter`)

The existing `parseSingleChapter` method already handles `<ch>` character tags in the DOM parsing switch statement. Add foreshadowing tag handling in the same switch:

```java
case "fs" -> {
    Map<String, String> fsData = parseForeshadowingSetupTag((Element) child);
    if (fsData != null) foreshadowingSetupList.add(fsData);
}
case "fp" -> {
    Map<String, String> fpData = parseForeshadowingPayoffTag((Element) child);
    if (fpData != null) foreshadowingPayoffList.add(fpData);
}
```

Add new parsing methods (following the pattern of `parseCharacterTag`):

```java
/**
 * Parse <fs> foreshadowing setup tag
 * Extracts: <ft>(title), <fd>(description), <fy>(type), <fl>(layout)
 */
private Map<String, String> parseForeshadowingSetupTag(Element fsElement) {
    Map<String, String> data = new HashMap<>();
    NodeList children = fsElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() != Node.ELEMENT_NODE) continue;
        String tag = child.getNodeName();
        String text = child.getTextContent().trim();
        switch (tag) {
            case "ft" -> data.put("title", text);
            case "fd" -> data.put("description", text);
            case "fy" -> data.put("type", text);
            case "fl" -> data.put("layoutType", text);
        }
    }
    return data.containsKey("title") ? data : null;
}

/**
 * Parse <fp> foreshadowing payoff tag
 * Extracts: <ft>(title), <fd>(callback description)
 */
private Map<String, String> parseForeshadowingPayoffTag(Element fpElement) {
    // Similar pattern, extracts title and callback description
}
```

**Area 3: Batch save foreshadowings** (in `saveChaptersToDatabase`)

After saving chapter plans, collect all parsed foreshadowing data and batch-create records:

```java
// Collect all foreshadowing setup items from parsed chapters
List<ForeshadowingCreateDto> allForeshadowings = new ArrayList<>();
for (Map<String, String> chapterData : chaptersList) {
    String fsJson = chapterData.get("foreshadowingSetupList");
    if (fsJson != null) {
        // Parse and create ForeshadowingCreateDto items
        // Set plantedChapter = chapterNumber, plantedVolume = volumeNumber
    }
}
if (!allForeshadowings.isEmpty()) {
    foreshadowingService.batchCreateForeshadowings(projectId, volumeNumber, allForeshadowings);
}
```

**Rationale:** Follows the exact pattern established for character parsing in v1.0.5. The DOM parser switch statement is the natural extension point. The batch create pattern avoids N+1 inserts.

**Impact:** Medium. Three areas modified, but each follows existing patterns exactly.

---

#### 7. OutlineTaskStrategy.java -- Outline/Volume Plan Strategy

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java`

**Change:** Inject foreshadowing context in `buildChapterPromptUsingTemplate`

Similar to the ChapterGenerationTaskStrategy change, but for the outline-level chapter plan generation:

```java
// Inject foreshadowing context
List<ForeshadowingDto> activeForeshadowings = foreshadowingService.getActiveForeshadowingContext(projectId);
String foreshadowingContext = buildForeshadowingContextText(activeForeshadowings);
variables.put("foreshadowingContext", foreshadowingContext);
```

Also extend `parseChaptersXml` in this class to handle `<fs>`/`<fp>` tags (same DOM-based approach as ChapterGenerationTaskStrategy).

**Impact:** Medium. Same pattern as ChapterGenerationTaskStrategy changes.

---

#### 8. PromptTemplateBuilder.java -- Prompt Builder

**File:** `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java`

**Change:** Add 2 methods, modify `buildTemplateVariables`

```java
/**
 * Check if chapter has foreshadowing constraints
 */
private boolean hasForeshadowingConstraints(NovelChapterPlan chapterPlan) {
    // Query foreshadowing service for plant/callback records
    // Return true if any records found for this chapter
}

/**
 * Build foreshadowing constraint text for chapter generation
 * Mirrors buildPlannedCharacterInfoText pattern
 */
private String buildForeshadowingConstraintText(
    List<ForeshadowingDto> toPlant,
    List<ForeshadowingDto> toCallback) {

    StringBuilder sb = new StringBuilder();
    sb.append("【伏笔约束 - 必须严格遵循】\n");

    if (!toPlant.isEmpty()) {
        sb.append("本章需要埋设以下伏笔：\n");
        for (ForeshadowingDto f : toPlant) {
            sb.append("- ").append(f.getTitle());
            sb.append(" (").append(f.getType()).append(")");
            sb.append(" - ").append(f.getDescription());
            sb.append(" [").append(f.getLayoutType()).append("]\n");
        }
    }

    if (!toCallback.isEmpty()) {
        sb.append("\n本章需要回收以下伏笔：\n");
        for (ForeshadowingDto f : toCallback) {
            sb.append("- ").append(f.getTitle());
            sb.append(" - ").append(f.getDescription()).append("\n");
        }
    }

    sb.append("\n注：请自然地将上述伏笔融入情节，避免生硬插入。\n");
    return sb.toString();
}
```

Modify `buildTemplateVariables`:

```java
// NEW: Foreshadowing constraints
List<ForeshadowingDto> toPlant = foreshadowingService.getForeshadowingsToPlant(
    projectId, volumePlan.getVolumeNumber(), chapterPlan.getChapterNumber());
List<ForeshadowingDto> toCallback = foreshadowingService.getForeshadowingsToCallback(
    projectId, volumePlan.getVolumeNumber(), chapterPlan.getChapterNumber());

String foreshadowingConstraints = "";
if (!toPlant.isEmpty() || !toCallback.isEmpty()) {
    foreshadowingConstraints = buildForeshadowingConstraintText(toPlant, toCallback);
}
variables.put("foreshadowingConstraints", foreshadowingConstraints);
```

**Rationale:** This follows the exact pattern of `hasPlannedCharacters`/`buildPlannedCharacterInfoText` already in the codebase (lines 657-721 of PromptTemplateBuilder.java). The constraint text format mirrors the character constraint format for consistency.

**Impact:** Medium. New methods + modification to template variable building.

---

### SQL Schema: MODIFY

```sql
ALTER TABLE novel_foreshadowing
ADD COLUMN planted_volume INT NULL COMMENT '埋伏笔的分卷编号' AFTER planted_chapter,
ADD COLUMN planned_callback_volume INT NULL COMMENT '计划填坑的分卷编号' AFTER planned_callback_chapter;

CREATE INDEX idx_foreshadowing_volume ON novel_foreshadowing(project_id, planted_volume);
CREATE INDEX idx_foreshadowing_callback_volume ON novel_foreshadowing(project_id, planned_callback_volume);
```

**Rationale:** Volume fields enable cross-volume foreshadowing management. Indexes support volume-scoped queries for prompt injection.

---

### Prompt Templates: MODIFY

#### `llm_outline_chapter_generate` -- Chapter Plan Template

**Additions:**

1. Inject `${foreshadowingContext}` variable showing active foreshadowings
2. Add XML output instructions for `<fs>` and `<fp>` tags:

```
For each chapter, plan foreshadowing setup and callback:
<fs>
  <ft><![CDATA[伏笔标题]]></ft>
  <fd><![CDATA[伏笔描述]]></fd>
  <fy>event</fy>
  <fl>bright1</fl>
</fs>
<fp>
  <ft><![CDATA[要回收的伏笔标题]]></ft>
  <fd><![CDATA[回收方式描述]]></fd>
</fp>
```

3. Tag reference: `fs=foreshadowing setup, ft=title, fd=description, fy=type(event/item/character/secret), fl=layout(bright1/bright2/bright3/dark), fp=foreshadowing payoff`

---

#### `llm_chapter_generate_standard` -- Chapter Content Template

**Additions:**

Add `${foreshadowingConstraints}` variable usage. When non-empty, the template will render the constraint text instructing the LLM to plant and callback specific foreshadowings.

---

### Frontend: MODIFY

#### ChapterPlanDrawer.vue

**File:** `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue`

**Change:** Replace the "伏笔管理" tab's plain textareas with structured foreshadowing management.

Current (lines 484-516): Two plain `<textarea>` fields for `foreshadowingSetup` and `foreshadowingPayoff`.

Target: Replace with:
1. **"本章埋设" section**: Cards showing foreshadowings planted in this chapter, loaded from API
2. **"本章回收" section**: Cards showing foreshadowings called back in this chapter
3. **"添加伏笔" button**: Opens a mini-form or selector to create/link a foreshadowing
4. **Remove button on each card**: Unlinks or deletes the foreshadowing

This follows the same card-based pattern used for the "角色规划" tab (lines 519-676 of the current component).

---

#### ProjectSidebar.vue

**File:** `ai-factory-frontend/src/components/layout/ProjectSidebar.vue`

**Change:** Add "伏笔管理" menu item to navItems array (line 11)

```typescript
import { ArrowLeft, LayoutDashboard, Globe, Settings, PenTool, Users, BookOpen } from 'lucide-vue-next'

const navItems = [
  { name: '概览', icon: LayoutDashboard, path: 'overview' },
  { name: '世界观设定', icon: Globe, path: 'world-setting' },
  { name: '基础设置', icon: Settings, path: 'settings' },
  { name: '创作中心', icon: PenTool, path: 'creation' },
  { name: '人物管理', icon: Users, path: 'characters' },
  { name: '伏笔管理', icon: BookOpen, path: 'foreshadowing' }  // NEW
]
```

---

### Frontend: NEW

#### foreshadowing.ts -- API Client

**File:** `ai-factory-frontend/src/api/foreshadowing.ts`

```typescript
// CRUD
export function getForeshadowingList(projectId: string, params?: {...}): Promise<Foreshadowing[]>
export function getForeshadowingDetail(projectId: string, id: number): Promise<Foreshadowing>
export function createForeshadowing(projectId: string, data: CreateForeshadowingRequest): Promise<number>
export function updateForeshadowing(projectId: string, id: number, data: UpdateForeshadowingRequest): Promise<void>
export function deleteForeshadowing(projectId: string, id: number): Promise<void>
export function markForeshadowingCompleted(projectId: string, id: number, actualCallbackChapter: number): Promise<void>

// Chapter/Volume scoped
export function getForeshadowingsByChapter(projectId: string, volumePlanId: string, chapterNumber: number): Promise<Foreshadowing[]>
export function getForeshadowingsByVolume(projectId: string, volumeNumber: number): Promise<Foreshadowing[]>

// Stats
export function getForeshadowingStats(projectId: string): Promise<ForeshadowingStats>
```

**Rationale:** Follows the exact pattern of `faction.ts`, `character.ts`, and other API clients in the project. All endpoints map 1:1 to ForeshadowingController endpoints.

---

#### Foreshadowing.vue -- Project-Level Overview Page

**File:** `ai-factory-frontend/src/views/Project/Detail/Foreshadowing.vue`

**Content:**
- Stats bar: total / pending / in_progress / completed + completion rate
- Filter controls: by status, volume, type, layout
- Table: title, type, layout, planted (vol/ch), planned callback (vol/ch), actual callback, status, priority
- CRUD actions: create, edit, delete, mark complete
- Volume tabs or filter to switch between volumes

**Rationale:** Follows the pattern of `Characters.vue` -- a project-scoped management page accessible from the sidebar.

---

#### ForeshadowingList.vue + ForeshadowingForm.vue -- Reusable Components

**Files:**
- `ai-factory-frontend/src/views/Project/Detail/components/ForeshadowingList.vue`
- `ai-factory-frontend/src/views/Project/Detail/components/ForeshadowingForm.vue`

**Rationale:** The list and form components are used in both the ChapterPlanDrawer (chapter-scoped) and ForeshadowingView (project-scoped). Extracting them avoids duplication.

---

## LLM XML Output Schema Addition

### Proposed Tag Structure

```xml
<c>
  <o>
    <n>5</n>
    <t><![CDATA[章节标题]]></t>
    <p><![CDATA[情节大纲]]></p>
    <e><![CDATA[关键事件]]></e>
    <g><![CDATA[目标]]></g>
    <w>3000</w>
    <s><![CDATA[起点场景]]></s>
    <ed><![CDATA[终点场景]]></ed>
    <!-- Existing character tags (from v1.0.5) -->
    <ch><cn>角色名</cn><cd>描述</cd><ci>high</ci></ch>
    <!-- NEW: Foreshadowing setup (plant) -->
    <fs>
      <ft><![CDATA[伏笔标题]]></ft>
      <fd><![CDATA[伏笔描述]]></fd>
      <fy>event</fy>
      <fl>bright1</fl>
    </fs>
    <!-- NEW: Foreshadowing payoff (callback) -->
    <fp>
      <ft><![CDATA[要回收的伏笔标题]]></ft>
      <fd><![CDATA[回收方式描述]]></fd>
    </fp>
  </o>
</c>
```

### Tag Mapping

| Tag | Full name | Purpose | Values |
|-----|-----------|---------|--------|
| `<fs>` | foreshadowing setup | New foreshadowing to plant in this chapter | Container element |
| `<fp>` | foreshadowing payoff | Existing foreshadowing to callback | Container element |
| `<ft>` | foreshadowing title | Short title for the foreshadowing | Free text |
| `<fd>` | foreshadowing description | Detailed description | Free text (CDATA) |
| `<fy>` | foreshadowing type | Category of the foreshadowing | character / item / event / secret |
| `<fl>` | foreshadowing layout | Narrative line assignment | bright1 / bright2 / bright3 / dark |

### Why these tag names

- `fs`/`fp` follow the existing single-letter convention (n, t, p, e, g, w, s, ed, ch, cn, cd, ci)
- `ft`/`fd` reuse the pattern of `cn`/`cd` from character tags (name/description) for consistency
- `fy` (type) uses `y` from the volume plan XML's `Y` tag which already maps to foreshadowings in `parseVolumesXml`
- `fl` (layout) uses `l` which is unambiguous within the `<fs>` context

---

## Build Order (Dependency-Aware)

### Phase A: Data Foundation (no dependencies)

**What:** Extend foreshadowing entity with volume fields, mark chapter plan fields as transient, add SQL migration.

**Files modified:**
- `Foreshadowing.java` -- add plantedVolume, plannedCallbackVolume
- `ForeshadowingDto.java` -- add volume fields
- `ForeshadowingCreateDto.java` -- add volume fields
- `ForeshadowingUpdateDto.java` -- add volume fields
- `ForeshadowingMapper.java` -- add volume-scoped query if needed
- `ForeshadowingService.java` -- add getActiveForeshadowings, getForeshadowingsByChapter, getForeshadowingsToPlant, getForeshadowingsToCallback, batchCreateForeshadowings
- `ForeshadowingController.java` -- add new endpoints
- `NovelChapterPlan.java` -- mark foreshadowingSetup/Payoff as @TableField(exist=false)
- SQL: ALTER TABLE add volume columns + indexes

**Why first:** All subsequent phases depend on the data model and service methods being available.

---

### Phase B: AI Prompt Template + Context Injection (depends on Phase A)

**What:** Update prompt templates to include foreshadowing context and XML output instructions. Inject existing foreshadowing context into chapter plan prompts.

**Files modified:**
- `ChapterGenerationTaskStrategy.java` -- inject foreshadowingContext variable
- `OutlineTaskStrategy.java` -- inject foreshadowingContext variable
- DB: `llm_outline_chapter_generate` template -- add foreshadowing output instructions

**Why second:** The LLM must output foreshadowing XML tags before the parser can extract them. The prompt must reference the context injection methods from Phase A.

---

### Phase C: XML Parsing + Batch Save (depends on Phase A + B)

**What:** Extend parseSingleChapter to handle <fs>/<fp> tags. Implement batch create/update of foreshadowing records.

**Files modified:**
- `ChapterGenerationTaskStrategy.java` -- add parseForeshadowingSetupTag, parseForeshadowingPayoffTag, extend parseSingleChapter switch, extend saveChaptersToDatabase
- `OutlineTaskStrategy.java` -- similar parsing extension for its parseChaptersXml

**Why third:** Depends on LLM outputting the correct format (Phase B) and on service methods (Phase A).

---

### Phase D: Chapter Generation Constraint Injection (depends on Phase A)

**What:** Add foreshadowing constraint injection into PromptTemplateBuilder.

**Files modified:**
- `PromptTemplateBuilder.java` -- add hasForeshadowingConstraints, buildForeshadowingConstraintText, modify buildTemplateVariables
- DB: `llm_chapter_generate_standard` template -- add ${foreshadowingConstraints} variable

**Why parallel with C:** Depends only on Phase A (service query methods). No dependency on parsing.

---

### Phase E: Frontend API Client + Types (depends on Phase A)

**What:** Create foreshadowing.ts API client, add TypeScript types.

**Files created:**
- `ai-factory-frontend/src/api/foreshadowing.ts`

**Files modified:**
- `ai-factory-frontend/src/types/project.ts` -- add Foreshadowing, CreateForeshadowingRequest types

**Why parallel with C/D:** Only needs endpoint contracts from Phase A.

---

### Phase F: ChapterPlanDrawer Foreshadowing Tab (depends on Phase E)

**What:** Replace plain textareas with structured foreshadowing card management.

**Files modified:**
- `ChapterPlanDrawer.vue` -- replace foreshadowing tab content

**Why after E:** Needs the API client to load and manage foreshadowings.

---

### Phase G: Project-Level Foreshadowing View + Sidebar Menu (depends on Phase E)

**What:** Create ForeshadowingView page, add sidebar menu item.

**Files created:**
- `ai-factory-frontend/src/views/Project/Detail/Foreshadowing.vue`
- `ai-factory-frontend/src/views/Project/Detail/components/ForeshadowingList.vue` (optional, can inline)
- `ai-factory-frontend/src/views/Project/Detail/components/ForeshadowingForm.vue` (optional, can inline)

**Files modified:**
- `ProjectSidebar.vue` -- add menu item
- Router config -- add route

**Why last:** Full management UI depends on all backend APIs and frontend API client.

---

## Architectural Patterns

### Pattern 1: Constraint Injection (mirrors character planning from v1.0.5)

**What:** Inject structured constraints into LLM prompts.
**Precedent:** `hasPlannedCharacters` / `buildPlannedCharacterInfoText` in PromptTemplateBuilder (lines 657-721).
**Application:** Foreshadowing uses the exact same pattern: `hasForeshadowingConstraints` / `buildForeshadowingConstraintText`.

### Pattern 2: DOM XML Parsing for Nested Tags

**What:** Use DOM parser to extract structured data from LLM XML output.
**Precedent:** `parseSingleChapter` in ChapterGenerationTaskStrategy (lines 737-783) handles `<ch>` child tags.
**Application:** Add `<fs>` and `<fp>` cases to the same switch statement, following the `parseCharacterTag` pattern.

### Pattern 3: Name-Based Entity Resolution

**What:** LLM outputs names, backend resolves to IDs.
**Precedent:** `NameMatchUtil` used for character and faction matching.
**Application:** When `<fp>` references a foreshadowing to callback, match by title against existing records.

### Pattern 4: Dual-Source Data (LLM + Manual)

**What:** Data can come from LLM generation OR manual user input. Both paths write to the same table.
**Application:** Foreshadowing records are created either by batch-create from LLM output, or by manual CRUD through the ForeshadowingView.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Keeping Both Text Fields and Structured Table

**What people do:** Store foreshadowing data in both `novel_chapter_plan.foreshadowingSetup` AND `novel_foreshadowing`.
**Why it is wrong:** Two sources of truth diverge. Manual edits in one do not reflect in the other.
**Do this instead:** Mark the chapter plan fields as transient. Always query the foreshadowing table.

### Anti-Pattern 2: Asking LLM for Foreshadowing IDs

**What people do:** Ask LLM to output `foreshadowingId: 42`.
**Why it is wrong:** LLMs do not know database IDs and will hallucinate.
**Do this instead:** LLM outputs descriptive titles. Backend matches against existing records by title.

### Anti-Pattern 3: Injecting ALL Project Foreshadowings into Every Prompt

**What people do:** Dump every foreshadowing record into the prompt template.
**Why it is wrong:** At 100+ foreshadowings, this wastes tokens and confuses the LLM.
**Do this instead:** Filter by volume and status. Only inject active (pending/in_progress) foreshadowings relevant to the current volume.

### Anti-Pattern 4: Creating New Service/Mapper Files

**What people do:** Create `ForeshadowingBatchService` or `ForeshadowingLLMService`.
**Why it is wrong:** ForeshadowingService already exists with proper Spring wiring. Adding a parallel service creates confusion about where logic lives.
**Do this instead:** Add methods to the existing ForeshadowingService.

---

## Scalability Considerations

| Concern | At 10 foreshadowings | At 100 foreshadowings | At 1000 foreshadowings |
|---------|---------------------|----------------------|------------------------|
| Prompt token cost | Negligible (~100 tokens) | Manageable (~500 tokens) | Filter to current volume only (~50 tokens) |
| DB queries | Single indexed query fine | Indexed query fine | Volume-scoped index essential |
| Frontend table | Simple list | Pagination needed | Pagination + virtual scroll |
| Batch create | Single transaction | Batch insert fine | Batch insert with chunking |

**Scaling priority:** Volume-scoped filtering is the critical optimization. Always filter by `plantedVolume` when injecting context. The new index on `(project_id, planted_volume)` supports this.

---

## Component Boundary Summary

| Component | Type | Change | Complexity |
|-----------|------|--------|------------|
| `novel_foreshadowing` table | DB | ADD 2 columns + indexes | Low |
| `Foreshadowing.java` | Entity | ADD 2 fields | Low |
| `ForeshadowingDto.java` | DTO | ADD 2 fields | Low |
| `ForeshadowingCreateDto.java` | DTO | ADD 2 fields | Low |
| `ForeshadowingService.java` | Service | ADD 5 methods | Medium |
| `ForeshadowingController.java` | Controller | ADD 3 endpoints | Low |
| `ForeshadowingMapper.java` | Mapper | MINIMAL (BaseMapper sufficient) | Low |
| `NovelChapterPlan.java` | Entity | MODIFY 2 fields to @TableField(exist=false) | Low |
| `ChapterGenerationTaskStrategy.java` | Strategy | MODIFY 3 areas + ADD 2 parse methods | Medium-High |
| `OutlineTaskStrategy.java` | Strategy | MODIFY 2 areas | Medium |
| `PromptTemplateBuilder.java` | Builder | ADD 2 methods + MODIFY buildTemplateVariables | Medium |
| `llm_outline_chapter_generate` | Template | ADD foreshadowing output instructions | Medium |
| `llm_chapter_generate_standard` | Template | ADD variable section | Low |
| `foreshadowing.ts` | Frontend API | NEW: full API client | Medium |
| `project.ts` | Frontend Types | ADD Foreshadowing interfaces | Low |
| `ChapterPlanDrawer.vue` | Component | REPLACE foreshadowing tab content | Medium |
| `Foreshadowing.vue` | Component | NEW: project-level page | Medium-High |
| `ProjectSidebar.vue` | Component | ADD 1 menu item | Low |

**Total:** 18 files touched, 2 new frontend files, 1 new API file, 0 new tables, 0 new backend services.

---

## Sources

- Direct source code analysis: `ForeshadowingService.java`, `ForeshadowingController.java`, `Foreshadowing.java`, `ForeshadowingDto.java`, `ForeshadowingCreateDto.java`, `ForeshadowingMapper.java`, `ChapterGenerationTaskStrategy.java`, `OutlineTaskStrategy.java`, `PromptTemplateBuilder.java`, `ChapterPlanDrawer.vue`, `ProjectSidebar.vue`, `NovelChapterPlan.java`, `NovelVolumePlan.java`, `ChapterContextBuilder.java`, `project.ts`, `chapter.ts`
- Database schema: `sql/init.sql` (novel_foreshadowing table, lines 340-360)
- PROJECT.md: v1.0.6 Active tasks for foreshadowing milestone
- Established patterns from prior milestones: character constraint injection (v1.0.5 Phase 13), DOM XML parsing (v1.0.5 Phase 12), name matching (v1.0.5 Phase 11)

---
*Architecture research for: foreshadowing management integration into chapter planning and creation*
*Researched: 2026-04-10*
