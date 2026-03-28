package com.aifactory.mapper;

import com.aifactory.entity.NovelCharacterChapter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-章节关联Mapper
 *
 * @Author CaiZy
 * @Date 2025-03-04
 */
@Mapper
public interface NovelCharacterChapterMapper extends BaseMapper<NovelCharacterChapter> {

    /**
     * 根据角色ID查询所有章节关联
     *
     * @param characterId 角色ID
     * @return 章节-角色关联列表
     */
    @Select("SELECT * FROM novel_character_chapter WHERE character_id = #{characterId} ORDER BY chapter_number ASC")
    List<NovelCharacterChapter> selectByCharacterId(@Param("characterId") Long characterId);

    /**
     * 根据章节ID查询所有角色关联
     *
     * @param chapterId 章节ID
     * @return 章节-角色关联列表
     */
    @Select("SELECT * FROM novel_character_chapter WHERE chapter_id = #{chapterId}")
    List<NovelCharacterChapter> selectByChapterId(@Param("chapterId") Long chapterId);

    /**
     * 查询角色在指定章节的关联记录
     *
     * @param characterId 角色ID
     * @param chapterId   章节ID
     * @return 章节-角色关联
     */
    @Select("SELECT * FROM novel_character_chapter WHERE character_id = #{characterId} AND chapter_id = #{chapterId}")
    NovelCharacterChapter selectByCharacterAndChapter(@Param("characterId") Long characterId,
                                                        @Param("chapterId") Long chapterId);

    /**
     * 检查角色是否已在该项目出现过
     *
     * @param characterId 角色ID
     * @param projectId   项目ID
     * @return 出现次数
     */
    @Select("SELECT COUNT(*) FROM novel_character_chapter WHERE character_id = #{characterId} AND project_id = #{projectId}")
    int countByCharacterAndProject(@Param("characterId") Long characterId, @Param("projectId") Long projectId);
}
