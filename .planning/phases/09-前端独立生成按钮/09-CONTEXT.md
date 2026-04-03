# Phase 9: 前端独立生成按钮 - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

在 WorldSetting.vue 的地理环境、力量体系、阵营势力三个子模块区域各增加独立「AI 生成」按钮。点击后调用 Phase 7 创建的独立生成 REST 接口（generate-geography / generate-power-system / generate-faction），轮询任务状态直到完成，然后刷新对应模块的树/列表数据。生成过程中按钮显示 loading 状态、区域 disabled。不涉及后端改动。

</domain>

<decisions>
## Implementation Decisions

### 按钮位置与样式
- **D-01:** AI 生成按钮放在各模块 header 右侧，与蓝色「添加」按钮并排。三个模块（GeographyTree、PowerSystemSection、FactionTree）各自的 header 区域都需新增此按钮
- **D-02:** 按钮使用绿色渐变 + Sparkles 图标，与页面顶部的 AI 生成按钮视觉风格一致，让用户一眼识别为 AI 操作
- **D-03:** 页面顶部的现有「AI生成」按钮（整体生成）保留不变，文案不变

### Loading 状态与交互
- **D-04:** 独立生成中，该模块按钮显示 spinner + 文字变为「生成中...」，模块内树/列表进入 disabled 状态（不可操作）
- **D-05:** 三个模块的 AI 生成互斥 — 同一时间只能有一个模块在生成。如果某模块正在生成，其他两个模块的 AI 生成按钮 disabled
- **D-06:** 独立生成期间，页面顶部的整体「AI生成」按钮也 disabled（避免与独立生成冲突）

### 任务恢复
- **D-07:** 每个模块使用独立的 localStorage key 恢复生成状态，格式：`ai-factory-{module}-generate-{projectId}`（module = geography / powerSystem / faction）
- **D-08:** 页面刷新后仅恢复对应模块的生成状态和轮询，其他模块正常可用
- **D-09:** 复用现有 pollTaskStatus 逻辑（3 秒间隔，60 次上限，10 分钟超时判断）

### API 客户端
- **D-10:** 3 个独立生成 API 方法放在 worldview.ts 中，与 generateWorldviewAsync 并列：
  - `generateGeographyAsync(projectId)` → `POST /api/novel/{projectId}/worldview/generate-geography`
  - `generatePowerSystemAsync(projectId)` → `POST /api/novel/{projectId}/worldview/generate-power-system`
  - `generateFactionAsync(projectId)` → `POST /api/novel/{projectId}/worldview/generate-faction`

### Claude's Discretion
- 轮询逻辑是否提取为 composable（useTaskPolling）或保留在各组件内
- 错误提示的具体文案（含依赖校验失败的提示，后端已返回明确中文消息）
- 各模块生成成功后的 toast 提示文案

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 后端 API（Phase 7 产物，已完成）
- `ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java` — 3 个独立生成端点：generate-geography / generate-power-system / generate-faction，返回 `{ taskId, message }`
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/GeographyTaskStrategy.java` — 地理环境独立生成策略
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/PowerSystemTaskStrategy.java` — 力量体系独立生成策略
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java` — 阵营势力独立生成策略（含依赖校验）

### 前端主要修改目标
- `ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue` — 主页面，管理整体/独立生成状态，包含 pollTaskStatus、localStorage 恢复逻辑
- `ai-factory-frontend/src/views/Project/Detail/components/GeographyTree.vue` — 地理环境树组件，header 需加 AI 生成按钮
- `ai-factory-frontend/src/views/Project/Detail/components/PowerSystemSection.vue` — 力量体系组件，header 需加 AI 生成按钮
- `ai-factory-frontend/src/views/Project/Detail/components/FactionTree.vue` — 势力阵营树组件，header 需加 AI 生成按钮

### 前端 API 与任务基础设施
- `ai-factory-frontend/src/api/worldview.ts` — 需加 3 个独立生成 API 方法
- `ai-factory-frontend/src/api/task.ts` — TaskDto 接口 + getTaskStatus 方法（轮询用）

### 前置 Phase 上下文
- `.planning/phases/07-api/07-CONTEXT.md` — 独立生成 API 端点设计、依赖校验模式、任务架构

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorldSetting.vue` 中的 `pollTaskStatus(taskId, maxAttempts, interval)`: 完整的轮询逻辑，可直接复用于独立生成
- `WorldSetting.vue` 中的 `saveGeneratingTask / clearGeneratingTask / restoreGeneratingState`: localStorage 状态管理模式，可按模块复制
- 三个组件都暴露 `refresh()` 方法，生成完成后直接调用即可刷新数据
- `GeographyTree / FactionTree / PowerSystemSection` 都接受 `:disabled` prop 控制交互禁用

### Established Patterns
- Section header 布局: flex justify-between，左侧标题 + 右侧操作按钮（蓝色 xs 识别的「添加」按钮）
- AI 生成按钮样式: 绿色渐变 `bg-gradient-to-r from-green-500 to-teal-500` + Sparkles 图标
- 全局生成遮罩: `v-if="generating"` absolute inset-0 z-50 backdrop-blur
- Task 轮询: getTaskStatus → 3s interval → 60 max → completed/failed/cancelled
- API 方法命名: `generate{Module}Async(projectId)` 返回 `Promise<{ taskId, message }>`

### Integration Points
- GeographyTree / PowerSystemSection / FactionTree 各自 emit 或通过 ref 调用 refresh()
- WorldSetting.vue 统一管理所有生成状态（整体 + 3 个独立），通过 ref 控制各组件 disabled
- 阵营势力的依赖校验失败由后端返回 Result.error("请先生成地理环境数据")，前端 catch 后 toast 展示

</code_context>

<specifics>
## Specific Ideas

- 现有整体生成使用全页遮罩，独立生成不应遮盖全页 — 仅 disabled 对应模块区域
- 三个模块的 AI 生成按钮视觉应与顶部按钮一致（绿色渐变），与蓝色「添加」按钮形成区分
- 阵营势力的依赖错误提示由后端已提供明确中文消息，前端直接展示即可

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 09-前端独立生成按钮*
*Context gathered: 2026-04-03*
