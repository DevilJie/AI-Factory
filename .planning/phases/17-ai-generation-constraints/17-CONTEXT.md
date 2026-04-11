# Phase 17: AI Generation Constraints - Context

**Gathered:** 2026-04-11
**Status:** Ready for planning

<domain>
## Phase Boundary

章节生成时注入伏笔硬性创作指令，确保 LLM 按规划埋设或回收伏笔。具体：
1. 章节生成提示词包含"本章节需埋设的伏笔"和"本章节需回收的伏笔"约束段落
2. 约束文本仅包含当前章节相关项，格式简洁，不会导致上下文窗口溢出
3. 章节生成后自动更新伏笔状态（pending→in_progress, in_progress→completed）

不包含：章节规划伏笔输出（Phase 16 已完成）、前端伏笔管理 UI（Phase 18/19）。
</domain>

<decisions>
## Implementation Decisions

### 伏笔约束注入
- **D-01:** 约束段落使用指令式语言风格——"【本章节必须埋设的伏笔】"和"【本章节必须回收的伏笔】"作为标题，配合"请务必自然地融入情节"等指令语。与 Phase 13 角色约束的"必须严格遵循"风格一致
- **D-02:** 每个伏笔约束项包含标题 + 描述，不包含类型和布局线。减少 token 占用，生成阶段不需要规划阶段的完整信息
- **D-03:** 约束仅注入当前章节相关的伏笔——plantedChapter == currentGlobalChapter 的 pending 伏笔为"需埋设"，plannedCallbackChapter == currentGlobalChapter 的 in_progress 伏笔为"需回收"

### 状态自动更新
- **D-04:** 章节生成成功后自动更新伏笔状态：本章节需埋设的伏笔从 pending → in_progress，本章节需回收的伏笔从 in_progress → completed
- **D-05:** 章节重新生成时不回滚伏笔状态。用户重新生成通常是为了改内容而非伏笔，保持已更新的状态

### 实现路径
- **D-06:** 参照 Phase 13 角色约束注入模式——在 PromptTemplateBuilder 中添加伏笔约束构建方法（类似 buildPlannedCharacterInfoText），注入到 buildChapterPrompt() 的变量中
- **D-07:** 同时更新 llm_chapter_generate_standard 模板，增加伏笔约束变量占位符

### Claude's Discretion
- 约束文本的具体措辞和排版格式
- 无伏笔约束时是否显示空提示或完全省略
- 状态更新的时机（生成完成后立即更新 vs 生成过程中更新）
- 批量状态更新的优化策略

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 章节生成核心逻辑
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterContentGenerateTaskStrategy.java` — 章节内容生成策略，使用 PromptTemplateBuilder.buildChapterPrompt()
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` — 章节提示词构建器，含角色约束注入模式（hasPlannedCharacters + buildPlannedCharacterInfoText）

### 伏笔服务
- `ai-factory-backend/src/main/java/com/aifactory/service/ForeshadowingService.java` — 伏笔服务，含 CRUD、getForeshadowingList()、buildActiveForeshadowingContext()
- `ai-factory-backend/src/main/java/com/aifactory/entity/Foreshadowing.java` — 伏笔实体（含 plantedVolume/plannedCallbackVolume 分卷字段、plantedChapter/plannedCallbackChapter 章节定位、status 字段）

### 提示词模板
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptTemplateService.java` — 模板服务
- 章节生成模板 code: `llm_chapter_generate_standard` — 需增加伏笔约束变量占位符

### Phase 先例
- `.planning/phases/16-ai-chapter-planning/16-CONTEXT.md` — Phase 16 决策（伏笔上下文注入模式、<fs>/<fp> 标签）
- Phase 13 角色约束注入模式 — PromptTemplateBuilder 中的 hasPlannedCharacters/buildPlannedCharacterInfoText 是本 phase 的直接参照

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PromptTemplateBuilder.buildChapterPrompt()` — 章节提示词统一构建入口，角色约束已在此注入，伏笔约束可加在同一位置
- `PromptTemplateBuilder.hasPlannedCharacters()` + `buildPlannedCharacterInfoText()` — 角色约束注入的完整实现模式，可直接参照
- `ForeshadowingService.getForeshadowingList(queryDto)` — 已支持按 plantedChapter/plannedCallbackChapter、status 筛选，可查询当前章节相关伏笔
- `ForeshadowingService.buildActiveForeshadowingContext()` — Phase 16 已实现的伏笔上下文构建方法（规划阶段用），生成阶段需不同的格式

### Established Patterns
- **指令式约束段落**: Phase 13 角色约束用"【角色约束 - 必须严格遵循】"格式，伏笔约束遵循同样风格
- **模板变量注入**: buildChapterPrompt() 通过 variables map 注入到模板，新伏笔约束作为新变量
- **fallback 模式**: 角色约束有 fallback（无规划角色时用全量角色列表），伏笔约束无此需求——无伏笔时省略即可
- **全局章节序号**: Phase 15 D-04 决定章节编号为全局序号，查询直接用 plantedChapter/plannedCallbackChapter 匹配

### Integration Points
- `PromptTemplateBuilder.buildChapterPrompt()` — 注入伏笔约束变量到生成提示词
- `ChapterContentGenerateTaskStrategy.generateContent()` — 生成完成后调用状态更新
- `ForeshadowingService` — 查询当前章节伏笔 + 批量更新状态
- `llm_chapter_generate_standard` 模板 — 增加伏笔约束变量占位符

</code_context>

<specifics>
## Specific Ideas

- 约束段落示例：`【本章节必须埋设的伏笔】\n1. 神秘钥匙 — 主角在废弃矿洞中发现一把古钥匙\n请务必自然地融入情节，不可生硬。`
- 状态更新查询：`plantedChapter == currentGlobalChapter AND status == 'pending'` → 批量更新为 in_progress
- 状态更新查询：`plannedCallbackChapter == currentGlobalChapter AND status == 'in_progress'` → 批量更新为 completed
- 与角色约束注入模式高度一致——同一个 PromptTemplateBuilder 中的不同方法

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 17-ai-generation-constraints*
*Context gathered: 2026-04-11*
