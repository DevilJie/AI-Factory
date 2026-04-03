# Phase 1: 数据基础 - Research

**Researched:** 2026-04-01
**Domain:** MySQL DDL + MyBatis-Plus Entity/Mapper pattern (树形表 + 关联表)
**Confidence:** HIGH

## Summary

Phase 1 创建 4 张新数据库表（novel_faction、novel_faction_region、novel_faction_character、novel_faction_relation）、4 个 MyBatis-Plus 实体类、4 个 Mapper 接口，并将 NovelWorldview.forces 字段标记为 transient。这是一个纯粹的数据库 DDL + Java 实体层任务，所有模式均已在代码库中有成熟的参考实现。

项目已有两次类似的结构化重构（力量体系 power_system_migration.sql 和地理环境 geography_migration.sql），树形实体模式（NovelContinentRegion）和关联表实体模式（NovelWorldviewPowerSystem）均经过验证。本阶段的核心工作是复用这些已验证的模式，适配势力特有的字段（type、core_power_system、relation_type 等）。

**Primary recommendation:** 严格复用 NovelContinentRegion 的树形实体模式和 geography_migration.sql 的 DDL 风格，所有表不加外键约束，保持与代码库现有模式完全一致。

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** novel_faction 表使用 parentId + deep + sortOrder 树形模式，复用 ContinentRegion 已验证的架构，不加 path 字段
- **D-02:** type 字段使用枚举（正派/反派/中立），固定三种类型，前端颜色标签直接映射
- **D-03:** core_power_system 使用纯逻辑关联（Long 类型存力量体系 ID），不加数据库外键约束
- **D-04:** description 使用长文本（TEXT 类型），支持势力历史、理念等较长描述
- **D-05:** type 和 core_power_system 仅顶级势力设置，下级势力通过代码逻辑继承顶级势力值
- **D-06:** novel_faction_relation 关系双向存储（A->B 盟友 + B->A 盟友），查询只需查 faction_id OR target_faction_id
- **D-07:** novel_faction_character 的 role 字段使用自由文本（VARCHAR），适配不同题材的职位体系
- **D-08:** 三张关联表均不加数据库外键约束，纯逻辑关联，保持与 ContinentRegion 模式一致
- **D-09:** novel_faction_region 只需 faction_id + region_id 两个字段，不加 description
- **D-10:** SQL 迁移脚本只建新表、不删除 NovelWorldview.forces 列，原有数据保留，回退无风险
- **D-11:** NovelWorldview.java 中 forces 字段标记为 @TableField(exist = false) transient，从关联表动态填充

### Claude's Discretion
- SQL 迁移脚本的具体格式和注释风格
- 实体类中是否添加额外辅助方法（如 toString）
- Mapper 接口是否添加自定义查询方法

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DB-01 | 新建 novel_faction 表（树形结构），字段：id、parent_id、deep、sort_order、project_id、name、type、core_power_system、description、create_time、update_time | NovelContinentRegion 实体 + geography_migration.sql DDL 模式完全可复用，增加 type/core_power_system 两个势力特有字段 |
| DB-02 | type 和 core_power_system 仅顶级势力设置，下级势力通过代码逻辑继承顶级势力值 | 数据库层只存储值，继承逻辑在 Phase 2 BACK-06 实现。本阶段只需确保字段 nullable |
| DB-03 | 新建 novel_faction_region 关联表（势力-地区），简单多对多，字段：id、faction_id、region_id | NovelWorldviewPowerSystem 关联表模式可复用，增加 region_id 字段 |
| DB-04 | 新建 novel_faction_character 关联表（势力-人物），字段：id、faction_id、character_id、role | 参照 NovelWorldviewPowerSystem 模式，增加 role 字段 (VARCHAR) |
| DB-05 | 新建 novel_faction_relation 表（势力-势力关系），字段：id、faction_id、target_faction_id、relation_type、description | 双向存储设计（D-06），需要两个 faction ID 字段 + relation_type + description |
| DB-06 | NovelWorldview.forces 字段改为 @TableField(exist = false) transient | 已有先例：geography 字段在同一个实体类中已标记为 @TableField(exist = false) |
| DB-07 | SQL 迁移脚本（建表 + 处理 forces 列），保留 forces 列直到所有消费方迁移完毕 | D-10 决定不删列，只建新表。参照 geography_migration.sql 和 power_system_migration.sql 格式 |
</phase_requirements>

## Standard Stack

### Core (Already in project - no new installs)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| MyBatis-Plus | 3.5.5 | ORM + BaseMapper | Project standard, all entities/mappers use it |
| Lombok | (managed) | @Data annotation | Project standard, all entities use it |
| MySQL | 8.0+ | Database | Project standard, utf8mb4 charset |

### Verified in Codebase
| Pattern | Reference File | Status |
|---------|---------------|--------|
| 树形实体 | NovelContinentRegion.java | Verified - parentId + deep + sortOrder + children |
| 关联表实体 | NovelWorldviewPowerSystem.java | Verified - id + two FK fields |
| Mapper 接口 | NovelContinentRegionMapper.java | Verified - extends BaseMapper + @Mapper |
| 树形 DDL | geography_migration.sql | Verified - CHARACTER SET utf8mb4, InnoDB, Dynamic ROW_FORMAT |
| 关联表 DDL | power_system_migration.sql | Verified - novel_worldview_power_system table |
| Transient 字段 | NovelWorldview.java geography field | Verified - @TableField(exist = false) |

**No new installations required.** This phase uses only existing dependencies.

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-backend/src/main/java/com/aifactory/
├── entity/
│   ├── NovelFaction.java              # NEW - 树形实体
│   ├── NovelFactionRegion.java        # NEW - 势力-地区关联
│   ├── NovelFactionCharacter.java     # NEW - 势力-人物关联
│   ├── NovelFactionRelation.java      # NEW - 势力-势力关系
│   └── NovelWorldview.java            # MODIFY - forces 字段加 @TableField(exist=false)
├── mapper/
│   ├── NovelFactionMapper.java        # NEW
│   ├── NovelFactionRegionMapper.java  # NEW
│   ├── NovelFactionCharacterMapper.java # NEW
│   └── NovelFactionRelationMapper.java  # NEW

sql/
└── faction_migration.sql              # NEW - 4 张表的 DDL
```

### Pattern 1: 树形实体 (NovelFaction)
**What:** 树形层级结构实体，使用 parentId + deep + sortOrder 实现
**When to use:** 有父子层级关系的数据（势力、地区等）
**Example:**
```java
// Source: NovelContinentRegion.java (verified in codebase)
@Data
@TableName("novel_faction")
public class NovelFaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;         // null = 根节点
    private Integer deep;          // 0 = 根节点
    private Integer sortOrder;
    private Long projectId;
    private String name;
    private String type;           // NEW: 正派/反派/中立 (nullable, 仅顶级)
    private Long corePowerSystem;  // NEW: 力量体系ID (nullable, 仅顶级)
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<NovelFaction> children;
}
```

### Pattern 2: 关联表实体 (NovelFactionRegion / NovelFactionCharacter)
**What:** 简单关联表，两个外键 ID + 可选附加字段
**When to use:** 多对多关系映射
**Example:**
```java
// Source: NovelWorldviewPowerSystem.java (verified in codebase)
@Data
@TableName("novel_faction_region")
public class NovelFactionRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factionId;
    private Long regionId;
}
```

### Pattern 3: 关系表实体 (NovelFactionRelation)
**What:** 双向关系存储，两个方向各一条记录
**When to use:** 需要从任一端查询关系的场景
**Example:**
```java
@Data
@TableName("novel_faction_relation")
public class NovelFactionRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factionId;
    private Long targetFactionId;
    private String relationType;   // 盟友/敌对/中立
    private String description;
}
```

### Pattern 4: Mapper 接口
**What:** 纯 BaseMapper 继承，不加自定义 SQL
**When to use:** 所有 Mapper（本阶段全部）
**Example:**
```java
// Source: NovelContinentRegionMapper.java (verified in codebase)
@Mapper
public interface NovelFactionMapper extends BaseMapper<NovelFaction> {
}
```

### Pattern 5: SQL 迁移脚本
**What:** 纯 DDL 脚本，包含 CREATE TABLE + 索引，不加 DROP
**When to use:** 结构化重构迁移
**Example:**
```sql
-- Source: geography_migration.sql (verified in codebase)
SET NAMES utf8mb4;

CREATE TABLE `novel_faction` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID（NULL表示根节点）',
    `deep` int NOT NULL DEFAULT 0 COMMENT '树层级深度（0=根节点）',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '同级排序（越小越靠前）',
    `project_id` bigint NOT NULL COMMENT '归属项目ID',
    `name` varchar(200) ... NOT NULL COMMENT '势力名称',
    `type` varchar(20) ... NULL DEFAULT NULL COMMENT '势力类型（正派/反派/中立，仅顶级）',
    `core_power_system` bigint NULL DEFAULT NULL COMMENT '核心力量体系ID（仅顶级）',
    `description` text ... NULL COMMENT '势力描述',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
    INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力表（树形结构）' ROW_FORMAT = Dynamic;
```

### Anti-Patterns to Avoid
- **加数据库外键约束:** 项目约定纯逻辑关联，所有已有关联表均不加 FOREIGN KEY。保持一致。
- **删除 forces 列:** D-10 明确保留。geography 迁移中 DROP 了 geography 列，但 forces 场景不同，保留到所有消费方迁移完毕。
- **type/core_power_system 设 NOT NULL:** D-02/D-05 决定仅顶级设置，下级为 null，必须允许 NULL。
- **给 novel_faction_region 加 description 字段:** D-09 明确不需要。
- **在 Mapper 中加自定义查询:** 本阶段只需 BaseMapper，自定义查询在 Phase 2。
- **使用 Java Enum 类型作为 type 字段类型:** 项目中所有类似字段（如 role_type、status 等）均使用 String 类型。前端做枚举映射更灵活。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| ID 生成 | UUID 或雪花算法 | MyBatis-Plus IdType.AUTO | 项目所有实体统一使用自增主键 |
| 日期字段 | 手动设置时间 | MySQL DEFAULT CURRENT_TIMESTAMP / ON UPDATE CURRENT_TIMESTAMP | 数据库层自动管理，所有表一致 |
| JSON 字段 | 手动序列化 | VARCHAR/TEXT 存储 String | 项目中 cultivationLevel 等字段直接用 String |
| 树形遍历 | SQL 递归查询 | 全量查询 + Java 内存构建 | ContinentRegion 的已验证模式，项目数据量适合 |

**Key insight:** 项目中每种模式都有成熟的参考实现。本阶段不应引入任何新的设计模式或技术方案。

## Common Pitfalls

### Pitfall 1: MySQL 列名与 Java 字段名映射
**What goes wrong:** MyBatis-Plus 默认使用驼峰转下划线映射。如果 DDL 中列名与 Java 字段名不匹配（如 core_power_system 对应 corePowerSystem），运行时查不到数据。
**Why it happens:** MyBatis-Plus 配置了 map-underscore-to-camel-case=true，会自动转换。
**How to avoid:** DDL 用下划线（core_power_system），Java 用驼峰（corePowerSystem），框架自动映射，无需额外注解。
**Warning signs:** 查询返回 null 值但数据库有数据。

### Pitfall 2: NovelWorldview.forces 标记后影响现有功能
**What goes wrong:** 将 forces 标记为 @TableField(exist=false) 后，现有读取 forces 字符串的代码仍能工作（因为 SELECT * 会查出该列，MyBatis-Plus 仍会填充），但 INSERT/UPDATE 时会跳过该字段。
**Why it happens:** @TableField(exist=false) 告诉 MyBatis-Plus 该字段不参与 SQL 操作，但数据库列仍然存在。
**How to avoid:** 这正是预期行为。forces 列保留（D-10），读取不受影响。后续 Phase 3 会全面迁移读取逻辑。
**Warning signs:** 如果有代码通过 MyBatis-Plus 的 insert/update 操作 forces 字段，会静默跳过。

### Pitfall 3: DDL 中 TEXT 字段的索引限制
**What goes wrong:** MySQL 不允许在 TEXT 列上直接创建普通索引（需要指定前缀长度）。
**Why it happens:** description 用了 TEXT 类型，如果误加索引会报错。
**How to avoid:** description 字段不加索引。只对 id、project_id、parent_id、faction_id 等查询字段加索引。
**Warning signs:** CREATE TABLE 报 "BLOB/TEXT column used in key specification without a key length"。

### Pitfall 4: novel_faction_relation 的双向存储设计
**What goes wrong:** 设计时可能误以为只需要存单向关系，导致查询复杂化。
**Why it happens:** D-06 决定双向存储（A->B + B->A），每条关系存两行。
**How to avoid:** 在 SQL 注释中明确标注"双向存储，同一关系存两条记录"，并在 Phase 2 实现时确保写入/删除的原子性。
**Warning signs:** 如果查询需要同时查 faction_id 和 target_faction_id 才能找到关系，说明没按双向存储设计。

### Pitfall 5: relation_type 字段的值域
**What goes wrong:** 如果 relation_type 用中文直接存储，可能在查询条件中出编码问题。
**Why it happens:** 项目中其他类似字段（如 status、role_type）使用英文值。
**How to avoid:** relation_type 使用英文编码值（ally/hostile/neutral），在代码注释中标注映射关系。前端做中文显示。
**Warning signs:** SQL WHERE 条件中使用中文字符串。

## Code Examples

### novel_faction 表 DDL (完整)
```sql
-- Source: 参照 geography_migration.sql + power_system_migration.sql 风格
CREATE TABLE `novel_faction` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID（NULL表示根节点）',
    `deep` int NOT NULL DEFAULT 0 COMMENT '树层级深度（0=根节点）',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '同级排序（越小越靠前）',
    `project_id` bigint NOT NULL COMMENT '归属项目ID',
    `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '势力名称',
    `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '势力类型（ally/hostile/neutral，仅顶级设置）',
    `core_power_system` bigint NULL DEFAULT NULL COMMENT '核心力量体系ID（仅顶级设置，下级继承）',
    `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '势力描述',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
    INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力表（树形结构）' ROW_FORMAT = Dynamic;
```

### novel_faction_region 关联表 DDL
```sql
-- Source: 参照 novel_worldview_power_system 表风格
CREATE TABLE `novel_faction_region` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `faction_id` bigint NOT NULL COMMENT '势力ID',
    `region_id` bigint NOT NULL COMMENT '地区ID（关联 novel_continent_region.id）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
    INDEX `idx_region_id`(`region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-地区关联表' ROW_FORMAT = Dynamic;
```

### novel_faction_character 关联表 DDL
```sql
CREATE TABLE `novel_faction_character` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `faction_id` bigint NOT NULL COMMENT '势力ID',
    `character_id` bigint NOT NULL COMMENT '人物ID（关联 novel_character.id）',
    `role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位（如掌门、长老、弟子，自由文本）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
    INDEX `idx_character_id`(`character_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-人物关联表' ROW_FORMAT = Dynamic;
```

### novel_faction_relation 关系表 DDL
```sql
CREATE TABLE `novel_faction_relation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `faction_id` bigint NOT NULL COMMENT '势力ID',
    `target_faction_id` bigint NOT NULL COMMENT '目标势力ID',
    `relation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系类型（ally/hostile/neutral）',
    `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关系描述',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
    INDEX `idx_target_faction_id`(`target_faction_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-势力关系表（双向存储）' ROW_FORMAT = Dynamic;
```

### NovelFaction.java 实体
```java
// Source: 参照 NovelContinentRegion.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("novel_faction")
public class NovelFaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private Integer deep;

    private Integer sortOrder;

    private Long projectId;

    private String name;

    /** 势力类型（ally/hostile/neutral，仅顶级设置） */
    private String type;

    /** 核心力量体系ID（仅顶级设置，下级继承） */
    private Long corePowerSystem;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 子势力（非数据库字段） */
    @TableField(exist = false)
    private List<NovelFaction> children;
}
```

### NovelFactionRelation.java 实体
```java
// Source: 参照 NovelWorldviewPowerSystem.java，扩展 relation_type + description
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_faction_relation")
public class NovelFactionRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factionId;
    private Long targetFactionId;
    private String relationType;
    private String description;
}
```

### NovelWorldview.java 修改
```java
// 修改前（当前代码）:
private String forces;

// 修改后:
/** 势力分布（非数据库字段，由 FactionService 从 novel_faction 表构建） */
@TableField(exist = false)
private String forces;
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 纯文本 forces 字段 | 结构化树形表 + 关联表 | 本次重构 | AI 生成可精确引用势力关系 |
| 单字符串存储所有势力 | parentId + deep 树形结构 | 地理环境重构时验证 | 支持多层级嵌套编辑 |

**Deprecated/outdated:**
- novel_worldview.forces 列：保留但不再作为主要数据源，后续迁移后可清理

## Open Questions

1. **relation_type 的编码值约定**
   - What we know: D-02 定义了 type 的枚举（正派/反派/中立），但 relation_type 的具体编码值未在 decisions 中明确
   - What's unclear: 使用中文还是英文编码
   - Recommendation: 使用英文编码（ally/hostile/neutral），与项目中其他 status/type 字段风格一致（如 role_type 使用 protagonist/supporting/antagonist/npc）

2. **novel_faction_relation.description 的字段类型**
   - What we know: 需要支持关系描述
   - What's unclear: 使用 VARCHAR 还是 TEXT
   - Recommendation: 使用 VARCHAR(500)，关系描述通常较短，不需要 TEXT。且 VARCHAR 可以方便加索引（虽然本阶段不加）

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (already in project) |
| Config file | src/test/resources/application-test.yml (if exists) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=*Test -q` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DB-01 | novel_faction 表创建成功，字段完整 | manual (SQL DDL validation) | `mysql -e "DESC novel_faction"` | N/A - manual |
| DB-02 | type/core_power_system 允许 NULL | manual (DDL check) | `mysql -e "SHOW COLUMNS FROM novel_faction WHERE Field='type'"` | N/A - manual |
| DB-03 | novel_faction_region 表创建成功 | manual (SQL DDL validation) | `mysql -e "DESC novel_faction_region"` | N/A - manual |
| DB-04 | novel_faction_character 表创建成功 | manual (SQL DDL validation) | `mysql -e "DESC novel_faction_character"` | N/A - manual |
| DB-05 | novel_faction_relation 表创建成功 | manual (SQL DDL validation) | `mysql -e "DESC novel_faction_relation"` | N/A - manual |
| DB-06 | NovelWorldview.forces 标记为 transient | unit (compile check) | `mvn compile -q` | N/A - compile validates |
| DB-07 | SQL 迁移脚本语法正确 | manual (execute against DB) | `mysql < sql/faction_migration.sql` | N/A - manual |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn compile -q` (编译验证)
- **Per wave merge:** `cd ai-factory-backend && mvn test` (全量测试)
- **Phase gate:** 编译通过 + SQL 脚本可执行 + 4 个实体类和 Mapper 接口就绪

### Wave 0 Gaps
- None - 本阶段主要是 DDL + 实体类定义，编译验证即可。不需要单独的单元测试文件。
- 验证方式：`mvn compile` 通过 + 手动执行 SQL 脚本建表成功。

## Sources

### Primary (HIGH confidence)
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelContinentRegion.java` - 树形实体模式
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelContinentRegionMapper.java` - Mapper 模式
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java` - forces 字段位置 + geography transient 先例
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldviewPowerSystem.java` - 关联表实体模式
- `sql/geography_migration.sql` - DDL 风格和注释规范
- `sql/power_system_migration.sql` - 关联表 DDL 和提示词更新风格
- `sql/init.sql` - 完整数据库 schema 参考

### Secondary (MEDIUM confidence)
- `.planning/phases/01-数据基础/01-CONTEXT.md` - 用户决策
- `.planning/REQUIREMENTS.md` - 需求定义

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - 所有模式均在代码库中验证过，无需外部依赖
- Architecture: HIGH - 严格复用已有实体和 DDL 模式，零创新
- Pitfalls: HIGH - 基于代码库实际模式的注意事项，非推测

**Research date:** 2026-04-01
**Valid until:** 30 days (stable patterns, no external dependencies)
