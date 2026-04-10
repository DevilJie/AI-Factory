# Phase 16: AI Chapter Planning - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-11
**Phase:** 16-ai-chapter-planning
**Areas discussed:** 伏笔上下文注入格式, LLM 输出 XML 标签设计, 解析与持久化策略, 两条规划路径统一

---

## 伏笔上下文注入格式

| Option | Description | Selected |
|--------|-------------|----------|
| 结构化列表 | 每个伏笔显示标题、类型、布局线、状态、埋设/回收位置 | ✓ |
| XML 格式 | 用 XML 包裹，与输出格式呼应 | |
| 简洁文本 | 仅标题+状态，减少 token 占用 | |

**User's choice:** 结构化列表
**Notes:** 完整上下文让 LLM 做更好的伏笔规划决策

**注入范围:**

| Option | Description | Selected |
|--------|-------------|----------|
| 仅当前分卷 | 只注入当前卷活跃伏笔，减少 token | ✓ |
| 全部活跃伏笔 | 跨卷伏笔也注入 | |
| Claude 决定 | 根据上下文窗口动态决定 | |

**User's choice:** 仅当前分卷

---

## LLM 输出 XML 标签设计

**子标签格式:**

| Option | Description | Selected |
|--------|-------------|----------|
| 子标签格式 | `<fs><ft>标题</ft><fy>类型</fy>...</fs>`，与现有 `<n>/<t>/<p>` 一致 | ✓ |
| 属性格式 | `<fs ft="标题" fy="item" ... />` | |
| Claude 决定 | 根据解析能力决定 | |

**User's choice:** 子标签格式

**数量:**

| Option | Description | Selected |
|--------|-------------|----------|
| 多个 | 每个 `<o>` 内零个或多个 `<fs>/<fp>` | ✓ |
| 各一个 | 每个 `<o>` 内最多一个 `<fs>` 和一个 `<fp>` | |

**User's choice:** 多个

---

## 解析与持久化策略

**持久化方式:**

| Option | Description | Selected |
|--------|-------------|----------|
| 只创建新记录 | LLM 输出直接创建，不做匹配 | ✓ |
| 匹配+创建/更新 | 用标题匹配已有伏笔 | |
| Claude 决定 | | |

**User's choice:** 只创建新记录

**重新规划时处理:**

| Option | Description | Selected |
|--------|-------------|----------|
| 删除+重建 | 先删该卷 pending 伏笔，再根据 LLM 输出重建 | ✓ |
| 仅追加 | 保留已有，仅追加新伏笔 | |
| Claude 决定 | | |

**User's choice:** 删除+重建

---

## 两条规划路径统一

| Option | Description | Selected |
|--------|-------------|----------|
| 两条都改 | generateChaptersInVolume + generateChaptersForVolume 都支持 | ✓ |
| 只改单卷路径 | 仅改 generateChaptersForVolume | |
| Claude 决定 | | |

**User's choice:** 两条都改

---

## Claude's Discretion

- 结构化列表的具体文本格式排版
- 注入伏笔上下文的模板变量名
- `<fs>/<fp>` regex 解析实现细节
- 批量创建伏笔的优化
- 暗线（dark）伏笔的特殊展示

## Deferred Ideas

None — discussion stayed within phase scope
