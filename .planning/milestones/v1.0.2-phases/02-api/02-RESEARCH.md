# Phase 2: 后端服务与 API - Research

**Researched:** 2026-04-02
**Domain:** Spring Boot 3.2 + MyBatis-Plus tree CRUD, association table management, text builder pattern
**Confidence:** HIGH

## Summary

Phase 2 implements the complete backend CRUD lifecycle for the faction (势力) entity, including tree queries with type/corePowerSystem inheritance, cascading deletes across 4 tables, association table management APIs (faction-region, faction-character, faction-faction relation), and the `fillForces()` text builder that reconstructs faction tree data into a prompt-consumable text string.

The implementation directly mirrors the already-validated `ContinentRegionService` / `ContinentRegionServiceImpl` / `ContinentRegionController` pattern. All Phase 1 assets are in place: the 4 entity classes (NovelFaction, NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation), the 4 BaseMapper interfaces, the SQL migration script, and the `NovelWorldview.forces` field marked as `@TableField(exist = false)`. No new external dependencies are needed.

**Primary recommendation:** Clone ContinentRegionServiceImpl as the FactionService skeleton, add the 3 association mappers for cascading deletes and association CRUD, then build `fillForces()` by extending `buildGeographyText()` with type labels, power system name resolution, and region name lookups.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** FactionService single class manages faction tree CRUD + fillForces(), following ContinentRegionService pattern
- **D-02:** Clone ContinentRegionService tree CRUD pattern (getTreeByProjectId, cascading delete, buildGeographyText, fillGeography) directly for faction entity
- **D-03:** fillForces() output format: hierarchical indented format with type labels, power system names, region references, color tags
- **D-04:** Only top-level factions carry type + core_power_system info
- **D-05:** Single-layer flat output (no deep hierarchy), one line per item
- **D-06:** Trailing summary of sub-factions under each top-level faction (indented display)
- **D-07:** Single FactionController (faction CRUD + 3 association management endpoints), RESTful, route prefix `/api/novel/faction/*`
- **D-08:** Association CRUD endpoints (faction-region, faction-character, faction-relation) embedded in FactionController, sharing FactionService instance
- **D-09:** deleteByProjectId cleans all 4 tables
- **D-10:** Application-level recursive delete: collect all descendant IDs -> delete associations -> delete factions, within transaction
- **D-11:** All operations in a single @Transactional method for data consistency
- **D-12:** Frontend confirmation dialog before cascading delete (frontend responsibility, backend just executes)
- **Phase 1 decision:** No database foreign key constraints, pure logical associations, consistent with ContinentRegion pattern

### Claude's Discretion
- DTO class field details and structure
- Transaction annotation usage specifics
- Abstract class design (if any)
- Exception handling and error response details
- Logging levels
- Method naming conventions (keep consistent with ContinentRegionService)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BACK-01 | FactionService interface and implementation, following ContinentRegionService pattern, supporting tree CRUD | ContinentRegionServiceImpl is a 280-line verified reference; clone and extend with association mappers |
| BACK-02 | Tree query: getTreeByProjectId() returns tree with children | buildTree() pattern from ContinentRegionServiceImpl lines 174-190, plus type/corePowerSystem inheritance logic |
| BACK-03 | Cascading delete: recursive delete of children and all association data | ContinentRegionServiceImpl.collectDescendantIds() pattern + delete from 3 association tables before deleting factions |
| BACK-04 | deleteByProjectId() central cleanup of all faction data for a project (4 tables) | LambdaQueryWrapper delete on novel_faction + 3 association tables |
| BACK-05 | fillForces(NovelWorldview) method, builds text description from faction tree for PromptBuilder | buildGeographyText() pattern extended with type labels, power system name lookup via PowerSystemService, region name lookup via ContinentRegionService |
| BACK-06 | type/corePowerSystem inheritance: child factions auto-fill values from root ancestor | Tree traversal: find root ancestor for each node, propagate type and corePowerSystem values |
| BACK-07 | FactionController REST API (CRUD endpoints) | ContinentRegionController pattern with Result<T> wrapper, Swagger annotations |
| BACK-08 | Faction-region association CRUD API | Embedded in FactionController, uses NovelFactionRegionMapper |
| BACK-09 | Faction-character association CRUD API (includes role field) | Embedded in FactionController, uses NovelFactionCharacterMapper |
| BACK-10 | Faction-faction relation CRUD API | Embedded in FactionController, uses NovelFactionRelationMapper |
</phase_requirements>

## Standard Stack

### Core (all pre-existing in project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.0 | REST API framework | Project standard, all controllers use it |
| MyBatis-Plus | 3.5.5 | ORM for all database operations | Project standard, BaseMapper pattern used everywhere |
| Lombok | bundled | @Data, @Slf4j | All entities and services use it |
| SpringDoc OpenAPI | 2.3.0 | Swagger annotations (@Tag, @Operation) | All controllers use @Tag and @Operation |

### Supporting (pre-existing, needed by FactionService)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| PowerSystemService | (internal) | Resolve corePowerSystem ID to name | fillForces() needs to show power system name in text output |
| ContinentRegionService | (internal) | Resolve regionId to region name | fillForces() needs to show region names for each faction |
| NovelContinentRegionMapper | (internal) | Query region names for faction-region associations | fillForces() and association endpoints |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Single FactionController | Separate controllers per association | Separate controllers add file count with no benefit; D-07 locks single controller |

**Installation:** No new packages needed. All dependencies are pre-existing from Phase 1 and the project's existing stack.

## Architecture Patterns

### Recommended Project Structure
```
ai-factory-backend/src/main/java/com/aifactory/
├── controller/
│   └── FactionController.java          # NEW: REST API for faction + associations
├── service/
│   └── FactionService.java             # NEW: Interface
├── service/impl/
│   └── FactionServiceImpl.java         # NEW: Implementation
├── entity/
│   ├── NovelFaction.java               # EXISTS (Phase 1)
│   ├── NovelFactionRegion.java         # EXISTS (Phase 1)
│   ├── NovelFactionCharacter.java      # EXISTS (Phase 1)
│   └── NovelFactionRelation.java       # EXISTS (Phase 1)
└── mapper/
    ├── NovelFactionMapper.java         # EXISTS (Phase 1)
    ├── NovelFactionRegionMapper.java   # EXISTS (Phase 1)
    ├── NovelFactionCharacterMapper.java # EXISTS (Phase 1)
    └── NovelFactionRelationMapper.java # EXISTS (Phase 1)
```

### Pattern 1: Tree CRUD Service (clone from ContinentRegionServiceImpl)
**What:** Flat list query -> buildTree() -> recursive tree construction. AddRegion auto-calculates deep and sortOrder. Delete collects all descendant IDs then batch-deletes.
**When to use:** All tree entities in this project.
**Example (from ContinentRegionServiceImpl lines 174-190):**
```java
private List<NovelContinentRegion> buildTree(List<NovelContinentRegion> allRegions) {
    if (allRegions == null || allRegions.isEmpty()) {
        return new ArrayList<>();
    }
    Map<Long, List<NovelContinentRegion>> childrenMap = allRegions.stream()
        .filter(r -> r.getParentId() != null)
        .collect(Collectors.groupingBy(NovelContinentRegion::getParentId));
    for (NovelContinentRegion region : allRegions) {
        region.setChildren(childrenMap.get(region.getId()));
    }
    return allRegions.stream()
        .filter(r -> r.getParentId() == null)
        .collect(Collectors.toList());
}
```

### Pattern 2: Cascading Delete with Associations
**What:** When deleting a faction node, collect all descendant IDs, then delete from association tables first, then delete the faction records themselves.
**When to use:** Faction delete (BACK-03) and project-level cleanup (BACK-04).
**Example:**
```java
@Override
@Transactional
public void deleteFaction(Long id) {
    List<Long> idsToDelete = collectDescendantIds(id);
    idsToDelete.add(id);

    // Delete associations first (no FK constraints, must be explicit)
    factionRegionMapper.delete(new LambdaQueryWrapper<NovelFactionRegion>()
        .in(NovelFactionRegion::getFactionId, idsToDelete));
    factionCharacterMapper.delete(new LambdaQueryWrapper<NovelFactionCharacter>()
        .in(NovelFactionCharacter::getFactionId, idsToDelete));
    factionRelationMapper.delete(new LambdaQueryWrapper<NovelFactionRelation>()
        .in(NovelFactionRelation::getFactionId, idsToDelete)
        .or()
        .in(NovelFactionRelation::getTargetFactionId, idsToDelete));
    // Then delete factions
    factionMapper.deleteBatchIds(idsToDelete);
}
```

### Pattern 3: fillForces() Text Builder
**What:** Build hierarchical text from faction tree, resolving type labels, power system names, and region names.
**When to use:** fillForces() (BACK-05) and buildFactionText() methods.
**Example:**
```java
private void buildFactionTextRecursive(List<NovelFaction> nodes, int level,
    String rootType, String rootPowerSystemName,
    StringBuilder sb) {
    if (nodes == null) return;
    String indent = "  ".repeat(level);
    for (NovelFaction node : nodes) {
        // Root-level: type + power system name
        if (level == 0) {
            String typeLabel = formatTypeLabel(node.getType()); // e.g., "正派"
            sb.append("【").append(typeLabel).append("】 ");
            rootType = node.getType();
            rootPowerSystemName = resolvePowerSystemName(node.getCorePowerSystem());
        }
        sb.append(indent).append(node.getName());
        if (rootPowerSystemName != null && level == 0) {
            sb.append("（力量体系：").append(rootPowerSystemName).append("）");
        }
        if (node.getDescription() != null && !node.getDescription().isEmpty()) {
            sb.append("：").append(node.getDescription());
        }
        // Append associated regions for this faction
        appendRegions(node.getId(), sb);
        sb.append("\n");
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            buildFactionTextRecursive(node.getChildren(), level + 1,
                rootType, rootPowerSystemName, sb);
        }
    }
}
```

### Pattern 4: Type/corePowerSystem Inheritance
**What:** When building the tree, propagate root-level type and corePowerSystem to all children.
**When to use:** getTreeByProjectId() (BACK-06) -- child factions should display inherited values.
**Implementation:** After building the tree, traverse it and set inherited values on children. Only root nodes (parentId == null) have type and corePowerSystem set in the database. Children get these from their root ancestor at query time.

### Pattern 5: REST Controller with Result<T> Wrapper
**What:** All endpoints return `Result<T>`, use try-catch with log.error, and Swagger annotations.
**When to use:** FactionController (BACK-07 through BACK-10).
**Example (from ContinentRegionController):**
```java
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/faction")
@Tag(name = "势力管理", description = "势力树形结构管理API")
public class FactionController {
    @Autowired
    private FactionService factionService;

    @Operation(summary = "获取势力树")
    @GetMapping("/tree")
    public Result<List<NovelFaction>> getTree(@PathVariable Long projectId) {
        try {
            return Result.ok(factionService.getTreeByProjectId(projectId));
        } catch (Exception e) {
            log.error("获取势力树失败，projectId={}", projectId, e);
            return Result.error("获取势力树失败: " + e.getMessage());
        }
    }
    // ... association endpoints
}
```

### Anti-Patterns to Avoid
- **DB-level cascade deletes:** Project explicitly avoids FK constraints (Phase 1 decision). All cascading must be application-level.
- **N+1 queries in fillForces():** Do NOT query region names per-faction inside the recursive text builder. Batch-fetch all region IDs upfront or accept N small queries (acceptable for small datasets).
- **Setting type/corePowerSystem on non-root nodes in the database:** Only root nodes store these values. Children inherit at query time.
- **Forgetting to delete faction-relation entries where the faction is the TARGET:** The relation table stores bidirectional pairs. When deleting a faction, check both `faction_id` and `target_faction_id` columns.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Tree building from flat list | Custom tree builder | ContinentRegionServiceImpl.buildTree() pattern | Proven pattern, handles null parentId correctly |
| Auto-calculating tree depth | Manual deep tracking | Parent lookup + deep = parent.deep + 1 pattern | Same as addRegion() in ContinentRegionServiceImpl |
| Sort order management | Custom sorting logic | getMaxSortOrder() pattern from ContinentRegionServiceImpl | Handles same-level ordering correctly |
| Descendant ID collection | Custom recursive SQL | collectDescendantIds() from ContinentRegionServiceImpl | Simple recursive in-memory approach, proven |
| API response wrapping | Custom response format | Result<T> static factory methods | Project standard, consistent across all controllers |

**Key insight:** The ContinentRegionService pattern has been running in production for geography data. It handles all edge cases (null parent, orphan nodes, sort order gaps). Cloning it is safer and faster than building from scratch.

## Common Pitfalls

### Pitfall 1: Missing Association Table Cleanup in Cascading Delete
**What goes wrong:** Deleting a faction but forgetting to delete its rows from novel_faction_region, novel_faction_character, or novel_faction_relation, leaving orphan records.
**Why it happens:** No FK constraints to enforce cleanup. Developer only deletes from novel_faction table.
**How to avoid:** Always delete associations BEFORE deleting faction records. For faction_relation, check BOTH faction_id and target_faction_id columns.
**Warning signs:** Orphan rows in association tables after deleting a parent faction.

### Pitfall 2: Type/corePowerSystem Not Inherited to Children
**What goes wrong:** Tree nodes show null type and null corePowerSystem for non-root factions, causing fillForces() to produce incomplete output.
**Why it happens:** Developer queries the flat list and builds the tree without propagating root values to descendants.
**How to avoid:** After buildTree(), traverse root nodes and propagate type/corePowerSystem down the tree. This is a post-build step, not a database operation.
**Warning signs:** Child factions showing "null" type in API responses or text output.

### Pitfall 3: Faction-Relation Bidirectional Cleanup
**What goes wrong:** Deleting faction A only deletes rows where faction_id = A, but misses rows where target_faction_id = A.
**Why it happens:** The relation table stores each relationship twice (bidirectional per D-06 from Phase 1). A query filtering only faction_id misses the reverse direction.
**How to avoid:** Use `.or().in(NovelFactionRelation::getTargetFactionId, idsToDelete)` in the delete wrapper.
**Warning signs:** Orphan relation rows after faction deletion.

### Pitfall 4: fillForces() N+1 Query for Region Names
**What goes wrong:** Inside the recursive text builder, querying region names one faction at a time causes O(n) database queries.
**Why it happens:** Naive approach of querying `SELECT name FROM novel_continent_region WHERE id IN (...)` per faction inside the recursive loop.
**How to avoid:** Pre-fetch all faction-region associations for the project, then group by factionId. Join with region names upfront. For the expected dataset size (< 100 factions), this is acceptable even with per-faction queries, but pre-fetching is cleaner.
**Warning signs:** Slow API response when filling forces for projects with many factions.

### Pitfall 5: deleteByProjectId Missing Association Tables
**What goes wrong:** deleteByProjectId only deletes from novel_faction but leaves orphan rows in the 3 association tables.
**Why it happens:** Developer mirrors ContinentRegionServiceImpl.deleteByProjectId() which only has one table to worry about.
**How to avoid:** Delete from all 4 tables in the correct order: faction_region, faction_character, faction_relation (both directions), then faction.
**Warning signs:** Leftover association data after project cleanup, causing foreign reference errors on re-import.

### Pitfall 6: Controller Route Prefix Mismatch
**What goes wrong:** Using `/api/novel/faction/{projectId}/...` instead of `/api/novel/{projectId}/faction/...`.
**Why it happens:** Inconsistent interpretation of the route pattern.
**How to avoid:** Follow the ContinentRegionController pattern exactly: `@RequestMapping("/api/novel/{projectId}/faction")`.
**Warning signs:** Frontend 404 errors when calling the API.

## Code Examples

### FactionService Interface (BACK-01)
```java
// Source: Pattern from ContinentRegionService.java
package com.aifactory.service;

import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelWorldview;
import java.util.List;

public interface FactionService {
    List<NovelFaction> getTreeByProjectId(Long projectId);
    List<NovelFaction> listByProjectId(Long projectId);
    NovelFaction addFaction(NovelFaction faction);
    NovelFaction updateFaction(NovelFaction faction);
    void deleteFaction(Long id);
    void saveTree(Long projectId, List<NovelFaction> rootNodes);
    void deleteByProjectId(Long projectId);
    String buildFactionText(Long projectId);
    void fillForces(NovelWorldview worldview);
}
```

### Cascading Delete with 4 Tables (BACK-03)
```java
// Source: Extended from ContinentRegionServiceImpl.deleteRegion() pattern
@Override
@Transactional
public void deleteFaction(Long id) {
    List<Long> idsToDelete = collectDescendantIds(id);
    idsToDelete.add(id);

    // 1. Delete faction-region associations
    factionRegionMapper.delete(new LambdaQueryWrapper<NovelFactionRegion>()
        .in(NovelFactionRegion::getFactionId, idsToDelete));

    // 2. Delete faction-character associations
    factionCharacterMapper.delete(new LambdaQueryWrapper<NovelFactionCharacter>()
        .in(NovelFactionCharacter::getFactionId, idsToDelete));

    // 3. Delete faction-faction relations (BOTH directions)
    factionRelationMapper.delete(new LambdaQueryWrapper<NovelFactionRelation>()
        .in(NovelFactionRelation::getFactionId, idsToDelete)
        .or()
        .in(NovelFactionRelation::getTargetFactionId, idsToDelete));

    // 4. Delete faction records
    factionMapper.deleteBatchIds(idsToDelete);

    log.info("删除势力及子节点，共删除{}条势力记录", idsToDelete.size());
}
```

### deleteByProjectId Cleaning 4 Tables (BACK-04)
```java
// Source: Extended from ContinentRegionServiceImpl.deleteByProjectId()
@Override
@Transactional
public void deleteByProjectId(Long projectId) {
    // Collect all faction IDs for this project
    List<NovelFaction> factions = factionMapper.selectList(
        new LambdaQueryWrapper<NovelFaction>()
            .eq(NovelFaction::getProjectId, projectId)
            .select(NovelFaction::getId)
    );
    List<Long> factionIds = factions.stream().map(NovelFaction::getId).toList();

    if (!factionIds.isEmpty()) {
        // Delete associations first
        factionRegionMapper.delete(new LambdaQueryWrapper<NovelFactionRegion>()
            .in(NovelFactionRegion::getFactionId, factionIds));
        factionCharacterMapper.delete(new LambdaQueryWrapper<NovelFactionCharacter>()
            .in(NovelFactionCharacter::getFactionId, factionIds));
        factionRelationMapper.delete(new LambdaQueryWrapper<NovelFactionRelation>()
            .in(NovelFactionRelation::getFactionId, factionIds)
            .or()
            .in(NovelFactionRelation::getTargetFactionId, factionIds));
    }

    // Then delete all factions
    factionMapper.delete(
        new LambdaQueryWrapper<NovelFaction>()
            .eq(NovelFaction::getProjectId, projectId)
    );
    log.info("已删除项目所有势力数据，projectId={}", projectId);
}
```

### fillForces() Method (BACK-05)
```java
// Source: Extended from ContinentRegionServiceImpl.buildGeographyText() + fillGeography()
@Override
public void fillForces(NovelWorldview worldview) {
    if (worldview == null || worldview.getProjectId() == null) return;
    String text = buildFactionText(worldview.getProjectId());
    worldview.setForces(text.isEmpty() ? null : text);
}

@Override
public String buildFactionText(Long projectId) {
    List<NovelFaction> tree = getTreeByProjectId(projectId);
    if (tree == null || tree.isEmpty()) {
        return "";
    }
    StringBuilder sb = new StringBuilder();
    buildFactionTextRecursive(tree, 0, null, null, sb);
    return sb.toString().trim();
}

private void buildFactionTextRecursive(List<NovelFaction> nodes, int level,
    String rootType, String rootPowerSystemName, StringBuilder sb) {
    if (nodes == null) return;
    String indent = "  ".repeat(level);
    for (NovelFaction node : nodes) {
        if (level == 0) {
            // Root node: show type label and power system
            String typeLabel = formatTypeLabel(node.getType());
            sb.append("【").append(typeLabel).append("】 ");
            rootType = node.getType();
            rootPowerSystemName = resolvePowerSystemName(node.getCorePowerSystem());
        }
        sb.append(indent).append(node.getName());
        if (level == 0 && rootPowerSystemName != null) {
            sb.append("（力量体系：").append(rootPowerSystemName).append("）");
        }
        if (node.getDescription() != null && !node.getDescription().isEmpty()) {
            sb.append("：").append(node.getDescription());
        }
        // Append regions associated with this faction
        String regions = getRegionNamesForFaction(node.getId());
        if (regions != null && !regions.isEmpty()) {
            sb.append(" | 地区：").append(regions);
        }
        sb.append("\n");
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            buildFactionTextRecursive(node.getChildren(), level + 1,
                rootType, rootPowerSystemName, sb);
        }
    }
}
```

### Type/corePowerSystem Inheritance (BACK-06)
```java
// Post-tree-build inheritance step
private void inheritRootValues(List<NovelFaction> tree) {
    if (tree == null) return;
    for (NovelFaction root : tree) {
        String rootType = root.getType();
        Long rootPowerSystem = root.getCorePowerSystem();
        propagateToChildren(root.getChildren(), rootType, rootPowerSystem);
    }
}

private void propagateToChildren(List<NovelFaction> children, String type, Long corePowerSystem) {
    if (children == null) return;
    for (NovelFaction child : children) {
        child.setType(type);
        child.setCorePowerSystem(corePowerSystem);
        propagateToChildren(child.getChildren(), type, corePowerSystem);
    }
}
```

### FactionController Association Endpoints (BACK-08, BACK-09, BACK-10)
```java
// Faction-Region associations
@GetMapping("/{factionId}/regions")
public Result<List<NovelFactionRegion>> listRegions(@PathVariable Long projectId,
                                                    @PathVariable Long factionId) {
    try {
        return Result.ok(factionService.listRegionsByFactionId(factionId));
    } catch (Exception e) {
        log.error("获取势力地区关联失败，factionId={}", factionId, e);
        return Result.error("获取势力地区关联失败: " + e.getMessage());
    }
}

@PostMapping("/{factionId}/regions")
public Result<NovelFactionRegion> addRegion(@PathVariable Long projectId,
                                            @PathVariable Long factionId,
                                            @RequestBody NovelFactionRegion association) {
    try {
        association.setFactionId(factionId);
        return Result.ok(factionService.addFactionRegion(association));
    } catch (Exception e) {
        log.error("添加势力地区关联失败，factionId={}", factionId, e);
        return Result.error("添加势力地区关联失败: " + e.getMessage());
    }
}

@DeleteMapping("/{factionId}/regions/{id}")
public Result<Void> deleteRegion(@PathVariable Long projectId,
                                 @PathVariable Long factionId,
                                 @PathVariable Long id) {
    try {
        factionService.deleteFactionRegion(id);
        return Result.ok();
    } catch (Exception e) {
        log.error("删除势力地区关联失败，id={}", id, e);
        return Result.error("删除势力地区关联失败: " + e.getMessage());
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NovelWorldview.forces as DB text column | @TableField(exist=false) + fillForces() from structured tables | Phase 1 (2026-04-01) | forces column kept for backward compat but no longer written to |
| Single geography table | Separate tree tables per domain | Earlier refactor | ContinentRegion pattern proven, now applying to factions |

**Deprecated/outdated:**
- Direct worldview.getForces() reading from DB: Now must call fillForces() first. All 7+ call sites identified in REQUIREMENTS.md Phase 3 (PROMPT-01 through PROMPT-06).

## Open Questions

1. **Power system name resolution in fillForces()**
   - What we know: NovelFaction.corePowerSystem stores the ID of a NovelPowerSystem record. PowerSystemService.getById(id) returns the entity with name.
   - What's unclear: Whether PowerSystemService.getById() exists or only listByProjectId() does. Grep confirms it exists in the interface (line 3 of PowerSystemService.java: `NovelPowerSystem getById(Long id)`).
   - Recommendation: Use PowerSystemService.getById() to resolve the name. LOW risk -- method exists in the interface.

2. **Region name resolution in fillForces()**
   - What we know: NovelFactionRegion stores regionId which references novel_continent_region.id. NovelContinentRegionMapper.selectById(regionId) returns the entity with name.
   - What's unclear: Whether to inject NovelContinentRegionMapper directly or go through ContinentRegionService.
   - Recommendation: Inject NovelContinentRegionMapper directly for batch lookups. Simpler than routing through the service.

3. **Association CRUD: separate DTOs or reuse entities?**
   - What we know: CONTEXT.md marks DTOs as Claude's discretion. Current ContinentRegionController reuses the entity directly in request/response.
   - Recommendation: Reuse NovelFactionRegion, NovelFactionCharacter, NovelFactionRelation entities directly, consistent with existing pattern. No DTOs needed for simple association tables.

## Environment Availability

Step 2.6: Environment audit for this phase.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | Backend compilation | Yes | 21.0.10 | -- |
| Maven 3.9+ | Backend build | Yes | 3.9.14 | -- |
| MySQL 8.0+ | Database | Yes (configured) | -- | -- |
| Spring Boot 3.2 | Application framework | Yes (in pom.xml) | 3.2.0 | -- |
| MyBatis-Plus 3.5.5 | ORM | Yes (in pom.xml) | 3.5.5 | -- |

**Missing dependencies with no fallback:** None

**Missing dependencies with fallback:** None

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + Spring Boot Test |
| Config file | pom.xml (spring-boot-starter-test dependency) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=FactionServiceTest -q` |
| Full suite command | `cd ai-factory-backend && mvn test -q` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BACK-01 | FactionService CRUD interface | unit | `mvn test -Dtest=FactionServiceTest#testAddFaction -q` | Wave 0 |
| BACK-02 | getTreeByProjectId returns tree with children | unit | `mvn test -Dtest=FactionServiceTest#testGetTreeByProjectId -q` | Wave 0 |
| BACK-03 | Cascading delete removes children + associations | unit | `mvn test -Dtest=FactionServiceTest#testCascadingDelete -q` | Wave 0 |
| BACK-04 | deleteByProjectId cleans 4 tables | unit | `mvn test -Dtest=FactionServiceTest#testDeleteByProjectId -q` | Wave 0 |
| BACK-05 | fillForces builds text from tree | unit | `mvn test -Dtest=FactionServiceTest#testFillForces -q` | Wave 0 |
| BACK-06 | Type/corePowerSystem inherited from root | unit | `mvn test -Dtest=FactionServiceTest#testTypeInheritance -q` | Wave 0 |
| BACK-07 | FactionController REST endpoints return Result<T> | unit | `mvn test -Dtest=FactionControllerTest -q` | Wave 0 |
| BACK-08 | Faction-region CRUD API | unit | `mvn test -Dtest=FactionControllerTest#testRegionCRUD -q` | Wave 0 |
| BACK-09 | Faction-character CRUD API (with role) | unit | `mvn test -Dtest=FactionControllerTest#testCharacterCRUD -q` | Wave 0 |
| BACK-10 | Faction-relation CRUD API | unit | `mvn test -Dtest=FactionControllerTest#testRelationCRUD -q` | Wave 0 |

### Sampling Rate
- **Per task commit:** `cd ai-factory-backend && mvn test -Dtest=FactionServiceTest -q`
- **Per wave merge:** `cd ai-factory-backend && mvn test -q`
- **Phase gate:** Full suite green before verify-work

### Wave 0 Gaps
- [ ] `ai-factory-backend/src/test/java/com/aifactory/service/FactionServiceTest.java` -- covers BACK-01 through BACK-06
- [ ] `ai-factory-backend/src/test/java/com/aifactory/controller/FactionControllerTest.java` -- covers BACK-07 through BACK-10
- [ ] No test infrastructure gaps -- JUnit 5 + Mockito already in pom.xml via spring-boot-starter-test

## Sources

### Primary (HIGH confidence)
- `ContinentRegionServiceImpl.java` -- verified tree CRUD pattern (getTreeByProjectId, buildTree, collectDescendantIds, addRegion, deleteRegion, deleteByProjectId, buildGeographyText, fillGeography)
- `ContinentRegionController.java` -- verified REST controller pattern (Result<T>, @Tag, @Operation, try-catch)
- `NovelFaction.java` -- verified entity with type, corePowerSystem fields
- `NovelFactionRegion.java`, `NovelFactionCharacter.java`, `NovelFactionRelation.java` -- verified association entities
- `NovelFactionMapper.java` + 3 association mappers -- verified BaseMapper interfaces
- `Result.java` -- verified API response wrapper
- `PowerSystemService.java` -- verified getById() method exists for name resolution
- `PromptTemplateBuilder.java` -- verified buildWorldviewString() calls continentRegionService.fillGeography() and reads worldview.getForces()
- `faction_migration.sql` -- verified DDL for all 4 tables

### Secondary (MEDIUM confidence)
- `NovelWorldview.java` -- forces field already marked @TableField(exist=false) per Phase 1

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all dependencies pre-existing, verified by reading pom.xml and source files
- Architecture: HIGH -- ContinentRegionService pattern is a 1:1 reference, already production-validated
- Pitfalls: HIGH -- identified from careful analysis of the relation table's bidirectional storage and the 4-table cascading delete requirement

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable stack, no external dependency changes expected)
