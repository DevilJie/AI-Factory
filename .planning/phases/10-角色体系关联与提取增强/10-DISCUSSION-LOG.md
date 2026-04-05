# Phase 10: 角色体系关联与提取增强 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-06
**Phase:** 10-角色体系关联与提取增强
**Areas discussed:** 修炼体系关联, 势力阵营提取, 角色类型识别修复, 前端展示与编辑, 提示词模板优化

---

## 修炼体系关联策略

| Option | Description | Selected |
|--------|-------------|----------|
| 复用名称回查模式 | systemName → power_system ID, currentLevel → level ID (精确/模糊匹配) | ✓ |
| 仅 systemName 回查 ID | 只匹配 power_system ID，level 留空让用户手动选择 | |
| 不自动匹配，全手动 | 提取后只保存文本，用户手动选择修炼体系和境界 | |

**User's choice:** 复用名称回查模式
**Notes:** 与 v1.0.2 势力名称匹配同一策略，已有成功经验

---

## 关联创建/更新时机

| Option | Description | Selected |
|--------|-------------|----------|
| 跟随提取 upsert | 每次提取时更新 current_realm_id/current_sub_realm_id | ✓ |
| 仅首次创建 | 第一次创建关联，后续不更新 | |
| 历史记录模式 | 每次提取创建新记录，保留境界变化历史 | |

**User's choice:** 跟随提取 upsert

---

## ID 匹配失败处理

| Option | Description | Selected |
|--------|-------------|----------|
| ID + null fallback | ID 字段为 null，文本信息保留在 character_chapter JSON | ✓ |
| ID + 文本冗余字段 | 关联表多加 current_level_text 字段 | |

**User's choice:** ID + null fallback

---

## 势力信息提取方式

| Option | Description | Selected |
|--------|-------------|----------|
| 新增 FC 标签 | <FC><N>势力名</N><R>职位</R></FC>，名称回查 faction ID | ✓ |
| 自由文本提示 | 提示词加一句注释，AI 自由输出 | |

**User's choice:** 新增 FC 标签

---

## 势力关联创建方式

| Option | Description | Selected |
|--------|-------------|----------|
| 提取时自动 upsert | 角色可属多个势力，每个势力一个关联 | ✓ |
| 确认后写入 | 提取后保存临时字段，用户确认后才写入 | |

**User's choice:** 提取时自动 upsert

---

## 角色类型识别修复

| Option | Description | Selected |
|--------|-------------|----------|
| 细化规则 + 已有分布 | 中文类型定义 + 注入已有角色分布 | ✓ |
| 仅细化规则 | 只加判断规则，不注入分布 | |

**User's choice:** 细化规则 + 已有分布

---

## 角色类型体系

| Option | Description | Selected |
|--------|-------------|----------|
| 4 类 + 中文定义 | protagonist/supporting/antagonist/npc，加中文说明 | ✓ |
| 5 类（新增 minor_supporting） | 拆分 supporting 范围 | |

**User's choice:** 4 类 + 中文定义

---

## 前端展示位置

| Option | Description | Selected |
|--------|-------------|----------|
| Drawer Tab 扩展 | 新增"修炼体系"和"所属势力"Tab | ✓ |
| 内嵌展示 | 角色详情页内嵌，不弹窗 | |
| 仅展示无编辑 | 只做展示，用户手动去其他页面编辑 | |

**User's choice:** Drawer Tab 扩展

---

## 角色列表关联信息

| Option | Description | Selected |
|--------|-------------|----------|
| 列表卡片增加关联信息 | 显示修炼境界 + 势力名称 | ✓ |
| 仅详情页 | 只修改详情页，列表不变 | |

**User's choice:** 列表卡片增加关联信息

---

## 数据加载方式

| Option | Description | Selected |
|--------|-------------|----------|
| 服务端聚合 | 角色 detail API JOIN 返回关联信息 | ✓ |
| 前端多次请求 | 前端分别调多个 API | |

**User's choice:** 服务端聚合

---

## 提示词模板优化

| Option | Description | Selected |
|--------|-------------|----------|
| 增量优化 | 新增 FC 标签说明 + 类型中文定义，保留现有结构 | ✓ |
| 全面重写 | 重新设计 XML 格式和引导规则 | |

**User's choice:** 增量优化

---

## Claude's Discretion

- 名称匹配容错规则
- 关联表索引策略
- Drawer Tab UI 组件选择
- 列表卡片排版细节
