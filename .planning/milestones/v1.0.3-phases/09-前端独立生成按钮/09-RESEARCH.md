# Phase 9: 前端独立生成按钮 - Research

**Researched:** 2026-04-03
**Domain:** Vue 3 Frontend - Async Task UI with Polling, State Management, Component Communication
**Confidence:** HIGH

## Summary

Phase 9 is a frontend-only phase that adds independent AI generation buttons to three worldview sub-module sections (GeographyTree, PowerSystemSection, FactionTree). Each button triggers an independent generation API endpoint created in Phase 7, polls the task status until completion, then refreshes the corresponding tree/list data. The backend APIs are already complete and return `{ taskId, message }` following the same pattern as the existing `generateWorldviewAsync`.

The existing `WorldSetting.vue` already implements the full task polling lifecycle for the whole-worldview generation: calling the API, saving state to localStorage, polling via `getTaskStatus`, restoring state on page refresh, and refreshing child components. This phase replicates that pattern three times with module-specific variations (different API endpoints, different refresh targets, different localStorage keys, mutual exclusion between modules).

**Primary recommendation:** Extract the polling logic into a composable (`useTaskPolling`) shared across all four generation types (1 whole + 3 independent), keeping state management in `WorldSetting.vue` as the single source of truth. The three child components only need to receive a new `generating` boolean prop and emit a trigger event; the actual API call, polling, and state persistence all live in `WorldSetting.vue`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** AI 生成按钮放在各模块 header 右侧，与蓝色「添加」按钮并排。三个模块（GeographyTree、PowerSystemSection、FactionTree）各自的 header 区域都需新增此按钮
- **D-02:** 按钮使用绿色渐变 + Sparkles 图标，与页面顶部的 AI 生成按钮视觉风格一致，让用户一眼识别为 AI 操作
- **D-03:** 页面顶部的现有「AI生成」按钮（整体生成）保留不变，文案不变
- **D-04:** 独立生成中，该模块按钮显示 spinner + 文字变为「生成中...」，模块内树/列表进入 disabled 状态（不可操作）
- **D-05:** 三个模块的 AI 生成互斥 — 同一时间只能有一个模块在生成。如果某模块正在生成，其他两个模块的 AI 生成按钮 disabled
- **D-06:** 独立生成期间，页面顶部的整体「AI生成」按钮也 disabled（避免与独立生成冲突）
- **D-07:** 每个模块使用独立的 localStorage key 恢复生成状态，格式：`ai-factory-{module}-generate-{projectId}`（module = geography / powerSystem / faction）
- **D-08:** 页面刷新后仅恢复对应模块的生成状态和轮询，其他模块正常可用
- **D-09:** 复用现有 pollTaskStatus 逻辑（3 秒间隔，60 次上限，10 分钟超时判断）
- **D-10:** 3 个独立生成 API 方法放在 worldview.ts 中，与 generateWorldviewAsync 并列

### Claude's Discretion
- 轮询逻辑是否提取为 composable（useTaskPolling）或保留在各组件内
- 错误提示的具体文案（含依赖校验失败的提示，后端已返回明确中文消息）
- 各模块生成成功后的 toast 提示文案

### Deferred Ideas (OUT OF SCOPE)
None
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-01 | 地理环境区域增加独立「AI 生成」按钮，点击调用地理环境独立生成接口，生成完成后刷新树形数据 | GeographyTree.vue header 需新增按钮，调用 `generateGeographyAsync`，轮询完成后调 `geographyTreeRef.refresh()` |
| UI-02 | 力量体系区域增加独立「AI 生成」按钮，点击调用力量体系独立生成接口，生成完成后刷新树形数据 | PowerSystemSection.vue header 需新增按钮，调用 `generatePowerSystemAsync`，轮询完成后调 `powerSystemRef.refresh()` |
| UI-03 | 阵营势力区域增加独立「AI 生成」按钮，点击调用阵营势力独立生成接口，生成完成后刷新树形数据 | FactionTree.vue header 需新增按钮，调用 `generateFactionAsync`，轮询完成后调 `factionTreeRef.refresh()` |
| UI-04 | 独立生成按钮的加载状态和错误提示（含依赖校验失败的提示信息） | 按钮显示 spinner + disabled 状态；后端依赖校验返回 `Result.error("请先生成地理环境数据")` 中文消息，前端 axios 拦截器自动 toast 展示 |
</phase_requirements>

## Standard Stack

### Core (All Already Installed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3.5.x | installed | Reactive UI framework | Project standard, Composition API with `<script setup>` |
| Lucide Vue 0.469.x | installed | Icon library (Sparkles, Loader2) | Project standard, already imported in WorldSetting.vue |
| Pinia 3.0.x | installed | State management | Available but not needed for this phase -- ref-based state is sufficient |
| Axios 1.13.x | installed | HTTP client | Project standard, `request.ts` wrapper handles response unwrapping |

### No New Dependencies

This phase requires **zero new package installations**. Everything needed is already in the project:
- `lucide-vue-next` provides `Sparkles` and `Loader2` icons (already imported)
- `@/api/task.ts` provides `getTaskStatus` polling
- `@/utils/toast` provides `success` / `error` notifications
- `localStorage` is used for task state persistence (no library needed)

## Architecture Patterns

### Recommended Implementation Architecture

```
WorldSetting.vue (State Owner)
  +-- generatingModule: ref<'geography'|'powerSystem'|'faction'|'whole'|null>
  +-- handleGenerateGeography()     --> API call + poll + refresh
  +-- handleGeneratePowerSystem()   --> API call + poll + refresh
  +-- handleGenerateFaction()       --> API call + poll + refresh
  +-- handleGenerate()              --> existing whole-worldview generation
  |
  +-- GeographyTree   :disabled="!!generatingModule"  @generate="handleGenerateGeography"
  +-- PowerSystemSection :disabled="!!generatingModule" @generate="handleGeneratePowerSystem"
  +-- FactionTree     :disabled="!!generatingModule"  @generate="handleGenerateFaction"
```

**Key principle:** `WorldSetting.vue` remains the single source of truth for all generation state. Child components emit events; parent handles the full lifecycle.

### Pattern 1: Module-Level Generation in WorldSetting.vue
**What:** Each module's generation handler follows the exact same pattern as the existing `handleGenerate()`, but with module-specific API call, localStorage key, and refresh target.
**When to use:** For each of the 3 independent generation types.
**Example:**
```typescript
// In WorldSetting.vue -- new state
const generatingModule = ref<'geography' | 'powerSystem' | 'faction' | null>(null)

// localStorage key per module
const getModuleTaskKey = (module: string, projectId: string) =>
  `ai-factory-${module}-generate-${projectId}`

// Example: Geography generation handler
const handleGenerateGeography = async () => {
  generatingModule.value = 'geography'
  try {
    const result = await generateGeographyAsync(projectId())
    saveModuleTask('geography', result.taskId)
    await pollTaskStatus(result.taskId)
    clearModuleTask('geography')
    geographyTreeRef.value?.refresh()
    success('地理环境生成成功')
  } catch (e: any) {
    error(e.message || '地理环境生成失败')
  } finally {
    generatingModule.value = null
    clearModuleTask('geography')
  }
}
```

### Pattern 2: Child Component Button Placement
**What:** Each child component gains an AI generation button in its header, next to the existing blue "Add" button. The button emits a `generate` event; the parent handles the rest.
**When to use:** In GeographyTree.vue, PowerSystemSection.vue, FactionTree.vue headers.
**Example (GeographyTree.vue header):**
```html
<!-- Existing header structure -->
<div class="flex items-center justify-between mb-4">
  <div class="flex items-center gap-2">
    <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">地理环境</h3>
    <span v-if="treeData.length" class="text-xs text-gray-400">
      {{ treeData.length }} 个顶级区域，{{ countNodes(treeData) }} 个节点
    </span>
  </div>
  <div class="flex items-center gap-2">
    <!-- NEW: AI Generate Button -->
    <button
      @click="$emit('generate')"
      :disabled="disabled || generatingModule === 'geography'"
      class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-green-500 to-teal-500 rounded-lg hover:from-green-600 hover:to-teal-600 transition-colors disabled:opacity-50"
    >
      <Loader2 v-if="generatingModule === 'geography'" class="w-3 h-3 animate-spin" />
      <Sparkles v-else class="w-3 h-3" />
      {{ generatingModule === 'geography' ? '生成中...' : 'AI生成' }}
    </button>
    <!-- Existing: Add button -->
    <button @click="showAddForm(null)" :disabled="disabled" ...>
      <Plus class="w-3 h-3" />
      添加区域
    </button>
  </div>
</div>
```

### Pattern 3: localStorage State Recovery per Module
**What:** On page load, check each module's localStorage key independently. If a task is found and not timed out, resume polling for that specific module only.
**When to use:** In `onMounted` alongside existing `restoreGeneratingState()`.
**Example:**
```typescript
const restoreModuleState = async (module: 'geography' | 'powerSystem' | 'faction') => {
  const key = getModuleTaskKey(module, projectId())
  const taskData = localStorage.getItem(key)
  if (!taskData) return

  const { taskId, startTime } = JSON.parse(taskData)
  if (Date.now() - startTime > 10 * 60 * 1000) {
    localStorage.removeItem(key)
    return
  }

  generatingModule.value = module
  try {
    await pollTaskStatus(taskId)
    localStorage.removeItem(key)
    // Refresh the corresponding component
    const refMap = {
      geography: geographyTreeRef,
      powerSystem: powerSystemRef,
      faction: factionTreeRef
    }
    refMap[module].value?.refresh()
    success(`${moduleNames[module]}生成完成`)
  } catch (e: any) {
    error(e.message || `${moduleNames[module]}生成失败`)
  } finally {
    generatingModule.value = null
    localStorage.removeItem(key)
  }
}

// In onMounted:
onMounted(() => {
  loadData()
  restoreGeneratingState()        // existing whole-worldview restore
  restoreModuleState('geography') // independent module restore
  restoreModuleState('powerSystem')
  restoreModuleState('faction')
})
```

### Anti-Patterns to Avoid

- **Anti-pattern: Duplicate polling logic in each child component.** The polling function is identical for all modules. Keep it in `WorldSetting.vue` or extract to a composable. Do NOT copy `pollTaskStatus` into each child component.
- **Anti-pattern: Multiple simultaneous generations.** The single `generatingModule` ref ensures mutual exclusion. Never use 3 independent `generating` booleans -- race conditions will occur.
- **Anti-pattern: Full-page overlay for independent generation.** Only the specific module should be disabled, not the entire page. The existing `v-if="generating"` overlay applies only to whole-worldview generation.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Task polling | Custom polling with setTimeout chains | Existing `pollTaskStatus` in WorldSetting.vue | Already battle-tested with 3s interval, 60 attempts, error handling |
| Error display for dependency validation | Custom error parsing | Axios interceptor + toast | Backend returns `Result.error("请先生成地理环境数据")`, interceptor auto-toasts the `msg` field |
| localStorage persistence | New storage abstraction | Same pattern as existing `saveGeneratingTask/clearGeneratingTask` | Proven pattern, just use different key prefix per module |
| Loading spinner animation | Custom CSS animation | `<Loader2 class="animate-spin" />` | Tailwind utility, consistent with existing usage |

## Common Pitfalls

### Pitfall 1: Axios Interceptor Double-Toast
**What goes wrong:** The Axios response interceptor in `request.ts` (line 36) calls `toastError(res.msg)` when `code !== 0`. If the component also catches the rejected promise and calls `error()`, the user sees two toast messages for the same error.
**Why it happens:** The interceptor rejects the promise after showing the toast, but the catch block shows another toast.
**How to avoid:** In the component's catch block, do NOT call `error()` for API-level errors (where `res.code !== 0`). Only call `error()` for poll-level failures (task failed/cancelled/timeout) which bypass the interceptor. Alternatively, use a silent catch and let the interceptor handle all API errors.
**Warning signs:** User sees duplicate toast notifications.

### Pitfall 2: Whole-Worldview Generation vs Independent Generation Conflict
**What goes wrong:** User triggers whole-worldview generation (which internally calls all 3 modules via Phase 8 refactored orchestrator), and simultaneously an independent module generation is running.
**Why it happens:** D-06 specifies that independent generation should disable the whole-worldview button, but the reverse is also needed -- whole-worldview generation should disable all 3 independent buttons.
**How to avoid:** The existing `generating` ref (for whole-worldview) already disables all children via `:disabled="generating"`. The new logic adds: when `generatingModule` is set, disable the whole-worldview button too. Both conditions must be checked on both sides.
**Warning signs:** Clicking AI generation during another generation causes data corruption or task conflicts.

### Pitfall 3: Stale localStorage After Route Change
**What goes wrong:** User navigates to a different project, but the old project's localStorage keys remain. If the key format includes `projectId`, this is harmless but wastes storage.
**Why it happens:** `onUnmounted` deliberately does not clear localStorage (to allow recovery on page refresh). But when switching projects within the same SPA session, the old keys persist.
**How to avoid:** The key format already includes `projectId` (`ai-factory-{module}-generate-{projectId}`), so stale keys from other projects are naturally ignored during restoration. No additional cleanup needed, but be aware of the storage accumulation.
**Warning signs:** Developer tools show multiple localStorage keys from different projects.

### Pitfall 4: Button Props Not Propagated Correctly
**What goes wrong:** The child components receive `disabled` prop (boolean) but need to know *which* module is generating for button text/icon changes.
**Why it happens:** Current `disabled` is a simple boolean. For independent generation, the child needs to know if IT is the one generating vs. a sibling is generating.
**How to avoid:** Either pass a `generatingModule` string prop alongside `disabled`, OR have the button just emit and let the parent control all visual state. Recommended approach: emit `generate` event from child, parent handles everything including button state via prop binding (`:is-generating="generatingModule === 'geography'"`).
**Warning signs:** Button shows "generating" state even when a different module is generating.

### Pitfall 5: Component Refresh Timing
**What goes wrong:** After task completes, calling `refresh()` immediately but data not yet committed to DB.
**Why it happens:** Task status shows "completed" but DB transaction might still be committing.
**How to avoid:** The existing pattern (check task status via polling API, then refresh data) provides sufficient delay. The task status endpoint returns "completed" only after the strategy finishes all persistence. This is safe.
**Warning signs:** Refresh shows stale/empty data.

## Code Examples

### Verified pattern: API methods in worldview.ts

```typescript
// Source: Existing generateWorldviewAsync pattern in worldview.ts
// Add these 3 methods alongside the existing one

export const generateGeographyAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-geography`, {}
  )
}

export const generatePowerSystemAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-power-system`, {}
  )
}

export const generateFactionAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-faction`, {}
  )
}
```

### Verified pattern: Disabled prop binding with mutual exclusion

```html
<!-- Source: Existing pattern in WorldSetting.vue template -->
<!-- Current: :disabled="generating" applies to all children -->
<!-- New: Combined condition for independent + whole generation -->

<PowerSystemSection
  ref="powerSystemRef"
  :project-id="projectId()"
  :disabled="!!generating || !!generatingModule"
  :generating-self="generatingModule === 'powerSystem'"
  @generate="handleGeneratePowerSystem"
/>
```

### Verified pattern: Existing pollTaskStatus (reuse as-is)

```typescript
// Source: WorldSetting.vue lines 96-121
// No changes needed -- this function is generic and works for any taskId
const pollTaskStatus = async (
  taskId: string,
  maxAttempts: number = 60,
  interval: number = 3000
) => {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const task = await getTaskStatus(taskId)
      if (task.status === 'completed') return true
      if (task.status === 'failed' || task.status === 'cancelled') {
        throw new Error(task.errorMessage || 'AI生成世界观失败')
      }
      await new Promise(resolve => setTimeout(resolve, interval))
    } catch (e: any) {
      if (e.message?.includes('失败')) throw e
      console.log(`轮询第${i + 1}次出错，继续等待:`, e)
      await new Promise(resolve => setTimeout(resolve, interval))
    }
  }
  throw new Error('AI生成世界观超时')
}
```

### Verified pattern: localStorage per-module key

```typescript
// Source: Existing getGenerateTaskKey pattern (line 38-39)
// Per D-07, extend with module-specific prefix
const getModuleTaskKey = (module: string) =>
  `ai-factory-${module}-generate-${projectId()}`

const saveModuleTask = (module: string, taskId: string) => {
  localStorage.setItem(
    getModuleTaskKey(module),
    JSON.stringify({ taskId, startTime: Date.now() })
  )
}

const clearModuleTask = (module: string) => {
  localStorage.removeItem(getModuleTaskKey(module))
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Whole-worldview only | Independent per-module generation | Phase 7 (backend), Phase 9 (frontend) | Users can regenerate individual modules |
| Single generating state (boolean) | Module-aware generating state (ref with module name) | This phase | Mutual exclusion between modules |
| Full-page overlay during generation | Module-only disable during independent generation | This phase | Better UX -- other sections remain usable |

**No deprecation concerns** -- this phase extends existing patterns, does not replace them.

## Open Questions

1. **Composable extraction (Claude's Discretion)**
   - What we know: The polling logic is identical across 4 generation types. A `useTaskPolling` composable would reduce duplication.
   - What's unclear: Whether the code volume justifies extraction vs. inline duplication.
   - Recommendation: Given only 3 new handlers (~15 lines each) plus the existing handler, and the handlers differ in API call + refresh target + localStorage key, extracting a composable adds abstraction complexity for minimal gain. Recommend keeping the handlers inline in `WorldSetting.vue` -- the total new code is ~80 lines of handler logic, which is manageable in one file.

2. **Error message for dependency validation**
   - What we know: Backend returns `Result.error("请先生成地理环境数据")` and `Result.error("请先生成力量体系数据")`. The Axios interceptor auto-toasts these messages.
   - What's unclear: Whether the component should also show a custom message or rely solely on the interceptor.
   - Recommendation: Let the interceptor handle API-level errors (code !== 0). In the catch block, only handle poll-level errors (task failed, timeout). This avoids double-toast (Pitfall 1).

## Files to Modify

### 1. `ai-factory-frontend/src/api/worldview.ts`
**Change:** Add 3 new API methods (`generateGeographyAsync`, `generatePowerSystemAsync`, `generateFactionAsync`)
**Lines affected:** ~12 new lines at end of file
**Risk:** LOW -- trivial additions following existing pattern

### 2. `ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue`
**Change:** Add `generatingModule` state, 3 new handlers, 3 new localStorage helpers, module restore logic, update template to pass new props to children, update top-level AI button disabled condition
**Lines affected:** ~80 new lines (script) + ~20 modified lines (template)
**Risk:** MEDIUM -- central state management changes, must handle mutual exclusion correctly

### 3. `ai-factory-frontend/src/views/Project/Detail/components/GeographyTree.vue`
**Change:** Add AI generate button to header, accept new props (`generatingSelf`), emit `generate` event
**Lines affected:** ~15 new/modified lines in header section
**Risk:** LOW -- UI-only changes, no logic

### 4. `ai-factory-frontend/src/views/Project/Detail/components/PowerSystemSection.vue`
**Change:** Add AI generate button to header, accept new props (`generatingSelf`), emit `generate` event
**Lines affected:** ~15 new/modified lines in header section
**Risk:** LOW -- UI-only changes, no logic

### 5. `ai-factory-frontend/src/views/Project/Detail/components/FactionTree.vue`
**Change:** Add AI generate button to header (root instance only, line 198 `v-if="isRoot"`), accept new props (`generatingSelf`), emit `generate` event
**Lines affected:** ~15 new/modified lines in header section
**Risk:** LOW -- UI-only changes, no logic. Note: Only root instance shows the header (v-if="isRoot" guard already in place).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Frontend build | Yes | 18+ | -- |
| npm | Package management | Yes | 9+ | -- |
| Vite | Dev server / build | Yes | 7.2.x | -- |
| Vue 3 | Framework | Yes | 3.5.x | -- |
| Lucide Vue | Icons | Yes | 0.469.x | -- |

**Missing dependencies with no fallback:** None

**Missing dependencies with fallback:** N/A

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None detected -- no test runner configured |
| Config file | None |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UI-01 | Geography AI button calls API, polls, refreshes | manual-only | N/A | N/A |
| UI-02 | PowerSystem AI button calls API, polls, refreshes | manual-only | N/A | N/A |
| UI-03 | Faction AI button calls API, polls, refreshes | manual-only | N/A | N/A |
| UI-04 | Loading state, error display, dependency validation messages | manual-only | N/A | N/A |

**Justification for manual-only:** This phase is entirely UI interaction (button clicks, visual state changes, API calls with polling). The project has no frontend test infrastructure (no Vitest, no Jest, no test scripts in package.json). Setting up a test framework would be out of scope for a UI-focused phase. Verification should be done visually in the browser with the dev server running.

### Sampling Rate
- **Per task commit:** Visual inspection in browser
- **Per wave merge:** Full manual walkthrough of all 4 requirements
- **Phase gate:** All 4 success criteria verified manually

### Wave 0 Gaps
- No test framework to set up -- this is a UI-only phase with no existing test infrastructure. Manual verification is appropriate.

## Project Constraints (from CLAUDE.md)

- **Tech Stack:** Vue 3 + Vite + Tailwind CSS -- no new dependencies needed
- **Frontend:** Follow GeographyTree.vue component pattern, maintain UI consistency
- **Conventions:** Composition API with `<script setup>`, PascalCase components, camelCase methods, `@/` import alias
- **Error handling:** Try-catch for async, toast for user feedback
- **Icons:** Lucide Vue (Sparkles, Loader2 already available)
- **No backend changes:** This phase modifies frontend files only

## Sources

### Primary (HIGH confidence)
- Source code analysis of `WorldSetting.vue` -- existing generation flow, polling, localStorage pattern
- Source code analysis of `worldview.ts` -- API method pattern
- Source code analysis of `task.ts` -- TaskDto interface, getTaskStatus method
- Source code analysis of `WorldviewController.java` -- backend endpoint paths, response format, dependency validation messages
- Source code analysis of `GeographyTree.vue`, `PowerSystemSection.vue`, `FactionTree.vue` -- header structure, disabled prop, refresh() exposure
- Source code analysis of `request.ts` -- Axios interceptor behavior, error auto-toast

### Secondary (MEDIUM confidence)
- CONTEXT.md decisions (D-01 through D-10) -- user-locked implementation choices
- Phase 7 CONTEXT.md -- backend API design, dependency validation behavior

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing code analyzed
- Architecture: HIGH -- existing patterns are clear and directly applicable
- Pitfalls: HIGH -- identified from source code analysis (interceptor double-toast, mutual exclusion)

**Research date:** 2026-04-03
**Valid until:** 2026-05-03 (stable frontend patterns, no external dependencies)
