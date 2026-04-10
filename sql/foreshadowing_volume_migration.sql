SET NAMES utf8mb4;

-- Phase 15-01: Add volume fields to novel_foreshadowing
ALTER TABLE novel_foreshadowing
  ADD COLUMN `planted_volume` int NULL DEFAULT NULL COMMENT '埋设伏笔的分卷编号' AFTER `planted_chapter`,
  ADD COLUMN `planned_callback_volume` int NULL DEFAULT NULL COMMENT '计划回收伏笔的分卷编号' AFTER `planned_callback_chapter`;

ALTER TABLE novel_foreshadowing
  ADD INDEX `idx_planted_volume`(`project_id`, `planted_volume`),
  ADD INDEX `idx_callback_volume`(`project_id`, `planned_callback_volume`);

-- Phase 15-02: Remove redundant foreshadowing text fields from novel_chapter_plan
ALTER TABLE novel_chapter_plan
  DROP COLUMN foreshadowing_setup,
  DROP COLUMN foreshadowing_payoff;
