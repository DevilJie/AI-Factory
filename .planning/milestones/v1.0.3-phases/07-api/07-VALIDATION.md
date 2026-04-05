---
phase: 7
slug: api
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-03
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Spring Boot Test |
| **Config file** | none — Wave 0 creates test files |
| **Quick run command** | `cd ai-factory-backend && mvn compile` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn compile`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | API-01 | unit | `mvn test -Dtest=GeographyTaskStrategyTest -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-01-02 | 01 | 1 | API-01 | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateGeography -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-02-01 | 02 | 1 | API-02 | unit | `mvn test -Dtest=PowerSystemTaskStrategyTest -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-02-02 | 02 | 1 | API-02 | unit | `mvn test -Dtest=WorldviewControllerTest#testGeneratePowerSystem -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-03-01 | 03 | 2 | API-03, DEP-03 | unit | `mvn test -Dtest=FactionTaskStrategyTest#testContextInjection -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-03-02 | 03 | 2 | DEP-01 | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateFactionNoGeography -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |
| 07-03-03 | 03 | 2 | DEP-02 | unit | `mvn test -Dtest=WorldviewControllerTest#testGenerateFactionNoPowerSystem -DfailIfNoTests=false` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/controller/WorldviewControllerTest.java` — stubs for API-01/02/03, DEP-01/02
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/GeographyTaskStrategyTest.java` — stubs for API-01
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/PowerSystemTaskStrategyTest.java` — stubs for API-02
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/FactionTaskStrategyTest.java` — stubs for DEP-03 context injection

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Full async generation flow (LLM call → XML parse → DB persist) | API-01, API-02, API-03 | Requires running LLM provider + DB | Call each endpoint via Swagger UI, verify data in DB |
| Dependency error message wording | DEP-01, DEP-02 | Non-functional (exact text) | Call faction endpoint without geo/power data, check error message |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
