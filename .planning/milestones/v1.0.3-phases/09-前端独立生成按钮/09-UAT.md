---
status: testing
phase: 09-前端独立生成按钮
source: 09-01-PLAN.md (no SUMMARY.md)
started: 2026-04-03T00:00:00Z
updated: 2026-04-03T00:00:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

number: 1
name: AI生成 Button Visibility
expected: |
  Navigate to a project's worldview settings page (世界设定). In each of the 3 sub-module sections — 地理环境 (Geography), 力量体系 (PowerSystem), 势力阵营 (Faction) — there is a green gradient "AI生成" button with a Sparkles icon visible next to the blue "Add" button in the section header.
awaiting: user response

## Tests

### 1. AI生成 Button Visibility
expected: Each of the 3 sections (地理环境, 力量体系, 势力阵营) shows a green gradient "AI生成" button with Sparkles icon next to the blue Add button in the section header.
result: [pending]

### 2. Independent Generation — Geography
expected: Click the "AI生成" button in the 地理环境 section header. The button changes to show a spinner + "生成中..." text. On completion, only the geography tree data refreshes and a success toast "地理环境生成成功" appears. The button returns to its normal state.
result: [pending]

### 3. Independent Generation — PowerSystem
expected: Click the "AI生成" button in the 力量体系 section header. The button changes to show a spinner + "生成中..." text. On completion, only the power system list refreshes and a success toast "力量体系生成成功" appears. The button returns to its normal state.
result: [pending]

### 4. Independent Generation — Faction
expected: Click the "AI生成" button in the 势力阵营 section header. The button changes to show a spinner + "生成中..." text. On completion, only the faction tree refreshes and a success toast "势力阵营生成成功" appears. The button returns to its normal state.
result: [pending]

### 5. Mutual Exclusion — Other Modules Disabled
expected: While one module is generating (e.g. geography), the other two modules' "AI生成" buttons are disabled (grayed out, reduced opacity). The module's tree/list area is also disabled (cannot add/edit/delete nodes). The top-level "AI生成" button is also disabled.
result: [pending]

### 6. Dependency Validation Error
expected: Click the "AI生成" button on a module that has unmet prerequisites (e.g. faction generation with no geography data). A single toast message appears with the backend's Chinese error message (e.g. "请先生成地理环境数据"). No double-toast occurs.
result: [pending]

### 7. Page Refresh Recovery
expected: Start a module generation (e.g. geography). Refresh the browser page. The generation continues polling (button still shows spinner). On completion, the data refreshes and the success toast appears. The button returns to normal state.
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0

## Gaps

[none yet]
