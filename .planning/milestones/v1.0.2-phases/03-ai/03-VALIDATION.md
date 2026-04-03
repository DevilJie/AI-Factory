---
phase: 03
slug: ai
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito |
| **Config file** | pom.xml (maven-surefire-plugin) |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest=FactionServiceTest -DfailIfNoTests=false` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -pl .`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | PROMPT-01~06 | unit | `mvn test -Dtest=FactionServiceTest#testFillForces` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 1 | AI-03 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testSaveFactionsFromXml` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 1 | AI-04 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testTwoPassInsert` | ❌ W0 | ⬜ pending |
| 03-02-03 | 02 | 1 | AI-05 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testNameMatching` | ❌ W0 | ⬜ pending |
| 03-02-04 | 02 | 1 | AI-06 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCheckExistingDeletesFactions` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `WorldviewTaskStrategyTest.java` — stubs for AI-03, AI-04, AI-05, AI-06
- [ ] `FactionServiceTest.java` — stubs for PROMPT-01~06 (fillForces behavior)
- [ ] Test relies on Mockito for FactionService, ContinentRegionService, mapper mocks

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AI prompt template outputs structured XML | AI-01, AI-02 | Template stored in DB, needs runtime verification | Generate worldview, verify XML structure in AI response |
| Existing project re-generate clears old data | AI-06, AI-07 | Requires full integration with DB | Re-generate worldview on existing project, verify old factions deleted |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
