# Roadmap: AI Factory

## Milestones

- ✅ **v1.0.2 势力阵营结构化重构** — Phases 1-5 (shipped 2026-04-03)
- ✅ **v1.0.3 世界观生成任务拆分** — Phases 6-9 (shipped 2026-04-05)
- ✅ **v1.0.4 角色体系关联与提取增强** — Phase 10 (shipped 2026-04-06)
- ✅ **v1.0.5 章节角色规划体系** — Phases 11-14 (shipped 2026-04-10)
- 🚧 **v1.0.6 伏笔管理** — Phases 15-19 (in progress)

## Current: v1.0.6 伏笔管理

**Status:** IN PROGRESS

| Phase | Name | Plans | Status |
|-------|------|-------|--------|
| 15 | Data Foundation | Complete    | 2026-04-10 |
| 16 | AI Chapter Planning | Complete    | 2026-04-11 |
| 17 | 1/1 | Complete   | 2026-04-11 |
| 18 | Frontend Chapter Foreshadowing | 0/2 | Not Started |
| 19 | Frontend Project Management | 0/1 | Not Started |

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

<details>
<summary>✅ v1.0.4 角色体系关联与提取增强 (Phase 10) — SHIPPED 2026-04-06</summary>

- [x] Phase 10: 角色体系关联与提取增强 (3/3 plans) — completed 2026-04-06

</details>

<details>
<summary>✅ v1.0.5 章节角色规划体系 (Phases 11-14) — SHIPPED 2026-04-10</summary>

- [x] **Phase 11: 数据基础 + 实体映射** — completed 2026-04-07
- [x] **Phase 12: AI 规划输出 + XML 解析** — completed 2026-04-08
- [x] **Phase 13: 章节生成约束注入** — completed 2026-04-09
- [x] **Phase 14: 前端展示 + 闭环验证** — completed 2026-04-09

</details>

### 🚧 v1.0.6 伏笔管理 (In Progress)

**Milestone Goal:** 激活 novel_foreshadowing 表为章节规划和生成的核心驱动，让 LLM 自动规划伏笔埋设/回收，用户可跨卷管理伏笔。

- [x] **Phase 15: Data Foundation** — 伏笔表扩展 + 旧字段删除 + 距离校验 (completed 2026-04-10)
- [x] **Phase 16: AI Chapter Planning** — 章节规划伏笔上下文注入 + XML 输出 + 解析保存 (completed 2026-04-11)
- [x] **Phase 17: AI Generation Constraints** — 章节生成伏笔约束注入 (completed 2026-04-11)
- [ ] **Phase 18: Frontend Chapter Foreshadowing** — ChapterPlanDrawer 伏笔管理区
- [ ] **Phase 19: Frontend Project Management** — 项目级伏笔总览页面 + 健康度评分

## v1.0.6 伏笔管理 (Phases 15-19) — IN PROGRESS

- [x] Phase 15: 数据基础 (2/2 plans) — completed 2026-04-11
- [x] Phase 16: AI 章节规划伏笔 (completed 2026-04-11)
- [ ] Phase 17: AI 生成伏笔约束
- [ ] Phase 18: 前端章节伏笔管理
- [ ] Phase 19: 前端项目伏笔管理

## Phase Details

### Phase 15: Data Foundation
**Goal**: novel_foreshadowing 表支持跨卷引用，旧伏笔字段彻底清理，距离校验防止瞬发回收
**Depends on**: Nothing (first phase of v1.0.6, builds on existing novel_foreshadowing table)
**Requirements**: DATA-01, DATA-02, DATA-03
**Success Criteria** (what must be TRUE):
  1. novel_foreshadowing 表包含 planted_volume 和 planned_callback_volume 字段，可按分卷查询伏笔
  2. novel_chapter_plan 的 foreshadowingSetup/foreshadowingPayoff 字段从数据库列、实体类、DTO、前端类型中完全删除
  3. 创建/更新伏笔时，若埋设章节与回收章节距离不足 N 章，返回校验错误
**Plans:** 2/2 plans complete

Plans:
- [ ] 15-01-PLAN.md — 伏笔表扩展(分卷字段) + 距离校验 + 回调章节上限校验 [DATA-01, DATA-03]
- [ ] 15-02-PLAN.md — 彻底删除 foreshadowingSetup/foreshadowingPayoff (DB+实体+DTO+前端) [DATA-02]

### Phase 16: AI Chapter Planning
**Goal**: LLM 章节规划时感知已有伏笔状态，自动输出新伏笔的埋设和回收决策，解析并持久化到伏笔表
**Depends on**: Phase 15
**Requirements**: AIP-01, AIP-02, AIP-03
**Success Criteria** (what must be TRUE):
  1. 章节规划提示词包含当前活跃伏笔列表（待埋设 + 待回收），LLM 能看到伏笔上下文
  2. LLM 章节规划输出包含 <fs>/<fp> XML 标签，含标题、描述、类型、布局线等子标签
  3. DOM 解析器正确解析 <fs>/<fp> 标签，批量创建/更新 novel_foreshadowing 记录
**Plans**: 2/2 complete

Plans:
- [x] 16-01: Foreshadowing context injection into chapter planning prompts [AIP-01]
- [x] 16-02: Foreshadowing XML parsing and persistence [AIP-02, AIP-03]

### Phase 17: AI Generation Constraints
**Goal**: 章节生成时注入伏笔硬性创作指令，确保 LLM 按规划埋设或回收伏笔
**Depends on**: Phase 16
**Requirements**: AIC-01, AIC-02
**Success Criteria** (what must be TRUE):
  1. 章节生成提示词包含"本章节需埋设的伏笔"和"本章节需回收的伏笔"约束段落
  2. 约束文本仅包含当前章节相关项，格式简洁，不会导致上下文窗口溢出
**Plans:** 1/1 plans complete

Plans:
- [x] 17-01-PLAN.md — Foreshadowing constraint injection + batch status update [AIC-01, AIC-02]

### Phase 18: Frontend Chapter Foreshadowing
**Goal**: 用户在章节规划抽屉中查看和管理本章节的伏笔埋设/回收
**Depends on**: Phase 15
**Requirements**: FC-01, FC-02
**Success Criteria** (what must be TRUE):
  1. ChapterPlanDrawer 伏笔区展示本章节需埋设和需回收的伏笔卡片，替代原有 textarea
  2. 用户可在伏笔管理区直接编辑伏笔内容、添加新伏笔、删除已有伏笔
**Plans**: TBD

Plans:
- [ ] 18-01: TBD
- [ ] 18-02: TBD
**UI hint**: yes

### Phase 19: Frontend Project Management
**Goal**: 用户在项目级别查看和管理所有伏笔，掌握伏笔体系健康度
**Depends on**: Phase 18
**Requirements**: FP-01, FP-02, FP-03, FP-04
**Success Criteria** (what must be TRUE):
  1. 侧边栏显示「伏笔管理」菜单项，点击进入项目级伏笔总览页面
  2. 伏笔总览页面展示所有伏笔列表，支持按类型、布局线、状态、分卷筛选
  3. 用户可在总览页面新增、删除、修改伏笔（包括修改回收分卷/章节）
  4. 伏笔总览页面展示健康度评分（埋设/回收比例、逾期数、平均埋设-回收距离）
**Plans**: TBD

Plans:
- [ ] 19-01: TBD
- [ ] 19-02: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 15 → 16 → 17 → 18 → 19

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 15. Data Foundation | v1.0.6 | 2/2 | Complete | 2026-04-10 |
| 16. AI Chapter Planning | v1.0.6 | 2/2 | Complete | 2026-04-11 |
| 17. AI Generation Constraints | v1.0.6 | 0/1 | Planned | - |
| 18. Frontend Chapter Foreshadowing | v1.0.6 | 0/? | Not started | - |
| 19. Frontend Project Management | v1.0.6 | 0/? | Not started | - |
