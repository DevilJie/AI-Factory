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
            system = powerSystemMapper.selectById(request.getId());
            system.setName(request.getName());
            system.setSourceFrom(request.getSourceFrom());
            system.setCoreResource(request.getCoreResource());
            system.setCultivationMethod(request.getCultivationMethod());
            system.setDescription(request.getDescription());
            system.setUpdateTime(now);
            powerSystemMapper.updateById(system);
            deleteLevelsBySystemId(system.getId());
        } else {
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
