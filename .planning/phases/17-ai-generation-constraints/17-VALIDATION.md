---
phase: 17
slug: ai-generation-constraints
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-11
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito |
| **Config file** | none (annotation-based via `@ExtendWith(MockitoExtension.class)`) |
| **Quick run command** | `cd ai-factory-backend && mvn test -Dtest=PromptTemplateBuilderTest -DfailIfNoTests=false` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -Dtest=PromptTemplateBuilderTest -DfailIfNoTests=false`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 17-01-01 | 01 | 1 | AIC-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testForeshadowingConstraintInjection` | Wave 0 | pending |
| 17-01-02 | 01 | 1 | AIC-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintLanguageStyle` | Wave 0 | pending |
| 17-01-03 | 01 | 1 | AIC-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNoForeshadowingConstraint` | Wave 0 | pending |
| 17-02-01 | 01 | 1 | AIC-02 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintItemFormat` | Wave 0 | pending |
| 17-02-02 | 01 | 1 | AIC-02 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testOnlyCurrentChapterForeshadowing` | Wave 0 | pending |
| 17-03-01 | 01 | 1 | D-04 | unit | `mvn test -Dtest=ForeshadowingServiceTest#testBatchUpdatePlantedStatus` | Wave 0 | pending |
| 17-03-02 | 01 | 1 | D-04 | unit | `mvn test -Dtest=ForeshadowingServiceTest#testBatchUpdateResolvedStatus` | Wave 0 | pending |
| 17-03-03 | 01 | 1 | D-05 | unit | `mvn test -Dtest=ForeshadowingServiceTest#testNoRollbackOnRegeneration` | Wave 0 | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] `PromptTemplateBuilderTest` — add foreshadowing constraint tests (AIC-01, AIC-02)
- [ ] `ForeshadowingServiceTest` — add batch status update tests (D-04, D-05)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| LLM actually follows foreshadowing constraint directives in generated text | AIC-01 | Requires LLM execution and qualitative review | Generate a chapter with foreshadowing constraints and verify the output includes the foreshadowing elements naturally |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
