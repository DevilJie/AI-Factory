---
phase: 04
slug: 前端树组件
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None — no frontend test runner configured |
| **Config file** | none — Wave 0 not applicable |
| **Quick run command** | `cd ai-factory-frontend && npm run build` |
| **Full suite command** | `cd ai-factory-frontend && npm run build` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-frontend && npm run build`
- **After every plan wave:** Full manual browser verification pass
- **Before `/gsd:verify-work`:** All 3 success criteria verified manually
- **Max feedback latency:** ~15 seconds (build)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | UI-02 | build | `npm run build` | ❌ W0 | ⬜ pending |
| 04-01-02 | 01 | 1 | UI-01 | manual | browser | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 1 | UI-03 | build | `npm run build` | ❌ W0 | ⬜ pending |
| 04-02-02 | 02 | 1 | UI-03 | manual | browser | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test framework installation needed — this is a UI-heavy phase using manual browser verification and build checks.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| FactionTree renders tree hierarchy, expand/collapse | UI-01 | Visual UI behavior | Open WorldSetting, verify tree renders with correct nesting and expand/collapse icons |
| Inline editing (name, description, type, power system) | UI-01 | Interactive UI | Click edit on a node, modify fields, verify save persists to backend |
| Add/delete faction nodes | UI-01 | Interactive UI | Add root + child nodes, delete nodes, verify tree updates and backend persists |
| Type badges with correct colors (green/red/gray) | UI-01 | Visual rendering | Verify [正派]=green, [反派]=red, [中立]=gray badges on root factions |
| Power system label resolution (ID → name) | UI-01 | Data display | Verify `· 力量体系名` shows resolved name, not ID |
| WorldSetting textarea replaced by FactionTree | UI-03 | DOM structure | Inspect WorldSetting page, confirm no textarea, FactionTree component present |
| AI generate refreshes FactionTree | UI-03 | Async behavior | Trigger AI generation, verify FactionTree refreshes with new data |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
