# Phase 6: 独立提示词模板 - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

为地理环境、力量体系、阵营势力各创建独立 AI 提示词模板（3个新模板），并精简原有世界观整体生成模板（1个更新）。本 Phase 只涉及提示词模板内容的创建/更新（DB中的 ai_prompt_template + ai_prompt_template_version 记录），不涉及后端代码或前端改动。
</domain>

<decisions>
## Implementation Decisions

### 模板拆分策略
- **D-01:** 从现有 `llm_worldview_create` 统一模板中提取地理环境、力量体系、阵营势力三部分指令，各自成为独立模板
- **D-02:** 三个独立模板的 AI 输出格式完全复用现有 `<g>/<p>/<f>` 标签格式，与当前 `WorldviewTaskStrategy` 解析逻辑一致，Phase 7 无需新增解析代码
- **D-03:** 独立模板的提示词指令从现有统一模板中提取对应模块的格式说明和内容要求，保持格式一致性

### 上下文注入格式
- **D-04:** 阵营势力独立模板（`llm_faction_create`）使用**结构化 XML 嵌入**方式注入已有的地理环境和力量体系数据 — 即从数据库读取已入库的数据，序列化为 `<g>...</g>` 和 `<p>...</p>` XML 片段，直接嵌入提示词中
- **D-05:** AI 看到的是和自己输出时相同的格式，名称交叉引用最准确

### 精简后世界观模板
- **D-06:** `llm_worldview_create` 精简后仅保留：世界类型 `<t>`、世界背景 `<b>`、时间线 `<l>`、世界规则 `<r>` 四个字段
- **D-07:** 精简后仍保持 `<w>` 根标签包裹的 XML 格式，确保 Phase 8 组合调用时能无缝拼接三个独立模块的输出

### 模板变量设计
- **D-08:** 地理环境模板（`llm_geography_create`）、力量体系模板（`llm_power_system_create`）、精简世界观模板均复用现有 4 个变量：`projectDescription`、`storyTone`、`storyGenre`、`tagsSection`
- **D-09:** 阵营势力模板（`llm_faction_create`）额外增加 `geographyContext`（已入库的地理环境 XML）和 `powerSystemContext`（已入库的力量体系 XML）两个变量

### Claude's Discretion
- 各独立模板的具体提示词措辞和细节（从现有统一模板中提取并优化）
- 地理/力量体系独立模板中是否需要额外上下文（如世界类型信息）
- 模板的 scenario 标签值命名

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 现有提示词模板（DB中最新版本）
- DB: `ai_prompt_template` WHERE `template_code = 'llm_worldview_create'` — 当前统一世界观生成模板的完整内容（含地理环境/力量体系/阵营势力三部分指令和 XML 格式说明），拆分工作基于此版本
- DB: `ai_prompt_template_version` JOIN `ai_prompt_template` — 模板版本机制（version_number, is_active, current_version_id）

### 提示词模板基础设施
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptTemplateService.java` — 模板执行接口，`executeTemplate(code, variables)` 是核心方法
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/impl/PromptTemplateServiceImpl.java` — 模板执行实现，Hutool StrUtil.format 变量替换
- `ai-factory-backend/src/main/java/com/aifactory/entity/AiPromptTemplate.java` — 模板实体（templateCode, templateName, serviceType, scenario, currentVersionId）
- `ai-factory-backend/src/main/java/com/aifactory/entity/AiPromptTemplateVersion.java` — 模板版本实体（templateContent, variableDefinitions, versionNumber）

### 数据解析（Phase 7 复用）
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` — 当前世界观生成策略，包含地理环境 DOM 解析（saveGeographyRegionsFromXml）、力量体系解析（savePowerSystems）、阵营势力 DOM 解析（saveFactionsFromXml）的完整实现
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptContextBuilder.java` — 上下文构建服务，已有 buildWorldviewContext() 方法

### 数据库迁移参考
- `ai-factory-backend/src/main/resources/db/migration/V3__faction_prompt_template.sql` — 上次模板更新的迁移脚本格式参考

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PromptTemplateService.executeTemplate(code, vars)`: 模板执行基础设施完备，新模板只需按 templateCode 查询即可使用
- `PromptContextBuilder`: 已有构建世界观上下文的方法，可扩展为构建地理/力量体系 XML 上下文
- `WorldviewTaskStrategy` 中的解析逻辑: DOM 解析 `<g>/<f>` + Jackson 解析 `<p>` 的代码在 Phase 7 可直接复用于独立生成接口

### Established Patterns
- 模板存储模式: `ai_prompt_template`（主记录）+ `ai_prompt_template_version`（版本内容），通过 `current_version_id` 关联当前激活版本
- 变量替换: 使用 Hutool `StrUtil.format()`，变量格式为 `{variableName}`
- 迁移脚本: SQL UPDATE 语句更新 `ai_prompt_template_version.template_content`，WHERE 条件通过子查询匹配 `template_code`

### Integration Points
- Phase 7 的独立生成 API 将调用 `PromptTemplateService.executeTemplate("llm_geography_create"/"llm_power_system_create"/"llm_faction_create", vars)` 执行新模板
- Phase 8 的 `WorldviewTaskStrategy` 重构将调用精简后的 `llm_worldview_create` + 三个独立模板
- 阵营势力独立生成需要从 DB 读取已入库的地理/力量体系数据并序列化为 XML 嵌入提示词

</code_context>

<specifics>
## Specific Ideas

- 用户明确说"其实就是把原来耦合在世界观生成里面的全部拆出来了" — 本质是解耦，不是重写
- 独立模板的格式说明和内容要求从现有 `llm_worldview_create` 中提取对应模块部分，保持一致性
- 当前 DB 中最新的地理格式已更新为 `<r><n>名称</n><d>描述</d></r>` 结构（非迁移SQL中的旧 `name` 属性格式）

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope
</deferred>

---

*Phase: 06-独立提示词模板*
*Context gathered: 2026-04-03*
