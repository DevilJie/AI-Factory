# Feature Research

**Domain:** Structured faction/force system for AI novel generation application
**Researched:** 2026-04-01
**Confidence:** HIGH (based on codebase analysis + competitor research)

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist in a structured faction system. Missing these makes the refactoring pointless because the AI cannot produce better content than the current flat text approach.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Faction CRUD with tree hierarchy | Users need to create, edit, delete factions with parent-child nesting (e.g., Sect > Inner Sect > Peak). The geography refactoring already proved this pattern. | MEDIUM | Directly mirrors NovelContinentRegion. Service pattern: FactionService with addRegion/updateRegion/deleteRegion equivalents. |
| Faction type classification (ally/enemy/neutral) | Without type, the AI cannot reason about conflict dynamics. Every faction in xianxia/wuxia has alignment. | LOW | Single enum field on top-level faction. Child factions inherit. |
| Power system association | A faction without a power system is narratively hollow -- the AI needs to know what cultivation path the sect practices. | LOW | FK to novel_power_system on top-level faction. Child factions inherit. Requires power systems to exist first (already guaranteed by generation order). |
| Region association (faction-location mapping) | Factions occupy territory. Without this link the AI cannot place events geographically. | LOW | Simple many-to-many table novel_faction_region. No extra UI beyond a multi-select dropdown. |
| AI-generated faction creation | The worldview prompt already generates faction content (<f> tag). Must continue working but now parse into structured tables instead of flat text. | MEDIUM | Refactor <f> section in prompt template to output structured XML. Add DOM parsing in WorldviewTaskStrategy analogous to saveGeographyRegionsFromXml. |
| Transient forces field for prompt building | Chapter generation (PromptTemplateBuilder), outline generation, chapter fix, volume optimize -- all call fillGeography(). Need equivalent fillForces() that reconstructs text from faction tables for AI context. | LOW | Add fillForces(NovelWorldview) to FactionService. Build text from tree + relations. Mark forces as @TableField(exist = false). |
| Tree view UI component | Users already have GeographyTree.vue. A faction tree without a tree component is unusable. | MEDIUM | FactionTree.vue mirrors GeographyTree.vue. Additional fields (type badge, power system label) add moderate UI complexity. |
| Cascading delete | Deleting a parent faction must delete children, relations, and association rows. Users expect this from the geography tree behavior. | LOW | Same recursive collectDescendantIds pattern as ContinentRegionServiceImpl. Also clean novel_faction_relation, novel_faction_region, novel_faction_character rows. |

### Differentiators (Competitive Advantage)

Features that elevate the product beyond flat-text competitors and approach the capabilities of dedicated worldbuilding tools like World Anvil and Kanka.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Faction-to-faction relationships with typed relations (ally/enemy/neutral + description) | World Anvil charges for Diplomacy Webs. Kanka has basic relations. Having structured faction relationships that feed into AI prompts is a genuine differentiator -- the AI can then write conflict-aware dialogue and plot. No other AI novel tool does this. | MEDIUM | novel_faction_relation table. CRUD API. UI: relationship list per faction with type selector + description. The fillForces() text builder includes relationship info for AI context. |
| Faction-to-character association with role/title | Kanka has this as "Members" with roles. For an AI novel tool, this is critical -- the AI needs to know that "Elder Chen" belongs to "Qingyun Sect" when writing chapters. Without it, the AI hallucinates affiliations. | MEDIUM | novel_faction_character table with role field. Manual-only per PROJECT.md. UI: character picker + role input on faction detail page. The fillForces() method can include member info. |
| Type/power-system inheritance from root faction | Novelcrafter uses flat codex entries. Having automatic inheritance (child factions inherit type and core_power_system from their root ancestor) is a structural differentiator -- prevents data inconsistency and reduces user effort. | LOW | Inheritance logic in FactionService: when reading, walk up to root to resolve type and power system. No extra UI -- just display the inherited value with a "(inherited)" badge. |
| Name-based ID resolution during AI generation | AI outputs faction names, backend resolves to IDs by matching against existing power systems and regions. This is clever -- it means the AI does not need to know database IDs, and the system can cross-reference naturally. | LOW | Post-parse resolution step: for each faction, look up core_power_system by name in novel_power_system table, look up region names in novel_continent_region table. Store resolved IDs. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem appealing but would harm the project scope, architecture, or user experience.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| AI auto-association of characters to factions | "Why not have AI automatically link characters to factions during generation?" | Worldview generation happens before character creation in the workflow. Characters do not exist yet. Even if they did, AI hallucination rates for entity linking are high and create dirty data that is hard to audit. | Keep manual-only character linking. Users explicitly assign characters after both entities exist. This is the same approach Kanka and Novelcrafter use. |
| Visual faction relationship graph / map | "Show a diplomacy web like World Anvil" | World Anvil built their Diplomacy Webs as a premium feature with substantial D3.js/canvas rendering. The current frontend uses no graph rendering library. Building this would double the frontend scope and the ROI for an AI novel tool (not a TTRPG campaign manager) is low -- users need the data in prompts, not on a canvas. | Simple tabular relationship list with colored badges for relation type. If demand arises later, a future phase can add visualization. |
| Faction timeline/history events | "Track when factions formed, went to war, merged" | This is timeline structuring, which PROJECT.md explicitly scopes out. Mixing timeline events into faction data would create a coupling that complicates both the timeline refactor (future work) and this faction refactor. | Keep timeline as flat text in worldview. When timeline is later structured, add faction-event links then. |
| Dynamic power shifts / resource tracking | "Track faction influence scores, territory changes over time" | This is a simulation game feature, not a novel writing tool feature. Adding numeric tracking (influence, resources, military strength) creates a game system that needs constant updates and validation, with zero benefit for AI text generation. | Static descriptive fields (description text). The AI reads descriptions and relationships to write -- it does not need numeric simulation data. |
| Bidirectional relation sync | "When I set Faction A as enemy of Faction B, automatically create the reverse relation" | Sounds convenient but creates edge cases: what if the relationship is asymmetric? (A considers B an ally, but B considers A a puppet.) Enforcing symmetry reduces narrative expressiveness for no real UX gain in a manual-editing context. | Allow separate directional relations. The UI can show a hint if a reverse relation is missing, but not auto-create it. |
| Nested faction depth limits | "Limit to 3 levels deep like geography" | Artificially limiting depth prevents valid world structures (e.g., Sect > Hall > Division > Team > Cell). The tree table pattern handles arbitrary depth with no performance cost at typical novel scale (tens to low hundreds of nodes). | No depth limit. Same as geography -- the tree handles it. The UI renders recursively. |

## Feature Dependencies

```
[Faction Table (novel_faction)]
    |--requires--> [Power System Table (novel_power_system)] -- for core_power_system FK
    |--requires--> [Geography Table (novel_continent_region)] -- for region association
    |
    +--[Faction-Region Association (novel_faction_region)]
    |       +--requires--> [Faction Table]
    |       +--requires--> [Geography Table]
    |
    +--[Faction-Character Association (novel_faction_character)]
    |       +--requires--> [Faction Table]
    |       +--requires--> [Character Table (novel_character)]
    |       +--requires--> [Manual linking UI]
    |
    +--[Faction-Faction Relation (novel_faction_relation)]
    |       +--requires--> [Faction Table]
    |
    +--[AI Generation (WorldviewTaskStrategy)]
    |       +--requires--> [Faction Table]
    |       +--requires--> [Prompt Template Update]
    |       +--requires--> [Power System Table] (generation order: power systems saved first)
    |       +--requires--> [Geography Table] (generation order: geography saved first)
    |
    +--[fillForces() for AI Prompts]
            +--requires--> [Faction Table]
            +--enhances--> [Faction-Faction Relation] (includes relation info in prompt text)
            +--enhances--> [Faction-Character Association] (includes member info in prompt text)
            +--requires--> [Transient forces field on NovelWorldview]

[Frontend FactionTree.vue]
    +--requires--> [Faction CRUD API]
    +--requires--> [Faction Tree endpoint]

[Frontend Faction Relations UI]
    +--requires--> [Faction-Faction Relation API]

[Frontend Faction-Character UI]
    +--requires--> [Faction-Character Association API]
    +--requires--> [Character List API] (for character picker dropdown)
```

### Dependency Notes

- **Faction Table requires Power System and Geography Tables:** The generation order in WorldviewTaskStrategy is: power systems -> geography -> factions. This is already guaranteed because the AI output contains all sections, and parsing is sequential. The faction's core_power_system field references a power system by name, and regions are referenced by name. Both must exist before faction parsing resolves names to IDs.
- **Faction-Character Association is post-generation only:** Characters are created after worldview generation. The association is manual-only and happens in a separate UI context (character management or faction detail page). This is explicitly scoped in PROJECT.md.
- **fillForces() enhances all downstream AI features:** Every strategy that reads worldview (ChapterGenerationTaskStrategy, OutlineTaskStrategy, ChapterFixTaskStrategy, VolumeOptimizeTaskStrategy, PromptTemplateBuilder, PromptContextBuilder) will benefit. The forces field becomes transient and is populated from structured tables, giving the AI richer context.
- **Frontend Faction Relations UI conflicts with Geography Tree timing:** Both components render in the same worldview management page. If both are expanded simultaneously, the page could get crowded. Consider collapsible sections or tab-based layout.

## MVP Definition

### Launch With (v1)

The minimum set that makes the faction system structurally equivalent to the geography system and provides AI-usable data.

- [ ] **Faction table with tree CRUD** -- The foundational data structure. Without this, nothing else works. Mirrors NovelContinentRegion exactly.
- [ ] **Type and power system fields on root factions with inheritance** -- Required for AI to reason about faction alignment and capabilities. Low implementation cost since it follows the same pattern.
- [ ] **AI prompt template update for structured faction XML** -- Without this, the AI still outputs flat text for factions, making the structured tables useless. Must change the <f> section in the prompt template (id=3) to output structured XML with faction hierarchy, type, power system name, and region names.
- [ ] **DOM parsing of faction XML in WorldviewTaskStrategy** -- Analogous to saveGeographyRegionsFromXml. Parse the new <f> XML structure, resolve names to IDs, insert into faction tables.
- [ ] **fillForces() method and transient field** -- Every prompt builder needs this. The existing `worldview.getForces()` calls must be replaced with dynamically-built text from the faction tree.
- [ ] **FactionTree.vue component** -- Users must be able to see and edit the faction hierarchy. Mirrors GeographyTree.vue with added type badge and power system label.
- [ ] **SQL migration script** -- Create tables, remove forces column from novel_worldview.

### Add After Validation (v1.x)

Features that complete the system but are not blocking for initial structural value.

- [ ] **Faction-Faction relationship management** -- Add after the basic tree works. The relation table is independent and can be layered on without schema changes to the faction table itself. Trigger: once users have 5+ factions, they need relationship tracking.
- [ ] **Faction-Character manual association** -- Add after the faction tree is stable. Requires character picker UI. Trigger: users start asking "how do I link characters to factions?"
- [ ] **Faction-Region association** -- Can be deferred initially since region info is already in the AI prompt via the geography section. Trigger: users want to filter factions by region or see which regions are contested.

### Future Consideration (v2+)

Features that would be valuable but are explicitly out of scope for this refactoring milestone.

- [ ] **Visual relationship graph** -- Only if user demand is strong. Consider integrating a lightweight graph library (e.g., vis-network or d3-force) rather than building from scratch.
- [ ] **Faction templates per genre** -- Pre-built faction archetypes for different novel types (xianxia sects, fantasy guilds, sci-fi corporations). Defer because genre-specific features need their own research cycle.
- [ ] **Faction event log** -- Requires timeline structuring first. Defer to the timeline refactor milestone.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Faction table + tree CRUD | HIGH | MEDIUM | P1 |
| Type classification (ally/enemy/neutral) | HIGH | LOW | P1 |
| Power system association | HIGH | LOW | P1 |
| AI prompt template update | HIGH | MEDIUM | P1 |
| DOM parsing + name-to-ID resolution | HIGH | MEDIUM | P1 |
| fillForces() transient field | HIGH | LOW | P1 |
| FactionTree.vue | HIGH | MEDIUM | P1 |
| SQL migration | HIGH | LOW | P1 |
| Type/power-system inheritance | MEDIUM | LOW | P1 |
| Faction-Faction relationships | MEDIUM | MEDIUM | P2 |
| Faction-Character association | MEDIUM | MEDIUM | P2 |
| Faction-Region association | MEDIUM | LOW | P2 |
| Visual relationship graph | LOW | HIGH | P3 |
| Faction templates per genre | LOW | MEDIUM | P3 |
| Faction event log | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for launch -- the faction system is useless without these
- P2: Should have, add when possible -- completes the system for serious users
- P3: Nice to have, future consideration -- speculative features

## Competitor Feature Analysis

| Feature | World Anvil | Kanka | Novelcrafter | AI Factory (Our Approach) |
|---------|-------------|-------|--------------|---------------------------|
| Faction hierarchy (tree) | Yes (nested orgs) | Yes (parent/child orgs) | Flat codex entries | Tree table with parent_id + deep, same as geography |
| Faction-member linking | Yes (Members tab with roles) | Yes (Members with roles + status) | Manual codex cross-references | novel_faction_character with role field, manual-only |
| Faction-faction relations | Yes (Diplomacy Webs, premium) | Yes (Relations system) | Affinity scores (-10 to 10) | novel_faction_relation with type + description |
| Faction-region mapping | Yes (Location links) | Yes (Location entity linking) | Manual codex entries | novel_faction_region many-to-many |
| AI auto-generation | No (manual entry only) | No (manual entry only) | Partial (AI can reference codex) | Full AI generation with DOM parsing into structured tables |
| AI prompt integration | N/A (TTRPG tool) | N/A (TTRPG tool) | Codex auto-injected into AI context | fillForces() builds text from structured data for all AI strategies |
| Visual relationship graph | Yes (Diplomacy Webs) | No | No | Deferred (P3) |
| Power system per faction | No (TTRPG focus) | No (custom attributes) | No | Direct FK to novel_power_system -- unique to our domain |

**Key differentiator:** AI Factory is the only tool that auto-generates structured faction data from AI and feeds it back into AI chapter generation. World Anvil and Kanka are manual TTRPG tools. Novelcrafter has AI integration but uses flat codex entries. Our structured approach with tree hierarchy + typed relations + power system links gives the AI richer, more consistent context.

## Sources

- **Codebase analysis:** NovelContinentRegion entity, ContinentRegionServiceImpl, GeographyTree.vue, WorldviewTaskStrategy, PromptTemplateBuilder, WorldSettingXmlDto, NovelWorldview entity, NovelCharacter entity
- **World Anvil:** [Diplomacy Webs Feature Guide](https://www.worldanvil.com/learn/diplomacy-webs/diplomacy-webs), [Organization Template Guide](https://www.worldanvil.com/learn/article-templates/organization)
- **Kanka:** [Worldbuilding Software for Worldbuilders](https://kanka.io/use-cases/worldbuilders) -- organization/faction features based on platform documentation and community analysis
- **Novelcrafter:** [Codex Features](https://www.novelcrafter.com/features/codex), [Codex Relations](https://www.novelcrafter.com/courses/codex-cookbook/codex-relationships), [Papercut Post Novelcrafter Review](https://www.papercutpost.com/writing-a-novel-in-novelcrafter-part-5/) -- faction affinity scoring and codex integration patterns
- **PROJECT.md:** Explicit scope boundaries (no timeline, no rules, no AI character linking, no map visualization)

---
*Feature research for: structured faction/force system in AI novel generation*
*Researched: 2026-04-01*
