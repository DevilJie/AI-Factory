package com.aifactory.service;

import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.NovelWorldview;

import java.util.List;

/**
 * 大陆区域服务接口
 */
public interface ContinentRegionService {

    /**
     * 获取项目的地理区域树
     */
    List<NovelContinentRegion> getTreeByProjectId(Long projectId);

    /**
     * 获取项目的地理区域平铺列表
     */
    List<NovelContinentRegion> listByProjectId(Long projectId);

    /**
     * 新增区域节点
     */
    NovelContinentRegion addRegion(NovelContinentRegion region);

    /**
     * 更新区域节点
     */
    NovelContinentRegion updateRegion(NovelContinentRegion region);

    /**
     * 删除区域节点（级联删除子节点）
     */
    void deleteRegion(Long id);

    /**
     * 批量保存地理区域树（AI生成后使用）
     */
    void saveTree(Long projectId, List<NovelContinentRegion> rootNodes);

    /**
     * 删除项目下所有区域
     */
    void deleteByProjectId(Long projectId);

    /**
     * 将地理区域树转换为文本描述（用于提示词构建）
     */
    String buildGeographyText(Long projectId);

    /**
     * 填充世界观实体的 geography 字段（从区域树构建文本）
     */
    void fillGeography(NovelWorldview worldview);
}
