# Phase 10: 角色体系关联与提取增强 - Research

**Researched:** 2026-04-06
**Domain:** Spring Boot 3.2 + MyBatis-Plus / Vue 3 + TypeScript / LLM XML prompt engineering
**Confidence:** HIGH

## Summary

This phase adds structured associations between characters and power systems (cultivation systems), and between characters and factions. The core technical work involves: (1) creating a new `novel_character_power_system` join table, (2) extending the XML DTO to include `<FC>` faction tags, (3) implementing name-to-ID resolution for power system names and level names, (4) updating the prompt template to add faction extraction rules and Chinese roleType definitions, and (5) extending the frontend character detail view with Drawer tabs for managing these associations.

The existing codebase has well-established patterns for all of these operations. The `NovelFactionCharacter` entity and `FactionDrawer` tab pattern provide exact templates for the new association table and UI. The `PowerSystemService.buildPowerSystemConstraint()` method already produces all system/level names needed for name resolution. The `ChapterCharacterExtractService` has the extraction pipeline ready to extend.

**Primary recommendation:** Follow the existing `novel_faction_character` join table pattern exactly for the new `novel_character_power_system` table. Use the `FactionDrawer` tab pattern for the character detail drawer. Extend the prompt template incrementally (v2 of template_id=15).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 复用名称回查模式 -- systemName 精确/模糊匹配 power_system ID，currentLevel 匹配 level ID。与 v1.0.2 的势力名称匹配同一策略
- **D-02:** 提取时 upsert 关联 -- 每次章节提取时更新 novel_character_power_system 关联的 current_realm_id/current_sub_realm_id。角色可能修炼多个体系（多个关联记录）
- **D-03:** 匹配失败时 ID 字段为 null，文本信息保留在 character_chapter 关联的 cultivation_level JSON 中。不丢弃数据
- **D-04:** XML 格式新增 `<FC>` 标签 -- 每个角色可输出多个 `<FC><N>势力名称</N><R>职位/角色</R></FC>`，复用势力名称回查 faction ID
- **D-05:** 提取时自动 upsert `novel_faction_character` 关联（已有表结构：faction_id + character_id + role），无需新建表
- **D-06:** 角色可属于多个势力（转投、双重身份），每个势力一条关联记录
- **D-07:** 提示词新增中文类型定义和判断规则 -- protagonist=主角/第一视角人物, supporting=重要配角/多次出现, antagonist=反派/与主角对抗, npc=过场人物/一两句台词
- **D-08:** 注入已有角色类型分布到提示词 -- "本项目已有 1 个 protagonist（李云），新提取的角色不会是 protagonist"。避免重复分类
- **D-09:** 保持 4 类不变 (protagonist/supporting/antagonist/npc)，不加 minor_supporting 等新类型
- **D-10:** Drawer Tab 扩展 -- 参照 FactionDrawer 的 Tab 模式，角色详情新增"修炼体系"和"所属势力"两个 Tab，支持增删改关联
- **D-11:** 角色列表卡片增加修炼境界名称和势力名称展示（如"玄武境 . 青云门长老"），一眼看到关联状态
- **D-12:** 服务端聚合 -- 角色 detail API 返回关联的 power_system + level + faction 信息（JOIN 查询），前端一次请求拿到完整数据
- **D-13:** 增量优化 -- 在现有模板基础上新增 FC 标签说明、角色类型中文定义、已有角色分布注入。保留现有 XML 格式和字段不变

### Claude's Discretion
- 具体的名称匹配容错规则（精确->模糊->跳过的阈值）
- 关联表索引策略
- Drawer Tab 内的具体 UI 组件选择（下拉/树形/列表）
- 列表卡片关联信息的排版细节

### Deferred Ideas (OUT OF SCOPE)
None
</user_constraints>

## Standard Stack

### Core (Existing - No New Dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| MyBatis-Plus | 3.5.5 | ORM for new entity + mapper | Project standard ORM, all entities use it |
| Jackson XML | (with Spring Boot 3.2) | XML DTO parsing for FC tags | Already used by XmlParser + ChapterCharacterExtractXmlDto |
| Tailwind CSS | 4.1.x | Frontend styling | Project standard, all UI uses utility classes |
| Vue 3 Composition API | 3.5.x | Frontend framework | All views use `<script setup>` pattern |

### No New Packages Required
All capabilities needed for this phase already exist in the project. No npm or Maven dependencies need to be added.

## Architecture Patterns

### Recommended Project Structure (File Changes)
```
# Backend - New Files
src/main/java/com/aifactory/entity/CharacterPowerSystem.java        # New join entity
src/main/java/com/aifactory/mapper/CharacterPowerSystemMapper.java  # New mapper

# Backend - Modified Files
src/main/java/com/aifactory/dto/ChapterCharacterExtractXmlDto.java  # Add FC inner class + field
src/main/java/com/aifactory/service/ChapterCharacterExtractService.java  # Add association logic
src/main/java/com/aifactory/service/NovelCharacterService.java      # Add detail with associations
src/main/java/com/aifactory/controller/NovelCharacterController.java # Extend detail API
src/main/resources/ (or SQL migration)                              # Add novel_character_power_system table

# Frontend - New Files
src/views/Project/Detail/components/CharacterPowerSystemTab.vue     # Power system tab
src/views/Project/Detail/components/CharacterFactionTab.vue         # Faction tab

# Frontend - Modified Files
src/views/Project/Detail/Characters.vue                             # Add drawer + association display
src/api/character.ts                                                # Add association API calls + types
```

### Pattern 1: Join Table Entity (mirrors NovelFactionCharacter)
**What:** Simple association table with auto-increment ID and foreign keys
**When to use:** For character <-> power_system and character <-> faction associations
**Example:**
```java
// Source: NovelFactionCharacter.java (existing pattern)
@Data
@TableName("novel_character_power_system")
public class CharacterPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long characterId;
    private Long powerSystemId;
    private Long currentRealmId;    // Maps to novel_power_system_level.id
    private Long currentSubRealmId; // Maps to novel_power_system_level_step.id
}
```

### Pattern 2: Mapper extends BaseMapper (mirrors NovelFactionCharacterMapper)
**What:** MyBatis-Plus BaseMapper provides all CRUD for simple association tables
**Example:**
```java
@Mapper
public interface CharacterPowerSystemMapper extends BaseMapper<CharacterPowerSystem> {
}
```

### Pattern 3: XML DTO Extension (for FC tags)
**What:** Add FactionConnectionDto inner class and List field to CharacterExtractDto
**When to use:** LLM returns `<FC>` tags alongside existing character data
**Example:**
```java
// Add to CharacterExtractDto:
@JacksonXmlElementWrapper(useWrapping = false)
@JacksonXmlProperty(localName = "FC")
private List<FactionConnectionDto> factionConnections;

@Data
public static class FactionConnectionDto {
    @JacksonXmlProperty(localName = "N")
    private String factionName;
    @JacksonXmlProperty(localName = "R")
    private String role;
}
```

### Pattern 4: Name Resolution (mirrors FactionService name matching)
**What:** AI outputs names, backend resolves to IDs via exact/fuzzy match
**When to use:** Power system name -> power_system.id, level name -> level.id/step.id
**Resolution order (per D-01):**
1. Exact match on `name` field
2. Fuzzy match (LIKE '%name%')
3. No match -> null (per D-03, text preserved in cultivation_level JSON)

### Pattern 5: Drawer Tab UI (mirrors FactionDrawer.vue)
**What:** Full-height side drawer with tab navigation for managing associations
**When to use:** Character detail view with multiple association types
**Example:** See FactionDrawer.vue -- Teleport to body, overlay, tab bar with icon+label, tab content switching with `v-if`

### Anti-Patterns to Avoid
- **Do NOT use Jackson XML for nested same-name tags:** The XmlParser uses Jackson XML which struggles with multiple `<V>` tags without `@JacksonXmlElementWrapper(useWrapping = false)`. The existing code already handles this correctly with the wrapper annotation. Same pattern must be applied for `<FC>`.
- **Do NOT create new service for simple CRUD:** Association table CRUD can live in the existing `NovelCharacterService` or a thin dedicated service. The FactionController pattern shows direct mapper usage from controller is acceptable for simple associations.
- **Do NOT lose text data when name resolution fails:** Per D-03, always preserve the original text in `cultivationLevel` JSON even when ID resolution fails.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| XML parsing of LLM response | Custom XML parser | Existing `XmlParser` + Jackson annotations | Already handles CDATA, markdown cleaning, unknown properties |
| Name-to-ID matching | Custom string matching | Reuse FactionServiceImpl pattern with MyBatis-Plus `LIKE` queries | Consistent matching logic across the codebase |
| Prompt template management | Hardcoded prompt strings | `PromptTemplateService.executeTemplate()` with v2 of template 15 | Supports versioning, variable injection, and hot-swapping |
| Association CRUD | Custom DAO | MyBatis-Plus `BaseMapper` + `LambdaQueryWrapper` | One-liner CRUD for simple join tables |
| Frontend drawer | Custom dialog | FactionDrawer pattern (Teleport + transition + tabs) | Consistent UX with existing faction management |

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | `novel_character_chapter.cultivation_level` JSON contains text cultivation data | Code edit only -- keep existing data, new ID fields in separate table |
| Stored data | `ai_prompt_template_version` row id=15 contains current template text | SQL update -- create v2 with FC tags + roleType definitions |
| Stored data | `novel_faction_character` table exists with `faction_id + character_id + role` | Code edit -- upsert logic in extract service |
| Live service config | None -- no n8n workflows or external services affected | None |
| OS-registered state | None | None |
| Secrets/env vars | None | None |
| Build artifacts | None | None |

**Nothing found in categories:** Live service config, OS-registered state, secrets/env vars, build artifacts -- all verified by codebase inspection. This is a code-only phase with one SQL migration.

## Common Pitfalls

### Pitfall 1: Jackson XML Multiple Same-Name Tags
**What goes wrong:** Jackson XML silently drops or overwrites duplicate tag names (e.g., multiple `<FC>` or `<V>`) without `@JacksonXmlElementWrapper(useWrapping = false)`
**Why it happens:** Jackson XML's default behavior wraps lists in a parent element. Without the wrapper annotation, it parses only the last tag.
**How to avoid:** Always use `@JacksonXmlElementWrapper(useWrapping = false)` + `@JacksonXmlProperty(localName = "FC")` for list fields. The existing `cultivationSystems` field already demonstrates this pattern.
**Warning signs:** Only one faction connection parsed when LLM outputs multiple `<FC>` tags.

### Pitfall 2: Name Resolution Ambiguity in Power Systems
**What goes wrong:** Level names like "初期" or "中期" appear in multiple levels across systems, causing wrong ID mapping.
**Why it happens:** Sub-realm names (steps) are often generic ("初期/中期/后期") and shared across multiple levels within the same system.
**How to avoid:** Always resolve in hierarchical order: (1) match systemName -> powerSystemId, (2) within that system, match currentLevel -> levelId (realm), (3) within that level, match stepName -> stepId. Never match level names globally.
**Warning signs:** Character assigned to wrong power system or wrong realm level.

### Pitfall 3: Upsert Race Condition on Concurrent Extraction
**What goes wrong:** Two concurrent chapter extractions for the same character create duplicate `novel_character_power_system` records.
**Why it happens:** `ChapterCharacterExtractService.extractCharactersAsync()` runs asynchronously. Two chapters processed simultaneously could both try to insert.
**How to avoid:** Use UNIQUE index on `(character_id, power_system_id)` and handle `DuplicateKeyException` with an update fallback. The CONTEXT.md explicitly requires this unique index.
**Warning signs:** Duplicate rows in `novel_character_power_system` table for same character+system.

### Pitfall 4: Prompt Template Bloat Breaking LLM Compliance
**What goes wrong:** Adding too many rules to the template causes LLM to ignore some or produce malformed XML.
**Why it happens:** Template is already substantial. Adding FC tags, roleType definitions, and existing character distribution increases cognitive load for the LLM.
**How to avoid:** Keep additions concise and well-structured. Use bullet points and examples. The existing template already uses clear section headers. Add new rules in the "注意事项" section with minimal redundancy.
**Warning signs:** LLM outputs missing `<FC>` tags or malformed roleType values after template update.

### Pitfall 5: Frontend Character List N+1 Query
**What goes wrong:** Loading power system and faction info for each character individually in the list view.
**Why it happens:** Character list API only returns basic NovelCharacter fields. Adding association display requires extra queries per character.
**How to avoid:** Per D-12, extend the character list/detail API to include aggregated association data via JOIN or batch query. Do this at the service layer, not frontend.
**Warning signs:** Character list page takes >2s to load with many characters.

## Code Examples

### Example 1: New CharacterPowerSystem Entity
```java
// Pattern source: NovelFactionCharacter.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_character_power_system")
public class CharacterPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long characterId;
    private Long powerSystemId;
    private Long currentRealmId;     // -> novel_power_system_level.id
    private Long currentSubRealmId;  // -> novel_power_system_level_step.id
}
```

### Example 2: SQL Migration for novel_character_power_system
```sql
-- Pattern source: novel_faction_character table definition (init.sql line 574-583)
DROP TABLE IF EXISTS `novel_character_power_system`;
CREATE TABLE `novel_character_power_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `character_id` bigint NOT NULL COMMENT 'Character ID',
  `power_system_id` bigint NOT NULL COMMENT 'Power system ID',
  `current_realm_id` bigint DEFAULT NULL COMMENT 'Current realm (novel_power_system_level.id)',
  `current_sub_realm_id` bigint DEFAULT NULL COMMENT 'Current sub-realm (novel_power_system_level_step.id)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_character_system` (`character_id`, `power_system_id`),
  KEY `idx_power_system_id` (`power_system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Character-PowerSystem association';
```

### Example 3: Name Resolution for Power System
```java
// In ChapterCharacterExtractService or a utility method:
// Resolution: systemName -> powerSystemId, then currentLevel -> levelId, then stepName -> stepId

// Step 1: Match system name (exact then LIKE)
NovelPowerSystem system = powerSystemMapper.selectOne(
    new LambdaQueryWrapper<NovelPowerSystem>()
        .eq(NovelPowerSystem::getProjectId, projectId)
        .eq(NovelPowerSystem::getName, systemName));
if (system == null) {
    // Fuzzy match
    system = powerSystemMapper.selectOne(
        new LambdaQueryWrapper<NovelPowerSystem>()
            .eq(NovelPowerSystem::getProjectId, projectId)
            .like(NovelPowerSystem::getName, systemName));
}
// Step 2: Match level name within system
NovelPowerSystemLevel level = levelMapper.selectOne(
    new LambdaQueryWrapper<NovelPowerSystemLevel>()
        .eq(NovelPowerSystemLevel::getPowerSystemId, system.getId())
        .eq(NovelPowerSystemLevel::getLevelName, currentLevel));
// Step 3: If level found, try matching step name
// ... similar pattern for steps
```

### Example 4: FC DTO Extension
```java
// Add to ChapterCharacterExtractXmlDto.CharacterExtractDto:

@JacksonXmlElementWrapper(useWrapping = false)
@JacksonXmlProperty(localName = "FC")
@Schema(description = "势力关联列表")
private List<FactionConnectionDto> factionConnections;

@Data
@Schema(description = "势力关联信息")
public static class FactionConnectionDto {
    @JacksonXmlProperty(localName = "N")
    private String factionName;

    @JacksonXmlProperty(localName = "R")
    private String role;
}
```

### Example 5: Prompt Template v2 Incremental Addition
```
## 角色类型定义（严格遵循）
- protagonist：主角，第一视角人物，故事核心人物。一个故事通常只有1-2个主角
- supporting：重要配角，多次出现、对剧情有推动作用
- antagonist：反派，与主角对抗、制造冲突的人物
- npc：过场人物，只在一两章出现、一两句台词的边缘角色

{existingRoleDistribution}

## 势力关联
如章节中出现角色所属势力/门派/组织，为每个角色添加FC标签：
<FC><N>势力名称</N><R>职位/角色</R></FC>
角色可属于多个势力。势力名称必须严格使用已有势力名称。
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Cultivation data as JSON in character_chapter only | Dedicated association table + JSON fallback | This phase | Structured queries, upsert on extraction |
| No faction info in extraction | FC tags in extraction XML | This phase | Auto-link characters to factions |
| roleType from LLM without guidance | Chinese definitions + existing distribution injection | This phase | Better roleType accuracy |

**No deprecated patterns being replaced** -- this phase is additive, building on existing infrastructure.

## Open Questions

1. **Name matching threshold for fuzzy match**
   - What we know: D-01 says "精确/模糊匹配". The FactionService pattern uses `LIKE` for fuzzy.
   - What's unclear: Whether to also try removing common prefixes/suffixes (e.g., "剑宗" matching "玄天剑宗")
   - Recommendation: Start with exact then `LIKE '%name%'`. If system has 3+ systems, add prefix/suffix heuristic later.

2. **Prompt template update mechanism**
   - What we know: `PromptTemplateService.updateTemplate()` creates a new version. Current active version is v1 (id=15).
   - What's unclear: Whether to create a new row in `ai_prompt_template_version` via SQL migration or through the API at runtime.
   - Recommendation: SQL migration (INSERT new version row with id=19 or next auto-increment, set `current_version_id` to it). This matches the project pattern where `init.sql` contains all template versions.

3. **Character detail API design**
   - What we know: D-12 requires server-side aggregation. Current `getCharacterDetail()` returns raw `NovelCharacter`.
   - What's unclear: Whether to create a new VO class or extend the existing `CharacterDto`.
   - Recommendation: Create a `CharacterDetailVO` that extends `NovelCharacter` fields with `List<PowerSystemAssociation>` and `List<FactionAssociation>` embedded objects. Keep list API lightweight, detail API enriched.

## Environment Availability

> This phase is primarily code/config changes. External dependencies are already verified from prior phases.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL 8.0+ | New table + data | Inferred available | 8.0+ | -- |
| Redis 6.0+ | Caching (existing) | Inferred available | 6.0+ | -- |
| Java 21 | Backend compilation | Available | 21 | -- |
| Maven 3.8+ | Backend build | Available | 3.8+ | -- |
| Node.js 18+ | Frontend build | Available | 18+ | -- |

**Missing dependencies with no fallback:** None identified.

**Missing dependencies with fallback:** None identified.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (existing) |
| Config file | None -- tests in `src/test/java/` |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=*Test` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| D-01/D-02 | Name resolution maps system/level names to IDs | unit | `mvn test -Dtest=NameResolutionTest` | Wave 0 needed |
| D-04/D-05 | FC tags parsed from XML and faction associations upserted | unit | `mvn test -Dtest=ChapterCharacterExtractXmlDtoTest` | Wave 0 needed |
| D-07/D-08 | Prompt template includes Chinese roleType definitions | manual-only | Verify template content in DB | N/A |
| D-10 | Drawer tabs render and switch correctly | manual-only | Visual inspection | N/A |
| D-11/D-12 | Character detail API returns aggregated data | unit | `mvn test -Dtest=NovelCharacterServiceTest` | Wave 0 needed |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn test -Dtest=*Test`
- **Per wave merge:** `cd ai-factory-backend && mvn test`
- **Phase gate:** Full backend test suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/aifactory/service/ChapterCharacterExtractServiceTest.java` -- covers D-01/D-02/D-04/D-05
- [ ] `src/test/java/com/aifactory/dto/ChapterCharacterExtractXmlDtoTest.java` -- covers FC tag XML parsing
- [ ] `src/test/java/com/aifactory/service/NovelCharacterServiceTest.java` -- covers D-12 aggregated detail

## Key Implementation Notes

### 1. Extraction Flow Extension Point
The main extension point is in `ChapterCharacterExtractService.extractCharacters()`, specifically inside the `for (CharacterExtractDto charDto : dto.getCharacters())` loop (line 165). After `matchOrCreateCharacter()` (line 173) and before `createCharacterChapterRelation()` (line 176), the new association logic should be inserted:

```
For each character extracted:
  1. matchOrCreateCharacter() -- existing
  2. NEW: resolveAndSavePowerSystemAssociations(characterId, charDto.getCultivationSystems(), projectId)
  3. NEW: resolveAndSaveFactionAssociations(characterId, charDto.getFactionConnections(), projectId)
  4. createCharacterChapterRelation() -- existing
```

### 2. Power System Name Resolution Hierarchy
The `PowerSystemServiceImpl.buildPowerSystemConstraint()` method already builds the full hierarchy text. For ID resolution, query the database directly:
- `NovelPowerSystemMapper` -> find system by name within projectId
- `NovelPowerSystemLevelMapper` -> find level by `levelName` within `powerSystemId`
- `NovelPowerSystemLevelStepMapper` -> find step by `levelName` within `powerSystemLevelId`

The `currentLevel` string from the DTO may contain either a level name (realm) or a step name (sub-realm). Resolution should try level match first, then step within that level.

### 3. Faction Name Resolution
Faction names from `<FC><N>` tags should be matched against `NovelFaction` using the same pattern as `FactionServiceImpl`. The `NovelFactionMapper` is already available via `@Autowired`.

### 4. Prompt Template v2 Content
The template update should be a SQL INSERT into `ai_prompt_template_version` with:
- `template_id` = 15
- `version_number` = 2
- Updated `template_content` with additions for FC tags, roleType definitions, existing distribution placeholder
- Update `ai_prompt_template.current_version_id` to the new version ID

### 5. Frontend Character Detail Extension
The current `Characters.vue` has no drawer mechanism. The approach should be:
1. Add a `CharacterDrawer` component (modeled on `FactionDrawer.vue`)
2. Click a character card -> open drawer with tabs: 基本信息, 修炼体系, 所属势力
3. The 基本信息 tab is the existing character editing form
4. The 修炼体系 tab is a new `CharacterPowerSystemTab.vue` (like `FactionCharacterTab.vue` but for power systems)
5. The 所属势力 tab is a new `CharacterFactionTab.vue` (similar to `FactionCharacterTab.vue` but from the character side)

### 6. Character List Card Enhancement (D-11)
The existing card grid in `Characters.vue` shows name + role. Add a line below showing cultivation realm + faction info:
```
玄武境 . 青云门长老
```
This requires the list API to return aggregated data (or the frontend makes a batch request).

## Sources

### Primary (HIGH confidence)
- Codebase inspection: `ChapterCharacterExtractService.java` -- extraction pipeline, line 165 loop is extension point
- Codebase inspection: `ChapterCharacterExtractXmlDto.java` -- current DTO structure with `@JacksonXmlElementWrapper` pattern
- Codebase inspection: `NovelFactionCharacter.java` + `NovelFactionCharacterMapper.java` -- join table pattern
- Codebase inspection: `FactionDrawer.vue` + `FactionCharacterTab.vue` -- Drawer tab UI pattern
- Codebase inspection: `FactionServiceImpl.java` -- name matching and CRUD patterns
- Codebase inspection: `PowerSystemServiceImpl.java` -- power system hierarchy and constraint building
- Codebase inspection: `sql/init.sql` -- table schemas for `novel_faction_character`, `novel_power_system_level`, `novel_power_system_level_step`
- Codebase inspection: `ai_prompt_template_version` row id=15 -- current prompt template content

### Secondary (MEDIUM confidence)
- Project history: v1.0.2 faction name matching was validated and works in production (STATE.md confirms)
- Project history: `buildPowerSystemConstraint()` verified to produce correct system/level names for LLM consumption

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all components exist in codebase, verified by reading source files
- Architecture: HIGH -- existing patterns (FactionDrawer, FactionCharacterTab, join tables) provide exact templates
- Pitfalls: HIGH -- based on known Jackson XML behavior and project history
- Implementation details: HIGH -- all integration points identified by reading source code

**Research date:** 2026-04-06
**Valid until:** 2026-05-06 (stable codebase, no external API changes)
