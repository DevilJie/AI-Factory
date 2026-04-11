---
phase: 18
slug: frontend-chapter-foreshadowing
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-11
---

# Phase 18 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (Vue 3 + Vite) |
| **Config file** | `ai-factory-frontend/vitest.config.ts` |
| **Quick run command** | `cd ai-factory-frontend && npx vitest run --reporter=verbose 2>&1 | tail -20` |
| **Full suite command** | `cd ai-factory-frontend && npx vitest run 2>&1` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-frontend && npx vitest run --reporter=verbose 2>&1 | tail -20`
- **After every plan wave:** Run `cd ai-factory-frontend && npx vitest run 2>&1`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 18-01-01 | 01 | 1 | FC-01 | unit | `cd ai-factory-frontend && npx vitest run src/types/__tests__/foreshadowing.test.ts` | ❌ W0 | ⬜ pending |
| 18-01-02 | 01 | 1 | FC-01 | unit | `cd ai-factory-frontend && npx vitest run src/api/__tests__/foreshadowing.test.ts` | ❌ W0 | ⬜ pending |
| 18-02-01 | 02 | 1 | FC-01 | component | `cd ai-factory-frontend && npx vitest run` | ❌ W0 | ⬜ pending |
| 18-02-02 | 02 | 1 | FC-02 | component | `cd ai-factory-frontend && npx vitest run` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-frontend/src/types/__tests__/foreshadowing.test.ts` — stubs for FC-01/FC-02
- [ ] `ai-factory-frontend/src/api/__tests__/foreshadowing.test.ts` — stubs for API client tests
- [ ] Vitest already configured — verify `vitest.config.ts` exists

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Visual card layout with color-coded left border | FC-01 | Requires browser visual inspection | Open ChapterPlanDrawer, select foreshadowing tab, verify card layout and color coding |
| Modal form appears above drawer (z-index) | FC-02 | Requires browser interaction | Click add/edit foreshadowing, verify modal renders above drawer |
| Collapsible volume reference section | FC-01 | Requires browser interaction | Verify section collapses/expands and defaults to collapsed |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
