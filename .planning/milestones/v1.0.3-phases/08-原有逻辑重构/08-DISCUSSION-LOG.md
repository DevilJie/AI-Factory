# Phase 8: 原有逻辑重构 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 08-原有逻辑重构
**Areas discussed:** 组合调用方式, 步骤结构设计, 结果汇总方式, 重复代码清理

---

## 组合调用方式

| Option | Description | Selected |
|--------|-------------|----------|
| 注入 Bean 直接调用 | 注入 GeographyTaskStrategy/PowerSystemTaskStrategy/FactionTaskStrategy 为 Spring Bean，直接调用其内部方法 | ✓ |
| 提取公共工具类 | 将三个 Strategy 的公共逻辑提取为 WorldviewModuleService 工具类，改动范围更大 | |
| 通过 TaskService 创建子任务 | 调用 TaskService.createTask + AsyncTaskExecutor 执行，产生额外 task 记录 | |

**User's choice:** 注入 Bean 直接调用
**Notes:** 用户选择推荐方案 — 最简单的组合方式，复用现有 Strategy 实例，不创建额外 task 记录。需要为各 Strategy 添加 public 的完整流程方法（executeFullFlow）

---

## 步骤结构设计

| Option | Description | Selected |
|--------|-------------|----------|
| 5 步骤（模块合并） | check → generate_modules（内部调用 3 个策略）→ generate_core → save_core → finalize | |
| 9 步骤（每个模块独立） | check → clean/geography → clean/power_system → clean/faction → generate_core → save_core | ✓ |
| Claude 决定 | 由 Claude 决定最佳粒度 | |

**User's choice:** 9 步骤（每个模块独立）
**Notes:** 用户选择细粒度步骤 — 前端可看到每个模块的生成进度。9 步骤：check_existing → clean_geography → generate_geography → clean_power_system → generate_power_system → clean_faction → generate_faction → generate_core → save_core

---

## 结果汇总方式

| Option | Description | Selected |
|--------|-------------|----------|
| 从 DB 回查拼装 | 各模块入库后从 DB 回查全部数据，组装完整 NovelWorldview 对象 | ✓ |
| 拼接 AI 原始响应 | 拼接各策略的 AI 原始响应，不查 DB | |
| 只需 DB 写入正确 | 依赖前端 GET /worldview 获取数据，不需要特殊汇总 | |

**User's choice:** 从 DB 回查拼装
**Notes:** 用户选择数据准确性最高的方案 — 从 DB 回查确保前端收到与数据库一致的数据

---

## 重复代码清理

| Option | Description | Selected |
|--------|-------------|----------|
| 提取共享工具类 | 创建 WorldviewXmlParser 工具类，提取 DOM 解析和三级名称匹配，4 个 Strategy 共用 | ✓ |
| 仅重构调用关系 | 只重构 WorldviewTaskStrategy 调用关系，不清理重复代码 | |
| Claude 决定 | 由 Claude 决定是否值得在本 Phase 一起做 | |

**User's choice:** 提取共享工具类
**Notes:** Phase 07 已计划 "Phase 8 will consolidate"。提取地理环境 DOM 解析、势力 DOM 解析、三级名称匹配（地区+力量体系）、中文映射方法为共享工具类

---

## Claude's Discretion

- WorldviewXmlParser 的具体方法签名和返回类型设计
- 各 Strategy 的 executeFullFlow 方法是否返回具体结果对象或仅返回成功/失败
- 错误处理细节（某模块生成失败时的回滚策略）

## Deferred Ideas

None — discussion stayed within phase scope
