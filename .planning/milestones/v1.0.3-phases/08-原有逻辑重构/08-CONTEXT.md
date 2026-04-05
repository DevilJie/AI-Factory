# Phase 8: 原有逻辑重构 - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

将 WorldviewTaskStrategy 整体生成流程从"单次 AI 调用 + 全量 XML 解析"重构为"依次调用三个独立生成策略（地理环境、力量体系、阵营势力）+ 精简模板生成剩余内容（世界类型/背景/时间线/规则）"。对外行为不变 — 前端调用 generate-async 端点后仍收到完整世界观数据。不涉及前端改动和新的 API 端点。

</domain>

<decisions>
## Implementation Decisions

### 组合调用方式
- **D-01:** 注入 GeographyTaskStrategy / PowerSystemTaskStrategy / FactionTaskStrategy 为 Spring Bean，直接调用其内部方法（非通过 TaskService 创建子任务）
- **D-02:** 为三个独立 Strategy 各添加一个 public 的完整流程方法（如 `executeFullFlow(projectId, config)`），内部编排 clean → generate → save 三步，避免 WorldviewTaskStrategy 直接调用 private 方法
- **D-03:** WorldviewTaskStrategy 的 `check_existing` 步骤仅删除旧世界观记录本身（`novel_worldview`）和关联表（`novel_worldview_power_system`），模块数据清理由各独立 Strategy 的 clean 步骤负责

### 步骤结构设计
- **D-04:** 9 步骤结构，每模块独立步骤可见于进度报告：
  1. `check_existing` — 删除旧世界观记录 + 关联表（不含模块数据）
  2. `clean_geography` — 调用 GeographyTaskStrategy 清理旧地理环境
  3. `generate_geography` — 调用 GeographyTaskStrategy 生成地理环境（含保存入库）
  4. `clean_power_system` — 调用 PowerSystemTaskStrategy 清理旧力量体系
  5. `generate_power_system` — 调用 PowerSystemTaskStrategy 生成力量体系（含保存入库）
  6. `clean_faction` — 调用 FactionTaskStrategy 清理旧阵营势力
  7. `generate_faction` — 调用 FactionTaskStrategy 生成阵营势力（含保存入库，需注入地理+力量体系上下文）
  8. `generate_core` — 调用精简后 `llm_worldview_create` 模板生成 `<t>/<b>/<l>/<r>` 核心世界观
  9. `save_core` — 解析核心世界观 AI 响应，保存 `novel_worldview` 记录 + 建立 `novel_worldview_power_system` 关联
- **D-05:** FactionTaskStrategy 生成时的依赖上下文（geographyContext / powerSystemContext）在 WorldviewTaskStrategy 中构建（调用 ContinentRegionService.buildGeographyText 和 PowerSystemService.buildPowerSystemConstraint），传入 Strategy 方法

### 结果汇总方式
- **D-06:** 所有模块入库完成后，从 DB 回查全部数据（世界观 + 地理环境 + 力量体系 + 阵营势力），组装为完整的 `NovelWorldview` 对象作为最终步骤的结果
- **D-07:** 使用现有的 `NovelWorldviewMapper.selectOne` + 各 Service 的 list/fill 方法回查拼装，确保前端收到与 DB 一致的数据

### 重复代码清理
- **D-08:** 提取 `WorldviewXmlParser` 工具类（`com.aifactory.common` 包），包含：
  - 地理环境 DOM 解析：`parseGeographyXml(String aiResponse, Long projectId)` → `List<NovelContinentRegion>`
  - 势力 DOM 解析：`parseFactionXml(String aiResponse, Long projectId)` → 解析结果包含 `List<NovelFaction>` + `List<PendingFactionAssociations>`
  - 三级名称匹配：`findRegionIdByName(Long projectId, String name)` 和 `findPowerSystemIdByName(Long projectId, String name)`
  - 中文→英文映射：`mapFactionType(String)` 和 `mapRelationType(String)`
  - Record 类型：`PendingFactionAssociations` 和 `PendingRelation`
- **D-09:** WorldviewTaskStrategy、GeographyTaskStrategy、FactionTaskStrategy 均改为调用 WorldviewXmlParser，删除各自的重复 DOM 解析和名称匹配代码
- **D-10:** PowerSystemTaskStrategy 使用 Jackson XmlParser（非 DOM 解析），不受此工具类影响

### Claude's Discretion
- WorldviewXmlParser 的具体方法签名和返回类型设计
- 各 Strategy 的 executeFullFlow 方法是否返回具体结果对象或仅返回成功/失败
- 错误处理细节（某模块生成失败时的回滚策略）

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 主要重构目标
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` — 本 Phase 的核心重构对象，当前包含全部生成+解析+保存逻辑（约 920 行）

### 注入的独立策略（Phase 7 产物）
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/GeographyTaskStrategy.java` — 地理环境独立生成策略，需添加 executeFullFlow 方法
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/PowerSystemTaskStrategy.java` — 力量体系独立生成策略，需添加 executeFullFlow 方法
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/FactionTaskStrategy.java` — 阵营势力独立生成策略，需添加 executeFullFlow 方法

### 任务执行基础设施
- `ai-factory-backend/src/main/java/com/aifactory/service/task/TaskStrategy.java` — Strategy 接口（getTaskType, createSteps, executeStep, StepConfig, StepResult, TaskContext）
- `ai-factory-backend/src/main/java/com/aifactory/service/AsyncTaskExecutor.java` — 异步任务执行器，通过 strategyMap 查找 Strategy bean

### Controller 层
- `ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java` — generate-async 端点（不变），参考 config 构建和 task 创建模式

### 上下文构建服务（faction 依赖注入需要）
- `ai-factory-backend/src/main/java/com/aifactory/service/ContinentRegionService.java` — buildGeographyText(projectId) 用于构建地理环境上下文
- `ai-factory-backend/src/main/java/com/aifactory/service/PowerSystemService.java` — buildPowerSystemConstraint(projectId) 用于构建力量体系上下文
- `ai-factory-backend/src/main/java/com/aifactory/service/FactionService.java` — deleteByProjectId(projectId) 用于清理旧势力数据

### 数据层
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelWorldviewMapper.java` — 世界观查询
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelWorldviewPowerSystemMapper.java` — 世界观-力量体系关联
- `ai-factory-backend/src/main/java/com/aifactory/common/XmlParser.java` — 现有 Jackson XML 解析器（力量体系用）

### 前置 Phase 上下文
- `.planning/phases/06-独立提示词模板/06-CONTEXT.md` — 模板变量设计、精简后模板结构（仅 `<t>/<b>/<l>/<r>`）
- `.planning/phases/07-api/07-CONTEXT.md` — 独立策略架构设计、依赖校验模式、API 端点路径

### 提示词模板
- DB: `ai_prompt_template` WHERE `template_code = 'llm_worldview_create'` — 精简后的世界观模板（Phase 06 已更新，仅生成核心字段）
- DB: `ai_prompt_template` WHERE `template_code = 'llm_geography_create'` — 地理环境独立模板
- DB: `ai_prompt_template` WHERE `template_code = 'llm_power_system_create'` — 力量体系独立模板
- DB: `ai_prompt_template` WHERE `template_code = 'llm_faction_create'` — 阵营势力独立模板（含 geographyContext/powerSystemContext 变量）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GeographyTaskStrategy`: clean/generate/save 三步流程完整实现，可直接复用
- `PowerSystemTaskStrategy`: clean/generate/save 三步流程完整实现，可直接复用
- `FactionTaskStrategy`: clean/generate/save 三步流程完整实现，含依赖上下文注入和两遍插入策略
- `ContinentRegionService.buildGeographyText()` / `PowerSystemService.buildPowerSystemConstraint()`: 已有构建上下文文本的方法，controller 层已在用
- `PromptTemplateService.executeTemplate()`: 模板执行基础设施，精简模板按 templateCode 查询即可
- `WorldviewXmlParser` 待提取的公共逻辑: DOM 解析 `<g>/<f>` 标签 + 三级名称匹配，目前在 3 个 Strategy 中重复

### Established Patterns
- TaskStrategy 模式: getTaskType() 返回字符串 → AsyncTaskExecutor 通过 bean name convention 匹配
- 任务步骤: createSteps() 返回 List<StepConfig>，executeStep() 按步骤类型路由执行
- StepResult: success(result, progress) / failure(errorMessage)
- TaskContext: sharedData 用于步骤间数据传递（如 aiResponse）
- clean → generate → save 三步模式: 每个独立 Strategy 都遵循此模式
- 上下文构建: Controller 层构建 config（含上下文文本），Strategy 从 config 读取 — 保持一致

### Integration Points
- WorldviewTaskStrategy 注入 3 个独立 Strategy Bean（Spring @Autowired）
- WorldviewTaskStrategy 的 `generate_core` 步骤调用精简后 `llm_worldview_create` 模板
- `save_core` 步骤解析 `<t>/<b>/<l>/<r>` 并保存到 `novel_worldview` 记录 + 建立 `novel_worldview_power_system` 关联
- WorldviewController.generate-async 端点不变 — 仍创建 "worldview" 类型任务
- 前端通过 task 轮询获取进度，task 完成后 GET /worldview 获取完整数据

</code_context>

<specifics>
## Specific Ideas

- 用户明确选择"注入 Bean 直接调用"而非创建子任务 — 简化执行链，避免额外 task 记录
- 用户选择 9 步骤粒度 — 前端可看到每个模块的生成进度
- 用户选择"从 DB 回查拼装" — 确保前端收到与数据库一致的数据
- Phase 07 设计决策记录："DOM parsing methods copied from WorldviewTaskStrategy rather than extracted to shared utility (Phase 8 will consolidate)" — 本 Phase 就是执行这个计划

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 08-原有逻辑重构*
*Context gathered: 2026-04-03*
