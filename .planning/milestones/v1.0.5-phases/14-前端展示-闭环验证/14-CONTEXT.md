# Phase 14: 前端展示 + 闭环验证 - Context

**Gathered:** 2026-04-09
**Status:** Ready for planning

<domain>
## Phase Boundary

用户能在前端直观看到章节规划角色安排，并验证生成结果是否遵循了规划。具体范围：
1. 前端对比视图 — 规划角色 vs 实际登场角色的左右分栏对比，偏差角色用颜色+图标双重标记
2. 前端规划展示轻量优化 — 角色名旁增加查看详情链接（characterId 存在时）
3. 后端新增 chapter→characters REST 端点 — 为对比视图提供实际登场角色数据
4. 对比逻辑 — ID 优先匹配 + characterName fallback，前端实现对比匹配

不包含：角色自动创建（CG-03 递延）、角色关系图谱、角色出场统计。

</domain>

<decisions>
## Implementation Decisions

### 对比验证视图 (FE-02)
- **D-01:** 左右分栏并排对比 — 左栏规划角色列表，右栏实际登场角色列表。同名角色对齐，偏差角色高亮
- **D-02:** 颜色+图标双重标记 — 规划了但未出场用红色✗，实际出场但未规划用黄色⚠，一致用绿色✓
- **D-03:** 对比结果内嵌在 ChapterPlanDrawer 角色规划 tab 内 — 位于规划角色列表上方，折叠/展开可切换，默认展开
- **D-04:** 章节未生成时仅显示规划角色列表（当前行为不变），不显示对比区域
- **D-05:** 章节已生成且有实际角色数据时，对比区域自动出现并展示摘要（如"3/4 出场"）

### 规划展示优化 (FE-01)
- **D-06:** 轻量优化 — 在角色名旁显示链接按钮，点击跳转打开 CharacterDrawer 查看角色详情。仅 characterId 存在时显示链接
- **D-07:** 不做重度重构（卡片化、头像、聚合信息等），保持现有可编辑列表形式

### 对比触发时机与交互
- **D-08:** 用户打开 ChapterPlanDrawer 切换到角色规划 tab 时自动显示对比（如果章节已生成）。无需手动触发
- **D-09:** 对比区域位于规划角色列表上方，包含摘要行（出场率）和左右分栏详细对比
- **D-10:** 对比区域可折叠/展开，不遮挡规划角色编辑功能

### 后端 API 设计
- **D-11:** 新增 REST 端点 GET /api/novel/{projectId}/chapters/{chapterId}/characters — 返回该章节的所有实际登场角色列表
- **D-12:** 复用现有 NovelCharacterChapterMapper.getCharactersByChapterId()，Controller 层新增端点暴露
- **D-13:** 返回数据包含 characterId、characterName（需 JOIN novel_character 表）、roleType、importanceLevel 等对比所需字段
- **D-14:** 前端分别调用 chapter plan API（获取规划角色）和新增 API（获取实际角色），在前端做对比匹配逻辑

### 对比匹配逻辑
- **D-15:** ID 优先 + 名称 fallback — 规划角色有 characterId 时用 ID 精确匹配，没有时用 characterName 字符串匹配
- **D-16:** 前端实现对比逻辑（不需要后端聚合端点）。匹配结果分三组：一致（✓）、规划未出场（✗红色）、计划外出场（⚠黄色）
- **D-17:** 不复用 NameMatchUtil 的三级匹配（前端场景不需要去后缀、包含等容错），用简单的 ID/名称匹配即可

### Claude's Discretion
- 对比区域的具体 Tailwind 样式和颜色值
- 折叠/展开交互的过渡动画
- 左右分栏在 Drawer 窄屏下的响应式处理（640px 宽度）
- 角色详情链接的具体实现方式（按钮图标、点击行为）
- 对比摘要的文字措辞
- 新端点的 VO/DTO 命名

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 前端核心组件（需修改）
- `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue` — 章节规划 Drawer，角色规划 tab 需增加对比视图
- `ai-factory-frontend/src/stores/editor.ts` — Editor store，管理当前章节和章节规划状态
- `ai-factory-frontend/src/api/chapter.ts` — 章节 API client，可能需要新增获取章节关联角色的调用

### 前端类型定义
- `ai-factory-frontend/src/types/project.ts` — ChapterPlan 类型（plannedCharacters 字段已存在）
- `ai-factory-frontend/src/api/character.ts` — ChapterAssociation 类型（实际登场角色数据结构参考）

### 前端模式参考
- `ai-factory-frontend/src/views/Project/Detail/components/CharacterDrawer.vue` — 角色 Drawer 组件（FE-01 轻量优化中跳转目标）
- `ai-factory-frontend/src/views/Project/Detail/components/FactionDrawer.vue` — Drawer Tab 模式参考

### 后端需修改
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelCharacterChapterMapper.java` — 已有 getCharactersByChapterId 方法
- `ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterChapterService.java` — 已有 getCharactersByChapterId 服务方法
- `ai-factory-backend/src/main/java/com/aifactory/controller/NovelCharacterController.java` — 参考现有角色相关端点模式，新增 chapter→characters 端点

### 数据模型
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelCharacterChapter.java` — 角色-章节关联实体（实际登场角色数据来源）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` — 章节规划实体（plannedCharacters 字段，规划角色数据来源）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChapterPlanDrawer.vue` — 角色规划 tab 已有完整实现：可编辑角色列表、JSON 解析、添加/删除角色、保存到后端。对比视图在此基础上扩展
- `NovelCharacterChapterMapper.getCharactersByChapterId()` — 后端已有按章节 ID 查询角色列表的 SQL，只需 Controller 层暴露
- `editor.ts` store — 已管理 currentChapterPlan（含 plannedCharacters）和 currentChapter（含 chapterId），可驱动对比数据加载
- `CharacterDrawer.vue` — 已有完整角色详情展示，FE-01 轻量优化只需触发其打开

### Established Patterns
- **Drawer 模式**: Teleport to body + 右侧滑入 + Tab 导航（所有 Drawer 一致）
- **角色类型 badge**: protagonist/supporting/antagonist/npc 已有中文标签和颜色映射
- **JSON 字段**: plannedCharacters 和 characterArcs 都是 String 类型 JSON，前端 JSON.parse 处理
- **API 调用**: `request.get<T>(url)` 模式，Axios 封装

### Integration Points
- `ChapterPlanDrawer.vue` 角色规划 tab — 主要修改点，增加对比区域
- `NovelCharacterController` — 新增 REST 端点
- `NovelCharacterChapterService.getCharactersByChapterId()` — 服务层已存在，直接暴露
- `editor.ts` store — 需要加载章节的实际角色数据（新增 action 或在 Drawer 内直接调用 API）
- `CharacterDrawer.vue` — FE-01 跳转目标

</code_context>

<specifics>
## Specific Ideas

- 对比摘要参考："3/4 规划角色已出场 | 1 位计划外登场"
- 左右分栏在 640px Drawer 中可能需要上下排列作为移动端 fallback
- 角色详情链接使用 Lucide 的 ExternalLink 图标，小号显示在角色名旁边
- 对比区域使用蓝色信息背景色区分于下方白色可编辑列表

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 14-前端展示-闭环验证*
*Context gathered: 2026-04-09*
