# 力量体系重构设计文档

## 概述

将世界观表中的 `power_system` 文本字段抽离为独立的结构化力量体系模块，支持多套修炼体系、等级划分、境界划分，以及丰富的等级属性描述。同时改造前端为卡片式交互，优化 AI 生成提示词和解析逻辑。

## 1. 数据库设计

### 1.1 新增表

#### novel_power_system（力量体系）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AUTO_INCREMENT | 主键 |
| project_id | bigint | 归属项目ID |
| name | varchar(100) | 体系名称（修仙、炼体等） |
| source_from | varchar(500) | 能量来源（天地灵气等） |
| core_resource | varchar(500) | 核心资源（灵石、源石等） |
| cultivation_method | varchar(500) | 修炼方式（打坐冥想、战斗突破等） |
| description | text | 体系整体描述 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### novel_power_system_level（体系等级）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AUTO_INCREMENT | 主键 |
| power_system_id | bigint | 关联力量体系ID |
| level | int | 等级索引（1开始，越小越低） |
| level_name | varchar(100) | 等级名称 |
| description | text | 等级描述 |
| breakthrough_condition | text | 突破条件 |
| lifespan | varchar(100) | 寿命范围 |
| power_range | text | 战力描述 |
| landmark_ability | varchar(500) | 标志性能力（踏空飞行、创造世界等） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### novel_power_system_level_step（等级境界）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AUTO_INCREMENT | 主键 |
| power_system_level_id | bigint | 关联等级ID |
| level | int | 境界序号（1开始，越小越低） |
| level_name | varchar(100) | 境界名称（前期、中期等） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### novel_worldview_power_system（世界观-力量体系关联）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AUTO_INCREMENT | 主键 |
| worldview_id | bigint | 世界观ID |
| power_system_id | bigint | 力量体系ID |

### 1.2 修改现有表

**novel_worldview** — 移除 `power_system` text 字段。力量体系通过关联表查询。

## 2. 后端 API 设计

### 2.1 新增 PowerSystemController

路径前缀：`/api/novel/{projectId}/power-system`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 获取项目下所有力量体系（含等级和境界） |
| GET | `/{id}` | 获取单个力量体系详情 |
| POST | `/save` | 新增/更新力量体系（含等级和境界，整体提交） |
| DELETE | `/{id}` | 删除力量体系（级联删除等级和境界） |

### 2.2 WorldviewController 变更

- `POST /{projectId}/worldview/generate-async` — AI 生成完成后，解析力量体系 XML 并入库，同时写入关联表
- `POST /{projectId}/worldview/save` — 移除 power_system 字段的处理

### 2.3 AI 生成与解析流程

#### 提示词模板

`buildWorldviewPrompt` 从硬编码改为读取 `ai_prompt_template_version` 表 id=3 的模板。力量体系部分要求 AI 以 XML 子节点格式返回。

#### XML 返回格式

```xml
<worldview>
  <worldType>玄幻世界</worldType>
  <worldBackground>背景描述...</worldBackground>
  <powerSystem>
    <system>
      <name>修仙</name>
      <sourceFrom>天地灵气</sourceFrom>
      <coreResource>灵石</coreResource>
      <cultivationMethod>打坐冥想，运转经脉吸收灵气</cultivationMethod>
      <description>以天地灵气为根基，追求长生大道</description>
      <levels>
        <level>
          <levelName>练气期</levelName>
          <description>能感应灵气，使用简单法术</description>
          <breakthroughCondition>需聚灵阵辅助，感应灵气入体</breakthroughCondition>
          <lifespan>约150年</lifespan>
          <powerRange>可敌数名凡人武者</powerRange>
          <landmarkAbility>灵气外放</landmarkAbility>
          <steps>
            <step>前期</step>
            <step>中期</step>
            <step>后期</step>
            <step>大圆满</step>
          </steps>
        </level>
      </levels>
    </system>
  </powerSystem>
  <geography>...</geography>
  <forces>...</forces>
  <timeline>...</timeline>
  <rules>...</rules>
</worldview>
```

#### 解析流程

```
AI返回XML → 解析powerSystem节点 → 遍历每个<system>：
  ├── 插入 novel_power_system
  ├── 遍历 <level> → 插入 novel_power_system_level
  │     └── 遍历 <step> → 插入 novel_power_system_level_step
  └── 插入 novel_worldview_power_system 关联
```

### 2.4 buildPowerSystemConstraint 公共方法

新建 Service 层公共方法，输入 `projectId`，查询该项目的力量体系数据并组装为约束字符串：

```
【力量体系约束】
体系：修仙（能量来源：天地灵气 | 核心资源：灵石 | 修炼方式：打坐冥想）
描述：以天地灵气为根基，追求长生大道
等级划分：
  练气期（前期/中期/后期/大圆满）
    描述：能感应灵气，使用简单法术 | 寿命：约150年 | 标志能力：灵气外放
    突破条件：需聚灵阵辅助，感应灵气入体
  筑基期（前期/中期/后期/大圆满）
    描述：灵力凝实，可御器飞行 | 寿命：约300年 | 标志能力：御剑飞行
    突破条件：需筑基丹一枚，灵气充裕之地
```

**调用方改造：**
- `ChapterCharacterExtractService.buildPowerSystemConstraint()` → 调用公共方法，移除原逻辑
- `PromptTemplateBuilder.buildPowerSystemConstraint()` → 调用公共方法，移除原逻辑

## 3. 前端设计

### 3.1 组件结构

```
WorldSetting.vue（现有，改造）
  └── PowerSystemSection.vue（新增，替换原 power_system 文本域）
        ├── PowerSystemCard.vue（单个体系卡片，可展开/收起）
        │     └── LevelEditor.vue（等级编辑，内嵌在卡片展开区域）
        └── PowerSystemForm.vue（新增/编辑体系的表单弹窗）
```

### 3.2 交互设计

**展示态：**
- 力量体系区域显示为卡片列表
- 每个卡片显示：体系名称、能量来源、修炼方式（简略）
- 卡片右上角有「编辑」和「删除」图标
- 底部有「+ 添加力量体系」按钮
- AI 生成世界观后，力量体系卡片自动填充

**卡片展开态（点击卡片）：**
- 展开显示体系完整信息：描述、核心资源、修炼方式
- 下方展示等级列表（表格形式），每行显示：等级名、标志性能力、描述摘要
- 点击等级行可展开显示：突破条件、寿命、战力描述、境界列表
- 等级支持上下箭头调整顺序

**编辑态（点击编辑或添加）：**
- 弹出 PowerSystemForm.vue 表单弹窗
- 上半部分：体系基本信息（名称、能量来源、核心资源、修炼方式、描述）
- 下半部分：等级列表编辑区
  - 每个等级可展开编辑所有字段 + 境界列表
  - 境界支持增删和排序
  - 等级支持增删和排序
- 保存时整体提交（体系 + 等级 + 境界一次性保存）

### 3.3 数据流

```
页面加载 → GET /list → 渲染卡片列表
点击添加/编辑 → 填写表单 → POST /save
AI生成世界观 → 解析XML → 自动调用 save 接口入库 → 刷新卡片列表
删除 → DELETE /{id} → 移除卡片
```

### 3.4 与现有 WorldSetting.vue 的集成

- 移除原有的 `power_system` 文本域
- 在原位置替换为 `PowerSystemSection` 组件
- 保留 AI 生成按钮和任务轮询逻辑
- 生成完成后同时刷新世界观表单和力量体系卡片

## 4. 改造范围汇总

### 数据库
- 新建 4 张表（power_system, level, level_step, worldview_power_system）
- 修改 1 张表（worldview 移除 power_system 字段）

### 后端
- 新增 Entity/Mapper/Service/Controller：PowerSystem 相关全套
- 改造 WorldviewTaskStrategy：提示词模板化 + XML 解析重构
- 改造 ChapterCharacterExtractService / PromptTemplateBuilder：调用公共约束方法
- 新增 Service 公共方法：buildPowerSystemConstraint
- 更新 ai_prompt_template_version id=3 的模板内容

### 前端
- 新增 4 个组件：PowerSystemSection, PowerSystemCard, LevelEditor, PowerSystemForm
- 改造 WorldSetting.vue：替换 power_system 文本域
- 新增 API 接口调用（worldview.ts 或新文件）
