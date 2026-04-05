---
phase: 6
slug: 06-独立提示词模板
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-03
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test |
| **Config file** | ai-factory-backend/src/test/resources/application-test.yml |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest="*PromptTemplate*Test" -Dspring.profiles.active=test` |
| **Full suite command** | `cd ai-factory-backend && mvn test -Dspring.profiles.active=test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | PROMPT-01 | unit | `mvn test -Dtest="GeographyPromptTemplateTest"` | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | PROMPT-02 | unit | `mvn test -Dtest="PowerSystemPromptTemplateTest"` | ❌ W0 | ⬜ pending |
| 06-01-03 | 01 | 1 | PROMPT-03 | unit | `mvn test -Dtest="FactionPromptTemplateTest"` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 2 | PROMPT-04 | unit | `mvn test -Dtest="WorldviewPromptTemplateUpdateTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/PromptTemplateServiceTest.java` — stubs for PROMPT-01~04
- [ ] Test data: V3 migration SQL fixture for baseline template data

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Template renders correct XML output for each module | PROMPT-01~03 | Requires LLM call to verify end-to-end prompt quality | Call executeTemplate with each new template code, inspect LLM XML response |
| Updated worldview template excludes removed modules | PROMPT-04 | Requires full worldview generation run | Generate worldview with updated template, verify no geography/power/faction content |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
