---
phase: 1
slug: 数据基础
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-01
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (already in project) |
| **Config file** | src/test/resources/application-test.yml |
| **Quick run command** | `cd ai-factory-backend && mvn compile -q` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn compile -q`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | DB-01, DB-07 | manual | `mysql < sql/faction_migration.sql` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | DB-02 | manual | `mysql -e "DESC novel_faction"` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 1 | DB-06 | compile | `cd ai-factory-backend && mvn compile -q` | ❌ W0 | ⬜ pending |
| 01-02-02 | 02 | 1 | DB-03, DB-04, DB-05 | compile | `cd ai-factory-backend && mvn compile -q` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- Existing infrastructure covers all phase requirements. This is primarily DDL + entity class work validated by compilation.

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| SQL DDL execution succeeds | DB-01, DB-07 | Requires running MySQL instance | Execute `sql/faction_migration.sql` and verify tables created |
| novel_faction table has correct columns | DB-02 | DDL structural check | Run `DESC novel_faction` and verify column list |
| Association tables created | DB-03, DB-04, DB-05 | DDL structural check | Run `DESC` on each association table |
| NovelWorldview.forces marked transient | DB-06 | Compile-time check | `mvn compile` passes, forces field has @TableField(exist=false) |

*All phase behaviors have automated or compile-time verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
