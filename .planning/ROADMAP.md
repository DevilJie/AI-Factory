# Roadmap: AI Factory

## Milestones

- ✅ **v1.0.2 势力阵营结构化重构** — Phases 1-5 (shipped 2026-04-03)
- ✅ **v1.0.3 世界观生成任务拆分** — Phases 6-9 (shipped 2026-04-05)
- ✅ **v1.0.4 角色体系关联与提取增强** — Phase 10 (shipped 2026-04-06)
- 🔄 **v1.0.5 章节角色规划体系** — Phases 11-14 (in progress)

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
<summary>🔄 v1.0.5 章节角色规划体系 (Phases 11-14) — IN PROGRESS</summary>

- [x] **Phase 11: 数据基础 + 实体映射** — 数据库迁移、实体字段映射、DTO 合并、三级名称匹配 (completed 2026-04-07)
- [ ] **Phase 12: AI 规划输出 + XML 解析** — 章节规划模板升级、角色规划 XML 解析、名称匹配关联
- [ ] **Phase 13: 章节生成约束注入** — 提示词注入规划角色、严格约束语言、新角色自动创建
- [ ] **Phase 14: 前端展示 + 闭环验证** — 规划角色展示、规划-实际对比、闭环验证

## Phase Details

### Phase 11: 数据基础 + 实体映射
**Goal**: 数据库和实体层具备存储和关联规划角色的能力，为后续 AI 规划和生成提供数据基础
**Depends on**: Phase 10 (completed)
**Requirements**: CP-03, CP-04
**Success Criteria** (what must be TRUE):
  1. novel_chapter_plan 表包含 planned_characters JSON 列，NovelChapterPlan 实体映射 plannedCharacters 和 characterArcs 两个字段
  2. 用户在章节规划 API 响应中能看到 planned_characters 和 character_arcs 字段（即使为空也不报错）
  3. 系统能通过三级名称匹配（精确 -> 去后缀 -> 包含）将输入的角色名关联到数据库中已有的角色记录，匹配结果包含角色 ID
  4. 两个重复的 ChapterPlanXmlDto 类已合并为单一 DTO，所有引用点更新一致
**Plans**: 2 plans

Plans:
- [x] 11-01-PLAN.md — Wave 1: 数据库迁移 + 实体/DTO 字段映射 + DTO 删除合并
- [x] 11-02-PLAN.md — Wave 1: 三级名称匹配工具 NameMatchUtil (TDD)

### Phase 12: AI 规划输出 + XML 解析
**Goal**: 用户生成章节规划时，AI 输出包含角色规划信息，系统自动解析并持久化到数据库
**Depends on**: Phase 11
**Requirements**: CP-01, CP-02
**Success Criteria** (what must be TRUE):
  1. 用户生成章节规划后，AI 输出 XML 中每个章节包含角色规划标签（角色名、戏份梗概、重要程度）
  2. 系统从 AI 输出 XML 中正确提取角色规划数据并持久化为 planned_characters JSON 字段
  3. AI 规划输出的角色名通过三级名称匹配关联到数据库已有角色，匹配失败的以原始名称存储
  4. 用户查看已生成的章节规划时，planned_characters 字段不为空（包含解析后的角色数据）
**Plans**: TBD

### Phase 13: 章节生成约束注入
**Goal**: 章节生成严格按规划角色执行，避免 AI 自由发挥引入无关角色，新角色自动入库
**Depends on**: Phase 12
**Requirements**: CG-01, CG-02, CG-03
**Success Criteria** (what must be TRUE):
  1. 有规划角色的章节生成提示词仅包含规划角色信息，不包含全量角色列表
  2. 章节生成提示词中规划角色部分使用"必须遵循"约束语言，AI 输出应严格遵循规划的角色和戏份
  3. 无规划角色的章节生成仍走原有全量角色注入逻辑，不产生兼容性问题
  4. 规划中出现数据库中不存在的新角色名时，章节生成完成后系统自动创建角色记录并建立章节关联
**Plans**: TBD
**Research flag**: This phase modifies prompt construction logic. The constraint language and character filtering must be validated with real LLM calls during execution.

### Phase 14: 前端展示 + 闭环验证
**Goal**: 用户能在前端直观看到章节规划角色安排，并验证生成结果是否遵循了规划
**Depends on**: Phase 12 (API 数据就绪即可开始前端开发，不依赖 Phase 13)
**Requirements**: FE-01, FE-02
**Success Criteria** (what must be TRUE):
  1. 用户打开章节规划 Drawer 时，能看到规划角色列表卡片（显示角色名、戏份梗概、重要程度标签）
  2. 章节生成完成后，用户在章节详情中能看到规划角色与实际登场角色的对比视图，偏差角色有明显标记
  3. 规划角色卡片使用与现有 Drawer 一致的 Tailwind 样式，不破坏已有 UI 布局
**Plans**: TBD
**UI hint**: yes

</details>

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
| 10. 角色体系关联与提取增强 | v1.0.4 | 3/3 | Complete | 2026-04-06 |
| 11. 数据基础 + 实体映射 | v1.0.5 | 2/2 | Complete    | 2026-04-07 |
| 12. AI 规划输出 + XML 解析 | v1.0.5 | 0/? | Not started | - |
| 13. 章节生成约束注入 | v1.0.5 | 0/? | Not started | - |
| 14. 前端展示 + 闭环验证 | v1.0.5 | 0/? | Not started | - |
