-- faction_migration.sql
-- 势力阵营结构化重构迁移脚本

SET NAMES utf8mb4;

-- 1. 新建势力表（树形结构，支持多层级嵌套）
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

-- 2. 新建势力-地区关联表（多对多关联）
CREATE TABLE `novel_faction_region` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `faction_id` bigint NOT NULL COMMENT '势力ID',
    `region_id` bigint NOT NULL COMMENT '地区ID（关联 novel_continent_region.id）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
    INDEX `idx_region_id`(`region_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-地区关联表' ROW_FORMAT = Dynamic;

-- 3. 新建势力-人物关联表（含职位字段）
CREATE TABLE `novel_faction_character` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `faction_id` bigint NOT NULL COMMENT '势力ID',
    `character_id` bigint NOT NULL COMMENT '人物ID（关联 novel_character.id）',
    `role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位（如掌门、长老、弟子，自由文本）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_faction_id`(`faction_id` ASC) USING BTREE,
    INDEX `idx_character_id`(`character_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '势力-人物关联表' ROW_FORMAT = Dynamic;

-- 4. 新建势力-势力关系表（双向存储，同一关系存两条记录）
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
