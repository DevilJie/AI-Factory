# Milestones

## v1.0.6 伏笔管理 (Shipped: 2026-04-11)

**Phases completed:** 7 phases, 10 plans, 27 tasks

**Key accomplishments:**

- Foreshadowing constraint injection into chapter generation prompts with directive-style text, plus automatic status transitions (pending->in_progress->completed) via batch update in both streaming and synchronous generation paths
- Backend controller 8-param query bindings + frontend TypeScript types/API client for foreshadowing CRUD
- ChapterPlanDrawer 5th tab with color-coded foreshadowing cards in 待埋设/待回收/分卷参考 sections, modal CRUD with plantedChapter immutability
- 项目级伏笔总览页面 with card grid, type/layout/volume/status filters, CRUD modal, and health score dashboard

---

## v1.0.5 章节角色规划体系 (Shipped: 2026-04-10)

**Phases completed:** 6 phases, 10 plans, 20 tasks

**Key accomplishments:**

- Character detail drawer with 3 tabs (info/power system/faction) for association management, plus list cards showing cultivation realm and faction info
- Added planned_characters JSON column to novel_chapter_plan, mapped plannedCharacters and characterArcs in entity and DTO, deleted unused common.xml.ChapterPlanXmlDto duplicate
- Generic three-tier Chinese name matching utility (NameMatchUtil) with 22 passing tests, greedy suffix stripping, and NamedEntity interface for reuse in Phase 12 character name association
- DOM-based parseChaptersXml replacing Jackson XML, with <ch> character tag extraction, NameMatchUtil name resolution, and plannedCharacters JSON persistence
- Conditional planned character constraint injection in chapter prompt with fallback to full character list, constraint language wrapping, and 6 TDD tests
- REST endpoint and frontend API for fetching actual characters that appeared in a generated chapter, enabling plan-vs-actual comparison
- Collapsible comparison region with green/red/amber markers for planned vs actual characters, plus ExternalLink icon opening CharacterDrawer from ChapterPlanDrawer

---

## v1.0.4 角色体系关联与提取增强 (Shipped: 2026-04-06)

**Phases completed:** 1 phase, 3 plans

**Key accomplishments:**

- character_power_system 关联表 + 扩展 ChapterCharacterExtractXmlDto FC 标签 + 关联解析和 upsert 逻辑
- 角色提取提示词模板 v2：添加 roleType 四级定义（protagonist/antagonist/supporting/minor）、势力标签 FC、roleType 分配逻辑
- CharacterDrawer 角色详情抽屉：4 个 Tabs（基本信息、力量体系、势力、章节关联），卡片展示角色关联聚合数据

---

## v1.0.3 世界观生成任务拆分 (Shipped: 2026-04-04)

**Phases completed:** 4 phases, 6 plans, 11 tasks

**Key accomplishments:**

- V4 Flyway migration creating 3 independent AI prompt templates (geography/power system/faction) by extracting from unified worldview template, plus simplifying the unified template to only output t/b/l/r fields
- FactionTaskStrategy with 3-step async pipeline (clean/generate/save) plus 3 REST endpoints for independent geography, power system, and faction generation with dependency validation
- Extracted ~400 lines of duplicated DOM parsing into WorldviewXmlParser utility, refactored GeographyTaskStrategy and FactionTaskStrategy to delegate all XML parsing and name matching
- Rewrote WorldviewTaskStrategy from 920-line monolith to ~250-line 9-step orchestrator delegating to GeographyTaskStrategy, PowerSystemTaskStrategy, FactionTaskStrategy with dependency context injection and 14 unit tests
- 3 independent AI generation buttons with mutual exclusion, localStorage recovery, and polling for Geography/PowerSystem/Faction modules

---

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
