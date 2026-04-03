# Phase 1: 数据基础 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-01
**Phase:** 01-数据基础
**Areas discussed:** 势力表字段设计, 关联表设计细节, forces 列迁移策略

---

## 势力表字段设计

| Option | Description | Selected |
|--------|-------------|----------|
| type 枚举（正派/反派/中立） | 固定三种，前端颜色标签直接映射 | ✓ |
| type 自由文本 | 灵活但失去颜色标签一致性 | |

| Option | Description | Selected |
|--------|-------------|----------|
| core_power_system 不加外键 | 纯逻辑关联，删除力量体系时不因外键报错 | ✓ |
| core_power_system 加外键 | 数据库层保证引用完整性 | |

| Option | Description | Selected |
|--------|-------------|----------|
| description 长文本（TEXT） | 支持势力历史、理念等较长描述 | ✓ |
| description 短文本（VARCHAR(500)） | 控制长度 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 纯树形复用（parentId + deep） | 复用 ContinentRegion 模式，查询时内存构建树 | ✓ |
| 增加 path 字段 | 快速查询子孙节点，但增加维护复杂度 | |

**Notes:** 所有选择均与 ContinentRegion 现有模式保持一致。

---

## 关联表设计细节

| Option | Description | Selected |
|--------|-------------|----------|
| faction_relation 双向存储 | 存 A→B 和 B→A 两条，查询简单 | ✓ |
| faction_relation 单向存储 | 只存一条，查询需要 UNION | |

| Option | Description | Selected |
|--------|-------------|----------|
| faction_character role 自由文本 | 灵活适配不同题材职位体系 | ✓ |
| faction_character role 枚举 | 前端下拉选择，但题材差异大 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 关联表不加外键 | 保持与 ContinentRegion 一致 | ✓ |
| 关联表加外键 | 数据库层保证引用完整性 | |

| Option | Description | Selected |
|--------|-------------|----------|
| faction_region 不加 description | 只需 faction_id + region_id | ✓ |
| faction_region 加 description | 记录势力在该地区的具体情况 | |

**Notes:** 三张关联表全部不加外键，保持一致性。

---

## forces 列迁移策略

| Option | Description | Selected |
|--------|-------------|----------|
| 保留 forces 列 | 只建新表不删列，回退无风险 | ✓ |
| 立即删除 forces 列 | 彻底清理但无法回退 | |

**Notes:** forces 列保留直到所有消费方迁移完毕，后续单独脚本删除。

---

## Claude's Discretion

- SQL 迁移脚本的具体格式和注释风格
- 实体类中是否添加额外辅助方法
- Mapper 接口是否添加自定义查询方法

## Deferred Ideas

None — discussion stayed within phase scope
