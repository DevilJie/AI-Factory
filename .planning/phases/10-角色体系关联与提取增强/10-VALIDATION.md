---
phase: 10
slug: 角色体系关联与提取增强
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-06
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (backend), Vitest (frontend) |
| **Config file** | `ai-factory-backend/pom.xml` (backend), `ai-factory-frontend/package.json` (frontend) |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest=*CharacterExtract* -DfailIfNoTests=false` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -Dtest=*Character*,*Faction* -DfailIfNoTests=false`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | D-01/D-02/D-03 | unit | `mvn test -Dtest=*CharacterPowerSystem*` | ❌ W0 | ⬜ pending |
| 10-01-02 | 01 | 1 | D-04/D-05/D-06 | unit | `mvn test -Dtest=*CharacterExtract*` | ❌ W0 | ⬜ pending |
| 10-02-01 | 02 | 1 | D-07/D-08/D-09 | unit | `mvn test -Dtest=*CharacterExtract*` | ❌ W0 | ⬜ pending |
| 10-02-02 | 02 | 1 | D-13 | unit | `mvn test -Dtest=*PromptTemplate*` | ❌ W0 | ⬜ pending |
| 10-03-01 | 03 | 2 | D-10/D-11/D-12 | unit | `mvn test -Dtest=*CharacterService*` | ❌ W0 | ⬜ pending |
| 10-03-02 | 03 | 2 | D-10/D-11 | integration | manual — UI verification | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/CharacterPowerSystemServiceTest.java` — stubs for D-01/D-02/D-03
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/ChapterCharacterExtractServiceTest.java` — stubs for D-04/D-05/D-06/D-07/D-08/D-09

*Frontend tests: existing infrastructure covers UI verification needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Drawer Tab displays power systems and factions correctly | D-10 | Visual UI verification | Open character detail drawer, verify new tabs render with data |
| Character list card shows "境界 · 势力" format | D-11 | Visual UI verification | View character list, check card shows cultivation + faction info |
| Prompt template generates correct FC tags | D-13 | Requires LLM invocation | Run chapter extract, verify XML output includes FC elements |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
