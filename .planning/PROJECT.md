# AI Factory — 小说 AI 辅助创作平台

## What This Is

AI Factory 世界观模块已完成势力阵营结构化重构、世界观生成任务拆分、角色体系关联增强、章节角色规划体系、以及伏笔管理体系。包含势力阵营 4 张关联表、3 个独立 AI 生成策略、章节角色规划闭环（规划 → 约束生成 → 对比验证）、伏笔管理闭环（伏笔表扩展 → AI 规划伏笔 → 约束注入生成 → 章节管理 + 项目总览）、NameMatchUtil 三级中文名称匹配、前端规划-实际角色对比视图、伏笔健康度评分。

## Core Value

世界观的模块化独立生成让用户能按需单独重新生成地理环境、力量体系或阵营势力，伏笔的结构化数据让 AI 生成章节时准确引用伏笔关系，也让用户方便地查看、编辑、管理伏笔体系。

## Requirements

### Validated

- ✓ 世界观基础信息（世界类型、背景、时间线、规则） — existing
- ✓ 力量体系结构化（novel_power_system / level / step 三表） — existing
- ✓ 地理环境结构化（novel_continent_region 树形表 + GeographyTree 组件） — existing
- ✓ 世界观 AI 生成流程（WorldviewTaskStrategy） — existing
- ✓ 人物管理（novel_character 实体及 CRUD） — existing
- ✓ JWT 认证、项目管理体系 — existing
- ✓ novel_faction 树形表（parent_id + deep + type + core_power_system） — v1.0.2
- ✓ 3 张关联表（faction_region, faction_character, faction_relation） — v1.0.2
- ✓ FactionService 树形 CRUD + 级联删除 + fillForces() — v1.0.2
- ✓ FactionController 14 REST 端点 — v1.0.2
- ✓ AI 提示词模板更新（结构化 XML） — v1.0.2
- ✓ DOM 解析两遍插入 + 三级名称匹配 — v1.0.2
- ✓ FactionTree.vue 递归树组件 — v1.0.2
- ✓ 3 个独立 AI 生成策略 + 独立按钮 — v1.0.3
- ✓ WorldviewXmlParser 共享 DOM 解析工具类 — v1.0.3
- ✓ 章节角色规划闭环（DOM 解析 + 约束注入 + 对比验证） — v1.0.5
- ✓ NameMatchUtil 三级中文名称匹配 — v1.0.5
- ✓ novel_foreshadowing 分卷字段 + 距离校验 — v1.0.6
- ✓ 旧伏笔字段彻底删除（DB+实体+DTO+前端） — v1.0.6
- ✓ 章节规划伏笔上下文注入 + <fs>/<fp> XML 解析持久化 — v1.0.6
- ✓ 章节生成伏笔约束注入 + 自动状态流转 — v1.0.6
- ✓ ChapterPlanDrawer 伏笔管理区（待埋设/待回收/分卷参考卡片 + CRUD） — v1.0.6
- ✓ 项目级伏笔总览页面（筛选 + CRUD + 健康度评分） — v1.0.6

### Active

(No active requirements — awaiting next milestone definition)

### Out of Scope

- 时间线（timeline）结构化 — 保持文本
- 世界规则（rules）结构化 — 保持文本
- 角色关系图谱可视化 — 前端成本高
- 角色出场统计仪表盘 — 需要聚合查询优化
- 章节生成后新角色自动创建 (CG-03) — deferred per D-10
- 伏笔自动状态同步（从 ChapterPlotMemory 到结构化表） — 手动管理更安全
- 分卷摘要中的伏笔统计 — 非核心
- sanitizeXml 统一重构 — 已有技术债务标记

## Context

Shipped v1.0.2–v1.0.6 across 19 phases, 38+ plans, 16 days (2026-03-28 → 2026-04-12).
~56,000 LOC total (40,500 Java + 15,500 TypeScript/Vue).
Tech stack: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS.
All milestones verified complete.
Technical debt: sanitizeXml ~80 lines duplicated in ChapterGenerationTaskStrategy.

## Constraints

- **Tech Stack**: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS — 沿用现有
- **Database**: MySQL 8.0+，使用树形表模式（parent_id + deep）
- **AI Integration**: LangChain4j + DOM XML 解析（Jackson XML 无法处理嵌套同名标签）
- **Frontend**: 参照 GeographyTree.vue / Characters.vue 组件模式，保持 UI 一致性

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 树形表模式复用地理重构方案 | 同类数据（多层级嵌套），已验证可行 | ✓ Good |
| AI 输出名称而非 ID，后端按名称回查 | AI 不知道数据库 ID，名称是最可靠的自然键 | ✓ Good |
| getChildNodes() 替代 getElementsByTagName | 避免 DOM 解析取到所有后代节点 | ✓ Good |
| parseChaptersXml 专有 sanitizeXml 副本 | ~80 行复制而非抽取共享工具 | ⚠ Revisit |
| plantedVolume immutable after creation | 伏笔埋设位置不可变，只有 plannedCallbackVolume 可修改 | ✓ Good |
| Dark-line foreshadowing exempt from distance validation | 暗线伏笔可能长距离埋设，不适用跨章节距离规则 | ✓ Good |
| D-01: Structured list format for foreshadowing context | 简洁格式（title, type, layout, status, locations）避免上下文溢出 | ✓ Good |
| D-07: Inject into both template and hardcoded prompt paths | 覆盖所有提示词路径，确保伏笔信息完整注入 | ✓ Good |
| D-04: auto status update (pending→in_progress→completed) | 生成后自动流转伏笔状态，无需手动 | ✓ Good |
| D-05: chapter re-generation does NOT roll back foreshadowing status | 避免重生成导致伏笔状态回退，保持向前推进 | ✓ Good |
| Volume reference section default collapsed | 减少视觉噪音，按需查看 | ✓ Good |
| Health score uses completion ratio | 简单有效的伏笔体系健康度衡量 | ✓ Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-12 after v1.0.6 milestone*
