# 世界观四大区域建表设计

## 背景

世界观（novel_worldview）现有 `geography`、`forces`、`timeline`、`rules` 四个 TEXT 字段，存储非结构化数据。参照力量体系（power_system）的改造模式，将这四个区域拆分为独立的结构化表，支持多层级管理和 AI 约束生成。

## 设计原则

- 完全复刻力量体系的模式：独立主表 + 子表 + 世界观关联表
- 每张表包含 `project_id`（主表）或外键（子表）用于关联
- 所有表使用 `bigint AUTO_INCREMENT` 主键、`utf8mb4` 字符集、`InnoDB` 引擎
- 后端 Entity 使用 MyBatis-Plus + Lombok，子列表通过 `@TableField(exist = false)` 嵌套
- 子表均有 `level` 字段支持排序

## 总览

共 15 张新表：

| 区域 | 表名 | 说明 |
|------|------|------|
| 地理环境 | novel_geo_region | 大陆/世界层 |
| 地理环境 | novel_geo_sub_region | 子区域层（国家/州省） |
| 地理环境 | novel_geo_landmark | 地标层（城市/秘境/遗迹） |
| 地理环境 | novel_worldview_geo | 世界观-地理区域关联 |
| 势力阵营 | novel_faction | 势力主表 |
| 势力阵营 | novel_faction_relation | 势力间关系 |
| 势力阵营 | novel_faction_member | 势力成员（预留） |
| 势力阵营 | novel_faction_geo | 势力与子区域关联 |
| 势力阵营 | novel_worldview_faction | 世界观-势力关联 |
| 时间线 | novel_timeline_era | 时代表 |
| 时间线 | novel_timeline_event | 事件表 |
| 时间线 | novel_worldview_timeline | 世界观-时代关联 |
| 世界规则 | novel_rule_category | 规则类别表 |
| 世界规则 | novel_rule_detail | 规则明细表 |
| 世界规则 | novel_worldview_rule | 世界观-规则类别关联 |

---

## 一、地理环境（Geography）

三层结构：大陆/世界 → 子区域/国家 → 城市/地标

### novel_geo_region（大陆/世界层）

```sql
CREATE TABLE `novel_geo_region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '区域名称',
  `region_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '区域类型（大陆/海洋/岛屿/虚空等）',
  `climate` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '气候特征',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '区域整体描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地理区域表（大陆/世界）' ROW_FORMAT = Dynamic;
```

### novel_geo_sub_region（子区域层）

```sql
CREATE TABLE `novel_geo_sub_region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `geo_region_id` bigint NOT NULL COMMENT '关联大陆/世界ID',
  `level` int NULL DEFAULT NULL COMMENT '排序索引',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '子区域名称',
  `terrain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地形特征',
  `resources` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源特产',
  `danger_level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '危险等级',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '子区域描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_geo_region_id`(`geo_region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地理子区域表（国家/州省）' ROW_FORMAT = Dynamic;
```

### novel_geo_landmark（地标层）

```sql
CREATE TABLE `novel_geo_landmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `geo_sub_region_id` bigint NOT NULL COMMENT '关联子区域ID',
  `level` int NULL DEFAULT NULL COMMENT '排序索引',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地标名称',
  `landmark_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型（城市/秘境/遗迹/自然景观等）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '地标描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_geo_sub_region_id`(`geo_sub_region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地理地标表（城市/秘境/遗迹）' ROW_FORMAT = Dynamic;
```

### novel_worldview_geo（关联表）

```sql
CREATE TABLE `novel_worldview_geo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worldview_id` bigint NOT NULL COMMENT '世界观ID',
  `geo_region_id` bigint NOT NULL COMMENT '地理区域ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
  INDEX `idx_geo_region_id`(`geo_region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-地理区域关联表' ROW_FORMAT = Dynamic;
```

---

## 二、势力阵营（Faction）

1 主表 + 2 子表 + 1 区域关联 + 1 世界观关联

### novel_faction（势力主表）

```sql
CREATE TABLE `novel_faction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '势力名称',
  `faction_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型（宗门/帝国/商会/暗组织/种族等）',
  `territory` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '势力范围/领地',
  `power_level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实力等级',
  `philosophy` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '核心理念/宗旨',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '势力描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力阵营表' ROW_FORMAT = Dynamic;
```

### novel_faction_relation（势力关系表）

```sql
CREATE TABLE `novel_faction_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `faction_id` bigint NOT NULL COMMENT '势力A的ID',
  `related_faction_id` bigint NOT NULL COMMENT '势力B的ID',
  `relation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关系类型（敌对/同盟/附属/中立/贸易等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关系描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力关系表' ROW_FORMAT = Dynamic;
```

### novel_faction_member（势力成员表，预留）

```sql
CREATE TABLE `novel_faction_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `faction_id` bigint NOT NULL COMMENT '关联势力ID',
  `character_id` bigint NULL DEFAULT NULL COMMENT '角色ID（预留）',
  `role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份/职位',
  `join_time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加入时间',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '成员描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力成员表' ROW_FORMAT = Dynamic;
```

### novel_faction_geo（势力与子区域关联表）

```sql
CREATE TABLE `novel_faction_geo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `faction_id` bigint NOT NULL COMMENT '势力ID',
  `geo_sub_region_id` bigint NOT NULL COMMENT '子区域ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
  INDEX `idx_geo_sub_region_id`(`geo_sub_region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-子区域关联表' ROW_FORMAT = Dynamic;
```

### novel_worldview_faction（关联表）

```sql
CREATE TABLE `novel_worldview_faction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worldview_id` bigint NOT NULL COMMENT '世界观ID',
  `faction_id` bigint NOT NULL COMMENT '势力ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
  INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-势力阵营关联表' ROW_FORMAT = Dynamic;
```

---

## 三、时间线（Timeline）

双层结构：时代 → 事件

### novel_timeline_era（时代表）

```sql
CREATE TABLE `novel_timeline_era` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时代名称',
  `time_range` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时间范围',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '时代概述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '时间线时代表' ROW_FORMAT = Dynamic;
```

### novel_timeline_event（事件表）

```sql
CREATE TABLE `novel_timeline_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `timeline_era_id` bigint NOT NULL COMMENT '关联时代ID',
  `level` int NULL DEFAULT NULL COMMENT '排序索引',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '事件名称',
  `time_point` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时间点',
  `impact` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '事件影响',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '事件详细描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_timeline_era_id`(`timeline_era_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '时间线事件表' ROW_FORMAT = Dynamic;
```

### novel_worldview_timeline（关联表）

```sql
CREATE TABLE `novel_worldview_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worldview_id` bigint NOT NULL COMMENT '世界观ID',
  `timeline_era_id` bigint NOT NULL COMMENT '时代ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
  INDEX `idx_timeline_era_id`(`timeline_era_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-时间线关联表' ROW_FORMAT = Dynamic;
```

---

## 四、世界规则（WorldRule）

双层结构：规则类别 → 规则明细

### novel_rule_category（规则类别表）

```sql
CREATE TABLE `novel_rule_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类别名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '类别概述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界规则类别表' ROW_FORMAT = Dynamic;
```

### novel_rule_detail（规则明细表）

```sql
CREATE TABLE `novel_rule_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_category_id` bigint NOT NULL COMMENT '关联类别ID',
  `level` int NULL DEFAULT NULL COMMENT '排序索引',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '规则描述',
  `exceptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '例外情况',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rule_category_id`(`rule_category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界规则明细表' ROW_FORMAT = Dynamic;
```

### novel_worldview_rule（关联表）

```sql
CREATE TABLE `novel_worldview_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worldview_id` bigint NOT NULL COMMENT '世界观ID',
  `rule_category_id` bigint NOT NULL COMMENT '规则类别ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
  INDEX `idx_rule_category_id`(`rule_category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-世界规则关联表' ROW_FORMAT = Dynamic;
```

---

## 迁移说明

参照力量体系迁移模式（power_system_migration.sql），迁移脚本需要：

1. 创建以上 15 张新表
2. 从 `novel_worldview` 表的 `geography`/`forces`/`timeline`/`rules` 四个 TEXT 字段解析数据并迁移到新表
3. 更新 AI 提示词模板，支持新的 XML 结构化输出格式
4. 保留原 TEXT 字段作为兼容（可选，视迁移策略而定）

## 后端实现模式

参照 PowerSystemController/Service/Mapper 的完整链路，每个区域需要：

- Entity 层：主表 Entity（含 `@TableField(exist = false)` 嵌套子列表）
- Mapper 层：继承 BaseMapper
- Service 层：CRUD + 嵌套保存/删除 + 约束文本生成
- Controller 层：REST API（list/get/save/delete）

## 前端实现模式

参照 PowerSystemSection/PowerSystemCard/PowerSystemForm 组件结构：

- Section 容器组件
- Card 展示组件（可展开查看子层级）
- Form 编辑组件（支持嵌套编辑）
- API 层（TypeScript 接口 + axios 调用）
