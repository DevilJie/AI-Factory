# Phase 4: 前端树组件 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 04-前端树组件
**Areas discussed:** 树组件渲染方式, 势力类型与力量体系标签, 编辑体验, WorldSetting.vue 集成

---

## 树组件渲染方式

| Option | Description | Selected |
|--------|-------------|----------|
| 手动嵌套（跟 GeographyTree 一样） | 硬编码 4 层嵌套模板，约 350 行，风格统一但代码重复 | |
| 递归组件 | Vue 3 递归组件，一个模板处理任意层级，约 100-150 行 | ✓ |
| 递归组件 + 后续重构 GeographyTree | 先重构 FactionTree，后续再重构 GeographyTree | |

**User's choice:** 递归组件
**Notes:** 势力层级可能比地理区域更深，递归组件更合适

---

## 势力类型标签

| Option | Description | Selected |
|--------|-------------|----------|
| 颜色 badge 在名称前 | 正派=绿, 反派=红, 中立=灰，显示在名称前 `[正派] 紫阳宗` | ✓ |
| 颜色 badge 在名称后 | 颜色同上，显示在名称后 `紫阳宗 [正派]` | |
| 图标区分 | 用不同图标区分，不用颜色 badge | |

**User's choice:** 颜色 badge 在名称前

---

## 力量体系标签

| Option | Description | Selected |
|--------|-------------|----------|
| 点号分隔在名称行内 | `[正派] 紫阳宗 · 仙道` | ✓ |
| 描述下方独立行 | 力量体系显示在描述下方单独一行 | |
| Tooltip 悬停显示 | 鼠标悬停时显示力量体系信息 | |

**User's choice:** 点号分隔在名称行内

---

## 编辑体验

| Option | Description | Selected |
|--------|-------------|----------|
| 内联编辑（跟 GeographyTree 一样） | 点击编辑后在节点位置展开所有字段表单 | |
| 侧面板编辑 | 弹出侧面/底部面板编辑 | |
| 混合方式 | 名称/描述内联编辑，类型/力量体系点击展开选择器 | ✓ |

**User's choice:** 混合方式
**Notes:** 用户提到势力关系编辑也需要考虑，但这属于 Phase 5 范围

---

## WorldSetting.vue 集成

### 替换策略

| Option | Description | Selected |
|--------|-------------|----------|
| 完全替换 | 删除 textarea，只显示 FactionTree | ✓ |
| 替换 + 保留只读文本 | FactionTree 为主，下方保留只读 textarea | |

### 自动刷新

| Option | Description | Selected |
|--------|-------------|----------|
| 自动刷新 | AI 生成后自动刷新 FactionTree | ✓ |
| 手动刷新 | 用户手动点击刷新 | |

---

## Claude's Discretion

- 递归组件具体实现细节
- 类型/力量体系选择器 UI 细节
- 空状态文案
- 力量体系名称获取方式

## Deferred Ideas

- 势力关系编辑界面 — Phase 5
- 重构 GeographyTree.vue 为递归组件 — 可选优化
