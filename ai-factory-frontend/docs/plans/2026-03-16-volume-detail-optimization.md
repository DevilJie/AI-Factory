# 分卷详情页面优化实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 优化分卷详情页面，删除无用字段，增加AI优化功能，改进关键事件输入方式

**Architecture:** 前后端同步删除三个无用字段；新增AI优化分卷详情的异步任务；重构关键事件为5阶段动态列表；完善章节规划生成的上下文构建

**Tech Stack:** Vue 3 + TypeScript + Spring Boot 3 + MyBatis-Plus + MySQL

---

## Task 1: 后端删除无用字段

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelVolumePlan.java`
- Modify: `ai-factory-backend/src/main/java/com/aifactory/dto/VolumePlanDto.java`
- Modify: `ai-factory-backend/src/main/java/com/aifactory/dto/UpdateVolumeRequest.java`
- Modify: `ai-factory-backend/src/main/java/com/aifactory/common/xml/VolumePlanXmlDto.java`

**Step 1: 修改实体类 NovelVolumePlan.java**

删除以下三个字段及其注释：
- `estimatedWordCount`（预计字数）
- `startChapter`（起始章节）
- `endChapter`（结束章节）

**Step 2: 修改 VolumePlanDto.java**

删除对应的三个字段

**Step 3: 修改 UpdateVolumeRequest.java**

删除对应的三个字段

**Step 4: 修改 VolumePlanXmlDto.java**

删除对应的三个字段及 @JacksonXmlProperty 注解

**Step 5: 提交**

```bash
cd ai-factory-backend
git add src/main/java/com/aifactory/entity/NovelVolumePlan.java \
        src/main/java/com/aifactory/dto/VolumePlanDto.java \
        src/main/java/com/aifactory/dto/UpdateVolumeRequest.java \
        src/main/java/com/aifactory/common/xml/VolumePlanXmlDto.java
git commit -m "refactor: 删除分卷规划无用字段

- 删除 estimatedWordCount（预估字数）
- 删除 startChapter（起始章节）
- 删除 endChapter（结束章节）

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 创建数据库迁移脚本

**Files:**
- Create: `ai-factory-backend/src/main/resources/db/migrations/2026-03-16-remove-volume-unused-fields.sql`

**Step 1: 创建迁移脚本**

```sql
-- 删除分卷规划表中的无用字段
-- 创建日期: 2026-03-16

ALTER TABLE novel_volume_plan
DROP COLUMN IF EXISTS estimated_word_count,
DROP COLUMN IF EXISTS start_chapter,
DROP COLUMN IF EXISTS end_chapter;

SELECT '分卷规划表字段删除完成' AS message;
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/resources/db/migrations/2026-03-16-remove-volume-unused-fields.sql
git commit -m "chore: 添加删除分卷无用字段的数据库迁移脚本

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 前端删除无用字段

**Files:**
- Modify: `ai-factory-frontend2/src/types/project.ts`
- Modify: `ai-factory-frontend2/src/views/Project/Detail/creation/VolumeDetail.vue`

**Step 1: 修改 project.ts 类型定义**

在 Volume 接口中删除：
- `estimatedWordCount?: number`
- `startChapter?: number`
- `endChapter?: number`

**Step 2: 修改 VolumeDetail.vue formData**

删除 formData 中的三个字段：
- `estimatedWordCount`
- `targetChapterCount` (保留)
- `startChapter`
- `endChapter`

删除 watch 中的对应赋值

**Step 3: 删除 VolumeDetail.vue 模板中的表单项**

删除"预估字数"、"起始章节"、"结束章节"的表单输入框

**Step 4: 提交**

```bash
cd ai-factory-frontend2
git add src/types/project.ts \
        src/views/Project/Detail/creation/VolumeDetail.vue
git commit -m "refactor: 前端删除分卷无用字段

- 删除 estimatedWordCount、startChapter、endChapter
- 更新类型定义和表单数据

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 新增AI优化请求DTO

**Files:**
- Create: `ai-factory-backend/src/main/java/com/aifactory/dto/VolumeOptimizeRequest.java`

**Step 1: 创建请求DTO**

```java
package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI优化分卷详情请求
 */
@Data
@Schema(description = "AI优化分卷详情请求")
public class VolumeOptimizeRequest {

    @Schema(description = "预计章节数", example = "50")
    private Integer targetChapterCount;
}
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/java/com/aifactory/dto/VolumeOptimizeRequest.java
git commit -m "feat: 新增AI优化分卷详情请求DTO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 新增AI优化提示词模板

**Files:**
- Create: `ai-factory-backend/src/main/resources/db/migrations/2026-03-16-add-volume-optimize-template.sql`

**Step 1: 创建提示词模板SQL**

```sql
-- 新增AI优化分卷详情提示词模板
-- 创建日期: 2026-03-16

INSERT INTO ai_prompt_template (template_code, template_name, service_type, scenario, current_version_id, description, tags, is_active, is_system, created_time, updated_time)
VALUES ('llm_volume_optimize', 'AI优化分卷详情模板', 'llm', 'volume_optimize', NULL, '用于AI优化单个分卷详情的提示词模板', '小说,大纲,分卷,优化', 1, 1, NOW(), NOW());

SET @template_id = LAST_INSERT_ID();

INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, version_comment, is_active, created_time)
VALUES (
    @template_id,
    1,
'你是一位资深的网络小说作家和编辑。

请根据以下信息，为第{volumeNumber}卷优化分卷详情：

【故事背景】
{projectDescription}

【故事基调】{storyTone}
【故事类型】{storyGenre}

{worldviewInfo}

{previousVolumesInfo}

【当前分卷基础信息】
- 卷号：第{volumeNumber}卷
- 预计章节数：{targetChapterCount}章

【重要】请严格按照以下XML格式返回数据：
1. 必须返回纯XML格式，不要使用markdown代码块
2. 不要包含任何解释文字，直接返回XML数据
3. 对于长文本内容，请使用CDATA标签包裹

返回格式：
<v>
  <t><![CDATA[分卷标题]]></t>
  <m><![CDATA[本卷主旨]]></m>
  <c><![CDATA[主要冲突]]></c>
  <p><![CDATA[情节走向]]></p>
  <d><![CDATA[本卷简介]]></d>
  <e><![CDATA[关键事件（开篇事件1；发展事件1；转折事件1；高潮事件1；收尾事件1）]]></e>
  <g><![CDATA[核心目标]]></g>
  <x><![CDATA[高潮事件]]></x>
  <n><![CDATA[收尾描述]]></n>
  <l><![CDATA[时间线设定]]></l>
  <o><![CDATA[分卷备注]]></o>
</v>

现在请返回优化后的分卷详情XML：',
    '[{"name":"volumeNumber","type":"integer","desc":"卷号","required":true},{"name":"projectDescription","type":"string","desc":"故事背景","required":true},{"name":"storyTone","type":"string","desc":"故事基调","required":true},{"name":"storyGenre","type":"string","desc":"故事类型","required":true},{"name":"worldviewInfo","type":"string","desc":"世界观设定","required":false},{"name":"previousVolumesInfo","type":"string","desc":"前面所有卷摘要","required":false},{"name":"targetChapterCount","type":"integer","desc":"预计章节数","required":true}]',
    '初始版本，用于AI优化单个分卷详情',
    1,
    NOW()
);

UPDATE ai_prompt_template SET current_version_id = (SELECT id FROM ai_prompt_template_version WHERE template_id = @template_id AND version_number = 1) WHERE id = @template_id;

SELECT 'AI优化分卷详情模板创建完成' AS message;
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/resources/db/migrations/2026-03-16-add-volume-optimize-template.sql
git commit -m "feat: 新增AI优化分卷详情提示词模板

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 新增VolumeOptimizeTaskStrategy任务策略

**Files:**
- Create: `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/VolumeOptimizeTaskStrategy.java`

**Step 1: 创建任务策略类**

参考 `OutlineTaskStrategy.java` 的实现模式，创建 `VolumeOptimizeTaskStrategy.java`：

```java
package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.*;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI优化分卷详情任务策略
 */
@Slf4j
@Component
public class VolumeOptimizeTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return "volume_optimize";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "验证分卷状态", "validate_volume", new HashMap<>()));
        steps.add(new StepConfig(2, "构建上下文", "build_context", new HashMap<>()));
        steps.add(new StepConfig(3, "AI生成分卷详情", "generate_volume", new HashMap<>()));
        steps.add(new StepConfig(4, "保存结果", "save_result", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        String stepType = step.getStepType();
        switch (stepType) {
            case "validate_volume":
                return validateVolume(step, context);
            case "build_context":
                return buildContext(step, context);
            case "generate_volume":
                return generateVolume(step, context);
            case "save_result":
                return saveResult(step, context);
            default:
                return StepResult.failure("未知步骤类型: " + stepType);
        }
    }

    // ... 实现各步骤方法
}
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/java/com/aifactory/service/task/impl/VolumeOptimizeTaskStrategy.java
git commit -m "feat: 新增AI优化分卷详情任务策略

- 支持验证分卷状态
- 构建上下文（前面所有卷+世界观）
- AI生成分卷详情
- 保存结果

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: OutlineController新增AI优化接口

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/controller/OutlineController.java`

**Step 1: 添加AI优化接口**

```java
/**
 * AI优化分卷详情（异步）
 */
@PostMapping("/volumes/{volumeId}/optimize")
@Operation(
    summary = "AI优化分卷详情",
    description = "异步方式调用AI优化单个分卷的详情信息。需要提供预计章节数。"
)
public Result<Map<String, Object>> optimizeVolume(
        @PathVariable Long projectId,
        @PathVariable Long volumeId,
        @RequestBody VolumeOptimizeRequest request) {
    Long userId = UserContext.getUserId();
    log.info("用户 {} 请求AI优化项目 {} 分卷 {} 的详情", userId, projectId, volumeId);

    // 构建任务配置
    Map<String, Object> config = new HashMap<>();
    config.put("volumeId", volumeId);
    config.put("targetChapterCount", request.getTargetChapterCount());

    // 创建异步任务
    Long taskId = taskService.createTask(projectId, "volume_optimize", "AI优化分卷详情", config);

    Map<String, Object> result = new HashMap<>();
    result.put("taskId", taskId);
    result.put("message", "AI优化任务已创建");

    return Result.ok(result);
}
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/java/com/aifactory/controller/OutlineController.java
git commit -m "feat: OutlineController新增AI优化分卷接口

POST /api/novel/{projectId}/volumes/{volumeId}/optimize

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 更新章节规划的volumeInfo构建

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java`

**Step 1: 更新 volumeInfo 构建逻辑**

找到构建 `volumeInfo` 的代码位置（约156行），修改为包含所有分卷字段：

```java
// 4. 构建完整的分卷信息字符串
StringBuilder volumeInfoBuilder = new StringBuilder();
volumeInfoBuilder.append("【分卷信息】\n");
volumeInfoBuilder.append("- 卷号：第").append(volumeNumber).append("卷\n");
volumeInfoBuilder.append("- 卷名：").append(volumeTitle).append("\n");
volumeInfoBuilder.append("- 本卷主旨：").append(volumeTheme != null ? volumeTheme : "待补充").append("\n");
volumeInfoBuilder.append("- 主要冲突：").append(mainConflict != null ? mainConflict : "待补充").append("\n");
volumeInfoBuilder.append("- 情节走向：").append(plotArc != null ? plotArc : "待补充").append("\n");

// 获取完整的分卷信息
NovelVolumePlan volume = volumePlanMapper.selectById(volumeId);
if (volume != null) {
    if (volume.getVolumeDescription() != null && !volume.getVolumeDescription().isEmpty()) {
        volumeInfoBuilder.append("- 本卷简介：").append(volume.getVolumeDescription()).append("\n");
    }
    if (volume.getKeyEvents() != null && !volume.getKeyEvents().isEmpty()) {
        volumeInfoBuilder.append("- 关键事件：").append(volume.getKeyEvents()).append("\n");
    }
    if (volume.getCoreGoal() != null && !volume.getCoreGoal().isEmpty()) {
        volumeInfoBuilder.append("- 核心目标：").append(volume.getCoreGoal()).append("\n");
    }
    if (volume.getClimax() != null && !volume.getClimax().isEmpty()) {
        volumeInfoBuilder.append("- 高潮事件：").append(volume.getClimax()).append("\n");
    }
    if (volume.getEnding() != null && !volume.getEnding().isEmpty()) {
        volumeInfoBuilder.append("- 收尾描述：").append(volume.getEnding()).append("\n");
    }
    if (volume.getTimelineSetting() != null && !volume.getTimelineSetting().isEmpty()) {
        volumeInfoBuilder.append("- 时间线设定：").append(volume.getTimelineSetting()).append("\n");
    }
    if (volume.getVolumeNotes() != null && !volume.getVolumeNotes().isEmpty()) {
        volumeInfoBuilder.append("- 分卷备注：").append(volume.getVolumeNotes()).append("\n");
    }
}

String volumeInfo = volumeInfoBuilder.toString();
```

**Step 2: 提交**

```bash
cd ai-factory-backend
git add src/main/java/com/aifactory/service/task/impl/ChapterGenerationTaskStrategy.java
git commit -m "feat: 完善章节规划生成的volumeInfo构建

- 包含分卷所有详情字段
- 为AI生成提供更完整的上下文

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 前端新增AI优化API

**Files:**
- Modify: `ai-factory-frontend2/src/api/outline.ts`

**Step 1: 添加AI优化接口函数**

```typescript
// AI优化分卷详情请求
export interface VolumeOptimizeRequest {
  targetChapterCount: number
}

// AI优化分卷详情
export const optimizeVolume = (projectId: string, volumeId: string, data: VolumeOptimizeRequest) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/volumes/${volumeId}/optimize`,
    data
  )
}
```

**Step 2: 提交**

```bash
cd ai-factory-frontend2
git add src/api/outline.ts
git commit -m "feat: 新增AI优化分卷详情API

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 重构关键事件为5阶段动态列表

**Files:**
- Modify: `ai-factory-frontend2/src/views/Project/Detail/creation/VolumeDetail.vue`

**Step 1: 更新 formData 结构**

将 `keyEvents: ''` 改为对象结构：

```typescript
// 关键事件（5阶段）
const keyEventStages = [
  { key: 'opening', label: '开篇事件' },
  { key: 'development', label: '发展事件' },
  { key: 'turning', label: '转折事件' },
  { key: 'climax', label: '高潮事件' },
  { key: 'ending', label: '收尾事件' }
]

const formData = ref({
  // ... 其他字段
  keyEvents: {
    opening: [] as string[],
    development: [] as string[],
    turning: [] as string[],
    climax: [] as string[],
    ending: [] as string[]
  }
})

// 添加事件
const addKeyEvent = (stage: string) => {
  formData.value.keyEvents[stage as keyof typeof formData.value.keyEvents].push('')
}

// 删除事件
const removeKeyEvent = (stage: string, index: number) => {
  formData.value.keyEvents[stage as keyof typeof formData.value.keyEvents].splice(index, 1)
}
```

**Step 2: 更新 watch 中的数据解析**

解析 JSON 格式的 keyEvents：

```typescript
// 解析 keyEvents JSON
let parsedKeyEvents = { opening: [], development: [], turning: [], climax: [], ending: [] }
if (v.keyEvents) {
  try {
    parsedKeyEvents = JSON.parse(v.keyEvents)
  } catch {
    // 如果不是JSON，尝试按分号分割（兼容旧数据）
    parsedKeyEvents.opening = v.keyEvents.split(';').filter(Boolean)
  }
}
formData.value.keyEvents = parsedKeyEvents
```

**Step 3: 更新模板**

将单个 textarea 替换为5阶段动态列表：

```vue
<!-- 关键事件 -->
<div class="space-y-4">
  <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
    <Star class="w-4 h-4 text-purple-500" />
    关键事件
  </h3>
  <div class="space-y-4">
    <div v-for="stage in keyEventStages" :key="stage.key" class="space-y-2">
      <div class="flex items-center justify-between">
        <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
          {{ stage.label }}
        </label>
        <button
          @click="addKeyEvent(stage.key)"
          class="text-xs text-purple-600 hover:text-purple-700"
        >
          + 添加事件
        </button>
      </div>
      <div
        v-for="(event, index) in formData.keyEvents[stage.key]"
        :key="index"
        class="flex items-center gap-2"
      >
        <input
          v-model="formData.keyEvents[stage.key][index]"
          type="text"
          class="flex-1 px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm"
          :placeholder="`${stage.label}${index + 1}`"
        />
        <button
          @click="removeKeyEvent(stage.key, index)"
          class="text-red-500 hover:text-red-700"
        >
          删除
        </button>
      </div>
    </div>
  </div>
</div>
```

**Step 4: 提交**

```bash
cd ai-factory-frontend2
git add src/views/Project/Detail/creation/VolumeDetail.vue
git commit -m "feat: 重构关键事件为5阶段动态列表

- 开篇、发展、转折、高潮、收尾5个阶段
- 每个阶段支持动态添加/删除事件
- 兼容旧数据格式

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 11: 添加AI优化按钮和锁定逻辑

**Files:**
- Modify: `ai-factory-frontend2/src/views/Project/Detail/creation/VolumeDetail.vue`
- Modify: `ai-factory-frontend2/src/views/Project/Detail/creation/VolumeTree.vue`

**Step 1: VolumeDetail.vue 添加锁定状态判断**

```typescript
// 锁定状态：分卷下存在章节规划
const isLocked = computed(() => {
  return props.volume?.chapters && props.volume.chapters.length > 0
})

// AI优化中状态
const optimizing = ref(false)

// AI优化
const handleOptimize = async () => {
  if (!props.volume || isLocked.value) return

  optimizing.value = true
  try {
    const result = await optimizeVolume(
      props.volume.projectId,
      props.volume.id,
      { targetChapterCount: formData.value.targetChapterCount || 50 }
    )
    success(result.message || 'AI优化任务已启动')
    // TODO: 开始任务轮询
  } catch (e: any) {
    error(e.message || 'AI优化失败')
  } finally {
    optimizing.value = false
  }
}
```

**Step 2: 更新模板添加AI优化按钮和锁定提示**

```vue
<!-- Header 添加 AI优化按钮 -->
<button
  v-if="!isLocked"
  @click="handleOptimize"
  :disabled="optimizing"
  class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-purple-600 bg-purple-50 rounded-lg hover:bg-purple-100 transition-colors disabled:opacity-50"
>
  <Loader2 v-if="optimizing" class="w-4 h-4 animate-spin" />
  <Sparkles v-else class="w-4 h-4" />
  AI优化
</button>

<!-- 锁定提示 -->
<div v-if="isLocked" class="flex items-center gap-2 px-3 py-1 text-xs text-amber-600 bg-amber-50 rounded-lg">
  <Lock class="w-3.5 h-3.5" />
  已有章节规划，分卷信息已锁定
</div>

<!-- 表单添加 disabled 属性 -->
<input
  v-model="formData.volumeTitle"
  :disabled="isLocked"
  ...
/>
```

**Step 3: 提交**

```bash
cd ai-factory-frontend2
git add src/views/Project/Detail/creation/VolumeDetail.vue
git commit -m "feat: 添加AI优化按钮和锁定逻辑

- 存在章节规划时锁定整个分卷表单
- AI优化按钮触发异步任务
- 显示锁定状态提示

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 12: 集成测试

**Files:**
- 无新文件，运行测试

**Step 1: 后端编译测试**

```bash
cd ai-factory-backend
mvn clean compile -DskipTests
```

**Step 2: 前端类型检查**

```bash
cd ai-factory-frontend2
npm run type-check
```

**Step 3: 前端构建测试**

```bash
cd ai-factory-frontend2
npm run build
```

**Step 4: 提交最终确认**

```bash
git add -A
git commit -m "test: 分卷详情优化功能集成测试通过

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 执行顺序总结

1. Task 1: 后端删除无用字段
2. Task 2: 创建数据库迁移脚本
3. Task 3: 前端删除无用字段
4. Task 4: 新增AI优化请求DTO
5. Task 5: 新增AI优化提示词模板
6. Task 6: 新增VolumeOptimizeTaskStrategy
7. Task 7: OutlineController新增接口
8. Task 8: 更新章节规划volumeInfo
9. Task 9: 前端新增AI优化API
10. Task 10: 重构关键事件UI
11. Task 11: 添加AI优化按钮和锁定逻辑
12. Task 12: 集成测试
