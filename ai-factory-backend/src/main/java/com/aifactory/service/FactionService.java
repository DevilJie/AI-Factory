package com.aifactory.service;

import com.aifactory.entity.NovelFaction;
import com.aifactory.entity.NovelWorldview;

import java.util.List;

/**
 * 势力阵营服务接口
 */
public interface FactionService {

    /**
     * 获取项目的势力树（含 type/corePowerSystem 继承）
     */
    List<NovelFaction> getTreeByProjectId(Long projectId);

    /**
     * 获取项目的势力平铺列表
     */
    List<NovelFaction> listByProjectId(Long projectId);

    /**
     * 新增势力节点
     */
    NovelFaction addFaction(NovelFaction faction);

    /**
     * 更新势力节点
     */
    NovelFaction updateFaction(NovelFaction faction);

    /**
     * 删除势力节点（级联删除子节点及关联数据）
     */
    void deleteFaction(Long id);

    /**
     * 批量保存势力树（AI生成后使用）
     */
    void saveTree(Long projectId, List<NovelFaction> rootNodes);

    /**
     * 删除项目下所有势力及关联数据
     */
    void deleteByProjectId(Long projectId);

    /**
     * 将势力树转换为文本描述（用于提示词构建）
     */
    String buildFactionText(Long projectId);

    /**
     * 填充世界观实体的 forces 字段（从势力树构建文本）
     */
    void fillForces(NovelWorldview worldview);
}
