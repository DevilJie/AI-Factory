# Phase 7: 独立生成 API + 依赖校验 - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

提供 3 个独立生成 REST 接口（地理环境、力量体系、阵营势力），各调用 Phase 6 创建的独立提示词模板进行 AI 生成并入库。阵营势力生成前需校验地理和力量体系依赖。不涉及前端改动（Phase 9）和原有逻辑重构（Phase 8）。

</domain>

<decisions>
## Implementation Decisions

### 数据清理策略
- **D-01:** 独立重新生成时采用"先清后生"策略 — 先删除该项目对应模块的旧数据，再执行 AI 生成，与现有世界观整体生成的 check_existing 行为一致
- **D-02:** 阵营势力重新生成时全部清除（含用户手动添加的势力-人物关联），生成后用户可重新调整。现有级联删除逻辑已覆盖关联表清理

### API 端点路径设计
- **D-03:** 3 个独立生成端点扩展 WorldviewController，与现有 `generate-async` 同级，路径为：
  - `POST /api/novel/{projectId}/worldview/generate-geography`
  - `POST /api/novel/{projectId}/worldview/generate-power-system`
  - `POST /api/novel/{projectId}/worldview/generate-faction`

### 任务执行模式
- **D-04:** 复用现有 TaskService + AsyncTaskExecutor + TaskStrategy 架构，创建 3 个新 TaskStrategy 实现类：
  - `GeographyTaskStrategy`（taskType = `"geography"`）— 调用 `llm_geography_create` 模板，解析 `<g>` XML 入库
  - `PowerSystemTaskStrategy`（taskType = `"power_system"`）— 调用 `llm_power_system_create` 模板，解析 `<p>` 数据入库
  - `FactionTaskStrategy`（taskType = `"faction"`）— 调用 `llm_faction_create` 模板，注入依赖上下文，解析 `<f>` XML 入库
- **D-05:** 前端可复用现有 task 轮询逻辑（GET /api/task/{taskId}/status）

### 依赖校验交互体验
- **D-06:** 阵营势力生成的依赖校验放在 Controller 层同步执行，不创建 task：
  - 检查 `novel_continent_region` 是否有数据 → 无则返回 `Result.error("请先生成地理环境数据")`
  - 检查 `novel_power_system` 是否有数据 → 无则返回 `Result.error("请先生成力量体系数据")`
  - 两者都有 → 创建 task 并异步执行
- **D-07:** 地理环境和力量体系独立生成无前置依赖，直接创建 task 执行

### Claude's Discretion
- 3 个 TaskStrategy 的具体步骤划分（createSteps / executeStep）
- 上下文序列化实现细节（从 DB 读取已入库数据并构建 geographyContext/powerSystemContext）
- 错误提示文案的最终措辞
- Strategy 内部是否复用 WorldviewTaskStrategy 中的解析方法还是提取公共工具方法

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 现有生成架构
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` — 当前世界观生成策略，包含完整的地理环境 DOM 解析（saveGeographyRegionsFromXml）、力量体系解析（savePowerSystems）、阵营势力 DOM 解析（saveFactionsFromXml）实现。Phase 7 的 3 个新 Strategy 复用这些解析逻辑
- `ai-factory-backend/src/main/java/com/aifactory/service/AsyncTaskExecutor.java` — 异步任务执行器，通过 taskType 解析到对应 Strategy bean
- `ai-factory-backend/src/main/java/com/aifactory/service/task/TaskStrategy.java` — Strategy 接口定义（getTaskType, createSteps, executeStep, StepConfig, StepResult, TaskContext）

### 控制器层
- `ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java` — 现有世界观控制器，`generate-async` 端点模式参考（build config → createTask → return taskId）
- `ai-factory-backend/src/main/java/com/aifactory/response/Result.java` — API 响应封装（Result.ok/error/userErrorParam）

### 提示词基础设施
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptTemplateService.java` — executeTemplate(code, variables) 核心接口
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/impl/PromptTemplateServiceImpl.java` — Hutool StrUtil.format 变量替换实现

### 上下文构建
- `ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptContextBuilder.java` — 上下文构建服务，已有 buildWorldviewContext() 可参考
- `ai-factory-backend/src/main/java/com/aifactory/service/ContinentRegionService.java` — 地理环境服务，fillGeography() 可用于构建地理上下文
- `ai-factory-backend/src/main/java/com/aifactory/service/PowerSystemService.java` — 力量体系服务，buildPowerSystemConstraint() 可用于构建力量体系上下文

### 数据服务
- `ai-factory-backend/src/main/java/com/aifactory/service/FactionService.java` — 势力服务，saveTree() + 级联删除
- `ai-factory-backend/src/main/java/com/aifactory/service/TaskService.java` — 任务创建服务

### 数据库迁移
- `ai-factory-backend/src/main/resources/db/migration/V4__independent_prompt_templates.sql` — Phase 6 创建的 3 个独立提示词模板 + 精简后的世界观模板

### Phase 6 上下文
- `.planning/phases/06-独立提示词模板/06-CONTEXT.md` — Phase 6 决策记录，特别是模板变量设计和上下文注入格式

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorldviewTaskStrategy` 中的解析方法（saveGeographyRegionsFromXml、savePowerSystems、saveFactionsFromXml）: 完整的 AI 输出解析入库逻辑，3 个新 Strategy 可直接复用或提取为公共方法
- `TaskService.createTask()`: 异步任务创建基础设施，新 Strategy 注册后即可使用
- `AsyncTaskExecutor`: 通过 taskType 自动路由到对应 Strategy，新 Strategy 只需实现接口并注册为 Spring Bean
- `PromptTemplateService.executeTemplate()`: 模板执行基础设施，新模板按 templateCode 查询即可
- `ContinentRegionService.fillGeography()` / `PowerSystemService.buildPowerSystemConstraint()`: 已有构建上下文文本的方法

### Established Patterns
- TaskStrategy 模式: getTaskType() 返回字符串 → AsyncTaskExecutor 通过 bean name convention 匹配
- 任务步骤: createSteps() 返回 List<StepConfig>，executeStep() 按步骤名路由执行
- API 响应: Result<T> 封装，code=0 成功，code=-1 错误，code=30001 参数错误
- 清理模式: WorldviewTaskStrategy.check_existing 中先删旧数据再生成

### Integration Points
- 3 个新 TaskStrategy 需注册为 Spring Bean（@Service 注解即可被 AsyncTaskExecutor 发现）
- WorldviewController 增加 3 个新端点，复用现有的 project 验证和 config 构建逻辑
- FactionTaskStrategy 的依赖校验在 Controller 层完成（同步），不在 Strategy 中
- Phase 8 的 WorldviewTaskStrategy 重构将组合调用这 3 个独立 Strategy

</code_context>

<specifics>
## Specific Ideas

- 用户说"其实就是把原来耦合在世界观生成里面的全部拆出来了" — 本质是解耦，保持与现有架构高度一致
- 独立生成的解析逻辑直接复用 WorldviewTaskStrategy 中已验证的代码，不重新实现

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 07-api*
*Context gathered: 2026-04-03*
