# 需求：势力阵营结构化重构

**定义日期：** 2026-04-01
**核心价值：** 势力的结构化数据能让 AI 生成章节时准确引用势力关系，也让用户方便地查看、编辑、管理势力体系

## v1 需求

### 数据库与领域层

- [x] **DB-01**: 新建 novel_faction 表（树形结构），字段：id、parent_id、deep、sort_order、project_id、name、type（正派/反派/中立）、core_power_system（关联力量体系 ID）、description、create_time、update_time
- [x] **DB-02**: type 和 core_power_system 仅顶级势力设置，下级势力通过代码逻辑继承顶级势力值
- [x] **DB-03**: 新建 novel_faction_region 关联表（势力-地区），简单多对多，字段：id、faction_id、region_id
- [x] **DB-04**: 新建 novel_faction_character 关联表（势力-人物），字段：id、faction_id、character_id、role（职位，如掌门、长老、弟子）
- [x] **DB-05**: 新建 novel_faction_relation 表（势力-势力关系），字段：id、faction_id、target_faction_id、relation_type（盟友/敌对/中立）、description
- [x] **DB-06**: NovelWorldview.forces 字段改为 @TableField(exist = false) transient，从关联表动态填充
- [x] **DB-07**: SQL 迁移脚本（建表 + 处理 forces 列），保留 forces 列直到所有消费方迁移完毕

### 后端服务

- [x] **BACK-01**: FactionService 接口及实现，参照 ContinentRegionService 模式，支持树形 CRUD
- [x] **BACK-02**: 树形查询：getTreeByProjectId() 返回带 children 的树结构
- [x] **BACK-03**: 级联删除：删除势力时递归删除子势力及所有关联数据（关联表、关系表）
- [x] **BACK-04**: deleteByProjectId() 集中清理项目下所有势力数据（供 WorldviewTaskStrategy 调用）
- [x] **BACK-05**: fillForces(NovelWorldview) 方法，从势力树构建文本描述供 PromptBuilder 使用
- [x] **BACK-06**: type/core_power_system 继承逻辑：查询子势力时自动填充从顶级势力继承的值
- [ ] **BACK-07**: FactionController REST API（CRUD 端点）
- [ ] **BACK-08**: 势力-地区关联 CRUD API
- [ ] **BACK-09**: 势力-人物关联 CRUD API（含 role 字段）
- [ ] **BACK-10**: 势力-势力关系 CRUD API

### AI 集成

- [x] **AI-01**: 更新 AI 提示词模板（id=3），势力部分从 `<f><![CDATA[纯文本]]></f>` 改为结构化 XML 格式
- [x] **AI-02**: 提示词中势力 XML 通过名称引用力量体系和地理区域，确保与已输出的 `<p>` 和 `<g>` 标签中的名称一致
- [x] **AI-03**: WorldviewTaskStrategy 新增 saveFactionsFromXml() 方法，DOM 解析 `<f>` 标签
- [x] **AI-04**: 两遍插入策略：第一遍存所有势力（构建名称→ID 映射），第二遍建立势力-地区和势力-势力关联
- [x] **AI-05**: 名称→ID 模糊匹配：AI 输出的名称与数据库记录进行匹配时支持容错（如"紫阳宗"匹配"紫阳宗门"）
- [x] **AI-06**: checkExisting 步骤中增加删除旧势力数据的逻辑
- [x] **AI-07**: DOM 解析使用 getChildNodes() 直接子元素迭代，不使用 getElementsByTagName（避免取到所有后代）
- [x] **AI-08**: 所有关联表存储数据库 ID，不存储名称

### 提示词构建迁移

- [x] **PROMPT-01**: PromptTemplateBuilder 中 getForces() 调用迁移为 fillForces()
- [x] **PROMPT-02**: ChapterPromptBuilder 中 getForces() 调用迁移为 fillForces()
- [x] **PROMPT-03**: PromptContextBuilder 中 getForces() 调用迁移为 fillForces()
- [x] **PROMPT-04**: ChapterFixTaskStrategy 中 getForces() 调用迁移为 fillForces()
- [x] **PROMPT-05**: VolumeOptimizeTaskStrategy 中 getForces() 调用迁移为 fillForces()
- [x] **PROMPT-06**: OutlineTaskStrategy 中 getForces() 调用迁移为 fillForces()

### 前端

- [ ] **UI-01**: FactionTree.vue 递归树组件，支持展开/折叠/增删改/描述编辑，类型标签（正派/反派/中立颜色区分），力量体系标签
- [ ] **UI-02**: faction.ts API 客户端（CRUD 接口）
- [ ] **UI-03**: WorldSetting.vue 中替换势力 textarea 为 FactionTree 组件
- [ ] **UI-04**: 势力-势力关系管理界面（选择两个势力、关系类型、描述）
- [ ] **UI-05**: 势力-人物手动关联界面（选择人物、分配职位）
- [ ] **UI-06**: 势力-地区关联界面（选择已有地区关联到势力）

## v2 需求

### 扩展功能

- **EXT-01**: 势力关系可视化图谱（类似 World Anvil 的 Diplomacy Web）
- **EXT-02**: 势力模板按题材分类（仙侠/武侠/都市等预设势力结构）
- **EXT-03**: 势力事件日志 / 时间线集成（需要时间线结构化完成后）
- **EXT-04**: AI 自动生成势力-人物关联（需要角色生成与势力生成在同一流程中）

## 超出范围

| 功能 | 原因 |
|------|------|
| 时间线结构化 | 本次只做势力，时间线保持文本 |
| 世界规则结构化 | 本次只做势力，规则保持文本 |
| 势力地图可视化 | 前端成本高，ROI 低 |
| 势力-人物 AI 自动关联 | 世界观生成时人物可能未创建 |

## 可追溯性

| 需求 | 阶段 | 状态 |
|------|------|------|
| DB-01 | Phase 1 | Complete |
| DB-02 | Phase 1 | Complete |
| DB-03 | Phase 1 | Complete |
| DB-04 | Phase 1 | Complete |
| DB-05 | Phase 1 | Complete |
| DB-06 | Phase 1 | Complete |
| DB-07 | Phase 1 | Complete |
| BACK-01 | Phase 2 | Complete |
| BACK-02 | Phase 2 | Complete |
| BACK-03 | Phase 2 | Complete |
| BACK-04 | Phase 2 | Complete |
| BACK-05 | Phase 2 | Complete |
| BACK-06 | Phase 2 | Complete |
| BACK-07 | Phase 2 | Pending |
| BACK-08 | Phase 2 | Pending |
| BACK-09 | Phase 2 | Pending |
| BACK-10 | Phase 2 | Pending |
| AI-01 | Phase 3 | Complete |
| AI-02 | Phase 3 | Complete |
| AI-03 | Phase 3 | Complete |
| AI-04 | Phase 3 | Complete |
| AI-05 | Phase 3 | Complete |
| AI-06 | Phase 3 | Complete |
| AI-07 | Phase 3 | Complete |
| AI-08 | Phase 3 | Complete |
| PROMPT-01 | Phase 3 | Complete |
| PROMPT-02 | Phase 3 | Complete |
| PROMPT-03 | Phase 3 | Complete |
| PROMPT-04 | Phase 3 | Complete |
| PROMPT-05 | Phase 3 | Complete |
| PROMPT-06 | Phase 3 | Complete |
| UI-01 | Phase 4 | Pending |
| UI-02 | Phase 4 | Pending |
| UI-03 | Phase 4 | Pending |
| UI-04 | Phase 5 | Pending |
| UI-05 | Phase 5 | Pending |
| UI-06 | Phase 5 | Pending |

**覆盖率：**
- v1 需求：36 条
- 已映射阶段：36
- 未映射：0 ✓

---
*需求定义：2026-04-01*
*最后更新：2026-04-01 路线图创建，可追溯性确认覆盖
