# Phase 13: 章节生成约束注入 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-09
**Phase:** 13-章节生成约束注入
**Areas discussed:** 约束语言强度, 规划角色注入格式, 新角色自动创建流程, 兼容与回退策略

---

## 约束语言强度

| Option | Description | Selected |
|--------|-------------|----------|
| 分段约束 | 开头约束 + 结尾提醒，双重提醒 | ✓ |
| XML 标签约束 | 用 <MUST_FOLLOW> 包裹规划角色数据 | |
| 超严格措辞 | 强烈否定语言约束 | |

**User's choice:** 分段约束
**Notes:** 仅约束角色出场，不约束情节走向和戏份内容。仅提示词级别约束，无后端检测/重试。偏差在 Phase 14 前端对比视图中体现。

### 约束范围

| Option | Description | Selected |
|--------|-------------|----------|
| 仅约束角色 | 只约束谁登场，不约束情节走向 | ✓ |
| 角色 + 戏份 | 约束出场 + 戏份描述 | |
| 角色 + 戏份 + 比例 | 约束出场 + 戏份 + 重要程度比例 | |

### 违约束处理

| Option | Description | Selected |
|--------|-------------|----------|
| 仅提示词约束 | 偏差在 Phase 14 前端对比中体现 | ✓ |
| 提示词 + 后端检测重试 | 检测到规划外角色自动重试 | |

---

## 规划角色注入格式

| Option | Description | Selected |
|--------|-------------|----------|
| 纯文本列表 | 每行一个角色简要信息，简洁易懂 | ✓ |
| XML 结构 | 结构化但增加模板长度 | |
| 混合（文字 + XML） | 文字介绍 + 结构化数据 | |

### 替换策略

| Option | Description | Selected |
|--------|-------------|----------|
| 完全替换 | AI 只看到规划角色 | ✓ |
| 主 + 参考池 | 规划角色为主，全量列表保留为参考 | |

### 注入位置

| Option | Description | Selected |
|--------|-------------|----------|
| 复用 characterInfo | 复用现有占位符，代码改动最小 | ✓ |
| 新增占位符 | 新增模板变量，需新增解析逻辑 | |

---

## 新角色自动创建流程

**User's clarification:** 角色提取相关逻辑不需要处理，只要在生成章节时把规划登场的角色信息加入提示词即可。规划角色只有主要角色（主角、配角、反派），提示词中应说明 NPC 性质角色可以适当自行生成。

**Result:** CG-03 (新角色自动创建) 递延至后续 phase。

---

## 兼容与回退策略

| Option | Description | Selected |
|--------|-------------|----------|
| 二分支干净回退 | 有规划角色走约束注入，无规划角色走原有逻辑 | ✓ |
| 混合模式 | 规划角色 + 全量参考 | |

### NPC 允许提示

| Option | Description | Selected |
|--------|-------------|----------|
| 一句式提示 | "跑龙套、路人甲等 NPC 性质角色可根据情节需要自行安排" | ✓ |
| 一段式说明 | 更详细的 NPC 角色自由度说明 | |

---

## Claude's Discretion

- 约束语言具体中文措辞
- 规划角色文本列表格式细节
- 判断"有规划角色"的代码条件
- 是否需要创建新的提示词模板版本

## Deferred Ideas

- CG-03 新角色自动创建 — 用户明确表示本 phase 仅处理提示词注入
