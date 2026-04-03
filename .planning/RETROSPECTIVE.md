# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0.2 — 势力阵营结构化重构

**Shipped:** 2026-04-03
**Phases:** 5 | **Plans:** 12 | **Tasks:** 18

### What Was Built
- 4 database tables (novel_faction tree + 3 association tables) with full entity/mapper layer
- FactionService with tree CRUD, 4-table cascading delete, root inheritance, fillForces() text builder
- FactionController with 14 REST endpoints (5 faction + 3x3 association management)
- AI prompt template migration to structured XML with DOM parsing, two-pass insert, three-tier name matching
- 11 call-site migration from getForces() to fillForces() across 8 files
- Recursive FactionTree.vue with type badges, power system labels, CRUD at all depths
- FactionDrawer with 3 association management tabs (Relation, Character, Region)

### What Worked
- Cloning existing patterns (ContinentRegionService/GeographyTree) led to zero-deviation execution across all phases
- Tree table pattern (parent_id + deep) reused seamlessly for third worldview domain
- Two-pass insert strategy (saveTree → buildNameToIdMap → insert associations) handled circular dependencies cleanly
- Yolo mode with parallel agent execution kept velocity high — 12 plans in ~1 hour of execution time

### What Was Inefficient
- REQUIREMENTS.md checkboxes fell behind — 5 items were built but not checked off, creating false gaps at milestone review
- ROADMAP.md plan checkboxes also stale (02-02, 05-01, 05-02 unchecked despite completion)
- Worktree branches occasionally behind feature branch, requiring merge before implementation

### Patterns Established
- Child-instance-local state with refresh emit for recursive tree CRUD (solves all-depth operations)
- Direct mapper injection in controllers for simple association table CRUD (bypass service layer)
- HTML datalist for preset + custom input fields (role selection)
- Inline tree selector via flat visibleNodes computed (not recursive component)

### Key Lessons
1. Document completion (checkboxes) should be automated — summary files track reality but REQUIREMENTS.md is manual
2. Cloning verified patterns delivers massive efficiency — no creative design needed, just execution
3. Two-pass insert is the go-to strategy for entity graphs with cross-references
4. Recursive Vue components need child-local state management, not event bubbling with data

### Cost Observations
- Model mix: 100% sonnet (balanced profile)
- Avg plan duration: ~5min (range: 2-13min)
- Notable: AI integration (Phase 03) took longest at 20min total due to 11-call-site migration across 8 files

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0.2 | 3+ | 5 | Third worldview structuring — pattern fully mature |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.0.2 | 0 | n/a | 37 requirements, all met |

### Top Lessons (Verified Across Milestones)

1. Clone existing patterns for similar domains — zero deviation means zero risk
2. Tree table + transient field + DOM parsing is a proven stack for worldview structuring
