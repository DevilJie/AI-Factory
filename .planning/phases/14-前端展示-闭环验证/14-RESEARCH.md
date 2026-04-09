# Phase 14: 前端展示 + 闭环验证 - Research

**Researched:** 2026-04-09
**Domain:** Vue 3 frontend component enhancement + Spring Boot REST endpoint
**Confidence:** HIGH

## Summary

Phase 14 adds a comparison view to the existing ChapterPlanDrawer's "角色规划" tab, allowing users to see planned vs. actual characters side-by-side after chapter generation. The implementation is primarily frontend-focused with one new backend REST endpoint.

The backend work is minimal: a single new GET endpoint in `ChapterController` (or `NovelCharacterController`) that reuses the existing `NovelCharacterChapterMapper.selectByChapterId()` and JOINs with `novel_character` to return character names. The service layer method `NovelCharacterChapterService.getCharactersByChapterId()` already exists and returns `List<NovelCharacterChapter>` -- the new endpoint only needs to augment this data with character names and wrap it in a VO.

The frontend work centers on modifying `ChapterPlanDrawer.vue` to add a collapsible comparison region above the existing editable character list in the "角色规划" tab. A new API call fetches actual characters from the new endpoint, and a simple frontend matching function compares planned vs. actual using ID-first, name-fallback logic.

**Primary recommendation:** Add one backend endpoint, then implement the comparison as a self-contained section within the existing ChapterPlanDrawer's character tab -- no new components or stores needed.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 左右分栏并排对比 -- 左栏规划角色列表，右栏实际登场角色列表。同名角色对齐，偏差角色高亮
- **D-02:** 颜色+图标双重标记 -- 规划了但未出场用红色✗，实际出场但未规划用黄色⚠，一致用绿色✓
- **D-03:** 对比结果内嵌在 ChapterPlanDrawer 角色规划 tab 内 -- 位于规划角色列表上方，折叠/展开可切换，默认展开
- **D-04:** 章节未生成时仅显示规划角色列表（当前行为不变），不显示对比区域
- **D-05:** 章节已生成且有实际角色数据时，对比区域自动出现并展示摘要（如"3/4 出场"）
- **D-06:** 轻量优化 -- 在角色名旁显示链接按钮，点击跳转打开 CharacterDrawer 查看角色详情。仅 characterId 存在时显示链接
- **D-07:** 不做重度重构（卡片化、头像、聚合信息等），保持现有可编辑列表形式
- **D-08:** 用户打开 ChapterPlanDrawer 切换到角色规划 tab 时自动显示对比（如果章节已生成）。无需手动触发
- **D-09:** 对比区域位于规划角色列表上方，包含摘要行（出场率）和左右分栏详细对比
- **D-10:** 对比区域可折叠/展开，不遮挡规划角色编辑功能
- **D-11:** 新增 REST 端点 GET /api/novel/{projectId}/chapters/{chapterId}/characters -- 返回该章节的所有实际登场角色列表
- **D-12:** 复用现有 NovelCharacterChapterMapper.selectByChapterId()，Controller 层新增端点暴露
- **D-13:** 返回数据包含 characterId、characterName（需 JOIN novel_character 表）、roleType、importanceLevel 等对比所需字段
- **D-14:** 前端分别调用 chapter plan API（获取规划角色）和新增 API（获取实际角色），在前端做对比匹配逻辑
- **D-15:** ID 优先 + 名称 fallback -- 规划角色有 characterId 时用 ID 精确匹配，没有时用 characterName 字符串匹配
- **D-16:** 前端实现对比逻辑（不需要后端聚合端点）。匹配结果分三组：一致（✓）、规划未出场（✗红色）、计划外出场（⚠黄色）
- **D-17:** 不复用 NameMatchUtil 的三级匹配（前端场景不需要去后缀、包含等容错），用简单的 ID/名称匹配即可

### Claude's Discretion
- 对比区域的具体 Tailwind 样式和颜色值
- 折叠/展开交互的过渡动画
- 左右分栏在 Drawer 窄屏下的响应式处理（640px 宽度）
- 角色详情链接的具体实现方式（按钮图标、点击行为）
- 对比摘要的文字措辞
- 新端点的 VO/DTO 命名

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FE-01 | 用户在章节规划 Drawer 中能看到规划角色列表及其戏份梗概 | ChapterPlanDrawer.vue 已有角色规划 tab，含 editableCharacters 列表展示角色名、roleType、importance、roleDescription。轻量优化：在 characterName 旁添加 ExternalLink 图标链接（仅 characterId 存在时），触发 CharacterDrawer 打开 |
| FE-02 | 用户章节生成后能看到规划角色 vs 实际登场角色的对比，标记偏差 | 新增后端端点返回章节实际角色列表 + 前端在角色规划 tab 顶部添加可折叠对比区域 + ID/name 双模式匹配逻辑 + 三色标记（绿/红/黄） |
</phase_requirements>

## Standard Stack

### Core (existing -- no new dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 | 3.5.x | Frontend framework | Project-wide standard, Composition API with `<script setup>` |
| Pinia | 3.0.x | State management | Already used by editor store |
| Tailwind CSS | 4.1.x | Styling | Project-wide utility CSS |
| Lucide Vue | 0.469.x | Icons | Already imported in ChapterPlanDrawer (X, Save icons) |
| Axios | 1.13.x | HTTP client | Already wrapped in `@/utils/request` |
| Spring Boot | 3.2.0 | Backend framework | Standard backend |
| MyBatis-Plus | 3.5.5 | ORM | Existing mapper pattern |

### Supporting (no new installs needed)
| Library | Purpose | When to Use |
|---------|---------|-------------|
| Lombok | VO/DTO boilerplate reduction | New `ChapterCharacterVO` for the endpoint response |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Inline comparison in ChapterPlanDrawer | Separate ComparisonView component | Inline keeps it simple (D-03, D-07); separate component only if complexity grows |
| Frontend matching logic | Backend aggregation endpoint | Frontend is simpler and matches D-14/D-16; backend aggregation adds a second endpoint for no benefit |

**Installation:** No new packages required. All dependencies already in project.

## Architecture Patterns

### Recommended Project Structure (no new files except backend VO)

```
# Backend additions
ai-factory-backend/src/main/java/com/aifactory/
  vo/ChapterCharacterVO.java         -- NEW: VO for chapter->characters response
  controller/ChapterController.java  -- MODIFY: add GET endpoint

# Frontend modifications (no new files)
ai-factory-frontend/src/
  api/chapter.ts                     -- MODIFY: add getChapterCharacters() API call
  views/Project/Detail/creation/
    ChapterPlanDrawer.vue            -- MODIFY: add comparison region + character link
```

### Pattern 1: Backend Endpoint -- Reuse Existing Mapper + Service
**What:** Add a GET endpoint that queries `novel_character_chapter` by chapterId and JOINs `novel_character` for the name.
**When to use:** This is the only backend addition.
**Example:**
```java
// In ChapterController.java (follows existing pattern of chapter-scoped endpoints)
@GetMapping("/{chapterId}/characters")
public Result<List<ChapterCharacterVO>> getChapterCharacters(
        @PathVariable Long projectId,
        @PathVariable Long chapterId) {
    Long userId = UserContext.getUserId();
    log.info("用户 {} 获取章节 {} 的实际登场角色列表", userId, chapterId);

    List<NovelCharacterChapter> relations =
        characterChapterService.getCharactersByChapterId(chapterId);

    // Map to VO with character name from novel_character table
    List<ChapterCharacterVO> result = relations.stream().map(rel -> {
        NovelCharacter character = characterMapper.selectById(rel.getCharacterId());
        return ChapterCharacterVO.builder()
            .characterId(rel.getCharacterId())
            .characterName(character != null ? character.getName() : "未知角色")
            .roleType(rel.getImportanceLevel())
            .importanceLevel(rel.getImportanceLevel())
            .build();
    }).collect(Collectors.toList());

    return Result.ok(result);
}
```

### Pattern 2: Frontend API Call
**What:** Add a function to `chapter.ts` following the existing `request.get<T>(url)` pattern.
**When to use:** Single API call for actual characters.
**Example:**
```typescript
// In api/chapter.ts
export interface ChapterCharacter {
  characterId: number
  characterName: string
  roleType: string
  importanceLevel: string
}

export const getChapterCharacters = (projectId: string, chapterId: string) => {
  return request.get<ChapterCharacter[]>(
    `/api/novel/${projectId}/chapters/${chapterId}/characters`
  )
}
```

### Pattern 3: Frontend Matching Logic (ID-first, name fallback)
**What:** Compare planned characters (from `plannedCharacters` JSON) with actual characters (from new endpoint).
**When to use:** In ChapterPlanDrawer, computed property that runs when both lists are available.
**Example:**
```typescript
interface ComparisonResult {
  matched: Array<{ planned: any; actual: ChapterCharacter }>       // green ✓
  plannedOnly: any[]                                                  // red ✗
  actualOnly: ChapterCharacter[]                                      // yellow ⚠
}

function compareCharacters(planned: any[], actual: ChapterCharacter[]): ComparisonResult {
  const matched: Array<{ planned: any; actual: ChapterCharacter }> = []
  const plannedOnly: any[] = []
  const actualOnly: ChapterCharacter[] = [...actual]

  for (const p of planned) {
    let found = false
    // ID-first matching
    if (p.characterId) {
      const idx = actualOnly.findIndex(a => a.characterId === Number(p.characterId))
      if (idx !== -1) {
        matched.push({ planned: p, actual: actualOnly.splice(idx, 1)[0] })
        found = true
      }
    }
    // Name fallback
    if (!found && p.characterName) {
      const idx = actualOnly.findIndex(a => a.characterName === p.characterName)
      if (idx !== -1) {
        matched.push({ planned: p, actual: actualOnly.splice(idx, 1)[0] })
        found = true
      }
    }
    if (!found) {
      plannedOnly.push(p)
    }
  }

  return { matched, plannedOnly, actualOnly }
}
```

### Anti-Patterns to Avoid
- **Opening CharacterDrawer from within ChapterPlanDrawer via direct import:** CharacterDrawer is currently only used in `Characters.vue` and manages its own state. The ChapterPlanDrawer lives in the creation view. Opening CharacterDrawer requires either emitting an event to a parent that can host CharacterDrawer, or using a global event bus. Recommendation: emit event from ChapterPlanDrawer, handle in the parent component (`Creation.vue` or equivalent).
- **Using NameMatchUtil for frontend matching:** D-17 explicitly excludes this. Simple ID + exact string comparison is sufficient for the comparison view.
- **Adding comparison data to the editor store:** The comparison is view-only and tab-specific. Keep it as local state in ChapterPlanDrawer to avoid store bloat.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Character name resolution | N+1 query loop | Batch fetch or stream with `characterMapper.selectById()` | For typical chapters (5-15 characters), individual selects are acceptable. If performance concerns arise later, batch with `selectBatchIds()` |
| Icon rendering | Custom SVG components | Lucide Vue icons (CheckCircle2, XCircle, AlertTriangle, ExternalLink, ChevronDown, ChevronUp) | Already a project dependency |
| HTTP response unwrapping | Custom fetch logic | `@/utils/request` wrapper (already strips `res.data` from Result<T>) | Consistent error handling |

**Key insight:** This phase is almost entirely UI layer work. The backend piece is a thin wrapper over existing service methods. No complex logic needs to be built from scratch.

## Common Pitfalls

### Pitfall 1: ChapterPlan has no chapterId when chapter is not generated
**What goes wrong:** The comparison requires a `chapterId` to call the new endpoint. But `ChapterPlanDto.chapterId` is only populated when the chapter has been generated.
**Why it happens:** The frontend `currentChapterPlan` might not carry `chapterId` if the chapter hasn't been generated yet.
**How to avoid:** D-04 says "章节未生成时仅显示规划角色列表". Check `currentChapterPlan.chapterId` (or `editorStore.isChapterGenerated`) before showing comparison. If no chapterId, skip the API call and hide the comparison region.
**Warning signs:** Console error "Cannot read property of undefined" when accessing chapterId.

### Pitfall 2: CharacterDrawer is not accessible from ChapterPlanDrawer's parent
**What goes wrong:** ChapterPlanDrawer.vue lives inside the creation editor view, but CharacterDrawer is imported in Characters.vue. The drawer components use Teleport to body, so they can coexist, but there needs to be a CharacterDrawer instance mounted somewhere reachable.
**Why it happens:** CharacterDrawer requires `modelValue`, `character`, and `projectId` props, plus an event listener for `saved`. It cannot be opened by just calling a function.
**How to avoid:** Either (a) add a CharacterDrawer instance in the creation view's parent component (where ChapterPlanDrawer is used), or (b) use a simpler approach -- navigate to the Characters tab or emit a custom event. Option (a) is straightforward since CharacterDrawer teleports to body anyway.
**Warning signs:** Clicking the link does nothing because there's no CharacterDrawer rendered.

### Pitfall 3: plannedCharacters JSON field is a string, not parsed
**What goes wrong:** The `plannedCharacters` field is a JSON string in the backend, and the frontend already handles parsing via `editableCharacters`. The comparison logic needs to parse this same string. If using a different parsing path, you might get inconsistent data.
**Why it happens:** Multiple computed properties or watches parsing the same JSON string.
**How to avoid:** Reuse the existing `parsedPlannedCharacters` computed or `editableCharacters` ref for the comparison, rather than re-parsing `form.plannedCharacters` separately.
**Warning signs:** Comparison shows empty planned list when it should have entries.

### Pitfall 4: Drawer width (640px) too narrow for side-by-side comparison
**What goes wrong:** With left and right panels plus padding, each panel gets less than 280px -- tight for character names + badges + icons.
**Why it happens:** D-01 specifies left-right split. Drawer is 640px (w-[640px]).
**How to avoid:** Keep panel content minimal (name + badge only, no description in comparison view). Use compact spacing. Consider stacking vertically only if both panels have 5+ entries. CONTEXT.md suggests blue info background to visually distinguish from the editable list below.
**Warning signs:** Horizontal scrolling or text truncation in comparison panels.

### Pitfall 5: Actual characters endpoint returns empty list for newly generated chapters
**What goes wrong:** After chapter generation, the character extraction might not have run yet, or the `novel_character_chapter` table might not have records for this chapter.
**Why it happens:** Character extraction (Phase 12) runs after chapter generation, but there could be a delay or the extraction might fail silently.
**How to avoid:** Handle empty actual character list gracefully -- show comparison with all planned characters marked as "未出场" (red ✗). Do not assume actual characters always exist for generated chapters.
**Warning signs:** Comparison area shows all red ✗ for a chapter that was just generated.

## Code Examples

### Backend VO Definition (NEW file)
```java
// Source: Pattern from existing CharacterChapterVO.java
package com.aifactory.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterCharacterVO {
    private Long characterId;
    private String characterName;
    private String roleType;        // importanceLevel value from novel_character_chapter
    private String importanceLevel;
}
```

### Frontend: Character Link (in ChapterPlanDrawer.vue template)
```html
<!-- Inside editableCharacters loop, next to characterName input -->
<button
  v-if="char.characterId"
  class="p-1 text-blue-500 hover:text-blue-600 dark:text-blue-400"
  title="查看角色详情"
  @click.stop="$emit('openCharacter', char.characterId)"
>
  <ExternalLink class="w-3.5 h-3.5" />
</button>
```

### Frontend: Collapsible Comparison Region
```html
<!-- Above the editable character list, inside the character tab -->
<div v-if="activeSection === 'character' && chapterId" class="mb-4">
  <!-- Summary + toggle -->
  <div
    class="flex items-center justify-between px-3 py-2 bg-blue-50 dark:bg-blue-900/20
           rounded-lg cursor-pointer select-none"
    @click="showComparison = !showComparison"
  >
    <span class="text-sm font-medium text-blue-700 dark:text-blue-300">
      {{ comparisonSummary }}
    </span>
    <component :is="showComparison ? ChevronUp : ChevronDown" class="w-4 h-4 text-blue-500" />
  </div>

  <!-- Comparison detail (collapsible) -->
  <div v-if="showComparison" class="mt-2 grid grid-cols-2 gap-3">
    <!-- Left: planned, Right: actual -->
    <div class="space-y-1">
      <div class="text-xs font-medium text-gray-500 mb-1">规划角色</div>
      <!-- matched items (green ✓) -->
      <!-- planned-only items (red ✗) -->
    </div>
    <div class="space-y-1">
      <div class="text-xs font-medium text-gray-500 mb-1">实际登场</div>
      <!-- matched items (green ✓) -->
      <!-- actual-only items (yellow ⚠) -->
    </div>
  </div>
</div>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SSE streaming for chapter generation | Async task + polling | Phase 12+ | Comparison data available after polling completes and `isChapterGenerated` becomes true |

**Deprecated/outdated:**
- None relevant to this phase

## Open Questions

1. **CharacterDrawer hosting in creation view**
   - What we know: CharacterDrawer is currently only in Characters.vue. ChapterPlanDrawer is used in the creation editor flow.
   - What's unclear: Where exactly ChapterPlanDrawer's parent is (which Vue file hosts it), and whether a CharacterDrawer can be added there.
   - Recommendation: Research during implementation -- find the parent component of ChapterPlanDrawer, add CharacterDrawer there. The Teleport-to-body pattern means they won't clash visually.

2. **Endpoint placement: ChapterController vs NovelCharacterController**
   - What we know: D-11 says "GET /api/novel/{projectId}/chapters/{chapterId}/characters". The URL is chapter-scoped (`/chapters/{chapterId}/...`).
   - What's unclear: Whether it belongs in ChapterController (which handles `/api/novel/{projectId}/chapters/...`) or NovelCharacterController (which handles `/api/novel/{projectId}/characters/...`).
   - Recommendation: Place in **ChapterController** because the URL path is chapter-scoped. This follows the REST convention where the resource path determines the controller. ChapterController already has methods injected with `ChapterService`, so it needs `NovelCharacterChapterService` and `NovelCharacterMapper` added as autowired dependencies.

## Environment Availability

> This phase depends on MySQL, Redis, and the Spring Boot backend. All are required for backend endpoint testing.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL 8.0+ | Backend data | Assumed running (dev) | -- | H2 in-memory not configured |
| Redis 6.0+ | Caching | Assumed running (dev) | -- | Disabled in dev profile |
| Node.js 18+ | Frontend build | Yes | -- | -- |
| Java 21 | Backend runtime | Yes | -- | -- |
| Maven 3.8+ | Backend build | Yes | -- | -- |

**Missing dependencies with no fallback:**
- None identified -- standard dev environment expected

**Missing dependencies with fallback:**
- Redis disabled in dev profile -- caching skipped, no impact on this phase

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (backend), none (frontend) |
| Config file | none (frontend) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=ChapterControllerTest -DfailIfNoTests=false` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FE-01 | Character link shown when characterId exists, hidden when null | Manual (frontend) | Visual inspection in browser | N/A |
| FE-02 | Comparison shows green/red/yellow markers correctly | Manual (frontend) | Visual inspection in browser | N/A |
| FE-02 | Backend endpoint returns correct character data for a chapter | Unit (backend) | `mvn test -Dtest=ChapterControllerTest#testGetChapterCharacters` | No -- Wave 0 |
| FE-02 | Matching logic handles ID match, name match, and no match | Unit (frontend) | No frontend test infrastructure | N/A |

### Sampling Rate
- **Per task commit:** `mvn test` (backend)
- **Per wave merge:** Full backend suite + manual frontend verification
- **Phase gate:** Full backend suite green + manual UI walkthrough

### Wave 0 Gaps
- [ ] `ChapterControllerTest.java` -- backend unit test for new endpoint (if desired)
- [ ] No frontend test infrastructure exists -- all frontend validation is manual

**Note:** This phase is primarily UI work. Backend endpoint can be tested via Swagger UI or curl. Frontend changes require visual verification. No automated frontend test setup is recommended given the project has no frontend test infrastructure.

## Sources

### Primary (HIGH confidence)
- Codebase analysis: `ChapterPlanDrawer.vue` -- full file read, 641 lines
- Codebase analysis: `NovelCharacterChapterMapper.java` -- confirmed `selectByChapterId()` exists
- Codebase analysis: `NovelCharacterChapterService.java` -- confirmed `getCharactersByChapterId()` exists
- Codebase analysis: `ChapterController.java` -- confirmed URL pattern and DI pattern
- Codebase analysis: `CharacterDrawer.vue` -- confirmed props/events interface
- Codebase analysis: `editor.ts` store -- confirmed state management pattern

### Secondary (MEDIUM confidence)
- Codebase analysis: `NovelCharacter.java` entity -- confirmed `name` field for JOIN
- Codebase analysis: `ChapterPlanDto.java` -- confirmed `chapterId` and `plannedCharacters` fields

### Tertiary (LOW confidence)
- None -- all findings verified against source code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing code analyzed
- Architecture: HIGH -- patterns well-established by existing codebase
- Pitfalls: HIGH -- identified from actual code structure analysis

**Research date:** 2026-04-09
**Valid until:** 2026-05-09 (stable codebase, no fast-moving dependencies)
