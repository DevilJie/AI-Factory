# Phase 6: 独立提示词模板 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 06-独立提示词模板
**Areas discussed:** 上下文注入格式, 独立模板XML输出格式, 精简后世界观模板范围, 模板变量设计

---

## 上下文注入格式

| Option | Description | Selected |
|--------|-------------|----------|
| 结构化XML嵌入 | 把已入库的地理环境和力量体系数据序列化为结构化XML，直接嵌入提示词 | ✓ |
| 摘要文本描述 | 生成简洁的文本描述，更省token但可能丢失细节 | |
| 引用占位符+后端拼接 | 提示词中用占位符，实际数据由后端拼接到模板变量中 | |

**User's choice:** 结构化XML嵌入
**Notes:** AI看到的是和自己输出时相同的格式，名称交叉引用最准确

---

## 独立模板XML输出格式

| Option | Description | Selected |
|--------|-------------|----------|
| 复用现有标签 | 地理环境只输出`<g>`片段，力量体系只输出`<p>`片段，阵营只输出`<f>`片段 | ✓ |
| 自定义格式 | 每个独立模板用自定义格式，需要新的解析逻辑 | |

**User's choice:** 复用现有标签
**Notes:** 用户说"所有的都可以复用，其实就是把原来耦合在世界观生成里面的全部拆出来了"

---

## 精简后世界观模板范围

| Option | Description | Selected |
|--------|-------------|----------|
| 保持XML格式 | 继续用`<w>`根标签包裹`<t>/<b>/<l>/<r>`，Phase 8组合时无缝拼接 | ✓ |
| 纯文本格式 | 改为纯文本或Markdown，更简单但Phase 8需要转换 | |

**User's choice:** 保持XML格式
**Notes:** 移除`<g>/<p>/<f>`三个模块后，仅保留`<t>`世界类型、`<b>`世界背景、`<l>`时间线、`<r>`世界规则

---

## 模板变量设计

| Option | Description | Selected |
|--------|-------------|----------|
| 复用+扩展 | 地理/力量/精简世界觌用同一套4变量，阵营模板多加geographyContext和powerSystemContext | ✓ |
| 每个模板独立设计 | 各模板独立设计变量，更精细但增加复杂度 | |

**User's choice:** 复用+扩展
**Notes:** 地理/力量/精简世界观模板复用 projectDescription/storyGenre/storyTone/tagsSection；阵营模板额外增加 geographyContext 和 powerSystemContext

---

## Claude's Discretion

- 各独立模板的具体提示词措辞和细节（从现有统一模板中提取并优化）
- 地理/力量体系独立模板中是否需要额外上下文
- 模板的 scenario 标签值命名

## Deferred Ideas

None — discussion stayed within phase scope
