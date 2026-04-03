# Phase 1: 数据基础 - Context

**Gathered:** 2026-04-01
**Status:** Ready for planning

<domain>
## Phase Boundary

新建 4 张数据库表（novel_faction、novel_faction_region、novel_faction_character、novel_faction_relation）、4 个 MyBatis-Plus 实体类、4 个 Mapper 接口，将 NovelWorldview.forces 标记为 transient。为所有后续开发提供数据模型基础。

</domain>

<decisions>
## Implementation Decisions

### 势力表字段设计
- **D-01:** novel_faction 表使用 parentId + deep + sortOrder 树形模式，复用 ContinentRegion 已验证的架构，不加 path 字段
- **D-02:** type 字段使用枚举（正派/反派/中立），固定三种类型，前端颜色标签直接映射
- **D-03:** core_power_system 使用纯逻辑关联（Long 类型存力量体系 ID），不加数据库外键约束
- **D-04:** description 使用长文本（TEXT 类型），支持势力历史、理念等较长描述
- **D-05:** type 和 core_power_system 仅顶级势力设置，下级势力通过代码逻辑继承顶级势力值

### 关联表设计
- **D-06:** novel_faction_relation 关系双向存储（A→B 盟友 + B→A 盟友），查询只需查 faction_id OR target_faction_id
- **D-07:** novel_faction_character 的 role 字段使用自由文本（VARCHAR），适配不同题材的职位体系
- **D-08:** 三张关联表均不加数据库外键约束，纯逻辑关联，保持与 ContinentRegion 模式一致
- **D-09:** novel_faction_region 只需 faction_id + region_id 两个字段，不加 description

### forces 列迁移
- **D-10:** SQL 迁移脚本只建新表、不删除 NovelWorldview.forces 列，原有数据保留，回退无风险
- **D-11:** NovelWorldview.java 中 forces 字段标记为 @TableField(exist = false) transient，从关联表动态填充

### Claude's Discretion
- SQL 迁移脚本的具体格式和注释风格
- 实体类中是否添加额外辅助方法（如 toString）
- Mapper 接口是否添加自定义查询方法

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 现有树形实体参考
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelContinentRegion.java` — 树形实体字段模式（parentId + deep + sortOrder + children）
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelContinentRegionMapper.java` — Mapper 接口模式（纯 BaseMapper）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java` — forces 字段位置，需改为 transient

### 需求定义
- `.planning/REQUIREMENTS.md` §v1 需求 > 数据库与领域层 — DB-01 到 DB-07 的完整定义

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **NovelContinentRegion 实体**: 树形字段模式完全可复用（id, parentId, deep, sortOrder, projectId, name, description, children）
- **NovelContinentRegionMapper**: 纯 BaseMapper 模式，无需自定义 SQL

### Established Patterns
- **树形表模式**: parentId (Long, null=根节点) + deep (Integer, 根=0) + sortOrder (Integer, 同级排序) + children (transient List)
- **MyBatis-Plus 实体注解**: @Data + @TableName + @TableId(type = IdType.AUTO) + @TableField(exist = false) for transient
- **Mapper 模式**: 继承 BaseMapper<Entity>，加 @Mapper 注解

### Integration Points
- NovelWorldview.java 的 forces 字段需改为 @TableField(exist = false)，与 geography 字段保持一致
- 后续 Phase 2 的 FactionService 将参照 ContinentRegionService 模式

</code_context>

<specifics>
## Specific Ideas

- 复用 ContinentRegion 的树形模式，保持代码库一致性
- 所有表不加外键约束，与现有代码风格统一

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-数据基础*
*Context gathered: 2026-04-01*
