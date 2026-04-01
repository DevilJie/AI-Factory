package com.aifactory.service.impl;

import com.aifactory.entity.*;
import com.aifactory.mapper.*;
import com.aifactory.service.FactionService;
import com.aifactory.service.PowerSystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FactionServiceImpl implements FactionService {

    @Autowired
    private NovelFactionMapper factionMapper;

    @Autowired
    private NovelFactionRegionMapper factionRegionMapper;

    @Autowired
    private NovelFactionCharacterMapper factionCharacterMapper;

    @Autowired
    private NovelFactionRelationMapper factionRelationMapper;

    @Autowired
    private NovelContinentRegionMapper continentRegionMapper;

    @Autowired
    private PowerSystemService powerSystemService;

    // ======================== Public Methods ========================

    @Override
    public List<NovelFaction> getTreeByProjectId(Long projectId) {
        List<NovelFaction> allFactions = factionMapper.selectList(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getProjectId, projectId)
                .orderByAsc(NovelFaction::getSortOrder)
                .orderByAsc(NovelFaction::getId)
        );

        List<NovelFaction> tree = buildTree(allFactions);
        inheritRootValues(tree);
        return tree;
    }

    @Override
    public List<NovelFaction> listByProjectId(Long projectId) {
        return factionMapper.selectList(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getProjectId, projectId)
                .orderByAsc(NovelFaction::getSortOrder)
        );
    }

    @Override
    @Transactional
    public NovelFaction addFaction(NovelFaction faction) {
        // Auto-calculate deep
        if (faction.getParentId() == null) {
            faction.setDeep(0);
        } else {
            NovelFaction parent = factionMapper.selectById(faction.getParentId());
            if (parent != null) {
                faction.setDeep(parent.getDeep() + 1);
            } else {
                faction.setDeep(0);
                faction.setParentId(null);
            }
        }

        // Auto-calculate sortOrder (append to end of siblings)
        if (faction.getSortOrder() == null || faction.getSortOrder() == 0) {
            Long maxSort = getMaxSortOrder(faction.getProjectId(), faction.getParentId());
            faction.setSortOrder(maxSort != null ? maxSort.intValue() + 1 : 1);
        }

        LocalDateTime now = LocalDateTime.now();
        faction.setCreateTime(now);
        faction.setUpdateTime(now);
        factionMapper.insert(faction);

        log.info("新增势力成功，id={}, name={}, projectId={}", faction.getId(), faction.getName(), faction.getProjectId());
        return faction;
    }

    @Override
    @Transactional
    public NovelFaction updateFaction(NovelFaction faction) {
        NovelFaction existing = factionMapper.selectById(faction.getId());
        if (existing == null) {
            throw new RuntimeException("势力不存在，id=" + faction.getId());
        }

        // If parent changed, recalculate deep
        if (faction.getParentId() != null && !faction.getParentId().equals(existing.getParentId())) {
            if (faction.getParentId() == 0L || faction.getParentId().equals(0L)) {
                faction.setParentId(null);
                faction.setDeep(0);
            } else {
                NovelFaction parent = factionMapper.selectById(faction.getParentId());
                if (parent != null) {
                    faction.setDeep(parent.getDeep() + 1);
                }
            }
            // Cascade update children deep
            updateChildrenDeep(faction.getId(), faction.getDeep());
        }

        faction.setUpdateTime(LocalDateTime.now());
        factionMapper.updateById(faction);

        log.info("更新势力成功，id={}, name={}", faction.getId(), faction.getName());
        return faction;
    }

    @Override
    @Transactional
    public void deleteFaction(Long id) {
        // Collect all descendant IDs recursively
        List<Long> idsToDelete = collectDescendantIds(id);
        idsToDelete.add(id);

        // Delete from association tables
        for (Long deleteId : idsToDelete) {
            factionRegionMapper.delete(
                new LambdaQueryWrapper<NovelFactionRegion>()
                    .eq(NovelFactionRegion::getFactionId, deleteId)
            );
            factionCharacterMapper.delete(
                new LambdaQueryWrapper<NovelFactionCharacter>()
                    .eq(NovelFactionCharacter::getFactionId, deleteId)
            );
        }

        // Delete relations in both directions (faction_id and target_faction_id)
        factionRelationMapper.delete(
            new LambdaQueryWrapper<NovelFactionRelation>()
                .in(NovelFactionRelation::getFactionId, idsToDelete)
                .or()
                .in(NovelFactionRelation::getTargetFactionId, idsToDelete)
        );

        // Delete faction records
        factionMapper.deleteBatchIds(idsToDelete);

        log.info("删除势力及子节点，共删除{}条势力记录", idsToDelete.size());
    }

    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        // Query all faction IDs for the project
        List<NovelFaction> allFactions = factionMapper.selectList(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getProjectId, projectId)
                .select(NovelFaction::getId)
        );

        List<Long> factionIds = allFactions.stream()
            .map(NovelFaction::getId)
            .collect(Collectors.toList());

        if (!factionIds.isEmpty()) {
            // Delete from association tables
            factionRegionMapper.delete(
                new LambdaQueryWrapper<NovelFactionRegion>()
                    .in(NovelFactionRegion::getFactionId, factionIds)
            );
            factionCharacterMapper.delete(
                new LambdaQueryWrapper<NovelFactionCharacter>()
                    .in(NovelFactionCharacter::getFactionId, factionIds)
            );
            // Delete relations in both directions
            factionRelationMapper.delete(
                new LambdaQueryWrapper<NovelFactionRelation>()
                    .in(NovelFactionRelation::getFactionId, factionIds)
                    .or()
                    .in(NovelFactionRelation::getTargetFactionId, factionIds)
            );
        }

        // Delete all faction records
        factionMapper.delete(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getProjectId, projectId)
        );

        log.info("已删除项目所有势力数据，projectId={}", projectId);
    }

    @Override
    @Transactional
    public void saveTree(Long projectId, List<NovelFaction> rootNodes) {
        if (rootNodes == null || rootNodes.isEmpty()) {
            return;
        }

        int sortOrder = 1;
        for (NovelFaction rootNode : rootNodes) {
            rootNode.setProjectId(projectId);
            rootNode.setParentId(null);
            rootNode.setDeep(0);
            rootNode.setSortOrder(sortOrder++);
            saveNodeRecursive(rootNode, projectId);
        }

        log.info("势力树保存完成，projectId={}，根节点数={}", projectId, rootNodes.size());
    }

    @Override
    public String buildFactionText(Long projectId) {
        List<NovelFaction> tree = getTreeByProjectId(projectId);
        if (tree == null || tree.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        buildFactionTextRecursive(tree, 0, null, null, sb);
        return sb.toString().trim();
    }

    @Override
    public void fillForces(NovelWorldview worldview) {
        if (worldview == null || worldview.getProjectId() == null) return;
        String text = buildFactionText(worldview.getProjectId());
        worldview.setForces(text.isEmpty() ? null : text);
    }

    // ======================== Private Methods ========================

    /**
     * Build tree structure from flat list
     */
    private List<NovelFaction> buildTree(List<NovelFaction> allFactions) {
        if (allFactions == null || allFactions.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<NovelFaction>> childrenMap = allFactions.stream()
            .filter(f -> f.getParentId() != null)
            .collect(Collectors.groupingBy(NovelFaction::getParentId));

        for (NovelFaction faction : allFactions) {
            faction.setChildren(childrenMap.get(faction.getId()));
        }

        return allFactions.stream()
            .filter(f -> f.getParentId() == null)
            .collect(Collectors.toList());
    }

    /**
     * Inherit type and corePowerSystem from root ancestor to all descendants
     */
    private void inheritRootValues(List<NovelFaction> rootNodes) {
        if (rootNodes == null) return;
        for (NovelFaction root : rootNodes) {
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                propagateToChildren(root.getChildren(), root.getType(), root.getCorePowerSystem());
            }
        }
    }

    /**
     * Recursively propagate type and corePowerSystem to descendants
     */
    private void propagateToChildren(List<NovelFaction> children, String rootType, Long rootCorePowerSystem) {
        if (children == null) return;
        for (NovelFaction child : children) {
            child.setType(rootType);
            child.setCorePowerSystem(rootCorePowerSystem);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                propagateToChildren(child.getChildren(), rootType, rootCorePowerSystem);
            }
        }
    }

    /**
     * Recursively collect all descendant node IDs
     */
    private List<Long> collectDescendantIds(Long parentId) {
        List<Long> ids = new ArrayList<>();

        List<NovelFaction> children = factionMapper.selectList(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getParentId, parentId)
        );

        for (NovelFaction child : children) {
            ids.add(child.getId());
            ids.addAll(collectDescendantIds(child.getId()));
        }

        return ids;
    }

    /**
     * Cascade update children deep
     */
    private void updateChildrenDeep(Long parentId, int parentDeep) {
        List<NovelFaction> children = factionMapper.selectList(
            new LambdaQueryWrapper<NovelFaction>()
                .eq(NovelFaction::getParentId, parentId)
        );

        for (NovelFaction child : children) {
            child.setDeep(parentDeep + 1);
            child.setUpdateTime(LocalDateTime.now());
            factionMapper.updateById(child);
            updateChildrenDeep(child.getId(), child.getDeep());
        }
    }

    /**
     * Get max sortOrder among siblings
     */
    private Long getMaxSortOrder(Long projectId, Long parentId) {
        LambdaQueryWrapper<NovelFaction> wrapper = new LambdaQueryWrapper<NovelFaction>()
            .eq(NovelFaction::getProjectId, projectId)
            .select(NovelFaction::getSortOrder);

        if (parentId == null) {
            wrapper.isNull(NovelFaction::getParentId);
        } else {
            wrapper.eq(NovelFaction::getParentId, parentId);
        }

        wrapper.orderByDesc(NovelFaction::getSortOrder).last("LIMIT 1");

        NovelFaction last = factionMapper.selectOne(wrapper);
        return last != null ? last.getSortOrder() : 0L;
    }

    /**
     * Recursively save node and its children
     */
    private void saveNodeRecursive(NovelFaction node, Long projectId) {
        LocalDateTime now = LocalDateTime.now();
        node.setProjectId(projectId);
        node.setCreateTime(now);
        node.setUpdateTime(now);
        factionMapper.insert(node);

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            int childSortOrder = 1;
            for (NovelFaction child : node.getChildren()) {
                child.setParentId(node.getId());
                child.setDeep(node.getDeep() + 1);
                child.setSortOrder(childSortOrder++);
                child.setProjectId(projectId);
                saveNodeRecursive(child, projectId);
            }
        }
    }

    /**
     * Build faction text recursively for prompt construction
     */
    private void buildFactionTextRecursive(List<NovelFaction> nodes, int level,
                                            String rootType, String rootPowerSystemName,
                                            StringBuilder sb) {
        if (nodes == null) return;
        String indent = "  ".repeat(level);
        for (NovelFaction node : nodes) {
            // For root nodes, resolve type label and power system name
            String typeLabel = "";
            String powerSystemName = null;

            if (level == 0) {
                typeLabel = formatTypeLabel(node.getType());
                if (node.getCorePowerSystem() != null) {
                    NovelPowerSystem ps = powerSystemService.getById(node.getCorePowerSystem());
                    if (ps != null) {
                        powerSystemName = ps.getName();
                    }
                }
                rootType = typeLabel;
                rootPowerSystemName = powerSystemName;
            } else {
                typeLabel = rootType;
                powerSystemName = rootPowerSystemName;
            }

            // Format: 【typeLabel】 name
            if (!typeLabel.isEmpty()) {
                sb.append(indent).append("【").append(typeLabel).append("】 ");
            } else {
                sb.append(indent);
            }
            sb.append(node.getName());

            // Append power system name for root level
            if (level == 0 && powerSystemName != null) {
                sb.append("（力量体系：").append(powerSystemName).append("）");
            }

            // Append description
            if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                sb.append("：").append(node.getDescription());
            }

            // Append region names
            String regionNames = getRegionNamesForFaction(node.getId());
            if (!regionNames.isEmpty()) {
                sb.append(" | 地区：").append(regionNames);
            }

            sb.append("\n");

            // Recurse into children
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                buildFactionTextRecursive(node.getChildren(), level + 1, rootType, rootPowerSystemName, sb);
            }
        }
    }

    /**
     * Map type string to Chinese label
     */
    private String formatTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "ally" -> "正派";
            case "hostile" -> "反派";
            case "neutral" -> "中立";
            default -> "";
        };
    }

    /**
     * Get region names for a faction via association table
     */
    private String getRegionNamesForFaction(Long factionId) {
        List<NovelFactionRegion> assocList = factionRegionMapper.selectList(
            new LambdaQueryWrapper<NovelFactionRegion>()
                .eq(NovelFactionRegion::getFactionId, factionId));
        if (assocList == null || assocList.isEmpty()) return "";
        List<String> names = new ArrayList<>();
        for (NovelFactionRegion assoc : assocList) {
            NovelContinentRegion region = continentRegionMapper.selectById(assoc.getRegionId());
            if (region != null) names.add(region.getName());
        }
        return String.join("、", names);
    }
}
