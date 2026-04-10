# Phase 15: Data Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-10
**Phase:** 15-data-foundation
**Areas discussed:** 分卷字段设计, 距离校验策略, 旧字段清理范围

---

## 分卷字段设计

| Option | Description | Selected |
|--------|-------------|----------|
| 互补存储（推荐） | 分卷号+章节号同时保留，四个字段组合定位 | |
| 分卷号替代章节号 | 只保留 volume 维度，废弃 chapter | |
| 存储全局章节序号 | 不用分卷号，用跨卷递增的全局序号 | ✓ |

**User's choice:** 分卷号+章节号组合存储，但章节编号改为全局序号（跨卷递增）。plantedVolume+plantedChapter 定位埋设，plannedCallbackVolume+plannedCallbackChapter 定位回收。

**约束规则确认：**
1. 埋设字段可为 null（无伏笔）
2. 埋设有值时回收字段不可为 null
3. plannedCallbackChapter ≤ 该卷规划章节数
4. 现有测试数据可清空，不需要迁移

**Notes:** 用户明确每个分卷章节序号从1开始改为全局递增。距离计算简化为 abs(callback - planted) >= N。

---

## 距离校验策略

| Option | Description | Selected |
|--------|-------------|----------|
| 全局章节距离（推荐） | abs(callbackChapter - plantedChapter) >= N | ✓ |
| 卷内章节距离 | 只统计同一卷内章节差，跨卷不做校验 | |

**最小间隔：** 默认 3 章

**暗线豁免：**

| Option | Description | Selected |
|--------|-------------|----------|
| 按 layoutType 豁免（推荐） | dark（暗线）伏笔跳过距离校验 | ✓ |
| 新增 importance 字段 | major/minor，major 跳过校验 | |

**Notes:** 用户提到"贯穿全本的伏笔"——核心悬念可能跨几十甚至上百章。暗线伏笔豁免校验是合理方案。

---

## 旧字段清理范围

| Option | Description | Selected |
|--------|-------------|----------|
| 彻底删除（推荐） | DB列DROP + Java实体/DTO + 前端类型/引用全部删除 | ✓ |
| 渐进弃用 | 保留DB列，实体标记 @TableField(exist=false) | |

**删除时机：**

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 15 一并删除（推荐） | 新增分卷字段+删除旧字段+距离校验一步到位 | ✓ |
| 延后到前端完成时 | 等Phase 18前端完成后再删 | |

**Notes:** DATA-02 已在 STATE.md 中决定彻底删除，本次确认删除时机也在 Phase 15。涉及文件：NovelChapterPlan.java, ChapterPlanDto.java, ChapterPlanUpdateRequest.java, project.ts, chapter.ts, ChapterPlanDrawer.vue, VolumeTree.vue, CreationCenter.vue。

---

## Claude's Discretion

- DB 列数据类型和约束定义
- 索引策略
- 距离校验错误信息文案
- 章节全局序号生成/维护方式

## Deferred Ideas

None — discussion stayed within phase scope
