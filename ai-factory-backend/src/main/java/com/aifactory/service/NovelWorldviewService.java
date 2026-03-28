package com.aifactory.service;

import com.aifactory.entity.NovelWorldview;

/**
 * 世界观设定Service
 *
 * @Author CaiZy
 * @Date 2025-01-27
 */
public interface NovelWorldviewService {

    /**
     * 根据项目ID获取世界观设定
     */
    NovelWorldview getByProjectId(Long projectId);

    /**
     * 根据大纲ID获取世界观设定
     */
    NovelWorldview getByOutlineId(Long outlineId);

    /**
     * 保存或更新世界观设定
     */
    NovelWorldview saveOrUpdate(NovelWorldview worldview);

    /**
     * 根据ID删除世界观设定
     */
    void deleteById(Long id);
}
