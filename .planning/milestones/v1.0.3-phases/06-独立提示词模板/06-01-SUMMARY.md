---
phase: 06-独立提示词模板
plan: 01
subsystem: database
tags: [sql, flyway, prompt-template, ai, xml]

# Dependency graph
requires:
  - phase: 05-势力管理前端
    provides: V3 faction prompt template SQL migration pattern
provides:
  - V4 migration with 3 independent prompt templates (geography, power system, faction)
  - Simplified unified worldview template (only t/b/l/r fields)
  - Template variable definitions for Phase 7 API integration
affects: [07-独立生成API, 08-世界观组合重构]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "3-step template creation: INSERT master -> INSERT version -> UPDATE current_version_id"
    - "Context injection via XML wrapper tags (<existing_geography>, <existing_power_systems>)"

key-files:
  created:
    - ai-factory-backend/src/main/resources/db/migration/V4__independent_prompt_templates.sql
  modified: []

key-decisions:
  - "Geography template uses <r><n>/<d> sub-tag format exclusively (not old name attribute)"
  - "Faction template wraps context data in <existing_geography>/<existing_power_systems> to differentiate input from output"
  - "Unified template UPDATE keeps same version row (no new version), matching V3 pattern"
  - "All WHERE clauses use template_code column, fixing V3's incorrect code column reference"

patterns-established:
  - "Independent prompt template pattern: each worldview submodule gets its own template_code with scenario naming convention worldview_{module}_generate"
  - "Context injection pattern: existing data embedded as XML in prompt with wrapper tags for disambiguation"

requirements-completed: [PROMPT-01, PROMPT-02, PROMPT-03, PROMPT-04]

# Metrics
duration: 5min
completed: 2026-04-03
---

# Phase 6 Plan 01: 独立提示词模板 Summary

**V4 Flyway migration creating 3 independent AI prompt templates (geography/power system/faction) by extracting from unified worldview template, plus simplifying the unified template to only output t/b/l/r fields**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-03T03:31:15Z
- **Completed:** 2026-04-03T03:36:12Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Created llm_geography_create template with <r><n>/<d> sub-tag format and nested region generation rules
- Created llm_power_system_create template with <ss> multi-system structure and step-level detail requirements
- Created llm_faction_create template with geographyContext/powerSystemContext context injection via XML wrapper tags
- Simplified llm_worldview_create to only output <t>/<b>/<l>/<r> fields, removing all geography/power/faction instructions

## Task Commits

Each task was committed atomically:

1. **Task 1: Create V4 migration with geography and power system templates** - `ddf70da` (feat)
2. **Task 2: Add faction template INSERT and unified template UPDATE** - `bc5d99f` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/resources/db/migration/V4__independent_prompt_templates.sql` - Flyway migration creating 3 new prompt templates and updating the unified worldview template

## Decisions Made
- Geography template uses `<r><n>名称</n><d>描述</d></r>` format exclusively (matching parseSingleRegion primary format), not the old `name` attribute format
- Faction template wraps injected context in `<existing_geography>` and `<existing_power_systems>` tags to differentiate input data from output format instructions
- All WHERE clauses use `template_code` column (correct), not `code` (V3 had this wrong)
- Variable definitions JSON includes 4 base variables for geography/power templates, 6 variables for faction template (adding geographyContext, powerSystemContext)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- V4 migration ready for Flyway execution on app startup
- 3 new template codes (llm_geography_create, llm_power_system_create, llm_faction_create) available for Phase 7 API integration
- Phase 7 will call PromptTemplateService.executeTemplate() with these codes
- Phase 8 will restructure WorldviewTaskStrategy to use simplified llm_worldview_create + 3 independent templates

## Self-Check: PASSED

- FOUND: ai-factory-backend/src/main/resources/db/migration/V4__independent_prompt_templates.sql
- FOUND: .planning/phases/06-独立提示词模板/06-01-SUMMARY.md
- FOUND: commit ddf70da (Task 1)
- FOUND: commit bc5d99f (Task 2)

---
*Phase: 06-独立提示词模板*
*Completed: 2026-04-03*
