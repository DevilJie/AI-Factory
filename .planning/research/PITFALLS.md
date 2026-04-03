# Pitfalls Research

**Domain:** Faction/Force structured refactoring for AI novel generation app (tree-structured data + relationship tables + AI XML parsing)
**Researched:** 2026-04-01
**Confidence:** HIGH (derived from direct codebase analysis of existing patterns, not external sources)

---

## Critical Pitfalls

### Pitfall 1: getElementsByTagName Silently Collects All Descendants (Not Just Direct Children)

**What goes wrong:**
When parsing nested faction XML like `<faction><name>X</name><faction><name>Y</name></faction></faction>`, calling `element.getElementsByTagName("name")` returns ALL descendant `<name>` elements across the entire subtree. The parent faction gets assigned the child faction's name, or the first match is used incorrectly. This is the exact bug pattern already present in `WorldviewTaskStrategy#parseSingleRegion` at lines 434 and 442, where `getElementsByTagName("n")` and `getElementsByTagName("d")` would grab nested region names/descriptions from grandchildren if the XML ever contained them.

**Why it happens:**
`Element.getElementsByTagName()` is a DOM method that searches the entire subtree recursively. Developers assume it works like `querySelector` with direct-child semantics, but it does not. The geography code partially avoids this for `<r>` child nodes by using `getChildNodes()` at line 449, but still uses the greedy `getElementsByTagName` for `<n>` and `<d>` fields. For factions with nested sub-factions, this bug is guaranteed to manifest.

**How to avoid:**
Always use `getChildNodes()` iteration with node name checks for direct-child access. Write a helper method:
```java
private String getDirectChildText(Element parent, String tagName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
            return child.getTextContent().trim();
        }
    }
    return null;
}
```
Use this consistently for ALL tag reads within the faction parsing code.

**Warning signs:**
- A faction with sub-factions gets its name set to a sub-faction's name
- `description` field contains concatenated text from multiple nested descriptions
- Tests pass for flat faction lists but fail when factions have children

**Phase to address:**
Phase 1 (Backend: Faction entity + DOM parsing). The parsing helper must be correct from the first commit.

---

### Pitfall 2: Incomplete Migration of worldview.getForces() Call Sites

**What goes wrong:**
After making `worldview.forces` transient and adding `fillForces()`, some callers are missed. Codebase grep shows `worldview.getForces()` is read in at least 10 different locations across 7 files:
- `ChapterService.java` (lines 1003-1004)
- `PromptTemplateBuilder.java` (lines 382-383)
- `ChapterPromptBuilder.java` (lines 160-161)
- `PromptContextBuilder.java` (lines 178-179)
- `ChapterFixTaskStrategy.java` (lines 421-422)
- `ChapterGenerationTaskStrategy.java` (lines 423-424, 557-558)
- `VolumeOptimizeTaskStrategy.java` (lines 367-368)
- `OutlineTaskStrategy.java` (lines 907-908, 977-978, 1672, 1723-1724, 1770-1771)
- `WorldviewTaskStrategy.java` (line 337 -- writer)

Missing even one of these means that in some chapter generation flows, the AI receives no faction information, producing chapters that ignore faction relationships entirely. The symptom is intermittent -- chapters generated via some code paths reference factions, others do not.

**Why it happens:**
The callers are spread across multiple strategy classes with no shared interface for "build worldview context." Each strategy independently constructs its prompt string. There is no single point where `fillForces()` can be called -- it must be called before every `getForces()` usage, matching the pattern of `fillGeography()` which is called at 8+ separate locations.

**How to avoid:**
1. Do a comprehensive grep for `getForces()` before starting the migration
2. For each call site, add `factionService.fillForces(worldview)` immediately before it, following the exact pattern of `continentRegionService.fillGeography(worldview)` calls
3. After making `forces` transient (`@TableField(exist = false)`), the compiler will NOT catch missed sites because Lombok generates the getter regardless -- only runtime testing will reveal gaps
4. Write an integration test that exercises each strategy and asserts that faction text appears in the prompt

**Warning signs:**
- A chapter generation produces content that ignores all faction relationships
- The behavior differs between "generate chapter" and "fix chapter" flows
- Some strategies produce faction-aware output, others do not

**Phase to address:**
Phase 2 (Backend: WorldviewTaskStrategy refactor + transient migration). Must be verified before Phase 4 (Frontend).

---

### Pitfall 3: Name-to-ID Resolution Fails on Non-Exact Matches

**What goes wrong:**
The plan specifies that AI outputs faction names (not IDs), and the backend resolves names to IDs by querying the database. But AI-generated text is inherently imprecise. The LLM might output "紫阳宗" in the faction list but reference "紫阳宗门" or "紫阳宗（正道）" in the relationship table. The name lookup returns null, the faction-region or faction-faction relation is silently dropped, and the structured data ends up incomplete with no error reported.

This is especially critical because the PROJECT.md specifies: "AI outputs names for power systems and geography, backend looks up by name." But power systems have unique, unambiguous names within a single project. Factions may have ambiguous or similar names (e.g., "青龙会" vs "青龙帮").

**Why it happens:**
LLMs do not guarantee exact string reproduction across different parts of the same response. A faction name defined in one `<faction>` block might be referenced with slight variations in a `<relation>` block. The name-as-natural-key assumption breaks when the key producer is a probabilistic model.

**How to avoid:**
1. Build a fuzzy name matcher: after exact-match fails, try normalized comparison (trim whitespace, strip parenthetical annotations, remove common suffixes like "门" / "派" / "宗")
2. When a name cannot be resolved, LOG the failure at WARN level with the exact string -- do not silently skip
3. Include all faction names in the prompt template as a reference list before asking AI to generate relationships, so the AI has the exact names available
4. Consider a post-processing validation step that counts how many names were resolved vs. failed, and includes this in the task result

**Warning signs:**
- Faction relationship table has fewer rows than expected
- Some factions have no associated regions despite the AI response clearly mentioning territory
- Log files show no errors but faction-region associations are incomplete

**Phase to address:**
Phase 1 (Backend: entity + service). The name resolution logic must be designed from the start with fuzzy matching in mind.

---

### Pitfall 4: Cascade Delete Misses Relationship Tables

**What goes wrong:**
When a user re-generates worldview or deletes a project, the `checkExisting` method must delete all faction-related data. Looking at the existing pattern in `WorldviewTaskStrategy#checkExisting` (lines 141-207), it manually deletes: worldview-power-system associations, power system levels, steps, the power system itself, continent regions, and the worldview. For factions, there are FOUR tables to clean up: `novel_faction`, `novel_faction_region`, `novel_faction_character`, `novel_faction_relation`. If any one is missed in the cascade, orphan rows remain in the database. When new factions are inserted with auto-increment IDs, these orphans create phantom associations.

This is especially dangerous because `ContinentRegionServiceImpl` handles deletion via `deleteByProjectId` but this is a single table. The faction system has 4 tables with interdependencies.

**Why it happens:**
The delete cascade is implemented as procedural code in `checkExisting`, not as database foreign key constraints. There are no FK constraints in the current schema (based on the codebase pattern). Developers must remember every table in the right order. When a new table is added later (e.g., a `novel_faction_item` table for faction artifacts), the delete cascade code must be updated in multiple places.

**How to avoid:**
1. Centralize faction deletion in `FactionService.deleteByProjectId(Long projectId)` which deletes ALL four faction tables in the correct order (relations first, then character associations, then region associations, then factions themselves)
2. Call this single method from `WorldviewTaskStrategy#checkExisting` -- do NOT inline the delete logic
3. Write the deletion order as: `novel_faction_relation` -> `novel_faction_character` -> `novel_faction_region` -> `novel_faction` (delete dependent tables before the main table)
4. Test with a fixture that has data in all four tables, delete, then assert all four tables are empty for that project

**Warning signs:**
- After worldview regeneration, old faction relationships appear alongside new ones
- Auto-increment ID gaps suggest orphan rows
- Faction count on frontend differs from expected after regeneration

**Phase to address:**
Phase 1 (Backend: entity + service). The `deleteByProjectId` method must be implemented before any other CRUD.

---

### Pitfall 5: GeographyTree.vue Hard-Coded Depth Levels Create Unmaintainable Copy-Paste

**What goes wrong:**
The existing `GeographyTree.vue` (357 lines) renders tree depth through explicit template nesting: Level 0, Level 1, Level 2, Level 3+ are each separate `<template v-for>` blocks with hard-coded padding (`paddingLeft: '40px'`, `'60px'`, `'80px'`). This is a massive copy-paste pattern where each depth level duplicates the entire node template (edit mode, add-child form, delete button, icon logic). For the faction tree, if the same approach is used, any UI change (e.g., adding a faction type badge, power system indicator) must be replicated across 4+ template blocks.

The faction tree has MORE data per node than geography (type, core_power_system, inherited fields). Hard-coding depth levels with this richer data model would make the component nearly unmaintainable.

**Why it happens:**
Vue 2 had limited recursive component support. Vue 3 supports recursive components natively, but the GeographyTree developer chose the explicit-nesting approach, likely for simplicity or to avoid recursive component complexity. Once established, the pattern gets copied to new components.

**How to avoid:**
Build `FactionTree.vue` as a proper recursive Vue 3 component from the start:
1. Create a `FactionTreeNode.vue` component that accepts a single node and renders itself, then recursively renders `FactionTreeNode` for each child
2. Pass `depth` as a prop to compute indentation dynamically: `paddingLeft: ${20 + depth * 20}px`
3. Keep all node-specific logic (type badge, power system label, edit/delete/add actions) in the single recursive component
4. This eliminates the 4x template duplication entirely

**Warning signs:**
- FactionTree.vue exceeds 300 lines
- The same edit/delete/add markup appears more than twice
- A UI change requires edits in 3+ places within the same file

**Phase to address:**
Phase 4 (Frontend: FactionTree component). Must be the first design decision before writing any template code.

---

### Pitfall 6: Top-Level-Only Fields (type, core_power_system) Not Properly Inherited on Read

**What goes wrong:**
The PROJECT.md specifies that `type` and `core_power_system` are set only on top-level factions; sub-factions inherit these values from their top-level ancestor. When building the prompt text (via `fillForces()` or `buildFactionText()`), if the code reads `faction.getType()` on a sub-faction, it gets `null` because the sub-faction's `type` column is empty in the database. The prompt either omits the sub-faction's type or outputs "null", confusing the AI.

Even worse: when displaying in the frontend tree, sub-factions without an explicit type appear untyped, breaking the UI consistency.

**Why it happens:**
The "inherit from top-level ancestor" requirement is a display/inference rule, not a storage rule. The database stores `NULL` for sub-faction type. But the code that reads faction data for prompt building or API responses must walk up the tree to find the top-level ancestor and read its type. This ancestor-walk is easy to forget or implement incorrectly (especially for deeply nested factions).

**How to avoid:**
1. Implement `FactionService.getTopLevelAncestor(Long factionId)` that walks the `parent_id` chain until `parent_id IS NULL`
2. Use this in `buildFactionText()` to prefix each faction with its inherited type
3. In the API response for the tree, either: (a) add a computed `inheritedType` and `inheritedPowerSystemId` field that is populated during tree assembly, or (b) let the frontend compute inheritance from the tree structure
4. Prefer option (b) for the API -- simpler backend, and the frontend already has the full tree in memory

**Warning signs:**
- Sub-factions appear without a type badge in the frontend
- AI-generated chapters refer to sub-factions without knowing if they are good/evil/neutral
- The `fillForces()` prompt text has "null" where type should be

**Phase to address:**
Phase 1 (Backend: entity + service). The inheritance logic must be in `buildFactionText()` from the start.

---

### Pitfall 7: AI Prompt Template Change Breaks Existing Projects

**What goes wrong:**
The worldview generation prompt template (id=3 in `ai_prompt_template_version`) will be modified to request structured XML output for factions instead of plain text. But existing projects that already have a worldview with plain-text `forces` field will break in two ways:
1. If the user re-generates worldview, the new template expects different XML structure
2. If the user does NOT re-generate, the old plain-text `forces` data remains in the DB column which is about to become transient

Worse: making `forces` transient (`@TableField(exist = false)`) means the column data becomes unreadable via MyBatis-Plus. Any existing project with `forces` text in the DB column loses that data permanently. Since there is no structured faction data to replace it (no rows in `novel_faction`), these projects have zero faction information.

**Why it happens:**
The migration assumes a clean "re-generate worldview" flow. But users may have projects at any stage -- some with chapters already written that reference specific faction descriptions. Losing the forces text corrupts their story's continuity.

**How to avoid:**
1. Do NOT drop the `forces` column immediately. Keep it in the schema alongside the new transient field
2. The migration should: (a) add new faction tables, (b) keep `novel_worldview.forces` column, (c) mark it as deprecated
3. In `fillForces()`, first check if structured faction data exists. If yes, build text from it. If no, fall back to reading the old `forces` column directly (via a raw SQL query or a separate mapper)
4. Only drop the `forces` column in a future migration after all active projects have been regenerated or manually migrated
5. Consider a one-time migration script that parses old `forces` text into structured faction rows for existing projects

**Warning signs:**
- Existing project shows "no faction data" after deployment
- Chapters generated for old projects no longer reference any factions
- The `forces` column still exists in DB but the entity field is marked transient

**Phase to address:**
Phase 2 (Backend: transient migration + SQL migration). The migration script MUST include backward compatibility handling.

---

### Pitfall 8: Faction-Faction Relation Table Creates Bidirectional Confusion

**What goes wrong:**
The `novel_faction_relation` table stores a relationship between two factions (ally/hostile/neutral). If faction A is hostile to faction B, there are two ways to store this:
1. One row: `{faction_a_id: 1, faction_b_id: 2, type: "hostile"}`
2. Two rows: `{faction_a_id: 1, faction_b_id: 2, type: "hostile"}` AND `{faction_a_id: 2, faction_b_id: 1, type: "hostile"}`

If the design stores one row but the query checks both directions, some relationships are missed. If the design stores two rows but the AI generates asymmetric relationships (A is hostile to B, but B is neutral toward A -- common in complex politics), the bidirectional insert overwrites one direction. The frontend "edit relationship" dialog also becomes confusing: does editing A's view of B also change B's view of A?

**Why it happens:**
Faction relationships are conceptually undirected (alliance is mutual) but AI may generate them as directed (A fears B, B despises A). The table design must choose one model and enforce it consistently. The PROJECT.md says the relation table has "relationship type (ally/hostile/neutral)" which implies undirected, but AI-generated descriptions may be asymmetric.

**How to avoid:**
1. Store relationships as undirected: enforce `faction_a_id < faction_b_id` (smaller ID first) as a convention
2. Add a unique constraint on `(faction_a_id, faction_b_id)` where `faction_a_id < faction_b_id`
3. In the service layer, when inserting a relation, always normalize so the smaller ID is first
4. When querying relations for faction X, check both columns: `WHERE faction_a_id = X OR faction_b_id = X`
5. The `description` field captures any asymmetric nuance (e.g., "A distrusts B due to past betrayal, but B seeks reconciliation")

**Warning signs:**
- The same faction pair appears twice in the relationship table with different types
- Deleting a relationship from faction A's perspective does not remove it from faction B's perspective
- The relation count in the database grows faster than expected

**Phase to address:**
Phase 1 (Backend: entity + table design). The constraint and normalization must be in the initial schema.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Copy GeographyTree.vue's explicit nesting instead of recursive component | Ship faster, familiar pattern | 4x code duplication, every UI change requires 4 edits | Never -- do it right once |
| Keep `forces` DB column and mark transient without migration | No backward compatibility work | Existing projects lose faction data silently | Never -- must handle migration |
| Use `getElementsByTagName` for faction XML parsing | Less code, works for flat structures | Breaks when factions have sub-factions | Never -- use direct-child iteration |
| Skip fuzzy name matching in AI name-to-ID resolution | Simpler code, exact match only | Silent data loss when AI uses slightly different names | Only if AI output format is extremely constrained |
| Inline faction cascade delete in WorldviewTaskStrategy | No new service method needed | Must update multiple places when adding tables | Never -- centralize in FactionService |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Jackson XML (XmlParser) for faction parsing | Using it for nested same-name tags (e.g., `<faction>` inside `<faction>`) -- Jackson XML cannot handle this, same as `<r>` in geography | Use DOM parsing for faction tree, same as `saveGeographyRegionsFromXml` pattern |
| DOM parsing AI response | Extracting `<f>` fragment with `indexOf("<f>")` / `indexOf("</f>")` which breaks if `<f>` appears inside a description | Use the same pattern as geography: wrap in `<root>`, parse full DOM, then navigate to the correct element by position |
| Prompt template update (id=3) | Changing the template without versioning, breaking projects that were mid-generation | Create a new template version; do not modify the existing one in-place |
| Faction-to-region name lookup | Assuming region names are unique across the entire geography tree | Use `projectId` scope AND handle duplicate names by matching the first or logging ambiguity |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| N+1 queries in `fillForces()` when building text for each faction with its inherited type | Slow prompt construction, especially for worlds with 50+ factions | Batch-load all factions for the project, build a parentId->faction map, resolve inheritance in memory | 30+ factions in a single project |
| Recursive `collectDescendantIds` in delete (same pattern as geography `deleteByProjectId`) | Stack overflow or slow delete for deeply nested faction trees | Use a single `DELETE WHERE project_id = ?` for each table (the geography code already does this correctly) | 5+ levels of nesting |
| Loading full faction tree + relations + regions + characters for every chapter generation | Prompt construction takes 5+ seconds due to multiple DB queries | Cache the faction text in the worldview object, invalidate only on faction data change | More of a concern as project count grows |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| No project-scoping on faction API endpoints | User A reads User B's factions by guessing IDs | Always filter by `projectId` AND verify the project belongs to the authenticated user (follow existing pattern in controllers) |
| SQL injection in name-based lookup | Maliciously crafted faction name could exploit raw SQL for name resolution | Use parameterized queries even for name lookups: `WHERE name = ? AND project_id = ?` |
| XSS via faction description in frontend | AI-generated faction descriptions contain HTML/JS that executes in the tree view | Use `v-text` or sanitize AI output before display, not `v-html` |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Frontend shows empty tree after worldview generation (faction data exists but tree is not refreshed) | User thinks generation failed, re-generates unnecessarily | Auto-refresh tree after worldview generation completes, same as geography tree refresh pattern |
| Edit faction inline shows type/power system fields for sub-factions (which should inherit, not set) | User sets type on sub-faction, creating data inconsistency with the inheritance rule | Hide type and core_power_system fields for non-root factions, show inherited value as read-only badge |
| Deleting a faction with relations does not warn about cascading relation deletion | User loses faction relationship data without realizing | Show confirmation: "Deleting X will also remove 3 alliance/hostile relationships" |
| Faction tree with 50+ nodes loads slowly and is hard to navigate | User cannot find specific factions, gives up on managing them | Add search/filter, collapse-all/expand-all controls from the start |

## "Looks Done But Isn't" Checklist

- [ ] **Faction CRUD:** Often missing cascade delete of child factions when parent is deleted -- verify delete with nested children
- [ ] **Faction XML parsing:** Often works for flat faction lists but breaks for nested sub-factions -- verify with 3+ levels of nesting
- [ ] **Transient forces field:** Often marked `@TableField(exist = false)` but `fillForces()` is not called at ALL call sites -- verify every strategy that uses `getForces()`
- [ ] **Faction-region association:** Often created during AI generation but not displayed in the frontend tree -- verify region names appear in faction tree view
- [ ] **Faction-faction relation:** Often created in one direction only -- verify querying from either faction returns the relationship
- [ ] **SQL migration:** Often adds new tables but does not handle the old `forces` column data -- verify existing projects still have faction info after migration
- [ ] **Tree sort order:** Often factions are inserted in correct order but displayed in wrong order -- verify `sort_order` is set during AI generation and used in `ORDER BY`
- [ ] **checkExisting cascade:** Often deletes factions but forgets relation/region/character association tables -- verify all four tables are clean after worldview re-generation

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Missed getForces() call site | LOW | Add `factionService.fillForces(worldview)` before the missed call. No data migration needed. |
| AI name mismatch causing dropped relations | MEDIUM | Re-run worldview generation. Alternatively, manually fix in database by inserting missing relations. |
| getElementsByTagName bug in parsing | MEDIUM | Fix the parsing method, delete worldview, re-generate. No data loss if caught before production. |
| Old forces data lost by premature column drop | HIGH | Restore from database backup, then implement proper migration with backward compatibility. |
| Hard-coded depth levels in frontend | MEDIUM | Refactor to recursive component. Functional regression risk during refactor. |
| Bidirectional relation confusion | LOW | Add normalization constraint, run cleanup script to merge duplicate/reversed rows. |
| Cascade delete missing a table | LOW | Add the missing delete to FactionService.deleteByProjectId. Run cleanup for existing orphans. |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| getElementsByTagName descendant bleed | Phase 1: Backend entity + service + DOM parsing | Unit test with 3-level nested faction XML, assert each level gets correct name/description |
| Incomplete getForces() migration | Phase 2: Transient field migration | Grep for all `getForces()` call sites, verify each has `fillForces()` before it |
| Name-to-ID fuzzy match failure | Phase 1: Name resolution in FactionService | Test with AI output that uses slightly different names than the faction list |
| Cascade delete missing tables | Phase 1: FactionService.deleteByProjectId | Integration test: create data in all 4 tables, delete, assert all empty |
| GeographyTree copy-paste pattern | Phase 4: Frontend FactionTree | Component line count < 200, single template block, recursive child rendering |
| Type/power system inheritance not read | Phase 1: buildFactionText | Test that sub-faction text includes inherited type from root ancestor |
| Prompt template breaks existing projects | Phase 2: Template update + SQL migration | Deploy to staging with existing project data, verify old projects still work |
| Bidirectional relation confusion | Phase 1: Table schema + constraint | Unit test: insert relation A->B, query from B, verify relation found; insert duplicate, verify constraint blocks |

## Sources

- Direct codebase analysis of `WorldviewTaskStrategy.java` -- DOM parsing pattern at lines 368-461, cascade delete at lines 141-207
- Direct codebase analysis of `ContinentRegionServiceImpl.java` -- tree CRUD pattern, `fillGeography()` at lines 275-279
- Direct codebase analysis of `GeographyTree.vue` -- hard-coded depth level nesting pattern, 357 lines
- Direct codebase analysis of `XmlParser.java` -- Jackson XML limitations documented in `WorldSettingXmlDto.java` line 23
- Direct grep of `getForces()` across 7 files showing 10+ read locations requiring migration
- `CONCERNS.md` -- known issues with XML parsing fragility, missing transaction management, null handling

---
*Pitfalls research for: faction/force structured refactoring*
*Researched: 2026-04-01*
