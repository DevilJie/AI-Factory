# Phase 5: 关联管理界面 - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

用户可手动管理势力间关系（盟友/敌对/中立）、势力-人物关联（含职位）、势力-地区关联，完成势力数据的完整管理闭环。此阶段交付三种关联的前端管理界面，后端 API 已在 Phase 2 就绪。不包含 AI 自动关联和关系图谱可视化。

</domain>

<decisions>
## Implementation Decisions

### UI 入口与布局
- **D-01:** 关联管理入口集成在 FactionTree 内 — 每个势力节点加关联管理按钮（如 link/chain 图标），点击后打开侧边 Drawer 抽屉
- **D-02:** 抽屉内用 Tab 分区显示关系/人物/地区三种关联，每个 Tab 独立管理
- **D-03:** 抽屉打开时加载当前势力的所有关联数据，操作完成后自动刷新

### 势力关系交互
- **D-04:** 选择器 + 列表模式 — 下拉选择目标势力 + 关系类型选择 + 描述文本框，已有关系显示为可删除列表
- **D-05:** 双向关系 — 添加 A→B 关系时自动创建 B→A 同类型关系，删除时也双向删除。后端 API 调用两次或在一次请求中处理双向
- **D-06:** 标准表单模式 — 关系添加区域固定显示在列表上方，不是内联展开
- **D-07:** 目标势力选择范围：所有势力可选（排除自身），包含不同顶级势力下的子势力

### 人物关联交互
- **D-08:** 搜索下拉选择人物 — 带关键词过滤的下拉组件，输入时实时过滤人物名
- **D-09:** 职位（role）用预设选项 + 自定义输入 — 预设：掌门/副掌门/长老/弟子/护法/执事，同时允许自由输入其他职位名
- **D-10:** 允许一个人物关联多个势力 — 数据库不约束唯一性，同一人物可在不同势力担任不同职位

### 地区关联交互
- **D-11:** 树形选择器 — 下拉展示地区树（缩进显示层级），支持勾选地区节点
- **D-12:** 多选批量添加 — 一次可勾选多个地区，提交后批量创建关联

### Claude's Discretion
- Drawer/抽屉的具体宽度、动画细节
- 关系类型的颜色/图标映射（盟友=绿色？敌对=红色？中立=灰色？）
- 搜索下拉的防抖时间、空状态文案
- 树形选择器的展开/折叠行为
- 关联列表的排序方式（按添加时间？按名称？）
- 错误状态和空状态的处理

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 后端 API（已实现）
- `ai-factory-backend/src/main/java/com/aifactory/controller/FactionController.java` — 势力 REST API，包含关联管理端点（/{factionId}/relations、/{factionId}/characters、/{factionId}/regions）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRelation.java` — 势力关系实体（factionId, targetFactionId, relationType, description）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionCharacter.java` — 势力-人物关联实体（factionId, characterId, role）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionRegion.java` — 势力-地区关联实体（factionId, regionId）

### 前端参考组件
- `ai-factory-frontend/src/views/Project/Detail/components/FactionTree.vue` — 现有势力树组件，需在此组件中集成关联管理入口
- `ai-factory-frontend/src/api/faction.ts` — 现有 API 客户端，需补充关联 API 函数

### 参考数据 API
- `ai-factory-frontend/src/api/character.ts` — 人物 API 客户端（获取项目下人物列表供搜索选择）
- `ai-factory-frontend/src/api/continentRegion.ts` — 地区 API 客户端（获取地区树供树形选择器）

### 需求定义
- `.planning/REQUIREMENTS.md` §v1 需求 > 前端 — UI-04、UI-05、UI-06

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **FactionTree.vue**: 现有递归树组件，每个节点有操作按钮区域，可直接添加关联管理按钮
- **faction.ts**: API 客户端模式已建立，需补充 getRelations/addRelation/deleteRelation、getCharacters/addCharacter/deleteCharacter、getRegions/addRegion/deleteRegion 等函数
- **character.ts**: 人物列表 API 已有，可直接用于搜索下拉的数据源
- **continentRegion.ts**: 地区树 API 已有（getContinentRegionTree），可直接用于树形选择器数据源

### Established Patterns
- **Drawer/抽屉**: 项目使用自定义 Drawer 或 Ant Design 风格组件，侧边弹出
- **Toast 反馈**: success/error 从 `@/utils/toast` 导入
- **Lucide 图标**: 从 `lucide-vue-next` 导入
- **暗色模式**: 所有组件支持 dark mode
- **API 响应处理**: request 工具 + try-catch + toast 错误提示

### Integration Points
- FactionTree.vue 节点操作按钮区域：新增关联管理按钮
- faction.ts：新增 9 个关联 API 函数（3 种关联 × CRUD）
- 后端 FactionController 关联端点已就绪，前端直接对接

</code_context>

<specifics>
## Specific Ideas

- 双向关系在 UI 上只显示当前势力视角的关系列表（即 A→B 显示 B 和关系类型），不重复显示 B→A
- 搜索人物下拉需要防抖处理，避免频繁请求
- 职位预设选项可根据题材扩展（仙侠/武侠/都市等题材有不同职位体系）
- 树形地区选择器可参考 GeographyTree 的展开/折叠样式保持一致

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-关联管理界面*
*Context gathered: 2026-04-02*
