# Phase 18: Frontend Chapter Foreshadowing - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-11
**Phase:** 18-frontend-chapter-foreshadowing
**Areas discussed:** UI Layout, Interaction Design, Data Scope, Visual Design

---

## UI Layout

### Tab Position

| Option | Description | Selected |
|--------|-------------|----------|
| 第5个 Tab | 新增「伏笔管理」tab，独立组件 ForeshadowingTab.vue | ✓ |
| 内嵌在情节规划 tab | 在「情节规划」tab 内添加伏笔区块 | |
| 拆分为两个 tab | 「伏笔埋设」和「伏笔回收」两个独立 tab | |

**User's choice:** 第5个 Tab（推荐）
**Notes:** 与角色规划 tab 平级，参考 CharacterDrawer 子组件拆分模式

### Card Layout

| Option | Description | Selected |
|--------|-------------|----------|
| 卡片列表 | 每个伏笔一张卡片，展示标题/类型/状态/描述摘要 | ✓ |
| 紧凑列表/表格 | 每行一条伏笔，表格样式 | |
| 分区布局（埋设/回收） | 上下分区，各自用卡片或列表 | |

**User's choice:** 卡片列表

### Grouping

| Option | Description | Selected |
|--------|-------------|----------|
| 扁平列表，无分组 | 所有伏笔混合展示，视觉标记区分 | |
| 两组分区 | 「待埋设」和「待回收」可折叠区域 | ✓ |

**User's choice:** 两组分区（推荐）

---

## Interaction Design

### Edit Mode

| Option | Description | Selected |
|--------|-------------|----------|
| 行内编辑 | 点击编辑图标，卡片变为可编辑状态 | |
| 弹窗表单 | 点击添加/编辑弹出 Modal 表单 | ✓ |
| 卡片展开编辑 | 点击展开卡片进入编辑模式 | |

**User's choice:** 弹窗表单（推荐）

### Editable Fields

| Option | Description | Selected |
|--------|-------------|----------|
| 仅内容字段 | 标题、描述、类型、布局线、优先级 | |
| 内容 + 回收章节 | 除内容外还可修改回收分卷/章节 | ✓ |
| 全部字段 | 包括埋设章节 | |

**User's choice:** 内容 + 回收章节

### Delete Flow

| Option | Description | Selected |
|--------|-------------|----------|
| 直接删除 | 无确认，点击即删 | |
| 确认后删除 | 弹出确认对话框 | ✓ |
| 软删除 | 标记为取消状态 | |

**User's choice:** 确认后删除（推荐）

### Add Flow

| Option | Description | Selected |
|--------|-------------|----------|
| 手动填写表单 | 用户填写所有字段 | |
| 预填埋设章节 | 预填本章节编号，用户填写其余字段 | ✓ |
| 快速添加 | 仅标题和描述，其余用默认值 | |

**User's choice:** 预填埋设章节（推荐）

---

## Data Scope

### Display Range

| Option | Description | Selected |
|--------|-------------|----------|
| 仅本章节相关 | plantedChapter 或 plannedCallbackChapter 匹配 | |
| 本章节 + 分卷活跃伏笔 | 本章节伏笔 + 当前分卷 pending/in_progress 伏笔 | ✓ |
| 当前分卷全部 | 包括已完成伏笔 | |

**User's choice:** 本章节 + 分卷活跃伏笔（推荐）

### Chapter vs Volume Display

| Option | Description | Selected |
|--------|-------------|----------|
| 分区显示（主次分明） | 本章节伏笔在主分区，分卷伏笔在可折叠参考区 | ✓ |
| 混合展示（标签区分） | 所有伏笔混合，用标签区分来源 | |

**User's choice:** 分区显示（主次分明）（推荐）

### Volume Foreshadowing Editable

| Option | Description | Selected |
|--------|-------------|----------|
| 只读展示 | 分卷其他伏笔仅查看 | |
| 可编辑 | 分卷其他伏笔也可点击编辑 | ✓ |

**User's choice:** 可编辑

---

## Visual Design

### Layout Line Colors

| Option | Description | Selected |
|--------|-------------|----------|
| 颜色编码 | 卡片左侧色条 + 类型标签，bright1=蓝/bright2=绿/bright3=黄/dark=紫红 | ✓ |
| 图标/文字标签 | 不依赖颜色，用图标区分 | |
| 双重标识（颜色+图标） | 颜色 + 图标双重 | |

**User's choice:** 颜色编码（推荐）

### Status Badges

| Option | Description | Selected |
|--------|-------------|----------|
| 状态徽章 | pending=灰、in_progress=蓝、completed=绿 | ✓ |
| 背景色区分 | 卡片背景深浅区分状态 | |
| 图标状态 | ○/◆/✓ 图标表示状态 | |

**User's choice:** 状态徽章（推荐）

---

## Claude's Discretion

- 卡片具体间距和圆角大小
- 弹窗表单字段排列顺序
- 空状态文案和图标
- 加载骨架屏设计
- 颜色具体色值（Tailwind 色板选择）
- 卡片描述摘要截断长度

## Deferred Ideas

None — discussion stayed within phase scope
