# Stack Research

**Domain:** Tree-structured faction data with many-to-many relationships in Spring Boot + Vue.js
**Researched:** 2026-04-01
**Confidence:** HIGH

## Recommended Stack

This is a follow-on research for the faction/force structured refactoring milestone. The core stack is already locked by the existing codebase. The recommendations below focus on patterns and features within the existing stack that are relevant to this specific milestone.

### Core Technologies (Existing, No Changes)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Spring Boot | 3.2.0 | Backend framework | Already in use. Provides DI, transaction management, REST APIs. No version change needed. |
| MyBatis-Plus | 3.5.5 | ORM framework | Already in use. Provides `LambdaQueryWrapper`, `BaseMapper`, auto-increment IDs. This version supports all tree patterns needed. |
| Vue 3 | 3.5.24 | Frontend framework | Already in use. Composition API with `<script setup>` is the established pattern in this codebase. |
| Vite | 7.2.4 | Build tool | Already in use. No changes needed. |
| Tailwind CSS | 4.1.18 | Styling | Already in use. GeographyTree.vue demonstrates the visual pattern to follow. |
| MySQL | 8.0+ | Database | Already in use. Supports all needed features: AUTO_INCREMENT, foreign keys, indexes. |
| Jackson XML | (managed by Spring Boot) | XML parsing for simple structures | Already in use via XmlParser component for top-level world-setting parsing. |

### Tree Structure Pattern (Validated, Reuse Geography Pattern)

| Pattern | Implementation | Why |
|---------|---------------|-----|
| `parent_id + deep` adjacency list | `NovelContinentRegion` entity pattern | Already validated in this codebase. The `ContinentRegionServiceImpl` demonstrates the complete lifecycle: auto-calculate `deep` on add, cascade update `deep` on parent change, recursive delete, tree build from flat list. **Copy this pattern directly for `novel_faction`.** |
| Java-side tree building | `buildTree()` in `ContinentRegionServiceImpl` | Loads all nodes in one query, groups by `parentId` using `Collectors.groupingBy`, sets children, filters roots. Database-agnostic and handles unlimited depth. **Reuse verbatim.** |
| `@TableField(exist = false)` for children | `NovelContinentRegion.children` | MyBatis-Plus annotation to exclude `List<NovelFaction> children` from DB mapping. **Same pattern.** |
| Recursive save | `saveNodeRecursive()` in service | Inserts parent first, then iterates children setting their `parentId` to the parent's generated ID. Auto-increment ID is available immediately after `insert()`. **Same pattern.** |

**Confidence:** HIGH -- This is not theoretical. The pattern is already running in production for geography.

### Many-to-Many Relationship Tables

| Table | Pattern | Why |
|-------|---------|-----|
| `novel_faction_region` | Simple join table with `faction_id`, `region_id` | Standard many-to-many. No extra fields needed -- a faction controls regions, that is the entire relationship. Follow `novel_worldview_power_system` pattern (auto-increment ID + two foreign keys). |
| `novel_faction_character` | Join table with `faction_id`, `character_id`, `role` | Extended many-to-many. The `role` field (e.g., "掌门", "长老", "弟子") makes this more than a simple junction. Use VARCHAR for role since the values are free-form Chinese text, not an enum. |
| `novel_faction_relation` | Self-referencing pair table with `faction_id`, `related_faction_id`, `relation_type`, `description` | Faction-to-faction relationship. `relation_type` should be VARCHAR with values like "盟友", "敌对", "中立". Include `description` for context. Both faction IDs reference `novel_faction.id`. |

**Entity pattern for each:** Follow `NovelWorldviewPowerSystem` style -- `@Data`, `@TableName`, `@TableId(type = IdType.AUTO)`, Long IDs.

**Confidence:** HIGH -- Standard relational patterns, already proven in this codebase.

### DOM XML Parsing for Faction Data

| Technology | Purpose | Why |
|------------|---------|-----|
| `javax.xml.parsers.DocumentBuilder` | Parse AI response XML containing nested `<f>` faction tags | Already proven in `WorldviewTaskStrategy.saveGeographyRegionsFromXml()`. Jackson XML cannot handle nested same-name tags (e.g., `<f>` inside `<f>`), so DOM is the correct choice. **This is a settled decision in this codebase.** |
| `org.w3c.dom.Element.getChildNodes()` | Iterate direct children only | Critical: use `getChildNodes()` on each Element, NOT `getElementsByTagName()`. The latter searches the entire subtree, which would incorrectly pick up deeply nested `<f>` tags. The existing code already does this correctly in `parseSingleRegion()`. |
| `Node.ELEMENT_NODE` type check | Filter text/whitespace nodes | Already used in existing code: `if (node.getNodeType() == Node.ELEMENT_NODE && "r".equals(node.getNodeName()))`. Same pattern for `"f"` tags. |

**Recommended faction XML structure (for AI prompt):**

```xml
<f>
  <n>势力名称</n>
  <t>正派</t>                    <!-- type: 仅顶级 -->
  <ps>力量体系名称</ps>           <!-- core_power_system: 仅顶级，按名称引用 -->
  <d><![CDATA[描述]]></d>
  <f>                            <!-- 子势力，递归嵌套 -->
    <n>分堂名称</n>
    <d><![CDATA[描述]]></d>
    <f>...</f>
  </f>
  <regions>                      <!-- 关联区域，按名称引用 -->
    <r>东玄大陆</r>
    <r>北荒</r>
  </regions>
  <relations>                    <!-- 势力关系，按名称引用 -->
    <rel><n>敌对势力名</n><t>敌对</t><d>百年世仇</d></rel>
  </relations>
</f>
```

**Why this XML structure:**
- `<f>` for faction, `<n>` for name, `<d>` for description -- consistent with existing `<r>` pattern for regions
- `<t>` and `<ps>` only on top-level factions (inherited by children in application logic)
- `<regions>` and `<relations>` grouped in their own sub-elements to avoid ambiguity with recursive `<f>` children
- Names instead of IDs -- AI does not know database IDs; backend resolves names to IDs after parsing

**Confidence:** HIGH -- Extends the proven geography XML parsing pattern.

### Vue 3 Frontend Tree Component

| Pattern | Implementation | Why |
|---------|---------------|-----|
| Hand-rolled recursive template | Follow `GeographyTree.vue` pattern | The existing GeographyTree uses explicit nesting (Level 0, 1, 2, 3+) rather than a true recursive component. This is simpler to understand and debug for the limited depth of faction trees (typically 2-3 levels). **Follow the same approach for FactionTree.vue.** |
| No external tree library | -- | The existing codebase has zero external tree dependencies. Adding one (e.g., vue3-treeview) would introduce a new dependency for marginal benefit. The hand-rolled approach works well for the typical 3-4 level depth of novel world factions. |
| `expandedNodes` as `Set<number>` | `GeographyTree.vue` line 23 | Tracks which nodes are expanded. Simple and performant. **Reuse.** |
| Inline editing with `editingNode` ref | `GeographyTree.vue` pattern | Click edit -> populate local refs -> save -> reload. No complex form state management needed. **Reuse.** |
| Tailwind utility classes for styling | Existing patterns | Dark mode support, hover effects, icon colors by depth level. **Follow GeographyTree styling.** |
| Lucide icons by depth | `getLevelIcon()` / `getLevelColor()` | Replace geography icons (Globe2, Mountain, Trees, MapPin) with faction-appropriate icons. Suggested: `Sword` (top-level), `Shield` (second-level), `Users` (third-level), `User` (leaf). Use same `getLevelColor()` approach. |

**FactionTree additional UI elements (beyond GeographyTree):**

| Element | Implementation | Why |
|---------|---------------|-----|
| Faction type badge | Small colored pill next to name (`正派` green, `反派` red, `中立` gray) | Users need to see faction alignment at a glance. Not present in GeographyTree. |
| Power system tag | Small text tag showing associated power system name (top-level only) | Critical for understanding which cultivation system a faction uses. Loaded from the faction's `corePowerSystem` name. |
| Region associations | Collapsible list of region names under each top-level faction | Shows territory control. Clicking could navigate to geography section. |
| Relation indicators | Icon or badge showing number of allied/hostile factions | Quick overview of faction diplomacy. Detailed view on a separate relations panel. |
| Character roster (separate tab/panel) | Table listing characters and their roles in each faction | Separate from the tree because it involves many-to-many character data. Not inline. |

**Confidence:** HIGH -- Extends proven GeographyTree pattern with incremental additions.

### MyBatis-Plus Features to Use

| Feature | Where | Why |
|---------|-------|-----|
| `LambdaQueryWrapper` | All service queries | Already the standard in this codebase. Type-safe column references. Use `.eq()`, `.isNull()`, `.orderByAsc()` as in ContinentRegionServiceImpl. |
| `BaseMapper.insert()` | Single node inserts | Returns auto-generated ID immediately via `@TableId(type = IdType.AUTO)`. Essential for the recursive save pattern (insert parent, get ID, set as children's parentId). |
| `BaseMapper.deleteBatchIds()` | Cascade delete | Takes a `List<Long>` of IDs. Used in `ContinentRegionServiceImpl.deleteRegion()`. Same approach for factions -- collect all descendant IDs, add the target, delete all at once. |
| `BaseMapper.selectList()` | Flat list queries | Load all faction nodes for a project, then build tree in Java. Same as geography. |
| `BaseMapper.selectById()` | Parent lookups | Used when calculating `deep` for new/updated nodes. |
| `@Transactional` | Service methods | Already used on all mutating methods in ContinentRegionServiceImpl. **Must use** on `saveTree()`, `deleteRegion()`, and all cascade operations. |
| `@TableField(exist = false)` | Entity children field | Marks `List<NovelFaction> children` as non-database. Already proven pattern. |

### Name-to-ID Resolution (Post-Parse)

| Concern | Approach | Why |
|---------|----------|-----|
| Power system name -> ID | After parsing faction XML, query `novel_power_system` by `name` and `projectId` | Power systems are saved before factions in the worldview generation flow. Names are unique per project (enforced by AI prompt). |
| Region name -> ID | Query `novel_continent_region` by `name` and `projectId` | Geography is saved before factions. A region may appear under multiple factions, so use `LIKE` or exact match. |
| Faction name -> ID (for relations) | Parse all factions first, build a `Map<String, Long>` of name-to-ID | Faction relations reference other factions by name. Build the map after all factions are inserted, then iterate relations and resolve names. This requires a two-pass approach. |

**Two-pass insert strategy for factions with relations:**

```
Pass 1: Parse and insert all factions (tree structure). Build name-to-ID map.
Pass 2: Parse and insert faction-region associations and faction-faction relations,
         using the name-to-ID map for resolution.
```

This is necessary because faction relations reference other factions that may not exist yet during Pass 1.

**Confidence:** HIGH -- Logical extension of existing patterns.

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Tree storage | Adjacency list (`parent_id + deep`) | Nested sets / materialized path / closure table | Adjacency list is already the established pattern in this codebase. Nested sets add complexity for marginal query benefit. The tree is small (typically < 50 nodes per project), so the Java-side `buildTree()` is fast enough. |
| Tree query | Load all + Java-side build | `WITH RECURSIVE` CTE in MySQL | CTE is more complex, harder to debug, and the project uses Java-side tree building successfully already. No reason to change for a similarly-sized dataset. |
| XML parsing | `org.w3c.dom` DOM | Jackson XML (`XmlParser`) | Jackson XML cannot handle nested same-name tags. This is already a settled decision in the codebase -- see `WorldviewTaskStrategy` line 346 comment: "Jackson XML 无法处理嵌套同名 <r> 标签". Same applies to nested `<f>` tags. |
| XML parsing | `org.w3c.dom` DOM | SAX / StAX | DOM loads the entire XML into memory. The AI responses are small (< 10KB), so memory is not a concern. DOM provides easier random access and tree traversal, which matches the recursive parsing pattern. |
| Frontend tree | Hand-rolled template | `vue3-treeview` / `sl-vue-tree` / `vue-tree` | Adds a dependency for minimal benefit. The GeographyTree pattern works. Faction trees are shallow (2-3 levels). The explicit nesting approach is readable and maintainable. |
| Faction relation type | VARCHAR string | ENUM column type | VARCHAR is more flexible. Relation types may evolve (e.g., "宗门", "附属", "暗盟"). VARCHAR avoids ALTER TABLE for new types. The existing codebase uses strings for similar classifications (e.g., `NovelCharacter.roleType`). |
| Batch insert | Sequential `insert()` in loop | `IService.saveBatch()` | The faction tree for a single project is small (typically 5-30 nodes). Sequential insert within a `@Transactional` method is simple and fast enough. `saveBatch` requires extending `IService` which adds complexity for negligible gain at this data volume. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `Element.getElementsByTagName()` for recursive child parsing | Returns ALL descendants with that tag name across the entire subtree, not just direct children. This would incorrectly match deeply nested `<f>` tags when you only want immediate children. | `Element.getChildNodes()` + filter by `Node.ELEMENT_NODE` + check `getNodeName()`. This is what the existing `parseSingleRegion()` already does. |
| Jackson `XmlMapper` for nested faction parsing | Cannot handle nested same-name tags like `<f><f></f></f>`. Already confirmed broken in this codebase for the geography case. | `DocumentBuilder` + `org.w3c.dom` DOM parsing. |
| `IService<T>` / `ServiceImpl` for FactionService | Adds framework coupling and the batch methods are overkill for this data volume. The existing `ContinentRegionService` / `ContinentRegionServiceImpl` use plain `@Service` + `@Autowired Mapper`, which is simpler and the established pattern. | Plain `@Service` + `@Autowired NovelFactionMapper` + `@Transactional`. Match the `ContinentRegionServiceImpl` pattern. |
| `DELETE` without collecting descendants first | A simple `deleteById()` on a parent node leaves orphaned children in the database. The geography code handles this correctly by collecting all descendant IDs first. | `collectDescendantIds()` + `deleteBatchIds()`. Copy the pattern from `ContinentRegionServiceImpl`. |
| Frontend state management (Pinia store) for tree data | The GeographyTree loads data on mount and refreshes after each mutation. No cross-component sharing needed. Adding a Pinia store would be over-engineering. | Component-local `ref<NovelFaction[]>([])` loaded on mount. Same as GeographyTree. |
| Separate REST endpoint per relationship type | Could create `/factions/{id}/regions`, `/factions/{id}/characters`, `/factions/{id}/relations`. This fragments the API and increases frontend call count. | Include relationships in the faction tree response (regions as names, relations as nested objects). Use dedicated endpoints only for character associations (which are managed separately). |

## Stack Patterns by Variant

**If faction tree exceeds 100 nodes per project:**
- Consider adding a `project_id + parent_id` composite index on `novel_faction` for faster tree queries
- The Java-side `buildTree()` still works fine at this scale, no need for CTE

**If faction-faction relations become bidirectional (A is ally of B means B is ally of A):**
- Store relations once with a convention (lower ID first), add a UNIQUE constraint on `(faction_id, related_faction_id)`
- Alternatively, query both directions: `WHERE faction_id = ? OR related_faction_id = ?`
- For now, store relations as directed (one row per direction) since "A is ally of B" and "B is enemy of A" can coexist in fiction

**If the GeographyTree explicit nesting approach becomes unwieldy (more than 4 levels):**
- Refactor to a true recursive Vue component using `defineComponent` with `name` property for self-reference
- The current GeographyTree hardcodes 4 levels (lines 180-348), which is sufficient for factions
- Faction trees in Chinese web novels typically have: Sect -> Hall -> Branch (3 levels max)

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| MyBatis-Plus 3.5.5 | Spring Boot 3.2.0 | Confirmed via `mybatis-plus-spring-boot3-starter` artifact in pom.xml |
| MyBatis-Plus 3.5.5 | Java 21 | Supported since MyBatis-Plus 3.5.3+ |
| Vue 3.5.24 + Vite 7.2.4 | TypeScript 5.9.3 | All latest stable, confirmed compatible in existing package.json |
| Tailwind CSS 4.1.18 | PostCSS 8.5.6 | v4 uses new engine, already configured in the project |
| Jackson XML | Spring Boot 3.2.0 | Version managed by Spring Boot parent POM |
| `org.w3c.dom` | Java 21 (JDK built-in) | Part of JDK, no dependency needed |

## New Files to Create

### Backend
| File | Pattern Source |
|------|---------------|
| `NovelFaction.java` (entity) | Copy `NovelContinentRegion.java`, add `type`, `corePowerSystemId` fields |
| `NovelFactionMapper.java` | Copy `NovelContinentRegionMapper.java` |
| `NovelFactionRegion.java` (entity) | Copy `NovelWorldviewPowerSystem.java`, add second FK |
| `NovelFactionCharacter.java` (entity) | Copy `NovelWorldviewPowerSystem.java`, add `role` field |
| `NovelFactionRelation.java` (entity) | New, self-referencing pair with `relationType` + `description` |
| `FactionService.java` (interface) | Copy `ContinentRegionService.java` |
| `FactionServiceImpl.java` | Copy `ContinentRegionServiceImpl.java`, add name-to-ID resolution |
| `FactionController.java` | Copy `ContinentRegionController.java`, add relation endpoints |

### Frontend
| File | Pattern Source |
|------|---------------|
| `faction.ts` (API) | Copy `continentRegion.ts`, add relation/character endpoints |
| `FactionTree.vue` | Copy `GeographyTree.vue`, add type badge and power system tag |

### Database
| File | Purpose |
|------|---------|
| SQL migration script | Create 4 new tables, add indexes, eventually drop `novel_worldview.forces` column |

## Sources

- **Existing codebase analysis** (HIGH confidence) -- All patterns verified by reading actual source files:
  - `ContinentRegionServiceImpl.java` -- Tree CRUD lifecycle
  - `NovelContinentRegion.java` -- Entity pattern with `@TableField(exist = false)`
  - `GeographyTree.vue` -- Frontend tree component pattern
  - `WorldviewTaskStrategy.java` -- DOM XML parsing and worldview save flow
  - `XmlParser.java` -- Jackson XML wrapper (confirmed unsuitable for nested same-name tags)
  - `NovelWorldviewPowerSystem.java` -- Many-to-many join table pattern
  - `pom.xml` -- Dependency versions confirmed
  - `package.json` -- Frontend versions confirmed

- **MyBatis-Plus batch operations** -- Multiple sources confirm `saveBatch` is not true batch insert without `rewriteBatchedStatements=true`. Not relevant at faction data volumes. ([Comate](https://comate.baidu.com/zh/page/ut2wio36mvo), [CSDN](https://ask.csdn.net/questions/8968284), [juejin](https://juejin.cn/post/7221739494277791800))

- **DOM XML nested same-name tag parsing** -- Stack Overflow and community consensus: use `getChildNodes()` not `getElementsByTagName()` for recursive parsing. ([SO](https://stackoverflow.com/questions/18391388/parsing-xml-with-tags-with-same-name-on-different-levels-in-dom), [CodeRanch](https://coderanch.com/t/127860/languages/nested-elements-DOM))

- **Vue 3 recursive components** -- Community pattern: explicit nesting for shallow trees, self-referencing component for deep trees. ([Hashnode](https://nhasbeen.hashnode.dev/vue-3-recursion-with-treeview-component), [DEV Community](https://dev.to/jacobandrewsky/building-recursive-components-in-vue-46cc))

---
*Stack research for: faction structured refactoring in AI Factory*
*Researched: 2026-04-01*
