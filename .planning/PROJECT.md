# 势力阵营结构化重构 + 世界观生成任务拆分

## What This Is

AI Factory 世界观模块已完成势力阵营结构化重构和世界观生成任务拆分。包含 4 张势力关联表、14 个 REST 端点、3 个独立 AI 生成策略（地理环境/力量体系/阵营势力各配独立提示词模板）、WorldviewTaskStrategy 9 步编排器（从 ~920 行精简至 ~250 行）、前端递归树组件和关联管理 Drawer，以及 3 个独立 AI 生成按钮（互斥执行 + localStorage 恢复）。

## Core Value

世界观的模块化独立生成让用户能按需单独重新生成地理环境、力量体系或阵营势力，结构化数据让 AI 生成章节时准确引用势力关系，也方便用户查看、编辑、管理。

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
- ✓ GeographyTaskStrategy + PowerSystemTaskStrategy 独立生成策略 — v1.0.3 Phase 07
- ✓ FactionTaskStrategy 依赖上下文注入策略 — v1.0.3 Phase 07
- ✓ 3 个独立生成 REST 端点 + 依赖校验 — v1.0.3 Phase 07
- ✓ WorldviewXmlParser 共享 DOM 解析工具类 — v1.0.3 Phase 08
- ✓ GeographyTaskStrategy + FactionTaskStrategy 委托 WorldviewXmlParser — v1.0.3 Phase 08
- ✓ WorldviewTaskStrategy 9 步编排器（~250 LOC，从 ~920 行精简） — v1.0.3 Phase 08
- ✓ 前端独立生成按钮（地理环境、力量体系、阵营势力各一个） — v1.0.3 Phase 09
- ✓ 章节规划 XML 解析增强（DOM 解析 + <ch> 角色标签提取 + NameMatchUtil 匹配） — v1.0.5 Phase 12
- ✓ 章节规划模板增加角色规划输出（<ch>/<cn>/<cd>/<ci> XML 标签指令 + characterInfo 注入） — v1.0.5 Phase 12
- ✓ 章节生成提示词注入规划角色信息（hasPlannedCharacters + buildPlannedCharacterInfoText + fallback） — v1.0.5 Phase 13
- ✓ 章节实际登场角色后端端点 + 前端 API（ChapterCharacterVO + getChapterCharacters） — v1.0.5 Phase 14
- ✓ 前端角色对比视图 + 角色详情链接（ChapterPlanDrawer 对比区 + CharacterDrawer） — v1.0.5 Phase 14

### Active

(No active requirements — all shipped in v1.0.5)

## Current Milestone: v1.0.5 章节角色规划体系

**Goal:** 在章节规划阶段预置登场角色和戏份安排，生成章节时严格按规划执行，形成"规划角色 → 按规划生成 → 提取验证"的闭环

**Target features:**
- 章节规划模板升级（角色规划输出）
- novel_chapter_plan 表扩展（planned_characters + character_arcs）
- 章节规划 XML 解析增强
- 章节生成提示词注入规划角色
- 前端章节规划展示角色安排

### Out of Scope

- 时间线（timeline）结构化 — 本次只做势力，时间线保持文本
- 世界规则（rules）结构化 — 本次只做势力，规则保持文本
- 势力-人物 AI 自动关联 — 世界观生成时人物可能未创建
- 势力地图可视化 — 前端成本高，简单关联即可
- 独立生成的撤销/回滚 — 复杂度高，用户可手动删除或重新生成
- 批量独立生成调度 — 当前场景按需生成即可
- 生成历史版本管理 — 超出本次范围

## Context

Shipped v1.0.2 + v1.0.3 with ~12,300 LOC added across Java, TypeScript, Vue, SQL.
Tech stack: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS.
Timeline: 8 days (2026-03-28 → 2026-04-05), 9 phases, 18 plans.
All 53 requirements verified complete (37 v1.0.2 + 16 v1.0.3).
Post-milestone quick fixes: cascade faction re-generation (geography/power system → faction), indeterminate region tree state, ConfirmDialog, XML parser adaptation.

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
| Geography template 使用 sub-tag 格式 | 匹配 parseSingleRegion 主格式，避免 name 属性格式 | ✓ Good — 解析一致性 |
| Faction template 用 XML wrapper 注入上下文 | <existing_geography>/<existing_power_systems> 区分输入与输出 | ✓ Good — 消除歧义 |
| Controller 注入依赖上下文到 task config | Strategy 无需 DB 查询，保持无状态 | ✓ Good — 可测试性 |
| 阵营势力同步校验依赖 | 缺依赖立即报错，避免异步失败 | ✓ Good — 用户体验 |
| WorldviewXmlParser 作为 @Component | 名称匹配需要 DB 访问（continentRegionService/powerSystemService） | ✓ Good — 依赖注入 |
| parseFactionXml 返回 ParsedFactions record | 解析与持久化分离，调用方控制 saveTree | ✓ Good — 灵活性 |
| Step stub 委托模式 | createStepStub(String) 让编排器调用 Strategy 的 executeStep | ✓ Good — 代码复用 |
| Single generatingModule union-type ref | 3 个布尔 → 1 个联合类型，互斥更清晰 | ✓ Good — 状态管理 |
| localStorage 10 分钟过期恢复 | 匹配轮询超时 (60×3s=180s+buffer)，页面刷新后继续 | ✓ Good — 用户体验 |
| DOM 解析代码先复制后提取 | Phase 7 先保证功能正确，Phase 8 统一重构 | ✓ Good — 渐进重构 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-10 after Phase 14 (前端展示-闭环验证) completed — milestone v1.0.5 finished*
