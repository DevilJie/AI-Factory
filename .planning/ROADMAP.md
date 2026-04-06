# Roadmap: AI Factory

## Milestones

- **v1.0.2 势力阵营结构化重构** — Phases 1-5 (shipped 2026-04-03)
- **v1.0.3 世界观生成任务拆分** — Phases 6-9 (shipped 2026-04-05)
- **v1.0.4 角色体系关联与提取增强** — Phase 10 (in progress)

## Phases

<details>
<summary>v1.0.2 势力阵营结构化重构 (Phases 1-5) — SHIPPED 2026-04-03</summary>

- [x] Phase 1: 数据基础 (2/2 plans) — completed 2026-04-01
- [x] Phase 2: 后端服务与 API (2/2 plans) — completed 2026-04-02
- [x] Phase 3: AI 集成与提示词迁移 (3/3 plans) — completed 2026-04-02
- [x] Phase 4: 前端树组件 (3/3 plans) — completed 2026-04-02
- [x] Phase 5: 关联管理界面 (2/2 plans) — completed 2026-04-03

</details>

<details>
<summary>v1.0.3 世界观生成任务拆分 (Phases 6-9) — SHIPPED 2026-04-05</summary>

- [x] Phase 6: 独立提示词模板 (1/1 plan) — completed 2026-04-03
- [x] Phase 7: 独立生成 API + 依赖校验 (2/2 plans) — completed 2026-04-03
- [x] Phase 8: 原有逻辑重构 (2/2 plans) — completed 2026-04-03
- [x] Phase 9: 前端独立生成按钮 (1/1 plan) — completed 2026-04-03

</details>

### Phase 10: 角色体系关联与提取增强

**Goal:** 完善角色提取功能，建立角色与力量体系/势力阵营的结构化关联，修复角色类型识别问题
**Depends on:** Phases 1-9 (completed)

**Scope:**
1. 新建角色-修炼体系关联表 (character_id, power_system_id, current_realm_id, current_sub_realm_id, 唯一索引 character_id+system_id)
2. 识别角色与阵营势力的关联关系，更新 llm_chapter_character_extract 提示词模板明确生成势力阵营信息
3. 修复角色类型识别问题 (protagonist/supporting/antagonist/npc 无法准确识别)

**Plans:** 3/3 plans complete

Plans:
- [x] 10-01-PLAN.md — Wave 1: 后端数据层与提取逻辑 — completed 2026-04-06
- [x] 10-02-PLAN.md — Wave 2: 提示词模板v2与角色类型修复 — completed 2026-04-06
- [x] 10-03-PLAN.md — Wave 3: 前端Drawer Tab与列表增强 — completed 2026-04-06

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. 数据基础 | v1.0.2 | 2/2 | Complete | 2026-04-01 |
| 2. 后端服务与 API | v1.0.2 | 2/2 | Complete | 2026-04-02 |
| 3. AI 集成与提示词迁移 | v1.0.2 | 3/3 | Complete | 2026-04-02 |
| 4. 前端树组件 | v1.0.2 | 3/3 | Complete | 2026-04-02 |
| 5. 关联管理界面 | v1.0.2 | 2/2 | Complete | 2026-04-03 |
| 6. 独立提示词模板 | v1.0.3 | 1/1 | Complete | 2026-04-03 |
| 7. 独立生成 API + 依赖校验 | v1.0.3 | 2/2 | Complete | 2026-04-03 |
| 8. 原有逻辑重构 | v1.0.3 | 2/2 | Complete | 2026-04-03 |
| 9. 前端独立生成按钮 | v1.0.3 | 1/1 | Complete | 2026-04-03 |
| 10. 角色体系关联与提取增强 | v1.0.4 | 3/3 | Complete    | 2026-04-06 |
