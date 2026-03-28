package com.aifactory.service;

import com.aifactory.dto.ChapterMemoryXmlDto;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.NovelCharacterChapter;
import com.aifactory.mapper.NovelCharacterChapterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NovelCharacterChapterService 单元测试
 *
 * 测试覆盖：
 * 1. saveCharacterChapterRelation() - 保存角色-章节关联
 * 2. getChaptersByCharacterId() - 获取角色出场章节
 * 3. getCharactersByChapterId() - 获取章节相关角色
 * 4. isFirstAppearance() - 检查是否首次出现
 *
 * @Author CaiZy
 * @Date 2025-03-04
 */
@ExtendWith(MockitoExtension.class)
class NovelCharacterChapterServiceTest {

    @Mock
    private NovelCharacterChapterMapper characterChapterMapper;

    @InjectMocks
    private NovelCharacterChapterService characterChapterService;

    private static final Long CHARACTER_ID = 1L;
    private static final Long CHAPTER_ID = 100L;
    private static final Long PROJECT_ID = 10L;
    private static final Integer CHAPTER_NUMBER = 5;

    private NovelCharacterChapter createTestRelation() {
        NovelCharacterChapter relation = new NovelCharacterChapter();
        relation.setId(1L);
        relation.setCharacterId(CHARACTER_ID);
        relation.setChapterId(CHAPTER_ID);
        relation.setProjectId(PROJECT_ID);
        relation.setChapterNumber(CHAPTER_NUMBER);
        relation.setStatusInChapter("活跃");
        relation.setIsFirstAppearance(true);
        relation.setImportanceLevel("protagonist");
        relation.setCreateTime(LocalDateTime.now());
        relation.setUpdateTime(LocalDateTime.now());
        return relation;
    }

    private Chapter createTestChapter() {
        Chapter chapter = new Chapter();
        chapter.setId(CHAPTER_ID);
        chapter.setProjectId(PROJECT_ID);
        chapter.setChapterNumber(CHAPTER_NUMBER);
        chapter.setTitle("测试章节");
        return chapter;
    }

    private ChapterMemoryXmlDto.CharacterDetailDto createTestCharacterDetail() {
        ChapterMemoryXmlDto.CharacterDetailDto detail = new ChapterMemoryXmlDto.CharacterDetailDto();
        detail.setName("张三");
        detail.setRoleType("protagonist");
        detail.setGender("male");
        detail.setAge("25");
        detail.setStatus("状态良好");
        detail.setPersonality("勇敢,善良");
        detail.setAppearance("高大英俊");
        return detail;
    }

    @Nested
    @DisplayName("saveCharacterChapterRelation() 测试")
    class SaveCharacterChapterRelationTests {

        @Test
        @DisplayName("创建新的角色-章节关联记录")
        void shouldCreateNewRelation_whenNotExists() {
            // Given: 不存在关联记录
            when(characterChapterMapper.selectByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID))
                    .thenReturn(null);

            // When: 保存关联
            characterChapterService.saveCharacterChapterRelation(
                    CHARACTER_ID, CHAPTER_ID, PROJECT_ID,
                    CHAPTER_NUMBER, "活跃", true, "protagonist"
            );

            // Then: 验证插入操作被调用
            ArgumentCaptor<NovelCharacterChapter> captor = ArgumentCaptor.forClass(NovelCharacterChapter.class);
            verify(characterChapterMapper).insert(captor.capture());

            NovelCharacterChapter savedRelation = captor.getValue();
            assertEquals(CHARACTER_ID, savedRelation.getCharacterId());
            assertEquals(CHAPTER_ID, savedRelation.getChapterId());
            assertEquals(PROJECT_ID, savedRelation.getProjectId());
            assertEquals(CHAPTER_NUMBER, savedRelation.getChapterNumber());
            assertEquals("活跃", savedRelation.getStatusInChapter());
            assertTrue(savedRelation.getIsFirstAppearance());
            assertEquals("protagonist", savedRelation.getImportanceLevel());
            assertNotNull(savedRelation.getCreateTime());
            assertNotNull(savedRelation.getUpdateTime());
        }

        @Test
        @DisplayName("更新已存在的角色-章节关联记录")
        void shouldUpdateRelation_whenExists() {
            // Given: 已存在关联记录
            NovelCharacterChapter existingRelation = createTestRelation();
            existingRelation.setStatusInChapter("旧状态");
            existingRelation.setImportanceLevel("supporting");

            when(characterChapterMapper.selectByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID))
                    .thenReturn(existingRelation);

            // When: 保存关联（更新）
            characterChapterService.saveCharacterChapterRelation(
                    CHARACTER_ID, CHAPTER_ID, PROJECT_ID,
                    CHAPTER_NUMBER, "新状态", false, "protagonist"
            );

            // Then: 验证更新操作被调用
            ArgumentCaptor<NovelCharacterChapter> captor = ArgumentCaptor.forClass(NovelCharacterChapter.class);
            verify(characterChapterMapper).updateById(captor.capture());

            NovelCharacterChapter updatedRelation = captor.getValue();
            assertEquals("新状态", updatedRelation.getStatusInChapter());
            assertEquals("protagonist", updatedRelation.getImportanceLevel());
            assertNotNull(updatedRelation.getUpdateTime());

            // 验证insert未被调用
            verify(characterChapterMapper, never()).insert(any());
        }

        @Test
        @DisplayName("使用Chapter和CharacterDetailDto保存关联")
        void shouldSaveRelation_withChapterAndDetail() {
            // Given
            Chapter chapter = createTestChapter();
            ChapterMemoryXmlDto.CharacterDetailDto detail = createTestCharacterDetail();

            when(characterChapterMapper.selectByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID))
                    .thenReturn(null);

            // When
            characterChapterService.saveCharacterChapterRelation(
                    CHARACTER_ID, chapter, detail, true
            );

            // Then
            ArgumentCaptor<NovelCharacterChapter> captor = ArgumentCaptor.forClass(NovelCharacterChapter.class);
            verify(characterChapterMapper).insert(captor.capture());

            NovelCharacterChapter savedRelation = captor.getValue();
            assertEquals(CHARACTER_ID, savedRelation.getCharacterId());
            assertEquals(CHAPTER_ID, savedRelation.getChapterId());
            assertEquals(PROJECT_ID, savedRelation.getProjectId());
            assertEquals(CHAPTER_NUMBER, savedRelation.getChapterNumber());
            assertEquals("状态良好", savedRelation.getStatusInChapter());
            assertEquals("protagonist", savedRelation.getImportanceLevel());
        }
    }

    @Nested
    @DisplayName("getChaptersByCharacterId() 测试")
    class GetChaptersByCharacterIdTests {

        @Test
        @DisplayName("获取角色出现的所有章节")
        void shouldReturnChapters_whenCharacterHasRelations() {
            // Given
            NovelCharacterChapter relation1 = createTestRelation();
            NovelCharacterChapter relation2 = createTestRelation();
            relation2.setId(2L);
            relation2.setChapterId(101L);
            relation2.setChapterNumber(6);

            List<NovelCharacterChapter> expectedRelations = Arrays.asList(relation1, relation2);
            when(characterChapterMapper.selectByCharacterId(CHARACTER_ID))
                    .thenReturn(expectedRelations);

            // When
            List<NovelCharacterChapter> result = characterChapterService.getChaptersByCharacterId(CHARACTER_ID);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(CHARACTER_ID, result.get(0).getCharacterId());
            assertEquals(100L, result.get(0).getChapterId());
            assertEquals(101L, result.get(1).getChapterId());

            verify(characterChapterMapper).selectByCharacterId(CHARACTER_ID);
        }

        @Test
        @DisplayName("角色无章节关联时返回空列表")
        void shouldReturnEmptyList_whenNoRelations() {
            // Given
            when(characterChapterMapper.selectByCharacterId(CHARACTER_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<NovelCharacterChapter> result = characterChapterService.getChaptersByCharacterId(CHARACTER_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCharactersByChapterId() 测试")
    class GetCharactersByChapterIdTests {

        @Test
        @DisplayName("获取章节中的所有角色")
        void shouldReturnCharacters_whenChapterHasRelations() {
            // Given
            NovelCharacterChapter relation1 = createTestRelation();
            NovelCharacterChapter relation2 = createTestRelation();
            relation2.setId(2L);
            relation2.setCharacterId(2L);

            List<NovelCharacterChapter> expectedRelations = Arrays.asList(relation1, relation2);
            when(characterChapterMapper.selectByChapterId(CHAPTER_ID))
                    .thenReturn(expectedRelations);

            // When
            List<NovelCharacterChapter> result = characterChapterService.getCharactersByChapterId(CHAPTER_ID);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(CHAPTER_ID, result.get(0).getChapterId());
            assertEquals(CHAPTER_ID, result.get(1).getChapterId());
            assertEquals(1L, result.get(0).getCharacterId());
            assertEquals(2L, result.get(1).getCharacterId());

            verify(characterChapterMapper).selectByChapterId(CHAPTER_ID);
        }

        @Test
        @DisplayName("章节无角色关联时返回空列表")
        void shouldReturnEmptyList_whenNoCharacters() {
            // Given
            when(characterChapterMapper.selectByChapterId(CHAPTER_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<NovelCharacterChapter> result = characterChapterService.getCharactersByChapterId(CHAPTER_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("isFirstAppearance() 测试")
    class IsFirstAppearanceTests {

        @Test
        @DisplayName("角色首次出现时返回true")
        void shouldReturnTrue_whenFirstAppearance() {
            // Given
            when(characterChapterMapper.countByCharacterAndProject(CHARACTER_ID, PROJECT_ID))
                    .thenReturn(0);

            // When
            boolean result = characterChapterService.isFirstAppearance(CHARACTER_ID, PROJECT_ID);

            // Then
            assertTrue(result);
            verify(characterChapterMapper).countByCharacterAndProject(CHARACTER_ID, PROJECT_ID);
        }

        @Test
        @DisplayName("角色非首次出现时返回false")
        void shouldReturnFalse_whenNotFirstAppearance() {
            // Given
            when(characterChapterMapper.countByCharacterAndProject(CHARACTER_ID, PROJECT_ID))
                    .thenReturn(3);

            // When
            boolean result = characterChapterService.isFirstAppearance(CHARACTER_ID, PROJECT_ID);

            // Then
            assertFalse(result);
            verify(characterChapterMapper).countByCharacterAndProject(CHARACTER_ID, PROJECT_ID);
        }

        @Test
        @DisplayName("角色仅出现一次时返回false（已出现过）")
        void shouldReturnFalse_whenAlreadyAppearedOnce() {
            // Given
            when(characterChapterMapper.countByCharacterAndProject(CHARACTER_ID, PROJECT_ID))
                    .thenReturn(1);

            // When
            boolean result = characterChapterService.isFirstAppearance(CHARACTER_ID, PROJECT_ID);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getByCharacterAndChapter() 测试")
    class GetByCharacterAndChapterTests {

        @Test
        @DisplayName("根据角色ID和章节ID获取关联记录")
        void shouldReturnRelation_whenExists() {
            // Given
            NovelCharacterChapter expectedRelation = createTestRelation();
            when(characterChapterMapper.selectByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID))
                    .thenReturn(expectedRelation);

            // When
            NovelCharacterChapter result = characterChapterService.getByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID);

            // Then
            assertNotNull(result);
            assertEquals(CHARACTER_ID, result.getCharacterId());
            assertEquals(CHAPTER_ID, result.getChapterId());
        }

        @Test
        @DisplayName("关联记录不存在时返回null")
        void shouldReturnNull_whenNotExists() {
            // Given
            when(characterChapterMapper.selectByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID))
                    .thenReturn(null);

            // When
            NovelCharacterChapter result = characterChapterService.getByCharacterAndChapter(CHARACTER_ID, CHAPTER_ID);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("deleteByCharacterId() 测试")
    class DeleteByCharacterIdTests {

        @Test
        @DisplayName("删除角色的所有章节关联")
        void shouldDeleteAllRelations_forCharacter() {
            // When
            characterChapterService.deleteByCharacterId(CHARACTER_ID);

            // Then
            verify(characterChapterMapper).delete(any());
        }
    }

    @Nested
    @DisplayName("deleteByChapterId() 测试")
    class DeleteByChapterIdTests {

        @Test
        @DisplayName("删除章节的所有角色关联")
        void shouldDeleteAllRelations_forChapter() {
            // When
            characterChapterService.deleteByChapterId(CHAPTER_ID);

            // Then
            verify(characterChapterMapper).delete(any());
        }
    }
}
