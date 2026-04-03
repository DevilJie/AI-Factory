---
phase: 03-ai
plan: 01
subsystem: ai
tags: [sql, prompt-template, xml, faction, worldview-generation]

# Dependency graph
requires:
  - phase: 02-api
    provides: FactionService with fillForces/saveTree/deleteByProjectId
provides:
  - SQL migration updating worldview prompt template to output structured faction XML
affects: [03-02, 03-03]

# Tech tracking
tech-stack:
  added: []
  patterns: [structured-faction-xml-in-ai-prompt, name-consistency-references]

key-files:
  created:
    - ai-factory-backend/src/main/resources/db/migration/V3__faction_prompt_template.sql
  modified: []

key-decisions:
  - "Full template replacement in UPDATE statement rather than REPLACE() function, matching existing migration pattern from geography_migration.sql"
  - "Faction XML uses child elements only (no attributes), consistent with D-02 decision"
  - "Type enum specified as Chinese labels (正派/反派/中立) per D-11, matching AI output expectation"

patterns-established:
  - "Structured faction XML format: <f> container with nested <faction> elements using <n>, <type>, <power>, <regions>, <d>, <relation> child tags"
  - "AI name consistency enforcement: explicit instructions to reuse exact names from <p> and <g> sections"

requirements-completed: [AI-01, AI-02]

# Metrics
duration: 2min
completed: 2026-04-02
---

# Phase 3 Plan 01: AI Prompt Template Update Summary

**SQL migration replacing plain-text faction CDATA with structured XML format in worldview generation prompt, enforcing name consistency for power systems and regions**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-02T02:52:01Z
- **Completed:** 2026-04-02T02:54:01Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Created SQL migration that updates the `llm_worldview_create` prompt template to output structured faction XML instead of plain-text CDATA
- Template now instructs AI to output nested `<faction>` elements with `<n>`, `<type>`, `<power>`, `<regions>`, `<d>`, `<relation>` child tags
- Added explicit name consistency rules requiring `<power>` values to match `<p>` section names and `<regions>` values to match `<g>` section names
- Included complete XML example in the template following D-01 through D-05 locked format decisions

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SQL migration to update faction prompt template** - `078230d` (feat)

## Files Created/Modified
- `ai-factory-backend/src/main/resources/db/migration/V3__faction_prompt_template.sql` - SQL migration updating worldview prompt template faction section from plain-text to structured XML format

## Decisions Made
- Used full `template_content` replacement in UPDATE statement rather than MySQL REPLACE() function, matching the existing pattern from geography_migration.sql and power_system_migration.sql
- WHERE clause uses subquery on `ai_prompt_template.code = 'llm_worldview_create'` for robustness, not hard-coded version ID
- Faction XML example embedded directly in the template instruction text, following the same pattern as the geography XML example section

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Prompt template updated and ready for Plan 02 (DOM parser implementation)
- AI will now output structured faction XML that can be parsed by saveFactionsFromXml()
- Plan 03 can proceed with getForces() migration points independently

## Self-Check: PASSED

- [x] ai-factory-backend/src/main/resources/db/migration/V3__faction_prompt_template.sql - FOUND
- [x] Commit 078230d - FOUND
- [x] 03-01-SUMMARY.md - FOUND

---
*Phase: 03-ai*
*Completed: 2026-04-02*
