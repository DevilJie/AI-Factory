# Phase 5: 关联管理界面 - Research

**Researched:** 2026-04-02
**Domain:** Vue 3 Frontend - Drawer/Tab UI pattern with association CRUD management
**Confidence:** HIGH

## Summary

Phase 5 在已完成的势力树组件和后端关联 API 基础上，为每个势力节点添加关联管理入口。用户通过点击势力节点的关联按钮打开侧边 Drawer 抽屉，在抽屉内通过 Tab 切换管理三种关联：势力关系（盟友/敌对/中立）、势力-人物关联（含职位）、势力-地区关联。后端 API 已完全就绪（FactionController 中三个关联端点组），前端需要新增约 9 个 API 函数、1 个 Drawer 组件和 3 个 Tab 内容组件。

**Primary recommendation:** 沿用 SettingsDrawer.vue 的 Teleport + Transition 抽屉模式，在 FactionTree.vue 节点操作按钮区新增 Link 图标按钮，Drawer 内部用 Tab 分区实现三种关联管理。每个 Tab 独立管理自己的数据加载和操作。

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 关联管理入口集成在 FactionTree 内 -- 每个势力节点加关联管理按钮（如 link/chain 图标），点击后打开侧边 Drawer 抽屉
- **D-02:** 抽屉内用 Tab 分区显示关系/人物/地区三种关联，每个 Tab 独立管理
- **D-03:** 抽屉打开时加载当前势力的所有关联数据，操作完成后自动刷新
- **D-04:** 选择器 + 列表模式 -- 下拉选择目标势力 + 关系类型选择 + 描述文本框，已有关系显示为可删除列表
- **D-05:** 双向关系 -- 添加 A->B 关系时自动创建 B->A 同类型关系，删除时也双向删除。后端 API 调用两次或在一次请求中处理双向
- **D-06:** 标准表单模式 -- 关系添加区域固定显示在列表上方，不是内联展开
- **D-07:** 目标势力选择范围：所有势力可选（排除自身），包含不同顶级势力下的子势力
- **D-08:** 搜索下拉选择人物 -- 带关键词过滤的下拉组件，输入时实时过滤人物名
- **D-09:** 职位（role）用预设选项 + 自定义输入 -- 预设：掌门/副掌门/长老/弟子/护法/执事，同时允许自由输入其他职位名
- **D-10:** 允许一个人物关联多个势力 -- 数据库不约束唯一性，同一人物可在不同势力担任不同职位
- **D-11:** 树形选择器 -- 下拉展示地区树（缩进显示层级），支持勾选地区节点
- **D-12:** 多选批量添加 -- 一次可勾选多个地区，提交后批量创建关联

### Claude's Discretion
- Drawer/抽屉的具体宽度、动画细节
- 关系类型的颜色/图标映射（盟友=绿色？敌对=红色？中立=灰色？）
- 搜索下拉的防抖时间、空状态文案
- 树形选择器的展开/折叠行为
- 关联列表的排序方式（按添加时间？按名称？）
- 错误状态和空状态的处理

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-04 | 势力-势力关系管理界面（选择两个势力、关系类型、描述） | 后端 API 就绪（POST/GET/DELETE /{factionId}/relations），前端需新增 API 函数 + RelationTab 组件。双向关系需前端调两次 POST。目标势力列表用 getFactionTree 或 list 端点获取所有势力。 |
| UI-05 | 势力-人物手动关联界面（选择人物、分配职位） | 后端 API 就绪（POST/GET/DELETE /{factionId}/characters），前端需新增 API 函数 + CharacterTab 组件。人物列表用 getCharacterList 端点获取，前端本地关键词过滤。职位用 datalist 或 select+input 组合实现预设+自定义。 |
| UI-06 | 势力-地区关联界面（选择已有地区关联到势力） | 后端 API 就绪（POST/GET/DELETE /{factionId}/regions），前端需新增 API 函数 + RegionTab 组件。地区树用 getGeographyTree 获取，批量添加循环调用 POST。 |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3.5.x | 3.5.x | UI framework | Project standard, Composition API with `<script setup>` |
| TypeScript 5.9.x | 5.9.x | Type safety | Project standard, strict mode enabled |
| Tailwind CSS 4.1.x | 4.1.x | Styling | Project standard, all components use utility classes |
| Lucide Vue Next | 0.469.x | Icons | Project standard, all icons from this library |
| Axios 1.13.x | 1.13.x | HTTP client | Project standard via `@/utils/request` wrapper |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Pinia 3.0.x | 3.0.x | State management | Not needed for this phase -- Drawer uses local component state |

### No New Dependencies
**This phase requires ZERO new npm packages.** All functionality is achievable with:
- Native HTML `<select>` + `<input>` for dropdowns (project pattern, no UI library)
- Native `<datalist>` for preset + custom input (D-09 职位)
- Custom tree selector using recursive Vue components (matches GeographyTree pattern)
- Tailwind CSS for all styling (project standard)

## Architecture Patterns

### Recommended Component Structure
```
src/views/Project/Detail/components/
├── FactionTree.vue              # EXISTING - add Link icon button per node
├── FactionDrawer.vue            # NEW - Teleport Drawer with tabs
├── FactionRelationTab.vue       # NEW - 势力关系 Tab content
├── FactionCharacterTab.vue      # NEW - 势力-人物 Tab content
├── FactionRegionTab.vue         # NEW - 势力-地区 Tab content
```

### Pattern 1: Drawer Pattern (Teleport + Transition)
**What:** 侧边抽屉，使用 Teleport 挂载到 body，Transition 实现滑入滑出动画
**When to use:** 关联管理抽屉，参照 SettingsDrawer.vue 模式
**Example:**
```vue
<!-- Source: SettingsDrawer.vue pattern -->
<Teleport to="body">
  <!-- 遮罩层 -->
  <Transition
    enter-active-class="transition-opacity duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-300"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div v-if="isOpen" class="fixed inset-0 bg-black/50 z-40" @click="close" />
  </Transition>

  <!-- 抽屉面板 -->
  <Transition
    enter-active-class="transition-transform duration-300 ease-out"
    enter-from-class="translate-x-full"
    enter-to-class="translate-x-0"
    leave-active-class="transition-transform duration-300 ease-in"
    leave-from-class="translate-x-0"
    leave-to-class="translate-x-full"
  >
    <div v-if="isOpen"
      class="fixed top-0 right-0 h-full w-full max-w-2xl bg-white dark:bg-gray-900 z-50 shadow-2xl flex flex-col"
    >
      <!-- 头部 with tabs -->
      <div class="flex items-center justify-between px-6 py-4 border-b ...">
        <div class="flex gap-1 p-1 rounded-xl bg-gray-100 dark:bg-gray-800">
          <button v-for="tab in tabs" :key="tab.key"
            :class="[activeTab === tab.key ? 'bg-white shadow-sm' : 'text-gray-500']"
            @click="activeTab = tab.key"
          >{{ tab.label }}</button>
        </div>
        <button @click="close"><X /></button>
      </div>
      <!-- 内容区域 -->
      <div class="flex-1 overflow-y-auto p-6">
        <FactionRelationTab v-if="activeTab === 'relation'" ... />
        <FactionCharacterTab v-if="activeTab === 'character'" ... />
        <FactionRegionTab v-if="activeTab === 'region'" ... />
      </div>
    </div>
  </Transition>
</Teleport>
```

### Pattern 2: API Client Extension
**What:** 在 faction.ts 中添加关联 API 函数，沿用现有 request + async/await 模式
**When to use:** 所有关联 CRUD 操作
**Example:**
```typescript
// Source: faction.ts pattern + FactionController endpoints
// 势力关系 API
export const getFactionRelations = (projectId: string, factionId: number) => {
  return request.get<FactionRelation[]>(`/api/novel/${projectId}/faction/${factionId}/relations`)
}
export const addFactionRelation = (projectId: string, factionId: number, data: { targetFactionId: number; relationType: string; description?: string }) => {
  return request.post<FactionRelation>(`/api/novel/${projectId}/faction/${factionId}/relations`, data)
}
export const deleteFactionRelation = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/relations/${id}`)
}
```

### Pattern 3: Dual API Call for Bidirectional Relations (D-05)
**What:** 添加 A->B 关系时，前端连续调用两次 POST（一次用 factionId=A/targetFactionId=B，一次用 factionId=B/targetFactionId=A）
**When to use:** 关系添加和删除操作
**Example:**
```typescript
// 双向添加
const addBidirectionalRelation = async (projectId: string, factionA: number, factionB: number, type: string, desc?: string) => {
  await addFactionRelation(projectId, factionA, { targetFactionId: factionB, relationType: type, description: desc })
  await addFactionRelation(projectId, factionB, { targetFactionId: factionA, relationType: type, description: desc })
}
// 双向删除需要先查出 B->A 的记录 ID
const deleteBidirectionalRelation = async (projectId: string, factionId: number, relation: FactionRelation) => {
  // 删除当前视角的关系
  await deleteFactionRelation(projectId, factionId, relation.id)
  // 查找反向关系并删除
  const reverseRelations = await getFactionRelations(projectId, relation.targetFactionId)
  const reverse = reverseRelations.find(r => r.targetFactionId === factionId && r.relationType === relation.relationType)
  if (reverse?.id) {
    await deleteFactionRelation(projectId, relation.targetFactionId, reverse.id)
  }
}
```

### Pattern 4: Preset + Custom Input for Role (D-09)
**What:** 使用 `<datalist>` HTML 元素实现预设选项 + 自由输入
**When to use:** 人物关联的职位字段
**Example:**
```vue
<input list="role-options" v-model="selectedRole" placeholder="选择或输入职位" />
<datalist id="role-options">
  <option v-for="role in presetRoles" :value="role" />
</datalist>
```

### Pattern 5: Recursive Tree Selector for Regions (D-11)
**What:** 自定义递归组件渲染地区树，带 checkbox 勾选，缩进显示层级
**When to use:** 地区关联的选择器部分
**Example:**
```vue
<div v-for="region in regionTree" :key="region.id" class="space-y-0.5">
  <label class="flex items-center gap-2 py-1" :style="{ paddingLeft: `${region.deep * 16}px` }">
    <input type="checkbox" :value="region.id" v-model="selectedRegionIds" />
    <span class="text-sm">{{ region.name }}</span>
  </label>
  <!-- Recursive children handled by flattening tree or inline template -->
</div>
```

### Anti-Patterns to Avoid
- **引入 UI 组件库（如 Element Plus、Ant Design Vue）:** 项目使用纯 Tailwind CSS + 原生 HTML，不使用任何 UI 框架。所有组件手写。
- **将 Drawer 状态放到 Pinia store:** 项目 SettingsDrawer 用 composable，但本阶段 Drawer 仅在 FactionTree 内使用，用组件 props/emits 即可，不需要全局状态。
- **在 FactionTree.vue 内写所有 Tab 逻辑:** 应拆分为独立 Tab 组件，每个组件管理自己的数据和操作，FactionTree 只负责打开 Drawer 和传递 factionId。
- **在模板中直接写 API 调用:** 所有 API 调用通过 faction.ts 的函数封装，组件内通过 async 方法调用。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 双向关系管理 | 自己写后端双向逻辑 | 前端调两次 API（D-05 确认） | 后端 API 已是单条记录操作，前端控制双向更灵活 |
| 人员列表过滤 | 后端搜索 API | 前端 getCharacterList + 本地 keyword filter | 人物列表通常不超百条，前端过滤更即时，避免防抖复杂性 |
| 地区树数据 | 自己组装树 | getGeographyTree API | 后端已返回完整树结构，直接使用 |
| 势力列表（目标选择） | 自己写扁平化 | getFactionTree 或 list 端点 | 后端已有 list 端点返回扁平列表，更适合下拉选择 |

## Common Pitfalls

### Pitfall 1: 双向关系重复显示
**What goes wrong:** A->B 和 B->A 都存储，UI 显示关系列表时两条都出现，用户看到重复
**Why it happens:** 后端按 factionId 查询，A 的关系列表只有 A->B 方向，不会重复
**How to avoid:** 关系列表只显示当前 factionId 视角（GET /{factionId}/relations），天然不会重复。添加时前端确保双向都写入。
**Warning signs:** 关系列表出现 A->B 和 B->A 两条相同关系

### Pitfall 2: 递归组件中 Drawer 状态冲突
**What goes wrong:** FactionTree 是递归组件，每个节点都有操作按钮，多个节点共享同一个 Drawer 状态
**Why it happens:** Drawer 状态放在 FactionTree 根组件，但触发在子组件
**How to avoid:** Drawer 只在根 FactionTree 实例中渲染（isRoot 判断），子组件通过 emit 通知根组件打开 Drawer。与 delete 的冒泡模式一致。
**Warning signs:** 点击子势力节点的关联按钮没有反应，或 Drawer 数据错乱

### Pitfall 3: 目标势力选择器包含已删除/不存在的势力
**What goes wrong:** 目标势力列表过时，选择已删除的势力导致关联创建失败
**Why it happens:** 选择器数据在 Drawer 打开时加载一次，但外部可能已删除势力
**How to avoid:** Drawer 打开时重新加载势力列表（通过 list 端点），与关联数据同时刷新
**Warning signs:** API 返回 500 或外键约束错误

### Pitfall 4: 地区树选择器全量展开导致页面过长
**What goes wrong:** 地区树可能有很多层级节点，全部展开让下拉区域非常长
**Why it happens:** 没有折叠/展开控制
**How to avoid:** 树形选择器默认折叠，只展开第一级，用户点击展开子级。参考 GeographyTree 的展开/折叠行为。
**Warning signs:** 选择器下拉区域超过视口高度

### Pitfall 5: 人物选择时 ID 类型不匹配
**What goes wrong:** character.ts 中 Character.id 是 string 类型，但后端 NovelFactionCharacter.characterId 是 Long/number
**Why it happens:** 前端 API 层将后端 number 转成了 string（见 character.ts 第75行 `id: String(dto.id)`）
**How to avoid:** 调用 addFactionCharacter 时，确保 characterId 传 number 类型（`Number(character.id)`），与后端 Long 类型对齐
**Warning signs:** 后端收到的 characterId 为 null 或类型转换错误

### Pitfall 6: Drawer 关闭时未清理状态
**What goes wrong:** 关闭 Drawer 再打开另一个势力节点，显示上一个势力的数据
**Why it happens:** 组件缓存（v-if vs v-show），切换势力时未重置内部状态
**How to avoid:** 使用 v-if 条件渲染 Tab 组件（非 v-show），每次打开 Drawer 切换势力时 Tab 组件重新创建。或通过 watch factionId 变化重载数据。
**Warning signs:** 切换势力节点后关联列表显示错误数据

## Code Examples

### Faction API Extensions (faction.ts)
```typescript
// Source: faction.ts existing pattern + FactionController endpoints

// 类型定义
export interface FactionRelation {
  id?: number
  factionId?: number
  targetFactionId?: number
  relationType: string    // ally/hostile/neutral
  description?: string
}

export interface FactionCharacter {
  id?: number
  factionId?: number
  characterId: number
  role?: string
}

export interface FactionRegion {
  id?: number
  factionId?: number
  regionId: number
}

// 关系 API
export const getFactionRelations = (projectId: string, factionId: number) => {
  return request.get<FactionRelation[]>(`/api/novel/${projectId}/faction/${factionId}/relations`)
}
export const addFactionRelation = (projectId: string, factionId: number, data: Partial<FactionRelation>) => {
  return request.post<FactionRelation>(`/api/novel/${projectId}/faction/${factionId}/relations`, data)
}
export const deleteFactionRelation = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/relations/${id}`)
}

// 人物 API
export const getFactionCharacters = (projectId: string, factionId: number) => {
  return request.get<FactionCharacter[]>(`/api/novel/${projectId}/faction/${factionId}/characters`)
}
export const addFactionCharacter = (projectId: string, factionId: number, data: Partial<FactionCharacter>) => {
  return request.post<FactionCharacter>(`/api/novel/${projectId}/faction/${factionId}/characters`, data)
}
export const deleteFactionCharacter = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/characters/${id}`)
}

// 地区 API
export const getFactionRegions = (projectId: string, factionId: number) => {
  return request.get<FactionRegion[]>(`/api/novel/${projectId}/faction/${factionId}/regions`)
}
export const addFactionRegion = (projectId: string, factionId: number, data: Partial<FactionRegion>) => {
  return request.post<FactionRegion>(`/api/novel/${projectId}/faction/${factionId}/regions`, data)
}
export const deleteFactionRegion = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/regions/${id}`)
}
```

### Relation Type Color Mapping (Claude's Discretion)
```typescript
// Consistent with FactionTree.vue typeConfig pattern
const relationTypeConfig: Record<string, { label: string; bg: string; text: string }> = {
  ally:    { label: '盟友', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  hostile: { label: '敌对', bg: 'bg-red-100 dark:bg-red-900/30',    text: 'text-red-700 dark:text-red-400' },
  neutral: { label: '中立', bg: 'bg-gray-100 dark:bg-gray-700',     text: 'text-gray-600 dark:text-gray-400' },
}
```

### Lucide Icons for Association Button
```typescript
// Recommended icons from lucide-vue-next (already in project)
import { Link2, Users, MapPin, Swords, Handshake, Minus } from 'lucide-vue-next'
// Link2 - association button on tree node
// Users/Handshake - relation tab icon
// MapPin - region tab icon
// Swords - could represent hostile relation
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| UI component library (Element Plus) | Pure Tailwind + native HTML | Project convention | All UI components hand-built, no library dependencies |
| Pinia global state | Local component state + emit pattern | Phase 4 established | FactionTree uses local state, not Pinia |

**Deprecated/outdated:**
- N/A -- no deprecated patterns in this phase scope

## Open Questions

1. **双向关系显示策略**
   - What we know: 后端按 factionId 查询，A 只看到 A->B 方向的关系
   - What's unclear: 是否需要在 UI 上显示"对方势力也对此有相同关系"的提示
   - Recommendation: 不需要，保持简单。用户只看当前势力视角，D-05 已确认双向自动处理

2. **人物列表加载时机**
   - What we know: character.ts 的 getCharacterList 会加载项目下所有人物
   - What's unclear: 人物数量可能较多时的性能影响
   - Recommendation: 在 Drawer 打开时加载一次，前端本地过滤，不需要后端搜索 API

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js 18+ | Frontend build | Need to verify | -- | -- |
| npm 9+ | Package management | Need to verify | -- | -- |
| Backend API | Association CRUD | Yes (Phase 2 complete) | -- | -- |
| MySQL | Data storage | Yes | 8.0+ | -- |

**Missing dependencies with no fallback:**
- None -- all backend APIs are implemented, no new npm packages needed

**Missing dependencies with fallback:**
- None

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest (via Vite) -- if configured; otherwise manual UI verification |
| Config file | Not detected -- see Wave 0 |
| Quick run command | `cd ai-factory-frontend && npx vitest run --reporter=verbose 2>/dev/null || echo "No test runner configured"` |
| Full suite command | Same as quick -- manual UI verification primary |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UI-04 | 势力关系添加/删除/列表显示 | Manual (UI) | `npx vitest run` | No Wave 0 |
| UI-04 | 双向关系同步 | Manual (UI) | `npx vitest run` | No Wave 0 |
| UI-05 | 人物关联添加/删除，职位选择 | Manual (UI) | `npx vitest run` | No Wave 0 |
| UI-06 | 地区关联批量添加/删除 | Manual (UI) | `npx vitest run` | No Wave 0 |

### Sampling Rate
- **Per task commit:** Visual verification in browser + TypeScript compilation check (`npx vue-tsc --noEmit`)
- **Per wave merge:** Full manual verification of all three association types
- **Phase gate:** All three association CRUD operations verified end-to-end

### Wave 0 Gaps
- [ ] No frontend test infrastructure detected -- manual verification is the primary approach
- [ ] TypeScript compilation check (`npx vue-tsc --noEmit`) available as automated gate
- [ ] Consider adding basic Vitest config for future phases

## Sources

### Primary (HIGH confidence)
- FactionController.java -- verified all 9 association endpoints exist (relations/characters/regions x GET/POST/DELETE)
- NovelFactionRelation.java -- verified entity fields (id, factionId, targetFactionId, relationType, description)
- NovelFactionCharacter.java -- verified entity fields (id, factionId, characterId, role)
- NovelFactionRegion.java -- verified entity fields (id, factionId, regionId)
- FactionTree.vue -- verified recursive component structure, node action buttons pattern, isRoot/emit pattern
- SettingsDrawer.vue -- verified Teleport+Transition Drawer pattern with tabs
- faction.ts -- verified existing API client pattern and Faction interface
- character.ts -- verified CharacterDto type and getCharacterList function
- continentRegion.ts -- verified getGeographyTree function returns tree structure

### Secondary (MEDIUM confidence)
- GeographyTree.vue -- verified tree component pattern and styling conventions
- WorldSetting.vue -- verified FactionTree integration pattern

### Tertiary (LOW confidence)
- None -- all findings verified against source code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing project patterns
- Architecture: HIGH -- Drawer pattern from SettingsDrawer.vue, API pattern from faction.ts, tree pattern from GeographyTree.vue
- Pitfalls: HIGH -- based on direct code analysis of entity types, component recursion, and API response handling
- API integration: HIGH -- FactionController endpoints fully read and verified

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable codebase, no fast-moving dependencies)
