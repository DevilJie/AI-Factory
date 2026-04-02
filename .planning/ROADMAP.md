# 路线图：势力阵营结构化重构

## 概述

将 AI Factory 世界观模块中的势力阵营从纯文本字段重构为结构化树形数据体系。分 5 个阶段交付：先建立数据基础（表结构+实体），再实现后端服务（CRUD+API），接着完成 AI 集成（提示词+解析+迁移），然后构建前端树组件，最后交付关联管理界面。这是继力量体系和地理环境重构之后的第三轮世界观结构化改造，复用已验证的架构模式。

## 阶段

**阶段编号说明：**
- 整数阶段（1, 2, 3）：计划中的里程碑工作
- 小数阶段（2.1, 2.2）：紧急插入（标记 INSERTED）

小数阶段按数字顺序排列在相邻整数之间。

- [ ] **Phase 1: 数据基础** - 新建 4 张表、4 个实体类、4 个 Mapper，标记 forces 为 transient
- [ ] **Phase 2: 后端服务与 API** - FactionService CRUD、关联管理 API、fillForces() 文本构建
- [x] **Phase 3: AI 集成与提示词迁移** - 提示词模板更新、DOM 解析入库、所有 getForces() 调用点迁移 (completed 2026-04-02)
- [ ] **Phase 4: 前端树组件** - FactionTree.vue 递归树组件、API 客户端、WorldSetting 集成
- [ ] **Phase 5: 关联管理界面** - 势力关系、势力-人物、势力-地区的前端管理界面

## 阶段详情

### Phase 1: 数据基础
**Goal**: 数据库表和实体层就绪，为所有后续开发提供数据模型基础
**Depends on**: 无（首个阶段）
**Requirements**: DB-01, DB-02, DB-03, DB-04, DB-05, DB-06, DB-07
**Success Criteria** (what must be TRUE):
  1. novel_faction 表已创建，支持 parent_id + deep 树形结构，包含 name/type/core_power_system/description 字段
  2. novel_faction_region、novel_faction_character、novel_faction_relation 三张关联表已创建，字段定义完整
  3. 4 个 MyBatis-Plus 实体类及对应 Mapper 接口已就绪，编译通过
  4. NovelWorldview.forces 字段已标记为 @TableField(exist = false) transient
  5. SQL 迁移脚本可执行，建表成功且保留原有 forces 列
**Plans**: 2 plans

Plans:
- [x] 01-01: 创建 SQL 迁移脚本（4 张表 DDL）和 NovelFaction 树形实体 + Mapper
- [x] 01-02: 创建 3 个关联表实体 + Mapper，修改 NovelWorldview.forces 为 transient

### Phase 2: 后端服务与 API
**Goal**: 势力的完整 CRUD 生命周期可用，包括树形查询、级联删除、关联管理和 fillForces() 文本构建
**Depends on**: Phase 1
**Requirements**: BACK-01, BACK-02, BACK-03, BACK-04, BACK-05, BACK-06, BACK-07, BACK-08, BACK-09, BACK-10
**Success Criteria** (what must be TRUE):
  1. 通过 REST API 可创建、查询、更新、删除势力节点，删除父节点时子节点及所有关联数据递归清除
  2. getTreeByProjectId() 返回完整的势力树结构，子势力自动继承顶级势力的 type 和 core_power_system 值
  3. 势力-地区、势力-人物、势力-势力关系的 CRUD API 全部可用，数据正确持久化
  4. fillForces() 方法能从势力树构建完整的文本描述，可被 PromptBuilder 调用
  5. deleteByProjectId() 可集中清理项目下所有势力相关数据（4 张表）
**Plans**: 2 plans

Plans:
- [x] 02-01: FactionService 接口及实现（树形 CRUD、级联删除、type 继承、fillForces 文本构建）
- [ ] 02-02: FactionController REST API（势力 CRUD + 3 组关联管理端点）

### Phase 3: AI 集成与提示词迁移
**Goal**: AI 世界观生成流程能输出结构化势力 XML，后端正确解析入库，所有提示词构建调用点已迁移到新方法
**Depends on**: Phase 2
**Requirements**: AI-01, AI-02, AI-03, AI-04, AI-05, AI-06, AI-07, AI-08, PROMPT-01, PROMPT-02, PROMPT-03, PROMPT-04, PROMPT-05, PROMPT-06
**Success Criteria** (what must be TRUE):
  1. AI 提示词模板已更新，势力部分输出结构化 XML（含类型、力量体系引用、地区引用、势力间关系）
  2. WorldviewTaskStrategy 能通过 DOM 解析将 AI 输出的势力 XML 正确存入数据库，名称模糊匹配容错工作正常
  3. 两遍插入策略正确执行：先存势力构建名称-ID 映射，再建立关联关系
  4. 所有 6 处 getForces() 调用点已迁移为 fillForces()，无遗漏（PromptTemplateBuilder、ChapterPromptBuilder、PromptContextBuilder、ChapterFixTaskStrategy、VolumeOptimizeTaskStrategy、OutlineTaskStrategy）
  5. 已有项目重新生成世界观时，旧势力数据正确清除、新数据正确入库
**Plans**: 3 plans

Plans:
- [x] 03-01: 更新 AI 提示词模板（世界观生成模板势力部分改为结构化 XML 格式）
- [x] 03-02: WorldviewTaskStrategy 新增 saveFactionsFromXml（DOM 解析、两遍插入、三级名称匹配、checkExisting 清理）
- [x] 03-03: 迁移所有 getForces() 调用点到 fillForces()（8 个文件 10 处调用点）

### Phase 4: 前端树组件
**Goal**: 用户可在世界观设置页面以树形视图查看、编辑、新增、删除势力，替代原有的纯文本 textarea
**Depends on**: Phase 2
**Requirements**: UI-01, UI-02, UI-03
**Success Criteria** (what must be TRUE):
  1. FactionTree.vue 递归树组件正确渲染势力层级，支持展开/折叠
  2. 用户可在树中新增势力节点、编辑名称/描述/类型、删除节点，所有操作实时持久化到后端
  3. 势力类型（正派/反派/中立）通过颜色标签直观区分，关联的力量体系显示为标签
  4. WorldSetting.vue 中原有势力 textarea 已替换为 FactionTree 组件，页面功能完整
**Plans**: 2 plans
**UI hint**: yes

Plans:
- [x] 04-01-PLAN.md -- Faction API client + recursive FactionTree component (type badges, power system labels, CRUD)
- [x] 04-02-PLAN.md -- WorldSetting.vue integration (replace forces textarea, add refresh calls)

### Phase 5: 关联管理界面
**Goal**: 用户可手动管理势力间关系、势力-人物关联、势力-地区关联，完成势力数据的完整管理闭环
**Depends on**: Phase 4
**Requirements**: UI-04, UI-05, UI-06
**Success Criteria** (what must be TRUE):
  1. 用户可选择两个势力并设置关系类型（盟友/敌对/中立）和描述，关系列表可查看和删除
  2. 用户可将已有角色关联到势力并设置职位（如掌门、长老、弟子），关联列表可管理
  3. 用户可将已有地区关联到势力，关联列表可查看和移除
**Plans**: 2 plans
**UI hint**: yes

Plans:
- [x] 05-01-PLAN.md -- Association management API layer + Drawer with tabs + bidirectional relation CRUD
- [x] 05-02-PLAN.md -- Character tab (search + role datalist + CRUD) and Region tab (tree selector + batch add + CRUD)

## 进度

**执行顺序：**
阶段按数字顺序执行：1 -> 2 -> 3 -> 4 -> 5
注：Phase 3 和 Phase 4 均依赖 Phase 2，理论上可并行，但建议按顺序执行以降低风险。

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. 数据基础 | 2/2 | Planning complete | - |
| 2. 后端服务与 API | 1/2 | In progress | - |
| 3. AI 集成与提示词迁移 | 3/3 | Complete   | 2026-04-02 |
| 4. 前端树组件 | 1/2 | In progress | - |
| 5. 关联管理界面 | 2/2 | Complete | 2026-04-03 |
