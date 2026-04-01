package com.aifactory.service.impl;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.mapper.NovelContinentRegionMapper;
import com.aifactory.service.ContinentRegionService;
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
public class ContinentRegionServiceImpl implements ContinentRegionService {

    @Autowired
    private NovelContinentRegionMapper regionMapper;

    @Override
    public List<NovelContinentRegion> getTreeByProjectId(Long projectId) {
        List<NovelContinentRegion> allRegions = regionMapper.selectList(
            new LambdaQueryWrapper<NovelContinentRegion>()
                .eq(NovelContinentRegion::getProjectId, projectId)
                .orderByAsc(NovelContinentRegion::getSortOrder)
                .orderByAsc(NovelContinentRegion::getId)
        );

        return buildTree(allRegions);
    }

    @Override
    public List<NovelContinentRegion> listByProjectId(Long projectId) {
        return regionMapper.selectList(
            new LambdaQueryWrapper<NovelContinentRegion>()
                .eq(NovelContinentRegion::getProjectId, projectId)
                .orderByAsc(NovelContinentRegion::getSortOrder)
        );
    }

    @Override
    @Transactional
    public NovelContinentRegion addRegion(NovelContinentRegion region) {
        // 自动计算 deep
        if (region.getParentId() == null) {
            region.setDeep(0);
        } else {
            NovelContinentRegion parent = regionMapper.selectById(region.getParentId());
            if (parent != null) {
                region.setDeep(parent.getDeep() + 1);
            } else {
                region.setDeep(0);
                region.setParentId(null);
            }
        }

        // 自动计算 sort_order（排在同级最后）
        if (region.getSortOrder() == null || region.getSortOrder() == 0) {
            Long maxSort = getMaxSortOrder(region.getProjectId(), region.getParentId());
            region.setSortOrder(maxSort != null ? maxSort.intValue() + 1 : 1);
        }

        LocalDateTime now = LocalDateTime.now();
        region.setCreateTime(now);
        region.setUpdateTime(now);
        regionMapper.insert(region);

        log.info("新增区域成功，id={}, name={}, projectId={}", region.getId(), region.getName(), region.getProjectId());
        return region;
    }

    @Override
    @Transactional
    public NovelContinentRegion updateRegion(NovelContinentRegion region) {
        NovelContinentRegion existing = regionMapper.selectById(region.getId());
        if (existing == null) {
            throw new RuntimeException("区域不存在，id=" + region.getId());
        }

        // 如果更换了父节点，需要重新计算 deep
        if (region.getParentId() != null && !region.getParentId().equals(existing.getParentId())) {
            if (region.getParentId() == 0L || region.getParentId().equals(0L)) {
                region.setParentId(null);
                region.setDeep(0);
            } else {
                NovelContinentRegion parent = regionMapper.selectById(region.getParentId());
                if (parent != null) {
                    region.setDeep(parent.getDeep() + 1);
                }
            }
            // 级联更新子节点的 deep
            updateChildrenDeep(region.getId(), region.getDeep());
        }

        region.setUpdateTime(LocalDateTime.now());
        regionMapper.updateById(region);

        log.info("更新区域成功，id={}, name={}", region.getId(), region.getName());
        return region;
    }

    @Override
    @Transactional
    public void deleteRegion(Long id) {
        // 递归收集所有子节点ID
        List<Long> idsToDelete = collectDescendantIds(id);
        idsToDelete.add(id);

        // 批量删除
        regionMapper.deleteBatchIds(idsToDelete);
        log.info("删除区域及子节点，共删除{}条记录", idsToDelete.size());
    }

    @Override
    @Transactional
    public void saveTree(Long projectId, List<NovelContinentRegion> rootNodes) {
        if (rootNodes == null || rootNodes.isEmpty()) {
            return;
        }

        int sortOrder = 1;
        for (NovelContinentRegion rootNode : rootNodes) {
            rootNode.setProjectId(projectId);
            rootNode.setParentId(null);
            rootNode.setDeep(0);
            rootNode.setSortOrder(sortOrder++);
            saveNodeRecursive(rootNode, projectId);
        }

        log.info("地理区域树保存完成，projectId={}，根节点数={}", projectId, rootNodes.size());
    }

    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        regionMapper.delete(
            new LambdaQueryWrapper<NovelContinentRegion>()
                .eq(NovelContinentRegion::getProjectId, projectId)
        );
        log.info("已删除项目所有区域，projectId={}", projectId);
    }

    // ======================== Private Methods ========================

    /**
     * 递归保存节点及其子节点
     */
    private void saveNodeRecursive(NovelContinentRegion node, Long projectId) {
        LocalDateTime now = LocalDateTime.now();
        node.setProjectId(projectId);
        node.setCreateTime(now);
        node.setUpdateTime(now);
        regionMapper.insert(node);

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            int childSortOrder = 1;
            for (NovelContinentRegion child : node.getChildren()) {
                child.setParentId(node.getId());
                child.setDeep(node.getDeep() + 1);
                child.setSortOrder(childSortOrder++);
                child.setProjectId(projectId);
                saveNodeRecursive(child, projectId);
            }
        }
    }

    /**
     * 构建树形结构
     */
    private List<NovelContinentRegion> buildTree(List<NovelContinentRegion> allRegions) {
        if (allRegions == null || allRegions.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<NovelContinentRegion>> childrenMap = allRegions.stream()
            .filter(r -> r.getParentId() != null)
            .collect(Collectors.groupingBy(NovelContinentRegion::getParentId));

        for (NovelContinentRegion region : allRegions) {
            region.setChildren(childrenMap.get(region.getId()));
        }

        return allRegions.stream()
            .filter(r -> r.getParentId() == null)
            .collect(Collectors.toList());
    }

    /**
     * 递归收集所有后代节点ID
     */
    private List<Long> collectDescendantIds(Long parentId) {
        List<Long> ids = new ArrayList<>();

        List<NovelContinentRegion> children = regionMapper.selectList(
            new LambdaQueryWrapper<NovelContinentRegion>()
                .eq(NovelContinentRegion::getParentId, parentId)
        );

        for (NovelContinentRegion child : children) {
            ids.add(child.getId());
            ids.addAll(collectDescendantIds(child.getId()));
        }

        return ids;
    }

    /**
     * 级联更新子节点深度
     */
    private void updateChildrenDeep(Long parentId, int parentDeep) {
        List<NovelContinentRegion> children = regionMapper.selectList(
            new LambdaQueryWrapper<NovelContinentRegion>()
                .eq(NovelContinentRegion::getParentId, parentId)
        );

        for (NovelContinentRegion child : children) {
            child.setDeep(parentDeep + 1);
            child.setUpdateTime(LocalDateTime.now());
            regionMapper.updateById(child);
            updateChildrenDeep(child.getId(), child.getDeep());
        }
    }

    /**
     * 获取同级最大排序值
     */
    private Long getMaxSortOrder(Long projectId, Long parentId) {
        LambdaQueryWrapper<NovelContinentRegion> wrapper = new LambdaQueryWrapper<NovelContinentRegion>()
            .eq(NovelContinentRegion::getProjectId, projectId)
            .select(NovelContinentRegion::getSortOrder);

        if (parentId == null) {
            wrapper.isNull(NovelContinentRegion::getParentId);
        } else {
            wrapper.eq(NovelContinentRegion::getParentId, parentId);
        }

        wrapper.orderByDesc(NovelContinentRegion::getSortOrder).last("LIMIT 1");

        NovelContinentRegion last = regionMapper.selectOne(wrapper);
        return last != null ? last.getSortOrder() : 0L;
    }

    @Override
    public String buildGeographyText(Long projectId) {
        List<NovelContinentRegion> tree = getTreeByProjectId(projectId);
        if (tree == null || tree.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        buildGeographyTextRecursive(tree, 0, sb);
        return sb.toString().trim();
    }

    private void buildGeographyTextRecursive(List<NovelContinentRegion> nodes, int level, StringBuilder sb) {
        if (nodes == null) return;
        String indent = "  ".repeat(level);
        for (NovelContinentRegion node : nodes) {
            sb.append(indent).append("- ").append(node.getName());
            if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                sb.append("：").append(node.getDescription());
            }
            sb.append("\n");
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                buildGeographyTextRecursive(node.getChildren(), level + 1, sb);
            }
        }
    }

    @Override
    public void fillGeography(NovelWorldview worldview) {
        if (worldview == null || worldview.getProjectId() == null) return;
        String text = buildGeographyText(worldview.getProjectId());
        worldview.setGeography(text.isEmpty() ? null : text);
    }
}
