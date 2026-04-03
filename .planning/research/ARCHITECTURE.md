# Architecture Patterns

**Domain:** Faction system structured refactoring for AI novel generation app
**Researched:** 2026-04-01
**Confidence:** HIGH (based on direct codebase analysis of existing patterns)

## Recommended Architecture

The faction system follows the exact same architecture pattern proven by the geography (ContinentRegion) and power system refactorings. This is the third iteration of the same structural pattern, so the architecture is well-established and low-risk.

```
                    WorldSetting.vue
                         |
            +------------+------------+
            |            |            |
     PowerSystemSection  GeographyTree  FactionTree  <-- NEW
            |            |            |
     /api/novel/{pid}/   /api/novel/{pid}/  /api/novel/{pid}/
     power-system/*      continent-region/*  faction/*         <-- NEW
            |            |            |
     PowerSystemCtrl   ContinentRegionCtrl  FactionController  <-- NEW
            |            |            |
     PowerSystemService  ContinentRegionSvc  FactionService     <-- NEW
            |            |            |
     NovelPowerSystem    NovelContinentRegion NovelFaction       <-- NEW
     +Level+Step         Mapper              Mapper             <-- NEW
     Mappers
```

### Component Boundaries

| Component | Responsibility | Communicates With | File Location |
|-----------|---------------|-------------------|---------------|
| **NovelFaction** | Entity: tree-structured faction node with type, corePowerSystem, description | FactionMapper, FactionService | `entity/NovelFaction.java` |
| **NovelFactionRegion** | Entity: faction-region many-to-many link | FactionMapper | `entity/NovelFactionRegion.java` |
| **NovelFactionCharacter** | Entity: faction-character link with role field | FactionMapper | `entity/NovelFactionCharacter.java` |
| **NovelFactionRelation** | Entity: faction-faction relation with type + description | FactionMapper | `entity/NovelFactionRelation.java` |
| **NovelFactionMapper** | MyBatis-Plus mapper for all faction tables | FactionService | `mapper/NovelFactionMapper.java` (+ 3 association mappers) |
| **FactionService** | Interface: tree CRUD, fillForces, buildFactionsText, association management | FactionController, WorldviewTaskStrategy, PromptContextBuilder | `service/FactionService.java` |
| **FactionServiceImpl** | Implementation: mirrors ContinentRegionServiceImpl pattern | Mappers, PowerSystemService (for name lookup) | `service/impl/FactionServiceImpl.java` |
| **FactionController** | REST endpoints: tree CRUD + association endpoints | FactionService | `controller/FactionController.java` |
| **WorldviewTaskStrategy** | Modified: add DOM parsing for faction XML + save via FactionService | FactionService, ContinentRegionService, PowerSystemService | `service/task/impl/WorldviewTaskStrategy.java` (MODIFY) |
| **PromptContextBuilder** | Modified: add fillForces() call in getWorldview() | FactionService | `service/prompt/PromptContextBuilder.java` (MODIFY) |
| **ChapterPromptBuilder** | Modified: replace worldview.getForces() with factionService call | FactionService | `service/chapter/prompt/ChapterPromptBuilder.java` (MODIFY) |
| **FactionTree.vue** | Frontend: tree view/edit component mirroring GeographyTree.vue | `/api/novel/{pid}/faction/*` | `views/Project/Detail/components/FactionTree.vue` (NEW) |
| **WorldSetting.vue** | Modified: replace forces textarea with FactionTree component | FactionTree component | `views/Project/Detail/WorldSetting.vue` (MODIFY) |
| **faction.ts** | Frontend API client mirroring continentRegion.ts | Backend API | `api/faction.ts` (NEW) |

### Data Flow

#### Flow 1: AI Worldview Generation (Write Path)

This is the primary write path. AI generates worldview, backend parses and persists structured data.

```
User clicks "AI Generate"
        |
        v
WorldSetting.vue -> generateWorldviewAsync()
        |
        v
WorldviewTaskStrategy.executeStep()
        |
        +-> check_existing:
        |       Delete old worldview + cascading delete of
        |       power systems, geography regions, factions (NEW)
        |
        +-> generate_worldview:
        |       Build prompt from DB template (id=3)
        |       Prompt NOW includes structured faction XML format
        |       Call LLM provider -> get AI response
        |
        +-> save_worldview:
                |
                +-> Parse <b>, <t>, <l>, <r> -> NovelWorldview table
                |   (forces column becomes transient/ignored)
                |
                +-> saveGeographyRegionsFromXml() [EXISTING]
                |       DOM parse <g> tag -> ContinentRegionService.saveTree()
                |
                +-> savePowerSystems() [EXISTING]
                |       Jackson parse <p> tag -> PowerSystemService
                |
                +-> saveFactionsFromXml() [NEW]
                        DOM parse <f> tag -> FactionService.saveTree()
                        |
                        +-> Parse <faction> nodes recursively
                        +-> Resolve corePowerSystem by name lookup
                        +-> Resolve region associations by name lookup
                        +-> Insert novel_faction rows (tree structure)
                        +-> Insert novel_faction_region rows
                        +-> Insert novel_faction_relation rows
```

**Critical ordering constraint:** Geography and power systems MUST be saved BEFORE factions because faction save needs to resolve `corePowerSystem` names to IDs and `region` names to IDs via database lookups.

#### Flow 2: Prompt Building (Read Path)

When generating chapters, the system reads structured faction data and converts it to text for AI prompts.

```
ChapterPromptBuilder.buildPrompt()
        |
        v
PromptContextBuilder.getWorldview(projectId)
        |
        +-> worldviewMapper.selectOne(projectId)
        +-> continentRegionService.fillGeography(worldview)  [EXISTING]
        +-> factionService.fillForces(worldview)              [NEW]
        |
        v
NovelWorldview entity now has:
  - geography: String (transient, built from tree)
  - forces: String (transient, built from tree)  [NEW - replaces DB column]
  - worldBackground: String (from DB)
  - timeline: String (from DB)
  - rules: String (from DB)
```

The `fillForces()` method mirrors `fillGeography()`:
1. Load faction tree from `novel_faction` via `getTreeByProjectId()`
2. Load faction-region associations and resolve region names
3. Load faction-faction relations and resolve faction names
4. Build formatted text string like:
   ```
   - 天剑宗（正派，修仙体系）
     总部：中土神州太清山
     盟友：玉虚殿
     描述：正道第一大宗...
     - 外门：负责弟子招收...
     - 内门：核心修炼力量...
   ```
5. Set on worldview.setForces(text)

#### Flow 3: User Manual CRUD (Read/Write Path)

```
FactionTree.vue
    |
    +-> getFactionTree(projectId) -> GET /api/novel/{pid}/faction/tree
    +-> addFaction(projectId, data) -> POST /api/novel/{pid}/faction/save
    +-> updateFaction(projectId, data) -> PUT /api/novel/{pid}/faction/update
    +-> deleteFaction(projectId, id) -> DELETE /api/novel/{pid}/faction/{id}
    +-> manageCharacterAssociations (separate panel)
    +-> manageFactionRelations (separate panel)
```

#### Flow 4: Character Association (Manual Only)

Per PROJECT.md, faction-character association is manual only. AI does not create these links during worldview generation because characters may not exist yet.

```
User opens FactionDetail panel
    |
    +-> Select characters to associate
    +-> Set role (掌门/长老/弟子/etc.)
    +-> POST /api/novel/{pid}/faction/{factionId}/character
    |
    v
FactionService.associateCharacter(factionId, characterId, role)
    -> Insert into novel_faction_character
```

## Database Schema

### Table: novel_faction (tree structure, mirrors novel_continent_region)

```sql
CREATE TABLE `novel_faction` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `parent_id` bigint NULL DEFAULT NULL COMMENT 'Parent faction ID (NULL=root)',
    `deep` int NOT NULL DEFAULT 0 COMMENT 'Tree depth (0=root)',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT 'Sort within siblings',
    `project_id` bigint NOT NULL COMMENT 'Project ID',
    `name` varchar(200) NOT NULL COMMENT 'Faction name',
    `type` varchar(20) NULL DEFAULT NULL COMMENT 'Alignment: righteous/evil/neutral (root only)',
    `core_power_system_id` bigint NULL DEFAULT NULL COMMENT 'Linked power system (root only)',
    `description` text NULL COMMENT 'Faction description',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_parent_id` (`parent_id`)
) COMMENT 'Faction table (tree structure)';
```

### Table: novel_faction_region (many-to-many)

```sql
CREATE TABLE `novel_faction_region` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `faction_id` bigint NOT NULL COMMENT 'Faction ID',
    `region_id` bigint NOT NULL COMMENT 'Region ID (novel_continent_region)',
    `description` varchar(500) NULL COMMENT 'Association context',
    PRIMARY KEY (`id`),
    INDEX `idx_faction_id` (`faction_id`),
    INDEX `idx_region_id` (`region_id`)
) COMMENT 'Faction-Region association';
```

### Table: novel_faction_character (many-to-many with role)

```sql
CREATE TABLE `novel_faction_character` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `faction_id` bigint NOT NULL COMMENT 'Faction ID',
    `character_id` bigint NOT NULL COMMENT 'Character ID (novel_character)',
    `role` varchar(100) NULL COMMENT 'Role title (掌门/长老/弟子 etc.)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_faction_id` (`faction_id`),
    INDEX `idx_character_id` (`character_id`)
) COMMENT 'Faction-Character association';
```

### Table: novel_faction_relation (faction-faction relations)

```sql
CREATE TABLE `novel_faction_relation` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `faction_id` bigint NOT NULL COMMENT 'Source faction ID',
    `target_faction_id` bigint NOT NULL COMMENT 'Target faction ID',
    `relation_type` varchar(20) NOT NULL COMMENT 'ally/enemy/neutral',
    `description` varchar(500) NULL COMMENT 'Relation description',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_faction_id` (`faction_id`)
) COMMENT 'Faction-Faction relations';
```

## Patterns to Follow

### Pattern 1: Tree Table (parent_id + deep + sort_order)

**What:** Store hierarchical data using adjacency list with materialized depth.
**When:** Any multi-level nested domain data (geography, factions).
**Source:** Directly mirrors `NovelContinentRegion` entity.

```java
@Data
@TableName("novel_faction")
public class NovelFaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private Integer deep;
    private Integer sortOrder;
    private Long projectId;
    private String name;
    private String type;              // NEW: not in geography
    private Long corePowerSystemId;   // NEW: not in geography
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<NovelFaction> children;
}
```

**Key difference from geography:** Faction has `type` and `corePowerSystemId` fields that only apply to root nodes. Child factions inherit these from their root ancestor. The service layer must enforce this.

### Pattern 2: Transient Field + Fill Method

**What:** Replace DB column with transient field dynamically populated from structured tables.
**When:** Converting text fields to structured data while maintaining backward-compatible read interfaces.
**Source:** Mirrors `ContinentRegionService.fillGeography()` exactly.

```java
// In NovelWorldview.java - change forces to transient
@TableField(exist = false)
private String forces;

// In FactionService.java - new method
void fillForces(NovelWorldview worldview);

// In PromptContextBuilder.getWorldview() - add fill call
NovelWorldview worldview = worldviewMapper.selectOne(queryWrapper);
if (worldview != null) {
    continentRegionService.fillGeography(worldview);   // existing
    factionService.fillForces(worldview);               // new
}
```

### Pattern 3: DOM Parsing for Nested Same-Name Tags

**What:** Use W3C DOM parser (not Jackson XML) for XML with recursive same-name tags.
**When:** Jackson XML cannot handle nested tags with the same name (e.g., `<faction>` containing `<faction>`).
**Source:** Mirrors `WorldviewTaskStrategy.saveGeographyRegionsFromXml()` exactly.

```java
private void saveFactionsFromXml(Long projectId, String aiResponse) {
    int start = aiResponse.indexOf("<factions>");
    int end = aiResponse.indexOf("</factions>");
    if (start < 0 || end < 0) return;

    String factionXml = "<root>" + aiResponse.substring(start, end + 11) + "</root>";
    Document doc = builder.parse(new InputSource(new StringReader(factionXml)));

    Element factionsElement = (Element) doc.getElementsByTagName("factions").item(0);
    List<NovelFaction> rootFactions = parseFactionNodes(factionsElement, projectId);
    factionService.saveTree(projectId, rootFactions);
}
```

### Pattern 4: Name-to-ID Resolution for Cross-Entity References

**What:** AI outputs entity names (not IDs). Backend resolves names to IDs by querying existing records.
**When:** AI-generated data references entities saved in earlier steps of the same generation flow.
**Source:** Explicitly stated in PROJECT.md Key Decisions.

```java
// Resolve power system by name
NovelPowerSystem ps = powerSystemMapper.selectOne(
    new LambdaQueryWrapper<NovelPowerSystem>()
        .eq(NovelPowerSystem::getProjectId, projectId)
        .eq(NovelPowerSystem::getName, factionCorePowerSystemName)
);

// Resolve region by name
NovelContinentRegion region = regionMapper.selectOne(
    new LambdaQueryWrapper<NovelContinentRegion>()
        .eq(NovelContinentRegion::getProjectId, projectId)
        .eq(NovelContinentRegion::getName, regionName)
);
```

**Critical ordering:** This is why geography and power systems must be saved BEFORE factions in `parseAndSaveWorldview()`.

### Pattern 5: Frontend Tree Component

**What:** Vue 3 component with manual nesting levels, CRUD inline, same visual style.
**When:** Displaying tree-structured data with editing capability.
**Source:** Directly mirrors `GeographyTree.vue`.

The GeographyTree.vue uses explicit nesting (Level 0 -> Level 1 -> Level 2 -> Level 3+) rather than a recursive component. FactionTree.vue should follow the same approach for consistency, though it adds extra UI elements:
- Type badge (正派/反派/中立) for root nodes
- Power system label for root nodes
- Relation management link
- Character association link

## AI Prompt XML Format

The existing prompt template (id=3) uses `<f><![CDATA[plain text]]></f>` for forces. This changes to structured XML.

### Current Format (to be replaced):
```xml
<w>
  ...
  <f><![CDATA[plain text about factions]]></f>
  ...
</w>
```

### New Format:
```xml
<w>
  ...
  <factions>
    <faction>
      <name>天剑宗</name>
      <type>righteous</type>
      <powerSystem>修仙</powerSystem>
      <regions>中土神州,太清山</regions>
      <desc><![CDATA[正道第一大宗，以剑道闻名...]]></desc>
      <allies>玉虚殿</allies>
      <enemies>血魔教</enemies>
      <faction>
        <name>外门</name>
        <desc><![CDATA[负责弟子招收与基础修炼]]></desc>
      </faction>
      <faction>
        <name>内门</name>
        <desc><![CDATA[核心弟子修炼之所]]></desc>
        <faction>
          <name>剑阁</name>
          <desc><![CDATA[专修剑道的精英堂口]]></desc>
        </faction>
      </faction>
    </faction>
    <faction>
      <name>血魔教</name>
      <type>evil</type>
      <powerSystem>修仙</powerSystem>
      <regions>南荒炎洲</regions>
      <desc><![CDATA[魔道势力...]]></desc>
    </faction>
  </factions>
  ...
</w>
```

**Key design decisions for the XML format:**
- Root tag `<factions>` (not `<f>`) to avoid collision with the old format and make parsing unambiguous
- `<faction>` supports recursive nesting for sub-factions
- `<type>`, `<powerSystem>`, `<regions>` only on root-level factions
- `<allies>` and `<enemies>` as comma-separated faction name lists for simplicity
- Region names comma-separated, resolved to IDs on backend
- Uses `<name>` + `<desc>` sub-tags consistent with geography `<n>` + `<d>` convention

## Anti-Patterns to Avoid

### Anti-Pattern 1: Storing type/corePowerSystem on Every Node

**What:** Setting type and corePowerSystem on every child faction row.
**Why bad:** Data redundancy, inconsistency risk when root changes.
**Instead:** Only store on root nodes (deep=0). The `fillForces()` and `buildFactionsText()` methods resolve these by walking up to root. Service layer enforces: if `parentId != null`, set type and corePowerSystemId to null on insert/update.

### Anti-Pattern 2: Creating Faction-Character Links During AI Generation

**What:** Trying to parse character names from AI faction output and auto-link them.
**Why bad:** Characters do not exist during worldview generation (created later in outline/volume stages). AI would output fictional character names that have no DB records.
**Instead:** Manual-only association via UI after characters are created. PROJECT.md explicitly scopes this out.

### Anti-Pattern 3: Using Jackson XML for Faction Parsing

**What:** Adding faction fields to WorldSettingXmlDto and using XmlParser.
**Why bad:** Nested same-name `<faction>` tags inside `<faction>` tags -- the exact same problem documented in WorldSettingXmlDto line 23 for geography's `<r>` tags. Jackson XML flattens or overwrites same-name nested elements.
**Instead:** Manual DOM parsing, identical to `saveGeographyRegionsFromXml()`.

### Anti-Pattern 4: Dropping forces Column Immediately

**What:** Running `ALTER TABLE novel_worldview DROP COLUMN forces` in the same migration as creating new tables.
**Why bad:** Breaks existing chapters that reference worldview.getForces(). The prompt building code needs the `fillForces()` method in place first.
**Instead:** Make `forces` a transient field first (`@TableField(exist = false)`), deploy the new service layer, verify prompt building works, THEN run the ALTER TABLE in a separate migration.

## Scalability Considerations

| Concern | At 10 factions | At 100 factions | At 1000 factions |
|---------|---------------|-----------------|------------------|
| Tree query | Single SELECT by projectId, in-memory buildTree() | Same -- works fine for hundreds of nodes | Consider recursive CTE or closure table |
| fillForces() text | Negligible | Acceptable | Cache the built text, invalidate on faction CRUD |
| Name-to-ID lookup | Simple query per faction | Batch query: load all names into a Map first | Same -- Map-based lookup scales well |
| Frontend tree render | Direct DOM nesting | Direct DOM nesting | Consider virtual scroll or lazy-load children |

For this application (novel world-building, typically 5-50 factions per project), the adjacency list + in-memory buildTree() pattern is perfectly adequate.

## Integration Touchpoints Summary

### Files to CREATE (new):

| File | Layer | Description |
|------|-------|-------------|
| `entity/NovelFaction.java` | Domain | Faction tree entity |
| `entity/NovelFactionRegion.java` | Domain | Faction-region link entity |
| `entity/NovelFactionCharacter.java` | Domain | Faction-character link entity |
| `entity/NovelFactionRelation.java` | Domain | Faction-faction relation entity |
| `mapper/NovelFactionMapper.java` | Data | MyBatis-Plus mapper |
| `mapper/NovelFactionRegionMapper.java` | Data | Association mapper |
| `mapper/NovelFactionCharacterMapper.java` | Data | Association mapper |
| `mapper/NovelFactionRelationMapper.java` | Data | Relation mapper |
| `service/FactionService.java` | Service | Service interface |
| `service/impl/FactionServiceImpl.java` | Service | Implementation |
| `controller/FactionController.java` | Controller | REST endpoints |
| `api/faction.ts` | Frontend API | API client |
| `views/Project/Detail/components/FactionTree.vue` | Frontend UI | Tree component |
| `views/Project/Detail/components/FactionRelations.vue` | Frontend UI | Relation management |
| `views/Project/Detail/components/FactionCharacterPanel.vue` | Frontend UI | Character association |
| `sql/faction_migration.sql` | Migration | DDL + prompt template update |

### Files to MODIFY (existing):

| File | Change | Risk |
|------|--------|------|
| `entity/NovelWorldview.java` | Change `forces` from DB column to `@TableField(exist = false)` | LOW |
| `service/task/impl/WorldviewTaskStrategy.java` | Add `saveFactionsFromXml()` + DOM parsing + delete in check_existing | MEDIUM |
| `service/prompt/PromptContextBuilder.java` | Add `factionService.fillForces(worldview)` call | LOW |
| `service/chapter/prompt/ChapterPromptBuilder.java` | Replace `worldview.getForces()` with factionService-based text | LOW |
| `views/Project/Detail/WorldSetting.vue` | Replace forces textarea with FactionTree component | LOW |

## Build Order (Dependency Chain)

Based on data flow dependencies, the recommended build order is:

```
Phase 1: Database + Domain (no dependencies)
  1.1  SQL migration: create 4 tables
  1.2  Entity classes: NovelFaction, NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation
  1.3  Mapper interfaces (4 mappers, all extend BaseMapper<>)
  1.4  NovelWorldview.java: change forces to @TableField(exist = false)

Phase 2: Backend Service (depends on Phase 1)
  2.1  FactionService interface (mirrors ContinentRegionService)
  2.2  FactionServiceImpl (mirrors ContinentRegionServiceImpl + association methods)
  2.3  FactionController (mirrors ContinentRegionController + association endpoints)

Phase 3: AI Integration (depends on Phase 2)
  3.1  Update prompt template (id=3): change <f> to <factions> structured XML
  3.2  WorldviewTaskStrategy: add saveFactionsFromXml() with DOM parsing
  3.3  WorldviewTaskStrategy: add faction deletion in checkExisting()
  3.4  PromptContextBuilder: add fillForces() call
  3.5  ChapterPromptBuilder: replace direct forces field access

Phase 4: Frontend (depends on Phase 2)
  4.1  api/faction.ts (API client)
  4.2  FactionTree.vue (tree component, mirrors GeographyTree.vue)
  4.3  FactionRelations.vue (relation management)
  4.4  FactionCharacterPanel.vue (character association)
  4.5  WorldSetting.vue: replace textarea with FactionTree component

Phase 5: Cleanup (depends on all above deployed and verified)
  5.1  ALTER TABLE novel_worldview DROP COLUMN forces
  5.2  Remove any remaining direct forces column references
```

**Why this order:**
- Phase 1 first because everything else depends on the database tables and entity definitions
- Phase 2 before Phase 3 because the AI integration calls FactionService methods
- Phase 2 before Phase 4 because the frontend calls the backend API
- Phase 3 and Phase 4 can run in parallel once Phase 2 is complete
- Phase 5 last because dropping the column is irreversible and must only happen after all code paths use the new transient field approach

## Sources

- Direct codebase analysis of:
  - `NovelContinentRegion.java` (entity pattern)
  - `ContinentRegionServiceImpl.java` (tree service pattern with saveTree, buildTree, fillGeography)
  - `ContinentRegionController.java` (REST endpoint pattern)
  - `GeographyTree.vue` (frontend tree component pattern)
  - `WorldviewTaskStrategy.java` (AI generation + DOM parsing pattern)
  - `WorldSettingXmlDto.java` (XML parsing limitations documented)
  - `PromptContextBuilder.java` (prompt building with fillGeography)
  - `ChapterPromptBuilder.java` (chapter prompt consuming worldview data)
  - `PowerSystemServiceImpl.java` (buildPowerSystemConstraint for prompt context)
  - `NovelWorldview.java` (current forces field, transient geography field)
  - `geography_migration.sql` and `power_system_migration.sql` (migration pattern)
