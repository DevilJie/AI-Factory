---
phase: 15
slug: data-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-10
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Spring Boot Test |
| **Config file** | `ai-factory-backend/pom.xml` (test dependencies) |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest="Foreshadowing*Test" -Dsurefire.failIfNoSpecifiedTests=false` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -pl . -Dtest="Foreshadowing*Test" -Dsurefire.failIfNoSpecifiedTests=false`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 15-01-01 | 01 | 1 | DATA-01 | unit | `mvn test -Dtest="ForeshadowingServiceTest#testCreateWithVolumeFields"` | ❌ W0 | ⬜ pending |
| 15-01-02 | 01 | 1 | DATA-01 | unit | `mvn test -Dtest="ForeshadowingServiceTest#testQueryByVolume"` | ❌ W0 | ⬜ pending |
| 15-02-01 | 02 | 1 | DATA-02 | compile | `mvn compile` (no foreshadowingSetup/payoff refs) | ✅ | ⬜ pending |
| 15-02-02 | 02 | 1 | DATA-02 | grep | `grep -r "foreshadowingSetup\|foreshadowingPayoff" src/` returns 0 | ✅ | ⬜ pending |
| 15-03-01 | 03 | 2 | DATA-03 | unit | `mvn test -Dtest="ForeshadowingServiceTest#testDistanceValidation"` | ❌ W0 | ⬜ pending |
| 15-03-02 | 03 | 2 | DATA-03 | unit | `mvn test -Dtest="ForeshadowingServiceTest#testDarkForeshadowingExempt"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/ForeshadowingServiceTest.java` — unit test stubs for DATA-01, DATA-03
- [ ] Test covers: create with volume fields, query by volume, distance validation pass/fail, dark exemption

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Frontend no longer shows foreshadowing textareas | DATA-02 | UI verification | Open ChapterPlanDrawer, verify no foreshadowingSetup/payoff textareas |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
