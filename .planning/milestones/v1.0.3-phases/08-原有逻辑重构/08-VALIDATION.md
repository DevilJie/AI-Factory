---
phase: 8
slug: 08-原有逻辑重构
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-03
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito |
| **Config file** | None (standard Maven test structure) |
| **Quick run command** | `cd ai-factory-backend && mvn compile -q` |
| **Full suite command** | `cd ai-factory-backend && mvn test -q` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn compile -q`
- **After every plan wave:** Run `cd ai-factory-backend && mvn compile -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | REFACT-01 | unit | `mvn test -Dtest=WorldviewXmlParserTest#testParseGeographyXml -q` | Wave 0 | pending |
| 08-01-02 | 01 | 1 | REFACT-01 | unit | `mvn test -Dtest=WorldviewXmlParserTest#testParseFactionXml -q` | Wave 0 | pending |
| 08-01-03 | 01 | 1 | REFACT-01 | unit | `mvn test -Dtest=WorldviewXmlParserTest#testFindRegionIdByName -q` | Wave 0 | pending |
| 08-02-01 | 02 | 2 | REFACT-01 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCreateSteps -q` | Wave 0 | pending |
| 08-02-02 | 02 | 2 | REFACT-01 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCheckExisting -q` | Wave 0 | pending |
| 08-02-03 | 02 | 2 | REFACT-02 | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testSaveCoreAssembly -q` | Wave 0 | pending |
| all | all | all | REFACT-01 | build | `cd ai-factory-backend && mvn compile -q` | N/A | pending |

*Status: pending | green | red | flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/common/WorldviewXmlParserTest.java` — unit tests for DOM parsing extraction
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/WorldviewTaskStrategyTest.java` — unit tests for 9-step orchestration (mock Strategy dependencies)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| End-to-end worldview generation returns complete data | REFACT-02 | Requires running AI provider + DB + full Spring context | 1. Start backend 2. Call POST /worldview/generate-async 3. Verify response contains geography + power_system + faction + core worldview fields |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
