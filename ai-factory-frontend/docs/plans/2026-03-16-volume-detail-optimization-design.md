# 分卷详情页面优化设计文档

> **创建日期**: 2026-03-16
> **设计目标**: 优化分卷详情页面，删除无用字段，增加AI优化功能，改进关键事件输入方式

---

## 一、需求概述

### 1.1 删除无用字段
- `estimatedWordCount`（预估字数）
- `startChapter`（起始章节）
- `endChapter`（结束章节）

### 1.2 新增AI优化功能
- 在分卷详情页添加"AI优化"按钮
- 一键重新生成分卷所有详情字段
- 参考前面所有卷和世界观设定

### 1.3 优化关键事件输入
- 按阶段拆分为5个分组
- 每个阶段支持动态添加多个事件

### 1.4 AI生成章节规划分批
- 固定每批10章
- 传入完整上下文

---

## 二、详细设计

### 2.1 删除字段

#### 前端修改
- **文件**: `src/views/Project/Detail/creation/VolumeDetail.vue`
- **文件**: `src/types/project.ts`
- 删除 `estimatedWordCount`、`startChapter`、`endChapter` 相关代码

#### 后端修改
- **实体**: `NovelVolumePlan.java` - 删除三个字段
- **DTO**: `VolumePlanDto.java`、`UpdateVolumeRequest.java` - 删除对应字段
- **XML DTO**: `VolumePlanXmlDto.java` - 删除对应字段

#### 数据库迁移
```sql
ALTER TABLE novel_volume_plan
DROP COLUMN estimated_word_count,
DROP COLUMN start_chapter,
DROP COLUMN end_chapter;
```

### 2.2 关键事件字段优化

#### 存储结构（JSON格式，复用 keyEvents 字段）
```json
{
  "opening": ["开篇事件1", "开篇事件2"],
  "development": ["发展事件1", "发展事件2"],
  "turning": ["转折事件1"],
  "climax": ["高潮事件1"],
  "ending": ["收尾事件1"]
}
```

#### 前端交互设计
- 5个阶段分组：开篇、发展、转折、高潮、收尾
- 每个阶段可动态添加/删除事件条目
- 使用 `v-for` 渲染列表，支持拖拽排序（可选）

### 2.3 AI优化分卷详情

#### 新增API
```
POST /api/novel/{projectId}/volumes/{volumeId}/optimize
```

#### 请求参数
```json
{
  "targetChapterCount": 50
}
```

#### 后端实现逻辑
1. **校验**: 检查该分卷是否已有章节规划 → 有则返回错误
2. **创建任务**: 创建异步任务（taskType: `volume_optimize`）
3. **构建上下文**:
   - 查询前面所有卷的摘要
   - 查询世界观设定
4. **调用AI**: 使用 `llm_volume_optimize` 模板
5. **更新分卷**: 解析AI响应，更新分卷所有字段

#### 新增提示词模板: `llm_volume_optimize`
```
你是一位资深的网络小说作家和编辑。

请根据以下信息，优化第{volumeNumber}卷的分卷详情：

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
...（包含所有字段的XML模板）
```

#### 锁定规则
- **判断条件**: 该分卷下存在任何章节规划记录
- **锁定后**:
  - 整个分卷详情表单只读
  - AI优化按钮禁用
  - 前端显示锁定提示

### 2.4 AI生成章节规划（分批）

#### 分批规则
- 固定每批10章
- 根据 `targetChapterCount` 计算批次数

#### 传入上下文（每批）
- **当前分卷详情**（补全所有字段）
- **世界观设定**
- **前面所有卷的摘要**

#### 需要更新的代码
**文件**: `ChapterGenerationTaskStrategy.java`

**当前 volumeInfo 构建（不完整）**:
```java
String volumeInfo = String.format("""
    【分卷信息】
    - 卷号：第%d卷
    - 卷名：%s
    - 本卷主旨：%s
    - 主要冲突：%s
    - 情节走向：%s
    """, volumeNumber, volumeTitle, volumeTheme, mainConflict, plotArc);
```

**更新后（完整）**:
```java
String volumeInfo = String.format("""
    【分卷信息】
    - 卷号：第%d卷
    - 卷名：%s
    - 本卷主旨：%s
    - 主要冲突：%s
    - 情节走向：%s
    - 本卷简介：%s
    - 关键事件：%s
    - 核心目标：%s
    - 高潮事件：%s
    - 收尾描述：%s
    - 时间线设定：%s
    - 分卷备注：%s
    """, volumeNumber, volumeTitle, volumeTheme, mainConflict, plotArc,
    volumeDescription, keyEvents, coreGoal, climax, ending,
    timelineSetting, volumeNotes);
```

---

## 三、任务类型定义

### 3.1 新增任务类型: `volume_optimize`

#### 任务步骤
1. `validate_volume` - 验证分卷状态（无章节规划）
2. `build_context` - 构建上下文（前面所有卷 + 世界观）
3. `generate_volume` - 调用AI生成分卷详情
4. `save_result` - 保存结果

#### 任务策略类
- **新增**: `VolumeOptimizeTaskStrategy.java`
- **参考**: `OutlineTaskStrategy.java` 的实现模式

---

## 四、前端修改清单

### 4.1 VolumeDetail.vue
- 删除三个无用字段的表单项
- 重构关键事件为5阶段动态列表
- 添加AI优化按钮
- 添加锁定状态判断和UI展示

### 4.2 project.ts
- 更新 Volume 接口，删除三个字段
- keyEvents 类型改为 JSON 对象

### 4.3 新增API调用
- `src/api/outline.ts` 添加 `optimizeVolume` 函数

---

## 五、后端修改清单

### 5.1 实体和DTO
- `NovelVolumePlan.java` - 删除三个字段
- `VolumePlanDto.java` - 删除三个字段
- `UpdateVolumeRequest.java` - 删除三个字段
- `VolumePlanXmlDto.java` - 删除三个字段

### 5.2 新增
- `VolumeOptimizeRequest.java` - AI优化请求DTO
- `VolumeOptimizeTaskStrategy.java` - 任务策略类

### 5.3 修改
- `OutlineController.java` - 添加优化接口
- `ChapterGenerationTaskStrategy.java` - 更新 volumeInfo 构建
- `OutlineTaskStrategy.java` - 更新分卷解析逻辑

### 5.4 数据库
- 迁移脚本删除三列
- 新增提示词模板 `llm_volume_optimize`

---

## 六、实现顺序

1. **后端**: 删除字段（实体、DTO、数据库迁移）
2. **前端**: 删除字段、更新类型
3. **后端**: 新增AI优化接口和任务策略
4. **后端**: 更新章节规划的 volumeInfo 构建
5. **前端**: 关键事件UI重构
6. **前端**: AI优化按钮和锁定逻辑
7. **测试**: 完整流程测试
