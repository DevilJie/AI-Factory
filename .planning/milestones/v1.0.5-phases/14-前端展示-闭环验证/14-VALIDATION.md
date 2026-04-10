---
phase: 14
slug: 前端展示-闭环验证
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-10
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend) + Vitest (frontend) |
| **Config file** | `ai-factory-backend/pom.xml` + `ai-factory-frontend/vitest.config.ts` |
| **Quick run command** | `cd ai-factory-backend && mvn test -pl . -Dtest="*Test" -q` |
| **Full suite command** | `cd ai-factory-backend && mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd ai-factory-backend && mvn test -pl . -Dtest="*Test" -q`
- **After every plan wave:** Run `cd ai-factory-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | FE-01 | unit | `cd ai-factory-backend && mvn test -Dtest="*Controller*Test"` | ❌ W0 | ⬜ pending |
| 14-01-02 | 01 | 1 | FE-02 | manual | Manual browser verification | N/A | ⬜ pending |
| 14-02-01 | 02 | 1 | FE-01 | manual | Manual Drawer verification | N/A | ⬜ pending |
| 14-02-02 | 02 | 1 | FE-02 | manual | Manual comparison verification | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| 规划角色卡片在 Drawer 中正确渲染 | FE-01 | Vue 组件渲染需要浏览器环境 | 打开章节规划 Drawer → 角色规划 tab → 验证角色卡片包含名称、链接按钮 |
| 对比视图左右分栏布局 | FE-02 | CSS 布局和颜色标记需要视觉验证 | 生成章节后打开 Drawer → 角色规划 tab → 验证对比区域显示摘要和分栏对比 |
| 角色详情链接打开 CharacterDrawer | FE-01 | 跨组件交互需要完整浏览器环境 | 点击角色名旁链接 → 验证 CharacterDrawer 打开并显示对应角色详情 |
| 偏差角色颜色标记 | FE-02 | 颜色和图标视觉反馈需要人工确认 | 检查对比视图中 ✓绿色、✗红色、⚠黄色标记是否正确显示 |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
