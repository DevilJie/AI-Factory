---
phase: quick
plan: 260408-tw4
subsystem: backend+prompt-template
tags: [xml-parsing, prompt-template, power-system, backward-compat]
dependency_graph:
  requires: [existing-power-system-tables, existing-character-extract-template]
  provides: [rl-sl-split-xml-format, strict-power-system-name-constraints]
  affects: [chapter-character-extract, power-system-association-resolution]
tech_stack:
  added: []
  patterns: [backward-compat-helper-methods, exact-then-fuzzy-matching]
key_files:
  created: []
  modified:
    - ai-factory-backend/src/main/java/com/aifactory/dto/ChapterCharacterExtractXmlDto.java
    - ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java
    - sql/init.sql
decisions:
  - Keep LV field with @JacksonXmlProperty for backward compat with old LLM responses
  - Use getEffectiveRealmLevel/getEffectiveSubLevel helper methods to transparently handle both formats
  - Template version id=21 chosen (next available in ai_prompt_template_version table)
  - Keep exact-then-fuzzy matching strategy, only change source fields
metrics:
  duration: 11min
  completed: 2026-04-08
  tasks: 2
  files: 3
  commits: 2
---

# Quick Task 260408-tw4: Character Extract Prompt Template RL/SL Split Summary

Split the combined `<LV>` tag in character extraction XML into separate `<RL>` (realm level) and `<SL>` (sub level) tags with backward compatibility, and created prompt template v3 with strict power system name matching constraints.

## Changes Made

### Task 1: DTO and Service Refactor (c9910a7)

**ChapterCharacterExtractXmlDto.CultivationSystemDto:**
- Added `realmLevel` field mapped to `<RL>` (e.g., "醒血境")
- Added `subLevel` field mapped to `<SL>` (e.g., "感血")
- Kept `currentLevel` field mapped to `<LV>` for backward compatibility with old LLM responses
- Added `getEffectiveRealmLevel()` helper: returns realmLevel if present, otherwise extracts from currentLevel (part before "（")
- Added `getEffectiveSubLevel()` helper: returns subLevel if present, otherwise extracts from currentLevel (part inside "（）")
- Updated all Javadoc comments to reflect the new XML format

**ChapterCharacterExtractService:**
- `parseCultivationLevelJson()`: Now stores `realmLevel`, `subLevel`, and `currentLevel` in the JSON map
- `resolveAndSavePowerSystemAssociations()`: Uses `getEffectiveRealmLevel()` for level (大境界) matching and `getEffectiveSubLevel()` for step (小境界) matching instead of the old combined `currentLevel` approach. Matching strategy (exact then fuzzy) remains unchanged.

### Task 2: Prompt Template v3 (1602b70)

**sql/init.sql:**
- Added `ai_prompt_template_version` id=21, template_id=15, version_number=3
- Template content updated with `<RL>`/`<SL>` format replacing `<LV>` in XML output example
- Added "修炼体系输出约束（极其重要）" section requiring:
  - `<SYS>` must match power system name exactly
  - `<RL>` must match level name in power system table exactly
  - `<SL>` must match step name in power system table exactly
  - Forbidden to fabricate non-existent names
- `variable_definitions` JSON extended with `realmLevel` and `subLevel` entries
- Deactivated v2 (id=19) via `is_active = 0`
- Updated template table `current_version_id` to 21

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check

- All modified files verified present
- Commits c9910a7 and 1602b70 verified in git log
- Backend compiles cleanly (`mvn compile -q` passes)
- `<RL>` appears in init.sql (1 match)
- `<SL>` appears in init.sql (1 match)
- `realmLevel` appears in DTO (4 matches)
- `getEffectiveRealmLevel` used in service (2 matches)
