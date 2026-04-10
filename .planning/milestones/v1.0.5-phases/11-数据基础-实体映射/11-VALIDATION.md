---
phase: 11
slug: 数据基础-实体映射
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-07
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (via spring-boot-starter-test) |
| **Config file** | none — annotation-based (@ExtendWith(MockitoExtension.class)) |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest=NameMatchUtilTest -q` |
| **Full suite command** | `cd ai-factory-backend && mvn test -q` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -q`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | CP-03 | unit | `cd ai-factory-backend && mvn test -Dtest=NovelChapterPlanTest -q` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | CP-03 | unit | `cd ai-factory-backend && mvn test -Dtest=ChapterPlanDtoTest -q` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 1 | CP-04 | unit | `cd ai-factory-backend && mvn test -Dtest=NameMatchUtilTest -q` | ❌ W0 | ⬜ pending |
| 11-02-02 | 02 | 1 | CP-04 | unit | `cd ai-factory-backend && mvn test -Dtest=NameMatchUtilTest -q` | ❌ W0 | ⬜ pending |
| 11-03-01 | 03 | 1 | SC-4 | compile | `cd ai-factory-backend && mvn compile -q` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ai-factory-backend/src/test/java/com/aifactory/common/NameMatchUtilTest.java` — covers CP-04 (three-tier matching: exact, suffix-strip, contains, no-match, null input, single-char edge cases)
- [ ] `ai-factory-backend/src/test/java/com/aifactory/entity/NovelChapterPlanTest.java` — covers CP-03 (entity field existence and type)
- [ ] SQL migration script for planned_characters column

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| API response includes planned_characters field | CP-03 | Requires running server and HTTP client | Start server, GET /api/chapter-plan/{id}, verify JSON response contains "plannedCharacters" and "characterArcs" fields |
| Suffix list covers common honorifics | CP-04 | Subjective completeness check | Review CHARACTER_SUFFIXES list against common xianxia/wuxia honorifics |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
