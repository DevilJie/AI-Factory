package com.aifactory.mapper;

import com.aifactory.entity.NovelCharacterPowerSystem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Character-PowerSystem association mapper
 *
 * MyBatis-Plus BaseMapper provides all CRUD operations for the join table.
 * No custom methods needed.
 *
 * @Author AI-Factory
 * @Date 2026-04-06
 */
@Mapper
public interface NovelCharacterPowerSystemMapper extends BaseMapper<NovelCharacterPowerSystem> {
}
