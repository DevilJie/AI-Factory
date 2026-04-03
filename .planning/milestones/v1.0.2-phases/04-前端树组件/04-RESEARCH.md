# Phase 4: 前端树组件 - Research

**Researched:** 2026-04-02
**Domain:** Vue 3 frontend tree component, API client, page integration
**Confidence:** HIGH

## Summary

Phase 4 builds a recursive tree component (FactionTree.vue) for managing factions in the world-building settings page, replacing the existing plain-text textarea. The backend API is already complete (FactionController with tree/list/save/update/delete endpoints). The frontend work follows established patterns from GeographyTree.vue but uses a recursive component approach instead of GeographyTree's manual 4-level nesting.

The three deliverables are: (1) `faction.ts` API client mirroring `continentRegion.ts`, (2) `FactionTree.vue` recursive tree component with type badges and power system labels, and (3) WorldSetting.vue integration replacing the forces textarea. All patterns are well-established in the codebase, making this a straightforward implementation phase.

**Primary recommendation:** Follow the GeographyTree.vue interaction pattern exactly (inline editing, add/delete with confirm, loading/empty states), but use Vue 3's self-referencing recursive component capability via the component's own filename tag in the template.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** FactionTree uses Vue 3 recursive component (one template for any depth), NOT GeographyTree's manual 4-level nesting
- **D-02:** Type badges shown as colored prefix: `[正派] 紫阳宗`. Color mapping: ally=green, hostile=red, neutral=gray. Only on top-level factions
- **D-03:** Power system name displayed with dot separator: `[正派] 紫阳宗 · 仙道`. Only on top-level factions. Frontend resolves power system ID to name via the existing powerSystem API list
- **D-04:** Mixed edit mode: inline editing for name/description (click edit button -> input replaces text), type and power system use selectors that expand below the node
- **D-05:** Completely replace forces textarea, no read-only text fallback. FactionTree takes full row, same layout as GeographyTree
- **D-06:** AI-generated worldview auto-refreshes FactionTree via ref.refresh() call, same as GeographyTree

### Claude's Discretion
- Recursive component implementation details (props definition, event handling)
- Type selector and power system selector UI details
- Empty state text and prompt messages
- Power system name resolution approach (fetch list from API vs backend returning name directly)

### Deferred Ideas (OUT OF SCOPE)
- Faction relationship editing UI -- Phase 5 (UI-04)
- Faction-character association UI -- Phase 5 (UI-05)
- Faction-region association UI -- Phase 5 (UI-06)
- Refactoring GeographyTree.vue to recursive component -- optional optimization, not required
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-01 | FactionTree.vue recursive tree with expand/collapse, CRUD, description editing, type badges (color-coded), power system labels | Recursive component pattern (Section: Architecture Patterns), GeographyTree.vue reference, type badge design (D-02/D-03) |
| UI-02 | faction.ts API client (CRUD interfaces) | continentRegion.ts pattern, FactionController endpoints documented, NovelFaction entity fields |
| UI-03 | WorldSetting.vue replaces forces textarea with FactionTree component | WorldSetting.vue lines 329-341 identified for replacement, handleGenerate refresh pattern documented |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 | ^3.5.24 | UI framework, recursive component support | Already in project, Composition API with script setup |
| TypeScript | ~5.9.3 | Type safety for API interfaces and component props | Already in project, strict mode in tsconfig |
| Axios | ^1.13.2 | HTTP client for API calls | Already in project, configured with interceptors |
| Lucide Vue Next | ^0.469.0 | Icon library (ChevronRight, ChevronDown, Plus, Edit3, Trash2, etc.) | Already in project, used across all components |
| Tailwind CSS | ^4.1.18 | Utility-first styling (dark mode support) | Already in project, consistent with GeographyTree.vue styling |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Pinia | ^3.0.4 | State management | Not needed for this phase -- component uses local refs |
| Vue Router | ^4.6.4 | Route parameter access | Used in WorldSetting.vue for projectId via useRoute() |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Self-built recursive component | External tree library (vue3-tree, vue-tree) | External adds dependency weight for simple use case; recursive pattern is clean and well-understood in Vue 3 |
| GeographyTree's manual nesting | Recursive component | Decision D-01 locks recursive; cleaner for variable depth |

**Installation:** No new packages needed -- all dependencies already installed.

**Version verification:**
```
vue: ^3.5.24 (in package.json)
axios: ^1.13.2 (in package.json)
lucide-vue-next: ^0.469.0 (in package.json)
tailwindcss: ^4.1.18 (in package.json)
```

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-frontend/src/
  api/
    faction.ts                    # NEW: Faction API client
    index.ts                      # UPDATE: add export for faction
  views/Project/Detail/
    components/
      FactionTree.vue             # NEW: Recursive tree component
      FactionTreeNode.vue         # NEW (optional): Separate node component for recursion
      GeographyTree.vue           # EXISTING: Reference implementation
    WorldSetting.vue              # UPDATE: Replace textarea with FactionTree
```

**Important note on recursive component approach:** Two viable patterns exist:

1. **Single-file self-reference (simpler):** FactionTree.vue references itself as `<FactionTree>` in its own template. Vue 3 supports this -- a component with `<script setup>` can use its own filename as a tag. However, there is a caveat: the name used in the template must match the filename (e.g., `FactionTree.vue` -> `<FactionTree>`).

2. **Separate node component (cleaner separation):** FactionTree.vue is the container (loads data, manages state), FactionTreeNode.vue is the recursive node (receives a single node, renders self for children). This separates the "page-level" logic from "node-level" rendering.

**Recommendation:** Use single-file self-reference (approach 1). The component is small enough (~150 lines per D-01) that splitting into two files adds unnecessary indirection. If the component grows beyond 200 lines, split later.

### Pattern 1: API Client (faction.ts)
**What:** TypeScript interface + CRUD functions, identical pattern to continentRegion.ts
**When to use:** For all new API files in this project
**Example:**
```typescript
// Source: continentRegion.ts pattern + NovelFaction entity fields
import request from '@/utils/request'

export interface Faction {
  id?: number
  parentId?: number | null
  deep?: number
  sortOrder?: number
  projectId?: number
  name: string
  type?: string          // ally/hostile/neutral, top-level only
  corePowerSystem?: number | null  // power system ID, top-level only
  description?: string
  createTime?: string
  updateTime?: string
  children?: Faction[]
}

export const getFactionTree = (projectId: string) => {
  return request.get<Faction[]>(`/api/novel/${projectId}/faction/tree`)
}

export const addFaction = (projectId: string, data: Faction) => {
  return request.post<Faction>(`/api/novel/${projectId}/faction/save`, data)
}

export const updateFaction = (projectId: string, data: Faction) => {
  return request.put<Faction>(`/api/novel/${projectId}/faction/update`, data)
}

export const deleteFaction = (projectId: string, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${id}`)
}
```

**Key difference from continentRegion.ts:** Faction has `type` and `corePowerSystem` fields that ContinentRegion does not. These need to be included in the add/update payloads.

### Pattern 2: Recursive Tree Component
**What:** A Vue 3 component that renders tree nodes by calling itself for children
**When to use:** For tree data with arbitrary depth
**Example structure:**
```vue
<!-- FactionTree.vue (conceptual skeleton) -->
<script setup lang="ts">
// Top-level logic: loadData, refresh, state management
// Exposed: { refresh }

// Props: projectId (string), disabled (boolean)
// Local state: treeData, loading, expandedNodes, editingNode, etc.
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
    <!-- Header with "add root" button -->
    <!-- Loading / Empty states -->
    <!-- v-for on treeData, rendering FactionTreeNode or self -->
  </div>
</template>
```

**Recursive rendering within the same component:**
```vue
<!-- Inside FactionTree.vue template -->
<template v-for="node in nodes" :key="node.id">
  <!-- Node rendering: chevron, type badge, name, power system label, description -->
  <!-- Edit mode: inline inputs for name/description, selectors for type/powerSystem -->
  <!-- Action buttons: add child, edit, delete (visible on hover) -->

  <!-- Recursive call for children -->
  <div v-if="node.id && expandedNodes.has(node.id) && node.children?.length"
       :style="{ paddingLeft: '20px' }">
    <FactionTree  <!-- SELF-REFERENCE by filename -->
      :nodes="node.children"
      :depth="(node.deep || 0) + 1"
      :disabled="disabled"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
    />
  </div>
</template>
```

**Important:** For self-referencing to work, the component name in the template must match the filename. `FactionTree.vue` -> `<FactionTree>`. Vue 3 infers the component name from the filename automatically.

### Pattern 3: WorldSetting.vue Integration
**What:** Replace textarea with component, add ref.refresh() call
**When to use:** Component integration following existing GeographyTree pattern
**Example (specific changes to WorldSetting.vue):**
```vue
<!-- BEFORE (lines 329-341): -->
<div class="grid grid-cols-1 gap-6">
  <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
    <label>势力阵营</label>
    <textarea v-model="formData.forces" ... />
  </div>
</div>

<!-- AFTER: -->
<FactionTree ref="factionTreeRef" :project-id="projectId()" :disabled="generating" />
```

```typescript
// In script setup:
import FactionTree from './components/FactionTree.vue'
const factionTreeRef = ref()

// In handleGenerate(), after loadData():
factionTreeRef.value?.refresh()

// Also in restoreGeneratingState(), after loadData():
factionTreeRef.value?.refresh()
```

### Anti-Patterns to Avoid
- **Manual level nesting like GeographyTree.vue:** The whole point of D-01 is to avoid this. Do NOT copy GeographyTree's 4-level template duplication.
- **Power system name stored in Faction data:** The backend only stores corePowerSystem as an ID. Resolution to name must happen on the frontend via the powerSystem API list. Do not request backend changes.
- **Removing the `forces` field from Worldview interface:** Keep it. The field still exists in the backend as a transient field (filled by fillForces). It just won't be rendered as a textarea anymore.
- **Using `confirm()` for delete:** Actually this is fine -- GeographyTree uses `confirm()` and the project has no dialog component. Stay consistent.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP request handling | Custom fetch wrappers | Existing `@/utils/request` (Axios with interceptors) | Already handles auth, error toasts, response unwrapping |
| Toast notifications | Custom notification system | `@/utils/toast` (success, error, info, warning) | Already handles positioning, dark mode, auto-dismiss |
| Tree data fetching | Custom API call assembly | Backend `/tree` endpoint returns pre-built tree with `children` | Server builds the tree, frontend just renders it |
| Power system name resolution | Join query or backend change | `getPowerSystemList()` + build Map<id, name> client-side | Power system list is small (< 20 items), one-time fetch is sufficient |

**Key insight:** This phase is pure frontend with no backend changes. FactionController already has all needed endpoints (tree, save, update, delete). The only "creative" work is the recursive component structure and the type/powerSystem UI elements.

## Common Pitfalls

### Pitfall 1: Recursive Component Not Self-Referencing
**What goes wrong:** Component template uses `<FactionTreeNode>` but file is named `FactionTree.vue`, causing "Failed to resolve component" warning.
**Why it happens:** Vue 3 `<script setup>` infers component name from filename, but the template tag must match exactly.
**How to avoid:** Ensure the self-referencing tag name matches the filename exactly: `FactionTree.vue` -> `<FactionTree>`. No explicit `name` option needed.
**Warning signs:** Browser console shows "Failed to resolve component: FactionTreeNode" or similar.

### Pitfall 2: Type Field Value Mismatch
**What goes wrong:** Frontend uses Chinese labels ("正派"/"反派"/"中立") but backend stores English values ("ally"/"hostile"/"neutral").
**Why it happens:** NovelFaction.java `type` field stores "ally", "hostile", "neutral" (see entity comment). CONTEXT.md D-02 describes display as "正派"/"反派"/"中立".
**How to avoid:** Maintain a mapping: `{ ally: '正派', hostile: '反派', neutral: '中立' }`. Send English values to backend, display Chinese labels on frontend.
**Warning signs:** Save succeeds but type badge doesn't appear, or wrong type is displayed after refresh.

### Pitfall 3: Power System ID Resolution Before Data Loads
**What goes wrong:** FactionTree renders before power system list finishes loading, showing raw IDs instead of names.
**Why it happens:** Both API calls are async but not coordinated.
**How to avoid:** Load power system list first (or in parallel with faction tree), build a `Map<number, string>` for ID->name resolution, then render the tree. Use `Promise.all([getFactionTree(projectId), getPowerSystemList(projectId)])` to load both in parallel.
**Warning signs:** Power system label shows as a number or "undefined" briefly, then corrects.

### Pitfall 4: Top-Level-Only Fields Shown on Child Nodes
**What goes wrong:** Type badge and power system label appear on child factions where they should not (D-02/D-03 specify top-level only).
**Why it happens:** Forgetting to check `node.deep === 0` or `node.parentId === null` before rendering badges.
**How to avoid:** Conditionally render type badge and power system label only when `node.deep === 0` (or equivalently, `!node.parentId`).
**Warning signs:** Every node in the tree shows a type badge, cluttering the UI.

### Pitfall 5: WorldSetting.vue Still Shows forces Textarea
**What goes wrong:** Both the old textarea and new FactionTree appear on the page.
**Why it happens:** Adding FactionTree without removing the existing forces textarea block.
**How to avoid:** Precisely replace lines 329-341 (the entire forces section div) with the FactionTree component. Remove nothing else.
**Warning signs:** Page shows both a textarea labeled "势力阵营" and the tree component.

### Pitfall 6: AI Generate Does Not Refresh FactionTree
**What goes wrong:** After AI generates worldview, GeographyTree refreshes but FactionTree does not.
**Why it happens:** Forgot to add `factionTreeRef.value?.refresh()` in both `handleGenerate()` and `restoreGeneratingState()`.
**How to avoid:** Add the refresh call in both places, exactly mirroring the existing `geographyTreeRef.value?.refresh()` calls.
**Warning signs:** After AI generation, GeographyTree updates but FactionTree shows stale data until page refresh.

## Code Examples

### Type Badge Color Mapping
```typescript
// Maps backend type values to display labels and Tailwind color classes
const typeConfig: Record<string, { label: string; bg: string; text: string }> = {
  ally:    { label: '正派', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  hostile: { label: '反派', bg: 'bg-red-100 dark:bg-red-900/30',    text: 'text-red-700 dark:text-red-400' },
  neutral: { label: '中立', bg: 'bg-gray-100 dark:bg-gray-700',     text: 'text-gray-600 dark:text-gray-400' },
}
```

### Power System Name Resolution
```typescript
// Build ID->name map from power system list
const powerSystemMap = ref<Map<number, string>>(new Map())

const loadPowerSystems = async () => {
  try {
    const list = await getPowerSystemList(props.projectId)
    powerSystemMap.value = new Map(list.map(ps => [ps.id!, ps.name]))
  } catch (e) {
    console.error('加载力量体系失败:', e)
  }
}

// Usage in template: powerSystemMap.get(node.corePowerSystem!)
```

### Node Display Format (Top-Level Only)
```vue
<!-- Type badge + name + power system label (top-level only) -->
<template v-if="node.deep === 0 && node.type">
  <span :class="['px-1.5 py-0.5 text-xs rounded', typeConfig[node.type]?.bg, typeConfig[node.type]?.text]">
    {{ typeConfig[node.type]?.label }}
  </span>
</template>
<span class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ node.name }}</span>
<template v-if="node.deep === 0 && node.corePowerSystem">
  <span class="text-xs text-gray-400"> · {{ powerSystemMap.get(node.corePowerSystem) }}</span>
</template>
```

### Type Selector (Inline Expand)
```vue
<!-- Type selector: simple button group that expands below the node -->
<div v-if="editingType" class="flex items-center gap-2 mt-1">
  <button
    v-for="(config, key) in typeConfig" :key="key"
    @click="editingTypeValue = key"
    :class="['px-2 py-1 text-xs rounded', editingTypeValue === key ? config.bg + ' ' + config.text : 'bg-gray-100 dark:bg-gray-700 text-gray-500']"
  >
    {{ config.label }}
  </button>
  <button @click="saveType" class="px-2 py-1 text-xs text-white bg-blue-500 rounded">确定</button>
  <button @click="cancelTypeEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
</div>
```

### WorldSetting.vue Integration Points (Exact Lines)
```vue
<!-- Line 329-341: REMOVE this entire block -->
<!-- REPLACE with: -->
<FactionTree ref="factionTreeRef" :project-id="projectId()" :disabled="generating" />

<!-- Line 153 (in handleGenerate): ADD after geographyTreeRef refresh -->
factionTreeRef.value?.refresh()

<!-- Line 188 (in restoreGeneratingState): ADD after geographyTreeRef refresh -->
factionTreeRef.value?.refresh()

<!-- Line 16 (imports): ADD -->
import FactionTree from './components/FactionTree.vue'

<!-- Line 33 (refs): ADD -->
const factionTreeRef = ref()
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| GeographyTree manual 4-level nesting | Recursive self-referencing component | Project decision D-01 | Cleaner code, supports unlimited depth |
| Forces as textarea | Structured tree component | This phase | Full CRUD capability for factions |
| Backend returns only IDs | Frontend resolves IDs to names | Established pattern | Need parallel data fetch for power system list |

**Deprecated/outdated:**
- None relevant. All current versions are stable and well-supported.

## Open Questions

1. **Power System Selector in Edit Mode**
   - What we know: D-04 says type and power system use selectors that expand below the node. Power system needs a dropdown/selector from the power system list.
   - What's unclear: Should the power system selector allow "no power system" (clear the field)?
   - Recommendation: Yes, include a "无" (none) option. Some factions may not have an associated power system.

2. **Type Field for New Faction Nodes**
   - What we know: Type is only set on top-level factions (D-02, backend D-02 in STATE.md). Child factions inherit from parent.
   - What's unclear: When adding a child faction, should the type/powerSystem fields be hidden entirely?
   - Recommendation: Yes, hide type and power system selectors for child nodes (deep > 0). They inherit from the root parent automatically via backend logic.

3. **Add Form Fields for Root vs Child**
   - What we know: GeographyTree uses the same add form for root and child nodes. But factions have extra fields (type, corePowerSystem) for root nodes.
   - What's unclear: Should the root-level add form include type and power system fields?
   - Recommendation: Keep the add form simple (name + description only). Type and power system can be set after creation via inline editing. This matches GeographyTree's minimal add form pattern.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Frontend build | Yes | 24.14.1 | -- |
| npm | Package management | Yes | 11.11.0 | -- |
| Vite dev server | Development | Yes | ^7.2.4 | -- |
| Backend API (FactionController) | Data operations | Yes (Phase 2) | -- | -- |

**Missing dependencies with no fallback:**
- None -- all dependencies are available.

**Missing dependencies with fallback:**
- None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None detected -- no test runner configured in frontend |
| Config file | None |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UI-01 | FactionTree renders tree, expand/collapse, CRUD, type badges, power system labels | Manual (browser) | N/A | No |
| UI-02 | faction.ts API client calls correct endpoints | Manual (network tab) | N/A | No |
| UI-03 | WorldSetting.vue shows FactionTree, no textarea | Manual (browser) | N/A | No |

### Sampling Rate
- **Per task commit:** Visual verification in browser (no automated tests)
- **Per wave merge:** Full manual test pass
- **Phase gate:** All 3 success criteria verified manually

### Wave 0 Gaps
- No test infrastructure exists for the frontend. Given the UI-heavy nature of this phase and the project's current state (no test runner, no test scripts in package.json), manual browser verification is the appropriate approach.
- Adding Vitest or similar is OUT OF SCOPE for this phase and would be a separate infrastructure investment.

## Sources

### Primary (HIGH confidence)
- `ai-factory-frontend/src/views/Project/Detail/components/GeographyTree.vue` -- Complete reference implementation for tree component pattern (354 lines, all CRUD operations, dark mode, inline editing)
- `ai-factory-frontend/src/api/continentRegion.ts` -- API client pattern (interface + 4 CRUD functions)
- `ai-factory-frontend/src/api/powerSystem.ts` -- Power system list API (getPowerSystemList for ID->name resolution)
- `ai-factory-backend/src/main/java/com/aifactory/controller/FactionController.java` -- Backend API endpoints (tree/save/update/delete, all verified)
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java` -- Entity fields (id, parentId, deep, sortOrder, name, type, corePowerSystem, description, children)
- `ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue` -- Integration point (lines 329-341 for replacement, lines 153/188 for refresh calls)
- `ai-factory-frontend/package.json` -- Version verification

### Secondary (MEDIUM confidence)
- Vue 3 recursive component documentation -- self-referencing via filename confirmed by web search and Vue 3 documentation patterns
- [StackOverflow: Recursive components in Vue 3](https://stackoverflow.com/questions/75765611/how-to-get-the-reference-of-recursive-components-in-vue3)
- [Medium: Recursive Components in Vue 3](https://michael-verschoof.medium.com/recursive-components-in-vue-3-e897643df82)

### Tertiary (LOW confidence)
- None -- all findings verified against codebase.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all packages already installed, versions verified in package.json
- Architecture: HIGH -- patterns directly copied from existing GeographyTree.vue and continentRegion.ts
- Pitfalls: HIGH -- identified from direct code analysis of entity fields, API patterns, and component integration points

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable frontend patterns, no fast-moving dependencies)
