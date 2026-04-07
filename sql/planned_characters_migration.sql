-- Migration: Add planned_characters column to novel_chapter_plan
-- Purpose: Store AI-planned character assignments per chapter
-- Date: 2026-04-07

ALTER TABLE novel_chapter_plan
ADD COLUMN planned_characters json DEFAULT NULL COMMENT '规划角色（JSON格式，存储 AI 规划中计划登场的角色列表）'
AFTER foreshadowing_actions;
