# Phase 7: 独立生成 API + 依赖校验 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 07-api
**Areas discussed:** 数据清理策略, API 端点路径设计, 任务执行模式, 依赖校验交互体验

---

## 数据清理策略

| Option | Description | Selected |
|--------|-------------|----------|
| 先清后生 | 生成前先删除旧数据再重新生成，与整体生成行为一致 | ✓ |
| 不清直接插入 | AI 生成新数据直接插入，可能新旧混合 | |
| 归档后再生 | 旧数据移到备份，再生成新数据，复杂度高 | |

**User's choice:** 先清后生 (Recommended)
**Notes:** 与现有 WorldviewTaskStrategy 的 check_existing 行为一致

### 追问：清理范围

| Option | Description | Selected |
|--------|-------------|----------|
| 全部清除 | 包括用户手动添加的势力-人物关联 | ✓ |
| 保留手动关联 | 只清除 AI 生成的数据，实现复杂 | |

**User's choice:** 全部清除 (Recommended)
**Notes:** 现有级联删除逻辑已覆盖，用户可生成后重新调整

---

## API 端点路径设计

| Option | Description | Selected |
|--------|-------------|----------|
| 扩展 WorldviewController | 在 /worldview 下增加 generate-geography/power-system/faction | ✓ |
| 各模块 Controller | 分别在 ContinentRegionController/PowerSystemController/FactionController 中 | |

**User's choice:** 扩展 WorldviewController (Recommended)
**Notes:** 与现有 generate-async 同级，语义清晰

---

## 任务执行模式

| Option | Description | Selected |
|--------|-------------|----------|
| 复用 async task 模式 | 3 个新 TaskStrategy，通过 TaskService 异步执行 | ✓ |
| 同步调用 | 不经过 task 系统，直接等待 AI 返回 | |

**User's choice:** 复用 async task 模式 (Recommended)
**Notes:** 前端可复用现有轮询逻辑，与整体生成行为一致

---

## 依赖校验交互体验

| Option | Description | Selected |
|--------|-------------|----------|
| 同步拒绝 + 明确提示 | Controller 层校验，不创建 task，直接返回错误 | ✓ |
| Task 内校验 + 失败状态 | 创建 task 后 Strategy 层校验，task 状态变 failed | |

**User's choice:** 同步拒绝 + 明确提示 (Recommended)
**Notes:** 不浪费 task 资源，前端直接展示错误

---

## Claude's Discretion

- 3 个 TaskStrategy 的具体步骤划分
- 上下文序列化实现细节
- 解析方法复用策略（直接调用 vs 提取公共工具）
- 错误提示文案最终措辞

## Deferred Ideas

None — discussion stayed within phase scope
