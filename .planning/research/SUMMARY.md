# Project Research Summary

**Project:** AI Factory -- Faction/Force Structured Refactoring
**Domain:** Tree-structured faction data with many-to-many relationships for AI novel generation (Spring Boot + Vue.js)
**Researched:** 2026-04-01
**Confidence:** HIGH

## Executive Summary

This is a structural refactoring of the faction/force subsystem in an AI novel generation application, converting a flat-text `forces` field into a normalized tree of faction entities with associated relationship tables. The product is a Chinese web novel (xianxia/wuxia) writing tool where AI generates worldbuilding content, which the backend parses into structured database tables and later feeds back into AI chapter generation as rich prompt context. Experts in this domain (World Anvil, Kanka, Novelcrafter) build faction systems as hierarchical entities with typed relationships, and this refactoring brings AI Factory to structural parity while retaining its unique advantage: fully automated AI generation of structured faction data.

The recommended approach is to mirror the already-proven geography tree pattern (`NovelContinentRegion` / `ContinentRegionServiceImpl` / `GeographyTree.vue`) that exists in this codebase. This is the third iteration of the same structural pattern (power systems were first, geography was second, factions are third), so the architecture is well-established and the risk is low. The core stack is locked (Spring Boot 3.2.0, MyBatis-Plus 3.5.5, Vue 3.5.24, MySQL 8.0+), and no new dependencies are needed. The one area where factions differ from geography is the introduction of many-to-many relationship tables (faction-region, faction-character, faction-faction), which add moderate complexity to the service and persistence layers.

The key risks are: (1) incomplete migration of `worldview.getForces()` call sites across 7+ strategy classes, which would cause intermittent loss of faction context in some AI generation flows; (2) AI name-to-ID resolution failures when the LLM produces slightly different faction names across different parts of its response; and (3) data loss for existing projects if the `forces` DB column is dropped before all projects have been migrated. All three risks have clear mitigations documented in PITFALLS.md.

## Key Findings

### Recommended Stack

The core stack is locked by the existing codebase. This research confirms that no new dependencies, version changes, or library additions are required. The faction system reuses patterns already proven in production for the geography and power system modules.

**Core technologies:**
- Spring Boot 3.2.0 + MyBatis-Plus 3.5.5: Backend framework + ORM -- already in use, tree CRUD and adjacency list patterns validated
- Vue 3.5.24 + Tailwind CSS 4.1.18: Frontend -- Composition API with `<script setup>`, hand-rolled recursive tree component
- `org.w3c.dom` DOM parsing: XML parsing for nested faction tags -- Jackson XML cannot handle nested same-name tags (settled decision in codebase)
- MySQL 8.0+: Storage -- adjacency list (`parent_id + deep`) tree pattern, auto-increment IDs

### Expected Features

**Must have (table stakes -- P1):**
- Faction table with tree CRUD (hierarchical create, read, update, delete mirroring geography pattern)
- Type classification (righteous/evil/neutral) on root factions with inheritance for children
- Power system association via FK to `novel_power_system` on root factions
- AI prompt template update to output structured faction XML instead of plain text
- DOM parsing of faction XML in `WorldviewTaskStrategy` with name-to-ID resolution
- `fillForces()` transient field replacing the DB column, called at all prompt-building sites
- FactionTree.vue component mirroring GeographyTree.vue with type badge and power system label
- SQL migration creating 4 new tables (`novel_faction`, `novel_faction_region`, `novel_faction_character`, `novel_faction_relation`)

**Should have (competitive -- P2):**
- Faction-to-faction relationships with typed relations (ally/enemy/neutral + description) -- unique differentiator for AI context
- Faction-to-character association with role field (manual-only, post-generation)
- Faction-to-region association (deferrable since geography already provides region context)

**Defer (v2+):**
- Visual faction relationship graph / diplomacy web -- high frontend cost, low ROI for AI writing tool
- Faction templates per genre -- needs its own research cycle
- Faction event log / timeline integration -- blocked by timeline structuring milestone

### Architecture Approach

The architecture follows a three-layer vertical slice that directly mirrors the geography module: entities (4 new MyBatis-Plus entity classes + 4 mappers), service layer (FactionService/FactionServiceImpl with tree CRUD, fillForces, association management), and presentation (FactionController REST API + FactionTree.vue component). The existing `WorldviewTaskStrategy` and `PromptContextBuilder` are modified to integrate faction parsing and prompt building.

**Major components:**
1. **NovelFaction entity + FactionService** -- Tree-structured faction CRUD with adjacency list pattern, cascading delete, and `fillForces()` text builder
2. **Relationship tables** (NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation) -- Many-to-many associations with name-to-ID resolution
3. **WorldviewTaskStrategy modifications** -- DOM parsing of nested `<faction>` XML, two-pass insert (pass 1: factions, pass 2: relations with resolved IDs)
4. **FactionTree.vue** -- Recursive Vue 3 component with type badges, power system labels, and inline editing (must be recursive, NOT the GeographyTree explicit-nesting pattern)

### Critical Pitfalls

1. **`getElementsByTagName` silently collects all descendants** -- Use `getChildNodes()` iteration for direct-child access only. Write a `getDirectChildText()` helper from the start.
2. **Incomplete migration of `getForces()` call sites** -- 10+ locations across 7 files. Compiler will not catch missed sites because Lombok generates the getter regardless. Must grep comprehensively and test each strategy.
3. **AI name-to-ID resolution fails on non-exact matches** -- LLM may output "紫阳宗" in the faction list but "紫阳宗门" in the relation block. Build fuzzy matching with normalization; log failures at WARN level.
4. **Cascade delete misses relationship tables** -- 4 tables to clean up in correct order. Centralize in `FactionService.deleteByProjectId()`, do not inline in WorldviewTaskStrategy.
5. **Hard-coded depth levels in FactionTree.vue** -- GeographyTree uses explicit nesting (4 template blocks). FactionTree MUST use a proper recursive component; factions have richer data per node (type, power system) making copy-paste unmaintainable.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Database Schema + Domain Layer
**Rationale:** Everything depends on the database tables and entity definitions. This phase has zero external dependencies and establishes the data model that all other phases build on.
**Delivers:** 4 new tables, 4 entity classes, 4 mapper interfaces, NovelWorldview.forces marked as transient
**Addresses:** Faction table with tree CRUD, SQL migration (P1 features)
**Avoids:** Pitfall 1 (DOM parsing helper from the start), Pitfall 4 (centralized deleteByProjectId), Pitfall 6 (inheritance logic in buildFactionText), Pitfall 8 (bidirectional relation constraint in schema)

### Phase 2: Backend Service + Controller
**Rationale:** Depends on Phase 1 entities. Implements the full CRUD lifecycle and association management that both the AI integration and frontend will consume.
**Delivers:** FactionService interface + implementation, FactionController REST endpoints, fillForces() text builder
**Uses:** MyBatis-Plus LambdaQueryWrapper, @Transactional, BaseMapper patterns
**Implements:** Tree CRUD service, name-to-ID resolution with fuzzy matching, cascade delete, type/power system inheritance

### Phase 3: AI Integration (Backend)
**Rationale:** Depends on Phase 2 service. This is the highest-risk phase because it modifies the existing worldview generation pipeline and prompt template. Must be tested with existing project data.
**Delivers:** Updated prompt template (structured faction XML), DOM parsing in WorldviewTaskStrategy, fillForces() integration in all prompt builders
**Addresses:** AI prompt template update, DOM parsing, fillForces() migration (P1 features)
**Avoids:** Pitfall 2 (comprehensive getForces() migration), Pitfall 3 (fuzzy name matching), Pitfall 7 (backward compatibility for existing projects -- keep forces column, add fallback)
**Risk:** Modifying the AI prompt template affects generation quality. Must A/B test prompt output.

### Phase 4: Frontend Tree Component
**Rationale:** Depends on Phase 2 API. Can run in parallel with Phase 3 since both depend on Phase 2 but not on each other.
**Delivers:** FactionTree.vue (recursive component), faction.ts API client, WorldSetting.vue integration
**Uses:** Vue 3 Composition API, Tailwind CSS, Lucide icons
**Avoids:** Pitfall 5 (recursive component, not explicit nesting)
**Implements:** Type badges, power system labels, inline editing, collapsible sections

### Phase 5: Relationship Management (P2 Features)
**Rationale:** Faction-faction relations, faction-character associations, and faction-region associations can be layered on after the core tree is stable. These are independent features that use the same entity pattern.
**Delivers:** FactionRelations.vue, FactionCharacterPanel.vue, relation CRUD APIs, region association UI
**Addresses:** P2 competitive features

### Phase 6: Cleanup + Migration
**Rationale:** Must come last. Dropping the `forces` column is irreversible. Only execute after all code paths use the new transient field and existing projects have been verified.
**Delivers:** ALTER TABLE to drop forces column, removal of legacy code paths, optional data migration script for existing projects
**Avoids:** Pitfall 7 (data loss for existing projects)

### Phase Ordering Rationale

- Phase 1 is foundational: all other phases depend on database tables and entity classes
- Phase 2 before Phases 3 and 4: both the AI integration and frontend consume the service API
- Phases 3 and 4 can run in parallel: AI integration (backend) and frontend are independent once the service layer exists
- Phase 5 comes after the core tree is validated: relationship features layer on top of the stable tree
- Phase 6 is last: column drop is irreversible and requires all consumers to be migrated first

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** AI prompt template design is domain-specific. The structured XML format must be tested with actual LLM output to verify parseability. Consider running prompt experiments before implementation.
- **Phase 3:** Fuzzy name matching strategy needs validation -- the specific normalization rules for Chinese faction names should be tested against real AI output samples.
- **Phase 5:** Faction-character association UI needs UX design decisions (inline vs. panel, character picker component selection).

Phases with standard patterns (skip research-phase):
- **Phase 1:** Exact copy of geography migration pattern. Zero ambiguity.
- **Phase 2:** Direct mirror of ContinentRegionServiceImpl. All patterns documented in STACK.md and ARCHITECTURE.md.
- **Phase 4:** Follows GeographyTree.vue approach (with recursive improvement). Well-documented Vue 3 pattern.
- **Phase 6:** Standard SQL migration. Pattern established by geography/power system migrations.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All patterns verified in existing codebase. No new dependencies. Third iteration of the same structural pattern. |
| Features | HIGH | Feature set derived from direct codebase analysis and competitor research (World Anvil, Kanka, Novelcrafter). Priority matrix validated against PROJECT.md scope. |
| Architecture | HIGH | Architecture directly mirrors the geography module which is running in production. All component boundaries and data flows are traced from actual source files. |
| Pitfalls | HIGH | All pitfalls derived from direct codebase analysis (grep results, line-number references, known bug patterns). Not theoretical -- these are real patterns observed in existing code. |

**Overall confidence:** HIGH

### Gaps to Address

- **AI prompt template output quality:** The structured XML format for factions is designed but untested with actual LLM output. The prompt template must be validated by running test generations before Phase 3 implementation. The exact format may need iteration based on LLM compliance.
- **Existing project data migration:** The strategy of keeping the `forces` column and falling back to it for unmigrated projects is sound, but the specific fallback implementation (raw SQL query vs. separate mapper) needs a decision during Phase 3 planning.
- **Fuzzy name matching rules:** The specific normalization rules for Chinese faction names (which suffixes to strip, how to handle parenthetical annotations) need testing against real AI output samples. The rules proposed in PITFALLS.md are a starting point but need validation.
- **Recursive component depth behavior:** The recommendation to use a recursive Vue 3 component instead of explicit nesting is architecturally sound, but the specific implementation (self-referencing component vs. named component) should be decided during Phase 4 planning based on Vue 3 best practices.

## Sources

### Primary (HIGH confidence)
- **Existing codebase analysis** -- All patterns verified by reading actual source files:
  - `ContinentRegionServiceImpl.java` -- Tree CRUD lifecycle, fillGeography()
  - `NovelContinentRegion.java` -- Entity pattern with @TableField(exist = false)
  - `GeographyTree.vue` -- Frontend tree component (357 lines, explicit nesting pattern)
  - `WorldviewTaskStrategy.java` -- DOM XML parsing, cascade delete, worldview save flow
  - `PromptContextBuilder.java` -- Prompt building with fillGeography()
  - `ChapterPromptBuilder.java` -- Chapter prompt consuming worldview data
  - `NovelWorldview.java` -- Current forces field, transient geography field
  - `XmlParser.java` -- Jackson XML limitations
  - `pom.xml` + `package.json` -- Version compatibility confirmed

### Secondary (MEDIUM confidence)
- **World Anvil** -- Diplomacy Webs feature, Organization template -- competitor feature analysis
- **Kanka** -- Organization/faction features, member roles -- competitor feature analysis
- **Novelcrafter** -- Codex features, relationship affinity scoring -- competitor feature analysis
- **MyBatis-Plus community** -- Batch operations behavior, saveBatch limitations
- **DOM parsing** -- Stack Overflow consensus on getElementsByTagName vs. getChildNodes for nested tags
- **Vue 3 community** -- Recursive component patterns, tree rendering approaches

---
*Research completed: 2026-04-01*
*Ready for roadmap: yes*
