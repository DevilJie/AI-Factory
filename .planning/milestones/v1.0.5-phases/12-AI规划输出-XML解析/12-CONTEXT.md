# Phase 12: AI 规划输出 + XML 解析 - Context

**Gathered:** 2026-04-08
**Status:** Ready for planning

<domain>
## Phase Boundary

用户生成章节规划时，AI 输出包含角色规划信息，系统自动解析并持久化到数据库。具体范围：
1. 章节规划提示词模板 (template_id=6) 增加角色规划输出指令和 XML 标签格式
2. 角色列表（全量）注入到规划提示词，AI 规划每个章节的登场角色
3. 后端从 AI 输出 XML 解析角色规划数据，通过 NameMatchUtil 匹配已有角色 ID
4. 角色规划数据持久化为 `planned_characters` JSON 字段

不包含：章节生成约束注入（Phase 13）、前端展示（Phase 14）。

</domain>

<decisions>
## Implementation Decisions

### XML 解析方式
- **D-01:** 切换为 DOM 解析 — 参照 WorldviewXmlParser 模式，替代当前 Jackson XML (ChapterPlanXmlDto) 解析章节规划 XML。项目约束已明确 Jackson XML 无法处理嵌套同名标签，角色规划的 `<ch>` 标签在 `<o>` 内会有多个同名实例
- **D-02:** DOM 解析一次性提取章节主字段 + 角色规划数据 — 无需两遍解析。解析后转换为现有 saveChaptersToDatabase 所需的 Map 格式（兼容现有保存逻辑），额外附加 plannedCharacters JSON

### 提示词模板升级
- **D-03:** 全量角色注入 — 向规划模板注入项目所有角色的简要信息（名称 + 角色类型 + 势力），让 AI 自行判断每个章节谁登场。复用现有 `{characterInfo}` 变量位置
- **D-04:** 简要指令 + 格式约束 — 在模板中增加"每个章节必须输出登场角色列表"指令 + 角色规划 XML 标签格式说明（角色名、戏份梗概、重要程度）。不加过多规划哲学指导
- **D-05:** 允许新角色 — 提示词明确说明"可以使用角色列表中的角色，也可以根据情节需要规划新角色"。新角色名不在数据库中时 characterId 为 null，角色名保留在 characterName 字段

### 角色规划 XML 标签格式
- **D-06:** 在每个 `<o>` 章节标签内增加角色规划子标签 — 使用 `<ch>` 包裹每个规划角色，内含 `<cn>` (character name)、`<cd>` (character description/戏份)、`<ci>` (character importance: high/medium/low)
- **D-07:** 角色规划标签为可选 — 某些章节可能无特定角色规划（如过渡章节），解析时 gracefully 处理空值

### 名称匹配与持久化
- **D-08:** 解析后立即匹配 — XML 解析完成后，对每个角色名调用 NameMatchUtil.matchByName() 匹配已有角色。匹配结果写入 characterId，失败则 characterId=null
- **D-09:** 持久化为 planned_characters JSON — 按 Phase 11 定义的 schema：`[{"characterName":"李云","roleType":"protagonist","roleDescription":"...","importance":"high","characterId":42}]`。roleType 从 AI 输出推断或默认 "supporting"

### Claude's Discretion
- DOM 解析的具体实现位置（是否复用 WorldviewXmlParser 或新建解析方法）
- 角色列表注入的格式化细节（文本列表 vs XML 格式）
- parseChaptersXml 重构后的返回类型（是否保留 Map 格式或使用新的数据结构）
- ChapterPlanXmlDto 是否保留（DOM 解析后可能不再需要）
- 角色规划的 roleType 字段：AI 是否输出还是系统默认推断

### Folded Todos
None.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 章节规划 XML 解析（需修改）
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanXmlDto.java` — 当前 Jackson XML DTO，DOM 解析后可能废弃或简化
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` lines 635-700 — parseChaptersXml() 方法，需重构为 DOM 解析
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java` lines 706-737 — saveChaptersToDatabase() 方法，需增加 plannedCharacters 持久化

### DOM 解析模式参考
- `ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java` — DOM 解析模式参考，三级名称匹配已内嵌
- `ai-factory-backend/src/main/java/com/aifactory/common/XmlParser.java` — 通用 XML 解析工具（当前用 Jackson，DOM 解析可参考 WorldviewXmlParser）

### 名称匹配
- `ai-factory-backend/src/main/java/com/aifactory/common/NameMatchUtil.java` — 三级名称匹配工具（Phase 11 实现），角色匹配使用 CHARACTER_SUFFIXES

### 提示词模板
- `sql/init.sql` line 699 — template_id=6 (llm_outline_chapter_generate) 章节规划模板定义
- `sql/init.sql` line 719 — template_id=6 的模板内容（template_version id=6），需增加角色规划输出指令和 XML 标签

### 实体与 DTO
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` — 章节规划实体，已有 plannedCharacters 字段
- `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` — 章节规划响应 DTO，已有 plannedCharacters 字段

### 角色数据访问
- `ai-factory-backend/src/main/java/com/aifactory/service/NovelCharacterService.java` — 角色 CRUD 服务，需查询全量角色列表用于注入
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelCharacterMapper.java` — 角色数据访问

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorldviewXmlParser` DOM 解析模式 — 已验证可靠，使用 getChildNodes() 精确控制节点遍历，避免 getElementsByTagName 取到所有后代
- `NameMatchUtil` — 通用三级匹配工具，直接可用
- `ChapterGenerationTaskStrategy.buildChapterPromptUsingTemplate()` — 已有提示词构建方法，角色信息注入已有 `{characterInfo}` 占位符
- `NovelCharacterService` — 角色查询服务，可获取项目全量角色列表

### Established Patterns
- **DOM 解析**: WorldviewXmlParser 已验证 DOM 解析对嵌套同名标签可靠（v1.0.2-v1.0.3）
- **JSON 列**: planned_characters 使用 String 类型，与 keyEvents/foreshadowingSetup 等一致
- **模板版本管理**: 通过 ai_prompt_template_version 表管理，新增版本 + is_active 切换
- **名称回查后填 ID**: WorldviewXmlParser 中已验证（先解析获取名称，后回查填 ID）

### Integration Points
- `ChapterGenerationTaskStrategy.parseChaptersXml()` — 主要修改点，Jackson → DOM
- `ChapterGenerationTaskStrategy.saveChaptersToDatabase()` — 需增加 plannedCharacters 写入
- `ChapterGenerationTaskStrategy.generateChaptersForVolume()` — 规划流程主入口，需增加角色列表查询和注入
- `PromptTemplateService` — 模板变量解析，需确保 characterInfo 变量包含全量角色数据
- 章节规划模板 (template_id=6) — 需创建新版本，增加角色规划输出指令

</code_context>

<specifics>
## Specific Ideas

- 角色规划 XML 标签设计参考：`<ch><cn>李云</cn><cd>发现密室线索并决定深入调查</cd><ci>high</ci></ch>`
- 角色列表注入格式参考：每行一个角色的简要信息，如 "李云 (protagonist, 青云门弟子)"
- 允许新角色的提示词措辞："以上为已有角色，你也可以根据情节需要安排新角色登场"

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 12-AI规划输出-XML解析*
*Context gathered: 2026-04-08*
