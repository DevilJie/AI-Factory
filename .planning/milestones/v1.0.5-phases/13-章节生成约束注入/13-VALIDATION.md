---
phase: 13
slug: 章节生成约束注入
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-09
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (via Spring Boot Test) |
| **Config file** | pom.xml (spring-boot-starter-test) |
| **Quick run command** | `cd ai-factory-backend && mvn test -Dtest=PromptTemplateBuilderTest -q` |
| **Full suite command** | `cd ai-factory-backend && mvn test -q` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -Dtest=PromptTemplateBuilderTest -q`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | CG-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testPlannedCharacterInjection` | W0 | pending |
| 13-01-02 | 01 | 1 | CG-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNullPlannedCharactersFallback` | W0 | pending |
| 13-01-03 | 01 | 1 | CG-01 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testEmptyPlannedCharactersFallback` | W0 | pending |
| 13-01-04 | 01 | 1 | CG-02 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintLanguagePresent` | W0 | pending |
| 13-01-05 | 01 | 1 | CG-02 | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNpcAllowancePresent` | W0 | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilderTest.java` -- unit tests for CG-01 and CG-02

*No shared fixtures needed -- each test constructs its own chapterPlan with appropriate plannedCharacters JSON.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| LLM adheres to planned character constraints | CG-02 | Requires real LLM call | Generate chapter with planned characters, verify output includes all planned characters |
| NPC characters appear naturally | D-04 | Requires real LLM call | Generate chapter, verify NPC-like characters can appear alongside planned characters |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
