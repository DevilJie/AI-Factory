# Phase 12: AI 规划输出 + XML 解析 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-08
**Phase:** 12-AI规划输出-XML解析
**Areas discussed:** XML 标签与解析方式, 提示词模板角色指令

---

## XML 标签与解析方式

| Option | Description | Selected |
|--------|-------------|----------|
| 切 DOM 解析 | 参照 WorldviewXmlParser 模式，用 DOM 解析整个章节规划 XML | ✓ |
| Jackson + 字符串混合 | Jackson 解析主字段，角色信息用分隔符字符串 | |
| Jackson 尝试嵌套列表 | 在 ChapterPlanItem 中加 List 字段 + @JacksonXmlElementWrapper | |

**User's choice:** 切 DOM 解析
**Notes:** 项目约束已明确 Jackson XML 无法处理嵌套同名标签。DOM 解析与 WorldviewXmlParser 模式一致，已验证可靠。

---

## 角色注入方式

| Option | Description | Selected |
|--------|-------------|----------|
| 全量角色注入 | 注入项目所有角色的简要信息（名称+类型+势力），让 AI 自行判断每个章节谁登场 | ✓ |
| 按卷筛选注入 | 只注入本卷关联角色，减少 token 消耗 | |
| 不注入，AI 自行决定 | 不注入角色列表，仅要求 AI 根据情节规划自行确定登场角色 | |

**User's choice:** 全量角色注入
**Notes:** 全量角色信息让 AI 能准确引用已有角色名，避免名称偏差。

---

## 提示词指令详细度

| Option | Description | Selected |
|--------|-------------|----------|
| 简要指令 + 格式约束 | 增加"每个章节必须输出登场角色列表"指令 + XML 标签格式说明 + 角色名必须与角色列表一致 | ✓ (adapted) |
| 详细规划指导 | 详细说明角色规划原则：每个角色应有明确戏份、重要程度分级、角色搭配考虑等 | |
| 最小指令 | 仅说明 XML 标签格式，不加额外规划指令 | |

**User's choice:** 简要指令 + 格式约束（带补充）
**Notes:** 用户补充"要考虑新登场的角色可能在角色列表并没有数据"，需在指令中明确允许新角色。

---

## 新角色处理

| Option | Description | Selected |
|--------|-------------|----------|
| 允许新角色 | 提示词明确说"可以规划角色列表以外的角色"，characterId 为 null | ✓ |
| 仅限已有角色 | 严格限制只能用角色列表中的角色 | |

**User's choice:** 允许新角色
**Notes:** 与 Phase 11 决策一致（characterId=null 时角色名保留在 characterName）。

---

## Claude's Discretion

- DOM 解析的具体实现位置
- 角色列表注入的格式化细节
- ChapterPlanXmlDto 是否保留
- 角色规划的 roleType 字段处理方式
