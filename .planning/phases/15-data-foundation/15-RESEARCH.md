# Phase 15: Data Foundation - Research

**Researched:** 2026-04-10
**Domain:** Database schema extension + field deletion + distance validation for foreshadowing (伏笔)
**Confidence:** HIGH

## Summary

Phase 15 extends the existing `novel_foreshadowing` table with two volume-number columns (`planted_volume`, `planned_callback_volume`), completely removes the redundant `foreshadowingSetup`/`foreshadowingPayoff` text fields from `novel_chapter_plan` across all layers (DB, Java entity, DTOs, frontend types, frontend components), and adds a distance validation rule that prevents "instant callback" foreshadowing (minimum 3 chapters between planting and callback for bright-line foreshadowing, dark-line exempt).

The work is entirely additive to existing patterns. The `Foreshadowing` entity, `ForeshadowingService`, `ForeshadowingController`, and all related DTOs already exist with full CRUD. The only external dependency is the `NovelVolumePlan` entity (for querying `targetChapterCount` to validate callback chapter bounds). No new libraries, no new services, no new mappers. The total surface area is approximately 14 files to modify plus 1 SQL migration.

**Primary recommendation:** Execute in three waves -- (1) schema + entity/DTO changes, (2) distance validation logic in service layer, (3) old field cleanup across all layers. This ordering ensures the database and Java model are consistent before validation logic is added, and cleanup is last to avoid partial states.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** plantedVolume + plantedChapter combination for planting point, plannedCallbackVolume + plannedCallbackChapter for callback point. Four Integer fields work together.
- **D-02:** Planting fields (plantedVolume + plantedChapter) allow null (no foreshadowing). If planting fields have values, callback fields (plannedCallbackVolume + plannedCallbackChapter) must not be null.
- **D-03:** plannedCallbackChapter cannot exceed that volume's planned chapter count. Validation requires querying novel_volume_plan.targetChapterCount.
- **D-04:** Chapter numbering uses global sequence (cross-volume incrementing), not per-volume reset. Distance calculation simplifies to abs(callback - planted) >= N.
- **D-05:** Existing test data can be fully cleaned. No old data migration needed.
- **D-06:** Default minimum interval is 3 chapters (global sequence: callbackChapter - plantedChapter >= 3).
- **D-07:** layoutType=dark (dark line) foreshadowing is exempt from distance validation -- dark lines naturally span volumes, may span the entire book.
- **D-08:** bright1/bright2/bright3 (bright line) foreshadowing must satisfy minimum interval requirement.
- **D-09:** Completely delete foreshadowingSetup/foreshadowingPayoff within Phase 15: DB ALTER TABLE DROP COLUMN (sync init.sql), Java entity/DTO deletion, frontend type/component deletion.
- **D-10:** No soft deprecation (no @TableField(exist=false) retention). Complete deletion.

### Claude's Discretion
- DB column data types, constraints (NOT NULL / DEFAULT values)
- Index strategy (whether composite index on plantedVolume + plantedChapter is needed)
- Distance validation error message text
- Global chapter sequence number generation/maintenance approach

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DATA-01 | novel_foreshadowing table add planted_volume and planned_callback_volume fields (Integer), supporting cross-volume foreshadowing references | Foreshadowing.java entity has plantedChapter/plannedCallbackChapter already. Adding plantedVolume/plannedCallbackVolume follows same Integer pattern. DDL: ALTER TABLE ADD COLUMN. MyBatis-Plus auto-maps new columns to entity fields. |
| DATA-02 | Completely delete novel_chapter_plan foreshadowingSetup/foreshadowingPayoff fields (DB columns + entity + DTO + frontend types) | 26 files reference these fields. After filtering for non-planning files: 10 source files need modification (3 Java backend, 5 frontend, 1 SQL, 1 init.sql). D-09/D-10 mandate complete deletion with no soft deprecation. |
| DATA-03 | Foreshadowing create/update validates minimum planting-callback distance (at least N chapters), preventing instant callback | ForeshadowingService.createForeshadowing() and updateForeshadowing() are the injection points. Validation logic: if layoutType != "dark" and both plantedChapter and plannedCallbackChapter are present, check abs(plannedCallbackChapter - plantedChapter) >= 3. NovelVolumePlanMapper (extends BaseMapper) provides access to targetChapterCount for D-03 bounds checking. |
</phase_requirements>

## Standard Stack

### Core (Unchanged -- All Existing)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.0 | Backend framework | Project standard since v1.0.1 |
| MyBatis-Plus | 3.5.5 | ORM with auto-mapping | Auto-maps new DB columns to Java fields, no custom SQL needed |
| MySQL | 8.0+ | Database | Existing infrastructure |
| Vue 3 | 3.5.x | Frontend framework | Project standard |
| TypeScript | 5.9.x | Type safety | Project standard |
| Tailwind CSS | 4.1.x | Styling | Project standard |

### Supporting (Existing, Used in This Phase)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| BeanUtils (Spring) | managed | Entity-DTO property copying | ForeshadowingService already uses BeanUtils.copyProperties for create/update -- new volume fields auto-map |
| Jakarta Validation | managed | @NotBlank, @NotNull on DTOs | ForeshadowingCreateDto already has validation annotations; extend for new fields |
| LambdaQueryWrapper | MyBatis-Plus 3.5.5 | Type-safe DB queries | ForeshadowingService.getForeshadowingList uses this; add volume-based filters |
| JUnit 5 | managed | Unit testing | Test ForeshadowingService distance validation |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Explicit plantedVolume/plannedCallbackVolume fields | Compute volume from chapter range via JOIN | D-01 locks in explicit fields. Volume ranges are not fixed (volumes can be reorganized). Explicit fields give direct grouping without joins. |
| @TableField(exist=false) soft deprecation | Complete DROP COLUMN | D-10 locks in complete deletion. Soft deprecation leaves dead columns in production. |
| Per-volume chapter numbering | Global sequence | D-04 locks in global sequence. Simplifies distance calculation to abs() comparison. |

**Installation:**
No installation needed. Zero new packages.

```bash
# Backend: No new Maven dependencies
# Frontend: No new npm packages
# Database: ALTER TABLE migration only
```

## Architecture Patterns

### Recommended Project Structure (Unchanged)
No new files or directories. All changes are modifications to existing files.

### Pattern 1: MyBatis-Plus Field Addition
**What:** Add new DB columns, add matching Java entity fields, MyBatis-Plus auto-maps them.
**When to use:** Every time a new column is added to a table.
**Example:**
```java
// Entity field addition -- MyBatis-Plus auto-maps from DB column name (snake_case -> camelCase)
// After ALTER TABLE novel_foreshadowing ADD COLUMN planted_volume INT DEFAULT NULL:

/**
 * 埋伏笔的分卷编号
 */
private Integer plantedVolume;

/**
 * 计划回收伏笔的分卷编号
 */
private Integer plannedCallbackVolume;
```
**Source:** Existing pattern in every entity in the codebase (Foreshadowing.java, NovelChapterPlan.java, etc.)

### Pattern 2: Validation in Service Layer
**What:** Business rule validation in service methods before persistence.
**When to use:** When business rules must be enforced regardless of entry point (REST API, batch create, future CLI).
**Example:**
```java
// Source: Existing pattern in ForeshadowingService.createForeshadowing()
@Transactional
public Long createForeshadowing(Long projectId, ForeshadowingCreateDto createDto) {
    // Validate business rules BEFORE persistence
    validateForeshadowingDistance(createDto);
    validateCallbackChapterBounds(projectId, createDto);

    Foreshadowing foreshadowing = new Foreshadowing();
    BeanUtils.copyProperties(createDto, foreshadowing);
    // ... persist
}

private void validateForeshadowingDistance(ForeshadowingCreateDto dto) {
    if ("dark".equals(dto.getLayoutType())) return; // D-07: dark lines exempt
    if (dto.getPlantedChapter() == null || dto.getPlannedCallbackChapter() == null) return;
    int distance = Math.abs(dto.getPlannedCallbackChapter() - dto.getPlantedChapter());
    if (distance < 3) { // D-06: minimum 3 chapters
        throw new RuntimeException("伏笔回收距离不足：明线伏笔至少需要间隔3章才能回收");
    }
}
```
**Source:** Existing pattern in ForeshadowingService (null checks, status defaults before persist)

### Pattern 3: Complete Field Deletion (All Layers)
**What:** When removing a field, delete from DB + entity + DTO + frontend type + frontend components in one pass.
**When to use:** D-09/D-10 mandate complete deletion, no soft deprecation.
**Deletion order:**
1. Frontend components (remove UI references first -- no runtime breakage if field is just not displayed)
2. Frontend types (remove type definitions)
3. Java DTOs (remove from CreateDto, UpdateDto, QueryDto, ResponseDto)
4. Java entity (remove field)
5. DB column (ALTER TABLE DROP COLUMN)
6. init.sql (remove column from CREATE TABLE)

### Anti-Patterns to Avoid
- **Partial deletion (keeping @TableField(exist=false)):** D-10 explicitly forbids this. Dead code accumulates.
- **Deleting DB column before frontend:** Frontend will 500 if it tries to read/write a column that no longer exists. Delete frontend references first.
- **Validating in controller instead of service:** Distance validation is a business rule, belongs in ForeshadowingService so it applies to all entry points (REST API, future batch create, etc.)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity-DTO mapping | Custom mapping code | BeanUtils.copyProperties (already in ForeshadowingService) | New volume fields auto-map via BeanUtils, zero code needed |
| DB queries for volume plan | Custom SQL | NovelVolumePlanMapper.selectOne() with LambdaQueryWrapper | MyBatis-Plus provides this; mapper already extends BaseMapper |
| Distance calculation | Complex volume-aware distance formula | Simple abs(callback - planted) >= 3 | D-04 locks in global chapter sequence; no volume math needed |
| Validation error responses | Custom error format | RuntimeException thrown from service (existing pattern) | ForeshadowingService already throws RuntimeException("伏笔不存在"); same pattern |

**Key insight:** The existing codebase has patterns for everything this phase needs. The only "new" logic is the distance validation check (~10 lines) and the callback chapter bounds check (~5 lines). Everything else is field addition/deletion following established patterns.

## Runtime State Inventory

> This phase involves field deletion (foreshadowingSetup/foreshadowingPayoff) which could affect runtime state.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | `novel_chapter_plan.foreshadowing_setup` and `novel_chapter_plan.foreshadowing_payoff` columns contain free-text data in existing projects | D-05: existing test data can be fully cleaned. ALTER TABLE DROP COLUMN will destroy this data. Acceptable per locked decision. |
| Live service config | None -- no n8n workflows, no external services reference these columns | None |
| OS-registered state | None | None |
| Secrets/env vars | None -- no env vars reference foreshadowing fields | None |
| Build artifacts | `ai-factory-frontend` build output may cache old TypeScript types | Rebuild frontend after type changes (npm run build) |

**Nothing found in category:** Live service config, OS-registered state, Secrets/env vars -- verified by grep across entire codebase. Only the two DB columns contain runtime data that will be destroyed by DROP COLUMN.

## Common Pitfalls

### Pitfall 1: Incomplete Field Cleanup Leaves Ghost References
**What goes wrong:** foreshadowingSetup/foreshadowingPayoff deleted from Java entity but still referenced in frontend TypeScript types or Vue components. Frontend sends update requests containing these fields, which silently fail or cause 500 errors.
**Why it happens:** The 26 files referencing these fields span 3 layers (backend, frontend, SQL). Missing any one file leaves a dangling reference.
**How to avoid:** Use grep to find ALL references before starting cleanup. Delete in order: frontend components -> frontend types -> Java DTOs -> Java entity -> DB column. Verify with a full grep after each layer.
**Warning signs:** TypeScript compilation errors, runtime 400 errors on chapter plan update API, or silent field loss on save.

### Pitfall 2: Distance Validation Too Early or Too Late in Service Lifecycle
**What goes wrong:** Validation runs after BeanUtils.copyProperties, so the DTO's plantedChapter field is already on the entity, but the validation is checking the DTO's callback field against an entity that has not been fully populated.
**Why it happens:** ForeshadowingService.updateForeshadowing() does BeanUtils.copyProperties from UpdateDto, then separately sets plantedChapter from the existing entity (line 140). If validation runs between these two steps, it sees stale data.
**How to avoid:** Run validation AFTER all fields are resolved on the entity/DTO, BEFORE the mapper.insert/updateById call. Specifically: after copyProperties + manual field overrides, before persistence.
**Warning signs:** Validation passes but should have failed (stale data used for check), or validation fails incorrectly (null field not yet populated).

### Pitfall 3: ForeshadowingQueryDto.currentChapter Filter Becomes Invalid
**What goes wrong:** The existing currentChapter filter in ForeshadowingQueryDto queries `plannedCallbackChapter` (a simple Integer). After adding volume fields, the filter should consider volume context. A query for "chapter 10" could match chapter 10 in volume 1 OR volume 2 if volume context is not included.
**Why it happens:** D-04 changed to global chapter sequence, so this is actually SAFE -- global chapter numbers are unique across volumes. But the query DTO should be extended with volume-based filters for new use cases.
**How to avoid:** Add `plantedVolume` and `plannedCallbackVolume` filter fields to ForeshadowingQueryDto. Keep the existing `currentChapter` filter working (it uses global sequence per D-04). Add new filters as optional parameters.
**Warning signs:** Query returns foreshadowing from wrong volume when only chapter number is used.

### Pitfall 4: init.sql Gets Out of Sync with Migration
**What goes wrong:** ALTER TABLE is run against the development database, but init.sql is not updated. New developers spinning up a fresh database get the OLD schema without volume fields and WITH the deleted foreshadowing_setup/payoff columns.
**Why it happens:** Forgetting to update init.sql after running ALTER TABLE is common. The migration script is the operational change; init.sql is the schema-of-record.
**How to avoid:** ALWAYS update init.sql in the same commit as the ALTER TABLE migration. init.sql should be the single source of truth for a fresh database setup.
**Warning signs:** Fresh database setup fails when application tries to read plantedVolume column that does not exist.

### Pitfall 5: VolumePlan Query for Callback Chapter Bounds Fails When Volume Has No Plan
**What goes wrong:** D-03 requires checking that plannedCallbackChapter does not exceed the volume's targetChapterCount. But if the volume plan does not exist yet (volume is not yet planned), the query returns null, and the validation throws an NPE or silently passes.
**Why it happens:** Foreshadowing can be created before volume plans are generated (user manually creates foreshadowing targeting a future volume).
**How to avoid:** If volume plan is not found, skip the targetChapterCount validation (cannot validate against unknown bounds). Only validate when the volume plan exists and has a non-null targetChapterCount.
**Warning signs:** NullPointerException in ForeshadowingService when creating foreshadowing for an unplanned volume.

## Code Examples

### Adding Volume Fields to Foreshadowing Entity (Verified from Foreshadowing.java)

```java
// Current Foreshadowing.java fields (lines 55-66):
private Integer plantedChapter;
private Integer plannedCallbackChapter;
private Integer actualCallbackChapter;

// ADD after plantedChapter:
/**
 * 埋伏笔的分卷编号
 */
private Integer plantedVolume;

// ADD after plannedCallbackChapter:
/**
 * 计划回收伏笔的分卷编号
 */
private Integer plannedCallbackVolume;
```

### SQL Migration (Verified against init.sql lines 340-360)

```sql
-- Add volume columns to novel_foreshadowing
ALTER TABLE novel_foreshadowing
  ADD COLUMN planted_volume INT DEFAULT NULL
    COMMENT '埋设伏笔的分卷编号' AFTER planted_chapter,
  ADD COLUMN planned_callback_volume INT DEFAULT NULL
    COMMENT '计划回收伏笔的分卷编号' AFTER planned_callback_chapter;

-- Remove redundant fields from novel_chapter_plan
ALTER TABLE novel_chapter_plan
  DROP COLUMN foreshadowing_setup,
  DROP COLUMN foreshadowing_payoff;
```

### Distance Validation in ForeshadowingService (Verified against existing createForeshadowing pattern)

```java
// Add to ForeshadowingService -- injected dependency
@Autowired
private NovelVolumePlanMapper volumePlanMapper;

// Distance validation (called from createForeshadowing and updateForeshadowing)
private static final int MIN_FORESHADOWING_DISTANCE = 3;

private void validateForeshadowingDistance(ForeshadowingCreateDto dto) {
    // D-07: dark lines are exempt
    if ("dark".equals(dto.getLayoutType())) {
        return;
    }
    // Only validate when both planting and callback chapters are specified
    if (dto.getPlantedChapter() == null || dto.getPlannedCallbackChapter() == null) {
        return;
    }
    // D-04: global sequence, simple subtraction
    int distance = dto.getPlannedCallbackChapter() - dto.getPlantedChapter();
    if (distance < MIN_FORESHADOWING_DISTANCE) {
        throw new RuntimeException(
            "明线伏笔回收距离不足：埋设章节(" + dto.getPlantedChapter()
            + ")与回收章节(" + dto.getPlannedCallbackChapter()
            + ")至少需间隔" + MIN_FORESHADOWING_DISTANCE + "章"
        );
    }
}

// Callback chapter bounds validation (D-03)
private void validateCallbackChapterBounds(Long projectId, ForeshadowingCreateDto dto) {
    if (dto.getPlannedCallbackVolume() == null || dto.getPlannedCallbackChapter() == null) {
        return;
    }
    LambdaQueryWrapper<NovelVolumePlan> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(NovelVolumePlan::getProjectId, projectId)
           .eq(NovelVolumePlan::getVolumeNumber, dto.getPlannedCallbackVolume());
    NovelVolumePlan volumePlan = volumePlanMapper.selectOne(wrapper);
    if (volumePlan != null && volumePlan.getTargetChapterCount() != null) {
        if (dto.getPlannedCallbackChapter() > volumePlan.getTargetChapterCount()) {
            throw new RuntimeException(
                "回收章节超出范围：第" + dto.getPlannedCallbackVolume() + "卷规划"
                + volumePlan.getTargetChapterCount() + "章，无法设置回收章节为"
                + dto.getPlannedCallbackChapter()
            );
        }
    }
    // If volume plan not found, skip validation (volume not yet planned)
}
```

### Files Requiring foreshadowingSetup/foreshadowingPayoff Deletion (Complete Inventory)

**Backend (3 files):**
1. `ai-factory-backend/src/main/java/com/aifactory/entity/NovelChapterPlan.java` -- lines 97-103 (two fields)
2. `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanDto.java` -- lines 57-61 (two fields)
3. `ai-factory-backend/src/main/java/com/aifactory/dto/ChapterPlanUpdateRequest.java` -- lines 44-48 (two fields)

**Frontend (5 files):**
4. `ai-factory-frontend/src/types/project.ts` -- lines 132-133 (Chapter interface), lines 156-157 (ChapterPlan interface)
5. `ai-factory-frontend/src/api/chapter.ts` -- lines 41-42 (ChapterPlanUpdateRequest interface)
6. `ai-factory-frontend/src/views/Project/Detail/creation/ChapterPlanDrawer.vue` -- lines 30-31, 45-46, 250-251, 279-280, 308-309, 494, 510 (form definition, data mapping, save, template)
7. `ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue` -- lines 159-160 (chapter plan construction)
8. `ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue` -- lines 115-116 (chapter plan construction)

**SQL (1 file):**
9. `sql/init.sql` -- lines 174-175 (novel_chapter_plan CREATE TABLE)

### QueryDto Extension for Volume-Based Filtering

```java
// Add to ForeshadowingQueryDto.java
@Schema(description = "埋设伏笔的分卷编号筛选", example = "1")
private Integer plantedVolume;

@Schema(description = "计划回收伏笔的分卷编号筛选", example = "2")
private Integer plannedCallbackVolume;
```

```java
// Add to ForeshadowingService.getForeshadowingList() -- after existing filters:
if (queryDto.getPlantedVolume() != null) {
    queryWrapper.eq(Foreshadowing::getPlantedVolume, queryDto.getPlantedVolume());
}
if (queryDto.getPlannedCallbackVolume() != null) {
    queryWrapper.eq(Foreshadowing::getPlannedCallbackVolume, queryDto.getPlannedCallbackVolume());
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Per-volume chapter numbering | Global chapter sequence (D-04) | v1.0.6 Phase 15 decision | Distance calculation simplified to abs() |
| @TableField(exist=false) soft deprecation | Complete DROP COLUMN (D-10) | v1.0.6 Phase 15 decision | Cleaner schema, no dead columns |
| Text-based foreshadowing in chapter plan | Structured novel_foreshadowing table | v1.0.2 (table created), v1.0.6 (activated) | Structured data enables AI integration |

**Deprecated/outdated:**
- `novel_chapter_plan.foreshadowing_setup` / `novel_chapter_plan.foreshadowing_payoff`: Redundant with structured table. Per D-09, completely removed.
- `novel_chapter_plan.foreshadowing_actions`: Column exists in DB (init.sql line 179) but is NOT mapped in Java entity. Not part of this phase but noted as orphaned.

## Open Questions

1. **Global chapter sequence generation mechanism**
   - What we know: D-04 says chapters use global sequence. Current system has `chapterNumber` on NovelChapterPlan that appears to already be global (based on ChapterGenerationTaskStrategy.saveChaptersToDatabase which uses nextChapterNumber).
   - What's unclear: Is there an existing utility to compute "volume N starts at global chapter X"? The distance validation only needs plantedChapter and plannedCallbackChapter (both global), so this may not be needed for Phase 15.
   - Recommendation: For Phase 15 distance validation, use the simple global chapter numbers already stored in plantedChapter/plannedCallbackChapter. Volume start offset computation is only needed for display purposes (Phase 18/19 frontend).

2. **Index strategy for new volume columns**
   - What we know: Current table has only `idx_project_id`. Cross-volume queries will filter by (project_id, plantedVolume) or (project_id, plannedCallbackVolume).
   - What's unclear: Whether composite indexes are needed now or can be deferred.
   - Recommendation: Add lightweight indexes as part of the ALTER TABLE migration. Cost is minimal (empty columns during development). Two indexes: `idx_planted_volume (project_id, planted_volume)` and `idx_callback_volume (project_id, planned_callback_volume)`.

## Environment Availability

> Step 2.6: SKIPPED (no external dependencies identified -- all changes are to existing code/database schema with no new tools, services, or runtimes required)

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (already in project) |
| Config file | pom.xml (dependency exists) |
| Quick run command | `mvn test -pl ai-factory-backend -Dtest=ForeshadowingServiceTest -DfailIfNoTests=false` |
| Full suite command | `mvn test -pl ai-factory-backend` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DATA-01 | plantedVolume/plannedCallbackVolume fields present in entity, DTO, DB | unit | `mvn test -pl ai-factory-backend -Dtest=ForeshadowingServiceTest` | No (Wave 0) |
| DATA-02 | foreshadowingSetup/foreshadowingPayoff absent from all layers | compile check | `mvn compile -pl ai-factory-backend && cd ai-factory-frontend && npx tsc --noEmit` | No (Wave 0) |
| DATA-03a | Distance validation rejects bright-line foreshadowing with < 3 chapter gap | unit | `mvn test -pl ai-factory-backend -Dtest=ForeshadowingServiceTest` | No (Wave 0) |
| DATA-03b | Distance validation allows dark-line foreshadowing with any gap | unit | `mvn test -pl ai-factory-backend -Dtest=ForeshadowingServiceTest` | No (Wave 0) |
| DATA-03c | Callback chapter bounds validation rejects chapter > targetChapterCount | unit | `mvn test -pl ai-factory-backend -Dtest=ForeshadowingServiceTest` | No (Wave 0) |

### Sampling Rate
- **Per task commit:** `mvn compile -pl ai-factory-backend && cd ai-factory-frontend && npx tsc --noEmit`
- **Per wave merge:** `mvn test -pl ai-factory-backend`
- **Phase gate:** Full compile + test green, frontend TypeScript check passes

### Wave 0 Gaps
- [ ] `ForeshadowingServiceTest.java` -- distance validation + bounds validation tests
- [ ] Manual verification: ALTER TABLE migration runs without error on dev database
- [ ] Manual verification: Frontend builds after type deletion (`npm run build` in ai-factory-frontend)

## Sources

### Primary (HIGH confidence)
- Direct code analysis: Foreshadowing.java, ForeshadowingService.java, ForeshadowingController.java, ForeshadowingCreateDto.java, ForeshadowingUpdateDto.java, ForeshadowingQueryDto.java, ForeshadowingDto.java -- all verified current state of entity/DTO fields and service methods
- Direct code analysis: NovelChapterPlan.java -- verified foreshadowingSetup/foreshadowingPayoff at lines 98, 103
- Direct code analysis: NovelVolumePlan.java -- verified targetChapterCount field exists (line 76)
- Direct code analysis: sql/init.sql -- verified novel_foreshadowing CREATE TABLE (lines 340-360), novel_chapter_plan columns (lines 174-175)
- Direct code analysis: NovelVolumePlanMapper.java -- verified extends BaseMapper, no custom SQL needed
- Direct code analysis: ChapterPlanDrawer.vue, VolumeTree.vue, CreationCenter.vue, project.ts, chapter.ts -- verified all foreshadowingSetup/foreshadowingPayoff references
- CONTEXT.md decisions D-01 through D-10 -- locked user decisions

### Secondary (MEDIUM confidence)
- `.planning/research/STACK.md` -- zero new dependencies confirmed
- `.planning/research/PITFALLS.md` -- Pitfall 3 (old field removal) analysis, Pitfall 5 (query performance) index recommendations
- `.planning/research/ARCHITECTURE.md` -- data flow patterns, BeanUtils.copyProperties mapping behavior

### Tertiary (LOW confidence)
- None -- all findings based on direct codebase analysis

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- zero new dependencies, all existing code analyzed
- Architecture: HIGH -- all files read and verified, patterns confirmed from codebase
- Pitfalls: HIGH -- derived from codebase analysis, verified against actual file contents and line numbers
- Validation: MEDIUM -- test infrastructure exists but no foreshadowing-specific tests yet (Wave 0 gap)

**Research date:** 2026-04-10
**Valid until:** 2026-05-10 (stable codebase, no external dependencies)
