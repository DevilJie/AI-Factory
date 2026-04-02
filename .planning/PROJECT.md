# 势力阵营结构化重构

## What This Is

将 AI Factory 世界观模块中的势力阵营（forces）从纯文本字段重构为结构化树形数据表，参照已完成的力量体系和地理环境重构模式。新增势力表、势力-地区关联表、势力-人物关联表、势力-势力关系表，配套前端树形编辑组件和 AI 生成逻辑重构。

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

### Active

- [ ] WorldviewTaskStrategy 重构：AI 生成后 DOM 解析势力 XML 并结构化入库
- [ ] AI 提示词模板更新：势力部分从纯文本改为结构化 XML 格式输出
- [ ] 提示词中通过名称引用力量体系和地理区域，后端解析时按名称回查 ID
- ✓ type 和 core_power_system 仅顶级势力设置，下级势力继承顶级势力值 — Validated in Phase 02: 后端服务与 API
- ✓ 后端 FactionService CRUD（参照 ContinentRegionService 模式） — Validated in Phase 02: 后端服务与 API
- ✓ fillForces() 方法供 PromptBuilder 使用 — Validated in Phase 02: 后端服务与 API
- ✓ 前端 FactionTree.vue 组件（参照 GeographyTree.vue），支持树形查看、编辑、新增、删除 — Validated in Phase 04: 前端树组件
- ✓ 前端势力关系管理界面 — Validated in Phase 05: 关联管理界面
- ✓ 前端势力-人物手动关联界面 — Validated in Phase 05: 关联管理界面
- ✓ 前端势力-地区关联界面 — Validated in Phase 05: 关联管理界面

- ✓ 新增势力表（novel_faction），树形结构支持多层级嵌套 — Validated in Phase 01: 数据基础
- ✓ 新增势力-地区关联表（novel_faction_region） — Validated in Phase 01: 数据基础
- ✓ 新增势力-人物关联表（novel_faction_character） — Validated in Phase 01: 数据基础
- ✓ 新增势力-势力关系表（novel_faction_relation） — Validated in Phase 01: 数据基础
- ✓ novel_worldview.forces 字段改为 transient — Validated in Phase 01: 数据基础
- ✓ SQL 迁移脚本（建表） — Validated in Phase 01: 数据基础

### Out of Scope

- 时间线（timeline）结构化 — 本次只做势力，时间线保持文本
- 世界规则（rules）结构化 — 本次只做势力，规则保持文本
- 势力-人物 AI 自动关联 — 仅手动操作
- 势力地图可视化 — 简单关联即可，不做地图展示

## Context

- 这是继力量体系重构、地理环境重构之后的第三轮世界观结构化改造
- 已有的重构模式（树形表 + transient 字段 + DOM 解析 + 前端树组件）已验证可行，直接复用
- AI 生成时先存地理和力量体系，后存势力，确保按名称匹配 ID 时目标数据已存在
- 势力-人物关联在 AI 生成时不处理，因为世界观生成阶段人物可能还未创建
- 现有代码位置参考：
  - 势力核心生成逻辑：`WorldviewTaskStrategy#savePowerSystems` 后面新增
  - 地理重构参考：`ContinentRegionService` / `ContinentRegionServiceImpl` / `GeographyTree.vue`
  - 力量体系参考：`PowerSystemService` / `PowerSystemSection.vue`
  - 提示词模板：`ai_prompt_template_version` 表 id=3

## Constraints

- **Tech Stack**: Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Vite + Tailwind CSS — 沿用现有
- **Database**: MySQL 8.0+，使用树形表模式（parent_id + deep）
- **AI Integration**: LangChain4j + DOM XML 解析（Jackson XML 无法处理嵌套同名标签）
- **Frontend**: 参照 GeographyTree.vue 组件模式，保持 UI 一致性

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 树形表模式复用地理重构方案 | 同类数据（多层级嵌套），已验证可行 | — Pending |
| 势力-人物仅手动关联 | 世界观生成时人物可能未创建，AI 无法准确关联 | — Pending |
| AI 输出名称而非 ID，后端按名称回查 | AI 不知道数据库 ID，名称是最可靠的自然键 | — Pending |
| type 和 core_power_system 仅顶级设置 | 下级势力继承上级，避免数据冗余和不一致 | — Pending |

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
*Last updated: 2026-04-02 after Phase 05 completion*
