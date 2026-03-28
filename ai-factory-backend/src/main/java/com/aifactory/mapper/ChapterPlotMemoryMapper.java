package com.aifactory.mapper;

import com.aifactory.entity.ChapterPlotMemory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 章节剧情记忆Mapper
 *
 * @Author AI Assistant
 * @Date 2026-01-28
 */
@Mapper
public interface ChapterPlotMemoryMapper extends BaseMapper<ChapterPlotMemory> {

    /**
     * 查询前N章的剧情记忆
     */
    @Select("SELECT * FROM chapter_plot_memory WHERE project_id = #{projectId} " +
            "AND chapter_number < #{currentChapterNumber} " +
            "ORDER BY chapter_number DESC LIMIT #{limit}")
    List<ChapterPlotMemory> selectRecentMemories(@Param("projectId") Long projectId,
                                                  @Param("currentChapterNumber") Integer currentChapterNumber,
                                                  @Param("limit") Integer limit);

    /**
     * 查询所有待回收的伏笔（按项目）
     */
    @Select("SELECT * FROM chapter_plot_memory WHERE project_id = #{projectId} " +
            "AND pending_foreshadowing IS NOT NULL AND pending_foreshadowing != '' " +
            "ORDER BY chapter_number DESC")
    List<ChapterPlotMemory> selectPendingForeshadowing(@Param("projectId") Long projectId);

    /**
     * 根据章节ID查询
     */
    @Select("SELECT * FROM chapter_plot_memory WHERE chapter_id = #{chapterId} LIMIT 1")
    ChapterPlotMemory selectByChapterId(@Param("chapterId") Long chapterId);
}
