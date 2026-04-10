---
status: verifying
trigger: "ChapterPlanDrawer.vue 页面，点击规划角色列表时，无论选中第一卷第一章还是第二卷第一章，请求的后端接口都是同一个 URL（chapters/7/characters），章节 ID 没有随选中章节变化。"
created: 2026-04-10T00:00:00Z
updated: 2026-04-10T00:05:00Z
---

## Current Focus

hypothesis: CONFIRMED - ChapterService.getChapterPlans() used chapterNumber to match plan->chapter instead of chapterPlanId, causing wrong chapterId for multi-volume projects
test: Applied fix and verified compilation
expecting: Each chapter plan now gets its correct chapterId via chapterPlanId-based lookup
next_action: Request human verification

## Symptoms

expected: 点击不同卷不同章节时，应该请求对应章节ID的接口，如第一卷第一章请求 /chapters/X/characters，第二卷第一章请求 /chapters/Y/characters，X != Y
actual: 点击第一卷第一章和第二卷第一章，都请求同一个接口 http://localhost:5174/api/novel/35/chapters/7/characters
errors: 无报错，只是章节ID不变
reproduction: 打开 ChapterPlanDrawer，点击第一卷第一章查看规划角色列表，再点击第二卷第一章查看规划角色列表，观察网络请求
started: 一直存在此问题，可能是初始实现就有

## Eliminated

## Evidence

- timestamp: 2026-04-10T00:01
  checked: ChapterPlanDrawer.vue line 145 - chapterId computed from currentChapterPlan.value?.chapterId
  found: chapterId comes from editor store's currentChapterPlan, which is set by VolumeTree.vue handleChapterClick
  implication: The wrong value originates from the backend data returned by getChapterPlans API

- timestamp: 2026-04-10T00:02
  checked: VolumeTree.vue line 164 - chapterId: (chapter as any).chapterId
  found: chapterId is read directly from the chapter plan data returned by getChapterPlans API
  implication: Backend getChapterPlans returns wrong chapterId for some plans

- timestamp: 2026-04-10T00:03
  checked: ChapterService.getChapterPlans() lines 229-242
  found: Matching uses ch.getChapterNumber().equals(plan.getChapterNumber()) instead of chapterPlanId
  implication: This is the root cause - chapterNumber-based matching is ambiguous when plans share the same number across volumes

- timestamp: 2026-04-10T00:04
  checked: ChapterService.convertChapterPlanToDto() lines 583-593
  found: This method correctly uses Chapter::getChapterPlanId for matching
  implication: The fix pattern already exists in the codebase

- timestamp: 2026-04-10T00:05
  checked: ChapterPersistenceService.createChapterFromPlan() line 57
  found: Chapter entity has chapterPlanId field set to plan.getId(), confirming chapterPlanId-based matching is reliable
  implication: Every generated chapter has a chapterPlanId foreign key that can be used for exact matching

- timestamp: 2026-04-10T00:06
  checked: Backend compilation after fix
  found: mvn compile succeeds with no errors
  implication: Fix is syntactically correct

## Resolution

root_cause: ChapterService.getChapterPlans() matched Chapter to ChapterPlan using chapterNumber instead of chapterPlanId. When different volume chapter plans share the same chapterNumber (e.g. volume 1 chapter 1 and volume 2 chapter 1 both have chapterNumber=1), findFirst() always returned the same Chapter for both plans, causing the wrong chapterId to be returned for the second plan.
fix: Replaced chapterNumber-based stream matching with a Map<Long, Chapter> indexed by chapterPlanId, providing O(1) exact lookup. Also improved hasContent check to verify actual content exists rather than just matching chapter existence.
verification: Backend compiles successfully. Awaiting human verification in real workflow.
files_changed: [ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java]
