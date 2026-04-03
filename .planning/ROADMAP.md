# Roadmap: AI Factory

## Milestones

- ✅ **v1.0.2 势力阵营结构化重构** — Phases 1-5 (shipped 2026-04-03)
- 🚧 **v1.0.3 世界观生成任务拆分** — Phases 6-9 (in progress)

## Phases

<details>
<summary>✅ v1.0.2 势力阵营结构化重构 (Phases 1-5) — SHIPPED 2026-04-03</summary>

- [x] Phase 1: 数据基础 (2/2 plans) — completed 2026-04-01
- [x] Phase 2: 后端服务与 API (2/2 plans) — completed 2026-04-02
- [x] Phase 3: AI 集成与提示词迁移 (3/3 plans) — completed 2026-04-02
- [x] Phase 4: 前端树组件 (3/3 plans) — completed 2026-04-02
- [x] Phase 5: 关联管理界面 (2/2 plans) — completed 2026-04-03

</details>

### 🚧 v1.0.3 世界观生成任务拆分 (In Progress)

**Milestone Goal:** 将地理环境、力量体系、阵营势力的 AI 生成从单一提示词拆分为 3 个独立任务，各配独立提示词模板和 REST API，前端增加独立生成按钮。

- [x] **Phase 6: 独立提示词模板** - 为地理环境、力量体系、阵营势力各创建独立 AI 提示词模板，并精简原有整体生成模板
- [x] **Phase 7: 独立生成 API + 依赖校验** - 提供 3 个独立生成 REST 接口，阵营势力生成前校验地理和力量体系依赖
- [ ] **Phase 8: 原有逻辑重构** - 将 WorldviewTaskStrategy 整体生成流程重构为组合调用三个独立生成任务
- [ ] **Phase 9: 前端独立生成按钮** - 地理环境、力量体系、阵营势力各增加独立 AI 生成按钮及加载/错误状态

## Phase Details

### Phase 6: 独立提示词模板
**Goal**: 每个世界观子模块拥有独立的 AI 提示词模板，可单独调用生成各自的内容
**Depends on**: Nothing (uses existing prompt template infrastructure)
**Requirements**: PROMPT-01, PROMPT-02, PROMPT-03, PROMPT-04
**Success Criteria** (what must be TRUE):
  1. 地理环境拥有独立提示词模板，仅包含地理环境生成指令，调用后能生成地理环境 XML 数据
  2. 力量体系拥有独立提示词模板，仅包含力量体系生成指令，调用后能生成力量体系结构化数据
  3. 阵营势力拥有独立提示词模板，包含已生成的地理环境和力量体系作为上下文输入，调用后能生成阵营势力 XML 数据
  4. 原有世界观整体生成模板中地理环境、力量体系、阵营势力三个模块的生成指令已被移除，整体模板仅生成剩余内容
**Plans**: 1 plan

Plans:
- [x] 06-01-PLAN.md — 创建 V4 SQL 迁移：3 个独立模板 INSERT + 统一模板 UPDATE（覆盖 PROMPT-01~04 全部需求）

### Phase 7: 独立生成 API + 依赖校验
**Goal**: 用户可通过独立 REST 接口分别生成地理环境、力量体系、阵营势力，阵营势力生成前自动校验依赖
**Depends on**: Phase 6
**Requirements**: API-01, API-02, API-03, DEP-01, DEP-02, DEP-03
**Success Criteria** (what must be TRUE):
  1. 调用地理环境独立生成接口，传入项目 ID 后，系统使用独立提示词生成地理环境数据并入库
  2. 调用力量体系独立生成接口，传入项目 ID 后，系统使用独立提示词生成力量体系数据并入库
  3. 调用阵营势力独立生成接口时，若该项目尚未生成地理环境数据，接口返回明确的错误提示而非空数据
  4. 调用阵营势力独立生成接口时，若该项目尚未生成力量体系数据，接口返回明确的错误提示而非空数据
  5. 阵营势力独立生成成功时，提示词中自动包含该项目已生成的地理环境和力量体系结构化数据作为上下文
**Plans**: 2 plans

Plans:
- [x] 07-01-PLAN.md — 创建 GeographyTaskStrategy + PowerSystemTaskStrategy（API-01, API-02）
- [x] 07-02-PLAN.md — 创建 FactionTaskStrategy + WorldviewController 3个端点（API-03, DEP-01, DEP-02, DEP-03）

### Phase 8: 原有逻辑重构
**Goal**: 世界观整体生成流程内部改为组合调用三个独立生成任务，对外行为不变
**Depends on**: Phase 7
**Requirements**: REFACT-01, REFACT-02
**Success Criteria** (what must be TRUE):
  1. 调用原有世界观整体生成接口时，后端内部依次调用地理环境、力量体系、阵营势力三个独立生成任务，最后用精简后的模板生成剩余内容
  2. 整体生成完成后，前端收到的响应包含完整的地理环境、力量体系、阵营势力及剩余世界观数据，与重构前行为一致
**Plans**: 2 plans

Plans:
- [x] 08-01-PLAN.md — 提取 WorldviewXmlParser 公共工具类 + 重构 GeographyTaskStrategy/FactionTaskStrategy 调用它（REFACT-01 重复代码清理）
- [x] 08-02-PLAN.md — 重构 WorldviewTaskStrategy 为 9 步骤编排 + 最终结果回查拼装（REFACT-01, REFACT-02）

### Phase 9: 前端独立生成按钮
**Goal**: 用户可在各世界观子模块区域点击独立按钮单独重新生成，无需重新生成整个世界观
**Depends on**: Phase 7
**Requirements**: UI-01, UI-02, UI-03, UI-04
**Success Criteria** (what must be TRUE):
  1. 地理环境区域显示独立「AI 生成」按钮，点击后调用地理环境独立生成接口，完成后树形数据自动刷新
  2. 力量体系区域显示独立「AI 生成」按钮，点击后调用力量体系独立生成接口，完成后树形数据自动刷新
  3. 阵营势力区域显示独立「AI 生成」按钮，点击后调用阵营势力独立生成接口，完成后树形数据自动刷新
  4. 生成过程中按钮显示加载状态，生成失败时显示错误提示（含依赖校验失败的明确提示信息）
**UI hint**: yes
**Plans**: TBD

Plans:
- [ ] 09-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 6 → 7 → 8 → 9

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 6. 独立提示词模板 | v1.0.3 | 0/? | Planning complete | - |
| 7. 独立生成 API + 依赖校验 | v1.0.3 | 2/2 | Complete | 2026-04-03 |
| 8. 原有逻辑重构 | v1.0.3 | 0/2 | Planning complete | - |
| 9. 前端独立生成按钮 | v1.0.3 | 0/? | Not started | - |
