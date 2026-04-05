# Phase 10: 角色体系关联与提取增强 - Context

**Gathered:** 2026-04-06
**Status:** Ready for planning

<domain>
## Phase Boundary

完善角色提取功能，建立角色与力量体系/势力阵营的结构化关联，修复角色类型识别问题。具体范围：
1. 新建角色-修炼体系关联表并实现提取时自动关联
2. 更新提取提示词模板增加势力信息提取，自动创建 faction_character 关联
3. 修复 roleType 识别准确率（protagonist/supporting/antagonist/npc）
4. 前端展示关联信息并支持编辑

不包含：角色地图可视化、批量角色操作、角色 AI 自动生成优化。
</domain>

<decisions>
## Implementation Decisions

### 修炼体系关联
- **D-01:** 复用名称回查模式 — systemName 精确/模糊匹配 power_system ID，currentLevel 匹配 level ID。与 v1.0.2 的势力名称匹配同一策略
- **D-02:** 提取时 upsert 关联 — 每次章节提取时更新 character_power_system 关联的 current_realm_id/current_sub_realm_id。角色可能修炼多个体系（多个关联记录）
- **D-03:** 匹配失败时 ID 字段为 null，文本信息保留在 character_chapter 关联的 cultivation_level JSON 中。不丢弃数据

### 势力阵营提取与关联
- **D-04:** XML 格式新增 `<FC>` 标签 — 每个角色可输出多个 `<FC><N>势力名称</N><R>职位/角色</R></FC>`，复用势力名称回查 faction ID
- **D-05:** 提取时自动 upsert `novel_faction_character` 关联（已有表结构：faction_id + character_id + role），无需新建表
- **D-06:** 角色可属于多个势力（转投、双重身份），每个势力一条关联记录

### 角色类型识别修复
- **D-07:** 提示词新增中文类型定义和判断规则 — protagonist=主角/第一视角人物, supporting=重要配角/多次出现, antagonist=反派/与主角对抗, npc=过场人物/一两句台词
- **D-08:** 注入已有角色类型分布到提示词 — "本项目已有 1 个 protagonist（李云），新提取的角色不会是 protagonist"。避免重复分类
- **D-09:** 保持 4 类不变 (protagonist/supporting/antagonist/npc)，不加 minor_supporting 等新类型

### 前端展示与编辑
- **D-10:** Drawer Tab 扩展 — 参照 FactionDrawer 的 Tab 模式，角色详情新增"修炼体系"和"所属势力"两个 Tab，支持增删改关联
- **D-11:** 角色列表卡片增加修炼境界名称和势力名称展示（如"玄武境 · 青云门长老"），一眼看到关联状态
- **D-12:** 服务端聚合 — 角色 detail API 返回关联的 power_system + level + faction 信息（JOIN 查询），前端一次请求拿到完整数据

### 提示词模板
- **D-13:** 增量优化 — 在现有模板基础上新增 FC 标签说明、角色类型中文定义、已有角色分布注入。保留现有 XML 格式和字段不变

### Claude's Discretion
- 具体的名称匹配容错规则（精确→模糊→跳过的阈值）
- 关联表索引策略
- Drawer Tab 内的具体 UI 组件选择（下拉/树形/列表）
- 列表卡片关联信息的排版细节

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 角色提取
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java` — 角色提取主服务，包含匹配逻辑、DTO 转换、提示词构建
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterCharacterExtractXmlDto.java` — XML DTO 结构，需新增 FC 字段
- `ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterService.java` — 角色 CRUD 服务

### 关联表模式参考
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFactionCharacter.java` — 势力-角色关联表结构（新关联表参照此模式）
- `ai-factory-backend/src/main/java/com/aifactory/service/impl/FactionServiceImpl.java` — 关联 CRUD 模式参考
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystem.java` — 修炼体系实体
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystemLevel.java` — 境界等级实体

### 提示词模板
- `sql/init.sql` line 727 — `ai_prompt_template_version` 中 template_id=15 的模板内容，需增量修改

### 前端模式参考
- `ai-factory-frontend/src/views/` — 角色详情页位置
- FactionDrawer.vue — Drawer Tab 模式参考（v1.0.2 Phase 05 创建）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `XmlParser` (common/xml/) — DOM XML 解析，CharacterExtractDto 已用此解析
- `PowerSystemService.buildPowerSystemConstraint()` — 已有修炼体系约束文本生成，可扩展返回名称列表用于回查
- `FactionServiceImpl` 名称匹配逻辑 — 可提取为共享的名称回查工具方法
- `NovelCharacterMapper` — 角色数据访问，需扩展关联查询

### Established Patterns
- **名称回查**: AI 输出名称 → 后端按名称精确/模糊匹配 ID（v1.0.2 验证过三级匹配）
- **关联表**: 简单 join table + 直挂 Mapper（faction_region, faction_character 模式）
- **两遍插入**: 先保存角色获取 ID，再建关联
- **Drawer Tab**: FactionDrawer 已实现 Tab 切换关联管理模式

### Integration Points
- `ChapterCharacterExtractService.extractCharacters()` — 提取流程主入口，需在角色匹配/创建后新增关联创建
- `NovelCharacterController` — 角色详情 API，需扩展返回关联信息
- `NovelCharacterService.getCharacterDetail()` — 需 JOIN 查询关联数据
- 前端角色详情页 — 需集成新 Drawer Tab

</code_context>

<specifics>
## Specific Ideas

- 角色列表卡片显示格式："玄武境 · 青云门长老"（修炼境界 · 势力职位）
- 提示词注入已有角色分布格式："本项目已有 protagonist: 李云, supporting: 王明、赵雪, antagonist: 黑衣人"
- FC 标签允许角色输出多个势力关联，支持转投、双重身份等剧情

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 10-角色体系关联与提取增强*
*Context gathered: 2026-04-06*
