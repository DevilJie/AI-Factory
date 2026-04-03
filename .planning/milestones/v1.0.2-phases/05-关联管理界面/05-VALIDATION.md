---
phase: 5
slug: 关联管理界面
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (Vue 3 frontend tests) |
| **Config file** | ai-factory-frontend/vitest.config.ts (if exists) |
| **Quick run command** | `cd ai-factory-frontend && npx vitest run --reporter=verbose` |
| **Full suite command** | `cd ai-factory-frontend && npx vitest run` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-frontend && npx vitest run --reporter=verbose`
- **After every plan wave:** Run `cd ai-factory-frontend && npx vitest run`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | UI-04 | unit | `npx vitest run factionRelation` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | UI-04 | unit | `npx vitest run factionRelation` | ❌ W0 | ⬜ pending |
| 05-01-03 | 01 | 1 | UI-04 | unit | `npx vitest run factionRelation` | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 1 | UI-05 | unit | `npx vitest run factionCharacter` | ❌ W0 | ⬜ pending |
| 05-02-02 | 02 | 1 | UI-06 | unit | `npx vitest run factionRegion` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-frontend/src/api/__tests__/faction.test.ts` — stubs for relation/character/region API tests
- [ ] Vitest config — ensure vitest.config.ts exists with Vue test utils
- [ ] Test utilities — ensure @vue/test-utils installed

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Drawer open/close animation | UI-04/05/06 | Visual animation verification | Click faction node → verify drawer slides in; click close → verify slides out |
| Tree region selector expand/collapse | UI-06 | Interactive tree behavior | Click region dropdown → verify tree expands with indentation |
| Dual-relation bidirectional sync | UI-04 | Requires backend + DB state | Add A→B relation → verify B→A appears in B's relation list |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
