---
phase: 16
slug: ai-chapter-planning
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-11
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (Spring Boot Test) |
| **Config file** | ai-factory-backend/pom.xml |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest="OutlineTaskStrategyTest,ForeshadowingServiceTest" -DfailIfNoTests=false` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
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
| 16-01-01 | 01 | 1 | AIP-01 | unit | `mvn test -Dtest="*Foreshadowing*Context*"` | ❌ W0 | ⬜ pending |
| 16-01-02 | 01 | 1 | AIP-01 | unit | `mvn test -Dtest="*PromptTemplate*Test"` | ❌ W0 | ⬜ pending |
| 16-02-01 | 02 | 1 | AIP-02 | unit | `mvn test -Dtest="*XmlParse*Test"` | ❌ W0 | ⬜ pending |
| 16-02-02 | 02 | 1 | AIP-03 | unit | `mvn test -Dtest="*Foreshadowing*Save*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Test stubs for foreshadowing context injection (AIP-01)
- [ ] Test stubs for XML parsing of <fs>/<fp> tags (AIP-02)
- [ ] Test stubs for batch foreshadowing persistence (AIP-03)
- [ ] Test data fixtures for foreshadowing entities with planted/planned volumes

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| LLM outputs valid <fs>/<fp> XML in actual response | AIP-02 | Requires real LLM call | Generate chapter plan, verify XML output contains foreshadowing tags |
| Template renders foreshadowing context correctly in prompt | AIP-01 | Requires real LLM call | Check generated prompt text contains active foreshadowing list |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
