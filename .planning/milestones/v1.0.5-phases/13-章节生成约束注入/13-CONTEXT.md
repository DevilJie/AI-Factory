# Phase 13: 章节生成约束注入 - Context

**Gathered:** 2026-04-09
**Status:** Ready for planning

<domain>
## Phase Boundary

修改章节生成（非规划）提示词构建逻辑：有规划角色时替换全量角色列表为规划角色信息，分段约束语言强制 AI 按规划角色生成内容，明确允许 NPC 角色。无规划角色时保持原有逻辑不变。

具体范围：
1. 修改章节生成提示词构建逻辑 — 有规划角色时仅注入规划角色信息（替换全量列表）
2. 分段约束语言 — 开头约束 + 结尾提醒，AI 必须严格遵循规划角色出场
3. NPC 允许提示 — 一句式提示允许 NPC 性质角色自由生成
4. 无规划角色章节完全兼容 — 走原有全量角色注入逻辑

不包含：角色自动创建（CG-03 递延）、前端展示（Phase 14）。

</domain>

<decisions>
## Implementation Decisions

### 约束语言设计
- **D-01:** 分段约束 — 开头明确"你必须严格遵循以下角色安排，以下角色必须出场" + 结尾再次提醒"请检查你的输出是否遵循了上述角色约束"。双重提醒
- **D-02:** 仅约束角色出场（谁必须登场），不约束情节走向、戏份内容、对话风格等。AI 仍可自由发挥情节细节
- **D-03:** 仅提示词级别约束，无后端检测/重试机制。偏差在 Phase 14 前端对比视图中体现
- **D-04:** NPC 允许提示 — 在规划角色约束后加一句"跑龙套、路人甲等 NPC 性质角色可根据情节需要自行安排"

### 规划角色注入格式
- **D-05:** 纯文本列表格式注入 — 每行一个角色的简要信息，如"李云 (protagonist) - 发现密室线索并决定深入调查 [high]"。简洁、AI 易理解
- **D-06:** 有规划角色时完全替换全量角色列表 — AI 只看到规划角色，不保留全量列表作为参考
- **D-07:** 复用现有 `{characterInfo}` 模板变量 — 有规划角色时填入规划角色文本，无规划角色时填入全量角色列表。代码改动最小

### 兼容与回退策略
- **D-08:** 二分支干净回退 — 有规划角色走约束注入分支，无规划角色走原有全量角色注入分支。两个分支互不干扰
- **D-09:** 无混合模式 — 不存在"规划角色 + 全量参考"的中间状态

### CG-03 递延
- **D-10:** 新角色自动创建（CG-03）不在本 phase 范围内 — 用户明确表示仅处理提示词注入，角色提取/创建逻辑递延

### Claude's Discretion
- 约束语言的具体中文措辞（开头/结尾的精确表述）
- 规划角色文本列表的具体格式细节（字段顺序、分隔符）
- 代码中判断"有规划角色"的条件（plannedCharacters 非空/非 null/非空数组）
- 是否需要创建新的提示词模板版本还是修改现有模板

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 章节生成提示词构建（核心修改点）
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` lines 453-467 — 当前角色信息注入逻辑（characterInfo 变量填充），需增加规划角色分支
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` — buildChapterPromptUsingTemplate() 方法，提示词构建主流程
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` — generateChapterContent() 或类似方法，章节内容生成入口

### 章节生成模板
- `sql/init.sql` — llm_chapter_generate_standard 模板定义（template_id 用于章节内容生成），可能需要创建新版本

### 章节规划数据访问
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` — 章节规划实体，plannedCharacters 字段存储 JSON
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` — 章节规划响应 DTO

### 角色数据访问
- `ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterService.java` — 角色 CRUD 服务，查询全量角色列表用于注入

### 已有模式参考
- `ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java` — 三级名称匹配参考
- `ai-factory-backend/src/main/java/com/aifactory/common/NameMatchUtil.java` — 名称匹配工具（planned_characters 中 characterId 已在 Phase 12 解析时填入）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChapterGenerationTaskStrategy.buildChapterPromptUsingTemplate()` — 已有提示词构建方法，`{characterInfo}` 变量位置已确定，只需修改变量内容填充逻辑
- `NovelCharacterService` — 角色查询服务，可获取项目全量角色列表（已有 getCharactersByProject 方法）
- `NovelChapterPlan` 实体 — plannedCharacters JSON 字段已映射（Phase 11），可直接读取

### Established Patterns
- **模板变量替换**: PromptTemplateService 解析模板变量，service 层填充具体内容
- **JSON 列读取**: plannedCharacters 为 String 类型 JSON，使用 ObjectMapper 解析为 List
- **二分支逻辑**: 类似 GeographyTaskStrategy 和 FactionTaskStrategy 的依赖校验模式 — 有/无两种状态走不同路径
- **模板版本管理**: 通过 ai_prompt_template_version 表，新增版本 + is_active 切换

### Integration Points
- `ChapterGenerationTaskStrategy` — 主要修改点，characterInfo 变量填充逻辑增加规划角色分支
- 章节生成模板 — 可能需要新增版本（增加约束语言段落）
- `NovelChapterPlan` — 读取当前章节的 plannedCharacters 数据
- 章节生成流程入口 — 在调用 buildChapterPromptUsingTemplate 之前查询规划数据

</code_context>

<specifics>
## Specific Ideas

- 约束语言参考：开头"以下是本章必须出场的角色，请严格按照此列表安排角色出场" + 结尾"请确认你的章节内容包含了上述所有必须出场的角色"
- NPC 提示参考："跑龙套、路人甲等 NPC 性质角色可根据情节需要自行安排"
- 规划角色文本格式参考："李云 (protagonist) - 发现密室线索 [high]"
- 规划角色只包含主角、配角、反派 — NPC 类角色不在规划中

</specifics>

<deferred>
## Deferred Ideas

- **CG-03 新角色自动创建** — 规划中出现数据库不存在的新角色时，章节生成后自动创建角色记录并建立章节关联。递延至后续 phase。用户明确表示本 phase 仅处理提示词注入。

</deferred>

---

*Phase: 13-章节生成约束注入*
*Context gathered: 2026-04-09*
