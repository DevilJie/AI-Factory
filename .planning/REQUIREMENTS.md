# Requirements: AI Factory

**Defined:** 2026-04-07
**Core Value:** 世界观的模块化独立生成让用户能按需单独重新生成地理环境、力量体系或阵营势力

## v1.0.5 Requirements

Requirements for milestone v1.0.5 章节角色规划体系. Each maps to roadmap phases.

### 章节规划 (CP)

- [x] **CP-01**: 用户生成章节规划时，AI 输出包含登场角色列表（角色名、戏份梗概、重要程度）
- [x] **CP-02**: 用户查看章节规划时，能看到解析并持久化的规划角色信息（planned_characters JSON 字段）
- [x] **CP-03**: novel_chapter_plan 实体映射 planned_characters JSON 字段和已有 character_arcs JSON 字段
- [x] **CP-04**: 系统通过三级名称匹配（精确→简写→模糊）将 AI 输出的角色名关联到数据库已有角色

### 章节生成 (CG)

- [ ] **CG-01**: 章节生成提示词有规划角色时，仅注入规划角色信息（替换全量角色列表注入），避免矛盾信号
- [ ] **CG-02**: 章节生成提示词使用"必须遵循"约束语言，强制 AI 严格按规划的角色和戏份生成内容
- [ ] **CG-03**: 规划中包含数据库中不存在的新角色时，章节生成后自动创建角色并建立章节关联

### 前端展示 (FE)

- [ ] **FE-01**: 用户在章节规划 Drawer 中能看到规划角色列表及其戏份梗概
- [ ] **FE-02**: 用户章节生成后能看到规划角色 vs 实际登场角色的对比，标记偏差

## Future Requirements

Deferred to future release. Tracked but not in current roadmap.

### 角色出场统计

- **STAT-01**: 用户能看到角色在各卷/各章的出场频率统计
- **STAT-02**: 用户能看到角色出场时间线视图

### 角色关系图谱

- **REL-01**: 用户能看到角色之间的关系网络图谱可视化
- **REL-02**: AI 生成章节时自动维护角色关系变化

### 高级规划

- **ADV-01**: 系统自动检测角色规划中的冲突（同一角色同时出现在不同地点）
- **ADV-02**: 支持跨章节的角色弧光追踪和可视化
- **ADV-03**: AI 建议章节规划中的角色安排优化

## Out of Scope

| Feature | Reason |
|---------|--------|
| 角色关系图谱可视化 | 前端成本高，本次聚焦规划-生成闭环 |
| 角色出场统计仪表盘 | 需要聚合查询优化，超出本次范围 |
| 跨章节角色弧光可视化 | 需要时间线组件，复杂度高 |
| AI 自动优化角色安排 | 需要先有基础规划数据才能优化 |
| 角色情绪追踪曲线 | 需要结构化情绪数据，超出本次范围 |
| 多角色视角章节规划 | 网文场景主要是单一视角，暂不需要 |
| 章节规划模板版本管理 | 模板变更频率低，手动管理即可 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CP-01 | Phase 12 | Complete |
| CP-02 | Phase 12 | Complete |
| CP-03 | Phase 11 | Complete |
| CP-04 | Phase 11 | Complete |
| CG-01 | Phase 13 | Pending |
| CG-02 | Phase 13 | Pending |
| CG-03 | Phase 13 | Pending |
| FE-01 | Phase 14 | Pending |
| FE-02 | Phase 14 | Pending |

**Coverage:**
- v1.0.5 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-04-07*
*Last updated: 2026-04-07 after phase 11 completion*
