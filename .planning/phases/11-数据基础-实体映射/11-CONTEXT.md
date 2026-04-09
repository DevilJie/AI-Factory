# Phase 11: 数据基础 + 实体映射 - Context

**Gathered:** 2026-04-07
**Status:** Ready for planning

<domain>
## Phase Boundary

数据库和实体层具备存储和关联规划角色的能力，为后续 AI 规划和生成提供数据基础。具体范围：
1. 新增 planned_characters JSON 列到 novel_chapter_plan 表
2. NovelChapterPlan 实体映射 plannedCharacters 和 characterArcs 两个字段
3. 合并重复的 ChapterPlanXmlDto 类为单一 DTO
4. 实现三级名称匹配工具（精确 → 去后缀 → 包含），用于角色名关联

不包含：AI 规划模板升级（Phase 12）、章节生成约束注入（Phase 13）、前端展示（Phase 14）。
</domain>

<decisions>
## Implementation Decisions

### planned_characters JSON 结构
- **D-01:** 最小 schema — 每个规划角色包含 5 个字段：characterName（必填）、roleType（必填，protagonist/supporting/antagonist/npc）、roleDescription（可选，戏份概述）、importance（可选，high/medium/low 三级）、characterId（nullable，名称匹配后填入 DB 角色ID）
- **D-02:** JSON 列使用 String 类型（与 keyEvents、foreshadowingSetup 等现有 JSON 列一致），MyBatis-Plus 自动映射
- **D-03:** characterId 匹配失败时为 null，角色名保留在 characterName 中不丢失

### ChapterPlanXmlDto 合并
- **D-04:** 保留 `com.aifactory.dto.ChapterPlanXmlDto`（有 Schema 注解、被 ChapterGenerationTaskStrategy 活跃引用），删除 `com.aifactory.common.xml.ChapterPlanXmlDto`（无任何导入，历史遗留）
- **D-05:** common.xml 版本的 volumeNumber 字段不迁移 — 章节规划已通过 volumePlanId 外键关联分卷，volumeNumber 非必要

### 名称匹配工具
- **D-06:** 提取通用 NameMatchUtil 工具类 — 三级匹配逻辑参数化（输入：名称列表 + 目标名 + 后缀列表），可复用于角色、势力、地区等场景
- **D-07:** 角色名后缀列表：公子/小姐/大哥/大姐/师傅/师叔/长老/前辈/道友/兄弟/妹妹 等常见尊称/称谓
- **D-08:** Phase 11 仅实现角色匹配功能，不重构 WorldviewXmlParser 中的现有匹配逻辑（后续可委托调用 NameMatchUtil）

### character_arcs 字段处理
- **D-09:** 仅映射字段 — 在 NovelChapterPlan 实体中添加 characterArcs String 字段，对应 DB 已有的 character_arcs JSON 列
- **D-10:** character_arcs 数据填充不在 Phase 11 范围内，留给后续 phase 或手动编辑

### Claude's Discretion
- NameMatchUtil 的具体包位置（common/util 或 common/xml）
- 角色名后缀列表的完整枚举
- planned_characters JSON 序列化/反序列化细节
- ChapterPlanDto（响应 DTO）中是否同时暴露 plannedCharacters 字段

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 章节规划实体与表结构
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` — 章节规划实体，需添加 plannedCharacters 和 characterArcs 字段
- `sql/init.sql` lines 360-386 — novel_chapter_plan 表结构，已有 character_arcs JSON 列，需添加 planned_characters 列
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` — 章节规划响应 DTO，可能需要添加 plannedCharacters 字段

### ChapterPlanXmlDto 合并
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanXmlDto.java` — 保留版本，有 ChapterPlanItem 内部类、Schema 注解
- `ai-factory-backend/src/main/java/com/aifactory/common/xml/ChapterPlanXmlDto.java` — 删除版本，无引用，有 Chapter 内部类
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` line 638 — 唯一使用 ChapterPlanXmlDto 的地方

### 名称匹配参考
- `ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java` lines 591-660 — 三级匹配模式参考（findRegionIdByName、findPowerSystemIdByName）
- `ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java` line 489 — 现有角色精确匹配逻辑（isSameCharacter）

### 前端类型定义
- `ai-factory-frontend/src/types/project.ts` line 140 — ChapterPlan 类型定义，Phase 14 可能需要扩展

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorldviewXmlParser` 三级匹配模式 — 代码结构清晰（精确→去后缀→包含），可直接提取为通用工具
- `XmlParser` (common/xml/) — DOM XML 解析工具，ChapterPlanXmlDto 已使用 Jackson XML
- `NovelChapterPlanMapper` — 标准 MyBatis-Plus BaseMapper，无需自定义方法

### Established Patterns
- **JSON 列**: novel_chapter_plan 已有 keyEvents(String 类型 JSON)、foreshadowingSetup、foreshadowingPayoff 等多个 JSON 列，planned_characters 沿用同模式
- **String 类型 JSON 字段**: MyBatis-Plus 直接映射为 String，业务层用 ObjectMapper 解析
- **名称回查**: WorldviewXmlParser 中已验证三级匹配可行（v1.0.2），角色名匹配复用同一策略

### Integration Points
- `ChapterGenerationTaskStrategy` — 使用 ChapterPlanXmlDto 解析 AI 输出，Phase 12 会在此扩展角色规划解析
- `NovelChapterPlan` 实体 — 所有章节规划 API 都通过此实体读写，字段添加后自动可用
- `ChapterPlanDto` — 前端 API 响应 DTO，添加字段后前端即可访问

</code_context>

<specifics>
## Specific Ideas

- 角色名后缀匹配规则：AI 输出"李公子" → 去后缀"公子" → 精确匹配"李" → 包含匹配到"李云"（需要careful设计避免过度匹配）
- importance 三级标记可在 Phase 14 前端展示时用不同颜色/大小区分角色重要度
- planned_characters JSON 示例：`[{"characterName":"李云","roleType":"protagonist","roleDescription":"发现密室线索","importance":"high","characterId":42}]`

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---
*Phase: 11-数据基础-实体映射*
*Context gathered: 2026-04-07*
