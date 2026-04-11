# Phase 18: Frontend Chapter Foreshadowing - Context

**Gathered:** 2026-04-11
**Status:** Ready for planning

<domain>
## Phase Boundary

在 ChapterPlanDrawer 中添加伏笔管理区（第5个 tab），展示本章节需埋设和需回收的伏笔卡片，用户可直接编辑伏笔内容、添加新伏笔、删除已有伏笔。替代原有 textarea。

不包含：项目级伏笔总览页面（Phase 19）、AI 生成伏笔约束（Phase 17）。

</domain>

<decisions>
## Implementation Decisions

### UI 布局
- **D-01:** 新增第5个 Tab「伏笔管理」，使用独立组件 ForeshadowingTab.vue，与角色规划 tab 平级。参考 CharacterDrawer 的子组件拆分模式。
- **D-02:** 伏笔区内部使用卡片列表布局，每张卡片展示一个伏笔（标题、类型、状态、描述摘要）。
- **D-03:** 分为两个可折叠区域：「待埋设」和「待回收」，各自展示对应伏笔卡片。与 Phase 17 后端逻辑对应（pending→in_progress / in_progress→completed）。

### 交互设计
- **D-04:** 添加/编辑伏笔使用弹窗表单（Modal），而非行内编辑。表单包含完整字段。
- **D-05:** 编辑伏笔时可修改字段：标题、描述、类型（character/item/event/secret）、布局线（bright1-3/dark）、优先级、计划回收分卷和回收章节（plannedCallbackVolume + plannedCallbackChapter）。埋设章节不可修改。
- **D-06:** 删除伏笔需确认对话框后执行，防止误删。
- **D-07:** 添加新伏笔时预填本章节编号作为埋设章节，减少手动输入。

### 数据展示范围
- **D-08:** 展示两类伏笔数据：本章节相关伏笔（plantedChapter 或 plannedCallbackChapter 匹配当前章节）+ 当前分卷活跃伏笔（pending/in_progress）。
- **D-09:** 分区显示：本章节伏笔放在「待埋设」「待回收」主分区，分卷其他活跃伏笔放在可折叠的「分卷伏笔参考」区域。主次分明。
- **D-10:** 分卷其他活跃伏笔也可编辑（弹窗表单），方便用户在章节规划时顺便调整。

### 视觉区分设计
- **D-11:** 颜色编码区分布局线类型：卡片左侧色条 + 类型标签。颜色映射：bright1=蓝、bright2=绿、bright3=黄、dark=紫/红。与"明线暗线"概念对应。
- **D-12:** 状态用徽章样式：pending=灰色"待埋设"、in_progress=蓝色"进行中"、completed=绿色"已完成"。

### Claude's Discretion
- 卡片具体间距和圆角大小
- 弹窗表单字段排列顺序
- 空状态文案和图标
- 加载骨架屏设计
- 颜色具体色值（Tailwind 色板选择）
- 卡片描述摘要截断长度

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 前端核心组件
- `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue` — 章节规划抽屉，需添加第5个 tab
- `ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue` — 包含 ChapterPlanDrawer 的父组件

### 后端伏笔 API
- `ai-factory-backend/src/main/java/com/aifactory/controller/ForeshadowingController.java` — 完整 CRUD API（list/create/update/delete/complete/stats）
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` — 伏笔服务，含按章节筛选
- `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java` — 伏笔实体（含所有字段定义）
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java` — 伏笔 DTO
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java` — 查询 DTO（含 plantedChapter/plannedCallbackChapter 筛选）

### 参考组件模式
- CharacterDrawer 子组件模式 — tab 拆分为独立组件的先例

### 前端需新建
- `ai-factory-frontend/src/api/foreshadowing.ts` — 伏笔 API 客户端（需新建）
- `ai-factory-frontend/src/types/foreshadowing.ts` — 伏笔 TypeScript 类型（需新建）

### Phase 上下文
- `.planning/phases/16-ai-chapter-planning/16-CONTEXT.md` — Phase 16 决策（伏笔 XML 标签格式）
- `.planning/phases/15-data-foundation/15-CONTEXT.md` — Phase 15 决策（伏笔表结构）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChapterPlanDrawer.vue` 的 tab 切换机制 — 直接添加第5个 tab 即可
- `CharacterDrawer` 子组件模式 — ForeshadowingTab.vue 拆分参考
- 后端 `ForeshadowingController` 完整 API — 前端无需后端改动
- `ForeshadowingQueryDto` 已支持 plantedChapter/plannedCallbackChapter 筛选 — 精确查询本章节伏笔

### Established Patterns
- **Tab 子组件拆分**: CharacterDrawer 将复杂 tab 拆为独立组件（CharacterPowerSystemTab 等），伏笔 tab 应同样拆为 ForeshadowingTab.vue
- **Modal 表单模式**: 项目中已有 Modal 表单模式用于编辑操作
- **API 客户端**: `src/api/` 目录下 barrel export 模式
- **TypeScript 类型**: `src/types/` 目录下接口定义 + barrel export

### Integration Points
- `ChapterPlanDrawer.vue` — 添加第5个 tab 和 ForeshadowingTab 子组件
- `src/api/` — 新建 foreshadowing.ts API 客户端
- `src/types/` — 新建 foreshadowing.ts 类型定义
- 后端 `ForeshadowingController` — 已有完整 CRUD，前端直接对接

</code_context>

<specifics>
## Specific Ideas

- 伏笔管理区用两分区布局：「待埋设」和「待回收」，与 Phase 17 后端状态流转逻辑对应
- 添加新伏笔预填当前章节编号（plantedChapter），减少用户手动输入
- 颜色编码映射布局线：明线用偏暖色（蓝/绿/黄），暗线用偏冷色（紫/红），视觉直观
- 分卷参考伏笔可折叠，默认收起，避免喧宾夺主

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 18-frontend-chapter-foreshadowing*
*Context gathered: 2026-04-11*
