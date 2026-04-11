# Phase 18: Frontend Chapter Foreshadowing - Research

**Researched:** 2026-04-11
**Domain:** Vue 3 frontend component development (chapter plan drawer tab extension)
**Confidence:** HIGH

## Summary

Phase 18 adds a foreshadowing management tab ("伏笔管理") as the 5th tab in the existing ChapterPlanDrawer component. The backend API is fully complete from Phases 15-17 (ForeshadowingController provides full CRUD: list/create/update/delete/complete/stats). The frontend needs three new files (API client, TypeScript types, ForeshadowingTab component) and modifications to ChapterPlanDrawer.vue to add the tab. The work is purely frontend -- no backend changes required.

The existing codebase has clear patterns to follow: CharacterDrawer demonstrates tab-with-subcomponent decomposition, Characters.vue demonstrates the Modal form pattern, and the API/type barrel-export patterns are well established. The ConfirmDialog component is already available for delete confirmation.

**Primary recommendation:** Follow the CharacterDrawer sub-component pattern exactly -- ForeshadowingTab.vue receives props (projectId, chapterNumber, volumeNumber) and manages its own data fetching via the foreshadowing API. Use two API calls on mount: one for chapter-planted foreshadowing (plantedChapter=N) and one for chapter-callback foreshadowing (plannedCallbackChapter=N), plus a third for volume-active foreshadowing.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** New 5th Tab "伏笔管理" using independent ForeshadowingTab.vue component, peer-level with existing tabs. Reference CharacterDrawer sub-component split pattern.
- **D-02:** Card list layout inside foreshadowing area, each card shows one foreshadowing (title, type, status, description summary).
- **D-03:** Two collapsible sections: "待埋设" (pending plants) and "待回收" (pending callbacks), corresponding to Phase 17 backend status transitions (pending->in_progress / in_progress->completed).
- **D-04:** Add/edit foreshadowing uses a Modal dialog form (not inline editing). Form includes all fields.
- **D-05:** Editable fields: title, description, type (character/item/event/secret), layoutType (bright1-3/dark), priority, plannedCallbackVolume + plannedCallbackChapter. plantedChapter is immutable after creation.
- **D-06:** Delete requires confirmation dialog before execution.
- **D-07:** New foreshadowing pre-fills current chapter number as plantedChapter.
- **D-08:** Display two categories: chapter-related foreshadowing (plantedChapter or plannedCallbackChapter matches current chapter) + current volume active foreshadowing (pending/in_progress).
- **D-09:** Chapter foreshadowing in "待埋设"/"待回收" main sections, volume other active foreshadowing in collapsible "分卷伏笔参考" section. Clear hierarchy.
- **D-10:** Volume other active foreshadowing also editable via modal form.
- **D-11:** Color coding for layoutType: card left border color stripe + type label. bright1=blue, bright2=green, bright3=yellow, dark=purple/red.
- **D-12:** Status badges: pending=gray "待埋设", in_progress=blue "进行中", completed=green "已完成".

### Claude's Discretion
- Card spacing and border-radius specifics
- Modal form field ordering
- Empty state text and icons
- Loading skeleton design
- Exact Tailwind color values
- Card description truncation length

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FC-01 | ChapterPlanDrawer foreshadowing area replaces existing textarea, showing chapter-related foreshadowing cards (pending plants + pending callbacks) | ForeshadowingTab.vue with plantedChapter/plannedCallbackChapter queries to backend API; card layout per D-02/D-03; collapsible sections per D-03/D-09 |
| FC-02 | Users can edit, add, delete foreshadowing in chapter foreshadowing area (calling foreshadowing API) | Modal form per D-04/D-05; create via POST, update via PUT, delete via DELETE on ForeshadowingController; ConfirmDialog for delete per D-06; pre-fill plantedChapter per D-07 |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 | 3.5.x | UI framework | Project standard, Composition API with `<script setup>` |
| TypeScript | 5.9.x | Type safety | Project standard, strict mode |
| Tailwind CSS | 4.1.x | Styling | Project standard utility-first CSS |
| lucide-vue-next | 0.469.x | Icons | Project standard icon library |
| Axios | 1.13.x | HTTP client | Project standard, configured with interceptors |
| Pinia | 3.0.x | State management | Project standard for stores |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| ConfirmDialog.vue | (internal) | Delete confirmation | Reuse existing `@/components/ui/ConfirmDialog.vue` |
| request.ts | (internal) | HTTP wrapper | All API calls use `@/utils/request` |

### No New Dependencies Required
This phase uses only existing project dependencies. No npm install needed.

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-frontend/src/
  api/
    foreshadowing.ts          # NEW: foreshadowing API client
    index.ts                  # UPDATE: add barrel export
  types/
    foreshadowing.ts          # NEW: TypeScript interfaces
    index.ts                  # UPDATE: add barrel export
  views/Project/Detail/
    creation/
      ChapterPlanDrawer.vue   # UPDATE: add 5th tab + import ForeshadowingTab
      ForeshadowingTab.vue    # NEW: foreshadowing tab component
```

### Pattern 1: Tab Sub-Component (from CharacterDrawer)
**What:** CharacterDrawer splits tabs into independent components (CharacterPowerSystemTab.vue, CharacterFactionTab.vue), each receiving props and emitting refresh events.
**When to use:** Exactly this pattern for ForeshadowingTab.vue.
**Example:**
```typescript
// CharacterDrawer.vue pattern (lines 353-358)
<CharacterPowerSystemTab
  v-if="!loading && activeTab === 'powerSystem' && detail"
  :character-id="character!.id"
  :project-id="projectId"
  :associations="detail.powerSystemAssociations"
  @refresh="refreshDetail"
/>
```
ForeshadowingTab follows this pattern:
```vue
<ForeshadowingTab
  v-if="activeSection === 'foreshadowing'"
  :project-id="projectId"
  :chapter-number="chapterNumber"
  :volume-number="currentChapterPlan?.volumeNumber"
/>
```

### Pattern 2: API Client (from character.ts)
**What:** Named async functions wrapping `request.get/post/put/delete`. Return typed data. Use `@/utils/request`.
**When to use:** All foreshadowing API calls.
**Example:**
```typescript
// character.ts pattern
export const getCharacterList = async (projectId: string): Promise<Character[]> => {
  const response = await request.get<CharacterDto[]>(
    `/api/novel/${projectId}/characters`
  )
  return (response || []).map(dto => ({ ... }))
}
```

### Pattern 3: Modal Form (from Characters.vue)
**What:** Fixed overlay with centered card containing header/body/footer. Controlled by a `showXxxModal` ref.
**When to use:** Add/edit foreshadowing form.
**Example:**
```vue
<!-- Characters.vue modal pattern (line 378-508) -->
<div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
  @click.self="closeCreateModal">
  <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
    <!-- Header / Body / Footer -->
  </div>
</div>
```

### Pattern 4: Barrel Export (from api/index.ts, types/index.ts)
**What:** `export * from './foreshadowing'` added to barrel files.
**When to use:** After creating foreshadowing.ts in both api/ and types/.

### Anti-Patterns to Avoid
- **Inline editing in cards:** The project decided on Modal-based editing (D-04). Do not use inline edit.
- **Creating a Pinia store for foreshadowing tab data:** The tab manages its own data locally (like CharacterPowerSystemTab). Only editorStore exists for chapter-level state.
- **Hardcoded Chinese strings for API enum values:** Use the backend enum values directly (character/item/event/secret, bright1/bright2/bright3/dark, pending/in_progress/completed) and map to display labels in the component.
- **Modifying backend API:** All CRUD endpoints already exist. No backend changes.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Delete confirmation | Custom confirm logic | `@/components/ui/ConfirmDialog.vue` with `show()`/`hide()` via `defineExpose` | Already exists with danger/warning variants, ESC key support |
| HTTP requests | Raw axios calls | `@/utils/request` wrapper | Has auth interceptor, error handling, base URL |
| Toast notifications | Custom notification | `@/utils/toast` (success/error/info/warning) | Project standard |
| Tab switching | Custom tab state | Existing `sections` array + `activeSection` ref pattern | Already in ChapterPlanDrawer |

**Key insight:** This is a "wiring" phase -- all backend infrastructure exists. The work is creating API client + types + one new component and connecting them.

## Common Pitfalls

### Pitfall 1: Querying the Wrong Foreshadowing Set
**What goes wrong:** Calling the list API with wrong query parameters, showing foreshadowing from other chapters.
**Why it happens:** The backend `ForeshadowingQueryDto` supports multiple filter combinations (plantedChapter, plannedCallbackChapter, plantedVolume, plannedCallbackVolume, status). The foreshadowing tab needs 2-3 separate queries with different parameter combinations.
**How to avoid:** Plan the queries explicitly:
1. **"待埋设" section:** `plantedChapter=chapterNumber` (all foreshadowing planted in this chapter)
2. **"待回收" section:** `plannedCallbackChapter=chapterNumber` (all foreshadowing to be resolved in this chapter)
3. **"分卷伏笔参考" section:** `plantedVolume=volumeNumber` with `status=pending` or `status=in_progress`, excluding items already in sections 1-2
**Warning signs:** Cards appearing in wrong sections, or duplicate cards.

### Pitfall 2: plantedChapter Immutability Not Enforced in UI
**What goes wrong:** Edit modal allows changing the plantedChapter field, violating D-05.
**Why it happens:** Reusing the same form for create and edit without disabling the plantedChapter field.
**How to avoid:** When editing (foreshadowing has an id), make plantedChapter field disabled/read-only. When creating, pre-fill from current chapter number and allow editing. The backend already enforces immutability (updateForeshadowing uses existingForeshadowing.getPlantedChapter()), but the UI should reflect this constraint.

### Pitfall 3: Z-Index Conflicts with Drawer and Modal
**What goes wrong:** Modal form opens inside the drawer, but gets hidden behind the drawer backdrop or body scroll lock.
**Why it happens:** ChapterPlanDrawer uses `z-50` (line 322). Modal forms in the codebase also use `z-50` with `<Teleport to="body">`.
**How to avoid:** The ForeshadowingTab modal must use `<Teleport to="body">` and a higher z-index (z-[60] or z-[70]) to render above the drawer. ConfirmDialog already uses `z-50` with Teleport, so it needs the same z-index bump.
**Warning signs:** Modal opens but is invisible (behind drawer backdrop), or backdrop covers both modal and drawer.

### Pitfall 4: Volume Number Not Available in ChapterPlan
**What goes wrong:** D-08 requires filtering by `plantedVolume` for "分卷伏笔参考" section, but `ChapterPlan` type may not always have `volumeNumber` populated.
**Why it happens:** Looking at the `ChapterPlan` interface (project.ts line 138-162), `volumeNumber` is an optional field. It may not be populated for all chapter plans.
**How to avoid:** Check `currentChapterPlan?.volumeNumber` exists before making the volume query. If not available, skip the "分卷伏笔参考" section or show it empty. The `CreationCenter.vue` (line 105) does set `chapterNumber` from the chapter data, and `volumeNumber` may come from the volume context.
**Warning signs:** "分卷伏笔参考" section showing no data when it should, or API error from null volume number.

### Pitfall 5: Tab Content Not Loading on Switch
**What goes wrong:** Foreshadowing data only loads on first tab visit, not on subsequent switches.
**Why it happens:** Using `onMounted` instead of watching tab changes. The CharacterPowerSystemTab uses `onMounted` and gets re-mounted each time because of `v-if` (not `v-show`). This is correct.
**How to avoid:** Use `v-if` (not `v-show`) for the ForeshadowingTab in ChapterPlanDrawer, so it re-mounts each time the tab is activated and data is fresh. Alternatively, use a watcher on tab activation. The existing pattern uses `v-show` for other tabs (lines 367-630), but CharacterDrawer uses `v-if` for sub-components (line 353). Using `v-if` for ForeshadowingTab is correct.

### Pitfall 6: Foreshadowing Status Label Mismatch
**What goes wrong:** Status badges show wrong labels for the context. For example, in "待埋设" section a pending status means "awaiting plant" while in "待回收" section a pending status means something different.
**Why it happens:** The same status value has different semantic meaning depending on context (planted vs callback).
**How to avoid:** Per D-12: use status badges consistently. pending=gray "待埋设", in_progress=blue "进行中", completed=green "已完成". The status is a property of the foreshadowing entity, not the section. In "待埋设" section, most items will be pending. In "待回收" section, items will be in_progress (since they were already planted).

## Code Examples

### Foreshadowing API Client Pattern
```typescript
// src/api/foreshadowing.ts
import request from '@/utils/request'

export interface Foreshadowing {
  id: number
  projectId: number
  title: string
  type: string              // character/item/event/secret
  description: string
  layoutType: string        // bright1/bright2/bright3/dark
  plantedChapter: number
  plantedVolume: number | null
  plannedCallbackChapter: number | null
  plannedCallbackVolume: number | null
  actualCallbackChapter: number | null
  status: string            // pending/in_progress/completed
  priority: number
  notes: string | null
  createTime: string
  updateTime: string
}

export interface ForeshadowingCreateRequest {
  title: string
  type: string
  description: string
  layoutType?: string
  plantedChapter: number
  plantedVolume?: number
  plannedCallbackChapter?: number
  plannedCallbackVolume?: number
  priority?: number
  notes?: string
}

export interface ForeshadowingUpdateRequest {
  title: string
  type: string
  description: string
  layoutType?: string
  plannedCallbackChapter?: number
  plannedCallbackVolume?: number
  actualCallbackChapter?: number
  status?: string
  priority?: number
  notes?: string
}

export interface ForeshadowingQueryParams {
  type?: string
  layoutType?: string
  status?: string
  currentChapter?: number
  plantedVolume?: number
  plannedCallbackVolume?: number
  plantedChapter?: number
  plannedCallbackChapter?: number
}

// GET /api/novel/{projectId}/foreshadowings
export const getForeshadowingList = async (
  projectId: string | number,
  params?: ForeshadowingQueryParams
): Promise<Foreshadowing[]> => {
  return request.get<Foreshadowing[]>(
    `/api/novel/${projectId}/foreshadowings`,
    { params }
  )
}

// POST /api/novel/{projectId}/foreshadowings
export const createForeshadowing = async (
  projectId: string | number,
  data: ForeshadowingCreateRequest
): Promise<number> => {
  return request.post<number>(
    `/api/novel/${projectId}/foreshadowings`,
    data
  )
}

// PUT /api/novel/{projectId}/foreshadowings/{foreshadowingId}
export const updateForeshadowing = async (
  projectId: string | number,
  foreshadowingId: number,
  data: ForeshadowingUpdateRequest
): Promise<void> => {
  return request.put(
    `/api/novel/${projectId}/foreshadowings/${foreshadowingId}`,
    data
  )
}

// DELETE /api/novel/{projectId}/foreshadowings/{foreshadowingId}
export const deleteForeshadowing = async (
  projectId: string | number,
  foreshadowingId: number
): Promise<void> => {
  return request.delete(
    `/api/novel/${projectId}/foreshadowings/${foreshadowingId}`
  )
}
```

### ChapterPlanDrawer Tab Integration Pattern
```typescript
// In ChapterPlanDrawer.vue sections array (line 58-63), add 5th tab:
const sections = [
  { key: 'basic', label: '基本信息' },
  { key: 'plot', label: '情节规划' },
  { key: 'scene', label: '场景设定' },
  { key: 'character', label: '角色规划' },
  { key: 'foreshadowing', label: '伏笔管理' }   // NEW
] as const

// In template, add after character section:
// <ForeshadowingTab
//   v-if="activeSection === 'foreshadowing'"
//   :project-id="projectId"
//   :chapter-number="chapterNumber"
//   :volume-number="currentChapterPlan?.volumeNumber"
// />
```

### ForeshadowingTab Data Loading Pattern
```typescript
// ForeshadowingTab.vue script setup pattern
const props = defineProps<{
  projectId: string | number
  chapterNumber: number
  volumeNumber?: number
}>()

const loading = ref(false)
const plantForeshadowings = ref<Foreshadowing[]>([])   // 待埋设
const callbackForeshadowings = ref<Foreshadowing[]>([]) // 待回收
const volumeForeshadowings = ref<Foreshadowing[]>([])   // 分卷参考

const loadData = async () => {
  loading.value = true
  try {
    // Query 1: foreshadowing planted in this chapter
    const planted = await getForeshadowingList(props.projectId, {
      plantedChapter: props.chapterNumber
    })
    plantForeshadowings.value = planted || []

    // Query 2: foreshadowing planned for callback in this chapter
    const callbacks = await getForeshadowingList(props.projectId, {
      plannedCallbackChapter: props.chapterNumber
    })
    callbackForeshadowings.value = callbacks || []

    // Query 3: active foreshadowing in current volume (if volumeNumber available)
    if (props.volumeNumber) {
      const allActive = await getForeshadowingList(props.projectId, {
        plantedVolume: props.volumeNumber,
        status: 'pending'   // Note: may need two calls for pending + in_progress
      })
      // Filter out items already in plant/callback lists
      const chapterIds = new Set([
        ...plantForeshadowings.value.map(f => f.id),
        ...callbackForeshadowings.value.map(f => f.id)
      ])
      volumeForeshadowings.value = (allActive || []).filter(f => !chapterIds.has(f.id))
    }
  } catch (e: any) {
    error('加载伏笔数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
```

### LayoutType Color Mapping Pattern
```typescript
// Per D-11 color coding
const layoutTypeColors: Record<string, { border: string; bg: string; text: string; label: string }> = {
  bright1: { border: 'border-l-blue-500', bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-300', label: '明线1' },
  bright2: { border: 'border-l-green-500', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-300', label: '明线2' },
  bright3: { border: 'border-l-yellow-500', bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-700 dark:text-yellow-300', label: '明线3' },
  dark:    { border: 'border-l-purple-500', bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-300', label: '暗线' }
}

// Per D-12 status badges
const statusStyles: Record<string, { bg: string; text: string; label: string }> = {
  pending:     { bg: 'bg-gray-100 dark:bg-gray-700', text: 'text-gray-600 dark:text-gray-400', label: '待埋设' },
  in_progress: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-600 dark:text-blue-400', label: '进行中' },
  completed:   { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-600 dark:text-green-400', label: '已完成' }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| foreshadowingSetup/payoff textarea fields in ChapterPlanDrawer | Structured foreshadowing cards with CRUD | Phase 15 (DATA-02) | Old textarea fields deleted; new UI uses API-driven cards |
| Unstructured text for foreshadowing | Structured novel_foreshadowing table with type/layoutType/status | Phase 15 (DATA-01) | Enables filtering, color coding, status tracking |

**Deprecated/outdated:**
- `foreshadowingSetup` / `foreshadowingPayoff` fields on `ChapterPlan`: Deleted in Phase 15. Do NOT reference them.
- Any references in `project.ts` or `chapter.ts` to these old fields: Already cleaned in Phase 15.

## Open Questions

1. **Backend query: how to get pending+in_progress in one call?**
   - What we know: The ForeshadowingQueryDto has a single `status` field, and the service applies `eq()` filter. There is no "status in (pending, in_progress)" parameter.
   - What's unclear: Whether we need two API calls (one for pending, one for in_progress) for the volume reference section, or if we can omit the status filter and filter client-side.
   - Recommendation: Fetch all foreshadowing for the volume (no status filter) and filter client-side into the right sections. This is simpler and avoids multiple API calls. The data volume per volume is small (likely <50 foreshadowing items).

2. **ForeshadowingTab re-mount vs persistent state**
   - What we know: The existing ChapterPlanDrawer uses `v-show` for tab content (lines 367-630), meaning all tabs are always mounted. But CharacterDrawer uses `v-if` for sub-component tabs (line 353).
   - What's unclear: Whether the foreshadowing tab should use `v-if` (re-mount each time, fresh data) or `v-show` (persistent, stale data risk).
   - Recommendation: Use `v-if` to ensure fresh data on each tab visit. This matches the CharacterDrawer pattern for sub-components. The tab has no expensive initialization cost.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Frontend dev/build | Yes | 24.14.1 | -- |
| npm | Package management | Yes | 11.11.0 | -- |
| Vite dev server | Hot reload development | Yes | 7.2.x | -- |
| TypeScript compiler | Type checking | Yes | 5.9.x | -- |
| Tailwind CSS | Styling | Yes | 4.1.x | -- |
| Vue 3 | UI framework | Yes | 3.5.x | -- |

**Missing dependencies with no fallback:** None -- all required tools are available.

**Missing dependencies with fallback:** None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None detected -- no test runner configured |
| Config file | None |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FC-01 | Foreshadowing tab renders cards for chapter foreshadowing | Manual only | N/A | N/A |
| FC-02 | CRUD operations on foreshadowing via modal form | Manual only | N/A | N/A |

### Sampling Rate
- **Per task commit:** Manual visual verification in dev server
- **Per wave merge:** Full manual verification of tab rendering, CRUD flows, color coding
- **Phase gate:** Manual verification of all D-01 through D-12 decisions

### Wave 0 Gaps
- No test framework is configured for the frontend project (no vitest, jest, or similar in package.json). This is consistent with the rest of the project. Manual testing via the Vite dev server is the established approach.
- No automated tests should be introduced in this phase -- it is out of scope and inconsistent with project patterns.

## Sources

### Primary (HIGH confidence)
- `ForeshadowingController.java` -- Full API surface verified (6 endpoints: GET list, GET detail, POST create, PUT update, DELETE, POST complete, GET stats)
- `ForeshadowingService.java` -- Query logic with plantedChapter/plannedCallbackChapter filters verified
- `ForeshadowingCreateDto.java` / `ForeshadowingUpdateDto.java` / `ForeshadowingQueryDto.java` -- All DTO fields verified
- `Foreshadowing.java` entity -- All entity fields verified (id, projectId, title, type, description, layoutType, plantedChapter, plantedVolume, plannedCallbackChapter, plannedCallbackVolume, actualCallbackChapter, status, priority, notes, createTime, updateTime)
- `ChapterPlanDrawer.vue` -- Current 4-tab structure, sections array pattern, activeSection ref, drawer layout
- `CharacterDrawer.vue` -- Sub-component tab pattern (CharacterPowerSystemTab, CharacterFactionTab)
- `Characters.vue` -- Modal form pattern, ConfirmDialog usage
- `ConfirmDialog.vue` -- Reusable confirmation dialog with show()/hide() via defineExpose

### Secondary (MEDIUM confidence)
- `CharacterPowerSystemTab.vue` -- Props pattern for tab sub-components (characterId, projectId, associations, @refresh emit)
- `character.ts` API client -- API function patterns, TypeScript interface patterns
- `request.ts` -- HTTP wrapper with interceptors, response unwrapping (res.data extraction)
- `editor.ts` store -- Provides projectId, currentChapterPlan, chapterNumber access

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- No new dependencies, all existing codebase patterns verified by reading source
- Architecture: HIGH -- CharacterDrawer sub-component pattern is a direct reference, Modal pattern from Characters.vue is clear
- Pitfalls: HIGH -- All pitfalls identified from direct code inspection (z-index, query params, v-if vs v-show, volumeNumber availability)

**Research date:** 2026-04-11
**Valid until:** 2026-05-11 (stable frontend patterns, no external dependency changes expected)
