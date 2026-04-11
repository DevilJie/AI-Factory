# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## foreshadowing-volume-filter-missing — volumeNumber omitted from setChapterPlan calls causing cross-volume foreshadowing leak
- **Date:** 2026-04-12
- **Error patterns:** volumeNumber, foreshadowing, callback, filter, volume, plannedCallbackVolume, cross-volume, wrong volume data
- **Root cause:** VolumeTree.vue and CreationCenter.vue both called editorStore.setChapterPlan() with an explicitly constructed object that omitted the volumeNumber field. The chapter objects from the API response do contain volumeNumber, but it was dropped when building the object passed to setChapterPlan. This caused ForeshadowingTab to receive volumeNumber=undefined as a prop, skipping the plannedCallbackVolume filter in the callback query and returning foreshadowing from all volumes instead of only the current volume.
- **Fix:** Added `volumeNumber: (chapter as any).volumeNumber` to the setChapterPlan calls in both VolumeTree.vue and CreationCenter.vue
- **Files changed:** ai-factory-frontend/src/views/Project/Detail/creation/VolumeTree.vue, ai-factory-frontend/src/views/Project/Detail/creation/CreationCenter.vue
---

