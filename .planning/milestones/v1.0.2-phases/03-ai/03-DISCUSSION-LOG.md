# Phase 3: AI 集成与提示词迁移 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 03-ai
**Areas discussed:** 势力 XML 格式, 名称匹配与错误处理, 提示词迁移方案, 提示词模板设计

---

## 势力 XML 格式

| Option | Description | Selected |
|--------|-------------|----------|
| <f> 容器 + <faction> 嵌套 | 外层 <f> 是容器，内层每个势力用 <faction> 标签，子势力用嵌套 <faction> 表达层级 | ✓ |
| <factions> 容器 + <f> 嵌套 | 外层 <factions> 是容器，每个势力用 <f> 标签 | |
| 全部 <f> 嵌套 | 最简洁但容易混淆容器和项目 | |

**User's choice:** <f> 容器 + <faction> 嵌套
**Notes:** 参照桌面 demo.txt 中的地理区域格式，XML 节点不使用属性，用子元素 <n> 和 <d> 表达

### 引用方式

| Option | Description | Selected |
|--------|-------------|----------|
| 名称引用 + 逗号分隔 | 地区用 <regions>东域,北域</regions>，力量体系用 <power>仙道</power> | ✓ |
| 名称引用 + 子元素列表 | 每个地区一个 <region> 子元素 | |
| 其他格式 | 自定义 | |

**User's choice:** 名称引用 + 逗号分隔

---

## 名称匹配与错误处理

### 名称匹配策略

| Option | Description | Selected |
|--------|-------------|----------|
| 三级容错 + 跳过 | 精确匹配 → 去后缀匹配 → contains 匹配，未匹配则跳过并警告 | ✓ |
| 两级容错 + 跳过 | 精确匹配 + 去后缀匹配，更简单减少误匹配 | |
| 仅精确匹配 + 跳过 | 最严格，只做精确匹配 | |

**User's choice:** 三级容错 + 跳过

### 解析失败处理

| Option | Description | Selected |
|--------|-------------|----------|
| 跳过失败 + 警告 | 解析失败的势力跳过，成功的正常入库，记录警告日志 | ✓ |
| 回滚势力部分 | 解析失败则整个势力部分回滚 | |
| 全部回滚 | 任何解析失败则整个世界观生成失败 | |

**User's choice:** 跳过失败 + 警告

---

## 提示词迁移方案

### 迁移策略

| Option | Description | Selected |
|--------|-------------|----------|
| 一次性全部迁移 | 所有 6 个调用点一起改，减少中间状态 | ✓ |
| 分批迁移 | 按 2-3 个一批逐步迁移 | |

**User's choice:** 一次性全部迁移

### 调用方式

| Option | Description | Selected |
|--------|-------------|----------|
| 直接调 fillForces() | 注入 FactionService，调用 fillForces(worldview) | ✓ |
| 先检查再调用 | 先检查世界观是否有势力数据 | |

**User's choice:** 直接调 fillForces()

---

## 提示词模板设计

| Option | Description | Selected |
|--------|-------------|----------|
| 严格规则描述 | 详细描述每个字段的规则，包括类型枚举、层级限制、引用约束 | ✓ |
| 格式说明 + 示例 | 简洁格式和示例，不过多约束 | |
| 仅格式 + 示例 | 最简洁，依赖 AI 理解能力 | |

**User's choice:** 严格规则描述

---

## Claude's Discretion

- DOM 解析具体实现细节（使用 DocumentBuilder 还是其他方式）
- 日志记录级别和格式
- 提示词模板中的具体措辞
- fillForces() 调用后 null 检查的处理方式

## Deferred Ideas

None — discussion stayed within phase scope
