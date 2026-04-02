# Phase 5: 关联管理界面 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 05-关联管理界面
**Areas discussed:** UI 入口位置, 势力关系交互, 人物关联交互, 地区关联交互

---

## UI 入口位置

| Option | Description | Selected |
|--------|-------------|----------|
| FactionTree 内集成 | 每个势力节点加关联管理按钮，点击弹出抽屉 | ✓ |
| WorldSetting 内独立区域 | 新增独立区域，三个分区 | |
| 侧边详情面板 | 全屏/大抽屉，包含树+关联 | |

**User's choice:** FactionTree 内集成

| Option | Description | Selected |
|--------|-------------|----------|
| 侧边抽屉 Drawer | 侧边弹出，不离开树视图 | ✓ |
| 模态对话框 Dialog | 中央弹出模态框 | |

**User's choice:** 侧边抽屉 Drawer

| Option | Description | Selected |
|--------|-------------|----------|
| Tab 分区 | 抽屉内 Tab 切换关系/人物/地区 | ✓ |
| 垂直滚动分区 | 三区依次排列滚动 | |

**User's choice:** Tab 分区

---

## 势力关系交互

| Option | Description | Selected |
|--------|-------------|----------|
| 选择器 + 列表 | 下拉选目标势力 + 关系类型 + 描述，列表展示 | ✓ |
| 关系图可视化 | 节点连线可视化 | |

**User's choice:** 选择器 + 列表

| Option | Description | Selected |
|--------|-------------|----------|
| 单向关系 | A→B 和 B→A 独立设置 | |
| 双向关系 | A→B 自动创建 B→A | ✓ |

**User's choice:** 双向关系

| Option | Description | Selected |
|--------|-------------|----------|
| 标准表单模式 | 固定表单区域 + 列表 | ✓ |
| 内联展开表单 | 点击添加后在列表上方展开 | |

**User's choice:** 标准表单模式

| Option | Description | Selected |
|--------|-------------|----------|
| 所有势力可选 | 排除自身，其余都可选 | ✓ |
| 仅同级势力 | 限制为同级或兄弟势力 | |

**User's choice:** 所有势力可选

---

## 人物关联交互

| Option | Description | Selected |
|--------|-------------|----------|
| 下拉选择 | 简单下拉列表 | |
| 搜索下拉 | 带关键词过滤的下拉 | ✓ |

**User's choice:** 搜索下拉

| Option | Description | Selected |
|--------|-------------|----------|
| 自由文本输入 | 任意职位名 | |
| 预设 + 自定义 | 预设选项 + 自定义输入 | ✓ |

**User's choice:** 预设 + 自定义

| Option | Description | Selected |
|--------|-------------|----------|
| 一个人物一个势力 | 关联新势力自动解除旧关联 | |
| 允许多个势力 | 同一人物可在不同势力有不同职位 | ✓ |

**User's choice:** 允许一个人物多个势力

---

## 地区关联交互

| Option | Description | Selected |
|--------|-------------|----------|
| 树形选择器 | 缩进层级展示，勾选关联 | ✓ |
| 扁平下拉 | 列表显示，名称含层级 | |

**User's choice:** 树形选择器

| Option | Description | Selected |
|--------|-------------|----------|
| 单选添加 | 一次选一个地区 | |
| 多选批量添加 | 一次勾选多个地区 | ✓ |

**User's choice:** 多选批量添加

---

## Claude's Discretion

- Drawer 宽度、动画、样式细节
- 关系类型颜色/图标映射
- 搜索下拉防抖时间
- 树形选择器展开/折叠行为
- 列表排序方式

## Deferred Ideas

None — discussion stayed within phase scope
