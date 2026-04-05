# Phase 6: 独立提示词模板 - Research

**Researched:** 2026-04-03
**Domain:** AI prompt template management, SQL migration, prompt content extraction
**Confidence:** HIGH

## Summary

Phase 6 creates three new AI prompt templates (geography, power system, faction) by extracting the corresponding sections from the existing unified `llm_worldview_create` template, and then updates the unified template to remove those sections. The entire phase is SQL-only: one migration script creates 3 new `ai_prompt_template` + `ai_prompt_template_version` rows and updates the existing unified template's content.

The existing prompt template infrastructure is mature: `PromptTemplateService.executeTemplate(code, vars)` handles template lookup, version resolution, and variable substitution via Hutool `StrUtil.format()` with `{variableName}` placeholders. New templates only need correct DB rows to become immediately usable by Phase 7's API layer.

**Primary recommendation:** Write a single Flyway-compatible SQL migration (V4) that INSERTs 3 new templates and UPDATEs the unified template. Extract prompt content verbatim from the current DB template, splitting at the `<g>`, `<p>`, and `<f>` section boundaries.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 从现有 `llm_worldview_create` 统一模板中提取地理环境、力量体系、阵营势力三部分指令，各自成为独立模板
- **D-02:** 三个独立模板的 AI 输出格式完全复用现有 `<g>/<p>/<f>` 标签格式，与当前 `WorldviewTaskStrategy` 解析逻辑一致，Phase 7 无需新增解析代码
- **D-03:** 独立模板的提示词指令从现有统一模板中提取对应模块的格式说明和内容要求，保持格式一致性
- **D-04:** 阵营势力独立模板（`llm_faction_create`）使用**结构化 XML 嵌入**方式注入已有的地理环境和力量体系数据 -- 即从数据库读取已入库的数据，序列化为 `<g>...</g>` 和 `<p>...</p>` XML 片段，直接嵌入提示词
- **D-05:** AI 看到的是和自己输出时相同的格式，名称交叉引用最准确
- **D-06:** `llm_worldview_create` 精简后仅保留：世界类型 `<t>`、世界背景 `<b>`、时间线 `<l>`、世界规则 `<r>` 四个字段
- **D-07:** 精简后仍保持 `<w>` 根标签包裹的 XML 格式，确保 Phase 8 组合调用时能无缝拼接
- **D-08:** 地理环境模板（`llm_geography_create`）、力量体系模板（`llm_power_system_create`）、精简世界观模板均复用现有 4 个变量：`projectDescription`、`storyTone`、`storyGenre`、`tagsSection`
- **D-09:** 阵营势力模板（`llm_faction_create`）额外增加 `geographyContext` 和 `powerSystemContext` 两个变量

### Claude's Discretion
- 各独立模板的具体提示词措辞和细节（从现有统一模板中提取并优化）
- 地理/力量体系独立模板中是否需要额外上下文（如世界类型信息）
- 模板的 scenario 标签值命名

### Deferred Ideas (OUT OF SCOPE)
None
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PROMPT-01 | 地理环境拥有独立 AI 提示词模板，仅包含地理环境相关指令和上下文 | Extract `<g>` section from current unified template (V3 SQL, lines 54-96). New template code: `llm_geography_create` |
| PROMPT-02 | 力量体系拥有独立 AI 提示词模板，仅包含力量体系相关指令和上下文 | Extract `<p>` section from current unified template (V3 SQL, lines 29-53 + format rules). New template code: `llm_power_system_create` |
| PROMPT-03 | 阵营势力拥有独立 AI 提示词模板，仅包含阵营势力相关指令和上下文（含已生成的地理环境和力量体系数据作为上下文输入） | Extract `<f>` section from current unified template (V3 SQL, lines 58-131). New template code: `llm_faction_create`. Adds `geographyContext` + `powerSystemContext` variables per D-09 |
| PROMPT-04 | 原有世界观整体生成提示词模板剔除地理环境、力量体系、阵营势力三个模块的生成指令 | UPDATE existing `llm_worldview_create` template_content: keep only `<t>`, `<b>`, `<l>`, `<r>` sections per D-06 |
</phase_requirements>

## Standard Stack

### Core
| Library/Component | Version | Purpose | Why Standard |
|-------------------|---------|---------|--------------|
| ai_prompt_template table | existing | Template master record (templateCode, scenario, serviceType) | Project's established pattern |
| ai_prompt_template_version table | existing | Template content versions (templateContent, variableDefinitions, versionNumber) | Project's versioned template pattern |
| Hutool StrUtil.format() | 5.8.24 | Variable substitution `{varName}` | Already used by PromptTemplateServiceImpl |
| Flyway SQL migration | V3 existing | Schema/data migration | Project convention (V3__faction_prompt_template.sql) |

### Supporting
| Library/Component | Version | Purpose | When to Use |
|-------------------|---------|---------|-------------|
| PromptTemplateService.executeTemplate() | existing | Execute template by code with variable map | Phase 7 API will call this |
| BasicSettingsDictionary | existing | Convert enum keys to Chinese labels | Variable preparation for storyTone, novelType |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| SQL migration INSERT | Java service initBuiltinTemplates() | Java approach requires app restart + code change; SQL is cleaner, matches V3 precedent |
| New XML serialization | Existing text-based buildPowerSystemConstraint() | D-04 requires XML embedding; text format won't work for faction template context injection |

**Installation:**
No new packages needed. This is purely a SQL migration phase.

**Version verification:** N/A -- no new dependencies.

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-backend/src/main/resources/db/migration/
  V4__independent_prompt_templates.sql    # New migration: 3 INSERT + 1 UPDATE
```

### Pattern 1: Template Creation via SQL INSERT
**What:** Create `ai_prompt_template` master row + `ai_prompt_template_version` v1 row in a single migration
**When to use:** Adding new prompt templates to the system
**Example:**
```sql
-- Step 1: Insert template master record
INSERT INTO ai_prompt_template (template_code, template_name, service_type, scenario, ...)
VALUES ('llm_geography_create', '地理环境独立生成', 'llm', 'worldview_geography_generate', ...);

-- Step 2: Insert v1 version content (using LAST_INSERT_ID() or subquery)
INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, ...)
VALUES (
  (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_geography_create'),
  1,
  '...prompt content...',
  '[{"name":"projectDescription",...}]',
  ...
);

-- Step 3: Update master record's current_version_id
UPDATE ai_prompt_template
SET current_version_id = (SELECT MAX(id) FROM ai_prompt_template_version
  WHERE template_id = (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_geography_create'))
WHERE template_code = 'llm_geography_create';
```

### Pattern 2: Variable Substitution with StrUtil.format()
**What:** Placeholders in template content use `{variableName}` format
**When to use:** All prompt templates
**Example:**
```
Template content:
"根据以下项目描述，为这部小说创建地理环境设定：\n\n【项目描述】\n{projectDescription}\n\n【故事类型】{storyGenre}"

Variables map:
{"projectDescription": "一部修仙小说...", "storyGenre": "仙侠", "storyTone": "严肃", "tagsSection": "【标签】修仙,热血"}
```

### Pattern 3: Template Content Extraction
**What:** Split existing unified template at XML section boundaries
**When to use:** Creating independent templates from unified one
**Extraction points from current unified template (V3 SQL):**

The current `llm_worldview_create` template has these distinct sections:
1. **Common intro** (lines 1-28): Role definition, project context variables, XML format overview with `<w>` root
2. **Power system `<p>`** (lines 29-53): `<ss><name>...<steps>` format + level/step structure
3. **Geography `<g>`** (lines 54-96): `<r name="..."><d>...</d></r>` format + nested examples + generation requirements
4. **Faction `<f>`** (lines 58-131 in V3, but interspersed): `<faction><n>...<d>...</d></faction>` format + type/power/regions/relation rules
5. **Timeline `<l>` + Rules `<r>`** (lines 72-73): Simple CDATA fields
6. **Format requirements** (lines 133-145): CDATA rules, markdown rules, token-saving rules
7. **Content requirements** (lines 147-150): Quality guidelines

### Anti-Patterns to Avoid
- **Hardcoding prompt content in Java:** All prompts must live in DB via migration SQL, not in code
- **Using `code` column in WHERE clauses:** The actual column is `template_code` (MyBatis-Plus maps `templateCode` to `template_code` via camelCase-to-underscore convention). V3 migration has `WHERE code = ...` which may be wrong -- verify the actual DB column name before writing V4
- **Ignoring `is_active` flag:** Template versions must have `is_active = 1` to be picked up by `PromptTemplateServiceImpl`
- **Forgetting `current_version_id`:** Master record must point to the version's ID, or `executeTemplate()` will return null

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| XML serialization for geography/power system | Custom XML builder | Phase 7 will add `buildGeographyXml()` and `buildPowerSystemXml()` methods | This phase only creates templates; serialization code is Phase 7's scope |
| Template variable validation | Custom validator | Existing `validateRequiredVariables()` in PromptTemplateServiceImpl | Already handles the pattern, even if simplified |
| Template execution | Custom template runner | `PromptTemplateService.executeTemplate(code, vars)` | Fully implemented, handles version resolution and variable substitution |

**Key insight:** Phase 6 is purely about SQL data (prompt content). No Java code changes are needed. The existing infrastructure handles everything.

## Runtime State Inventory

> Not applicable -- this is not a rename/refactor/migration phase in the traditional sense. It's a data creation phase (new DB rows).

## Common Pitfalls

### Pitfall 1: Column Name Mismatch in SQL Migration
**What goes wrong:** V3 migration uses `WHERE code = 'llm_worldview_create'` but the actual DB column might be `template_code` (matching the entity field `templateCode`)
**Why it happens:** The entity uses `@TableName("ai_prompt_template")` with field `templateCode` which MyBatis-Plus maps to `template_code` column by default
**How to avoid:** In V4 migration, always use `template_code` in WHERE clauses, not `code`. Verify by checking if V3 actually ran successfully (if it did, the column might indeed be `code`)
**Warning signs:** Migration fails with "Unknown column 'code'"

### Pitfall 2: Missing current_version_id After INSERT
**What goes wrong:** Template master record is inserted but `current_version_id` remains NULL, causing `executeTemplate()` to return null
**Why it happens:** INSERT for version happens after INSERT for master record, requiring an UPDATE to link them
**How to avoid:** Always follow the 3-step pattern: INSERT master -> INSERT version -> UPDATE master.current_version_id
**Warning signs:** Phase 7 API calls return "提示词模板版本不存在"

### Pitfall 3: Variable Placeholder Format Confusion
**What goes wrong:** Using `${var}` or `{{var}}` instead of `{var}` in template content
**Why it happens:** Different template engines use different syntax
**How to avoid:** Use Hutool's `{variableName}` format exclusively. Verify against existing V3 template content which uses `{projectDescription}`, `{storyGenre}`, etc.
**Warning signs:** Template execution returns literal `{variableName}` text instead of substituted values

### Pitfall 4: Geography Format Inconsistency
**What goes wrong:** V3 template uses `<r name="...">` but CONTEXT.md says current DB format is `<r><n>名称</n><d>描述</d></r>`
**Why it happens:** The template was updated after V3 -- the V3 SQL shows the old `name` attribute format, but the code in `parseSingleRegion()` already handles both formats (checks `<n>` first, falls back to `name` attribute)
**How to avoid:** New geography template must use the NEW format `<r><n>名称</n><d>描述</d></r>` consistently, matching what the DOM parser expects as primary format
**Warning signs:** AI output uses old format, parser falls back to attribute matching, potential data loss

### Pitfall 5: Faction Template Context Variable Names
**What goes wrong:** Using wrong variable names for geography/power system context in faction template
**Why it happens:** D-09 specifies `geographyContext` and `powerSystemContext` but template authoring might use different names
**How to avoid:** Use exactly `{geographyContext}` and `{powerSystemContext}` as specified in D-09
**Warning signs:** Phase 7 code passes variables with different names, substitution silently fails

## Code Examples

Verified patterns from existing source code:

### Template Variable Preparation (from WorldviewTaskStrategy.buildWorldviewPrompt)
```java
// Source: WorldviewTaskStrategy.java lines 303-318
Map<String, Object> variables = new HashMap<>();
variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
variables.put("novelType", novelType != null && !novelType.isEmpty() ? BasicSettingsDictionary.getNovelType(novelType) : "待补充");

if (tags != null && !tags.isEmpty()) {
    variables.put("tagsSection", "【标签】" + tags);
} else {
    variables.put("tagsSection", "");
}

String templateCode = "llm_worldview_create";
return promptTemplateService.executeTemplate(templateCode, variables);
```

### Geography DOM Parsing Format (from WorldviewTaskStrategy.parseSingleRegion)
```java
// Source: WorldviewTaskStrategy.java lines 446-477
// Current code supports BOTH formats but prefers <n>/<d> sub-tags:
// NEW format: <r><n>名称</n><d>描述</d><r>子区域</r></r>
// OLD format: <r name="名称"><d>描述</d></r>  (fallback)

NodeList nNodes = rElement.getElementsByTagName("n");
if (nNodes.getLength() > 0) {
    region.setName(nNodes.item(0).getTextContent().trim());
} else {
    region.setName(rElement.getAttribute("name"));  // fallback to old format
}
```

### SQL Migration Pattern (from V3__faction_prompt_template.sql)
```sql
-- Source: V3__faction_prompt_template.sql
-- Pattern: UPDATE existing template version content
UPDATE ai_prompt_template_version
SET template_content = '...prompt text...',
    version_comment = '势力部分从纯文本改为结构化XML格式'
WHERE template_id = (SELECT id FROM ai_prompt_template WHERE code = 'llm_worldview_create')
  AND is_active = 1;
```

### Entity Structure (from AiPromptTemplate.java)
```java
// Key fields for new template creation:
// templateCode: 'llm_geography_create', 'llm_power_system_create', 'llm_faction_create'
// templateName: Display name in Chinese
// serviceType: 'llm'
// scenario: 'worldview_geography_generate', etc. (Claude's discretion)
// currentVersionId: Must link to version row after INSERT
// isActive: true (1)
// isSystem: true (1) -- system builtin template
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Geography `<r name="...">` attribute format | `<r><n>名称</n><d>描述</d></r>` sub-tag format | During Phase 5 refactor | New templates must use new format exclusively |
| Single unified worldview template | Independent per-module templates | This phase | Enables on-demand regeneration of individual modules |

**Deprecated/outdated:**
- V3 SQL `WHERE code = ...`: Column may actually be `template_code`. Verify before reuse.

## Template Content Design

### Template 1: llm_geography_create (PROMPT-01)
- **Variables:** `projectDescription`, `storyTone`, `storyGenre`, `tagsSection` (per D-08)
- **Output format:** `<g><r><n>名称</n><d>描述</d></r></g>` (must use new sub-tag format)
- **Content:** Extract geography-specific instructions from unified template
- **Context:** World type information from projectDescription is sufficient (no need for explicit worldType variable)

### Template 2: llm_power_system_create (PROMPT-02)
- **Variables:** `projectDescription`, `storyTone`, `storyGenre`, `tagsSection` (per D-08)
- **Output format:** `<p><ss><name>...<steps>...</steps></ss></p>` (Jackson-parseable format)
- **Content:** Extract power system-specific instructions from unified template

### Template 3: llm_faction_create (PROMPT-03)
- **Variables:** `projectDescription`, `storyTone`, `storyGenre`, `tagsSection` + `geographyContext` + `powerSystemContext` (per D-09)
- **Output format:** `<f><faction>...</faction></f>` (DOM-parsed format)
- **Content:** Extract faction-specific instructions from unified template, plus context injection points
- **Context injection:** `{geographyContext}` and `{powerSystemContext}` placeholders where the XML data will be embedded

### Template 4: llm_worldview_create UPDATE (PROMPT-04)
- **Variables:** Same 4 variables as current (per D-08)
- **Output format:** `<w><t>...</t><b>...</b><l>...</l><r>...</r></w>` (only 4 fields per D-06)
- **Content:** Remove all `<g>`, `<p>`, `<f>` format instructions and content requirements

## Open Questions

1. **DB column name: `code` vs `template_code`?**
   - What we know: V3 migration uses `WHERE code = 'llm_worldview_create'`. Entity maps `templateCode` field to table.
   - What's unclear: Whether the actual MySQL column is named `code` or `template_code`.
   - Recommendation: In V4 migration, check the actual table schema first. Use `SHOW COLUMNS FROM ai_prompt_template` to verify. Most likely `template_code` given MyBatis-Plus convention.

2. **Should the new templates reference `storyGenre` in variable definitions?**
   - What we know: The current unified template uses `{storyGenre}` in its content. The `buildWorldviewPrompt()` method maps `novelType` to `storyGenre` variable via `BasicSettingsDictionary.getNovelType()`.
   - What's unclear: Whether the new template variable definitions JSON should list `storyGenre` or `novelType` as the variable name.
   - Recommendation: Use `storyGenre` to match existing template convention (the template placeholder is `{storyGenre}`).

3. **How to handle the `llm_worldview_create` UPDATE -- new version or content replacement?**
   - What we know: Existing system supports version management. V3 used direct UPDATE on existing version row.
   - What's unclear: Whether to create a new version row or UPDATE the existing one.
   - Recommendation: Follow V3 pattern -- UPDATE the existing active version's `template_content`. This is simpler and avoids version proliferation for a template that hasn't been released to production yet.

## Environment Availability

Step 2.6: SKIPPED (no external dependencies identified -- this phase is purely SQL migration, no tools/services/runtimes beyond the MySQL database already in use)

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | None -- tests in `src/test/java/` |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=*Test` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PROMPT-01 | Geography template exists in DB with correct format instructions | SQL verification | Manual -- verify migration output | N/A -- SQL phase |
| PROMPT-02 | Power system template exists in DB with correct format instructions | SQL verification | Manual -- verify migration output | N/A -- SQL phase |
| PROMPT-03 | Faction template exists with geographyContext + powerSystemContext variables | SQL verification | Manual -- verify migration output | N/A -- SQL phase |
| PROMPT-04 | Unified template content reduced to t/b/l/r only | SQL verification | Manual -- verify migration output | N/A -- SQL phase |

### Sampling Rate
- **Per task commit:** Visual review of SQL migration content
- **Per wave merge:** Verify migration runs without errors against test DB
- **Phase gate:** All 4 templates queryable via `PromptTemplateService.getTemplate(code)`

### Wave 0 Gaps
None -- this is a SQL-only phase. Validation is through:
1. SQL syntax verification (dry-run against test DB)
2. Post-migration query: `SELECT template_code, template_name FROM ai_prompt_template WHERE template_code LIKE 'llm_%create'` should return 4 rows
3. Template variable presence: Each template's `variable_definitions` JSON must list the required variables

**Recommended verification SQL (post-migration):**
```sql
-- Verify all 4 templates exist
SELECT t.template_code, t.template_name, t.current_version_id, v.version_number
FROM ai_prompt_template t
JOIN ai_prompt_template_version v ON v.id = t.current_version_id
WHERE t.template_code IN ('llm_worldview_create', 'llm_geography_create', 'llm_power_system_create', 'llm_faction_create');

-- Verify unified template no longer contains geography/power/faction instructions
SELECT LENGTH(template_content), template_content
FROM ai_prompt_template_version v
JOIN ai_prompt_template t ON t.current_version_id = v.id
WHERE t.template_code = 'llm_worldview_create';

-- Verify faction template has context variables
SELECT variable_definitions
FROM ai_prompt_template_version v
JOIN ai_prompt_template t ON t.current_version_id = v.id
WHERE t.template_code = 'llm_faction_create';
```

## Sources

### Primary (HIGH confidence)
- Source code analysis: `PromptTemplateServiceImpl.java` -- template execution logic verified
- Source code analysis: `WorldviewTaskStrategy.java` -- XML parsing and prompt building verified
- Source code analysis: `V3__faction_prompt_template.sql` -- migration pattern and current template content verified
- Source code analysis: `AiPromptTemplate.java` + `AiPromptTemplateVersion.java` -- entity structure verified

### Secondary (MEDIUM confidence)
- `PromptContextBuilder.java` -- context building patterns verified (no XML serialization method exists yet)

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all components verified in source code
- Architecture: HIGH - template creation pattern well established from V3
- Pitfalls: HIGH - identified from source code review (column name issue, format inconsistency)
- Template content design: HIGH - extraction points clearly identifiable from V3 SQL content

**Research date:** 2026-04-03
**Valid until:** 2026-05-03 (stable -- no fast-moving dependencies)
