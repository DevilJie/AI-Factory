# Requirements: 世界观生成任务拆分

**Defined:** 2026-04-03
**Core Value:** 独立生成任务让用户能按需单独重新生成地理环境、力量体系或阵营势力，而不必每次重新生成整个世界观

## v1.0.3 Requirements

### 提示词拆分

- [x] **PROMPT-01**: 地理环境拥有独立 AI 提示词模板，仅包含地理环境相关指令和上下文
- [x] **PROMPT-02**: 力量体系拥有独立 AI 提示词模板，仅包含力量体系相关指令和上下文
- [x] **PROMPT-03**: 阵营势力拥有独立 AI 提示词模板，仅包含阵营势力相关指令和上下文（含已生成的地理环境和力量体系数据作为上下文输入）
- [x] **PROMPT-04**: 原有世界观整体生成提示词模板剔除地理环境、力量体系、阵营势力三个模块的生成指令

### 独立生成 API

- [ ] **API-01**: 提供地理环境独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成地理环境并入库
- [ ] **API-02**: 提供力量体系独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成力量体系并入库
- [ ] **API-03**: 提供阵营势力独立生成 REST 接口，接收项目 ID，调用独立提示词模板生成阵营势力并入库

### 依赖校验

- [ ] **DEP-01**: 阵营势力生成接口增加前置校验 — 若该项目未生成地理环境数据则拒绝并返回提示
- [ ] **DEP-02**: 阵营势力生成接口增加前置校验 — 若该项目未生成力量体系数据则拒绝并返回提示
- [ ] **DEP-03**: 阵营势力独立提示词模板中自动注入已生成的地理环境和力量体系结构化数据作为上下文

### 原有逻辑重构

- [ ] **REFACT-01**: WorldviewTaskStrategy 整体生成流程重构为：先调用三个独立生成任务，再调用剔除后的世界观提示词生成剩余内容
- [ ] **REFACT-02**: 整体生成流程中三个模块的生成结果汇总后仍作为完整世界观数据返回给前端

### 前端生成按钮

- [ ] **UI-01**: 地理环境区域增加独立「AI 生成」按钮，点击调用地理环境独立生成接口，生成完成后刷新树形数据
- [ ] **UI-02**: 力量体系区域增加独立「AI 生成」按钮，点击调用力量体系独立生成接口，生成完成后刷新树形数据
- [ ] **UI-03**: 阵营势力区域增加独立「AI 生成」按钮，点击调用阵营势力独立生成接口，生成完成后刷新树形数据
- [ ] **UI-04**: 独立生成按钮的加载状态和错误提示（含依赖校验失败的提示信息）

## Out of Scope

| Feature | Reason |
|---------|--------|
| 独立生成的撤销/回滚 | 复杂度高，用户可手动删除或重新生成 |
| 批量独立生成调度 | 当前场景按需生成即可，不需要批量调度 |
| 生成历史版本管理 | 超出本次范围，保持简单 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PROMPT-01 | Phase 6 | Complete |
| PROMPT-02 | Phase 6 | Complete |
| PROMPT-03 | Phase 6 | Complete |
| PROMPT-04 | Phase 6 | Complete |
| API-01 | Phase 7 | Pending |
| API-02 | Phase 7 | Pending |
| API-03 | Phase 7 | Pending |
| DEP-01 | Phase 7 | Pending |
| DEP-02 | Phase 7 | Pending |
| DEP-03 | Phase 7 | Pending |
| REFACT-01 | Phase 8 | Pending |
| REFACT-02 | Phase 8 | Pending |
| UI-01 | Phase 9 | Pending |
| UI-02 | Phase 9 | Pending |
| UI-03 | Phase 9 | Pending |
| UI-04 | Phase 9 | Pending |

**Coverage:**
- v1.0.3 requirements: 16 total
- Mapped to phases: 16
- Unmapped: 0

---
*Requirements defined: 2026-04-03*
*Last updated: 2026-04-03 after roadmap creation*
