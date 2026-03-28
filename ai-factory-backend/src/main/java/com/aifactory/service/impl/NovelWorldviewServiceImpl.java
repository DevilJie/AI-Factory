package com.aifactory.service.impl;

import com.aifactory.entity.NovelWorldview;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.service.NovelWorldviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 世界观设定Service实现
 *
 * @Author CaiZy
 * @Date 2025-01-27
 */
@Slf4j
@Service
public class NovelWorldviewServiceImpl implements NovelWorldviewService {

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Override
    public NovelWorldview getByProjectId(Long projectId) {
        return worldviewMapper.selectOne(
            new LambdaQueryWrapper<NovelWorldview>()
                .eq(NovelWorldview::getProjectId, projectId)
                .last("LIMIT 1")
        );
    }

    @Override
    public NovelWorldview getByOutlineId(Long outlineId) {
        return worldviewMapper.selectOne(
            new LambdaQueryWrapper<NovelWorldview>()
                .eq(NovelWorldview::getOutlineId, outlineId)
                .last("LIMIT 1")
        );
    }

    @Override
    public NovelWorldview saveOrUpdate(NovelWorldview worldview) {
        if (worldview.getId() != null) {
            // 更新
            worldview.setUpdateTime(LocalDateTime.now());
            worldviewMapper.updateById(worldview);
            log.info("更新世界观设定，ID: {}", worldview.getId());
        } else {
            // 新增
            worldview.setCreateTime(LocalDateTime.now());
            worldview.setUpdateTime(LocalDateTime.now());

            // 检查是否已存在该项目/大纲的世界观
            NovelWorldview existing = null;
            if (worldview.getProjectId() != null) {
                existing = getByProjectId(worldview.getProjectId());
            } else if (worldview.getOutlineId() != null) {
                existing = getByOutlineId(worldview.getOutlineId());
            }

            if (existing != null) {
                // 已存在，更新
                worldview.setId(existing.getId());
                worldview.setUpdateTime(LocalDateTime.now());
                worldviewMapper.updateById(worldview);
                log.info("世界观设定已存在，执行更新，ID: {}", existing.getId());
            } else {
                // 不存在，新增
                worldviewMapper.insert(worldview);
                log.info("新增世界观设定，ID: {}", worldview.getId());
            }
        }
        return worldview;
    }

    @Override
    public void deleteById(Long id) {
        worldviewMapper.deleteById(id);
    }
}
