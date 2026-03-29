# 力量体系重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将世界观中的 power_system 文本字段抽离为独立的结构化力量体系模块，支持多套修炼体系、等级划分、境界划分及丰富属性，改造前端为卡片式交互。

**Architecture:** 新建 4 张数据库表（power_system、level、level_step、worldview_power_system），后端新增完整的 Entity/Mapper/Service/Controller，改造 WorldviewTaskStrategy 的提示词和解析逻辑，抽取 buildPowerSystemConstraint 公共方法，前端将力量体系文本域替换为卡片式组件。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + Java 21 (后端) | Vue 3 + TypeScript + TailwindCSS (前端) | MySQL 8

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `sql/power_system_migration.sql`

- [ ] **Step 1: 编写迁移SQL脚本**

```sql
-- power_system_migration.sql
-- 力量体系重构迁移脚本

SET NAMES utf8mb4;

-- 1. 新建力量体系表
CREATE TABLE `novel_power_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '体系名称',
  `source_from` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '能量来源',
  `core_resource` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '核心资源',
  `cultivation_method` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修炼方式',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '体系整体描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '力量体系表' ROW_FORMAT = Dynamic;

-- 2. 新建体系等级表
CREATE TABLE `novel_power_system_level` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `power_system_id` bigint NOT NULL COMMENT '关联力量体系ID',
  `level` int NULL DEFAULT NULL COMMENT '等级索引（1开始，越小越低）',
  `level_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '等级描述',
  `breakthrough_condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '突破条件',
  `lifespan` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '寿命范围',
  `power_range` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '战力描述',
  `landmark_ability` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标志性能力',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_power_system_id`(`power_system_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '体系等级表' ROW_FORMAT = Dynamic;

-- 3. 新建等级境界表
CREATE TABLE `novel_power_system_level_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `power_system_level_id` bigint NOT NULL COMMENT '关联等级ID',
  `level` int NULL DEFAULT NULL COMMENT '境界序号（1开始，越小越低）',
  `level_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '境界名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_power_system_level_id`(`power_system_level_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '等级境界表' ROW_FORMAT = Dynamic;

-- 4. 新建世界观-力量体系关联表
CREATE TABLE `novel_worldview_power_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worldview_id` bigint NOT NULL COMMENT '世界观ID',
  `power_system_id` bigint NOT NULL COMMENT '力量体系ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
  INDEX `idx_power_system_id`(`power_system_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-力量体系关联表' ROW_FORMAT = Dynamic;

-- 5. 移除世界观表的power_system字段
ALTER TABLE `novel_worldview` DROP COLUMN `power_system`;
```

- [ ] **Step 2: 在数据库执行迁移**

根据项目配置连接 MySQL 执行上述 SQL。

- [ ] **Step 3: 提交**

```bash
git add sql/power_system_migration.sql
git commit -m "feat: 添加力量体系数据库迁移脚本"
```

---

## Task 2: 后端 Entity + Mapper

**Files:**
- Create: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystem.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystemLevel.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystemLevelStep.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldviewPowerSystem.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemMapper.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemLevelMapper.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemLevelStepMapper.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/mapper/NovelWorldviewPowerSystemMapper.java`
- Modify: `ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java` — 移除 powerSystem 字段

- [ ] **Step 1: 创建 NovelPowerSystem 实体**

```java
// ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystem.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("novel_power_system")
public class NovelPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String sourceFrom;
    private String coreResource;
    private String cultivationMethod;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 NovelPowerSystemLevel 实体**

```java
// ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystemLevel.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("novel_power_system_level")
public class NovelPowerSystemLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long powerSystemId;
    private Integer level;
    private String levelName;
    private String description;
    private String breakthroughCondition;
    private String lifespan;
    private String powerRange;
    private String landmarkAbility;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建 NovelPowerSystemLevelStep 实体**

```java
// ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystemLevelStep.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("novel_power_system_level_step")
public class NovelPowerSystemLevelStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long powerSystemLevelId;
    private Integer level;
    private String levelName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 4: 创建 NovelWorldviewPowerSystem 实体**

```java
// ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldviewPowerSystem.java
package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("novel_worldview_power_system")
public class NovelWorldviewPowerSystem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long worldviewId;
    private Long powerSystemId;
}
```

- [ ] **Step 5: 创建 4 个 Mapper 接口**

```java
// ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemMapper.java
package com.aifactory.mapper;

import com.aifactory.entity.NovelPowerSystem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelPowerSystemMapper extends BaseMapper<NovelPowerSystem> {
}
```

```java
// ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemLevelMapper.java
package com.aifactory.mapper;

import com.aifactory.entity.NovelPowerSystemLevel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelPowerSystemLevelMapper extends BaseMapper<NovelPowerSystemLevel> {
}
```

```java
// ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystemLevelStepMapper.java
package com.aifactory.mapper;

import com.aifactory.entity.NovelPowerSystemLevelStep;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelPowerSystemLevelStepMapper extends BaseMapper<NovelPowerSystemLevelStep> {
}
```

```java
// ai-factory-backend/src/main/java/com/aifactory/mapper/NovelWorldviewPowerSystemMapper.java
package com.aifactory.mapper;

import com.aifactory.entity.NovelWorldviewPowerSystem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelWorldviewPowerSystemMapper extends BaseMapper<NovelWorldviewPowerSystem> {
}
```

- [ ] **Step 6: 修改 NovelWorldview 实体 — 移除 powerSystem 字段**

从 `NovelWorldview.java` 中删除 `powerSystem` 字段及其注释（约第 43-47 行）：
```java
// 删除以下字段:
/**
 * 力量体系/修炼体系
 */
private String powerSystem;
```

- [ ] **Step 7: 提交**

```bash
git add ai-factory-backend/src/main/java/com/aifactory/entity/NovelPowerSystem*.java ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldviewPowerSystem.java ai-factory-backend/src/main/java/com/aifactory/mapper/NovelPowerSystem*.java ai-factory-backend/src/main/java/com/aifactory/mapper/NovelWorldviewPowerSystemMapper.java ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java
git commit -m "feat: 添加力量体系实体和Mapper，移除世界观powerSystem字段"
```

---

## Task 3: PowerSystemService — CRUD + buildPowerSystemConstraint 公共方法

**Files:**
- Create: `ai-factory-backend/src/main/java/com/aifactory/service/PowerSystemService.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/service/impl/PowerSystemServiceImpl.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemSaveRequest.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemLevelSaveRequest.java`
- Create: `ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemLevelStepSaveRequest.java`

- [ ] **Step 1: 创建请求DTO**

```java
// ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemLevelStepSaveRequest.java
package com.aifactory.dto;

import lombok.Data;

@Data
public class PowerSystemLevelStepSaveRequest {
    private Long id;
    private Integer level;
    private String levelName;
}
```

```java
// ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemLevelSaveRequest.java
package com.aifactory.dto;

import lombok.Data;
import java.util.List;

@Data
public class PowerSystemLevelSaveRequest {
    private Long id;
    private Integer level;
    private String levelName;
    private String description;
    private String breakthroughCondition;
    private String lifespan;
    private String powerRange;
    private String landmarkAbility;
    private List<PowerSystemLevelStepSaveRequest> steps;
}
```

```java
// ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystemSaveRequest.java
package com.aifactory.dto;

import lombok.Data;
import java.util.List;

@Data
public class PowerSystemSaveRequest {
    private Long id;
    private Long projectId;
    private String name;
    private String sourceFrom;
    private String coreResource;
    private String cultivationMethod;
    private String description;
    private List<PowerSystemLevelSaveRequest> levels;
}
```

- [ ] **Step 2: 创建 Service 接口**

```java
// ai-factory-backend/src/main/java/com/aifactory/service/PowerSystemService.java
package com.aifactory.service;

import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.NovelPowerSystem;
import java.util.List;

public interface PowerSystemService {

    List<NovelPowerSystem> listByProjectId(Long projectId);

    NovelPowerSystem getById(Long id);

    NovelPowerSystem savePowerSystem(PowerSystemSaveRequest request);

    void deleteById(Long id);

    /**
     * 构建力量体系约束文本（公共方法）
     * 供 ChapterCharacterExtractService 和 PromptTemplateBuilder 调用
     */
    String buildPowerSystemConstraint(Long projectId);
}
```

- [ ] **Step 3: 创建 Service 实现**

```java
// ai-factory-backend/src/main/java/com/aifactory/service/impl/PowerSystemServiceImpl.java
package com.aifactory.service.impl;

import com.aifactory.dto.PowerSystemLevelSaveRequest;
import com.aifactory.dto.PowerSystemLevelStepSaveRequest;
import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.*;
import com.aifactory.mapper.*;
import com.aifactory.service.PowerSystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PowerSystemServiceImpl implements PowerSystemService {

    @Autowired
    private NovelPowerSystemMapper powerSystemMapper;
    @Autowired
    private NovelPowerSystemLevelMapper levelMapper;
    @Autowired
    private NovelPowerSystemLevelStepMapper stepMapper;
    @Autowired
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Override
    public List<NovelPowerSystem> listByProjectId(Long projectId) {
        return powerSystemMapper.selectList(
            new LambdaQueryWrapper<NovelPowerSystem>()
                .eq(NovelPowerSystem::getProjectId, projectId)
                .orderByAsc(NovelPowerSystem::getId)
        );
    }

    @Override
    public NovelPowerSystem getById(Long id) {
        return powerSystemMapper.selectById(id);
    }

    @Override
    @Transactional
    public NovelPowerSystem savePowerSystem(PowerSystemSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        NovelPowerSystem system;

        if (request.getId() != null) {
            // 更新
            system = powerSystemMapper.selectById(request.getId());
            system.setName(request.getName());
            system.setSourceFrom(request.getSourceFrom());
            system.setCoreResource(request.getCoreResource());
            system.setCultivationMethod(request.getCultivationMethod());
            system.setDescription(request.getDescription());
            system.setUpdateTime(now);
            powerSystemMapper.updateById(system);

            // 删除旧等级和境界，重新插入
            deleteLevelsBySystemId(system.getId());
        } else {
            // 新增
            system = new NovelPowerSystem();
            system.setProjectId(request.getProjectId());
            system.setName(request.getName());
            system.setSourceFrom(request.getSourceFrom());
            system.setCoreResource(request.getCoreResource());
            system.setCultivationMethod(request.getCultivationMethod());
            system.setDescription(request.getDescription());
            system.setCreateTime(now);
            system.setUpdateTime(now);
            powerSystemMapper.insert(system);
        }

        // 保存等级和境界
        if (request.getLevels() != null) {
            for (int i = 0; i < request.getLevels().size(); i++) {
                PowerSystemLevelSaveRequest levelReq = request.getLevels().get(i);
                NovelPowerSystemLevel levelEntity = new NovelPowerSystemLevel();
                levelEntity.setPowerSystemId(system.getId());
                levelEntity.setLevel(levelReq.getLevel() != null ? levelReq.getLevel() : i + 1);
                levelEntity.setLevelName(levelReq.getLevelName());
                levelEntity.setDescription(levelReq.getDescription());
                levelEntity.setBreakthroughCondition(levelReq.getBreakthroughCondition());
                levelEntity.setLifespan(levelReq.getLifespan());
                levelEntity.setPowerRange(levelReq.getPowerRange());
                levelEntity.setLandmarkAbility(levelReq.getLandmarkAbility());
                levelEntity.setCreateTime(now);
                levelEntity.setUpdateTime(now);
                levelMapper.insert(levelEntity);

                // 保存境界
                if (levelReq.getSteps() != null) {
                    for (int j = 0; j < levelReq.getSteps().size(); j++) {
                        PowerSystemLevelStepSaveRequest stepReq = levelReq.getSteps().get(j);
                        NovelPowerSystemLevelStep stepEntity = new NovelPowerSystemLevelStep();
                        stepEntity.setPowerSystemLevelId(levelEntity.getId());
                        stepEntity.setLevel(stepReq.getLevel() != null ? stepReq.getLevel() : j + 1);
                        stepEntity.setLevelName(stepReq.getLevelName());
                        stepEntity.setCreateTime(now);
                        stepEntity.setUpdateTime(now);
                        stepMapper.insert(stepEntity);
                    }
                }
            }
        }

        log.info("保存力量体系成功，ID: {}", system.getId());
        return system;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // 级联删除关联表
        worldviewPowerSystemMapper.delete(
            new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                .eq(NovelWorldviewPowerSystem::getPowerSystemId, id)
        );
        deleteLevelsBySystemId(id);
        powerSystemMapper.deleteById(id);
        log.info("删除力量体系，ID: {}", id);
    }

    @Override
    public String buildPowerSystemConstraint(Long projectId) {
        List<NovelPowerSystem> systems = listByProjectId(projectId);
        if (systems.isEmpty()) {
            return "本小说未设定修炼体系，所有角色的修为字段留空（不输出V标签）。\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("本小说的修炼体系设定如下：\n\n");

        for (NovelPowerSystem system : systems) {
            sb.append("【").append(system.getName()).append("】");
            sb.append("（能量来源：").append(system.getSourceFrom() != null ? system.getSourceFrom() : "无");
            if (system.getCoreResource() != null) {
                sb.append(" | 核心资源：").append(system.getCoreResource());
            }
            if (system.getCultivationMethod() != null) {
                sb.append(" | 修炼方式：").append(system.getCultivationMethod());
            }
            sb.append("）\n");
            if (system.getDescription() != null) {
                sb.append("描述：").append(system.getDescription()).append("\n");
            }

            List<NovelPowerSystemLevel> levels = levelMapper.selectList(
                new LambdaQueryWrapper<NovelPowerSystemLevel>()
                    .eq(NovelPowerSystemLevel::getPowerSystemId, system.getId())
                    .orderByAsc(NovelPowerSystemLevel::getLevel)
            );

            sb.append("等级划分：\n");
            for (NovelPowerSystemLevel level : levels) {
                // 获取境界
                List<NovelPowerSystemLevelStep> steps = stepMapper.selectList(
                    new LambdaQueryWrapper<NovelPowerSystemLevelStep>()
                        .eq(NovelPowerSystemLevelStep::getPowerSystemLevelId, level.getId())
                        .orderByAsc(NovelPowerSystemLevelStep::getLevel)
                );
                String stepStr = steps.stream()
                    .map(NovelPowerSystemLevelStep::getLevelName)
                    .collect(Collectors.joining("/"));

                sb.append("  ").append(level.getLevelName());
                if (!stepStr.isEmpty()) {
                    sb.append("（").append(stepStr).append("）");
                }
                sb.append("\n");
                if (level.getDescription() != null) {
                    sb.append("    描述：").append(level.getDescription());
                }
                if (level.getLifespan() != null) {
                    sb.append(" | 寿命：").append(level.getLifespan());
                }
                if (level.getLandmarkAbility() != null) {
                    sb.append(" | 标志能力：").append(level.getLandmarkAbility());
                }
                sb.append("\n");
                if (level.getBreakthroughCondition() != null) {
                    sb.append("    突破条件：").append(level.getBreakthroughCondition()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("**重要约束：**\n");
        sb.append("1. 角色的修为等级**必须**严格使用上述修炼体系中的等级名称\n");
        sb.append("2. **不得**随意创造新的修炼体系或等级\n");
        sb.append("3. 如果章节中没有明确提到角色的修为等级，则不输出V标签\n");
        sb.append("4. 如果角色修炼多套体系，每套体系分别用一个V标签列出\n");

        return sb.toString();
    }

    private void deleteLevelsBySystemId(Long systemId) {
        List<NovelPowerSystemLevel> levels = levelMapper.selectList(
            new LambdaQueryWrapper<NovelPowerSystemLevel>()
                .eq(NovelPowerSystemLevel::getPowerSystemId, systemId)
        );
        for (NovelPowerSystemLevel level : levels) {
            stepMapper.delete(
                new LambdaQueryWrapper<NovelPowerSystemLevelStep>()
                    .eq(NovelPowerSystemLevelStep::getPowerSystemLevelId, level.getId())
            );
        }
        levelMapper.delete(
            new LambdaQueryWrapper<NovelPowerSystemLevel>()
                .eq(NovelPowerSystemLevel::getPowerSystemId, systemId)
        );
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add ai-factory-backend/src/main/java/com/aifactory/dto/PowerSystem*.java ai-factory-backend/src/main/java/com/aifactory/service/PowerSystemService.java ai-factory-backend/src/main/java/com/aifactory/service/impl/PowerSystemServiceImpl.java
git commit -m "feat: 添加PowerSystemService及DTO，含buildPowerSystemConstraint公共方法"
```

---

## Task 4: PowerSystemController — REST API

**Files:**
- Create: `ai-factory-backend/src/main/java/com/aifactory/controller/PowerSystemController.java`

- [ ] **Step 1: 创建 Controller**

```java
// ai-factory-backend/src/main/java/com/aifactory/controller/PowerSystemController.java
package com.aifactory.controller;

import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.response.Result;
import com.aifactory.service.PowerSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/power-system")
@Tag(name = "力量体系管理", description = "力量体系的增删改查API")
public class PowerSystemController {

    @Autowired
    private PowerSystemService powerSystemService;

    @Operation(summary = "获取项目下所有力量体系")
    @GetMapping("/list")
    public Result<List<NovelPowerSystem>> list(@PathVariable Long projectId) {
        try {
            return Result.ok(powerSystemService.listByProjectId(projectId));
        } catch (Exception e) {
            log.error("查询力量体系失败", e);
            return Result.error("查询力量体系失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取单个力量体系详情")
    @GetMapping("/{id}")
    public Result<NovelPowerSystem> getById(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            return Result.ok(powerSystemService.getById(id));
        } catch (Exception e) {
            log.error("查询力量体系详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增/更新力量体系")
    @PostMapping("/save")
    public Result<NovelPowerSystem> save(@PathVariable Long projectId, @RequestBody PowerSystemSaveRequest request) {
        try {
            request.setProjectId(projectId);
            return Result.ok(powerSystemService.savePowerSystem(request));
        } catch (Exception e) {
            log.error("保存力量体系失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除力量体系")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            powerSystemService.deleteById(id);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除力量体系失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add ai-factory-backend/src/main/java/com/aifactory/controller/PowerSystemController.java
git commit -m "feat: 添加PowerSystemController REST API"
```

---

## Task 5: 改造 WorldviewTaskStrategy — 提示词模板化 + XML 解析 + 力量体系入库

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java`

- [ ] **Step 1: 改造 buildWorldviewPrompt 方法**

将硬编码的提示词构建改为完全使用模板系统。修改 `buildWorldviewPrompt` 方法（约221-263行），将XML格式要求和力量体系子节点格式都放入模板：

```java
/**
 * 构建生成世界观的提示词
 * 全部通过模板系统生成，不再硬编码
 */
private String buildWorldviewPrompt(String projectDescription, String storyTone, String storyGenre, String tags) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
    variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
    variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");
    variables.put("tags", tags != null && !tags.isEmpty() ? tags : "");

    String templateCode = "llm_worldview_create";
    return promptTemplateService.executeTemplate(templateCode, variables);
}
```

- [ ] **Step 2: 改造 parseAndSaveWorldview 方法**

将原来简单提取 powerSystem 字符串改为解析嵌套 XML 并入库力量体系。替换 `parseAndSaveWorldview` 方法（约268-343行）：

```java
/**
 * 解析并保存世界观设定
 */
private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
    try {
        // 解析基础世界观字段（使用简化标签）
        Map<String, String> worldviewData = XmlParser.parseXml(
            aiResponse, "w", "t", "b", "g", "f", "l", "r"
        );

        if (worldviewData.isEmpty()) {
            log.warn("解析世界观XML失败，数据为空");
            return null;
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.error("项目不存在，projectId={}", projectId);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        NovelWorldview worldview = new NovelWorldview();
        worldview.setUserId(project.getUserId());
        worldview.setProjectId(projectId);
        worldview.setWorldType(worldviewData.getOrDefault("t", storyGenre));
        worldview.setWorldBackground(worldviewData.getOrDefault("b", ""));
        worldview.setGeography(worldviewData.getOrDefault("g", ""));
        worldview.setForces(worldviewData.getOrDefault("f", ""));
        worldview.setTimeline(worldviewData.getOrDefault("l", ""));
        worldview.setRules(worldviewData.getOrDefault("r", ""));
        worldview.setCreateTime(now);
        worldview.setUpdateTime(now);
        worldviewMapper.insert(worldview);
        log.info("世界观设定保存成功，ID: {}", worldview.getId());

        // 解析力量体系并入库
        parseAndSavePowerSystems(projectId, worldview.getId(), aiResponse);

        return worldview;
    } catch (Exception e) {
        log.error("解析世界观失败", e);
        log.error("AI响应: {}", aiResponse);
        return null;
    }
}

/**
 * 从AI响应中解析力量体系XML并入库
 */
private void parseAndSavePowerSystems(Long projectId, Long worldviewId, String aiResponse) {
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setNamespaceAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(aiResponse)));

        // 查找 powerSystem 节点（可能在不同位置）
        NodeList systemNodes = document.getElementsByTagName("system");
        if (systemNodes.getLength() == 0) {
            // 尝试简化标签 p
            Map<String, String> data = XmlParser.parseXml(aiResponse, "w", "p");
            if (data.containsKey("p")) {
                log.info("未找到结构化力量体系，使用文本模式");
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < systemNodes.getLength(); i++) {
            Element systemEl = (Element) systemNodes.item(i);

            // 保存力量体系
            NovelPowerSystem powerSystem = new NovelPowerSystem();
            powerSystem.setProjectId(projectId);
            powerSystem.setName(getElementText(systemEl, "name"));
            powerSystem.setSourceFrom(getElementText(systemEl, "sourceFrom"));
            powerSystem.setCoreResource(getElementText(systemEl, "coreResource"));
            powerSystem.setCultivationMethod(getElementText(systemEl, "cultivationMethod"));
            powerSystem.setDescription(getElementText(systemEl, "description"));
            powerSystem.setCreateTime(now);
            powerSystem.setUpdateTime(now);
            powerSystemMapper.insert(powerSystem);

            // 保存关联
            NovelWorldviewPowerSystem relation = new NovelWorldviewPowerSystem();
            relation.setWorldviewId(worldviewId);
            relation.setPowerSystemId(powerSystem.getId());
            worldviewPowerSystemMapper.insert(relation);

            // 解析等级
            NodeList levelNodes = systemEl.getElementsByTagName("level");
            for (int j = 0; j < levelNodes.getLength(); j++) {
                Element levelEl = (Element) levelNodes.item(j);
                NovelPowerSystemLevel levelEntity = new NovelPowerSystemLevel();
                levelEntity.setPowerSystemId(powerSystem.getId());
                levelEntity.setLevel(j + 1);
                levelEntity.setLevelName(getElementText(levelEl, "levelName"));
                levelEntity.setDescription(getElementText(levelEl, "description"));
                levelEntity.setBreakthroughCondition(getElementText(levelEl, "breakthroughCondition"));
                levelEntity.setLifespan(getElementText(levelEl, "lifespan"));
                levelEntity.setPowerRange(getElementText(levelEl, "powerRange"));
                levelEntity.setLandmarkAbility(getElementText(levelEl, "landmarkAbility"));
                levelEntity.setCreateTime(now);
                levelEntity.setUpdateTime(now);
                levelMapper.insert(levelEntity);

                // 解析境界
                NodeList stepNodes = levelEl.getElementsByTagName("step");
                for (int k = 0; k < stepNodes.getLength(); k++) {
                    Element stepEl = (Element) stepNodes.item(k);
                    NovelPowerSystemLevelStep stepEntity = new NovelPowerSystemLevelStep();
                    stepEntity.setPowerSystemLevelId(levelEntity.getId());
                    stepEntity.setLevel(k + 1);
                    stepEntity.setLevelName(stepEl.getTextContent().trim());
                    stepEntity.setCreateTime(now);
                    stepEntity.setUpdateTime(now);
                    stepMapper.insert(stepEntity);
                }
            }

            log.info("解析力量体系成功：{}", powerSystem.getName());
        }
    } catch (Exception e) {
        log.error("解析力量体系失败", e);
    }
}

/**
 * 安全获取XML元素的文本内容
 */
private String getElementText(Element parent, String tagName) {
    NodeList list = parent.getElementsByTagName(tagName);
    if (list.getLength() == 0) return null;
    String text = list.item(0).getTextContent();
    return text != null && !text.trim().isEmpty() ? text.trim() : null;
}
```

- [ ] **Step 3: 添加必要的 import 和依赖注入**

在 WorldviewTaskStrategy.java 顶部添加：
```java
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.entity.NovelPowerSystemLevel;
import com.aifactory.entity.NovelPowerSystemLevelStep;
import com.aifactory.entity.NovelWorldviewPowerSystem;
import com.aifactory.mapper.NovelPowerSystemMapper;
import com.aifactory.mapper.NovelPowerSystemLevelMapper;
import com.aifactory.mapper.NovelPowerSystemLevelStepMapper;
import com.aifactory.mapper.NovelWorldviewPowerSystemMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
```

在类中添加依赖注入：
```java
@Autowired
private NovelPowerSystemMapper powerSystemMapper;
@Autowired
private NovelPowerSystemLevelMapper levelMapper;
@Autowired
private NovelPowerSystemLevelStepMapper stepMapper;
@Autowired
private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;
```

- [ ] **Step 4: 提交**

```bash
git add ai-factory-backend/src/main/java/com/aifactory/service/task/impl/WorldviewTaskStrategy.java
git commit -m "feat: 改造WorldviewTaskStrategy，支持力量体系XML解析入库和模板化提示词"
```

---

## Task 6: 更新提示词模板

**Files:** 数据库 `ai_prompt_template_version` 表 id=3 的记录

- [ ] **Step 1: 更新数据库中 id=3 的模板内容**

执行以下 SQL，将力量体系的 XML 子节点格式要求写入模板：

```sql
UPDATE ai_prompt_template_version
SET template_content = '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以下项目描述，为这部小说创建完整的世界观设定：

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

请创建包含以下内容的世界观设定：
1. 世界类型（现实/奇幻/科幻/修真等）
2. 力量体系（支持多套修炼体系，包含等级划分、境界划分、突破条件等详细信息）
3. 地理环境（大陆、国家、重要地点）
4. 势力分布（阵营、组织、重要势力）
5. 核心设定（与故事相关的特殊规则）

【重要】请严格按照以下XML格式返回世界观设定（使用简化标签节省token）：
<w>
  <t>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</t>
  <b><![CDATA[世界背景描述（200-300字）]]></b>
  <p>
    <system>
      <name>体系名称（如：修仙）</name>
      <sourceFrom>能量来源（如：天地灵气）</sourceFrom>
      <coreResource>核心资源（如：灵石）</coreResource>
      <cultivationMethod>修炼方式（如：打坐冥想）</cultivationMethod>
      <description><![CDATA[体系整体描述]]></description>
      <levels>
        <level>
          <levelName>等级名称（如：练气期）</levelName>
          <description><![CDATA[等级描述，能做什么]]></description>
          <breakthroughCondition><![CDATA[突破到下一等级的条件]]></breakthroughCondition>
          <lifespan>寿命范围（如：约150年）</lifespan>
          <powerRange><![CDATA[战力描述]]></powerRange>
          <landmarkAbility>标志性能力（如：灵气外放）</landmarkAbility>
          <steps>
            <step>境界名称（如：前期）</step>
            <step>境界名称（如：中期）</step>
            <step>境界名称（如：后期）</step>
            <step>境界名称（如：大圆满）</step>
          </steps>
        </level>
        <level>
          ...更多等级...
        </level>
      </levels>
    </system>
    <system>
      ...更多修炼体系（如有）...
    </system>
  </p>
  <g><![CDATA[地理环境描述]]></g>
  <f><![CDATA[势力分布描述]]></f>
  <l><![CDATA[时间线设定]]></l>
  <r><![CDATA[世界的基本规则和限制]]></r>
</w>

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记（```xml），直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据
4. 力量体系<p>标签下可以包含多个<system>，代表多套修炼体系
5. 每个体系必须有至少2-3个等级，每个等级至少有2-3个境界
6. 如果故事类型不需要力量体系（如纯都市日常），<p>标签可以为空

内容要求：
1. 世界观要符合故事类型和基调
2. 力量体系要清晰、合理，有可发展性，等级之间要有明显的实力差距
3. 各个要素之间要相互关联，形成完整的世界
4. 返回的必须是纯XML格式，不要有任何其他说明文字',
version_comment = '力量体系结构化重构，支持XML子节点格式的多套修炼体系'
WHERE id = 3;
```

注意：模板中的 `{tagsSection}` 变量需要替换逻辑。在 `buildWorldviewPrompt` 中处理：
```java
String tagsSection = "";
if (tags != null && !tags.isEmpty()) {
    tagsSection = "【标签】" + tags;
}
variables.put("tagsSection", tagsSection);
```

- [ ] **Step 2: 更新模板变量定义**

```sql
UPDATE ai_prompt_template_version
SET variable_definitions = '[{"desc":"项目描述","name":"projectDescription","type":"string","required":true},{"desc":"故事类型","name":"storyGenre","type":"string","required":true},{"desc":"故事基调","name":"storyTone","type":"string","required":true},{"desc":"标签信息","name":"tagsSection","type":"string","required":false}]'
WHERE id = 3;
```

- [ ] **Step 3: 提交迁移SQL**

将上面的 SQL 语句追加到 `sql/power_system_migration.sql` 文件末尾并提交。

```bash
git add sql/power_system_migration.sql
git commit -m "feat: 添加提示词模板更新SQL"
```

---

## Task 7: 重构 buildPowerSystemConstraint 调用方

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java`
- Modify: `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java`

- [ ] **Step 1: 修改 ChapterCharacterExtractService**

在 `ChapterCharacterExtractService.java` 中：
1. 注入 `PowerSystemService`：
```java
@Autowired
private PowerSystemService powerSystemService;
```

2. 替换 `buildPowerSystemConstraint` 方法（约285-316行）为调用公共方法：
```java
private String buildPowerSystemConstraint(NovelWorldview worldview) {
    if (worldview == null || worldview.getProjectId() == null) {
        return "本小说未设定修炼体系，所有角色的修为字段留空（不输出V标签）。\n";
    }
    return "\n## 修炼体系设定\n" + powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
}
```

- [ ] **Step 2: 修改 PromptTemplateBuilder**

在 `PromptTemplateBuilder.java` 中：
1. 注入 `PowerSystemService`：
```java
@Autowired
private PowerSystemService powerSystemService;
```

2. 替换 `buildPowerSystemConstraint` 方法（约431-460行）：
```java
public String buildPowerSystemConstraint(NovelWorldview worldview) {
    if (worldview == null || worldview.getProjectId() == null) {
        return "本小说未设定修炼体系，所有角色的修为字段留空（不输出V标签）。\n";
    }
    return powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
}
```

- [ ] **Step 3: 提交**

```bash
git add ai-factory-backend/src/main/java/com/aifactory/service/ChapterCharacterExtractService.java ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/PromptTemplateBuilder.java
git commit -m "refactor: buildPowerSystemConstraint改为调用PowerSystemService公共方法"
```

---

## Task 8: 修复其他引用 worldview.powerSystem 的代码

**Files:**
- Modify: `ai-factory-backend/src/main/java/com/aifactory/controller/WorldviewController.java`
- 搜索所有引用 `getPowerSystem()` 或 `setPowerSystem()` 的文件并修改

- [ ] **Step 1: 全局搜索 powerSystem 引用**

搜索所有包含 `powerSystem` 或 `getPowerSystem()` 或 `setPowerSystem()` 的 Java 文件，逐一修改移除对 `worldview.powerSystem` 字段的引用。重点文件：
- `WorldviewController.java` — `parseAndSaveWorldview` 方法中会设置 powerSystem 字段
- 其他可能引用的文件

- [ ] **Step 2: 修改 WorldviewController.java**

移除 `WorldviewController.java` 中所有对 `worldview.setPowerSystem()` 和 `worldview.getPowerSystem()` 的调用。

- [ ] **Step 3: 编译验证**

```bash
cd ai-factory-backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: 移除所有worldview.powerSystem字段引用"
```

---

## Task 9: 前端 API 层

**Files:**
- Create: `ai-factory-frontend/src/api/powerSystem.ts`
- Modify: `ai-factory-frontend/src/api/worldview.ts` — 移除 powerSystem 字段

- [ ] **Step 1: 创建力量体系 API 文件**

```typescript
// ai-factory-frontend/src/api/powerSystem.ts
import request from '@/utils/request'

// 力量体系类型
export interface PowerSystemLevelStep {
  id?: number
  level?: number
  levelName: string
}

export interface PowerSystemLevel {
  id?: number
  level?: number
  levelName: string
  description?: string
  breakthroughCondition?: string
  lifespan?: string
  powerRange?: string
  landmarkAbility?: string
  steps?: PowerSystemLevelStep[]
}

export interface PowerSystem {
  id?: number
  projectId?: number
  name: string
  sourceFrom?: string
  coreResource?: string
  cultivationMethod?: string
  description?: string
  createTime?: string
  updateTime?: string
  levels?: PowerSystemLevel[]
}

// 获取项目下所有力量体系
export const getPowerSystemList = (projectId: string) => {
  return request.get<PowerSystem[]>(`/api/novel/${projectId}/power-system/list`)
}

// 获取单个力量体系详情
export const getPowerSystemDetail = (projectId: string, id: number) => {
  return request.get<PowerSystem>(`/api/novel/${projectId}/power-system/${id}`)
}

// 保存力量体系
export const savePowerSystem = (projectId: string, data: PowerSystem) => {
  return request.post<PowerSystem>(`/api/novel/${projectId}/power-system/save`, data)
}

// 删除力量体系
export const deletePowerSystem = (projectId: string, id: number) => {
  return request.delete(`/api/novel/${projectId}/power-system/${id}`)
}
```

- [ ] **Step 2: 修改 worldview.ts — 移除 powerSystem 字段**

从 `Worldview` 接口中移除 `powerSystem?: string` 字段。

- [ ] **Step 3: 提交**

```bash
git add ai-factory-frontend/src/api/powerSystem.ts ai-factory-frontend/src/api/worldview.ts
git commit -m "feat: 添加力量体系前端API层，移除worldview.powerSystem"
```

---

## Task 10: 前端力量体系组件

**Files:**
- Create: `ai-factory-frontend/src/views/Project/Detail/components/PowerSystemSection.vue`
- Create: `ai-factory-frontend/src/views/Project/Detail/components/PowerSystemCard.vue`
- Create: `ai-factory-frontend/src/views/Project/Detail/components/PowerSystemForm.vue`

- [ ] **Step 1: 创建 PowerSystemSection.vue（容器组件）**

替换原来的力量体系 textarea。负责：卡片列表展示、添加按钮、AI 生成后刷新。

关键功能：
- 调用 `getPowerSystemList` 加载数据
- 遍历渲染 `PowerSystemCard`
- 添加按钮打开 `PowerSystemForm` 弹窗
- 提供 `refresh()` 方法供父组件（WorldSetting.vue）在 AI 生成后调用

- [ ] **Step 2: 创建 PowerSystemCard.vue（卡片组件）**

单个力量体系卡片：
- 展示态：显示体系名称、能量来源、修炼方式摘要
- 展开态：点击卡片展开，显示完整信息和等级列表
- 每个等级行可展开显示详细属性和境界列表
- 右上角编辑/删除按钮

- [ ] **Step 3: 创建 PowerSystemForm.vue（表单弹窗）**

弹窗形式的编辑表单：
- 上半部分：体系基本信息（name, sourceFrom, coreResource, cultivationMethod, description）
- 下半部分：等级列表编辑区
  - 每个等级可展开编辑所有字段 + 境界列表
  - 境界支持增删和排序
  - 等级支持增删和排序
- 保存调用 `savePowerSystem` API

- [ ] **Step 4: 提交**

```bash
git add ai-factory-frontend/src/views/Project/Detail/components/PowerSystemSection.vue ai-factory-frontend/src/views/Project/Detail/components/PowerSystemCard.vue ai-factory-frontend/src/views/Project/Detail/components/PowerSystemForm.vue
git commit -m "feat: 添加前端力量体系组件（Section/Card/Form）"
```

---

## Task 11: 修改 WorldSetting.vue — 集成力量体系组件

**Files:**
- Modify: `ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue`

- [ ] **Step 1: 替换力量体系区域**

1. 移除 `formData.powerSystem` 及其 textarea
2. 引入 `PowerSystemSection` 组件
3. 在模板中用 `<PowerSystemSection ref="powerSystemRef" :project-id="projectId()" :disabled="generating" />` 替换原来的 textarea
4. AI 生成完成后调用 `powerSystemRef.value.refresh()` 刷新力量体系数据
5. 保存时不再提交 powerSystem 字段

具体改动：
- 在 `<script setup>` 中添加 `import PowerSystemSection from './components/PowerSystemSection.vue'`
- 添加 `const powerSystemRef = ref()`
- 在 `loadData` 成功后不再处理 powerSystem
- 在 `handleGenerate` 成功后添加 `powerSystemRef.value?.refresh()`
- 从 `formData` 中移除 `powerSystem` 字段
- 在模板中删除力量体系 textarea block（约315-327行），替换为 `<PowerSystemSection />`

- [ ] **Step 2: 验证前端编译**

```bash
cd ai-factory-frontend && npm run build 2>&1 | head -20
```

Expected: 无 TypeScript 编译错误

- [ ] **Step 3: 提交**

```bash
git add ai-factory-frontend/src/views/Project/Detail/WorldSetting.vue
git commit -m "feat: WorldSetting集成力量体系组件，替换原powerSystem文本域"
```

---

## Task 12: 端到端验证

- [ ] **Step 1: 后端编译验证**

```bash
cd ai-factory-backend && mvn compile -q
```

- [ ] **Step 2: 前端编译验证**

```bash
cd ai-factory-frontend && npm run build 2>&1 | head -20
```

- [ ] **Step 3: 数据库验证**

连接数据库确认 4 张新表已创建，`novel_worldview` 表的 `power_system` 字段已移除。

- [ ] **Step 4: 最终提交（如有修复）**

```bash
git add -A
git commit -m "fix: 端到端验证修复"
```
