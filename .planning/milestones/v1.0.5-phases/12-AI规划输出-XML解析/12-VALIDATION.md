---
phase: 12
slug: ai-xml
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-08
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito |
| **Config file** | ai-factory-backend/pom.xml |
| **Quick run command** | `mvn test -pl ai-factory-backend -Dtest=*Test -DfailIfNoTests=false` |
| **Full suite command** | `mvn test -pl ai-factory-backend` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -pl ai-factory-backend -Dtest=*Test -DfailIfNoTests=false`
- **After every plan wave:** Run `mvn test -pl ai-factory-backend`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | CP-01 | unit | `mvn test -pl ai-factory-backend -Dtest=ChapterGenerationTaskStrategyTest` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | CP-01 | unit | `mvn test -pl ai-factory-backend -Dtest=ChapterGenerationTaskStrategyTest` | ❌ W0 | ⬜ pending |
| 12-02-01 | 02 | 2 | CP-01 | unit | `mvn test -pl ai-factory-backend -Dtest=*Test` | ✅ | ⬜ pending |
| 12-02-02 | 02 | 2 | CP-02 | integration | `mvn test -pl ai-factory-backend -Dtest=*Test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategyTest.java` — stubs for CP-01, CP-02

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AI outputs valid character planning XML | CP-01 | Requires real LLM call | Generate a chapter plan and verify `<ch>` tags in output |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
