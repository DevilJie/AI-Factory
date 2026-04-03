# Milestones

## v1.0.2 势力阵营结构化重构 (Shipped: 2026-04-02)

**Phases completed:** 5 phases, 12 plans, 18 tasks

**Key accomplishments:**

- 4-table DDL migration script (novel_faction tree + 3 association tables) and NovelFaction tree entity with MyBatis-Plus BaseMapper
- 3 association table entities (FactionRegion, FactionCharacter, FactionRelation) with mappers, plus NovelWorldview.forces marked transient
- FactionService with tree CRUD, 4-table cascading delete, type/corePowerSystem root inheritance, and fillForces text builder for prompt construction
- REST controller with 14 endpoints: 5 faction tree CRUD + 3x3 association table management using direct mapper injection
- SQL migration replacing plain-text faction CDATA with structured XML format in worldview generation prompt, enforcing name consistency for power systems and regions
- saveFactionsFromXml() with two-pass insert, three-tier name matching, and DOM parsing using getChildNodes() exclusively for nested faction XML ingestion
- Migrated all 8 files (11 call sites) to call factionService.fillForces(worldview) before worldview.getForces(), bridging structured faction tables to prompt text generation
- Faction API client (4 CRUD functions) and recursive FactionTree component with type badges, power system labels, inline editing, and add/delete capabilities
- Replaced forces textarea with FactionTree component in WorldSetting.vue, adding AI generation auto-refresh in both generation paths
- Fixed recursive FactionTree so add/edit/delete work at all depths via child-instance-local state with refresh emit pattern

---
