# Phase 2: 后端服务与 API - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 02-api
**Areas discussed:** Service拆分策略, fillForces() 输出格式, API 端点设计, 级联删除策略

---

## Service拆分策略

| Option | Description | Selected |
|--------|-------------|----------|
| 单类 FactionService | 与 ContinentRegionService 相同，单类管理势力+关联+文本构建 | ✓ |
| 拆分 Service + AssociationService | 更清晰但增加代码量和文件数 | |

**User's choice:** 单类 FactionService (ContinentRegionService 模式)
**Notes:** 用户确认复用已验证的单类模式，便于维护

 关联表拆分独立管理保持职责单一清晰

---

## fillForces() 输出格式
| Option | Description | Selected |
|--------|-------------|----------|
| 层级缩进格式 | 每级缩进显示势力名、类型、力量体系、地区引用，使用颜色标签区分类型 | ✓ |
| 单层平铺 | 所有势力在一层显示，简洁直接 | |
| 栌─ Markdown 列表 | 势力名称: 描述 | (适合 AI 解析) | |

**User's choice:** 层级缩进格式 (buildGeographyText 有层级输出模式参考)
**Notes:** 用户确认使用颜色标签和地区引用，保留层次信息便于 Phase 3 的 AI 解析

带完整上下文

---

## API 端点设计
| Option | Description | Selected |
|--------|-------------|----------|
| 合并到 FactionController | 势力 CRUD + 3 个关联管理端点 | RESTful 风格 | ✓ |
| 拆分为 4 个 Controller | 每个 CRUD 篮围独立 API | |
| 拆分为 2 个 Controller | 势力 + 关联 | |

**User's choice:** 合并到 FactionController
 共享 FactionService 实例)
**Notes:** 凋用 ContinentRegion 模式：一个 Controller 皴理树形 CRUD + 关联

 势力-地区、势力-人物、势力-势力关系的 CRUD 狇点直接嵌入主 Controller, 共享 FactionService

 保持职责单一清晰

 无需单独 Controller

---

## 磧 联删除策略
| Option | Description | Selected |
|--------|-------------|----------|
| 应用层递归 | 先递归查子势力 ID → 刌据关联 Mapper → 事务保护 | ✓ |
| 批量 SQL IN | 使用 SQL 批量删除，性能更好 | |

**User's choice:** 应用层递归
 先查子势力 → 刌表关联 → 事务保护)
**Notes:** 全部操作在一个 @Transactional 中完成, 确保数据一致性。删除父势力前弹出确认对话框（前端实现）。Phase 1 已决定不加外键约束，保持与 ContinentRegion 模式一致。

---

## Claude's Discretion

 No questions explicitly deferred to Claude — all areas had recommended options selected.

 Decisions listed in CONTEXT.md under Claude's Discretion are all follow ContinentRegion patterns.

---
