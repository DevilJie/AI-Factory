---
status: testing
phase: 02-api
source: [02-01-SUMMARY.md, 02-02-SUMMARY.md]
started: 2026-04-03T12:10:00Z
updated: 2026-04-03T12:10:00Z
---

## Current Test

number: 1
name: FactionService 接口方法完整
expected: |
  FactionService.java 包含 9 个方法：getTreeByProjectId、listByProjectId、addFaction、updateFaction、deleteFaction、saveTree、deleteByProjectId、buildFactionText、fillForces
awaiting: user response

## Tests

### 1. FactionService 接口方法完整
expected: FactionService.java 包含 9 个方法：getTreeByProjectId、listByProjectId、addFaction、updateFaction、deleteFaction、saveTree、deleteByProjectId、buildFactionText、fillForces
result: [pending]

### 2. 级联删除覆盖 4 表
expected: FactionServiceImpl 的 deleteFaction 方法能级联删除 novel_faction 及 3 个关联表数据，包含双向关系清理（faction_id 和 target_faction_id）
result: [pending]

### 3. fillForces 文本构建器
expected: fillForces 方法能将势力树转为文本，type 使用中文标签（ally→正派、hostile→反派、neutral→中立），包含力量体系名称和所属地区
result: [pending]

### 4. FactionController 14 个端点
expected: FactionController 包含 14 个 REST 端点：5 个势力树 CRUD + 3x3 关联表管理（region/character/relation 各含增删查），路由前缀 /api/novel/{projectId}/faction
result: [pending]

### 5. 后端编译通过
expected: mvn compile 成功编译，FactionService、FactionServiceImpl、FactionController 无错误
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps

[none yet]
