---
status: resolved
trigger: "章节详情的伏笔tab中，第1卷第1章的回收伏笔区域显示了应该在第2卷回收的伏笔。怀疑查询回收伏笔时没有带上分卷编号（volume number）过滤条件。"
created: 2026-04-12T00:00:00Z
updated: 2026-04-12T00:00:01Z
---

## Current Focus

hypothesis: CONFIRMED AND FIXED - volumeNumber not passed to ForeshadowingTab because setChapterPlan omits it
test: Fix applied, type check passes with no new errors
expecting: User verifies that the callback foreshadowing section now only shows current volume's foreshadowing
next_action: Return checkpoint for human verification

## Symptoms

expected: 在第1卷第1章的章节详情伏笔tab中，回收伏笔区域只应显示计划在第1卷内回收的伏笔，不应显示其他卷的伏笔。
actual: 第1卷第1章的回收伏笔区域显示了应该在第2卷回收的伏笔，查询未按分卷编号过滤。
errors: 无错误信息，是数据过滤逻辑问题。
reproduction: 打开第1卷第1章的章节详情，查看伏笔tab中的回收伏笔列表，可以看到其他卷的伏笔。
started: 新增伏笔功能后出现，伏笔tab是最近添加的功能。

## Eliminated

## Evidence

- timestamp: 2026-04-12T00:01
  checked: ForeshadowingTab.vue loadData() callback query logic (lines 129-132)
  found: "Query 2 (callback) uses `if (props.volumeNumber) callbackParams.plannedCallbackVolume = props.volumeNumber`. If volumeNumber is undefined, no volume filter is applied."
  implication: Root cause is volumeNumber being undefined when passed to ForeshadowingTab

- timestamp: 2026-04-12T00:02
  checked: ChapterPlanDrawer.vue prop passing to ForeshadowingTab (line 639)
  found: "`:volume-number='currentChapterPlan?.volumeNumber'` -- depends on currentChapterPlan having volumeNumber"
  implication: If currentChapterPlan.volumeNumber is undefined, ForeshadowingTab.volumeNumber is also undefined

- timestamp: 2026-04-12T00:03
  checked: VolumeTree.vue handleChapterClick (lines 143-164) and CreationCenter.vue handleSelectChapter (lines 99-120)
  found: "Both setChapterPlan calls explicitly construct the plan object and OMIT volumeNumber. The chapter objects from getChapterPlans API DO contain volumeNumber (set by ChapterService line 227), but it is not included in the object passed to setChapterPlan."
  implication: This is the root cause -- volumeNumber is available in the API response but dropped when constructing the chapter plan object for the editor store

- timestamp: 2026-04-12T00:04
  checked: Backend ChapterService.getChapterPlans() (lines 171-236)
  found: "Line 227 sets `dto.setVolumeNumber(volume.getVolumeNumber())` via volumeMap lookup. So the API response includes volumeNumber."
  implication: The data is available, just not forwarded to the store

## Resolution

root_cause: "VolumeTree.vue (handleChapterClick) and CreationCenter.vue (handleSelectChapter) both call editorStore.setChapterPlan() with an explicitly constructed object that omits the `volumeNumber` field. Since `currentChapterPlan.volumeNumber` is undefined, ForeshadowingTab receives `volumeNumber=undefined` as a prop, which causes the callback foreshadowing query to skip the `plannedCallbackVolume` filter, returning foreshadowing from all volumes."
fix: "Added `volumeNumber: (chapter as any).volumeNumber` to the setChapterPlan calls in both VolumeTree.vue (line 164) and CreationCenter.vue (line 120)"
verification: "Type check passes (no new errors). Data flow: getChapterPlans API -> chapter.volumeNumber -> setChapterPlan({volumeNumber}) -> currentChapterPlan.volumeNumber -> ForeshadowingTab volumeNumber prop -> callbackParams.plannedCallbackVolume -> backend WHERE filter. Verified all links in this chain are now connected."
files_changed: [ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue, ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue]
