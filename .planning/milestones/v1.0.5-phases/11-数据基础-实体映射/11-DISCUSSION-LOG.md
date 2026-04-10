# Phase 11: 数据基础 + 实体映射 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-07
**Phase:** 11-数据基础-实体映射
**Areas discussed:** planned_characters JSON 结构, ChapterPlanXmlDto 合并策略, 名称匹配工具设计, character_arcs 与 planned_characters 关系

---

## planned_characters JSON 结构

| Option | Description | Selected |
|--------|-------------|----------|
| 最小 schema | characterName + roleType + roleDescription + importance + characterId（5 字段） | ✓ |
| 扩展 schema | 最小 + appearsIn + emotionalState + motivation | |
| Claude 决定 | 根据下游需求自行选择 | |

**User's choice:** 最小 schema（推荐）
**Notes:** importance 字段使用 high/medium/low 三级格式，不用数字等级

---

## ChapterPlanXmlDto 合并策略

| Option | Description | Selected |
|--------|-------------|----------|
| 保留 dto 版，删 common.xml | dto 版有 Schema 注解且活跃使用，common.xml 版无引用 | ✓ |
| 保留 common.xml 版，删 dto | common.xml 有 volumeNumber 字段更完整 | |
| 双版本同步更新 | 同时更新两个版本 | |

**User's choice:** 保留 dto 版，删 common.xml（推荐）
**Notes:** common.xml 版本经 grep 确认无任何导入引用。volumeNumber 非必要（有 volumePlanId 外键）

---

## 名称匹配工具设计

| Option | Description | Selected |
|--------|-------------|----------|
| 提取通用 NameMatchUtil | 三级匹配参数化，可复用于角色/势力/地区 | ✓ |
| 角色匹配单独实现 | 在 ChapterCharacterExtractService 中独立实现 | |
| 两级匹配（精确→包含） | 不加后缀剥离，避免误匹配 | |

**User's choice:** 提取通用 NameMatchUtil（推荐）

**Follow-up — 重构范围：**

| Option | Description | Selected |
|--------|-------------|----------|
| 仅角色匹配 | Phase 11 只做角色匹配，不重构 WorldviewXmlParser | ✓ |
| 同时重构 WorldviewXmlParser | 让现有匹配也使用新工具类 | |

**Notes:** 角色名后缀列表（公子/小姐/大哥/师傅等）不同于势力名后缀（宗/派/门等），工具类需参数化后缀列表

---

## character_arcs 与 planned_characters 关系

| Option | Description | Selected |
|--------|-------------|----------|
| 仅映射字段，不填数据 | Phase 11 只加实体字段，character_arcs 数据留给后续 | ✓ |
| 合并到 planned_characters | 在 planned_characters 中嵌入弧光变化 | |
| 同时实现填充 | Phase 11 同时实现 AI 解析和填充 | |

**User's choice:** 仅映射字段，不填数据（推荐）
**Notes:** character_arcs 存储角色弧光变化（心态/状态转变），与 planned_characters（谁出场）是不同维度。DB 列已存在但未映射到实体。

---

## Claude's Discretion

- NameMatchUtil 的具体包位置
- 角色名后缀列表的完整枚举
- planned_characters JSON 序列化/反序列化细节
- ChapterPlanDto 中是否暴露 plannedCharacters 字段

## Deferred Ideas

None — discussion stayed within phase scope
