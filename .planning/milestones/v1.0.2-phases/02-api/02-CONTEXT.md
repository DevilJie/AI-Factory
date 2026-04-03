# Phase 2: 后端服务与 API - Context

# Phase 2: 后端服务与 API — 势力的完整 CRUD 生命周期可用，包括树形查询、级联删除、关联管理和 fillForces() 文本构建

**Gathered:** 2026-04-02
**Status:** Ready for planning

**Source:** discuss-phase

import domain>
## Phase Boundary

## Phase Boundary
势力的完整后端 CRUD 生命周期可用，包括树形查询、级联删除、关联管理 API 和 fillForces() 文本构建。
此阶段交付势力实体、映射射关联表 CRUD 服务、关联管理 API 猡 AI 提示词构建提供文本描述。不包含前端页面或仅提供后端 API。

</domain>
<decisions>
## Implementation Decisions
### 服务拆分策略
- **D-01:** FactionService 单类管理势力树 CRUD + fillForces()，参照 ContinentRegionService 模式
便于维护。 关联表（势力-地区、势力-人物、势力-势力关系）拆分为独立管理
 保持职责单一清晰
- **D-02:** ContinentRegionService 模式对势力实体做树形 CRUD（getTreeByProjectId、级联删除、buildGeographyText、fillGeography），复用模式
 不再需要新的树形辅助方法
 直接适配 force 字段

### fillForces() 输出格式
- **D-03:** 层级缩进格式，每级缩进显示势力名、类型、力量体系、地区引用，使用颜色标签区分类型
 只 **D-04:** 只包含顶级势力信息 — type + 力量体系名称 + 地区名称
- **D-05:** 单层平铺（无 hierarchy),简洁直接,每项一行：势力名: 描述
- **D-06:** 末尾空行汇总顶级势力下的子势力列表（缩进显示）

示例输出:
```
【正派】 紫阳宗（力量体系： 紵灵宗)
  ├─ 猣新] 凌霄剑派 (核心: 御剑术)
  │   ├─ 掌门] 江南门 (力量体系: 仙道)
  │     ├─ 势力名称: 紵灵宗
  │       └─ 地区: 东域、北域、南海域
  │   └─ 未知势力
  ...
```
buildGeographyText() 有层级输出模式参考

已在 CONTEXT.md 中记录)

### API 端点设计
- **D-07:** 合并到单个 FactionController（势力 CRUD + 3 个关联管理端点），RESTful 风格，路由复 `/api/novel/faction/*`
- **D-08:** 势力-地区、势力-人物、势力-势力关系的 CRUD 端点直接嵌入主 Controller，共享 FactionService 实例
 无需单独 Controller
- **D-09:** 删除项目数据时清理 4 张关联表（在 deleteByProjectId 中实现）

### 级联删除策略
- **D-10:** 应用层递归删除：先递归查所有子势力 ID → 刌据关联 Mapper 扌关联 ID 刯 事务保护
- **D-11:** 全部操作在一个 @Transactional 方法中完成，确保数据一致性

- **D-12:** 删除父势力前先弹出确认对话框（前端实现），用户需确认才递归删除子势力

- **Phase 1 决定:** 不加数据库外键约束，纯逻辑关联，保持与 ContinentRegion 模式一致

### Claude's Discretion
- DTO 类的具体字段和结构
- 锽空 transaction注解的使用方式
- 锌空抽象类的设计
- 异常处理和错误响应细节

- 日志记录级别

- 枑空方法命名约定（与 ContinentRegionService 保持一致）

</decisions>
<canonical_refs>
## Canonical References
**Downstream agents MUST read these before planning or implementing.**
### 篡缩现有树形 CRUD 参考
- `ai-factory-backend/src/main/java/com/aifactory/service/ContinentRegionService.java` — 势力树形 CRUD 服务的参考实现（getTreeByProjectId、级联删除、buildGeographyText、fillGeography）
- `ai-factory-backend/src/main/java/com/aifactory/controller/ContinentRegionController.java` — REST Controller 路由结构、响应格式参考
 - `ai-factory-backend/src/main/java/com/aifactory/entity/NovelContinentRegion.java` — 树形实体字段模式（parentId + deep + sortOrder + children）
 - `ai-factory-backend/src/main/java/com/aifactory/entity/NovelFaction.java` — 势力实体（含 type、corePowerSystem、 type字段）
 - `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionMapper.java` — BaseMapper<NovelFaction>
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRegionMapper.java` — BaseMapper<NovelFactionRegion>
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionCharacterMapper.java` — BaseMapper<NovelFactionCharacter>
- `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelFactionRelationMapper.java` — BaseMapper<NovelFactionRelation>
### 提示词构建参考
- `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java` — 当前 forces 文本拼接方式（getForces 调用点）
 - `.planning/REQUIREMENTS.md` §v1 需求 > 后端服务 — BACK-01 到 BACK-10 的完整定义
</canonical_refs>
<code_context>
## Existing Code Insights
### Reusable Assets
- **ContinentRegionService**: 完整的树形 CRUD 实现，可直接复用为 FactionService
 getTreeByProjectId、级联删除、buildGeographyText → fillGeography 方法均可参照
- **ContinentRegionController**: REST 端点路由结构、响应格式可直接复用为 FactionController

- **NovelContinentRegion 实体**: 树形字段模式（parentId + deep + sortOrder + children）完全可复用
- **NovelFaction 实体**: 已含 type、 corePowerSystem 字段，仅用于势力
 但其他字段模式与 ContinentRegion 相同)
- **Result<T>**: 统一 API 响应包装类，全局异常处理

可在 `ai-factory-backend/src/main/java/com/aifactory/controller/ContinentRegionController.java`
- **4 个 BaseMapper 接口**: 已由 Phase 1 创建，可直接使用

### Established Patterns
- **树形 CRUD**: ContinentRegionService 实现了完整的树形增删改查+文本构建模式
- **@Transactional 写操作**: 所有写操作使用事务注解
 数据一致性
- **Result<T> 响应包装**: 所有 API 响应使用统一包装格式
- **RESTful 路由**: /api/novel/{projectId}/continent-region/* 模式,用于所有 API
端点
### Integration Points
- FactionService 将由 FactionController 调用（REST API）
- FactionService.fillForces() 将由 Phase 3 的 WorldviewTaskStrategy 调用（AI 集成)
- FactionService.deleteByProjectId() 将由 WorldviewTaskStrategy.checkExisting 调用（清理旧数据）
- FactionService 将依赖 NovelFactionMapper + 3 个关联 Mapper
- FactionController 路由前缀 /api/novel/faction（与 WorldSetting 页面的 API 前缀对齐）
</code_context>
<specifics>
## Specific Ideas
- ContinentRegionService 模式已验证可靠，直接复用，关键方法包括 getTreeByProjectId、级联删除、buildGeographyText、fillGeography
- fillForces() 输出格式参照 buildGeographyText() 的层级缩进模式，加入颜色标签和地区引用
 - 所有 API 端点合并到单个 FactionController，关联表 CRUD 不单独建 Controller
</specifics>
<deferred>
## Deferred Ideas

None — discussion stayed within phase scope
</deferred>
---

*Phase: 02-api*
*Context gathered: 2026-04-02*
