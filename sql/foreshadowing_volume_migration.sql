-- Migration for v1.0.6 foreshadowing management
-- Phase 15: Data Foundation

-- Remove redundant foreshadowing text fields from novel_chapter_plan
ALTER TABLE novel_chapter_plan
  DROP COLUMN foreshadowing_setup,
  DROP COLUMN foreshadowing_payoff;
