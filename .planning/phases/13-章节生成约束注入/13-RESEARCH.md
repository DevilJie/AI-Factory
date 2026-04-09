# Phase 13: 章节生成约束注入 - Research

**Researched:** 2026-04-09
**Domain:** Chapter content prompt engineering, constraint language injection, planned character routing
**Confidence:** HIGH

## Summary

Phase 13 modifies the chapter content generation prompt construction to inject planned character constraints when a chapter plan contains `plannedCharacters` data. Currently, `PromptTemplateBuilder.buildTemplateVariables()` (lines 149-209) always calls `buildCharacterPromptInfoList()` to fetch all non-NPC characters and formats them as a detailed reference block. When the chapter has planned characters (parsed and persisted in Phase 11-12), the system should instead inject a focused, constraint-enforced list of only those planned characters, replacing the full list entirely.

The modification surface is small and well-contained: one method in `PromptTemplateBuilder` (`buildTemplateVariables`) needs a conditional branch based on `chapterPlan.getPlannedCharacters()`. The `chapterPlan` object is already available in the call chain (`ChapterContentGenerateTaskStrategy` loads it at line 159 and passes it through to `buildChapterPrompt`). No database schema changes, no new entities, no frontend changes. The template variable `{characterInfo}` is the injection point -- it already exists in `llm_chapter_generate_standard` (version_id=1).

**Primary recommendation:** Add a planned-character branch in `PromptTemplateBuilder.buildTemplateVariables()` that reads `chapterPlan.getPlannedCharacters()`, parses the JSON, formats it as a constrained text block with opening/closing reminders, and substitutes it for `{characterInfo}`. When `plannedCharacters` is null/empty, fall through to existing full-list logic untouched.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 分段约束 -- 开头明确"你必须严格遵循以下角色安排，以下角色必须出场" + 结尾再次提醒"请检查你的输出是否遵循了上述角色约束"。双重提醒
- **D-02:** 仅约束角色出场（谁必须登场），不约束情节走向、戏份内容、对话风格等。AI 仍可自由发挥情节细节
- **D-03:** 仅提示词级别约束，无后端检测/重试机制。偏差在 Phase 14 前端对比视图中体现
- **D-04:** NPC 允许提示 -- 在规划角色约束后加一句"跑龙套、路人甲等 NPC 性质角色可根据情节需要自行安排"
- **D-05:** 纯文本列表格式注入 -- 每行一个角色的简要信息，如"李云 (protagonist) - 发现密室线索并决定深入调查 [high]"。简洁、AI 易理解
- **D-06:** 有规划角色时完全替换全量角色列表 -- AI 只看到规划角色，不保留全量列表作为参考
- **D-07:** 复用现有 `{characterInfo}` 模板变量 -- 有规划角色时填入规划角色文本，无规划角色时填入全量角色列表。代码改动最小
- **D-08:** 二分支干净回退 -- 有规划角色走约束注入分支，无规划角色走原有全量角色注入分支。两个分支互不干扰
- **D-09:** 无混合模式 -- 不存在"规划角色 + 全量参考"的中间状态
- **D-10:** 新角色自动创建（CG-03）不在本 phase 范围内 -- 用户明确表示仅处理提示词注入，角色提取/创建逻辑递延

### Claude's Discretion
- 约束语言的具体中文措辞（开头/结尾的精确表述）
- 规划角色文本列表的具体格式细节（字段顺序、分隔符）
- 代码中判断"有规划角色"的条件（plannedCharacters 非空/非 null/非空数组）
- 是否需要创建新的提示词模板版本还是修改现有模板

### Deferred Ideas (OUT OF SCOPE)
- **CG-03 新角色自动创建** -- 规划中出现数据库不存在的新角色时，章节生成后自动创建角色记录并建立章节关联。递延至后续 phase。用户明确表示本 phase 仅处理提示词注入。
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CG-01 | 章节生成提示词有规划角色时，仅注入规划角色信息（替换全量角色列表注入），避免矛盾信号 | `PromptTemplateBuilder.buildTemplateVariables()` lines 203-207 is the single injection point. `chapterPlan.getPlannedCharacters()` provides the JSON data. A conditional branch replaces the `buildCharacterPromptInfoList()` call when planned data exists. |
| CG-02 | 章节生成提示词使用"必须遵循"约束语言，强制 AI 严格按规划的角色和戏份生成内容 | New method `buildPlannedCharacterInfoText()` wraps parsed planned characters with D-01 constraint language (opening + closing reminders) and D-04 NPC allowance. Injected into `{characterInfo}` variable. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot 3.2.0 | 3.2.0 | Backend framework | Project constraint |
| MyBatis-Plus 3.5.5 | 3.5.5 | ORM | Project constraint |
| Jackson ObjectMapper | 2.x (via Spring Boot) | JSON parsing of plannedCharacters | Already injected in PromptTemplateBuilder, used throughout |
| JUnit 5 | 5.x | Unit testing | Project standard |

### No New Dependencies
This phase requires zero new libraries. All tools (`ObjectMapper`, `PromptTemplateService`, `NovelCharacterService`) are already injected and available in the target class.

## Architecture Patterns

### Current Data Flow (Chapter Content Generation)

```
ChapterContentGenerateTaskStrategy.generateContent()
    |
    |-- Loads NovelChapterPlan (chapterPlan) via chapterPlanMapper.selectById(planId)
    |       Contains: plannedCharacters (JSON String), plotOutline, chapterTitle, etc.
    |
    |-- Calls promptTemplateBuilder.buildChapterPrompt(projectId, chapterPlan, ...)
            |
            |-- buildTemplateVariables(projectId, chapterPlan, ...)
                    |
                    |-- Line 203-207: characterInfo injection
                    |   ALWAYS calls buildCharacterPromptInfoList(projectId, chapterNumber)
                    |   Returns List<CharacterPromptInfo> of ALL non-NPC characters
                    |   Then buildCharacterInfoText() formats as detailed block
                    |
                    |-- variables.put("characterInfo", formattedText)
                    |
            |-- promptTemplateService.executeTemplate("llm_chapter_generate_standard", variables)
                    |
                    |-- Template substitutes {characterInfo} with the formatted text
```

### Target Data Flow (With Planned Character Branch)

```
buildTemplateVariables(projectId, chapterPlan, ...)
    |
    |-- NEW: Check chapterPlan.getPlannedCharacters()
    |       |
    |       |-- NON-NULL + NON-EMPTY JSON array:
    |       |       Parse JSON -> List<Map<String, Object>>
    |       |       Build constraint text with:
    |       |         - Opening constraint (D-01)
    |       |         - One line per planned character (D-05)
    |       |         - NPC allowance sentence (D-04)
    |       |         - Closing reminder (D-01)
    |       |       variables.put("characterInfo", constraintText)
    |       |
    |       |-- NULL or EMPTY:
    |       |       EXISTING logic unchanged
    |       |       buildCharacterPromptInfoList() + buildCharacterInfoText()
    |       |       variables.put("characterInfo", fullListText)
```

### Recommended Project Structure
No new files needed. All changes are within existing files:

```
ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/
    PromptTemplateBuilder.java        -- ADD: buildPlannedCharacterInfoText() method, MODIFY: buildTemplateVariables()
```

### Pattern: Template Variable Dual-Path
**What:** A template variable that has two possible content sources depending on runtime data.
**When to use:** When the same template variable serves different purposes based on available context.
**Example:**
```java
// In buildTemplateVariables():
String characterInfo;
if (hasPlannedCharacters(chapterPlan)) {
    characterInfo = buildPlannedCharacterInfoText(chapterPlan.getPlannedCharacters());
} else {
    List<CharacterPromptInfo> allChars = buildCharacterPromptInfoList(projectId, chapterPlan.getChapterNumber());
    characterInfo = buildCharacterInfoText(allChars);
}
variables.put("characterInfo", characterInfo);
```

### Anti-Patterns to Avoid
- **Mixed injection:** Never inject both planned characters AND full character list in the same prompt. This creates contradictory signals (D-09).
- **Over-constraining:** Do not add constraints beyond character presence. No plot direction, no dialogue style, no scene details (D-02).
- **Backend validation:** Do not add post-generation character checking or retry logic (D-03). Phase 14 handles deviation display.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON parsing of plannedCharacters | Custom string splitting | ObjectMapper.readValue() with List<Map> | Already injected in class, handles edge cases, consistent with codebase |
| Role type display | Custom mapping | formatRoleType() (line 737) | Already exists in PromptTemplateBuilder |
| Empty/null checking | Complex guard logic | Simple StringUtils or null+isEmpty check | plannedCharacters is String type JSON, standard null/empty check sufficient |

## Common Pitfalls

### Pitfall 1: Parsing Empty JSON Array vs Null
**What goes wrong:** `plannedCharacters` can be `"[]"` (empty array) or `null` -- both should trigger the full-list fallback.
**Why it happens:** Phase 12 saves plannedCharacters only when `<ch>` tags are found. Chapters without character plans have null.
**How to avoid:** Check both null AND empty after parsing: `plannedChars == null || plannedChars.isEmpty()`.
**Warning signs:** If only null-check is done, chapters with `"[]"` plannedCharacters would show empty character info instead of full list.

### Pitfall 2: Modifying the Wrong Strategy Class
**What goes wrong:** `ChapterGenerationTaskStrategy` handles chapter **planning** generation (outline), not chapter **content** generation. Modifying it would be wrong.
**Why it happens:** Both classes deal with "chapter generation" and the names are confusing.
**How to avoid:** Only modify `PromptTemplateBuilder` (called by `ChapterContentGenerateTaskStrategy`). Do NOT modify `ChapterGenerationTaskStrategy`.
**Warning signs:** If changes appear in `ChapterGenerationTaskStrategy.java`, they are in the wrong file.

### Pitfall 3: Constraint Language Too Long
**What goes wrong:** Adding verbose constraint paragraphs pushes the prompt over token limits, causing truncated chapter output.
**Why it happens:** The template already has substantial content. Adding 500+ chars of constraint text could cause issues for chapters with many planned characters.
**How to avoid:** Keep constraint language minimal -- one opening sentence, one line per character, one NPC sentence, one closing sentence. Target under 300 chars for the constraint wrapper (excluding character data).
**Warning signs:** If prompt length increases by more than ~500 chars total, the constraint text is too verbose.

### Pitfall 4: Template Version Unnecessary
**What goes wrong:** Creating a new template version for `llm_chapter_generate_standard` when only the variable content changes, not the template structure.
**Why it happens:** Misunderstanding that `{characterInfo}` is a variable, not template text.
**How to avoid:** The template itself does not change. The `{characterInfo}` variable already exists. Only the Java code that fills the variable changes. No new template version needed.
**Warning signs:** If a SQL migration or template version INSERT is being written, it is unnecessary.

## Code Examples

### Planned Characters JSON Schema (from NovelChapterPlan entity)
```java
// Source: NovelChapterPlan.plannedCharacters field (Phase 11/12)
// Schema: [{"characterName":"李云","roleType":"protagonist","roleDescription":"发现线索","importance":"high","characterId":42}]
// characterId may be null (name match failed)
String plannedCharactersJson = chapterPlan.getPlannedCharacters();
```

### Parsing Planned Characters
```java
// Source: existing pattern in ChapterGenerationTaskStrategy.resolveCharacterIds()
ObjectMapper objectMapper = new ObjectMapper();
List<Map<String, Object>> plannedChars = objectMapper.readValue(
    plannedCharactersJson,
    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
);
```

### Building Constraint Text (Recommended Implementation)
```java
/**
 * Build planned character constraint text for chapter generation prompt.
 * Uses segmented constraint language (D-01) with NPC allowance (D-04).
 * Format per character (D-05): "角色名 (roleType) - 角色描述 [importance]"
 */
private String buildPlannedCharacterInfoText(String plannedCharactersJson) {
    try {
        List<Map<String, Object>> plannedChars = objectMapper.readValue(
            plannedCharactersJson,
            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
        );

        if (plannedChars.isEmpty()) {
            return null; // triggers fallback to full list
        }

        StringBuilder sb = new StringBuilder();

        // Opening constraint (D-01)
        sb.append("【角色约束 - 必须严格遵循】\n");
        sb.append("以下是本章必须出场的角色，请严格按照此列表安排角色出场：\n\n");

        // Character list (D-05)
        for (Map<String, Object> charData : plannedChars) {
            String name = (String) charData.getOrDefault("characterName", "未知角色");
            String roleType = (String) charData.getOrDefault("roleType", "");
            String description = (String) charData.getOrDefault("roleDescription", "");
            String importance = (String) charData.getOrDefault("importance", "");

            sb.append("- ").append(name);
            if (roleType != null && !roleType.isEmpty()) {
                sb.append(" (").append(roleType).append(")");
            }
            if (description != null && !description.isEmpty()) {
                sb.append(" - ").append(description);
            }
            if (importance != null && !importance.isEmpty()) {
                sb.append(" [").append(importance).append("]");
            }
            sb.append("\n");
        }
        sb.append("\n");

        // NPC allowance (D-04)
        sb.append("注：跑龙套、路人甲等 NPC 性质角色可根据情节需要自行安排。\n\n");

        // Closing reminder (D-01)
        sb.append("请确认你的章节内容包含了上述所有必须出场的角色。\n");

        return sb.toString();
    } catch (Exception e) {
        log.error("解析规划角色JSON失败: {}", e.getMessage());
        return null; // triggers fallback to full list
    }
}
```

### Checking for Planned Characters
```java
/**
 * Check if chapter plan has valid planned characters data.
 */
private boolean hasPlannedCharacters(NovelChapterPlan chapterPlan) {
    if (chapterPlan == null) return false;
    String pc = chapterPlan.getPlannedCharacters();
    if (pc == null || pc.trim().isEmpty()) return false;
    // Also check for empty JSON array
    return !pc.trim().equals("[]");
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Full character list always injected | Full list only when no planned characters | Phase 13 (this phase) | Reduces prompt noise, focuses AI on specific characters |
| No character presence constraints | Constraint language with double reminders | Phase 13 (this phase) | Improves AI adherence to planned character appearances |

**Not applicable for this phase** -- no deprecation concerns. This is an additive change to existing code.

## Open Questions

1. **Constraint language effectiveness**
   - What we know: LLMs generally follow explicit "must include" instructions well, especially with double reminders
   - What's unclear: Exact adherence rate for Chinese web novel generation context
   - Recommendation: Monitor effectiveness in Phase 14 (frontend deviation display). No pre-validation needed per D-03.

2. **roleType in plannedCharacters may be null for unmatched characters**
   - What we know: Phase 12 sets roleType from matched character or infers from importance. Some entries may still lack roleType.
   - What's unclear: How many planned characters have null roleType in practice
   - Recommendation: Handle gracefully in formatting -- skip roleType display if null (shown in code example above).

## Environment Availability

Step 2.6: SKIPPED (no external dependencies identified -- all changes are Java code within existing Spring Boot application, using already-injected services and ObjectMapper).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (via Spring Boot Test) |
| Config file | pom.xml (spring-boot-starter-test) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=PromptTemplateBuilderTest -q` |
| Full suite command | `cd ai-factory-backend && mvn test -q` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CG-01 | Planned characters replace full list in prompt | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testPlannedCharacterInjection -pl ai-factory-backend` | Wave 0 needed |
| CG-01 | Null plannedCharacters falls through to full list | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNullPlannedCharactersFallback -pl ai-factory-backend` | Wave 0 needed |
| CG-01 | Empty array plannedCharacters falls through to full list | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testEmptyPlannedCharactersFallback -pl ai-factory-backend` | Wave 0 needed |
| CG-02 | Constraint language present in planned character prompt | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testConstraintLanguagePresent -pl ai-factory-backend` | Wave 0 needed |
| CG-02 | NPC allowance sentence present | unit | `mvn test -Dtest=PromptTemplateBuilderTest#testNpcAllowancePresent -pl ai-factory-backend` | Wave 0 needed |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=PromptTemplateBuilderTest -pl ai-factory-backend -q`
- **Per wave merge:** `mvn test -pl ai-factory-backend -q`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilderTest.java` -- covers CG-01 and CG-02 (new test file needed)
- [ ] No shared fixtures needed -- each test constructs its own chapterPlan with appropriate plannedCharacters JSON

## Sources

### Primary (HIGH confidence)
- Codebase analysis: `PromptTemplateBuilder.java` (full file read, 748 lines) -- the exact modification target
- Codebase analysis: `ChapterContentGenerateTaskStrategy.java` (full file read, 447 lines) -- call chain entry point
- Codebase analysis: `NovelChapterPlan.java` (full file read, 130 lines) -- plannedCharacters field schema
- Codebase analysis: `sql/init.sql` (template version 1 content) -- `{characterInfo}` variable usage in template

### Secondary (MEDIUM confidence)
- Codebase analysis: `ChapterGenerationTaskStrategy.java` (full file read, 999 lines) -- pattern reference for planned character parsing and JSON handling
- Codebase analysis: `CharacterPromptInfo.java` (full file read, 104 lines) -- existing DTO structure

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing code
- Architecture: HIGH -- single-class modification, well-understood call chain
- Pitfalls: HIGH -- verified by reading all relevant source files
- Code examples: HIGH -- derived directly from existing codebase patterns

**Research date:** 2026-04-09
**Valid until:** 2026-05-09 (stable codebase, no external dependencies)
