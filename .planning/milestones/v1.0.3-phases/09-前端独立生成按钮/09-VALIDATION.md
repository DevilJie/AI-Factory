---
phase: 9
slug: 前端独立生成按钮
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-03
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None — UI-only phase, no frontend test infrastructure |
| **Config file** | none |
| **Quick run command** | Visual browser inspection |
| **Full suite command** | Manual walkthrough of all 4 requirements |
| **Estimated runtime** | ~120 seconds (manual) |

---

## Sampling Rate

- **After every task commit:** Visual inspection in browser (button state, loading spinner, toast messages)
- **After every plan wave:** Full manual walkthrough of all 4 requirements
- **Before `/gsd:verify-work`:** All 4 success criteria verified manually
- **Max feedback latency:** 120 seconds (manual)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | UI-01, UI-02, UI-03 | manual | `grep 'generateGeographyAsync' ai-factory-frontend/src/api/worldview.ts` | ⬜ W0 | ⬜ pending |
| 09-01-02 | 01 | 1 | UI-04 | manual | `grep 'generatingModule' ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue` | ⬜ W0 | ⬜ pending |
| 09-02-01 | 02 | 1 | UI-01 | manual | `grep 'emit.*generate' ai-factory-frontend/src/views/Project/Detail/components/GeographyTree.vue` | ⬜ W0 | ⬜ pending |
| 09-02-02 | 02 | 1 | UI-02 | manual | `grep 'emit.*generate' ai-factory-frontend/src/views/Project/Detail/components/PowerSystemSection.vue` | ⬜ W0 | ⬜ pending |
| 09-02-03 | 02 | 1 | UI-03 | manual | `grep 'emit.*generate' ai-factory-frontend/src/views/Project/Detail/components/FactionTree.vue` | ⬜ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements. No test framework setup needed — this is a frontend-only phase with manual verification.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Geography AI button click triggers generation, shows spinner, refreshes tree | UI-01 | No frontend test infrastructure | Click AI button in Geography section, verify spinner appears, tree refreshes after completion |
| PowerSystem AI button click triggers generation, shows spinner, refreshes list | UI-02 | No frontend test infrastructure | Click AI button in PowerSystem section, verify spinner appears, list refreshes after completion |
| Faction AI button click triggers generation, shows spinner, refreshes tree | UI-03 | No frontend test infrastructure | Click AI button in Faction section, verify spinner appears, tree refreshes after completion |
| Loading state, error display, dependency validation messages | UI-04 | No frontend test infrastructure | Trigger dependency validation failure (e.g., generate faction without geography), verify toast shows backend error message |
| Mutual exclusion between modules | UI-05 (from D-05) | No frontend test infrastructure | Start geography generation, verify powerSystem and faction buttons disabled |
| Whole-worldview button disabled during independent generation | UI-05 (from D-06) | No frontend test infrastructure | Start module generation, verify top AI button is disabled |
| Page refresh restores module generation state | UI-07 (from D-08) | No frontend test infrastructure | Start generation, refresh page, verify polling resumes and completes |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or manual-only with test instructions
- [ ] Sampling continuity: no 3 consecutive tasks without verification
- [ ] Wave 0 covers all MISSING references (N/A — no missing refs)
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
