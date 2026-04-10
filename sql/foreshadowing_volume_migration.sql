SET NAMES utf8mb4;

ALTER TABLE novel_foreshadowing
  ADD COLUMN `planted_volume` int NULL DEFAULT NULL COMMENT '埋设伏笔的分卷编号' AFTER `planted_chapter`,
  ADD COLUMN `planned_callback_volume` int NULL DEFAULT NULL COMMENT '计划回收伏笔的分卷编号' AFTER `planned_callback_chapter`;

ALTER TABLE novel_foreshadowing
  ADD INDEX `idx_planted_volume`(`project_id`, `planted_volume`),
  ADD INDEX `idx_callback_volume`(`project_id`, `planned_callback_volume`);
