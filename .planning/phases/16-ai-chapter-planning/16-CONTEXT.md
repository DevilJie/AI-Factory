# Phase 16: AI Chapter Planning - Context

**Gathered:** 2026-04-11
**Status:** Ready for planning

<domain>
## Phase Boundary

LLM 章节规划时感知已有伏笔状态，自动输出新伏笔的埋设和回收决策，DOM 解析并持久化到伏笔表。具体：
1. 章节规划提示词注入当前分卷活跃伏笔列表（待埋设 + 待回收），LLM 能看到伏笔上下文
2. LLM 章节规划输出新增 <fs>（伏笔埋设）和 <fp>（伏笔回收）XML 标签，含标题、描述、类型、布局线等子标签
3. DOM 解析器扩展处理 <fs>/<fp> 标签，解析伏笔数据并批量创建 novel_foreshadowing 记录

不包含：章节生成伏笔约束注入（Phase 17）、前端伏笔管理 UI（Phase 18/19）。
</domain>

<decisions>
## Implementation Decisions

### 伏笔上下文注入
- **D-01:** 结构化列表格式注入已有伏笔上下文——每个伏笔显示标题、类型、布局线、状态、埋设/回收位置，让 LLM 看到完整上下文做决策
- **D-02:** 注入范围仅当前分卷的活跃伏笔（status=pending 或 in_progress，且 plantedVolume 或 plannedCallbackVolume 匹配当前分卷），减少 token 占用

### LLM 输出 XML 标签
- **D-03:** `<fs>`（伏笔埋设）和 `<fp>`（伏笔回收）使用子标签格式，与现有 `<n>/<t>/<p>` 模式一致：
  - `<fs>` 子标签：`<ft>` 标题、`<fy>` 类型（character/item/event/secret）、`<fl>` 布局线（bright1/bright2/bright3/dark）、`<fd>` 描述、`<fc>` 回收分卷、`<fr>` 回收章节
  - `<fp>` 子标签：`<ft>` 标题（匹配待回收伏笔）、`<fd>` 回收方式描述
- **D-04:** 每个 `<o>`（章节）内可以包含零个或多个 `<fs>` 和 `<fp>` 标签，支持一章埋设/回收多个伏笔

### 解析与持久化策略
- **D-05:** LLM 输出伏笔直接创建新记录，不与已有伏笔做名称匹配。避免错误匹配导致伏笔数据混乱
- **D-06:** 重新规划分卷时，先删除该卷所有 pending 状态伏笔（plantedVolume 匹配当前卷），再根据 LLM 输出重新创建。与 saveChaptersToDatabase 先删后建模式一致

### 规划路径统一
- **D-07:** 两条规划路径都支持伏笔注入和解析——`generateChaptersInVolume`（批量大纲流程）和 `generateChaptersForVolume`（单卷重新规划）均注入伏笔上下文并解析 `<fs>/<fp>` 标签

### Claude's Discretion
- 结构化列表的具体文本格式（如何排版让 LLM 容易理解）
- 注入伏笔上下文的模板变量名
- `<fs>/<fp>` 的 regex 解析实现细节
- 批量创建伏笔时的批量插入优化
- 暗线（dark）伏笔在注入时的特殊展示

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 章节规划核心逻辑
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java` — 章节规划策略，含 parseChaptersXml()、buildChapterPromptUsingTemplate()、generateChaptersInVolume()、generateChaptersForVolume()
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` — 章节提示词构建器，含 buildVolumeInfo()、角色注入模式参考

### 伏笔服务
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` — 伏笔服务，含 CRUD、getForeshadowingList()、validateForeshadowingDistance()
- `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java` — 伏笔实体（含 plantedVolume/plannedCallbackVolume 分卷字段）
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingDto.java` — 伏笔 DTO
- `ai-factory-backend/src/main/java/com/aifactory/dto/ForeshadowingQueryDto.java` — 查询 DTO（含 plantedVolume/plannedCallbackVolume 筛选）

### 提示词模板
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptTemplateService.java` — 模板服务
- 章节规划模板 code: `llm_outline_chapter_generate` — 需增加伏笔上下文变量和 `<fs>/<fp>` 输出指令

### Phase 15 上下文（数据基础）
- `.planning/phases/15-data-foundation/15-CONTEXT.md` — Phase 15 决策（分卷字段、距离校验、旧字段清理）

### Phase 12 先例（角色规划 XML 标签）
- Phase 12 曾在章节规划中增加 `<ch>/<cn>/<cd>/<ci>` 角色标签——本 phase 的 `<fs>/<fp>` 模式参照此先例

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ForeshadowingService.getForeshadowingList(queryDto)` — 已支持按 plantedVolume/plannedCallbackVolume 筛选、按 status 筛选，可直接查询当前分卷活跃伏笔
- `ForeshadowingService.createForeshadowing(projectId, createDto)` — 已含距离校验和回调章节边界校验
- `ForeshadowingService.buildPendingForeshadowingText()` — 旧的纯文本格式，需替换为结构化列表
- `OutlineTaskStrategy.parseChaptersXml()` — 已有 regex DOM 解析，可扩展 <fs>/<fp> 标签提取
- `OutlineTaskStrategy.extractXmlFieldCData()` — 提取 XML 字段的通用方法，可复用
- `PromptTemplateService.executeTemplate(code, variables)` — 模板执行引擎

### Established Patterns
- **先删后建**: saveChaptersToDatabase 先删旧章节再创建新章节，伏笔持久化遵循同样模式
- **regex DOM 解析**: parseChaptersXml 用 regex 而非真正的 DOM parser，新标签需同样处理
- **模板变量注入**: buildChapterPromptUsingTemplate 通过 variables map 注入，新伏笔上下文作为新变量
- **DTO 批量转换**: ForeshadowingService 用 BeanUtils.copyProperties 做 Entity-DTO 转换

### Integration Points
- `OutlineTaskStrategy.buildChapterPromptUsingTemplate()` — 注入伏笔上下文变量到模板
- `OutlineTaskStrategy.parseChaptersXml()` — 扩展解析 <fs>/<fp> 标签
- `OutlineTaskStrategy.saveVolumeChaptersToDatabase()` / `saveChaptersToDatabase()` — 解析后批量创建伏笔
- `llm_outline_chapter_generate` 模板 — 增加伏笔上下文注入块和 <fs>/<fp> 输出指令
- `ForeshadowingService` — 查询当前卷活跃伏笔 + 批量创建新伏笔

</code_context>

<specifics>
## Specific Ideas

- 伏笔上下文注入示例格式：`伏笔标题 | 类型: event | 布局线: bright1 | 状态: pending | 埋设: 第1卷第3章 | 计划回收: 第1卷第8章`
- <fs> 输出样例：`<fs><ft>神秘钥匙</ft><fy>item</fy><fl>bright1</fl><fd>主角在废弃矿洞中发现一把古钥匙</fd><fc>1</fc><fr>8</fr></fs>`
- <fp> 输出样例：`<fp><ft>神秘钥匙</ft><fd>钥匙打开了地下密室，揭示主角身世</fd></fp>`
- 两条路径共用同一个伏笔注入逻辑（提取方法避免重复）

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 16-ai-chapter-planning*
*Context gathered: 2026-04-11*
