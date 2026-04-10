# Stack Research - Foreshadowing Management (v1.0.6)

**Domain:** Foreshadowing (伏笔) management activation for AI novel generation
**Researched:** 2026-04-10
**Confidence:** HIGH (entirely based on codebase analysis, zero new external dependencies)

## Executive Summary

This milestone requires **zero new dependencies**. All changes are extensions of existing patterns already validated in v1.0.2--v1.0.5. The `novel_foreshadowing` table, entity, service, controller, and DTOs already exist with full CRUD. The work involves: adding two volume-number columns to the existing entity, extending the chapter plan XML parsing to extract foreshadowing data, injecting foreshadowing constraints into chapter generation prompts (mirroring the character planning injection pattern from v1.0.5), and building a frontend foreshadowing management panel (mirroring the existing `ChapterPlanDrawer` character tab pattern).

The key insight is that the system already has two parallel foreshadowing tracking mechanisms: (1) the structured `novel_foreshadowing` table with ID-based tracking, and (2) the text-based `ChapterPlotMemory` foreshadowing fields (`foreshadowingPlanted`, `foreshadowingResolved`, `pendingForeshadowing`). This milestone activates the structured table as the primary driver for chapter planning and generation, while the memory-based system continues to serve its existing purpose for runtime recall. The `foreshadowingSetup` and `foreshadowingPayoff` JSON fields on `novel_chapter_plan` will be removed as part of consolidating onto the structured table.

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
| DOM XML parsing (javax.xml) | (JDK bundled) | XML parsing of AI output | Unchanged |
| Lucide Vue Next | 0.469.x | Icon library | Unchanged |

### No New Dependencies Required

| Need | Existing Solution | Location |
|------|-------------------|----------|
| Foreshadowing entity/table | `Foreshadowing.java` with `novel_foreshadowing` table | Already exists with full CRUD |
| Volume number tracking | Add `plantedVolume`, `plannedCallbackVolume` Integer fields to existing entity | Simple field addition |
| JSON field storage | MySQL columns + `String` fields in entity (existing pattern) | `NovelChapterPlan` JSON fields |
| Foreshadowing CRUD API | `ForeshadowingController` with 7 REST endpoints | Already exists |
| Foreshadowing service | `ForeshadowingService` with list/create/update/delete/stats | Already exists, needs extension |
| Chapter plan XML parsing | DOM-based `parseChaptersXml` in `ChapterGenerationTaskStrategy` | Extend to extract foreshadowing tags |
| AI prompt template system | `PromptTemplateService.executeTemplate()` | Extend chapter plan template |
| Chapter generation constraint injection | `ChapterGenerationTaskStrategy` | Extend like character injection in v1.0.5 |
| Frontend drawer component | `ChapterPlanDrawer.vue` with tab sections | Extend "伏笔管理" tab |
| Frontend API client | Axios + typed request pattern (`chapter.ts`, `faction.ts`) | Add `foreshadowing.ts` |
| Frontend sidebar navigation | `ProjectSidebar.vue` with `navItems` array | Add menu entry |
| Frontend routing | Vue Router lazy-loaded routes in `router/index.ts` | Add foreshadowing route |
| Frontend type definitions | TypeScript interfaces in `types/project.ts` | Add/update foreshadowing types |

---

## Database Changes

### Change 1: Add Volume Number Fields to `novel_foreshadowing` (NEW columns)

The existing `Foreshadowing` entity has `plantedChapter` and `plannedCallbackChapter` (Integer) for chapter-level tracking, but no volume-level fields. Cross-volume foreshadowing (a foreshadowing planted in volume 1 and resolved in volume 3) needs volume numbers for proper scoping and UI display.

```sql
-- V7__foreshadowing_add_volume_fields.sql

ALTER TABLE novel_foreshadowing
  ADD COLUMN planted_volume INT DEFAULT NULL
    COMMENT '埋设伏笔的分卷编号' AFTER planted_chapter,
  ADD COLUMN planned_callback_volume INT DEFAULT NULL
    COMMENT '计划回收伏笔的分卷编号' AFTER planned_callback_chapter;
```

**Why separate volume fields rather than computing from chapter numbers:** Volume-to-chapter ranges are not fixed (volumes can be reorganized). Storing explicit volume numbers gives the UI direct grouping capability without joining to volume tables or recomputing ranges. This matches the existing pattern where `NovelChapterPlan` stores both `volumePlanId` (volume reference) and `chapterNumber` (position).

### Change 2: Remove `foreshadowingSetup` and `foreshadowingPayoff` from `novel_chapter_plan` (MIGRATION)

These two JSON columns on `novel_chapter_plan` are redundant once foreshadowing is tracked in the dedicated `novel_foreshadowing` table with proper chapter references. They were placeholders before the structured table existed.

```sql
-- Part of V7 migration or separate V8

ALTER TABLE novel_chapter_plan
  DROP COLUMN foreshadowing_setup,
  DROP COLUMN foreshadowing_payoff;
```

**Caveat:** The frontend `ChapterPlanDrawer.vue` currently reads/writes these fields (lines 31-32, 249-250, 309-310). The "伏笔管理" tab currently displays two textareas bound to these fields. This entire section will be rebuilt to use the structured foreshadowing API instead. The removal is safe because the data in these fields was never systematically used for AI generation -- it was only displayed in the drawer.

**Decision:** Remove the columns rather than deprecate them. There is no migration path for the data because the existing text-based `foreshadowingSetup`/`foreshadowingPayoff` content is freeform text, not structured references that could map to `novel_foreshadowing` rows.

---

## Backend Changes (All Extensions of Existing Code)

### Entity Changes

| File | Change | Pattern Followed |
|------|--------|-----------------|
| `Foreshadowing.java` | Add `plantedVolume` (Integer), `plannedCallbackVolume` (Integer) | Same as adding `plannedCharacters` to `NovelChapterPlan` in v1.0.5 |
| `NovelChapterPlan.java` | Remove `foreshadowingSetup`, `foreshadowingPayoff` fields | Field removal, DB columns dropped |
| `ForeshadowingCreateDto.java` | Add `plantedVolume`, `plannedCallbackVolume` | Existing DTO pattern |
| `ForeshadowingUpdateDto.java` | Add `plantedVolume`, `plannedCallbackVolume` | Existing DTO pattern |
| `ForeshadowingDto.java` | Add `plantedVolume`, `plannedCallbackVolume` | Existing DTO pattern |
| `ChapterPlanUpdateRequest.java` | Remove `foreshadowingSetup`, `foreshadowingPayoff` | Field removal |

### Service Changes

| File | Change | Pattern Followed |
|------|--------|-----------------|
| `ForeshadowingService.java` | Add `getForeshadowingsForChapter(Long projectId, Integer chapterNumber)` -- returns foreshadowings to plant + foreshadowings to resolve for a given chapter | Mirrors `getPendingForeshadowingFromMemories` but queries structured table |
| `ForeshadowingService.java` | Add `batchCreateFromPlan(Long projectId, List<ParsedForeshadowing> items)` -- bulk create after AI chapter plan parsing | Mirrors `FactionService.saveTree` batch pattern |
| `ForeshadowingService.java` | Add `buildForeshadowingConstraintText(Long projectId, Integer chapterNumber)` -- builds prompt injection text | Mirrors `buildPendingForeshadowingText` + v1.0.5 character constraint injection |
| `ChapterGenerationTaskStrategy.java` | Extend `parseChaptersXml` to extract `<fs>` (foreshadowing setup) and `<fp>` (foreshadowing payoff) tags from chapter plan XML | Same DOM parsing pattern already used for `<ch>`, `<cn>`, `<cd>`, `<ci>` character tags |
| `ChapterGenerationTaskStrategy.java` | After parsing, create `novel_foreshadowing` rows for each parsed foreshadowing item | Mirrors v1.0.5 where `plannedCharacters` JSON is persisted after parsing |
| `ChapterGenerationTaskStrategy.java` | Inject foreshadowing constraints into chapter generation prompt (call `buildForeshadowingConstraintText`) | Mirrors v1.0.5 character constraint injection (`hasPlannedCharacters` + `buildPlannedCharacterInfoText`) |

### Controller Changes

| File | Change | Pattern Followed |
|------|--------|-----------------|
| `ForeshadowingController.java` | Add `GET /api/novel/{projectId}/foreshadowings/by-chapter?chapterNumber=N` -- returns foreshadowings relevant to a specific chapter | Standard REST endpoint pattern |
| `ForeshadowingController.java` | Add `POST /api/novel/{projectId}/foreshadowings/batch` -- batch create from plan | Mirrors existing `createForeshadowing` but for bulk |
| `ChapterPlanController` (or relevant) | Remove `foreshadowingSetup`/`foreshadowingPayoff` from update endpoint | Field removal |

### AI Template Changes

| Template | Change | Pattern Followed |
|----------|--------|-----------------|
| Chapter plan template (`llm_chapter_plan_standard` or equivalent) | Add XML output instructions for foreshadowing: `<fs>` for setup items, `<fp>` for payoff items per chapter | Mirrors `<ch>/<cn>/<cd>/<ci>` instructions added in v1.0.5 Phase 12 |
| Chapter plan template | Add existing foreshadowing context injection: list of unresolved foreshadowings from `novel_foreshadowing` table | Mirrors `characterInfo` injection in v1.0.5 |
| Chapter generation template (`llm_chapter_create_standard`) | Add foreshadowing constraint block: which foreshadowings to plant, which to resolve in this chapter | Mirrors planned character constraint injection in v1.0.5 Phase 13 |

---

## Frontend Changes (All Extensions of Existing Patterns)

### New Files

| File | Purpose | Pattern Followed |
|------|---------|-----------------|
| `src/api/foreshadowing.ts` | API client for foreshadowing CRUD | Mirrors `src/api/faction.ts`, `src/api/character.ts` |
| `src/views/Project/Detail/ForeshadowingManager.vue` | Project-level foreshadowing overview page | Mirrors `Characters.vue` as a list/manager page |
| `src/views/Project/Detail/components/ForeshadowingDrawer.vue` | Foreshadowing detail editor drawer | Mirrors `CharacterDrawer.vue`, `FactionDrawer.vue` |

### Modified Files

| File | Change | Pattern Followed |
|------|--------|-----------------|
| `src/components/layout/ProjectSidebar.vue` | Add nav entry: `{ name: '伏笔管理', icon: BookOpen, path: 'foreshadowing' }` | Add to `navItems` array, import icon from `lucide-vue-next` |
| `src/router/index.ts` | Add child route: `{ path: 'foreshadowing', name: 'ProjectForeshadowing', component: () => import('...ForeshadowingManager.vue') }` | Mirrors `characters` route |
| `src/types/project.ts` | Remove `foreshadowingSetup`, `foreshadowingPayoff` from `ChapterPlan`/`ChapterPlanUpdate`; add `ForeshadowingItem` interface | Standard type definitions |
| `src/views/Project/Detail/creation/ChapterPlanDrawer.vue` | Rebuild "伏笔管理" tab: replace textareas with structured foreshadowing list (items from API, with add/edit/delete actions, status badges) | Mirrors the "角色规划" tab's `editableCharacters` pattern |

### Frontend Foreshadowing API Module

The `src/api/foreshadowing.ts` module will follow the existing API client pattern:

```typescript
// Pattern from src/api/faction.ts, src/api/character.ts
import request from '@/utils/request'

export interface ForeshadowingItem {
  id: number
  projectId: number
  title: string
  type: 'character' | 'item' | 'event' | 'secret'
  description: string
  layoutType: 'bright1' | 'bright2' | 'bright3' | 'dark'
  plantedChapter: number | null
  plantedVolume: number | null
  plannedCallbackChapter: number | null
  plannedCallbackVolume: number | null
  actualCallbackChapter: number | null
  status: 'pending' | 'in_progress' | 'completed'
  priority: number
  notes: string
}

export const getForeshadowingList = (projectId: string, params?: {...}) => ...
export const createForeshadowing = (projectId: string, data: {...}) => ...
export const updateForeshadowing = (projectId: string, id: number, data: {...}) => ...
export const deleteForeshadowing = (projectId: string, id: number) => ...
export const getForeshadowingsByChapter = (projectId: string, chapterNumber: number) => ...
```

No new HTTP client, no new state management library -- just `axios` via the existing `request` utility.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Foreshadowing-chapter link | Direct `plantedChapter`/`plannedCallbackChapter` Integer fields on `novel_foreshadowing` | Junction table `novel_foreshadowing_chapter` | Overkill for a 1:N relationship (one foreshadowing is planted in one chapter, resolved in one chapter). Junction tables are for M:N. Current entity already has the integer chapter fields. |
| Volume tracking | Add `plantedVolume`/`plannedCallbackVolume` fields | Compute from chapter range via JOIN | Volume chapter ranges are not fixed. Explicit fields give direct grouping. Simpler queries. |
| Frontend state | Local component state (like `ChapterPlanDrawer` pattern) | Pinia store for foreshadowing | The foreshadowing data is scoped to individual views (drawer, manager page). No cross-component sharing needed. A Pinia store would add complexity with no benefit, matching the existing pattern where `Characters.vue` and `ChapterPlanDrawer.vue` both use local state. |
| AI foreshadowing output | XML tags in chapter plan output (`<fs>`, `<fp>`) | JSON output block | The entire chapter plan parsing pipeline uses DOM XML parsing (established in v1.0.5 Phase 12 specifically because Jackson XML cannot handle nested same-name tags). Introducing JSON for foreshadowing would create an inconsistent parsing path. XML is the proven pattern. |
| Foreshadowing memory integration | Leave `ChapterPlotMemory` text fields as-is, use `novel_foreshadowing` for structured planning only | Migrate memory fields to reference foreshadowing IDs | The memory system serves a different purpose (runtime AI recall during generation). Coupling it to the structured table would break the existing generation pipeline. Keep them independent -- memory for AI recall, structured table for user management and planning. |

---

## Installation

No installation needed. Zero new packages for either backend or frontend.

```bash
# Backend: No new Maven dependencies
# Frontend: No new npm packages

# Database migration only:
# Apply V7__foreshadowing_add_volume_fields.sql
```

---

## Sources

- Codebase analysis of `Foreshadowing.java`, `ForeshadowingService.java`, `ForeshadowingController.java` (all existing with full CRUD)
- Codebase analysis of `ChapterGenerationTaskStrategy.java` (DOM XML parsing pattern for chapter plans, character injection pattern)
- Codebase analysis of `ChapterPlanDrawer.vue` (existing foreshadowing section with textareas, character comparison pattern from v1.0.5)
- Codebase analysis of `ProjectSidebar.vue` (nav items pattern, icon imports)
- Codebase analysis of `router/index.ts` (route registration pattern)
- Previous milestone research: `.planning/research/STACK.md` (v1.0.5 character planning, confirmed zero-dependency pattern)
- Project context: `.planning/PROJECT.md` (validated decisions from v1.0.2--v1.0.5)
