# Phase 14: 前端展示 + 闭环验证 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-09
**Phase:** 14-前端展示-闭环验证
**Areas discussed:** 对比验证视图, 规划展示优化, 触发时机与交互流程, 后端 API 设计

---

## 对比验证视图 (FE-02)

| Option | Description | Selected |
|--------|-------------|----------|
| 左右分栏并排对比 | 左栏规划角色，右栏实际角色，同名对齐，偏差高亮 | ✓ |
| 单列状态列表 | 单列表，图标/颜色标记状态，更紧凑 | |
| Drawer 内嵌对比摘要 | 角色规划 tab 上方增加对比摘要区域 | |

**User's choice:** 左右分栏并排对比
**Notes:** 直观清晰，空间占用大但在 640px Drawer 内可接受

### 偏差标记方式

| Option | Description | Selected |
|--------|-------------|----------|
| 颜色+图标双重标记 | 红色✗未出场、黄色⚠计划外、绿色✓一致 | ✓ |
| 仅颜色区分 | 更简洁但不明显 | |
| 分组显示 | 偏差角色单独分组 | |

**User's choice:** 颜色+图标双重标记

---

## 规划展示优化 (FE-01)

| Option | Description | Selected |
|--------|-------------|----------|
| 轻量优化 | 角色名旁增加链接按钮跳转 CharacterDrawer | ✓ |
| 重度重构 | 卡片布局、头像、聚合信息 | |
| 不做优化 | 现有实现已够用 | |

**User's choice:** 轻量优化
**Notes:** ChapterPlanDrawer 角色规划 tab 已有基础可编辑列表，不需要重构

---

## 对比触发时机与交互流程

| Option | Description | Selected |
|--------|-------------|----------|
| Drawer 角色规划 tab 内嵌 | 在角色规划 tab 上方自动显示对比结果，可折叠 | ✓ |
| 独立弹窗自动弹出 | 章节生成后自动弹出独立对比 Modal | |
| 章节列表入口 | 章节列表中每个已生成章节显示对比图标 | |

**User's choice:** Drawer 角色规划 tab 内嵌
**Notes:** 对比区域在规划列表上方，默认展开，折叠/展开切换。章节未生成时不显示对比区域。

---

## 后端 API 设计

| Option | Description | Selected |
|--------|-------------|----------|
| 新增 chapter→characters 端点 | GET /chapters/{id}/characters 返回实际角色 | ✓ |
| 新增对比聚合端点 | 一次返回规划+实际+摘要 | |
| 前端对比，后端仅提供数据 | 分别调用现有 API | |

**User's choice:** 新增 chapter→characters 端点
**Notes:** 复用已有 NovelCharacterChapterMapper.getCharactersByChapterId()，Controller 新增端点。前端做对比匹配。

### 对比匹配逻辑

| Option | Description | Selected |
|--------|-------------|----------|
| ID 优先 + 名称 fallback | characterId 精确匹配，无 ID 时名称匹配 | ✓ |
| 纯名称匹配 | 简单但可能重名 | |
| 复用三级名称匹配 | NameMatchUtil 精确→去后缀→包含 | |

**User's choice:** ID 优先 + 名称 fallback
**Notes:** 前端实现对比逻辑，不需要后端聚合端点。不用 NameMatchUtil 的复杂匹配。

---

## Claude's Discretion

- 对比区域具体 Tailwind 样式和颜色值
- 折叠/展开过渡动画
- 左右分栏在 640px Drawer 下的响应式处理
- 角色详情链接的具体实现
- 对比摘要文字措辞
- 新端点 VO/DTO 命名

## Deferred Ideas

None — discussion stayed within phase scope
