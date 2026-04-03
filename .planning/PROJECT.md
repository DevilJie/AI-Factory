# 势力阵营结构化重构

## What This Is

AI Factory 世界观模块中的势力阵营已完成从纯文本到结构化树形数据的重构。新增 4 张数据库表（势力主表 + 3 张关联表），配套完整的后端 CRUD 服务、14 个 REST 端点、AI 提示词模板迁移（DOM XML 解析入库）、以及前端递归树组件和关联管理 Drawer。参照力量体系和地理环境的已验证模式完成。

## Core Value

势力的结构化数据能让 AI 生成章节时准确引用势力关系，也让用户方便地查看、编辑、管理势力体系。

## Requirements

### Validated

- ✓ 世界观基础信息（世界类型、背景、时间线、规则） — existing
- ✓ 力量体系结构化（novel_power_system / level / step 三表） — existing
- ✓ 地理环境结构化（novel_continent_region 树形表 + GeographyTree 组件） — existing
- ✓ 世界观 AI 生成流程（WorldviewTaskStrategy） — existing
- ✓ 人物管理（novel_character 实体及 CRUD） — existing
- ✓ JWT 认证、项目管理体系 — existing
- ✓ novel_faction 树形表（parent_id + deep + type + core_power_system） — v1.0.2 Phase 01
- ✓ 3 张关联表（faction_region, faction_character, faction_relation） — v1.0.2 Phase 01
- ✓ NovelWorldview.forces 标记 transient — v1.0.2 Phase 01
- ✓ FactionService 树形 CRUD + 级联删除 + fillForces() — v1.0.2 Phase 02
- ✓ FactionController 14 REST 端点 — v1.0.2 Phase 02
- ✓ AI 提示词模板更新（结构化 XML） — v1.0.2 Phase 03
- ✓ DOM 解析两遍插入 + 三级名称匹配 — v1.0.2 Phase 03
- ✓ 11 处 getForces() → fillForces() 迁移 — v1.0.2 Phase 03
- ✓ FactionTree.vue 递归树组件 — v1.0.2 Phase 04
- ✓ faction.ts API 客户端 — v1.0.2 Phase 04
- ✓ WorldSetting.vue 集成 — v1.0.2 Phase 04
- ✓ FactionDrawer + 关联管理 Tabs — v1.0.2 Phase 05
- ✓ 地理环境独立提示词模板 (llm_geography_create) — v1.0.3 Phase 06
- ✓ 力量体系独立提示词模板 (llm_power_system_create) — v1.0.3 Phase 06
- ✓ 阵营势力独立提示词模板 (llm_faction_create, 含 geographyContext/powerSystemContext) — v1.0.3 Phase 06
- ✓ 统一世界观模板精简（仅保留 t/b/l/r） — v1.0.3 Phase 06

### Active

- [ ] 地理环境独立生成 REST API（提示词模板已在 Phase 06 创建）
- [ ] 力量体系独立生成 REST API（提示词模板已在 Phase 06 创建）
- [ ] 阵营势力独立生成 REST API + 依赖校验（提示词模板已在 Phase 06 创建）
- [ ] 原有世界观生成逻辑重构（剔除三模块，改为组合调用）
- [ ] 前端独立生成按钮（地理环境、力量体系、阵营势力各一个）

### Out of Scope

- 时间线（timeline）结构化 — 本次只做势力，时间线保持文本
- 世界规则（rules）结构化 — 本次只做势力，规则保持文本
- 势力-人物 AI 自动关联 — 世界观生成时人物可能未创建
- 势力地图可视化 — 前端成本高，简单关联即可

## Context

Shipped v1.0.2 with ~8,900 LOC added across Java, TypeScript, Vue, SQL.
Tech stack: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS.
Timeline: 6 days (2026-03-28 → 2026-04-03), 5 phases, 12 plans.
All 37 v1 requirements verified complete.
Recursive tree CRUD at all depths works via child-instance-local state + refresh emit pattern.

## Constraints

- **Tech Stack**: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS — 沿用现有
- **Database**: MySQL 8.0+，使用树形表模式（parent_id + deep）
- **AI Integration**: LangChain4j + DOM XML 解析（Jackson XML 无法处理嵌套同名标签）
- **Frontend**: 参照 GeographyTree.vue 组件模式，保持 UI 一致性

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 树形表模式复用地理重构方案 | 同类数据（多层级嵌套），已验证可行 | ✓ Good — 零偏差复用 |
| 势力-人物仅手动关联 | 世界观生成时人物可能未创建，AI 无法准确关联 | ✓ Good — 简化 AI 流程 |
| AI 输出名称而非 ID，后端按名称回查 | AI 不知道数据库 ID，名称是最可靠的自然键 | ✓ Good — 三级匹配容错 |
| type/core_power_system 仅顶级设置 | 下级势力继承上级，避免数据冗余和不一致 | ✓ Good — inheritRootValues() |
| getChildNodes() 替代 getElementsByTagName | 避免 DOM 解析取到所有后代节点 | ✓ Good — 精确控制 |
| 两遍插入策略 | 先存势力获取 ID，再建关联 | ✓ Good — saveTree→buildNameToIdMap |
| 子实例本地 CRUD + refresh emit | 递归组件子节点独立管理状态 | ✓ Good — 解决所有深度 CRUD |
| Direct mapper injection for associations | 简单关联表无需 service 层 | ✓ Good — 减少样板代码 |

## Current Milestone: v1.0.3 世界观生成任务拆分

**Goal:** 将地理环境、力量体系、阵营势力的 AI 生成从单一提示词拆分为 3 个独立任务，各配独立提示词模板和 REST API，前端增加独立生成按钮。

**Target features:**
- 地理环境独立生成（提示词模板 + API）
- 力量体系独立生成（提示词模板 + API）
- 阵营势力独立生成（提示词模板 + API + 依赖校验）
- 原有世界观整体生成重构为组合调用
- 前端独立生成按钮

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-03 after Phase 06 completion*
