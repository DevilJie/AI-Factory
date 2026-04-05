# Phase 9: 前端独立生成按钮 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 09-前端独立生成按钮
**Areas discussed:** 按钮位置与样式, Loading 状态与交互, 任务恢复与竞态, API client 组织

---

## 按钮位置

| Option | Description | Selected |
|--------|-------------|----------|
| 添加按钮旁边 | 各模块 header 右侧，与蓝色「添加」按钮并排 | ✓ |
| 替换添加按钮 | 合并为下拉按钮，AI 生成 + 手动添加 | |
| 卡片内部浮动 | 放在模块卡片内部右上角或底栏 | |

**User's choice:** 添加按钮旁边
**Notes:** 三个模块（GeographyTree、PowerSystemSection、FactionTree）各自 header 区域都新增

## 按钮样式

| Option | Description | Selected |
|--------|-------------|----------|
| 绿色渐变 + Sparkles 图标 | 复用页面顶部 AI 按钮的渐变样式 | ✓ |
| 紫色渐变 + Sparkles 图标 | 用不同色系区分 AI 与添加操作 | |

**User's choice:** 绿色渐变 + Sparkles 图标
**Notes:** 与顶部 AI 生成按钮保持视觉一致

## Loading 状态

| Option | Description | Selected |
|--------|-------------|----------|
| 按钮 spinner + 区域 disabled | 按钮显示 spinner + 文字「生成中...」，树/列表不可操作 | ✓ |
| 仅按钮 spinner | 仅按钮 disabled，区域内数据仍可操作 | |

**User's choice:** 按钮 spinner + 区域 disabled
**Notes:** 通过 disabled prop 传递给各子组件

## 互斥策略

| Option | Description | Selected |
|--------|-------------|----------|
| 互斥，同时只能生成一个 | 其他模块 AI 生成按钮 disabled | ✓ |
| 允许多个并行生成 | 各模块独立，但阵营依赖校验可能失败 | |

**User's choice:** 互斥，同时只能生成一个

## 任务恢复

| Option | Description | Selected |
|--------|-------------|----------|
| Per-module localStorage key | 按模块存不同 key，刷新后仅恢复对应模块 | ✓ |
| 共用现有 worldview key | 复用现有 key，只存一个 taskId | |

**User's choice:** Per-module localStorage key

## API client 位置

| Option | Description | Selected |
|--------|-------------|----------|
| worldview.ts 加 3 个方法 | generateGeographyAsync / generatePowerSystemAsync / generateFactionAsync | ✓ |
| 各模块 API 文件各加一个 | 分散到 continentRegion.ts / powerSystem.ts / faction.ts | |

**User's choice:** worldview.ts 加 3 个方法

## 现有按钮处理

| Option | Description | Selected |
|--------|-------------|----------|
| 保留，文案不变 | 顶部「AI生成」按钮仍调用整体生成 | ✓ |
| 改名为「AI生成全部」 | 更明确区分整体 vs 独立生成 | |

**User's choice:** 保留，文案不变

## Claude's Discretion

- 轮询逻辑是否提取为 composable（当前 inline 在 WorldSetting.vue 中）
- 各组件 disabled 状态传递方式（prop vs emit event）
- 错误提示具体措辞

## Deferred Ideas

None
