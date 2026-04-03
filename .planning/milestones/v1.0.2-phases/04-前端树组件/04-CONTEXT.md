# Phase 4: 前端树组件 - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

用户可在世界观设置页面以树形视图查看、编辑、新增、删除势力，替代原有的纯文本 textarea。此阶段交付：FactionTree.vue 递归树组件、faction.ts API 客户端、WorldSetting.vue 集成。不包含势力关系、势力-人物、势力-地区的关联管理界面（Phase 5）。

</domain>

<decisions>
## Implementation Decisions

### 树组件渲染方式
- **D-01:** FactionTree 使用 Vue 3 递归组件（一个模板处理任意层级），不用 GeographyTree 的手动 4 层嵌套模式。递归组件更简洁（约 100-150 行 vs 350 行），且势力数据层级可能比地理区域更深

### 势力类型标签
- **D-02:** 类型用颜色 badge 显示在势力名称前面，格式：`[正派] 紫阳宗`。颜色映射：正派=绿色、反派=红色、中立=灰色。仅顶级势力显示（子势力继承顶级势力类型，Phase 1 D-05）

### 力量体系标签
- **D-03:** 力量体系名称用点号分隔在名称行内显示，格式：`[正派] 紫阳宗 · 仙道`。仅顶级势力显示（corePowerSystem 仅顶级设置）。前端需通过力量体系列表数据将 ID 解析为名称

### 编辑体验
- **D-04:** 混合编辑方式 — 名称和描述使用内联编辑（点击编辑按钮后原地变成输入框），类型和力量体系点击后在节点下方展开简单选择器。保持与 GeographyTree 一致的轻量编辑风格，同时处理势力多出的字段

### WorldSetting.vue 集成
- **D-05:** 完全替换势力 textarea，不保留只读文本。FactionTree 组件占据整行，与 GeographyTree 布局一致
- **D-06:** AI 生成世界观后自动刷新 FactionTree（跟 GeographyTree 一样，通过 ref.refresh() 调用）

### Claude's Discretion
- 递归组件的具体实现细节（props 定义、事件处理）
- 类型选择器和力量体系选择器的具体 UI 细节
- 空状态文案和提示信息
- 力量体系名称的获取方式（从现有 API 获取列表 vs 后端返回时直接包含名称）

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 前端参考组件
- `ai-factory-frontend/src/views/Project/Detail/components/GeographyTree.vue` — 树形组件参考实现（交互模式、样式风格、编辑逻辑、空状态处理）
- `ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue` — 世界观设置页面，势力 textarea 替换位置（第 329-341 行），AI 生成后刷新逻辑

### 前端 API 参考
- `ai-factory-frontend/src/api/continentRegion.ts` — API 客户端模式（接口定义 + CRUD 函数）
- `ai-factory-frontend/src/api/powerSystem.ts` — 力量体系 API（获取力量体系列表，用于名称解析）

### 后端 API（已实现）
- `ai-factory-backend/src/main/java/com/aifactory/controller/FactionController.java` — 势力 REST API（tree/list/save/update/delete + 关联管理端点）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java` — 势力实体字段（id, parentId, deep, sortOrder, name, type, corePowerSystem, description, children）

### 需求定义
- `.planning/REQUIREMENTS.md` §v1 需求 > 前端 — UI-01 到 UI-06 的完整定义

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **GeographyTree.vue**: 树形组件交互模式完全可复用（展开/折叠、内联编辑、添加/删除节点、loading/空状态）。但使用手动 4 层嵌套，FactionTree 改用递归组件
- **continentRegion.ts**: API 客户端模式（接口定义 + getTree/add/update/delete 函数），faction.ts 可直接复用此模式
- **WorldSetting.vue**: 已有 GeographyTree 集成模式（ref + refresh 调用 + disabled prop），FactionTree 集成方式完全一致

### Established Patterns
- **API 客户端模式**: TypeScript 接口 + request 工具函数，路径 `/api/novel/${projectId}/faction/*`
- **树形组件 Props**: projectId (string) + disabled (boolean) + refresh() 暴露方法
- **编辑模式**: editingNode ref 跟踪当前编辑节点，editingName/editingDesc ref 跟踪编辑值
- **Toast 反馈**: success/error 从 `@/utils/toast` 导入
- **Lucide 图标**: 从 `lucide-vue-next` 导入，项目已广泛使用
- **暗色模式**: 所有组件支持 dark mode（bg-gray-800, dark:text-gray-300 等）

### Integration Points
- WorldSetting.vue 第 329-341 行：替换势力 textarea 为 FactionTree 组件
- WorldSetting.vue handleGenerate() 中需添加 factionTreeRef.value?.refresh() 调用
- faction.ts 需调用 FactionController 的已有 API（tree/save/update/delete）
- 力量体系名称解析需获取力量体系列表（powerSystem API 已有）

</code_context>

<specifics>
## Specific Ideas

- 递归组件比 GeographyTree 的手动嵌套更干净，后续可考虑重构 GeographyTree
- 类型颜色映射：正派=green, 反派=red, 中立=gray，与 Tailwind 颜色类一致
- 力量体系名称需从 ID 解析，可能需要获取力量体系列表建立 ID→名称映射
- FactionTree 放在 GeographyTree 下方（与现有布局一致），占整行

</specifics>

<deferred>
## Deferred Ideas

- 势力关系编辑界面 — Phase 5（UI-04: 势力-势力关系管理）
- 势力-人物关联界面 — Phase 5（UI-05）
- 势力-地区关联界面 — Phase 5（UI-06）
- 后续重构 GeographyTree.vue 为递归组件 — 可选优化，非必要

</deferred>

---

*Phase: 04-前端树组件*
*Context gathered: 2026-04-02*
