/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.31.7
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : 192.168.31.7:3306
 Source Schema         : ai_factory

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 28/03/2026 23:16:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_interaction_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_interaction_log`;
CREATE TABLE `ai_interaction_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交互流水号',
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `volume_plan_id` bigint NULL DEFAULT NULL COMMENT '分卷计划ID',
  `chapter_plan_id` bigint NULL DEFAULT NULL COMMENT '章节规划ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `request_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求类型',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI提供商',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的模型名称',
  `request_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求提示词',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数（JSON格式）',
  `response_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应内容',
  `response_tokens` int NULL DEFAULT NULL COMMENT '响应token数',
  `response_duration` bigint NULL DEFAULT NULL COMMENT '响应耗时（毫秒）',
  `is_success` tinyint(1) NULL DEFAULT 1 COMMENT '是否成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_request_type`(`request_type` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_volume_plan_id`(`volume_plan_id` ASC) USING BTREE,
  INDEX `idx_chapter_plan_id`(`chapter_plan_id` ASC) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI交互日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_template`;
CREATE TABLE `ai_prompt_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码（唯一，语义化）',
  `template_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `service_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型：llm/image/video/common',
  `scenario` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '使用场景',
  `current_version_id` bigint NOT NULL COMMENT '当前激活版本ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模板描述',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统内置',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_code`(`template_code` ASC) USING BTREE,
  INDEX `idx_service_scenario`(`service_type` ASC, `scenario` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 106 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI提示词模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_prompt_template_version
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_template_version`;
CREATE TABLE `ai_prompt_template_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `version_number` int NOT NULL COMMENT '版本号',
  `template_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板内容',
  `variable_definitions` json NULL COMMENT '变量定义JSON',
  `version_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本说明',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否激活',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_version`(`template_id` ASC, `version_number` ASC) USING BTREE,
  INDEX `idx_template_id`(`template_id` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI提示词模板版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_task`;
CREATE TABLE `ai_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型',
  `task_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务配置',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '状态',
  `progress` int NULL DEFAULT 0 COMMENT '进度',
  `current_step` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前步骤',
  `result_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `created_by` bigint NOT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `started_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `completed_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_by`(`created_by` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_task_step
-- ----------------------------
DROP TABLE IF EXISTS `ai_task_step`;
CREATE TABLE `ai_task_step`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `step_order` int NOT NULL COMMENT '步骤顺序',
  `step_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤名称',
  `step_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤类型',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '步骤配置',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '状态',
  `progress` int NULL DEFAULT 0 COMMENT '进度',
  `result_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `started_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `completed_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_order`(`task_id` ASC, `step_order` ASC) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI任务步骤表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chapter_plot_memory
-- ----------------------------
DROP TABLE IF EXISTS `chapter_plot_memory`;
CREATE TABLE `chapter_plot_memory`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID',
  `chapter_number` int NULL DEFAULT NULL COMMENT '章节序号',
  `chapter_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '章节标题',
  `plot_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '剧情摘要',
  `chapter_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节内容总结',
  `key_events` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '核心事件',
  `character_status` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '人物状态变化',
  `new_settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '新出现的设定',
  `foreshadowing_planted` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '埋下的伏笔',
  `foreshadowing_resolved` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回收的伏笔',
  `pending_foreshadowing` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '待回收伏笔',
  `current_suspense` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '当前悬念',
  `chapter_ending_scene` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节结尾场景',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '章节剧情记忆表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_chapter
-- ----------------------------
DROP TABLE IF EXISTS `novel_chapter`;
CREATE TABLE `novel_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `volume_plan_id` bigint NULL DEFAULT NULL COMMENT '分卷规划ID',
  `chapter_plan_id` bigint NULL DEFAULT NULL COMMENT '章节规划ID',
  `chapter_number` int NOT NULL COMMENT '章节序号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '章节标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节内容',
  `word_count` int NULL DEFAULT 0 COMMENT '字数',
  `plot_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '剧情概要',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'draft' COMMENT '状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_chapter`(`project_id` ASC, `chapter_number` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '章节表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_chapter_plan
-- ----------------------------
DROP TABLE IF EXISTS `novel_chapter_plan`;
CREATE TABLE `novel_chapter_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节规划ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `volume_plan_id` bigint NULL DEFAULT NULL COMMENT '分卷规划ID',
  `chapter_number` int NOT NULL COMMENT '章节编号',
  `chapter_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '章节标题',
  `plot_outline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '情节大纲',
  `chapter_starting_scene` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节起点场景',
  `chapter_ending_scene` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节终点场景',
  `key_events` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关键事件',
  `chapter_goal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节目标',
  `word_count_target` int NULL DEFAULT NULL COMMENT '目标字数',
  `chapter_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'planned' COMMENT '状态',
  `plot_stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '情节阶段',
  `stage_completed` tinyint(1) NULL DEFAULT 0 COMMENT '阶段是否完成',
  `foreshadowing_setup` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '埋伏笔',
  `foreshadowing_payoff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '填伏笔',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `character_arcs` json NULL COMMENT '人物弧光变化',
  `foreshadowing_actions` json NULL COMMENT '伏笔操作',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_volume_plan_id`(`volume_plan_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 258 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小说章节规划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_character
-- ----------------------------
DROP TABLE IF EXISTS `novel_character`;
CREATE TABLE `novel_character`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `age` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '年龄',
  `role_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色类型',
  `role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色定位',
  `personality` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '性格特点',
  `appearance` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '外貌描述',
  `appearance_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图像生成描述词',
  `background` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '背景故事',
  `abilities` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '能力/技能',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小说角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_character_chapter
-- ----------------------------
DROP TABLE IF EXISTS `novel_character_chapter`;
CREATE TABLE `novel_character_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `chapter_number` int NOT NULL COMMENT '章节编号',
  `status_in_chapter` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '本章状态描述',
  `emotion_change` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '情绪变化',
  `key_behavior` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关键行为',
  `is_first_appearance` tinyint(1) NULL DEFAULT 0 COMMENT '是否首次出场',
  `importance_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'supporting' COMMENT '重要程度: protagonist/supporting/minor/cameo',
  `appearance_change` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '外貌/装扮变化',
  `personality_reveal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '性格展现',
  `ability_shown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '能力展现',
  `character_development` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '角色成长',
  `dialogue_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '核心对话摘要',
  `cultivation_level` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '修为境界(JSON格式)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_character_chapter`(`character_id` ASC, `chapter_id` ASC) USING BTREE,
  INDEX `idx_character_id`(`character_id` ASC) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色-章节关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_foreshadowing
-- ----------------------------
DROP TABLE IF EXISTS `novel_foreshadowing`;
CREATE TABLE `novel_foreshadowing`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '伏笔ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '伏笔标题',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '伏笔类型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '伏笔描述',
  `layout_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '布局类型',
  `planted_chapter` int NULL DEFAULT NULL COMMENT '埋伏笔的章节',
  `planned_callback_chapter` int NULL DEFAULT NULL COMMENT '计划填坑的章节',
  `actual_callback_chapter` int NULL DEFAULT NULL COMMENT '实际填坑的章节',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '状态',
  `priority` int NULL DEFAULT 0 COMMENT '优先级',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '伏笔表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_outline
-- ----------------------------
DROP TABLE IF EXISTS `novel_outline`;
CREATE TABLE `novel_outline`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '大纲ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `overall_concept` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '整体故事梗概',
  `main_theme` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主要主题',
  `target_volume_count` int NULL DEFAULT NULL COMMENT '目标卷数',
  `target_chapter_count` int NULL DEFAULT NULL COMMENT '目标章节数',
  `target_word_count` int NULL DEFAULT NULL COMMENT '目标字数',
  `genre` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故事类型',
  `tone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故事基调',
  `creation_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创作备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'draft' COMMENT '状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小说大纲表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_storyboard
-- ----------------------------
DROP TABLE IF EXISTS `novel_storyboard`;
CREATE TABLE `novel_storyboard`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分镜ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID',
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `shot_number` int NULL DEFAULT NULL COMMENT '镜头编号',
  `shot_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '景别',
  `camera_angle` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拍摄角度',
  `camera_movement` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '运镜方式',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分镜描述',
  `visual_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '画面生成提示词',
  `duration` int NULL DEFAULT NULL COMMENT '镜头时长（秒）',
  `character_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '出场角色ID列表',
  `dialogue` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '台词',
  `action` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '动作描述',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注信息',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '生成的首帧图URL',
  `generation_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图像生成参数',
  `sort_order` int NULL DEFAULT NULL COMMENT '排序序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小说章节分镜表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_volume_plan
-- ----------------------------
DROP TABLE IF EXISTS `novel_volume_plan`;
CREATE TABLE `novel_volume_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分卷规划ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `outline_id` bigint NOT NULL COMMENT '大纲ID',
  `volume_number` int NOT NULL COMMENT '卷号',
  `volume_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '卷标题',
  `volume_theme` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '卷主题',
  `main_conflict` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主要冲突',
  `plot_arc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '情节走向',
  `volume_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '本卷简介',
  `key_events` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关键事件',
  `estimated_word_count` int NULL DEFAULT NULL COMMENT '预计字数',
  `timeline_setting` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '时间线设定',
  `target_chapter_count` int NULL DEFAULT NULL COMMENT '目标章节数',
  `volume_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分卷备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'planned' COMMENT '状态',
  `sort_order` int NULL DEFAULT NULL COMMENT '排序',
  `volume_completed` tinyint(1) NULL DEFAULT 0 COMMENT '是否完成',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `core_goal` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '核心目标',
  `climax` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '高潮事件',
  `ending` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '收尾描述',
  `start_chapter` int NULL DEFAULT NULL COMMENT '起始章节号',
  `end_chapter` int NULL DEFAULT NULL COMMENT '结束章节号',
  `new_characters` json NULL COMMENT '本卷新增人物',
  `stage_foreshadowings` json NULL COMMENT '阶段伏笔配置',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_outline_volume`(`outline_id` ASC, `volume_number` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_outline_id`(`outline_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 86 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小说分卷规划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_worldview
-- ----------------------------
DROP TABLE IF EXISTS `novel_worldview`;
CREATE TABLE `novel_worldview`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `outline_id` bigint NULL DEFAULT NULL COMMENT '大纲ID',
  `world_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '世界类型',
  `world_background` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '世界背景描述',
  `geography` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '地理环境',
  `forces` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '势力分布',
  `timeline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '时间线设定',
  `rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '世界规则',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `protagonist_ids` json NULL COMMENT '主角ID列表',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观设定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for project_basic_settings
-- ----------------------------
DROP TABLE IF EXISTS `project_basic_settings`;
CREATE TABLE `project_basic_settings`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `narrative_structure` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '整体叙事结构：three_act/multi_line/flashback/interpolation',
  `ending_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结局类型：open/closed/semi_open',
  `ending_tone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结局基调：tragedy/comedy/serious',
  `writing_style` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文风调性：realistic/gorgeous/concise/humor/sharp/gentle/desolate',
  `writing_perspective` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'third_person' COMMENT '写作视角：first_person/third_person/omniscient',
  `narrative_pace` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节奏把控：fast/slow/mixed',
  `language_style` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语言体系：archaic/tech/urban',
  `description_focus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描写侧重：action/psychology/environment/dialogue',
  `plot_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '阶段节点/卷篇设定（JSON）',
  `chapter_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '单章创作设定（JSON）',
  `foreshadowing_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '伏笔与埋线配置（JSON）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目基础设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_activity_log
-- ----------------------------
DROP TABLE IF EXISTS `t_activity_log`;
CREATE TABLE `t_activity_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `project_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `activity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动类型: PROJECT_CREATED/CHAPTER_CREATED/CHAPTER_UPDATED',
  `resource_id` bigint NULL DEFAULT NULL COMMENT '资源ID',
  `resource_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源标题',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_flag` tinyint NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ai_provider
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_provider`;
CREATE TABLE `t_ai_provider`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `provider_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务商类型：llm/image/tts/video',
  `provider_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务商代码',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务商名称',
  `icon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标URL',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `api_endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API端点',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称',
  `is_default` int NULL DEFAULT 0 COMMENT '是否默认',
  `enabled` int NULL DEFAULT 1 COMMENT '是否启用',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置JSON',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_provider_type`(`provider_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI提供商表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ai_provider_template
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_provider_template`;
CREATE TABLE `t_ai_provider_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `template_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板代码',
  `provider_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务商类型：llm/image/tts/video',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商名称',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '显示名称',
  `icon_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标URL',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `default_endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认API端点',
  `default_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认模型',
  `config_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置参数JSON Schema',
  `required_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '必填字段（JSON数组）',
  `optional_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '可选字段（JSON数组）',
  `is_system` tinyint(1) NULL DEFAULT 1 COMMENT '是否系统内置',
  `is_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `template_code`(`template_code` ASC) USING BTREE,
  INDEX `idx_provider_type`(`provider_type` ASC) USING BTREE,
  INDEX `idx_is_enabled`(`is_enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_project
-- ----------------------------
DROP TABLE IF EXISTS `t_project`;
CREATE TABLE `t_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目描述',
  `project_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项目类型：video/novel',
  `story_tone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故事基调',
  `story_genre` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故事类型',
  `novel_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '小说类型',
  `target_length` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标长度',
  `visual_style` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '视觉风格',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图URL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'draft' COMMENT '状态',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签',
  `chapter_count` int NULL DEFAULT 0 COMMENT '章节数量',
  `total_word_count` int NULL DEFAULT 0 COMMENT '总字数',
  `total_duration` int NULL DEFAULT 0 COMMENT '总时长（秒）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `dify_dataset_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Dify知识库ID',
  `dify_dataset_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Dify知识库名称',
  `setup_stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'project_created' COMMENT '项目设置阶段',
  `view_count` int NULL DEFAULT 0 COMMENT '点击量',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞量',
  `favorite_count` int NULL DEFAULT 0 COMMENT '收藏量',
  `share_count` int NULL DEFAULT 0 COMMENT '转发量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_uid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户UID',
  `login_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录名',
  `login_pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录密码',
  `actual_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` int NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `disabled_flag` int NULL DEFAULT 0 COMMENT '禁用标志：0-正常，1-禁用',
  `deleted_flag` int NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `login_name`(`login_name` ASC) USING BTREE,
  UNIQUE INDEX `user_uid`(`user_uid` ASC) USING BTREE,
  INDEX `idx_login_name`(`login_name` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_ext
-- ----------------------------
DROP TABLE IF EXISTS `t_user_ext`;
CREATE TABLE `t_user_ext`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '语言',
  `theme` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主题',
  `email_notification` int NULL DEFAULT 1 COMMENT '邮件通知',
  `browser_notification` int NULL DEFAULT 1 COMMENT '浏览器通知',
  `project_notification` int NULL DEFAULT 1 COMMENT '项目通知',
  `auto_save` int NULL DEFAULT 1 COMMENT '自动保存',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '个人简介',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户扩展表' ROW_FORMAT = Dynamic;


/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.31.7
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : 192.168.31.7:3306
 Source Schema         : ai_factory

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 28/03/2026 23:17:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_template`;
CREATE TABLE `ai_prompt_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码（唯一，语义化）',
  `template_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `service_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型：llm/image/video/common',
  `scenario` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '使用场景',
  `current_version_id` bigint NOT NULL COMMENT '当前激活版本ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模板描述',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统内置',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_code`(`template_code` ASC) USING BTREE,
  INDEX `idx_service_scenario`(`service_type` ASC, `scenario` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 106 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI提示词模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_prompt_template
-- ----------------------------
INSERT INTO `ai_prompt_template` VALUES (1, 'llm_chapter_generate_standard', '章节生成标准模板', 'llm', 'chapter_generate', 1, '用于生成小说章节的标准提示词模板，包含字数控制、情节要求等', '小说,章节,生成', 1, 1, NULL, '2026-02-05 13:25:20', NULL, '2026-03-19 14:44:39');
INSERT INTO `ai_prompt_template` VALUES (2, 'llm_outline_generate_detailed', '大纲生成详细模板', 'llm', 'outline_generate', 2, '用于生成小说完整大纲的提示词模板，包含分卷规划等', '小说,大纲,生成', 1, 1, NULL, '2026-02-05 13:25:39', NULL, '2026-02-27 02:39:53');
INSERT INTO `ai_prompt_template` VALUES (3, 'llm_worldview_create', '世界观创建模板', 'llm', 'worldview_create', 3, '用于创建小说世界观的提示词模板', '小说,世界观,生成', 1, 1, NULL, '2026-02-05 13:25:39', NULL, '2026-02-27 02:39:53');
INSERT INTO `ai_prompt_template` VALUES (4, 'llm_project_name_generate', '项目名称和描述生成模板', 'llm', 'project_generate', 4, '用于根据用户想法生成项目名称和描述的提示词模板，主角姓名要符合当前主流的命名风格，尽可能简洁明了的描述出整个故事的大纲，以及剧情走向', '项目,名称,生成', 1, 1, NULL, '2026-02-05 13:48:28', NULL, '2026-03-17 23:18:16');
INSERT INTO `ai_prompt_template` VALUES (5, 'llm_outline_volume_generate', '分卷规划生成模板', 'llm', 'outline_volume_generate', 5, '用于生成小说分卷规划的提示词模板，支持全新生成、继续生成、重新生成', '小说,大纲,分卷,生成', 1, 1, NULL, '2026-02-06 22:21:13', NULL, '2026-03-28 23:07:18');
INSERT INTO `ai_prompt_template` VALUES (6, 'llm_outline_chapter_generate', '章节规划生成模板', 'llm', 'outline_chapter_generate', 6, '用于生成分卷章节规划的提示词模板，包含专业网文结构设计指导', '小说,大纲,章节,生成', 1, 1, NULL, '2026-02-06 22:21:54', NULL, '2026-02-27 02:39:53');
INSERT INTO `ai_prompt_template` VALUES (7, 'llm_outline_character_generate', '角色生成模板', 'llm', 'outline_character_generate', 7, '用于生成分卷角色的提示词模板', '小说,大纲,角色,生成', 1, 1, NULL, '2026-02-06 22:22:35', NULL, '2026-02-27 02:39:53');
INSERT INTO `ai_prompt_template` VALUES (8, 'llm_chapter_compress', '章节内容压缩模板', 'llm', 'chapter_generation', 8, '用于压缩超长的章节内容，保持剧情不变，减少冗余描述', '小说,章节,压缩', 1, 1, NULL, '2026-02-11 20:23:51', NULL, '2026-03-28 23:07:53');
INSERT INTO `ai_prompt_template` VALUES (9, 'llm_protagonist_create', '主角生成模板', 'llm', 'protagonist_create', 9, '用于生成小说主角的提示词模板，包含姓名、性别、年龄、性格、外貌、背景等', '小说,主角,生成,人物', 1, 1, NULL, '2026-02-12 22:02:56', NULL, '2026-03-28 23:07:54');
INSERT INTO `ai_prompt_template` VALUES (10, 'llm_character_extract', '章节人物提取模板', 'llm', 'character_extract', 10, '用于从章节内容中提取人物信息的提示词模板', '小说,人物,提取', 1, 1, NULL, '2026-02-12 22:34:00', NULL, '2026-03-28 23:07:30');
INSERT INTO `ai_prompt_template` VALUES (11, 'llm_chapter_fix_check', '章节问题检查模板', 'llm', 'chapter_fix_check', 11, 'AI剧情修复-问题检查阶段，检测章节中的逻辑问题、设定矛盾、连贯性问题等', NULL, 1, 1, 1, '2026-03-03 20:52:22', NULL, '2026-03-28 23:07:44');
INSERT INTO `ai_prompt_template` VALUES (12, 'llm_chapter_fix_apply', '章节修复应用模板', 'llm', 'chapter_fix_apply', 12, 'AI剧情修复-应用修复阶段，根据问题清单修复章节内容', NULL, 1, 1, 1, '2026-03-03 20:55:24', NULL, '2026-03-28 23:07:44');
INSERT INTO `ai_prompt_template` VALUES (13, 'llm_volume_optimize', '分卷优化模板', 'llm', 'volume_optimize', 13, '用于AI优化单个分卷详情的提示词模板，参考世界观和前面所有卷的信息', NULL, 1, 1, NULL, '2026-03-16 17:06:21', NULL, '2026-03-28 23:07:44');
INSERT INTO `ai_prompt_template` VALUES (14, 'llm_chapter_memory_extract', '章节剧情记忆提取模板', 'llm', 'chapter_memory_extract', 14, '从章节内容提取核心信息，用于后续剧情连贯承接', '小说,章节,记忆,提取', 1, 1, NULL, '2026-03-17 21:00:33', NULL, '2026-03-28 23:07:44');
INSERT INTO `ai_prompt_template` VALUES (15, 'llm_chapter_character_extract', '章节角色提取模板', 'llm', 'character_extract', 15, '从章节内容中提取角色信息的提示词模板', '小说,角色,提取', 1, 1, NULL, '2026-03-18 22:02:55', NULL, '2026-03-28 23:07:44');

-- ----------------------------
-- Table structure for ai_prompt_template_version
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_template_version`;
CREATE TABLE `ai_prompt_template_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `version_number` int NOT NULL COMMENT '版本号',
  `template_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板内容',
  `variable_definitions` json NULL COMMENT '变量定义JSON',
  `version_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本说明',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否激活',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_version`(`template_id` ASC, `version_number` ASC) USING BTREE,
  INDEX `idx_template_id`(`template_id` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI提示词模板版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_prompt_template_version
-- ----------------------------
INSERT INTO `ai_prompt_template_version` VALUES (1, 1, 1, '# 小说章节创作\r\n\r\n你是{role}，创作第{volumeNumber}卷第{chapterNumber}章《{chapterTitle}》。\r\n\r\n## 项目基础设定\r\n- 叙事结构：{narrativeStructure}\r\n- 写作风格：{writingStyle}\r\n- 写作视角：{writingPerspective}\r\n- 叙事节奏：{narrativePace}\r\n- 语言风格：{languageStyle}\r\n- 描述重点：{descriptionFocus}\r\n\r\n## 世界观设定（必须遵守）\r\n{worldview}\r\n\r\n## 分卷信息\r\n第{volumeNumber}卷《{volumeTitle}》- {volumeTheme}\r\n\r\n{volumeInfo}\r\n\r\n## 情节阶段\r\n{plotStage}\r\n\r\n## 前置章节回顾（严格遵循的人物和情节）\r\n{recentChapters}\r\n\r\n{knowledgeContext}\r\n\r\n## 已登场的人物信息\r\n{characterInfo}\r\n\r\n\r\n## 本章要求\r\n- 核心大纲：{chapterOutline}\r\n- 开篇起点：{startingScene}\r\n- 收尾终点：{endingScene}\r\n- 字数要求：1500字左右\r\n\r\n## 人物一致性要求（重要）\r\n根据前置章节回顾，本章必须：\r\n1. **保持现有人物**：已经出现的人物必须保持名字和身份一致\r\n2. **不要新增角色**：除非大纲明确提及，否则不要创造新角色\r\n3. **人物关系延续**：人物之间的关系状态必须与前章一致\r\n\r\n## 三幕式创作指导\r\n根据当前情节阶段「{plotStage}」，本章应侧重以下创作要点：\r\n\r\n**第一幕（铺垫）**：建立场景、引入人物、设置冲突种子\r\n**第二幕（发展）**：推进冲突、深化矛盾、角色成长\r\n**第三幕（高潮/收尾）**：冲突爆发、情节转折、情感释放\r\n\r\n请根据当前章节在整体故事中的位置，合理安排情节密度和节奏。\r\n\r\n## 创作要点\r\n\r\n1. **字数控制**：控制在1500字左右\r\n2. **场景衔接**：开篇自然承接上一章结尾\r\n3. **伏笔处理**：注意处理待回收伏笔\r\n4. **剧情推进**：严格按大纲推进\r\n5. **角色一致**：人物性格符合设定，人物身份与前章一致\r\n6. **人物连续性**：严格遵循前置章节回顾中的人物设定\r\n\r\n## 格式要求\r\n\r\n- 每段开头两个全角空格\r\n- 段落之间空一行\r\n- 对话独立成段\r\n- 只输出正文内容', '[{\"desc\": \"AI角色\", \"name\": \"role\", \"type\": \"string\", \"required\": true}, {\"desc\": \"目标字数\", \"name\": \"targetWordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"最大字数\", \"name\": \"maxWordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"最小字数\", \"name\": \"minWordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"卷号\", \"name\": \"volumeNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节号\", \"name\": \"chapterNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节标题\", \"name\": \"chapterTitle\", \"type\": \"string\", \"required\": true}, {\"desc\": \"卷标题\", \"name\": \"volumeTitle\", \"type\": \"string\", \"required\": true}, {\"desc\": \"卷主题\", \"name\": \"volumeTheme\", \"type\": \"string\", \"required\": true}, {\"desc\": \"情节阶段\", \"name\": \"plotStage\", \"type\": \"string\", \"required\": true}, {\"desc\": \"情节阶段描述\", \"name\": \"plotStageDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"叙事结构\", \"name\": \"narrativeStructure\", \"type\": \"string\", \"required\": false}, {\"desc\": \"结局类型\", \"name\": \"endingType\", \"type\": \"string\", \"required\": false}, {\"desc\": \"结局基调\", \"name\": \"endingTone\", \"type\": \"string\", \"required\": false}, {\"desc\": \"写作风格\", \"name\": \"writingStyle\", \"type\": \"string\", \"required\": false}, {\"desc\": \"写作视角\", \"name\": \"writingPerspective\", \"type\": \"string\", \"required\": false}, {\"desc\": \"叙事节奏\", \"name\": \"narrativePace\", \"type\": \"string\", \"required\": false}, {\"desc\": \"语言风格\", \"name\": \"languageStyle\", \"type\": \"string\", \"required\": false}, {\"desc\": \"描写重点\", \"name\": \"descriptionFocus\", \"type\": \"string\", \"required\": false}, {\"desc\": \"世界观设定\", \"name\": \"worldview\", \"type\": \"string\", \"required\": false}, {\"desc\": \"章节大纲\", \"name\": \"chapterOutline\", \"type\": \"string\", \"required\": true}, {\"desc\": \"起点场景\", \"name\": \"startingScene\", \"type\": \"string\", \"required\": true}, {\"desc\": \"终点场景\", \"name\": \"endingScene\", \"type\": \"string\", \"required\": true}, {\"desc\": \"上一章收尾场景\", \"name\": \"lastChapterEndingScene\", \"type\": \"string\", \"required\": false}, {\"desc\": \"前置章节\", \"name\": \"recentChapters\", \"type\": \"string\", \"required\": false}, {\"desc\": \"知识库检索上下文\", \"name\": \"knowledgeContext\", \"type\": \"string\", \"required\": false}]', '初始版本，从ChapterPromptBuilder迁移', 1, NULL, '2026-02-05 05:25:34');
INSERT INTO `ai_prompt_template_version` VALUES (2, 2, 1, '你是一位资深的网络小说作家和编辑，擅长创作引人入胜的故事。\n\n请根据以下信息，为这部小说创作一个完整的创作大纲：\n\n【故事背景】\n{projectDescription}\n\n【故事基调】{storyTone}\n【故事类型】{storyGenre}\n【目标篇幅】\n- 预计分卷数：{targetVolumeCount}卷\n- 每卷平均字数：{avgWordsPerVolume}万字\n- 预计总字数：{targetWordCount}万字\n\n{additionalRequirements}\n\n要求：\n1. 确保故事连贯性，各卷之间要有逻辑递进\n2. 每章的大纲要具体，避免空洞\n3. 合理分配关键冲突和情节高潮\n4. 章节目标要明确，符合故事发展需要\n5. 返回的必须是纯XML格式，不要有任何其他说明文字\n6. 所有标签都必须填写，不能有空值\n7. **章节起终点场景（重要）**：每章必须明确起点和终点场景，确保时间线不倒流\n   - 起点场景：描述章节开始时主角在哪里、什么时间、什么状态\n   - 终点场景：描述章节结束时主角在哪里、什么时间、什么状态\n   - 第N章的终点场景应该与第N+1章的起点场景保持一致\n\n请严格按照以下XML格式返回大纲（必须是纯XML，不要有任何其他文字）：\n\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<O>\n  <C>整体故事梗概（200-300字）</C>\n  <M>主要主题（一句话概括）</M>\n  <V>\n    <v>\n      <N>1</N>\n      <T>第一卷：XXX</T>\n      <Z>本卷的主旨和核心冲突</Z>\n      <F>本卷的主要矛盾冲突</F>\n      <P>本卷的情节走向概述</P>\n      <D>本卷的详细描述</D>\n      <E>本卷的关键事件，用分号分隔</E>\n      <W>100000</W>\n      <L>本卷的时间线设定</L>\n      <G>10</G>\n      <B>本卷的备注信息</B>\n      <O>本卷要达成的核心目标</O>\n      <H>本卷的高潮事件描述</H>\n      <R>本卷的收尾描述</R>\n      <S>1</S>\n      <X>10</X>\n      <A>[\"角色1\", \"角色2\"]</A>\n      <Y>[\"伏笔1\", \"伏笔2\"]</Y>\n    </v>\n  </V>\n  <H>\n    <c>\n      <N>1</N>\n      <V>1</V>\n      <T>章节标题</T>\n      <P>本章情节大纲（100-200字）</P>\n      <E>关键事件1；关键事件2；关键事件3</E>\n      <G>本章要达成的目标</G>\n      <W>3000</W>\n      <S>章节起点场景（地点、时间、状态）</S>\n      <X>章节终点场景（地点、时间、状态）</X>\n    </c>\n  </H>\n</O>\n\n标签说明：\n- O: Outline（大纲）\n- C: Concept（整体概念）\n- M: Main Theme（主要主题）\n- V: Volumes（分卷列表）\n- v: volume（单个分卷）\n- N: Number（编号）\n- T: Title（标题）\n- Z: 主题（Theme）\n- F: 冲突（Conflict）\n- P: 情节（Plot）\n- D: 描述（Description）\n- E: 事件（Events）\n- W: 字数（Word Count）\n- L: 时间线（Timeline）\n- G: 目标数量/目标（Goal/Count）\n- B: 备注（Notes）\n- O: 核心目标（Objective）\n- H: 高潮（Climax）\n- R: 结尾/收尾（Resolution）\n- S: 起点（Start）/场景（Scene）\n- X: 终点/结束（End）\n- A: 人物（Actors/Characters）\n- Y: 伏笔（Yield/Foreshadowing）\n- H: 章（Chapters）\n- c: chapter（单个章节）', '[{\"desc\": \"故事背景\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"预计分卷数\", \"name\": \"targetVolumeCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"每卷平均字数\", \"name\": \"avgWordsPerVolume\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"预计总字数\", \"name\": \"targetWordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"额外要求\", \"name\": \"additionalRequirements\", \"type\": \"string\", \"required\": false}]', '初始版本，从OutlineService迁移', 1, NULL, '2026-02-05 05:25:54');
INSERT INTO `ai_prompt_template_version` VALUES (3, 3, 1, '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。\n\n根据以下项目描述，为这部小说创建完整的世界观设定：\n\n【项目描述】\n{projectDescription}\n\n【故事类型】{storyGenre}\n【故事基调】{storyTone}\n\n请创建包含以下内容的世界观设定：\n1. 世界类型（现实/奇幻/科幻/修真等）\n2. 力量体系（魔法、修真、科技等级等）\n3. 地理环境（大陆、国家、重要地点）\n4. 势力分布（阵营、组织、重要势力）\n5. 核心设定（与故事相关的特殊规则）\n\n请以XML格式返回：', '[{\"desc\": \"项目描述\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}]', '初始版本，从WorldviewTaskStrategy迁移', 1, NULL, '2026-02-05 05:25:54');
INSERT INTO `ai_prompt_template_version` VALUES (4, 4, 1, '请根据用户的创作想法，生成一个网络小说项目。\r\n\r\n【用户想法】\r\n{idea}\r\n\r\n【生成要求】\r\n1. 项目名称：简洁、吸引人、能体现故事核心，不超过20个字\r\n2. 项目描述：详细描述故事背景、主要看点、创作方向，100-200字\r\n3. 主角姓名不要大众化，要符合当前最流行姓名的命名方式来取名\r\n\r\n【输出格式】\r\n请以纯JSON格式返回（不要使用markdown代码块，直接输出JSON）：\r\n{\r\n  \"name\": \"项目名称\",\r\n  \"description\": \"项目描述\"\r\n}', '[{\"desc\": \"用户创作想法\", \"name\": \"idea\", \"type\": \"string\", \"required\": true}]', '初始版本，从AIController.buildProjectGenerateTask迁移', 1, NULL, '2026-02-05 05:48:34');
INSERT INTO `ai_prompt_template_version` VALUES (5, 5, 1, '你是一位资深的网络小说作家和编辑，精通各类叙事结构和分卷规划技巧。\n\n## 核心创作要求\n\n请严格遵循以下创作设置为小说规划第{volumeNumber}卷：\n\n### 叙事与写作设置\n{narrativeSettings}\n\n### 结局规划\n{endingSettings}\n\n---\n\n## 小说基础信息\n\n【故事背景】\n{projectDescription}\n\n【故事基调】{storyTone}\n【故事类型】{storyGenre}\n\n---\n\n## 世界观设定\n\n{worldviewInfo}\n\n---\n\n{previousVolumeDesc}\n\n---\n\n## 单卷生成说明\n\n请为小说生成**第{volumeNumber}卷**的完整分卷规划。\n\n---\n\n## XML输出格式\n\n【重要】请严格按照以下XML格式返回数据（使用单字母标签节省token）：\n<V>\n  <v>\n    <N>{volumeNumber}</N>\n    <T><![CDATA[第{volumeNumber}卷：分卷标题]]></T>\n    <Z><![CDATA[本卷主旨（100-200字，描述本卷的核心主题和要表达的内容）]]></Z>\n    <F><![CDATA[主要冲突（100-200字，描述本卷的主要矛盾和冲突点）]]></F>\n    <P><![CDATA[情节走向（100-200字，描述本卷情节发展的整体脉络）]]></P>\n    <O><![CDATA[核心目标（100字以内，本卷要达成的核心目标）]]></O>\n    <E>\n      <opening>\n        <item><![CDATA[开篇事件1]]></item>\n        <item><![CDATA[开篇事件2]]></item>\n      </opening>\n      <development>\n        <item><![CDATA[发展事件1]]></item>\n        <item><![CDATA[发展事件2]]></item>\n        <item><![CDATA[发展事件3]]></item>\n      </development>\n      <turning>\n        <item><![CDATA[转折事件1]]></item>\n        <item><![CDATA[转折事件2]]></item>\n      </turning>\n      <climax>\n        <item><![CDATA[高潮事件1]]></item>\n        <item><![CDATA[高潮事件2]]></item>\n        <item><![CDATA[高潮事件3]]></item>\n      </climax>\n      <ending>\n        <item><![CDATA[收尾事件1]]></item>\n        <item><![CDATA[收尾事件2]]></item>\n      </ending>\n    </E>\n    <H><![CDATA[高潮事件（100-200字，本卷的高潮场景描述）]]></H>\n    <R><![CDATA[收尾描述（100字以内，本卷的收尾方式）]]></R>\n    <D><![CDATA[分卷描述（200-300字，对本卷内容的综合描述）]]></D>\n    <W>100000</W>\n    <L><![CDATA[时间线设定（本卷故事发生的时间跨度、重要时间节点）]]></L>\n    <G>10</G>\n    <B><![CDATA[分卷备注（创作注意事项、需要特别注意的点）]]></B>\n    <A><![CDATA[[\"角色1\", \"角色2\", \"角色3\"]]]></A>\n    <Y><![CDATA[[\"伏笔1：描述内容\", \"伏笔2：描述内容\"]]></Y>\n  </v>\n</V>\n\n### 标签说明\n- V: Volumes（分卷列表）\n- v: volume（单个分卷）\n- N: Number（卷号）\n- T: Title（标题，格式：第X卷：卷名，使用中文数字如第一卷、第二卷）\n- Z: 主题（Theme，本卷探讨的核心主题）\n- F: 矛盾（Conflict，本卷的主要矛盾冲突）\n- P: 情节（Plot，本卷整体情节走向）\n- O: 核心目标（Objective，主角本卷目标）\n- E: 事件（Events，5阶段XML子节点格式）\n  - opening: 开篇阶段事件（2-3个）\n  - development: 发展阶段事件（2-4个）\n  - turning: 转折阶段事件（2-3个）\n  - climax: 高潮阶段事件（2-4个）\n  - ending: 收尾阶段事件（2-3个）\n- H: 高潮（Climax，本卷高潮事件描述）\n- R: 收尾（Resolution，收尾与钩子）\n- D: 描述（Description，本卷简介200-300字）\n- W: 字数（Word Count，预估字数）\n- L: 时间线（Timeline，本卷时间跨度）\n- G: 目标章节数（Goal/Count）\n- B: 备注（Notes，特殊说明）\n- A: 人物（Actors/Characters，必须是标准JSON数组格式）\n- Y: 伏笔（Yield/Foreshadowing，必须是标准JSON数组格式）\n\n### 格式要求\n1. 直接返回纯XML格式，不要使用markdown代码块标记（不要使用```xml和```）\n2. 不要包含任何解释文字、注释或说明\n3. 只返回XML数据本身\n4. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n5. E标签使用5阶段XML子节点格式，每个阶段包含2-4个item子节点\n6. D标签的本卷简介需达到200-300字，包含本卷的起承转合概述\n7. 【重要】A标签和Y标签必须是标准JSON数组格式，用方括号包裹\n   例如：<![CDATA[[\"张三\", \"李四\", \"王五\"]]]>\n\n现在请返回第{volumeNumber}卷的完整XML数据（纯XML，不要markdown标记）：', '[{\"desc\": \"生成模式说明\", \"name\": \"generationMode\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事背景\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"叙事与写作设置（叙事结构、视角、节奏、语言风格、描写重点）\", \"name\": \"narrativeSettings\", \"type\": \"string\", \"required\": false}, {\"desc\": \"结局规划（结局类型、结局基调）\", \"name\": \"endingSettings\", \"type\": \"string\", \"required\": false}, {\"desc\": \"世界观设定信息\", \"name\": \"worldviewInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"分卷规划要求\", \"name\": \"volumeInfo\", \"type\": \"string\", \"required\": true}, {\"desc\": \"已有分卷概要\", \"name\": \"existingVolumesInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"叙事结构指导\", \"name\": \"structureGuide\", \"type\": \"string\", \"required\": false}, {\"desc\": \"起始卷号\", \"name\": \"volumeNumber\", \"type\": \"integer\", \"required\": false}, {\"desc\": \"目标分卷数\", \"name\": \"targetVolumeCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"连贯性说明\", \"name\": \"continuityNote\", \"type\": \"string\", \"required\": false}]', '修复A和Y标签的JSON数组格式要求', 1, NULL, '2026-02-06 14:21:38');
INSERT INTO `ai_prompt_template_version` VALUES (6, 6, 1, '你是一位资深的网络小说作家和编辑，精通章节节奏设计和叙事技巧。\n\n请为第{volumeNumber}卷规划详细章节大纲。\n\n## 核心创作要求\n\n请严格遵循以下创作设置进行章节规划：\n\n### 叙事与写作设置\n{narrativeSettings}\n\n---\n\n## 小说基础信息\n\n【故事背景】\n{projectDescription}\n\n【故事基调】{storyTone}\n【故事类型】{storyGenre}\n\n---\n\n## 分卷信息\n{volumeInfo}\n\n---\n\n## 世界观设定\n{worldviewInfo}\n\n---\n\n## 登场角色\n{characterInfo}\n\n---\n\n## 章节规划要求\n\n【预计章节数】{chapterCount}章\n【每章字数】{wordCount}字左右\n\n{paceGuide}\n\n---\n\n## 专业网文结构设计 - 必须遵循\n\n**1. 三明一暗伏笔架构**\n- 明线1（主线）：主角成长、升级、变强\n- 明线2（情感线）：友情、爱情、师徒情发展\n- 明线3（任务线）：寻宝、探秘、解决事件\n- 暗线（核心秘密）：贯穿全卷甚至全书的最大悬念\n\n**2. 章节节奏设计（每3章一个小循环）**\n- 第1章（铺垫章）：铺设场景、埋下伏笔、制造期待\n- 第2章（升级章）：冲突升级、遇到阻碍、紧张感增强\n- 第3章（高潮章）：小高潮爆发、问题部分解决、但留下更大悬念\n\n**3. 关键事件设计原则（极重要！）**\n- 每章最多1-2个关键事件，强烈建议只有1个核心关键事件\n- 关键事件过多样会导致后续章节生成时内容分散、情节展开不够充分、字数不足\n- 关键事件应该是本章最核心、最推动剧情发展的那件事\n- 细节描写、日常对话、场景转换、配角小动作等不属于关键事件\n- 示例分析：\n  ✅ 正确：\"主角首次进入秘境探索\"（1个，核心明确，适合后续展开）\n  ❌ 错误：\"主角遇到守门人；守门人检查身份；主角进入秘境；发现一片森林\"（4个，过于细碎，后续无法充分展开）\n- 记住：关键事件是骨架，细节描写要留白给后续章节生成时填充\n\n**4. 悬念设置要求**\n- 每章结尾必须设置钩子（让读者想读下一章）\n- 每3章一个小高潮，满足读者期待\n- 每章揭示一个新信息，但引出更多问题\n- 长期悬念要逐步透露线索，保持神秘感\n\n**5. 伏笔埋设原则**\n- 短期伏笔（1-3章回收）：小道具、小线索、人物小动作\n- 中期伏笔（5-10章回收）：次要角色的秘密、次要物品的来历\n- 长期伏笔（本卷或全书回收）：核心秘密、主角身世、世界真相\n- 回收伏笔时要有情感冲击，让读者有\"原来如此\"的感觉\n\n**6. 章节起终点设计（重要！确保剧情连贯）**\n- 每章必须有明确的起点场景和终点场景\n- **起点场景**：描述章节开始时主角在哪里、什么时间、什么状态\n- **终点场景**：描述章节结束时主角在哪里、什么时间、什么状态\n- **章节衔接**：第N章的终点场景 = 第N+1章的起点场景\n- **防止时间倒流**：明确标注场景状态，避免剧情重复或倒退\n\n---\n\n## XML输出格式\n\n【重要】请严格按照以下XML格式返回数据（使用小写单字母标签）：\n<c>\n  <o>\n    <n>1</n>\n    <v>{volumeNumber}</v>\n    <t><![CDATA[章节标题]]></t>\n    <p><![CDATA[本章情节大纲（100-150字）]]></p>\n    <e><![CDATA[本章核心关键事件（最多1-2个，建议1个）]]></e>\n    <g><![CDATA[本章目标：主角在本章要达成什么]]></g>\n    <w>{wordCount}</w>\n    <s><![CDATA[章节起点场景（地点、时间、状态）]]></s>\n    <f><![CDATA[章节终点场景（地点、时间、状态）]]></f>\n  </o>\n</c>\n\n### 标签说明\n- c: chapters（章节列表根元素）\n- o: chapter（单个章节，one chapter）\n- n: number（章节序号）\n- v: volume（所属卷号）\n- t: title（章节标题）\n- p: plot（情节大纲，100-150字）\n- e: events（关键事件，最多1-2个）\n- g: goal（章节目标）\n- w: words（目标字数）\n- s: start（起点场景）\n- f: finish（终点场景）\n\n### 格式要求\n1. 直接返回纯XML格式，不要使用markdown代码块标记（不要使用```xml和```）\n2. 不要包含任何解释文字、注释或说明\n3. 只返回XML数据本身\n4. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n5. p标签的情节大纲需达到100-150字，概述本章的主要内容和走向\n6. s和f标签的场景描述要具体，包含地点、时间、人物状态\n\n现在请返回{chapterCount}个章节的完整XML数据（纯XML，不要markdown标记）：', '[{\"desc\": \"卷号\", \"name\": \"volumeNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"故事背景\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"叙事与写作设置\", \"name\": \"narrativeSettings\", \"type\": \"string\", \"required\": false}, {\"desc\": \"分卷信息\", \"name\": \"volumeInfo\", \"type\": \"string\", \"required\": true}, {\"desc\": \"世界观设定\", \"name\": \"worldviewInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"登场角色\", \"name\": \"characterInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"章节数\", \"name\": \"chapterCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"每章字数\", \"name\": \"wordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节节奏指导\", \"name\": \"paceGuide\", \"type\": \"string\", \"required\": false}]', '增加基础设置变量、完善世界观信息、增加章节节奏指导', 1, NULL, '2026-02-06 14:22:18');
INSERT INTO `ai_prompt_template_version` VALUES (7, 7, 1, '你是一位资深的网络小说作家和角色设计师。\n\n请为第{volumeNumber}卷设计角色信息：\n\n【故事背景】\n{projectDescription}\n\n【故事基调】{storyTone}\n【故事类型】{storyGenre}\n\n{worldviewInfo}\n\n【分卷信息】\n{volumeInfo}\n\n{chapterInfo}\n\n{existingCharactersSummary}\n\n【重要提示】\n1. 只生成本卷新出现的角色\n2. 已有角色不需要重新生成\n3. 主角和重要配角在第一卷生成，后续卷只生成新角色\n4. 角色数量控制在3-8个（包括主角、配角、反派等）\n\n【重要】请严格按照以下XML格式返回角色数据（使用简化标签节省token）：\n<c>\n  <o>\n    <n>角色名称</n>\n    <g>male/female/other</g>\n    <a>年龄数字</a>\n    <t>protagonist/supporting/antagonist/npc</t>\n    <d><![CDATA[角色定位（如：主角、男二号、反派BOSS等）]]></d>\n    <p><![CDATA[性格特点（如：勇敢、智慧、冷酷等）]]></p>\n    <e><![CDATA[外貌描述（中文自然语言）]]></e>\n    <q><![CDATA[AI图像生成描述词（专业3D动漫风格，英文，逗号分隔）]]></q>\n    <b><![CDATA[背景故事]]></b>\n    <l><![CDATA[能力或技能描述]]></l>\n    <s><![CDATA[标签，用逗号分隔（如：热血,智慧,正义）]]></s>\n  </o>\n</c>\n\n【XML格式要求】\n1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>\n2. 不要包含markdown代码块标记（```xml），直接返回XML\n3. 不要包含任何解释或说明文字，只返回XML数据\n4. 必须包含至少一个主角角色\n\n现在请返回本卷的角色XML数据：', '[{\"desc\": \"卷号\", \"name\": \"volumeNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"故事背景\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"世界观设定\", \"name\": \"worldviewInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"分卷信息\", \"name\": \"volumeInfo\", \"type\": \"string\", \"required\": true}, {\"desc\": \"章节概要\", \"name\": \"chapterInfo\", \"type\": \"string\", \"required\": false}, {\"desc\": \"已有角色概要\", \"name\": \"existingCharactersSummary\", \"type\": \"string\", \"required\": false}]', '初始版本，从OutlineTaskStrategy.buildCharacterPrompt迁移', 1, NULL, '2026-02-06 14:22:45');
INSERT INTO `ai_prompt_template_version` VALUES (8, 8, 1, '# 章节内容压缩任务\n\n你是资深网文编辑，擅长压缩文章内容而不损失核心剧情。\n\n## 任务说明\n\n对提供的章节内容进行压缩，将字数控制在2500字左右。\n\n## 压缩原则\n\n1. **保持核心剧情完整**：\n   - 保留所有关键情节转折\n   - 保留所有重要对话\n   - 保留所有人物互动和冲突\n   - 保留情节推进的核心节奏\n\n2. **删除冗余内容**：\n   - 删除过于冗长的环境描写\n   - 删除重复的心理描写\n   - 删除不必要的外貌和服装细节\n   - 删除无关紧要的动作细节\n   - 删除啰嗦的过渡段落\n\n3. **精简表达方式**：\n   - 将冗长的对话改为简洁表达\n   - 合并相似的段落\n   - 用概括性语言替代细节描写\n   - 删除过多的修饰语和形容词\n\n4. **保持原文格式**：\n   - 保持段落分隔（每段开头两个全角空格）\n   - 保持对话独立成段\n   - 保持引号使用规范\n\n## 字数要求\n\n- 目标字数：2400-2600字\n- 必须严格遵守，不得偏离此范围\n\n## 输出要求\n\n1. 只输出压缩后的正文内容\n2. 不包含标题、说明或标记\n3. 确保压缩后内容连贯、流畅、可读\n\n## 待压缩内容\n\n{chapterContent}', '[{\"desc\": \"待压缩的章节内容\", \"name\": \"chapterContent\", \"type\": \"string\", \"required\": true}]', '初始版本：章节内容压缩模板', 1, NULL, '2026-02-11 12:23:48');
INSERT INTO `ai_prompt_template_version` VALUES (9, 9, 1, '你是一位资深的网文角色设计师，擅长塑造深入人心、富有魅力的主角形象。\n\n根据以下世界观和故事设定，为这部小说创作一个引人入胜的主角：\n\n【世界观设定】\n{worldview}\n\n【故事类型】{storyGenre}\n【故事基调】{storyTone}\n\n【项目描述】\n{projectDescription}\n\n【角色要求】\n1. 主角姓名要符合世界观设定（修仙/玄幻/都市/科幻等风格）\n2. 性格要有层次感，既有优点也有缺点，便于成长\n3. 外貌描写要生动，便于后续AI生图\n4. 背景故事要与世界观紧密关联，为后续剧情埋下伏笔\n5. 能力/技能设定要符合力量体系，且有成长空间\n\n【重要】请严格按照以下XML格式返回主角设定（使用简化标签节省token）：\n\n<P>\n  <N><![CDATA[主角姓名]]></N>\n  <G><![CDATA[性别（male/female/other）]]></G>\n  <A><![CDATA[年龄（数字）]]></A>\n  <Pe><![CDATA[性格特点（多维度描述，如：坚韧、善良、有些冲动）]]></Pe>\n  <Ap><![CDATA[外貌描述（详细描写，便于AI生图，包括五官、身材、着装风格）]]></Ap>\n  <B><![CDATA[背景故事（100-200字，与世界观关联，包含身世、经历、动机等）]]></B>\n  <Ab><![CDATA[能力/技能（符合力量体系的初始能力，描述成长潜力）]]></Ab>\n  <T><![CDATA[标签（用逗号分隔，如：废柴流,热血,扮猪吃虎）]]></T>\n</P>\n\n【XML格式要求】\n1. 必须使用单字母标签（P=Protagonist, N=Name, G=Gender, A=Age, Pe=Personality, Ap=Appearance, B=Background, Ab=Abilities, T=Tags）\n2. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹\n3. 不要包含markdown代码块标记（```xml），直接返回XML\n4. 不要包含任何解释或说明文字，只返回XML数据', '[{\"desc\": \"世界观设定\", \"name\": \"worldview\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事类型\", \"name\": \"storyGenre\", \"type\": \"string\", \"required\": true}, {\"desc\": \"故事基调\", \"name\": \"storyTone\", \"type\": \"string\", \"required\": true}, {\"desc\": \"项目描述\", \"name\": \"projectDescription\", \"type\": \"string\", \"required\": true}]', '初始版本，支持世界观生成后自动创建主角', 1, NULL, '2026-02-12 14:03:04');
INSERT INTO `ai_prompt_template_version` VALUES (10, 10, 1, '你是一位专业的人物分析专家，擅长从文本中识别和提取人物信息。\r\n\r\n请分析以下章节内容，提取所有出现的人物信息。\r\n\r\n【章节号】\r\n{chapterNumber}\r\n【章节内容】\r\n{chapterContent}\r\n\r\n【任务要求】\r\n1. 仔细阅读章节内容，识别所有出现的人物\r\n2. 对于每个人物，提取以下信息：\r\n   - 姓名（如果未明确给出名字，可以根据特征描述，如\"白发老者\"、\"黑衣女子\"）\r\n   - 首次出场描述（外貌、动作、出场方式等）\r\n   - 性格特征（从对话、行为中推断）\r\n   - 对话风格（如果章节中有对话）\r\n   - 角色分类：主角、配角、反派或者NPC （protagonist/supporting/antagonist/npc）\r\n3. 跳过第一人称代词（\"我\"、\"主角\"），仅提取有具体名称或特征的人物\r\n\r\n【重要】请严格按照以下XML格式返回人物信息（使用简化标签节省token）：\r\n\r\n<E>\r\n  <C>\r\n    <N><![CDATA[人物姓名或特征描述]]></N>\r\n    <D><![CDATA[首次出场描述（外貌、动作、出场方式等）]]></D>\r\n    <Pe><![CDATA[性格特征（从对话、行为推断）]]></Pe>\r\n    <S><![CDATA[对话风格（语气、用词特点等）]]></S>\r\n  </C>\r\n  <C>\r\n    <N><![CDATA[第二个人物...]]></N>\r\n    ...\r\n  </C>\r\n</E>\r\n\r\n【XML格式要求】\r\n1. 必须使用单字母标签（E=Entities, C=Character, N=Name, D=Description, Pe=Personality, S=SpeechStyle）\r\n2. 对于长文本内容，请使用CDATA标签包裹\r\n3. 不要包含markdown代码块标记（```xml），直接返回XML\r\n4. 如果章节中没有新人物，返回空的<E></E>标签\r\n5. 不要包含任何解释或说明文字，只返回XML数据', '[{\"desc\": \"章节号\", \"name\": \"chapterNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节内容\", \"name\": \"chapterContent\", \"type\": \"string\", \"required\": true}, {\"desc\": \"已有人物列表\", \"name\": \"existingCharacters\", \"type\": \"string\", \"required\": false}]', '初始版本，支持从章节内容提取人物信息', 1, NULL, '2026-02-12 14:34:12');
INSERT INTO `ai_prompt_template_version` VALUES (11, 11, 1, '# 章节问题检查任务\n\n## 你的角色\n你是一位资深网络小说编辑，擅长发现剧情问题。\n\n**【重要】你的任务：只检查问题，不修改内容！**\n\n## 世界观设定（必须遵守的规则）\n{worldview}\n\n## 前一章结构化信息\n{prevChapterMemory}\n\n## 本章涉及人物档案\n{characterProfiles}\n\n## 待回收伏笔列表\n{pendingForeshadowing}\n\n## 需要检查的章节内容\n\n{content}\n\n## 检查维度\n\n请从以下维度进行全面检查：\n\n### 1. 剧情逻辑（T=logic）\n- 情节发展是否合理，因果关系是否清晰\n- 人物行为是否有足够的动机支撑\n- 事件发展是否符合基本逻辑\n\n### 2. 连贯性（T=continuity）\n- 与前一章的衔接是否自然\n- 是否与前一章结尾场景呼应\n- 时间线是否连贯\n\n### 3. 人物一致性（T=character）\n- 人物行为是否符合其性格设定（参考人物档案）\n- 人物能力表现是否与设定一致\n- 人物关系发展是否合理\n\n### 4. 世界观一致性（T=worldview）\n- 是否符合世界观设定（魔法体系、科技水平、社会规则等）\n- 是否出现与设定矛盾的内容\n\n### 5. 时间线（T=timeline）\n- 事件发生的时间顺序是否合理\n- 时间跨度描述是否一致\n\n### 6. 伏笔管理（T=foreshadow）\n- 伏笔埋设是否合理\n- 是否有应该回收但未回收的伏笔\n- 新埋设的伏笔是否有价值\n\n### 7. 重复内容（T=repetition）\n- 是否有重复的描写\n- 是否有重复的对话\n- 是否有冗余的表达\n\n### 8. 设定矛盾（T=setting）\n- 物品、功法、地名等设定是否前后一致\n- 是否与前文描述冲突\n\n## 输出格式（必须是XML格式，使用单字母标签）\n\n**【XML格式要求】**：\n1. 必须使用XML格式，不能使用JSON格式\n2. 根节点必须是<R>（Result）\n3. 使用CDATA包裹内容，避免特殊字符转义问题\n4. 标签必须是单字母，减少token消耗\n\nXML结构如下：\n```\n<R>\n  <I>\n    <T>问题类型代码（logic/continuity/character/worldview/timeline/foreshadow/repetition/setting）</T>\n    <S>严重程度（high/medium/low）</S>\n    <L><![CDATA[位置描述（如：第3段、对话中等）]]></L>\n    <O><![CDATA[原文片段（50字以内）]]></O>\n    <D><![CDATA[问题描述]]></D>\n    <G><![CDATA[修改建议]]></G>\n  </I>\n  <SUM><![CDATA[检查总结（100字以内）]]></SUM>\n  <N>问题总数</N>\n  <H>true或false（是否存在严重问题）</H>\n</R>\n```\n\n**【重要】**：\n- 如果没有发现问题，<I>标签可以省略，直接返回空的检查结果\n- 直接返回纯XML字符串，不要包含markdown标记\n- 问题按严重程度排序，high优先', '[{\"desc\": \"需要检查的章节内容\", \"name\": \"content\", \"type\": \"string\", \"required\": true}, {\"desc\": \"世界观设定\", \"name\": \"worldview\", \"type\": \"string\", \"required\": false}, {\"desc\": \"前一章结构化信息\", \"name\": \"prevChapterMemory\", \"type\": \"string\", \"required\": false}, {\"desc\": \"本章涉及人物档案\", \"name\": \"characterProfiles\", \"type\": \"string\", \"required\": false}, {\"desc\": \"待回收伏笔列表\", \"name\": \"pendingForeshadowing\", \"type\": \"string\", \"required\": false}]', '初始版本，包含8个检查维度', 1, 1, '2026-03-03 12:53:03');
INSERT INTO `ai_prompt_template_version` VALUES (12, 12, 1, '# 章节修复任务\n\n## 你的角色\n你是一位资深网络小说编辑，擅长修复剧情问题。\n\n**【重要】**：根据下面的问题清单，精准修复章节内容。\n\n## 原始章节内容\n\n{content}\n\n## 发现的问题清单\n\n{checkResult}\n\n## 世界观设定（修复时必须遵守）\n{worldview}\n\n## 本章涉及人物档案（修复时参考）\n{characterProfiles}\n\n## 修复要求\n\n1. **精准修复**：只修复问题清单中列出的问题，不要改动其他内容\n2. **保持主线**：保持原有情节主线不变\n3. **自然衔接**：确保修复后与前后文衔接自然\n4. **风格一致**：保持原文的写作风格和语言特色\n5. **人物一致**：修复后的人物行为必须符合人物档案设定\n6. **设定遵守**：修复内容必须符合世界观设定\n\n## 输出格式（必须是XML格式，使用单字母标签）\n\n**【XML格式要求】**：\n1. 必须使用XML格式，不能使用JSON格式\n2. 根节点必须是<F>（Fix）\n3. 使用CDATA包裹内容，避免特殊字符转义问题\n4. 标签必须是单字母，减少token消耗\n\nXML结构如下：\n```\n<F>\n  <C><![CDATA[修改后的完整章节内容]]></C>\n  <SUM><![CDATA[修复说明摘要（100字以内）]]></SUM>\n  <N>修复数量</N>\n  <I>\n    <T>问题类型代码</T>\n    <S>严重程度</S>\n    <O><![CDATA[修改前的原文片段（50字以内）]]></O>\n    <V><![CDATA[修改后的原文片段（50字以内）]]></V>\n    <E><![CDATA[修改的原因和说明]]></E>\n  </I>\n</F>\n```\n\n**【重要】**：\n- <C>标签中必须是完整的修复后章节内容，不能省略任何部分\n- <O>和<V>必须是实际的文本片段，不是描述\n- 直接返回纯XML字符串，不要包含markdown标记', '[{\"desc\": \"需要修复的章节内容\", \"name\": \"content\", \"type\": \"string\", \"required\": true}, {\"desc\": \"问题检查结果\", \"name\": \"checkResult\", \"type\": \"string\", \"required\": true}, {\"desc\": \"世界观设定\", \"name\": \"worldview\", \"type\": \"string\", \"required\": false}, {\"desc\": \"本章涉及人物档案\", \"name\": \"characterProfiles\", \"type\": \"string\", \"required\": false}]', '初始版本，支持精准修复和完整报告', 1, 1, '2026-03-03 12:56:10');
INSERT INTO `ai_prompt_template_version` VALUES (13, 13, 1, '你是一位资深的网络小说作家和编辑，擅长构建精彩的分卷剧情。\n\n请根据以下信息，优化第一卷的分卷详情：\n\n【故事背景】\n{projectDescription}\n\n【故事基调】{storyTone}\n【故事类型】{storyGenre}\n\n{worldviewInfo}\n\n{previousVolumesInfo}\n\n【当前分卷基础信息】\n- 卷号：第一卷\n- 预计章节数：{targetChapterCount}章\n\n【严格要求】\n1. 分卷标题<T>必须使用中文卷号，格式为：第X卷：具体标题（如：第一卷：异乡的星火）\n2. 关键事件<E>使用XML子节点格式，包含5个阶段（开篇opening、发展development、转折turning、高潮climax、收尾ending），每个阶段包含2-4个事件\n3. 请严格按照以下XML格式返回数据（必须是纯XML，不要有任何其他文字）\n\n<V>\n  <v>\n    <N>1</N>\n    <T><![CDATA[第一卷：分卷标题]]></T>\n    <Z><![CDATA[本卷主旨（100-200字，描述本卷的核心主题和要表达的内容）]]></Z>\n    <F><![CDATA[主要冲突（100-200字，描述本卷的主要矛盾和冲突点）]]></F>\n    <P><![CDATA[情节走向（100-200字，描述本卷情节发展的整体脉络）]]></P>\n    <O><![CDATA[核心目标（100字以内，本卷要达成的核心目标）]]></O>\n    <E>\n      <opening>\n        <item><![CDATA[开篇事件1]]></item>\n        <item><![CDATA[开篇事件2]]></item>\n      </opening>\n      <development>\n        <item><![CDATA[发展事件1]]></item>\n        <item><![CDATA[发展事件2]]></item>\n        <item><![CDATA[发展事件3]]></item>\n      </development>\n      <turning>\n        <item><![CDATA[转折事件1]]></item>\n        <item><![CDATA[转折事件2]]></item>\n      </turning>\n      <climax>\n        <item><![CDATA[高潮事件1]]></item>\n        <item><![CDATA[高潮事件2]]></item>\n        <item><![CDATA[高潮事件3]]></item>\n      </climax>\n      <ending>\n        <item><![CDATA[收尾事件1]]></item>\n        <item><![CDATA[收尾事件2]]></item>\n      </ending>\n    </E>\n    <H><![CDATA[高潮事件（100-200字，本卷的高潮场景描述）]]></H>\n    <R><![CDATA[收尾描述（100字以内，本卷的收尾方式）]]></R>\n    <D><![CDATA[分卷描述（200-300字，对本卷内容的综合描述）]]></D>\n    <L><![CDATA[时间线设定（本卷故事发生的时间跨度、重要时间节点）]]></L>\n    <B><![CDATA[分卷备注（创作注意事项、需要特别注意的点）]]></B>\n    <G>{targetChapterCount}</G>\n  </v>\n</V>', NULL, '更新关键事件为XML子节点格式，避免JSON转义问题', 1, 4, '2026-03-16 13:47:01');
INSERT INTO `ai_prompt_template_version` VALUES (14, 14, 1, '# 网文章节剧情记忆提取\r\n从下述章节内容提取核心信息，用于后续剧情连贯承接，严格按要求输出，贴合网文创作逻辑，无冗余。\r\n\r\n**章节信息**：\r\n- 第{chapterNumber}章：{chapterTitle}\r\n- 约{wordCount}字\r\n- 概要：{plotSummary}\r\n\r\n**章节完整内容**：\r\n{chapterContent}\r\n\r\n### 核心锚点\r\nchapterEndingScene必须锚定结尾**最后一行**，精准描述主角即时具体状态，核对结尾内容确保为真实收尾。\r\n\r\n## 提取要求（纯XML输出，标签用单字母）\r\n<M>\r\n  <SUM>本章内容总结（300-500字，涵盖主要情节、角色成长、世界观拓展等）</SUM>\r\n  <E>核心事件1</E>\r\n  <E>核心事件2</E>\r\n  <E>核心事件3</E>\r\n  <T>本章首次出现的重要设定/能力/物品/势力</T>\r\n  <P>本章新埋伏笔</P>\r\n  <R>本章回收的前文伏笔</R>\r\n  <L>地点+时间+自身状态+正在进行的动作</L>\r\n  <U>结尾即时核心悬念/未解决问题</U>\r\n</M>\r\n\r\n## 字段细则\r\n1. SUM（chapterSummary）：**章节内容总结**，300-500字，涵盖：\r\n   - 本章主要情节发展\r\n   - 主角及重要角色的成长/变化\r\n   - 世界观/设定的新拓展\r\n   - 对整体故事的影响\r\n   - 用于后续章节回顾参考\r\n2. E（keyEvents）：3-5个**主线核心事件**，按剧情顺序排列，每个事件独立一个<E>标签；\r\n3. T（newSettings）：非首次出现的设定不录入；\r\n4. L（chapterEndingScene，重中之重，下章直接承接）：**必须包含四大要素（地点+时间+自身状态+正在进行的动作）**，表述具象无模糊；\r\n5. P（foreshadowingPlanted）：无新伏笔填\"无明显新伏笔\"；\r\n6. R（foreshadowingResolved）：对标前文，无回收填\"无回收伏笔\"；\r\n7. U（currentSuspense）：无明显悬念填\"待续\"。\r\n\r\n**重要**：标签严格区分，不要混淆：\r\n- SUM用于章节总结（放在最前面）\r\n- E用于核心事件（多个）\r\n- T用于新设定（在所有C标签之后）\r\n- L用于结尾场景\r\n- U用于悬念\r\n\r\n### 输出格式硬性要求\r\n1. 仅返回XML字符串，标签用单字母，无任何额外内容（无代码块/注释）；\r\n2. 所有标签必填，无空值、无省略；\r\n3. chapterEndingScene/currentSuspense为核心字段，精准提取，不确定则按结尾原文合理推断；\r\n4. 表述为网文简洁风格，无书面化冗余词汇；', '[{\"desc\": \"章节号\", \"name\": \"chapterNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节标题\", \"name\": \"chapterTitle\", \"type\": \"string\", \"required\": true}, {\"desc\": \"字数\", \"name\": \"wordCount\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"剧情概要\", \"name\": \"plotSummary\", \"type\": \"string\", \"required\": false}, {\"desc\": \"章节完整内容\", \"name\": \"chapterContent\", \"type\": \"string\", \"required\": true}]', '初始版本，从ChapterService硬编码迁移', 1, NULL, '2026-03-17 13:01:18');
INSERT INTO `ai_prompt_template_version` VALUES (15, 15, 1, '# 角色提取任务\r\n\r\n你是一位专业的小说角色分析师。请从以下章节内容中提取所有出现的人物角色信息。\r\n\r\n## 提取要求\r\n1. 识别章节中所有有名字的角色\r\n2. 推断角色的类型（protagonist/supporting/antagonist/npc）\r\n3. 推断角色的性别和年龄（如果有相关信息）\r\n4. 提取角色的性格特点和外貌特征\r\n5. 描述角色在本章中的状态变化\r\n6. 如果涉及修炼体系，提取角色的修为等级信息\r\n\r\n## 输出格式\r\n请使用XML格式输出，标签使用单字母以节省token：\r\n\r\n```xml\r\n<M>\r\n  <C>\r\n    <N>角色名称</N>\r\n    <T>角色类型(protagonist/supporting/antagonist/npc)</T>\r\n    <G>性别(male/female/other)</G>\r\n    <A>年龄</A>\r\n    <S>本章状态变化</S>\r\n    <P>性格特点（逗号分隔）</P>\r\n    <F>外貌特征描述</F>\r\n    <V>\r\n      <SYS>修炼体系名称</SYS>\r\n      <LV>当前境界等级</LV>\r\n      <CH>本章境界变化</CH>\r\n    </V>\r\n    <AP>外貌/装扮变化</AP>\r\n    <PR>性格展现</PR>\r\n    <AB>能力展现</AB>\r\n    <CD>角色成长</CD>\r\n    <DG>核心对话摘要</DG>\r\n  </C>\r\n</M>\r\n```\r\n\r\n## 注意事项\r\n1. 每个角色使用一个C标签包裹\r\n2. 如果角色有多个修炼体系，使用多个V标签\r\n3. 如果章节中没有明确提到某项信息，对应标签可以省略\r\n4. 不要虚构不存在的角色信息\r\n\r\n{chapterInfo}', '[{\"desc\": \"章节号\", \"name\": \"chapterNumber\", \"type\": \"integer\", \"required\": true}, {\"desc\": \"章节标题\", \"name\": \"chapterTitle\", \"type\": \"string\", \"required\": true}, {\"desc\": \"章节内容\", \"name\": \"chapterContent\", \"type\": \"string\", \"required\": true}, {\"desc\": \"修炼体系约束\", \"name\": \"powerSystemConstraint\", \"type\": \"string\", \"required\": false}, {\"desc\": \"已有角色参考\", \"name\": \"existingCharacters\", \"type\": \"string\", \"required\": false}]', '初始版本，用于从章节内容中提取角色信息', 1, NULL, '2026-03-18 14:03:15');

-- ----------------------------
-- Table structure for t_ai_provider_template
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_provider_template`;
CREATE TABLE `t_ai_provider_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `template_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板代码',
  `provider_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务商类型：llm/image/tts/video',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商名称',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '显示名称',
  `icon_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标URL',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `default_endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认API端点',
  `default_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认模型',
  `config_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置参数JSON Schema',
  `required_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '必填字段（JSON数组）',
  `optional_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '可选字段（JSON数组）',
  `is_system` tinyint(1) NULL DEFAULT 1 COMMENT '是否系统内置',
  `is_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `template_code`(`template_code` ASC) USING BTREE,
  INDEX `idx_provider_type`(`provider_type` ASC) USING BTREE,
  INDEX `idx_is_enabled`(`is_enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_ai_provider_template
-- ----------------------------
INSERT INTO `t_ai_provider_template` VALUES (1, 'openai_gpt4', 'llm', 'OpenAI', 'OpenAI GPT-4 Turbo', '/icons/openai.png', 'OpenAI最先进的GPT-4 Turbo模型，支持128K上下文', 'https://api.openai.com/v1', 'gpt-4-turbo-preview', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":2,\"minimum\":0,\"type\":\"number\"},\"topP\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\", \"topP\"]', 1, 1, 1, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (2, 'claude_opus', 'llm', 'Anthropic', 'Claude 3 Opus', '/icons/anthropic.png', 'Anthropic最强大的Claude 3 Opus模型，支持200K上下文', 'https://api.anthropic.com/v1', 'claude-3-opus-20240229', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"},\"topP\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\", \"topP\"]', 1, 1, 2, '2026-02-05 22:05:05', '2026-02-05 23:18:01');
INSERT INTO `t_ai_provider_template` VALUES (3, 'deepseek_chat', 'llm', 'DeepSeek', 'DeepSeek Chat', '/icons/deepseek.png', '深度求索强大的中文对话模型', 'https://api.deepseek.com/v1', 'deepseek-chat', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":2,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 3, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (4, 'zhipu_glm4', 'llm', '智谱AI', 'GLM-4', '/icons/zhipu.png', '智谱AI最新一代基座大模型GLM-4', 'https://open.bigmodel.cn/api/paas/v4', 'glm-4', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 4, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (5, 'baichuan_turbo', 'llm', '百川智能', 'Baichuan2-Turbo', '/icons/baichuan.png', '百川智能Baichuan2大模型系列', 'https://api.baichuan-ai.com/v1', 'Baichuan2-Turbo', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 5, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (6, 'tongyi_qwen', 'llm', '通义千问', 'Qwen Turbo', '/icons/tongyi.png', '阿里云通义千问大模型', 'https://dashscope.aliyuncs.com/api/v1', 'qwen-turbo', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":2,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 6, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (7, 'wenxin_ernie', 'llm', '文心一言', 'ERNIE Bot 4', '/icons/wenxin.png', '百度文心一言ERNIE Bot 4', 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop', 'ernie-bot-4', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 7, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (8, 'kimi_moonshot', 'llm', 'Kimi', 'Moonshot v1', '/icons/kimi.png', '月之暗面Kimi智能助手', 'https://api.moonshot.cn/v1', 'moonshot-v1-8k', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 8, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (9, 'lingyi_yi', 'llm', '零一万物', 'Yi-34B Chat', '/icons/lingyi.png', '零一万物Yi系列大模型', 'https://api.lingyiwanwu.com/v1', 'yi-34b-chat', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 9, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (10, 'minimax_abab', 'llm', 'MiniMax', 'ABAB6.5', '/icons/minimax.png', 'MiniMax ABAB6.5系列模型', 'https://api.minimax.chat/v1', 'abab6.5s-chat', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 10, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (11, 'tiange_chat', 'llm', '天工', 'Tiange Chat', '/icons/tiange.png', '昆仑万维天工大模型', 'https://api.tiangong.cn/v1', 'tiangong-chat', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\"]', 1, 1, 11, '2026-02-05 22:05:05', '2026-02-05 22:05:05');
INSERT INTO `t_ai_provider_template` VALUES (12, 'midjourney_v6', 'image', 'Midjourney', 'Midjourney V6', '/icons/midjourney.png', '最强大的AI图像生成工具Midjourney V6', 'https://api.mindjourney.com/v1', 'v6', '{\"properties\":{\"imageSize\":{\"enum\":[\"1024x1024\",\"2048x2048\"],\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"imageSize\"]', 1, 1, 1, '2026-02-05 22:05:42', '2026-02-05 22:05:42');
INSERT INTO `t_ai_provider_template` VALUES (13, 'sd_stability', 'image', 'Stable Diffusion', 'SDXL', '/icons/stability.png', 'Stability AI官方Stable Diffusion XL', 'https://api.stability.ai/v1', 'stable-diffusion-xl-1024-v1-0', '{\"properties\":{\"imageSize\":{\"type\":\"string\"},\"steps\":{\"type\":\"number\"},\"cfgScale\":{\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"imageSize\", \"steps\", \"cfgScale\"]', 1, 1, 2, '2026-02-05 22:05:42', '2026-02-05 23:35:54');
INSERT INTO `t_ai_provider_template` VALUES (14, 'dalle3', 'image', 'DALL-E', 'DALL-E 3', '/icons/dalle.png', 'OpenAI DALL-E 3图像生成模型', 'https://api.openai.com/v1', 'dall-e-3', '{\"properties\":{\"imageSize\":{\"enum\":[\"1024x1024\",\"1792x1024\",\"1024x1792\"],\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"imageSize\"]', 1, 1, 3, '2026-02-05 22:05:42', '2026-02-05 22:05:42');
INSERT INTO `t_ai_provider_template` VALUES (15, 'comfyui_local', 'image', 'ComfyUI', 'ComfyUI', '/icons/comfyui.png', 'ComfyUI本地部署的Stable Diffusion', 'http://localhost:8188', 'sd_xl_base_1.0', '{\"properties\":{\"imageSize\":{\"type\":\"string\"},\"steps\":{\"type\":\"number\"},\"cfgScale\":{\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiEndpoint\"]', '[\"model\", \"imageSize\", \"steps\", \"cfgScale\"]', 1, 1, 4, '2026-02-05 22:05:42', '2026-02-05 22:05:42');
INSERT INTO `t_ai_provider_template` VALUES (16, 'sdwebui_local', 'image', 'Stable Diffusion WebUI', 'SD WebUI', '/icons/automatic.png', 'Stable Diffusion WebUI本地部署', 'http://localhost:7860', 'sd_xl_base_1.0', '{\"properties\":{\"imageSize\":{\"type\":\"string\"},\"steps\":{\"type\":\"number\"},\"cfgScale\":{\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiEndpoint\"]', '[\"model\", \"imageSize\", \"steps\", \"cfgScale\"]', 1, 1, 5, '2026-02-05 22:05:42', '2026-02-05 23:35:59');
INSERT INTO `t_ai_provider_template` VALUES (17, 'azure_tts', 'tts', 'Azure TTS', 'Azure Speech', '/icons/azure.png', '微软Azure认知服务语音合成', 'https://eastus.tts.speech.microsoft.com/cognitiveservices/v1', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 1, '2026-02-05 22:05:58', '2026-02-05 22:05:58');
INSERT INTO `t_ai_provider_template` VALUES (18, 'google_tts', 'tts', 'Google TTS', 'Google Cloud TTS', '/icons/google.png', 'Google云文本转语音服务', 'https://texttospeech.googleapis.com/v1', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 2, '2026-02-05 22:05:58', '2026-02-05 22:05:58');
INSERT INTO `t_ai_provider_template` VALUES (19, 'aliyun_tts', 'tts', '阿里云 TTS', '阿里云语音合成', '/icons/alibabacloud.png', '阿里云智能语音交互', 'https://nls-meta.cn-shanghai.aliyuncs.com', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 3, '2026-02-05 22:05:58', '2026-02-05 23:37:48');
INSERT INTO `t_ai_provider_template` VALUES (20, 'tencent_tts', 'tts', '腾讯云 TTS', '腾讯云语音合成', '/icons/tencent.png', '腾讯云语音合成服务', 'https://tts.cloud.tencent.com/stream', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 4, '2026-02-05 22:05:58', '2026-02-05 22:05:58');
INSERT INTO `t_ai_provider_template` VALUES (21, 'elevenlabs', 'tts', 'ElevenLabs', 'ElevenLabs', '/icons/elevenlabs.png', 'ElevenLabs AI语音合成', 'https://api.elevenlabs.io/v1', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 5, '2026-02-05 22:05:58', '2026-02-05 22:05:58');
INSERT INTO `t_ai_provider_template` VALUES (22, 'doubao_tts', 'tts', '豆包', '字节跳动语音合成', '/icons/doubao.png', '字节跳动豆包语音合成', 'https://openspeech.bytedance.com/api/v1', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 6, '2026-02-05 22:05:58', '2026-02-05 22:05:58');
INSERT INTO `t_ai_provider_template` VALUES (23, 'runway_gen2', 'video', 'Runway', 'Gen-2 视频生成', '/icons/runway.png', 'Runway Gen-2 AI视频生成模型', '', '', '{\"properties\":{},\"type\":\"object\"}', '[\"apiKey\"]', '[]', 1, 1, 1, '2026-02-05 22:06:03', '2026-02-05 22:06:03');
INSERT INTO `t_ai_provider_template` VALUES (24, 'pika_video', 'video', 'Pika', 'Pika 视频生成', '/icons/pika.png', 'Pika Labs AI视频生成', '', '', '{\"properties\":{},\"type\":\"object\"}', '[\"apiKey\"]', '[]', 1, 1, 2, '2026-02-05 22:06:03', '2026-02-05 22:06:03');
INSERT INTO `t_ai_provider_template` VALUES (25, 'custom_llm', 'llm', '自定义', '自定义 LLM', '/icons/custom.png', '自定义配置大语言模型', '', '', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":2,\"minimum\":0,\"type\":\"number\"},\"topP\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\", \"topP\"]', 1, 1, 99, '2026-02-05 22:11:33', '2026-02-05 22:11:33');
INSERT INTO `t_ai_provider_template` VALUES (26, 'custom_image', 'image', '自定义', '自定义图像生成', '/icons/custom.png', '自定义配置图像生成服务', '', '', '{\"properties\":{\"imageSize\":{\"type\":\"string\"},\"steps\":{\"type\":\"number\"},\"cfgScale\":{\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"imageSize\", \"steps\", \"cfgScale\"]', 1, 1, 99, '2026-02-05 22:11:33', '2026-02-05 22:11:33');
INSERT INTO `t_ai_provider_template` VALUES (27, 'custom_tts', 'tts', '自定义', '自定义语音合成', '/icons/custom.png', '自定义配置语音合成服务', '', '', '{\"properties\":{\"pitch\":{\"type\":\"number\"},\"speed\":{\"type\":\"number\"},\"voice\":{\"type\":\"string\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\"]', '[\"voice\", \"speed\", \"pitch\"]', 1, 1, 99, '2026-02-05 22:11:33', '2026-02-05 22:11:33');
INSERT INTO `t_ai_provider_template` VALUES (28, 'custom_video', 'video', '自定义', '自定义视频生成', '/icons/custom.png', '自定义配置视频生成服务', '', '', '{\"properties\":{},\"type\":\"object\"}', '[\"apiKey\"]', '[]', 1, 1, 99, '2026-02-05 22:11:33', '2026-02-05 22:11:33');
INSERT INTO `t_ai_provider_template` VALUES (29, 'gemini_pro', 'llm', 'Google', 'Gemini Pro', '/icons/gemini.png', 'Google Gemini Pro 大语言模型', 'https://generativelanguage.googleapis.com/v1beta', 'gemini-pro', '{\"properties\":{\"maxTokens\":{\"minimum\":1,\"type\":\"number\"},\"temperature\":{\"maximum\":2,\"minimum\":0,\"type\":\"number\"},\"topP\":{\"maximum\":1,\"minimum\":0,\"type\":\"number\"},\"topK\":{\"minimum\":1,\"type\":\"number\"}},\"type\":\"object\"}', '[\"apiKey\", \"apiEndpoint\", \"model\"]', '[\"temperature\", \"maxTokens\", \"topP\", \"topK\"]', 1, 1, 12, '2026-02-05 22:20:16', '2026-02-05 22:20:16');

SET FOREIGN_KEY_CHECKS = 1;
