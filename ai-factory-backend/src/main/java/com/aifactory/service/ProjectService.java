package com.aifactory.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aifactory.dto.DashboardStatsDto;
import com.aifactory.dto.ProjectCreateDto;
import com.aifactory.dto.ProjectQueryDto;
import com.aifactory.dto.ProjectUpdateDto;
import com.aifactory.dto.ProjectOverviewVO;
import com.aifactory.dto.ProjectVo;
import com.aifactory.entity.*;
import com.aifactory.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@Service
public class ProjectService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private ChapterPlotMemoryMapper chapterPlotMemoryMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelStoryboardMapper novelStoryboardMapper;

    @Autowired
    private NovelCharacterMapper novelCharacterMapper;

    @Autowired
    private ForeshadowingMapper foreshadowingMapper;

    @Autowired
    private NovelWorldviewMapper novelWorldviewMapper;

    @Autowired
    private NovelOutlineMapper novelOutlineMapper;

    @Autowired
    private NovelVolumePlanMapper novelVolumePlanMapper;

    /**
     * 创建项目
     */
    @Transactional(rollbackFor = Exception.class)
    public String createProject(ProjectCreateDto createDto, Long userId) {
        Project project = new Project();
        BeanUtils.copyProperties(createDto, project);
        project.setUserId(userId);
        project.setStatus("draft");
        project.setChapterCount(0);
        project.setTotalWordCount(0);
        project.setTotalDuration(0);
        String now = DATE_FORMATTER.format(LocalDateTime.now());
        project.setCreateTime(now);
        project.setUpdateTime(now);

        // 先插入项目，获取自增ID
        projectMapper.insert(project);

        return project.getId().toString();
    }

    /**
     * 更新项目
     */
    public void updateProject(ProjectUpdateDto updateDto, Long userId) {
        Project project = projectMapper.selectById(Long.parseLong(updateDto.getId()));

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        if (!project.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此项目");
        }

        // 只更新非空字段
        if (StrUtil.isNotBlank(updateDto.getName())) {
            project.setName(updateDto.getName());
        }
        if (StrUtil.isNotBlank(updateDto.getDescription())) {
            project.setDescription(updateDto.getDescription());
        }
        if (StrUtil.isNotBlank(updateDto.getStoryTone())) {
            project.setStoryTone(updateDto.getStoryTone());
        }
        if (StrUtil.isNotBlank(updateDto.getStoryGenre())) {
            project.setStoryGenre(updateDto.getStoryGenre());
        }
        if (StrUtil.isNotBlank(updateDto.getVisualStyle())) {
            project.setVisualStyle(updateDto.getVisualStyle());
        }
        if (StrUtil.isNotBlank(updateDto.getCoverUrl())) {
            project.setCoverUrl(updateDto.getCoverUrl());
        }
        if (StrUtil.isNotBlank(updateDto.getStatus())) {
            project.setStatus(updateDto.getStatus());
        }
        if (StrUtil.isNotBlank(updateDto.getTags())) {
            project.setTags(updateDto.getTags());
        }

        project.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));
        projectMapper.updateById(project);
    }

    /**
     * 删除项目及其所有关联数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(String projectId, Long userId) {
        Project project = projectMapper.selectById(Long.parseLong(projectId));

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        if (!project.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此项目");
        }

        Long projectIdLong = Long.parseLong(projectId);

        // 1. 删除章节剧情记忆
        LambdaQueryWrapper<ChapterPlotMemory> memoryWrapper = new LambdaQueryWrapper<>();
        memoryWrapper.eq(ChapterPlotMemory::getProjectId, projectIdLong);
        chapterPlotMemoryMapper.delete(memoryWrapper);

        // 2. 删除章节
        LambdaQueryWrapper<Chapter> chapterWrapper = new LambdaQueryWrapper<>();
        chapterWrapper.eq(Chapter::getProjectId, projectIdLong);
        chapterMapper.delete(chapterWrapper);

        // 3. 删除章节规划
        LambdaQueryWrapper<NovelChapterPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(NovelChapterPlan::getProjectId, projectIdLong);
        chapterPlanMapper.delete(planWrapper);

        // 4. 删除分镜
        LambdaQueryWrapper<NovelStoryboard> storyboardWrapper = new LambdaQueryWrapper<>();
        storyboardWrapper.eq(NovelStoryboard::getProjectId, projectIdLong);
        novelStoryboardMapper.delete(storyboardWrapper);

        // 5. 删除角色
        LambdaQueryWrapper<NovelCharacter> characterWrapper = new LambdaQueryWrapper<>();
        characterWrapper.eq(NovelCharacter::getProjectId, projectIdLong);
        novelCharacterMapper.delete(characterWrapper);

        // 6. 删除伏笔
        LambdaQueryWrapper<Foreshadowing> foreshadowingWrapper = new LambdaQueryWrapper<>();
        foreshadowingWrapper.eq(Foreshadowing::getProjectId, projectIdLong);
        foreshadowingMapper.delete(foreshadowingWrapper);

        // 7. 删除世界观
        LambdaQueryWrapper<NovelWorldview> worldviewWrapper = new LambdaQueryWrapper<>();
        worldviewWrapper.eq(NovelWorldview::getProjectId, projectIdLong);
        novelWorldviewMapper.delete(worldviewWrapper);

        // 8. 删除大纲
        LambdaQueryWrapper<NovelOutline> outlineWrapper = new LambdaQueryWrapper<>();
        outlineWrapper.eq(NovelOutline::getProjectId, projectIdLong);
        novelOutlineMapper.delete(outlineWrapper);

        // 9. 删除分卷规划
        LambdaQueryWrapper<NovelVolumePlan> volumeWrapper = new LambdaQueryWrapper<>();
        volumeWrapper.eq(NovelVolumePlan::getProjectId, projectIdLong);
        novelVolumePlanMapper.delete(volumeWrapper);

        // 10. 最后删除项目本身
        projectMapper.deleteById(projectIdLong);

        log.info("Successfully deleted project: {} (ID: {}) and all related data", project.getName(), projectId);
    }

    /**
     * 获取项目详情
     */
    public ProjectVo getProjectDetail(String projectId, Long userId) {
        Project project = projectMapper.selectById(Long.parseLong(projectId));

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        if (!project.getUserId().equals(userId)) {
            throw new RuntimeException("无权限查看此项目");
        }

        return convertToVo(project);
    }

    /**
     * 查询项目列表
     */
    public IPage<ProjectVo> getProjectList(ProjectQueryDto queryDto, Long userId) {
        // 构建查询条件
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getUserId, userId);

        if (StrUtil.isNotBlank(queryDto.getProjectType())) {
            queryWrapper.eq(Project::getProjectType, queryDto.getProjectType());
        }

        if (StrUtil.isNotBlank(queryDto.getStatus())) {
            queryWrapper.eq(Project::getStatus, queryDto.getStatus());
        }

        if (StrUtil.isNotBlank(queryDto.getKeyword())) {
            queryWrapper.like(Project::getName, queryDto.getKeyword());
        }

        // 处理排序
        String sortBy = StrUtil.isNotBlank(queryDto.getSortBy()) ? queryDto.getSortBy() : "updateTime";
        String sortOrder = StrUtil.isNotBlank(queryDto.getSortOrder()) ? queryDto.getSortOrder() : "desc";

        // 根据排序字段和方向排序
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortBy) {
            case "createTime":
                if (isAsc) {
                    queryWrapper.orderByAsc(Project::getCreateTime);
                } else {
                    queryWrapper.orderByDesc(Project::getCreateTime);
                }
                break;
            case "updateTime":
            default:
                if (isAsc) {
                    queryWrapper.orderByAsc(Project::getUpdateTime);
                } else {
                    queryWrapper.orderByDesc(Project::getUpdateTime);
                }
                break;
        }

        // 分页查询
        Page<Project> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
        IPage<Project> projectPage = projectMapper.selectPage(page, queryWrapper);

        // 转换为VO
        return projectPage.convert(this::convertToVo);
    }

    /**
     * 获取Dashboard统计数据
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    public DashboardStatsDto getDashboardStats(Long userId) {
        DashboardStatsDto stats = new DashboardStatsDto();

        // 统计项目数
        LambdaQueryWrapper<Project> projectQuery = new LambdaQueryWrapper<>();
        projectQuery.eq(Project::getUserId, userId);
        Long projectCount = projectMapper.selectCount(projectQuery);
        stats.setProjectCount(projectCount);

        // 统计章节数（通过用户的所有项目ID来统计）
        LambdaQueryWrapper<Project> projectIdsQuery = new LambdaQueryWrapper<>();
        projectIdsQuery.eq(Project::getUserId, userId)
                      .select(Project::getId);
        List<Long> projectIds = projectMapper.selectList(projectIdsQuery)
                .stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        Long chapterCount = 0L;
        Long characterCount = 0L;

        Long totalWordCount = 0L;

        if (!projectIds.isEmpty()) {
            // 统计章节数
            LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
            chapterQuery.in(Chapter::getProjectId, projectIds);
            chapterCount = chapterMapper.selectCount(chapterQuery);

            // 统计角色数
            LambdaQueryWrapper<NovelCharacter> characterQuery = new LambdaQueryWrapper<>();
            characterQuery.in(NovelCharacter::getProjectId, projectIds);
            characterCount = novelCharacterMapper.selectCount(characterQuery);

            // 统计总字数
            LambdaQueryWrapper<Chapter> wordCountQuery = new LambdaQueryWrapper<>();
            wordCountQuery.in(Chapter::getProjectId, projectIds)
                          .select(Chapter::getWordCount);
            totalWordCount = chapterMapper.selectList(wordCountQuery)
                    .stream()
                    .mapToLong(ch -> ch.getWordCount() != null ? ch.getWordCount().longValue() : 0L)
                    .sum();
        }

        stats.setChapterCount(chapterCount);
        stats.setCharacterCount(characterCount);
        stats.setTotalWordCount(totalWordCount);

        return stats;
    }

    /**
     * 转换为VO对象
     */
    private ProjectVo convertToVo(Project project) {
        ProjectVo vo = new ProjectVo();
        BeanUtils.copyProperties(project, vo);
        vo.setId(project.getId().toString());

        // 解析标签JSON
        if (StrUtil.isNotBlank(project.getTags())) {
            try {
                List<String> tagList = JSONUtil.toList(project.getTags(), String.class);
                vo.setTags(tagList);
            } catch (Exception e) {
                vo.setTags(List.of());
            }
        } else {
            vo.setTags(List.of());
        }

        // 实时计算统计数据
        Long projectId = project.getId();

        if ("novel".equals(project.getProjectType())) {
            // 小说项目：统计实际的章节数和总字数
            // 统计章节规划数
            LambdaQueryWrapper<NovelChapterPlan> planQuery = new LambdaQueryWrapper<>();
            planQuery.eq(NovelChapterPlan::getProjectId, projectId);
            Long planCount = chapterPlanMapper.selectCount(planQuery);
            vo.setChapterCount(planCount.intValue());

            // 统计已生成章节的总字数
            LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
            chapterQuery.eq(Chapter::getProjectId, projectId);
            List<Chapter> chapters = chapterMapper.selectList(chapterQuery);

            int totalWordCount = chapters.stream()
                .mapToInt(chapter -> chapter.getWordCount() != null ? chapter.getWordCount() : 0)
                .sum();

            vo.setTotalWordCount(totalWordCount);
        } else {
            // 视频项目：统计章节数
            LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
            chapterQuery.eq(Chapter::getProjectId, projectId);
            Long chapterCount = chapterMapper.selectCount(chapterQuery);
            vo.setChapterCount(chapterCount.intValue());
        }

        return vo;
    }

    /**
     * 根据ID获取项目
     */
    public Project getById(Long projectId) {
        return projectMapper.selectById(projectId);
    }

    /**
     * 更新项目
     */
    public void updateById(Project project) {
        project.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));
        projectMapper.updateById(project);
    }

    /**
     * 获取项目概览数据
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 项目概览数据
     */
    public ProjectOverviewVO getProjectOverview(String projectId, Long userId) {
        Project project = projectMapper.selectById(Long.parseLong(projectId));

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        if (!project.getUserId().equals(userId)) {
            throw new RuntimeException("无权限查看此项目");
        }

        ProjectOverviewVO vo = new ProjectOverviewVO();
        vo.setId(project.getId().toString());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setCoverUrl(project.getCoverUrl());
        vo.setStatus(project.getStatus());

        // 解析标签JSON
        if (StrUtil.isNotBlank(project.getTags())) {
            try {
                List<String> tagList = JSONUtil.toList(project.getTags(), String.class);
                vo.setTags(tagList);
            } catch (Exception e) {
                vo.setTags(List.of());
            }
        } else {
            vo.setTags(List.of());
        }

        Long projectIdLong = project.getId();

        // 统计已完成章节数（来自 novel_chapter 表）
        LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
        chapterQuery.eq(Chapter::getProjectId, projectIdLong);
        Long chapterCount = chapterMapper.selectCount(chapterQuery);
        vo.setChapterCount(chapterCount.intValue());

        // 统计角色数
        LambdaQueryWrapper<NovelCharacter> characterQuery = new LambdaQueryWrapper<>();
        characterQuery.eq(NovelCharacter::getProjectId, projectIdLong);
        Long characterCount = novelCharacterMapper.selectCount(characterQuery);
        vo.setCharacterCount(characterCount.intValue());

        // 统计总字数
        List<Chapter> chapters = chapterMapper.selectList(chapterQuery);
        int totalWordCount = chapters.stream()
            .mapToInt(chapter -> chapter.getWordCount() != null ? chapter.getWordCount() : 0)
            .sum();
        vo.setTotalWordCount(totalWordCount);

        // 计算目标章节数（分卷规划的目标章节数之和）
        LambdaQueryWrapper<NovelVolumePlan> volumeQuery = new LambdaQueryWrapper<>();
        volumeQuery.eq(NovelVolumePlan::getProjectId, projectIdLong);
        List<NovelVolumePlan> volumes = novelVolumePlanMapper.selectList(volumeQuery);
        int targetChapterCount = volumes.stream()
            .mapToInt(v -> v.getTargetChapterCount() != null ? v.getTargetChapterCount() : 0)
            .sum();
        vo.setTargetChapterCount(targetChapterCount);

        // 计算进度
        int progress = 0;
        if (targetChapterCount > 0) {
            progress = Math.min(100, (chapterCount.intValue() * 100) / targetChapterCount);
        }
        vo.setProgress(progress);

        return vo;
    }
}
