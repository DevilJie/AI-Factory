# 势力阵营结构化重构 + 世界观生成任务拆分

## What This Is

AI Factory 世界观模块已完成势力阵营结构化重构、世界观生成任务拆分、角色体系关联增强、以及章节角色规划体系。包含势力阵营 4 张关联表、3 个独立 AI 生成策略、章节角色规划闭环（规划 → 约束生成 → 对比验证）、NameMatchUtil 三级中文名称匹配、前端规划-实际角色对比视图。

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
- ✓ planned_characters JSON 字段映射 + characterArcs 已有字段映射 — v1.0.5 Phase 11
- ✓ NameMatchUtil 三级中文名称匹配（22 个测试用例） — v1.0.5 Phase 11
- ✓ DOM parseChaptersXml 替代 Jackson XML（<ch> 角色标签提取 + plannedCharacters JSON 持久化） — v1.0.5 Phase 12
- ✓ 章节规划模板升级（<ch>/<cn>/<cd>/<ci> XML 输出指令 + characterInfo 角色列表注入） — v1.0.5 Phase 12
- ✓ 章节生成约束注入（hasPlannedCharacters + 约束语言 + fallback 全量角色列表） — v1.0.5 Phase 13
- ✓ 章节实际登场角色后端端点 + 前端 API（ChapterCharacterVO） — v1.0.5 Phase 14
- ✓ novel_foreshadowing 分卷字段 + 距离校验 + 回收章节边界校验 — v1.0.6 Phase 15
- ✓ 移除 novel_chapter_plan foreshadowingSetup/foreshadowingPayoff 冗余字段 — v1.0.6 Phase 15

- ✓ 章节规划伏笔上下文注入（buildActiveForeshadowingContext + 双路径注入） — v1.0.6 Phase 16
- ✓ parseChaptersXml 伏笔标签解析（<fs>/<fp> 提取 + 伏笔持久化） — v1.0.6 Phase 16

- ✓ 章节创作注入伏笔约束（PromptTemplateBuilder + batchUpdateStatusForChapter 双路径） — v1.0.6 Phase 17

### Active

- [x] 章节创作注入伏笔约束（本章节埋设 + 回收） — v1.0.6 Phase 17
- [ ] ChapterPlanDrawer 伏笔管理区（查看/编辑/添加/删除）
- [ ] 侧边栏伏笔管理菜单（项目级伏笔总览）

## Current Milestone: v1.0.6 伏笔管理

**Goal:** 激活 novel_foreshadowing 表为章节规划和生成的核心驱动，让 LLM 自动规划伏笔埋设/回收，用户可跨卷管理伏笔。

**Target features:**
- novel_foreshadowing 增加分卷编号字段，支持跨卷伏笔
- 移除 novel_chapter_plan 冗余伏笔字段，统一使用伏笔表
- 章节规划生成时 LLM 输出伏笔规划（埋设 + 回收）
- 章节创作时注入伏笔约束（本章节需埋设 + 需回收的伏笔）
- ChapterPlanDrawer 增加伏笔管理区（查看/编辑/添加/删除）
- 侧边栏新增「伏笔管理」菜单（项目级伏笔总览）

### Out of Scope

- 时间线（timeline）结构化 — 保持文本
- 世界规则（rules）结构化 — 保持文本
- 角色关系图谱可视化 — 前端成本高
- 角色出场统计仪表盘 — 需要聚合查询优化
- 章节生成后新角色自动创建 (CG-03) — deferred per D-10

## Context

Shipped v1.0.2–v1.0.5 across 14 phases, 30+ plans, 14 days (2026-03-28 → 2026-04-10).
~54,500 LOC total (39,579 Java + 14,915 TypeScript/Vue).
Tech stack: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS.
All milestones verified complete.
Technical debt: sanitizeXml ~80 lines duplicated in ChapterGenerationTaskStrategy (unify with WorldviewXmlParser).
Post-v1.0.5 fixes: chapterPlanId matching fix, 章节序号显示, WorldviewXmlParser enhancement.

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
| plannedCharacters 用 String 类型 | 匹配现有 JSON 列模式（keyEvents, foreshadowingSetup） | ✓ Good — 一致性 |
| NameMatchUtil 通用化 NamedEntity 接口 | Phase 12 复用名称匹配逻辑 | ✓ Good — 复用 |
| parseChaptersXml 专有 sanitizeXml 副本 | ~80 行复制而非抽取共享工具（Phase 8 重构后再统一） | ⚠ Revisit — 未来统一 sanitizeXml |
| 模板版本 id=20 而非 id=19 | id=19 已被角色提取模板 v2 占用 | ✓ Good — 避免冲突 |
| CG-03 新角色自动创建 deferred | 简化闭环范围，避免生成时自动创建角色的不确定性 | ✓ Good — 范围控制 |
| ID-first + 精确名称回退匹配 | 规划 vs 实际角色对比优先用 ID，回退用名称 | ✓ Good — 准确匹配 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-11 after Phase 16 (AI Chapter Planning) complete*
