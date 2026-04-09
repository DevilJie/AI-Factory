---
status: awaiting_human_verify
trigger: "章节规划页面右上角的详情按钮，点击后出现的抽屉(Drawer)，显示的内容不全"
created: 2026-04-08T00:00:00Z
updated: 2026-04-08T00:30:00Z
---

## Current Focus

hypothesis: CONFIRMED - ChapterPlanDrawer.vue was incomplete, now fixed
test: Compilation passes, all tests green
expecting: User verification of drawer in browser
next_action: Wait for user to verify the fix works end-to-end

## Symptoms

expected: 抽屉应展示章节规划的所有字段数据（包括但不限于章节标题、概要、角色规划、情节要点等），并且允许编辑
actual: 抽屉只展示了部分内容，缺少很多章节规划字段
errors: 无报错，是功能不完整的问题
reproduction: 打开章节规划页面 -> 点击右上角详情按钮 -> 查看抽屉内容
started: 一直存在，抽屉从未完整展示过所有规划字段

## Eliminated

## Evidence

- timestamp: 2026-04-08T00:01:00Z
  checked: NovelChapterPlan.java entity fields
  found: 20 fields total: id, projectId, volumePlanId, chapterNumber, chapterTitle, plotOutline, chapterStartingScene, chapterEndingScene, keyEvents, chapterGoal, wordCountTarget, chapterNotes, status, plotStage, stageCompleted, foreshadowingSetup, foreshadowingPayoff, plannedCharacters, characterArcs, createTime, updateTime
  implication: Rich data available but not displayed

- timestamp: 2026-04-08T00:01:30Z
  checked: ChapterPlanDrawer.vue current form fields
  found: Only 6 fields displayed: chapterNumber, title, summary, keyEvents, characters (from newCharacters), foreshadowing (from foreshadowingSetup)
  implication: 14+ fields missing from drawer

- timestamp: 2026-04-08T00:02:00Z
  checked: ChapterPlan TypeScript interface
  found: Only has: id, volumeId, projectId, chapterNumber, title, summary, keyEvents, characters (string[]), foreshadowing (string)
  implication: Type interface needs expansion to carry all ChapterPlanDto fields

- timestamp: 2026-04-08T00:02:30Z
  checked: ChapterPlanDto.java backend DTO
  found: All 20 entity fields present plus extension fields (volumeTitle, volumeNumber, hasContent, chapterId, wordCount)
  implication: Backend already returns all fields; frontend just ignores them

- timestamp: 2026-04-08T00:03:00Z
  checked: setChapterPlan calls in VolumeTree.vue and CreationCenter.vue
  found: Only maps: id, volumeId, projectId, chapterNumber, title, summary, keyEvents, characters (from newCharacters split), foreshadowing (from foreshadowingSetup)
  implication: Data loss happens at setChapterPlan call - most fields not passed

- timestamp: 2026-04-08T00:03:30Z
  checked: ChapterUpdateRequest.java
  found: Only supports: title, content, volumeId, status, locked - no chapter plan fields
  implication: No backend update API for chapter plan fields. Need to create one.

- timestamp: 2026-04-08T00:20:00Z
  checked: Build and test after all changes
  found: Backend compiles clean, all 97 tests pass. Frontend has zero new TS errors (all pre-existing).
  implication: Implementation is sound

## Resolution

root_cause: ChapterPlanDrawer.vue only displayed 6 of the 20+ fields available in NovelChapterPlan/ChapterPlanDto. The ChapterPlan TypeScript interface only defined a subset (id, volumeId, projectId, chapterNumber, title, summary, keyEvents, characters, foreshadowing), and setChapterPlan() callers only passed those subset fields. The drawer used w-[400px] which was too narrow. No backend update API existed for chapter plan fields.
fix: Created full-stack solution: (1) New ChapterPlanUpdateRequest DTO and updateChapterPlan endpoint/service method in backend, (2) Expanded ChapterPlan TS interface with all fields, (3) Updated setChapterPlan callers to pass all fields, (4) Rewrote drawer with tab-based layout showing all fields across 5 sections (basic info, plot, scene, foreshadow, character), increased width to 640px
verification: Backend compiles and tests pass. Frontend type-checks with no new errors.
files_changed: [ChapterPlanUpdateRequest.java, ChapterService.java, ChapterController.java, project.ts, chapter.ts, VolumeTree.vue, CreationCenter.vue, ChapterPlanDrawer.vue]
