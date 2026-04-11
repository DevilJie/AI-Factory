---
phase: 16-ai-chapter-planning
verified: 2026-04-11T13:10:00Z
status: passed
score: 9/9 must-haves verified
---

# Phase 16: AI Chapter Planning Verification Report

**Phase Goal:** Inject active foreshadowing context into chapter planning prompts and parse foreshadowing tags from LLM output for persistence.
**Verified:** 2026-04-11T13:10:00+08:00
**Status:** passed
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | generateChaptersInVolume injects active foreshadowing context into the chapter planning prompt | VERIFIED | buildChapterPromptUsingTemplate() calls buildActiveForeshadowingContext() at line 988 and stores result as `foreshadowingContext` variable at line 989 |
| 2 | generateChaptersForVolume injects active foreshadowing context into the chapter planning prompt | VERIFIED | buildChapterPrompt() calls buildActiveForeshadowingContext() at line 1058 and appends to prompt StringBuilder at line 1060 |
| 3 | Foreshadowing context shows title, type, layout line, status, plant/callback locations for each active foreshadowing in current volume | VERIFIED | buildActiveForeshadowingContext() (lines 1421-1477) formats each foreshadowing with title, type, layoutType, status, and plannedCallbackVolume/plannedCallbackChapter or plantedVolume/plantedChapter |
| 4 | Only active foreshadowing (pending/in_progress) relevant to current volume is injected, not the entire project's foreshadowing | VERIFIED | Method queries plantedVolume+pending (line 1427-1428) and plannedCallbackVolume+in_progress (line 1434-1435), returns empty string if both empty (line 1438-1440) |
| 5 | parseChaptersXml extracts <fs> foreshadowing plant tags with sub-tags ft/fy/fl/fd/fc/fr from each chapter block | VERIFIED | extractForeshadowingPlants() (lines 1375-1393) uses regex to find <fs> tags, extracts ft via CDATA, fy/fl/fc/fr via plain extraction, fd via CDATA. parseChaptersXml() calls it at line 1268 and stores results with _fs_N_ prefix |
| 6 | parseChaptersXml extracts <fp> foreshadowing payoff tags with sub-tags ft/fd from each chapter block | VERIFIED | extractForeshadowingPayoffs() (lines 1400-1414) uses regex to find <fp> tags, extracts ft and fd via CDATA. parseChaptersXml() calls it at line 1279 and stores results with _fp_N_ prefix |
| 7 | Re-planning a volume deletes pending foreshadowing (plantedVolume matches current volume) before creating new ones | VERIFIED | ForeshadowingService.deletePendingForeshadowingForVolume() (lines 238-246) deletes where plantedVolume=volumeNumber AND status=pending. Called in saveVolumeChaptersToDatabase() at line 670 and saveChaptersToDatabase() at line 2308 |
| 8 | Parsed <fs> data creates novel_foreshadowing records via ForeshadowingService.createForeshadowing() | VERIFIED | Both save methods (lines 674-706 and 2312-2344) iterate _foreshadowingPlants_count, construct ForeshadowingCreateDto with title/type/description/layoutType/plantedChapter/plantedVolume/callback fields, and call foreshadowingService.createForeshadowing() |
| 9 | <fp> tags are parsed but do NOT trigger status updates on existing foreshadowing (per D-05) | VERIFIED | _fp_ keys only written in parseChaptersXml() (line 1285). No code in either save method reads _fp_ keys or calls setStatus on existing foreshadowing. ForeshadowingCreateDto only created from _fs_ plant data. Confirmed: grep for setStatus("completed") in OutlineTaskStrategy.java returns zero matches |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| OutlineTaskStrategyTest.java | Wave 0 test stubs for buildActiveForeshadowingContext and parseChaptersXml with foreshadowing tags | VERIFIED | File exists at ai-factory-backend/src/test/java/com/aifactory/service/task/impl/OutlineTaskStrategyTest.java (310 lines). Contains 6 tests using MockitoExtension + reflection. All 6 tests pass (verified via mvn test) |
| OutlineTaskStrategy.java | buildActiveForeshadowingContext() method, extractForeshadowingPlants/Payoffs(), injection into both prompt paths, persistence in both save paths | VERIFIED | File exists. ForeshadowingService injected (line 95). buildActiveForeshadowingContext() defined (line 1421). Injection in template path (line 988) and hardcoded path (line 1058). Extraction methods (lines 1375, 1400). Persistence in both save methods (lines 670-706, 2308-2344). parseIntSafe() helper (line 2350) |
| ForeshadowingService.java | deletePendingForeshadowingForVolume() method | VERIFIED | Method exists at line 238. Uses LambdaQueryWrapper with projectId + plantedVolume + status=pending filters. Returns deleted count |
| sql/foreshadowing_template_update.sql | SQL reference migration for template update with foreshadowingContext variable | VERIFIED | File exists (128 lines). Contains reference SQL for updating llm_outline_chapter_generate template with {foreshadowingContext} variable and <fs>/<fp> output format instructions. Both Approach A (new version) and Approach B (direct update) documented |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| buildChapterPromptUsingTemplate() | buildActiveForeshadowingContext() | variables.put("foreshadowingContext", ...) | WIRED | Line 988-989: context built and stored in template variables map before promptTemplateService.executeTemplate() call |
| buildChapterPrompt() | buildActiveForeshadowingContext() | prompt.append(foreshadowingContext) | WIRED | Line 1058-1060: context built and appended to StringBuilder |
| buildActiveForeshadowingContext() | ForeshadowingService.getForeshadowingList() | query with plantedVolume/plannedCallbackVolume + status filters | WIRED | Lines 1429, 1436: two queries with different filter combinations |
| parseChaptersXml() | _foreshadowingPlants / _foreshadowingPayoffs keys | regex extraction within each <o> block | WIRED | Lines 1268-1288: extractForeshadowingPlants/Payoffs called, results stored with prefixed keys |
| saveVolumeChaptersToDatabase() | ForeshadowingService.deletePendingForeshadowingForVolume() + createForeshadowing() | loop over parsed plant data after chapter save | WIRED | Line 670: delete call. Lines 674-706: loop iterating _foreshadowingPlants_count, constructing ForeshadowingCreateDto, calling createForeshadowing() |
| saveChaptersToDatabase() | ForeshadowingService.deletePendingForeshadowingForVolume() + createForeshadowing() | loop over parsed plant data after chapter save | WIRED | Line 2308: delete call. Lines 2312-2344: same loop pattern as above |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| buildActiveForeshadowingContext() | pendingPlants, pendingCallbacks | ForeshadowingService.getForeshadowingList() with ForeshadowingQueryDto | Yes -- queries by projectId + volumeNumber + status, returns List<ForeshadowingDto> from DB via MyBatis-Plus | FLOWING |
| parseChaptersXml() | _fs_N_* keys, _fp_N_* keys | extractForeshadowingPlants/Payoffs() regex on chapter XML content | Yes -- regex Pattern/Matcher on actual LLM XML output | FLOWING |
| saveVolumeChaptersToDatabase() persistence | ForeshadowingCreateDto | chapterData map from parseChaptersXml() output | Yes -- reads _fs_N_* keys, constructs DTO, calls createForeshadowing() which validates and inserts via MyBatis-Plus | FLOWING |
| saveChaptersToDatabase() persistence | ForeshadowingCreateDto | Same chapterData source | Yes -- identical pattern to above | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All 6 OutlineTaskStrategyTest tests pass | mvn test -Dtest="OutlineTaskStrategyTest" | Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS | PASS |
| Compilation succeeds | mvn compile -q | BUILD SUCCESS (implicit in test run) | PASS |
| ForeshadowingService.deletePendingForeshadowingForVolume() exists | grep in ForeshadowingService.java | Found at line 238 | PASS |
| No setStatus("completed") in foreshadowing persistence | grep in OutlineTaskStrategy.java | Zero matches | PASS (D-05 compliant) |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| AIP-01 | 16-01 | Chapter planning prompt injects active foreshadowing context (pending plants + pending callbacks) | SATISFIED | buildActiveForeshadowingContext() injected into both prompt paths. Queries pending (plantedVolume) and in_progress (plannedCallbackVolume) foreshadowing for current volume |
| AIP-02 | 16-02 | Chapter planning LLM output includes <fs>/<fp> XML tags with title, description, type, layout line sub-tags | SATISFIED | extractForeshadowingPlants() parses <fs> with ft/fy/fl/fd/fc/fr. extractForeshadowingPayoffs() parses <fp> with ft/fd. parseChaptersXml() integrates both. SQL migration documents expected output format for template path |
| AIP-03 | 16-02 | DOM parser extends to handle <fs>/<fp> tags, parse foreshadowing data and batch save to novel_foreshadowing table | SATISFIED | Both saveVolumeChaptersToDatabase() and saveChaptersToDatabase() delete pending foreshadowing (D-06) then iterate parsed <fs> data to create novel_foreshadowing records via ForeshadowingService.createForeshadowing(). Per-item try-catch prevents one bad entry from aborting batch |

No orphaned requirements found. All three requirements (AIP-01, AIP-02, AIP-03) are mapped to Phase 16 plans and each appears in a plan's `requirements` frontmatter field.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | No anti-patterns found in Phase 16 code |

No TODO/FIXME/HACK comments in modified files. No placeholder implementations. No empty return values in foreshadowing code paths. The `return null` instances in OutlineTaskStrategy.java are all in pre-existing error-handling code unrelated to Phase 16. No hardcoded empty data in foreshadowing logic.

### Human Verification Required

### 1. Template Path End-to-End Functionality

**Test:** Trigger chapter planning via the template path (buildChapterPromptUsingTemplate) with an active foreshadowing record in the database, and verify the LLM receives foreshadowing context in its prompt.
**Expected:** The generated prompt contains the structured foreshadowing list (title, type, layout line, status, locations).
**Why human:** The template path relies on the DB template `llm_outline_chapter_generate` containing the `{foreshadowingContext}` variable. The SQL migration is a reference script (all SQL commented out) and must be applied manually. The hardcoded path works immediately, but the template path requires this manual DB step. Automated testing verified the variable is injected into the code, but the DB template update is a manual operation.

### 2. LLM Output with <fs>/<fp> Tags Parses Correctly

**Test:** Generate a chapter plan where the LLM outputs <fs> and <fp> tags, and verify the foreshadowing records are created in the database.
**Expected:** novel_foreshadowing table contains new records matching the LLM output, with correct title, type, layoutType, plantedVolume, plantedChapter, and optional plannedCallbackVolume/plannedCallbackChapter.
**Why human:** Requires running the full Spring Boot application with a real LLM provider, database, and Redis. The parsing logic is verified by unit tests, but the end-to-end flow with real LLM output cannot be tested programmatically without external services.

### Gaps Summary

No gaps found. All 9 observable truths are verified at all four levels (exists, substantive, wired, data flowing). All 3 requirements (AIP-01, AIP-02, AIP-03) are satisfied with implementation evidence. All 6 unit tests pass. All 6 key links are wired. No anti-patterns detected. Two items flagged for human verification are about manual DB template update and end-to-end integration testing with a live LLM -- neither is a code gap.

---

_Verified: 2026-04-11T13:10:00+08:00_
_Verifier: Claude (gsd-verifier)_
