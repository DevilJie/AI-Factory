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

## Milestone: v1.0.3 — 世界观生成任务拆分

**Shipped:** 2026-04-05
**Phases:** 4 | **Plans:** 6 | **Tasks:** 11

### What Was Built
- V4 Flyway migration with 3 independent AI prompt templates + unified template simplification
- 3 independent generation strategies (GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy) with dependency validation
- WorldviewXmlParser shared DOM parsing utility (~400 lines extracted, 26 unit tests)
- WorldviewTaskStrategy 9-step orchestrator (~250 LOC, down from ~920)
- 3 independent AI generation buttons with mutual exclusion, localStorage recovery, polling
- Post-milestone: cascade faction re-generation, indeterminate region tree state, ConfirmDialog

### What Worked
- "Copy first, extract later" strategy — Phase 7 copied DOM parsing to unblock API work, Phase 8 consolidated cleanly
- 9-step orchestrator pattern — clean delegation via step stubs, each module fully persists before next starts
- Single generatingModule union-type ref — 3 booleans → 1 ref for cleaner mutual exclusion
- localStorage recovery for in-progress generation — 10min expiry matching poll timeout
- Config is yolo mode — auto-proceed on verification gates kept momentum

### What Was Inefficient
- Phase 6-8 all executed in a single day with minimal context breaks — risk of fatigue
- REQUIREMENTS.md UI items weren't checked off after Phase 9 execution — had to fix during milestone completion
- ROADMAP Progress table got out of sync (showed 0/? for completed phases) — STATE.md was more reliable
- UAT tests defined but never run (7 pending) — human verification skipped for milestone completion

### Patterns Established
- Step stub delegation: createStepStub(String) for cross-strategy calls from orchestrator
- Module generation state machine: generatingModule ref + handler + localStorage + restoreModuleState
- Controller-level dependency validation with context injection into async task config
- Green gradient AI button: `bg-gradient-to-r from-green-500 to-teal-500` with Sparkles/Loader2 icons

### Key Lessons
1. Extract after copy is a safe refactoring pattern — duplicate first to unblock, consolidate when all consumers are known
2. Orchestrator + Strategy pattern scales well — WorldviewTaskStrategy went from 920 → 250 LOC by delegating
3. REQUIREMENTS.md should be updated immediately after execution, not deferred to milestone completion
4. UAT testing should be run before claiming milestone complete — automate what can be automated

### Cost Observations
- Model mix: ~90% sonnet, ~10% opus (planning/review agents)
- Sessions: 3 (Phase 6-7, Phase 8, Phase 9)
- Notable: Phase 8-02 orchestrator rewrite completed in 5 minutes — clear plan + familiar codebase

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0.2 | 3+ | 5 | Third worldview structuring — pattern fully mature |
| v1.0.3 | 3 | 4 | Generation decomposition — orchestrator pattern proven |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.0.2 | 0 | n/a | 37 requirements, all met |
| v1.0.3 | 40+ | parser+orchestrator | 16 requirements, all met |

### Top Lessons (Verified Across Milestones)

1. Clone existing patterns for similar domains — zero deviation means zero risk
2. Tree table + transient field + DOM parsing is a proven stack for worldview structuring
3. Orchestrator + Strategy pattern decomposes monoliths cleanly — 920 → 250 LOC
4. Copy-first extract-later is safe for cross-cutting refactors
