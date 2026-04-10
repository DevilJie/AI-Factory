# Phase 15: Data Foundation - Context

**Gathered:** 2026-04-10
**Status:** Ready for planning

<domain>
## Phase Boundary

扩展 novel_foreshadowing 表支持跨卷引用，彻底删除 novel_chapter_plan 的旧伏笔字段，新增伏笔距离校验防止瞬发回收。具体：
1. novel_foreshadowing 新增 plantedVolume / plannedCallbackVolume 字段（Integer），与现有 plantedChapter / plannedCallbackChapter 组合定位
2. 章节编号改为全局序号（跨卷递增），距离校验基于全局序号计算
3. 彻底删除 novel_chapter_plan 的 foreshadowingSetup / foreshadowingPayoff（DB 列 + Java 实体 + DTO + 前端类型）
4. 距离校验：默认最小间隔 3 章，dark（暗线）伏笔豁免校验

不包含：AI 规划输出伏笔（Phase 16）、前端伏笔管理 UI（Phase 18/19）。
</domain>

<decisions>
## Implementation Decisions

### 分卷字段设计
- **D-01:** plantedVolume + plantedChapter 组合定位埋设点，plannedCallbackVolume + plannedCallbackChapter 组合定位回收点。四个 Integer 字段协同工作
- **D-02:** 埋设字段（plantedVolume + plantedChapter）允许 null（表示无伏笔）；如果埋设字段有值，回收字段（plannedCallbackVolume + plannedCallbackChapter）不可为 null
- **D-03:** plannedCallbackChapter 不能超过该卷规划的章节数量。校验时需查询 novel_volume_plan 的计划章节数
- **D-04:** 章节编号采用全局序号（跨卷递增），不再每卷从1开始。距离计算简化为 abs(callback - planted) >= N
- **D-05:** 现有测试数据可全部清理，不需要旧数据迁移

### 距离校验策略
- **D-06:** 默认最小间隔 3 章（全局序号计算：callbackChapter - plantedChapter >= 3）
- **D-07:** layoutType=dark（暗线）的伏笔豁免距离校验——暗线天然跨卷，可能贯穿全本
- **D-08:** bright1/bright2/bright3（明线）伏笔必须满足最小间隔要求

### 旧字段清理
- **D-09:** Phase 15 内彻底删除 foreshadowingSetup / foreshadowingPayoff：
  - DB 层：ALTER TABLE DROP COLUMN（init.sql 同步更新）
  - Java 层：NovelChapterPlan.java、ChapterPlanDto.java、ChapterPlanUpdateRequest.java 删除字段
  - 前端层：project.ts 类型定义、chapter.ts API 类型、ChapterPlanDrawer.vue、VolumeTree.vue、CreationCenter.vue 删除所有引用
- **D-10:** 不做软弃用（不保留 @TableField(exist=false)），彻底删除

### Claude's Discretion
- DB 列的具体数据类型和约束定义（NOT NULL / DEFAULT 值）
- 索引策略（是否需要复合索引 plantedVolume + plantedChapter）
- 距离校验错误信息的具体文案
- 章节全局序号的生成/维护方式

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 伏笔数据模型
- `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java` — 伏笔实体，需新增 plantedVolume / plannedCallbackVolume 字段
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java` — 伏笔 DTO，需同步新增字段
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingCreateDto.java` — 创建 DTO，需新增分卷字段
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingUpdateDto.java` — 更新 DTO，需新增分卷字段
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java` — 查询 DTO，需支持按分卷筛选
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` — 伏笔服务，需添加距离校验逻辑
- `ai-factory-backend/src/main/java/com/aifactory/controller/ForeshadowingController.java` — 伏笔控制器

### 旧字段清理目标
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` — 删除 foreshadowingSetup/foreshadowingPayoff 字段（line 98, 103）
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` — 删除 line 58, 61
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanUpdateRequest.java` — 删除 line 45, 48
- `ai-factory-frontend/src/types/project.ts` — 删除 foreshadowingSetup/foreshadowingPayoff 类型（line 132-133, 156-157）
- `ai-factory-frontend/src/api/chapter.ts` — 删除 line 41-42
- `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue` — 删除 textarea 及相关引用
- `ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue` — 删除 line 159-160
- `ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue` — 删除 line 115-116

### 分卷规划参考
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelVolumePlan.java` — 分卷规划实体（含计划章节数）
- `sql/init.sql` line 340-360 — novel_foreshadowing 建表语句
- `sql/init.sql` line 174-175 — foreshadowing_setup/payoff 列定义（待删除）

### 研究文档
- `.planning/research/STACK.md` §Change 2 — 旧字段删除的 DDL 参考
- `.planning/research/PITFALLS.md` §Pitfall 3 — 旧字段删除的陷阱分析
- `.planning/research/ARCHITECTURE.md` — 伏笔架构设计参考

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ForeshadowingService` — 已有完整 CRUD + 统计 + 待回收伏笔查询，需扩展距离校验
- `ForeshadowingController` — 已有 6 个 REST 端点（CRUD + markAsCompleted + stats），需扩展查询参数
- `ForeshadowingMapper` — MyBatis-Plus mapper，无需自定义 SQL
- `NovelVolumePlan` — 分卷规划实体，含计划章节数字段，用于回调章节上限校验

### Established Patterns
- **彻底删除**: DATA-02 已决定不保留 @TableField(exist=false)，直接 DROP COLUMN
- **DTO 同步**: 实体增删字段时 DTO/QueryDto/CreateDto/UpdateDto 全部同步
- **BeanUtils.copyProperties**: ForeshadowingService 用此做 Entity-DTO 转换，新增字段自动映射

### Integration Points
- `ForeshadowingService.createForeshadowing()` — 创建时需添加距离校验
- `ForeshadowingService.updateForeshadowing()` — 更新时需添加距离校验
- `ForeshadowingController.getForeshadowingList()` — 查询需支持按分卷筛选
- `ChapterPlanDrawer.vue` — 前端删除伏笔 textarea 后保留占位（Phase 18 填充新 UI）
- `VolumeTree.vue` / `CreationCenter.vue` — 前端伏笔字段引用需清理

</code_context>

<specifics>
## Specific Ideas

- 伏笔定位格式示例：plantedVolume=1, plantedChapter=1 → plannedCallbackVolume=2, plannedCallbackChapter=1（跨卷回收）
- 章节全局序号：第1卷10章，第2卷从11开始。Vol1 Ch5 到 Vol2 Ch5 实际距离=10章
- 暗线伏笔（dark）天然跨卷，可贯穿全本，豁免距离校验
- 回调章节上限：如第5卷规划20章，plannedCallbackChapter 不能超过20

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 15-data-foundation*
*Context gathered: 2026-04-10*
