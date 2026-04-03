# Phase 3: AI 集成与提示词迁移 - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

AI 世界观生成流程能输出结构化势力 XML，后端正确解析入库，所有提示词构建调用点已迁移到新方法。
此阶段交付：提示词模板更新（势力输出结构化 XML）、WorldviewTaskStrategy 新增 DOM 解析势力 XML 并入库、6 处 getForces() 调用点迁移为 fillForces()。不包含前端页面或新的后端 API。

</domain>

<decisions>
## Implementation Decisions

### 势力 XML 格式
- **D-01:** 外层 `<f>` 容器，内层 `<faction>` 标签，子势力通过嵌套 `<faction>` 表达层级关系（与地理 `<g>/<r>` 模式一致）
- **D-02:** XML 节点不使用属性，所有数据通过子元素表达：`<n>` 名称、`<d>` 描述（CDATA 包裹）、`<type>` 类型、`<power>` 力量体系引用、`<regions>` 地区引用
- **D-03:** 地区引用用逗号分隔的名称列表（如 `<regions>东域,北域</regions>`），力量体系用单个名称引用（如 `<power>仙道</power>`）
- **D-04:** 势力间关系用 `<relation>` 标签对，每对包含 `<target>`（目标势力名称）和 `<type>`（关系类型：盟友/敌对/中立）
- **D-05:** type 和 power 仅顶级势力设置（Phase 1 D-05 决定），子势力继承顶级势力值，AI 输出时子势力不需要写 type 和 power

最终 XML 格式示例：
```xml
<f>
  <faction>
    <n>紫阳宗</n>
    <type>正派</type>
    <power>仙道</power>
    <regions>东域,北域</regions>
    <d><![CDATA[正道大宗，以剑道著称]]></d>
    <relation><target>魔门</target><type>敌对</type></relation>
    <faction>
      <n>凌霄剑派</n>
      <d><![CDATA[紫阳宗下属剑派]]></d>
    </faction>
  </faction>
</f>
```

### 名称匹配与错误处理
- **D-06:** 三级容错匹配策略：精确匹配 → 去后缀匹配（去除宗/派/门等后缀再比较）→ contains 包含匹配。三级都未匹配则跳过该引用并记录 WARN 日志
- **D-07:** XML 解析失败时跳过失败的势力项，成功的正常入库。只记录警告日志。其他部分（地理、力量体系）不受影响。用户可以后续手动补充缺失数据
- **D-08:** DOM 解析使用 getChildNodes() 直接子元素迭代（AI-07 需求），不使用 getElementsByTagName 避免取到所有后代

### 提示词迁移方案
- **D-09:** 6 个 getForces() 调用点一次性全部迁移为 fillForces()，不做增量迁移。调用点列表：PromptTemplateBuilder、ChapterPromptBuilder、PromptContextBuilder、ChapterFixTaskStrategy、VolumeOptimizeTaskStrategy、OutlineTaskStrategy
- **D-10:** 迁移后直接调用 factionService.fillForces(worldview) 替换原有的 worldview.getForces() 调用，fillForces 内部处理 null/empty 检查

### 提示词模板设计
- **D-11:** 势力 XML 提示词使用严格规则描述，详细说明每个字段的规则（类型枚举：正派/反派/中立、层级限制、引用约束、命名要求等）
- **D-12:** 提示词中势力引用地区名称和力量体系名称，确保与已输出的 `<p>` 和 `<g>` 标签中的名称一致

### 两遍插入策略
- **D-13:** 两遍插入（AI-04 需求）：第一遍存所有势力构建名称→ID 映射，第二遍建立势力-地区和势力-势力关联（因为关联需要 ID，而 ID 在插入后才生成）
- **D-14:** 已有项目重新生成世界观时，先清除旧势力数据再解析新数据（AI-06 需求），调用 FactionService.deleteByProjectId()

### Claude's Discretion
- 名称模糊匹配的具体后缀列表（宗/派/门/殿/阁等）
- DOM 解析的异常处理细节
- 提示词模板中的具体措辞和格式说明
- 日志记录的格式和级别

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### AI 生成流程核心
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java` — 世界观 AI 生成主流程，包含 parseAndSaveWorldview、saveGeographyRegionsFromXml、savePowerSystems 方法，新增 saveFactionsFromXml 的位置
- `ai-factory-backend/src/main/java/com/aifactory/service/FactionService.java` — 势力服务接口，包含 fillForces、saveTree、deleteByProjectId 方法
- `ai-factory-backend/src/main/java/com/aifactory/service/impl/FactionServiceImpl.java` — 势力服务实现，fillForces 文本构建逻辑

### 6 个 getForces() 迁移目标
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` — getForces() 调用点 1
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/ChapterPromptBuilder.java` — getForces() 调用点 2
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptContextBuilder.java` — getForces() 调用点 3
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterFixTaskStrategy.java` — getForces() 调用点 4
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/VolumeOptimizeTaskStrategy.java` — getForces() 调用点 5
- `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/OutlineTaskStrategy.java` — getForces() 调用点 6

### 参考实现
- `ai-factory-backend/src/main/java/com/aifactory/service/impl/ContinentRegionServiceImpl.java` — 地理树形 CRUD 参考（fillGeography、saveTree 模式）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java` — 势力实体（type、corePowerSystem 字段）
- `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java` — forces 字段已标记 transient

### 需求定义
- `.planning/REQUIREMENTS.md` §v1 需求 > AI 集成 — AI-01 到 AI-08 完整定义
- `.planning/REQUIREMENTS.md` §v1 需求 > 提示词构建迁移 — PROMPT-01 到 PROMPT-06 完整定义

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **ContinentRegionServiceImpl.fillGeography()**: 势力 fillForces 的参考模式（从树构建文本）
- **ContinentRegionServiceImpl.saveTree()**: 保存树形数据的参考实现
- **WorldviewTaskStrategy.saveGeographyRegionsFromXml()**: DOM 解析嵌套 XML 的参考实现（使用 getChildNodes() 直接子元素迭代）
- **WorldviewTaskStrategy.savePowerSystems()**: DTO 解析力量体系的参考实现
- **FactionServiceImpl.fillForces()**: 已实现的 fillForces 方法，可直接调用
- **FactionServiceImpl.deleteByProjectId()**: 已实现的删除方法，可直接调用
- **FactionServiceImpl.saveTree()**: 已实现的保存方法，可直接调用

### Established Patterns
- **DOM 解析嵌套同名标签**: 使用 org.w3c.dom.Document + getChildNodes() 迭代（Jackson XML 无法处理嵌套同名标签）
- **两遍插入**: 先存主数据（获取 ID），再存关联数据（引用 ID）— 地理区域已使用此模式
- **@Transactional 写操作**: WorldviewTaskStrategy 的所有写操作在同一事务中
- **AI 输出 → 名称回查 ID**: 力量体系已使用此模式（savePowerSystems 中按名称匹配）
- **checkExisting 清理**: WorldviewTaskStrategy 已有清理旧数据的模式

### Integration Points
- WorldviewTaskStrategy.parseAndSaveWorldview() 中新增 saveFactionsFromXml() 调用，在 savePowerSystems 之后
- 6 个 PromptBuilder/TaskStrategy 中的 getForces() 替换为 fillForces()，需注入 FactionService
- 提示词模板（ai_prompt_template_version 表 id=3）中势力部分从 `<f><![CDATA[纯文本]]></f>` 改为结构化 XML 格式指令

</code_context>

<specifics>
## Specific Ideas

- 地理 XML 使用 `<n>` 和 `<d>` 子元素（不使用属性），势力 XML 保持一致风格
- AI 输出势力名称引用地区和力量体系时，应与前面 `<p>` 和 `<g>` 中输出的名称完全一致
- 提示词模板中的势力 XML 格式说明应包含完整的示例和严格规则，确保 AI 输出格式一致

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---
*Phase: 03-ai*
*Context gathered: 2026-04-02*
