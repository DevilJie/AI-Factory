package com.aifactory.service;

import com.aifactory.common.UserContext;
import com.aifactory.common.XmlParser;
import com.aifactory.enums.AIRole;
import com.aifactory.dto.ChapterContext;
import com.aifactory.dto.ChapterDto;
import com.aifactory.dto.ChapterMemoryXmlDto;
import com.aifactory.dto.ChapterPlanDto;
import com.aifactory.dto.ChapterPlanUpdateRequest;
import com.aifactory.dto.ChapterUpdateRequest;
import com.aifactory.dto.VolumePlanDto;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.ChapterAiFixResponse;
import com.aifactory.dto.ChapterAiFixRequest;
import com.aifactory.dto.ChapterAiPolishResponse;
import com.aifactory.dto.ChapterAiPolishRequest;
import com.aifactory.dto.ChapterIssueCheckResponse;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.Chapter;
import com.aifactory.entity.ChapterPlotMemory;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelOutline;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.Project;
import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.mapper.ChapterMapper;
import com.aifactory.mapper.ChapterPlotMemoryMapper;
import com.aifactory.mapper.NovelCharacterMapper;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelOutlineMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.chapter.persistence.ChapterPersistenceService;
import com.aifactory.service.llm.LLMProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aifactory.service.chapter.prompt.PromptTemplateBuilder;
import com.aifactory.service.prompt.PromptTemplateService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 章节服务
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Slf4j
@Service
public class ChapterService {

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    @Autowired
    private NovelOutlineMapper outlineMapper;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private AIGenerateService aiGenerateService;

    @Autowired
    private ChapterPlotMemoryMapper plotMemoryMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ChapterPersistenceService chapterPersistenceService;

    @Autowired
    private AiInteractionLogService aiInteractionLogService;

    @Autowired
    private PromptTemplateBuilder promptTemplateBuilder;

    @Autowired
    private NovelCharacterMapper novelCharacterMapper;

    @Autowired
    private ForeshadowingService foreshadowingService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ProjectBasicSettingsService projectBasicSettingsService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private FactionService factionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 剧情记忆配置：保留前N章的详细记忆
    private static final int PLOT_MEMORY_DEPTH = 5;

    /**
     * 获取章节列表
     */
    public List<ChapterDto> getChapterList(Long projectId) {
        LambdaQueryWrapper<Chapter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Chapter::getProjectId, projectId)
                .orderByDesc(Chapter::getChapterNumber);

        var chapters = chapterMapper.selectList(queryWrapper);
        return chapters.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * 获取分卷列表（从大纲规划中获取）
     */
    public List<VolumePlanDto> getVolumeList(Long projectId) {
        log.info("查询分卷列表，projectId: {}", projectId);

        // 查询所有分卷规划
        LambdaQueryWrapper<NovelVolumePlan> volumeQuery =
                new LambdaQueryWrapper<>();
        volumeQuery.eq(NovelVolumePlan::getProjectId, projectId)
                .orderByAsc(NovelVolumePlan::getVolumeNumber);

        var volumes = volumePlanMapper.selectList(volumeQuery);
        log.info("查询到的分卷数量: {}", volumes.size());
        if (!volumes.isEmpty()) {
            log.info("第一个分卷: id={}, projectId={}, volumeTitle={}",
                    volumes.get(0).getId(), volumes.get(0).getProjectId(), volumes.get(0).getVolumeTitle());
        }

        // 转换为DTO
        return volumes.stream()
                .map(this::convertToVolumeDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取大纲中的章节规划（带分卷信息）
     */
    public List<ChapterPlanDto> getChapterPlans(Long projectId) {
        // 1. 查询所有章节规划
        LambdaQueryWrapper<NovelChapterPlan> planQuery =
                new LambdaQueryWrapper<>();
        planQuery.eq(NovelChapterPlan::getProjectId, projectId)
                .orderByAsc(NovelChapterPlan::getChapterNumber);

        var plans = chapterPlanMapper.selectList(planQuery);

        // 2. 查询所有分卷规划
        LambdaQueryWrapper<NovelVolumePlan> volumeQuery =
                new LambdaQueryWrapper<>();
        volumeQuery.eq(NovelVolumePlan::getProjectId, projectId)
                .orderByAsc(NovelVolumePlan::getVolumeNumber);

        var volumes = volumePlanMapper.selectList(volumeQuery);

        // 3. 构建分卷ID到分卷的映射
        Map<Long, NovelVolumePlan> volumeMap = volumes.stream()
                .collect(Collectors.toMap(NovelVolumePlan::getId, v -> v));

        // 4. 查询所有实际章节，按 chapterPlanId 建立索引（避免 chapterNumber 重复导致错配）
        LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
        chapterQuery.eq(Chapter::getProjectId, projectId);
        var chapters = chapterMapper.selectList(chapterQuery);

        Map<Long, Chapter> chapterByPlanId = chapters.stream()
                .filter(ch -> ch.getChapterPlanId() != null)
                .collect(Collectors.toMap(Chapter::getChapterPlanId, ch -> ch, (a, b) -> a));

        // 5. 转换为DTO并填充分卷信息和实际章节信息
        return plans.stream()
                .map(plan -> {
                    ChapterPlanDto dto = new ChapterPlanDto();
                    dto.setId(plan.getId());
                    dto.setProjectId(plan.getProjectId());
                    dto.setVolumePlanId(plan.getVolumePlanId());
                    dto.setChapterNumber(plan.getChapterNumber());
                    dto.setChapterTitle(plan.getChapterTitle());
                    dto.setPlotOutline(plan.getPlotOutline());
                    dto.setKeyEvents(plan.getKeyEvents());
                    dto.setChapterGoal(plan.getChapterGoal());
                    dto.setWordCountTarget(plan.getWordCountTarget());
                    dto.setChapterStartingScene(plan.getChapterStartingScene());
                    dto.setChapterEndingScene(plan.getChapterEndingScene());
                    dto.setChapterNotes(plan.getChapterNotes());
                    dto.setStatus(plan.getStatus());
                    dto.setPlannedCharacters(plan.getPlannedCharacters());
                    dto.setCharacterArcs(plan.getCharacterArcs());
                    dto.setCreateTime(plan.getCreateTime());
                    dto.setUpdateTime(plan.getUpdateTime());

                    // 填充分卷信息
                    NovelVolumePlan volume = volumeMap.get(plan.getVolumePlanId());
                    if (volume != null) {
                        dto.setVolumeTitle(volume.getVolumeTitle());
                        dto.setVolumeNumber(volume.getVolumeNumber());
                    }

                    // 通过 chapterPlanId 精确匹配已生成的章节（而非 chapterNumber）
                    Chapter chapter = chapterByPlanId.get(plan.getId());
                    if (chapter != null && chapter.getContent() != null && !chapter.getContent().isEmpty()) {
                        dto.setHasContent(true);
                        dto.setChapterId(chapter.getId());
                        dto.setWordCount(chapter.getWordCount());
                    } else {
                        dto.setHasContent(false);
                    }

                    return dto;
                })
                .toList();
    }

    /**
     * 获取章节详情
     */
    public ChapterDto getChapterDetail(Long chapterId) {
        var chapter = chapterMapper.selectById(chapterId);
        return convertToDto(chapter);
    }

    /**
     * 根据章节规划ID获取章节详情
     * 先查询规划获取章节号，再根据章节号查询实际章节
     */
    public ChapterDto getChapterByPlanId(Long projectId, Long planId) {
        // 1. 查询章节规划
        var plan = chapterPlanMapper.selectById(planId);
        if (plan == null) {
            log.warn("章节规划不存在，planId: {}", planId);
            return null;
        }

        // 2. 根据项目ID和章节号查询实际章节
        LambdaQueryWrapper<Chapter> query = new LambdaQueryWrapper<>();
        query.eq(Chapter::getProjectId, projectId)
             .eq(Chapter::getChapterPlanId, planId);
        var chapter = chapterMapper.selectOne(query);

        if (chapter == null) {
            log.info("章节尚未生成，projectId: {}, chapterNumber: {}", projectId, plan.getChapterNumber());
            return null;
        }

        return convertToDto(chapter);
    }

    /**
     * 更新章节规划
     */
    @Transactional
    public void updateChapterPlan(Long planId, ChapterPlanUpdateRequest request) {
        NovelChapterPlan plan = chapterPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("章节规划不存在，planId: " + planId);
        }

        // 只更新非空字段
        if (request.getChapterTitle() != null) {
            plan.setChapterTitle(request.getChapterTitle());
        }
        if (request.getPlotOutline() != null) {
            plan.setPlotOutline(request.getPlotOutline());
        }
        if (request.getChapterStartingScene() != null) {
            plan.setChapterStartingScene(request.getChapterStartingScene());
        }
        if (request.getChapterEndingScene() != null) {
            plan.setChapterEndingScene(request.getChapterEndingScene());
        }
        if (request.getKeyEvents() != null) {
            plan.setKeyEvents(request.getKeyEvents());
        }
        if (request.getChapterGoal() != null) {
            plan.setChapterGoal(request.getChapterGoal());
        }
        if (request.getWordCountTarget() != null) {
            plan.setWordCountTarget(request.getWordCountTarget());
        }
        if (request.getChapterNotes() != null) {
            plan.setChapterNotes(request.getChapterNotes());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        if (request.getPlotStage() != null) {
            plan.setPlotStage(request.getPlotStage());
        }
        if (request.getPlannedCharacters() != null) {
            String pc = request.getPlannedCharacters().trim();
            plan.setPlannedCharacters(pc.isEmpty() ? null : pc);
        }
        if (request.getCharacterArcs() != null) {
            String ca = request.getCharacterArcs().trim();
            plan.setCharacterArcs(ca.isEmpty() ? null : ca);
        }

        plan.setUpdateTime(LocalDateTime.now());
        chapterPlanMapper.updateById(plan);
        log.info("更新章节规划成功，planId={}", planId);
    }

    /**
     * 创建章节
     */
    @Transactional
    public Long createChapter(Long projectId, Integer chapterNumber, String title) {
        // 获取当前项目最大章节号
        LambdaQueryWrapper<Chapter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Chapter::getProjectId, projectId)
                .orderByDesc(Chapter::getChapterNumber)
                .last("LIMIT 1");

        var lastChapter = chapterMapper.selectOne(queryWrapper);
        Integer nextChapterNumber = (lastChapter != null) ? lastChapter.getChapterNumber() + 1 : 1;

        // 确定要使用的章节号
        Integer targetChapterNumber = (chapterNumber != null) ? chapterNumber : nextChapterNumber;

        // 检查是否已存在相同章节号的章节，如果存在则先删除（用于重新生成）
        LambdaQueryWrapper<Chapter> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getChapterNumber, targetChapterNumber);
        var existingChapter = chapterMapper.selectOne(checkWrapper);

        if (existingChapter != null) {
            chapterMapper.deleteById(existingChapter.getId());
        }

        var chapter = new Chapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNumber(targetChapterNumber);
        chapter.setTitle(title);
        chapter.setContent("");
        chapter.setWordCount(0);
        chapter.setStatus("draft");
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());

        chapterMapper.insert(chapter);

        return chapter.getId();
    }

    /**
     * 更新章节
     */
    @Transactional
    public void updateChapter(Long chapterId, ChapterUpdateRequest request) {
        var chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }

        // 记录内容是否发生变化（用于判断是否需要更新知识库）
        boolean contentChanged = false;
        String oldContent = chapter.getContent();

        // 只更新非空字段
        if (request.getTitle() != null) {
            chapter.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            chapter.setContent(request.getContent());
            chapter.setWordCount(request.getContent().length());
            // 检查内容是否真的变化了
            contentChanged = !request.getContent().equals(oldContent);
        }
        if (request.getStatus() != null) {
            chapter.setStatus(request.getStatus());
        }

        chapter.setUpdateTime(LocalDateTime.now());
        chapterMapper.updateById(chapter);
        log.info("更新章节成功，chapterId={}", chapterId);
    }

    /**
     * 删除章节
     */
    @Transactional
    public void deleteChapter(Long chapterId) {
        var chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }

        // 删除数据库中的章节
        chapterMapper.deleteById(chapterId);
        log.info("删除章节成功，chapterId={}", chapterId);
    }

    /**
     * 重新构建章节记忆（手动触发）
     */
    @Transactional
    public void rebuildChapterMemory(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }

        if (chapter.getContent() == null || chapter.getContent().isEmpty()) {
            throw new RuntimeException("章节内容为空，无法构建记忆");
        }

        log.info("开始重新构建章节记忆，chapterId={}", chapterId);

        // 生成剧情概要（如果还没有）
        String plotSummary = chapter.getPlotSummary();
        NovelChapterPlan plan = null;
        if (chapter.getChapterPlanId() != null) {
            plan = chapterPlanMapper.selectById(chapter.getChapterPlanId());
        }
        if (plotSummary == null || plotSummary.isEmpty()) {
            plotSummary = generatePlotSummary(chapter.getProjectId(), plan, chapter, chapter.getContent(), chapter.getTitle(), chapter.getChapterNumber());
            chapter.setPlotSummary(plotSummary);
            chapterMapper.updateById(chapter);
            log.info("生成剧情概要成功，chapterId={}", chapterId);
        }

        // 查询章节规划（用于获取额外信息）
        if (plan == null && chapter.getChapterPlanId() != null) {
            plan = chapterPlanMapper.selectById(chapter.getChapterPlanId());
        }

        // 保存剧情记忆（如果已存在则更新）
        saveChapterPlotMemory(chapter.getProjectId(), chapter, plan, chapter.getContent(), plotSummary);

        log.info("重新构建章节记忆成功，chapterId={}", chapterId);
    }

    /**
     * 转换为DTO
     */
    private ChapterDto convertToDto(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        ChapterDto dto = new ChapterDto();
        dto.setId(chapter.getId());
        dto.setVolumePlanId(chapter.getVolumePlanId());
        dto.setChapterPlanId(chapter.getChapterPlanId());
        dto.setChapterNumber(chapter.getChapterNumber());
        dto.setTitle(chapter.getTitle());
        dto.setContent(chapter.getContent());
        dto.setWordCount(chapter.getWordCount());
        dto.setStatus(chapter.getStatus());

        if (chapter.getUpdateTime() != null) {
            dto.setUpdateTime(chapter.getUpdateTime().format(DATE_FORMATTER));
        }

        return dto;
    }

    /**
     * 转换分卷实体为DTO
     */
    private VolumePlanDto convertToVolumeDto(NovelVolumePlan volumePlan) {
        if (volumePlan == null) {
            return null;
        }

        VolumePlanDto dto = new VolumePlanDto();
        dto.setId(volumePlan.getId());
        dto.setProjectId(volumePlan.getProjectId());
        dto.setOutlineId(volumePlan.getOutlineId());
        dto.setVolumeNumber(volumePlan.getVolumeNumber());
        dto.setVolumeTitle(volumePlan.getVolumeTitle());
        dto.setVolumeTheme(volumePlan.getVolumeTheme());
        dto.setMainConflict(volumePlan.getMainConflict());
        dto.setPlotArc(volumePlan.getPlotArc());
        dto.setCoreGoal(volumePlan.getCoreGoal());
        dto.setKeyEvents(volumePlan.getKeyEvents());
        dto.setClimax(volumePlan.getClimax());
        dto.setEnding(volumePlan.getEnding());
        dto.setVolumeDescription(volumePlan.getVolumeDescription());
        dto.setVolumeNotes(volumePlan.getVolumeNotes());
        dto.setTimelineSetting(volumePlan.getTimelineSetting());
        dto.setTargetChapterCount(volumePlan.getTargetChapterCount());
        dto.setStatus(volumePlan.getStatus());
        dto.setSortOrder(volumePlan.getSortOrder());
        dto.setVolumeCompleted(volumePlan.getVolumeCompleted());

        // 加载该分卷下的章节规划
        try {
            LambdaQueryWrapper<NovelChapterPlan> chapterPlanQuery =
                    new LambdaQueryWrapper<>();
            chapterPlanQuery.eq(NovelChapterPlan::getVolumePlanId, volumePlan.getId())
                    .orderByAsc(NovelChapterPlan::getChapterNumber);

            List<NovelChapterPlan> chapterPlans = chapterPlanMapper.selectList(chapterPlanQuery);

            // 转换为DTO并设置
            List<ChapterPlanDto> chapterDtos = chapterPlans.stream()
                    .map(this::convertChapterPlanToDto)
                    .collect(Collectors.toList());
            dto.setChapters(chapterDtos);

            log.debug("分卷 {} 加载了 {} 个章节规划", volumePlan.getVolumeNumber(), chapterDtos.size());
        } catch (Exception e) {
            log.error("加载分卷 {} 的章节规划失败", volumePlan.getVolumeNumber(), e);
            dto.setChapters(new ArrayList<>());
        }

        return dto;
    }

    /**
     * 转换章节规划实体为DTO
     */
    private ChapterPlanDto convertChapterPlanToDto(NovelChapterPlan chapterPlan) {
        if (chapterPlan == null) {
            return null;
        }

        ChapterPlanDto dto = new ChapterPlanDto();
        dto.setId(chapterPlan.getId());
        dto.setProjectId(chapterPlan.getProjectId());
        dto.setVolumePlanId(chapterPlan.getVolumePlanId());
        dto.setChapterNumber(chapterPlan.getChapterNumber());
        dto.setChapterTitle(chapterPlan.getChapterTitle());
        dto.setPlotOutline(chapterPlan.getPlotOutline());
        dto.setKeyEvents(chapterPlan.getKeyEvents());
        dto.setChapterGoal(chapterPlan.getChapterGoal());
        dto.setWordCountTarget(chapterPlan.getWordCountTarget());
        dto.setChapterNotes(chapterPlan.getChapterNotes());
        dto.setStatus(chapterPlan.getStatus());
        dto.setChapterStartingScene(chapterPlan.getChapterStartingScene());
        dto.setChapterEndingScene(chapterPlan.getChapterEndingScene());
        dto.setPlannedCharacters(chapterPlan.getPlannedCharacters());
        dto.setCharacterArcs(chapterPlan.getCharacterArcs());
        dto.setUpdateTime(chapterPlan.getUpdateTime());
        dto.setCreateTime(chapterPlan.getCreateTime());

        // 查询是否已生成章节内容
        try {
            LambdaQueryWrapper<Chapter> chapterQuery = new LambdaQueryWrapper<>();
            chapterQuery.eq(Chapter::getChapterPlanId, chapterPlan.getId());
            Chapter chapter = chapterMapper.selectOne(chapterQuery);

            if (chapter != null) {
                dto.setHasContent(true);
                dto.setChapterId(chapter.getId());
                dto.setWordCount(chapter.getWordCount());
            } else {
                dto.setHasContent(false);
            }
        } catch (Exception e) {
            log.warn("查询章节内容失败，章节规划ID: {}", chapterPlan.getId(), e);
            dto.setHasContent(false);
        }

        return dto;
    }

    /**
     * @deprecated 已移除SSE流式生成，请使用异步任务接口 generate-async
     */
    // SSE流式生成方法已移除 - 请使用 ChapterContentGenerateTaskStrategy（异步任务模式）

    /**
     * 处理AI生成的内容格式
     * 确保段落格式符合中文作文规范
     *
     * @param text 原始文本
     * @return 处理后的文本
     */
    private String processFormatMarkers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        var processed = text;

        // 移除可能残留的 <<<BR>>> 标记（兼容旧数据）
        processed = processed.replace("<<<BR>>>", "\n\n");

        // 规范化换行符
        processed = processed.replace("\r\n", "\n");
        processed = processed.replace("\r", "\n");

        // 将多个连续换行符规范化为双换行（段落分隔）
        processed = processed.replaceAll("\n{3,}", "\n\n");

        // 处理段落：确保每个段落开头有两个全角空格缩进
        String[] paragraphs = processed.split("\n\n");
        var result = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // 移除段落开头已有的全角空格（避免重复）
            paragraph = paragraph.replaceAll("^[　\\s]+", "");

            // 添加两个全角空格缩进
            result.append("　　").append(paragraph).append("\n\n");
        }

        return result.toString().trim();
    }

    /**
     * 构建章节生成提示词
     */
    private String buildChapterGenerationPrompt(NovelChapterPlan plan, NovelOutline outline,
                                                NovelWorldview worldview, NovelVolumePlan volume,
                                                String previousChaptersSummary) {
        var prompt = new StringBuilder();

        prompt.append("# 小说创作任务\n\n");

        // 添加大纲信息
        if (outline != null) {
            prompt.append("## 故事背景\n");
            prompt.append("- 故事类型：").append(outline.getGenre()).append("\n");
            prompt.append("- 故事基调：").append(outline.getTone()).append("\n");
            if (outline.getMainTheme() != null) {
                prompt.append("- 主要主题：").append(outline.getMainTheme()).append("\n");
            }
            if (outline.getOverallConcept() != null) {
                prompt.append("- 故事梗概：").append(outline.getOverallConcept()).append("\n");
            }
            prompt.append("\n");
        }

        // 添加世界观信息
        if (worldview != null) {
            factionService.fillForces(worldview);
            prompt.append("## 世界观设定\n");
            if (worldview.getWorldType() != null) {
                prompt.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
            }
            if (worldview.getWorldBackground() != null) {
                prompt.append("- 世界背景：").append(worldview.getWorldBackground()).append("\n");
            }
            String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
            if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                prompt.append("- 力量体系：").append(powerConstraint).append("\n");
            }
            if (worldview.getForces() != null) {
                prompt.append("- 势力分布：").append(worldview.getForces()).append("\n");
            }
            prompt.append("\n");
        }

        // 添加分卷信息
        if (volume != null) {
            prompt.append("## 当前分卷\n");
            prompt.append("- 卷号：第").append(volume.getVolumeNumber()).append("卷\n");
            prompt.append("- 卷名：").append(volume.getVolumeTitle()).append("\n");
            if (volume.getVolumeTheme() != null) {
                prompt.append("- 卷主旨：").append(volume.getVolumeTheme()).append("\n");
            }
            if (volume.getMainConflict() != null) {
                prompt.append("- 主要冲突：").append(volume.getMainConflict()).append("\n");
            }
            if (volume.getPlotArc() != null) {
                prompt.append("- 情节走向：").append(volume.getPlotArc()).append("\n");
            }
            prompt.append("\n");
        }

        // 添加已完成章节的剧情概要（用于保持连贯性）
        if (previousChaptersSummary != null && !previousChaptersSummary.isEmpty()) {
            prompt.append(previousChaptersSummary);
            prompt.append("\n**【剧情连贯性强制要求 - 违反将导致剧情崩坏】**\n");
            prompt.append("1. **严格承接上一章结尾（最重要！）**：\n");
            prompt.append("   - 本章开头必须直接从第").append(plan.getChapterNumber() - 1)
                    .append("章结尾的场景继续，不能跳过、不能重置\n");
            prompt.append("   - **禁止时间倒流**：不能写'重新出发'、'再次告别'、'开始踏上旅程'等倒退情节\n");
            prompt.append("   - **禁止重复情节**：如果前一章已经写过'告别家人'，本章不能再写一遍\n");
            prompt.append("   - **地点不能变**：前一章结尾主角在哪里，本章开头就在哪里\n\n");

            prompt.append("2. **人物状态绝对一致**：\n");
            prompt.append("   - 主角的能力、情绪、目标必须与上一章结束时**完全相同**\n");
            prompt.append("   - 如果上一章结尾主角受伤，本章开头必须仍然受伤\n");
            prompt.append("   - 如果上一章结尾主角已经到达某地，本章开头不能'在路上'\n\n");

            prompt.append("3. **禁止OOC（角色崩坏）**：人物行为必须符合其性格和之前建立的人设\n");
            prompt.append("4. **设定不矛盾**：已出现的能力、物品、势力设定不能遗忘或改变\n");
            prompt.append("5. **伏笔优先回收**：如果'待回收伏笔清单'中有合适的伏笔，优先在本章回收\n");
            prompt.append("6. **悬念延续**：本章结尾要留下新的悬念或钩子\n");
            prompt.append("7. **设定一致**：不能突然引入前文未铺垫的新设定（如突然冒出的新名词）\n\n");

            prompt.append("**❌ 严禁以下情况（反面教材）**：\n");
            prompt.append("- 第N章已经写'到达宗门'，第N+1章不能再写'告别父母出发'\n");
            prompt.append("- 第N章已经写'参加考核'，第N+1章不能再写'准备参加考核'\n");
            prompt.append("- 第N章已经写'受伤倒地'，第N+1章开头不能'毫发无损地站起来'\n");
            prompt.append("- 第N章已经写'获得宝物'，第N+1章不能'突然发现宝物'\n\n");
        }

        // 添加悬念与伏笔设计指南
        prompt.append("## 【专业作家技巧 - 必须应用】\n\n");
        prompt.append("### 1. 悬念设计（让读者欲罢不能）\n");
        prompt.append("**章节钩子要求**：\n");
        prompt.append("- 章尾必须设置钩子，让读者想知道'接下来会发生什么'\n");
        prompt.append("- 钩子类型（选择其一）：危机降临/秘密揭露/意外发现/重大决定/神秘人物\n");
        prompt.append("- 不要给出全部答案，保持一定神秘感\n\n");

        prompt.append("**情节节奏控制**：\n");
        prompt.append("- 70%内容：铺垫、对话、描写、心理活动（让读者了解人物）\n");
        prompt.append("- 20%内容：冲突升级、小危机、阻碍出现（制造紧张感）\n");
        prompt.append("- 10%内容：高潮片段、关键转折、重要发现（满足读者期待）\n\n");

        prompt.append("### 2. 小高潮设计（每2-3章一个）\n");
        int chapterNum = plan.getChapterNumber();
        if (chapterNum % 3 == 0) {
            prompt.append("**本章是小高潮章节，必须包含**：\n");
            prompt.append("- 冲突达到临界点并爆发\n");
            prompt.append("- 主角展现能力或底牌\n");
            prompt.append("- 出现意外转折或新信息\n");
            prompt.append("- 主角获得实质性收获（宝物、突破、认可等）\n\n");
        } else {
            prompt.append("**本章是铺垫章节，重点要求**：\n");
            prompt.append("- 铺设下一章高潮的伏笔\n");
            prompt.append("- 制造紧张感和期待感\n");
            prompt.append("- 通过对话透露重要信息\n");
            prompt.append("- 章尾必须留下强钩子\n\n");
        }

        prompt.append("### 3. 伏笔使用艺术（重要！）\n");
        prompt.append("**埋设新伏笔的具体方法**：\n");
        prompt.append("- 环境伏笔：通过场景细节暗示（如'角落里那面古镜泛着诡异的光'）\n");
        prompt.append("- 对话伏笔：角色'随口'说出重要信息（如'你长得真像二十年前的那个女人'）\n");
        prompt.append("- 物品伏笔：给看似普通的物品特写（如'那枚玉佩上的纹路似乎有些特别'）\n");
        prompt.append("- 行为伏笔：人物的异常举动（如'他总是不自觉地摸着左手腕的伤疤'）\n");
        prompt.append("- 本章必须在内容中埋设1-2个明显的新伏笔\n\n");

        prompt.append("**回收伏笔的具体方法**：\n");
        prompt.append("- 如果本章有前文伏笔需要回收，必须在本章给予交代\n");
        prompt.append("- 回收时要有情感冲击，让主角和读者都有'原来如此'的感觉\n");
        prompt.append("- 回收一个伏笔时，可同时埋下更大的新伏笔\n\n");

        prompt.append("### 4. 情节合理性与循序渐进（极其重要！）\n");
        prompt.append("**情节发展原则**：\n");
        prompt.append("- **因果逻辑**：每个事件必须有合理的前因后果，不能凭空发生\n");
        prompt.append("- **能力匹配**：主角解决问题的方式必须与其当前能力相符，不能突然开挂\n");
        prompt.append("- **难度递增**：随着故事推进，挑战难度要逐步提升，不能忽高忽低\n");
        prompt.append("- **铺垫充分**：重大转折前必须有足够的铺垫和暗示，不能突兀\n\n");

        prompt.append("**循序渐进要求**：\n");
        prompt.append("- **能力成长**：主角的能力提升要有过程（练习→失败→领悟→成功）\n");
        prompt.append("- **关系发展**：人物关系从陌生→认识→熟悉→信任，不能跳跃\n");
        prompt.append("- **世界探索**：对世界的了解由浅入深，先局部后整体\n");
        prompt.append("- **冲突升级**：小冲突（个人恩怨）→中冲突（势力对抗）→大冲突（世界危机）\n\n");

        prompt.append("**禁止出现的情况**：\n");
        prompt.append("- ❌ 主角突然获得未提及的能力解决困境\n");
        prompt.append("- ❌ 重要角色毫无征兆地改变立场\n");
        prompt.append("- ❌ 关键信息/道具凭空出现推动剧情\n");
        prompt.append("- ❌ 人物行为违背其已建立的性格和动机\n");
        prompt.append("- ❌ 上一章还在危机中，下一章突然解决却没有过程\n\n");

        prompt.append("### 5. 读者留存技巧\n");
        prompt.append("- **信息阶梯**：每章揭示一个新信息，但引出更多问题\n");
        prompt.append("- **情感共鸣**：让读者代入主角的情绪波动\n");
        prompt.append("- **期待感**：让主角和读者都知道'大事件即将发生'\n");
        prompt.append("- **满足感**：主角要有实质性的成长和收获\n\n");

        // 添加章节规划信息
        prompt.append("## 章节要求（必须严格执行）\n");
        prompt.append("- 章节序号：第").append(plan.getChapterNumber()).append("章\n");
        prompt.append("- 章节标题：").append(plan.getChapterTitle()).append("\n");
        if (plan.getChapterGoal() != null) {
            prompt.append("- 章节目标：").append(plan.getChapterGoal()).append("\n");
        }

        // 情节大纲 - 强调必须严格执行
        if (plan.getPlotOutline() != null) {
            prompt.append("\n### 【情节大纲 - 必须严格遵循】\n");
            prompt.append("**核心情节：** ").append(plan.getPlotOutline()).append("\n");
            prompt.append("**要求：** 生成的内容必须完全围绕上述情节大纲展开，不得偏离主题！\n");
        }

        // 关键事件 - 强调必须包含
        if (plan.getKeyEvents() != null) {
            prompt.append("\n### 【关键事件 - 必须全部包含】\n");
            prompt.append("**事件列表：** ").append(plan.getKeyEvents()).append("\n");
            prompt.append("**要求：** 必须按照上述事件的顺序展开剧情，每个事件都要详细描写，缺一不可！\n");
        }

        // 章节边界 - 强制约束起点和终点（非常重要！）
        if (plan.getChapterStartingScene() != null || plan.getChapterEndingScene() != null) {
            prompt.append("\n\n");
            prompt.append("####################################################################\n");
            prompt.append("## 【🚨 最高优先级 - 章节边界约束 - 违反将导致剧情崩坏】\n");
            prompt.append("####################################################################\n\n");

            if (plan.getChapterStartingScene() != null && !plan.getChapterStartingScene().isEmpty()) {
                prompt.append("**📍 本章起点（强制）：** ").append(plan.getChapterStartingScene()).append("\n\n");
                
                // 检查时间线是否倒流
                boolean hasTimeIssue = false;
                String timeWarning = "";
                if (previousChaptersSummary != null && !previousChaptersSummary.isEmpty()) {
                    String startScene = plan.getChapterStartingScene();
                    if (startScene.contains("上午") && previousChaptersSummary.contains("傍晚")) {
                        hasTimeIssue = true;
                        timeWarning = "⚠️ 时间线警告：上一章结束在'傍晚'，但本章开头是'上午'，时间倒流！";
                    } else if (startScene.contains("清晨") && previousChaptersSummary.contains("深夜")) {
                        hasTimeIssue = true;
                        timeWarning = "⚠️ 时间线警告：上一章结束在'深夜'，但本章开头是'清晨'，时间倒流！";
                    } else if (startScene.contains("上午") && previousChaptersSummary.contains("黄昏")) {
                        hasTimeIssue = true;
                        timeWarning = "⚠️ 时间线警告：上一章结束在'黄昏'，但本章开头是'上午'，时间倒流！";
                    }
                }
                
                if (hasTimeIssue) {
                    prompt.append("**").append(timeWarning).append("**\n\n");
                    prompt.append("**【时间过渡强制要求】**：\n");
                    prompt.append("由于时间线存在问题，本章必须以下处理方式开头：\n");
                    prompt.append("1. 使用时间过渡词：'第二天清晨'、'次日上午'、'几个时辰后'\n");
                    prompt.append("2. 简略交代中间发生的事情（如赴路、休息、疗伤）\n");
                    prompt.append("3. 然后再进入正式场景：").append(plan.getChapterStartingScene()).append("\n\n");
                }
                
                prompt.append("**【绝对强制要求】**：\n");
                prompt.append("1. 章节开头第一段必须从上述场景开始，一个字都不能差！\n");
                prompt.append("2. 不能有任何前置情节、过渡句、回忆杀！\n");
                prompt.append("3. 直接从起点场景开始写，例如：\"林枫站在...\"\n");
                prompt.append("4. 如果有【上一章最后几段】的内容，必须从那里紧接着写！\n\n");
            }

            if (plan.getChapterEndingScene() != null && !plan.getChapterEndingScene().isEmpty()) {
                prompt.append("**📍 本章终点（强制）：** ").append(plan.getChapterEndingScene()).append("\n\n");
                prompt.append("**【绝对强制要求】**：\n");
                prompt.append("1. 章节结尾必须结束在上述场景，不能多写也不能少写！\n");
                prompt.append("2. 不能写到下一章的内容（例如不要写\"第二天早上...\"）\n");
                prompt.append("3. 必须在终点场景处结束章节！\n\n");
            }

            prompt.append("**❌❌❌ 严禁违反：** 超出起终点范围将导致整个故事的剧情连贯性崩坏！\n");
            prompt.append("####################################################################\n\n");
        }

        if (plan.getWordCountTarget() != null) {
            int targetWords = plan.getWordCountTarget();
            prompt.append("\n####################################################################\n");
            prompt.append("## 【🚨 字数硬性要求 - 严格限制】\n");
            prompt.append("####################################################################\n");
            prompt.append("**目标字数：**").append(targetWords).append("字（±10%误差范围）\n");
            prompt.append("**最少字数：**").append((int)(targetWords * 0.9)).append("字\n");
            prompt.append("**最多字数：**").append((int)(targetWords * 1.1)).append("字\n\n");
            prompt.append("**【严格执行要求】**：\n");
            prompt.append("1. 内容达到").append((int)(targetWords * 0.95)).append("字左右时，必须准备收尾\n");
            prompt.append("2. 绝对不能超过").append((int)(targetWords * 1.15)).append("字，超过将被强制截断\n");
            prompt.append("3. 如果发现字数接近上限，立即进入结尾场景，不要再展开新情节\n");
            prompt.append("4. 宁可稍微少写，也不要严重超字数！\n");
            prompt.append("####################################################################\n\n");
        }
        if (plan.getChapterNotes() != null) {
            prompt.append("- 备注：").append(plan.getChapterNotes()).append("\n");
        }

        prompt.append("## 输出格式要求（中文作文格式）\n\n");
        prompt.append("【段落格式】\n");
        prompt.append("1. 每个段落开头必须有两个全角空格缩进（　　）\n");
        prompt.append("2. 段落之间用双换行符（空一行）分隔\n");
        prompt.append("3. 对话、描写、叙述、心理活动都应独立成段\n");
        prompt.append("4. 每段字数控制在100-300字之间\n\n");
        prompt.append("【正确示例】\n");
        prompt.append("　　林默坐在工位上，手指无意识地敲打着桌面。他真的拥有\"言出法随\"的能力，这个发现让他既兴奋又恐惧。昨天那个测试只是小打小闹，今天他决定要在职场中尝试一下这个能力。\n\n");
        prompt.append("　　张强是个典型的职场恶霸，仗着职位之便，经常把工作推给下属，出了问题却把责任全推给别人。林默来到这家公司两年，没少受他的气。\n\n");
        prompt.append("　　\"林默，给我倒杯咖啡，要双份糖的。\"张强习惯性地命令道，眼睛甚至没有从电脑屏幕上移开。\n\n");
        prompt.append("　　林默深吸一口气，决定今天要改变这种局面。他抬起头，直视着张强的眼睛。\n\n");
        prompt.append("【格式规则】\n");
        prompt.append("1. 严格使用两个全角空格（　　）作为段落开头缩进\n");
        prompt.append("2. 段落之间必须空一行（使用双换行符）\n");
        prompt.append("3. 人物对话要单独成段，并使用引号\n");
        prompt.append("4. 场景描写、动作描写、心理活动都要分段\n");
        prompt.append("5. 不要输出任何格式标记，只输出正文内容\n\n");

        // 强调必须遵循大纲
        if (plan.getPlotOutline() != null || plan.getKeyEvents() != null) {
            prompt.append("**【内容要求】**\n");
            prompt.append("- 必须严格按照「情节大纲」展开剧情，不得随意发挥或偏离主线\n");
            prompt.append("- 必须包含「关键事件」中的所有事件，并按顺序展开\n");
            prompt.append("- 每个关键事件都要有详细的场景描写、对话和人物心理活动\n");
            prompt.append("- 确保情节发展的连贯性和合理性\n\n");
        }

        prompt.append("1. 请根据以上信息创作完整的章节内容\n");
        prompt.append("2. 注意保持与已有情节的连贯性\n");
        prompt.append("3. 人物性格、行为要符合设定\n");
        prompt.append("4. 文笔流畅，情节引人入胜\n");
        prompt.append("5. 控制好章节节奏和篇幅，每段100-300字\n");
        prompt.append("6. 只输出章节正文内容，不要包含章节标题等额外信息\n");

        return prompt.toString();
    }

    // ==================== 数据库优先的章节上下文构建 ====================

    /**
     * 构建章节生成所需的完整上下文（数据库优先策略）
     * 使用三层记忆模型：短期/中期/长期
     *
     * @param projectId 项目ID
     * @param plan 当前章节规划
     * @return 章节上下文
     */
    private ChapterContext buildChapterContext(Long projectId, NovelChapterPlan plan) {
        log.info("构建章节上下文（数据库优先），projectId={}, chapterNumber={}", projectId, plan.getChapterNumber());

        ChapterContext context = new ChapterContext();

        // 1. 短期记忆：前3章的结构化记忆
        List<ChapterPlotMemory> recentMemories = plotMemoryMapper.selectRecentMemories(
                projectId, plan.getChapterNumber(), 3
        );
        context.setRecentMemories(recentMemories);
        log.debug("短期记忆：获取到{}章的结构化记忆", recentMemories.size());

        // 2. 待回收伏笔：从数据库精确查询
        List<String> pendingForeshadowing = foreshadowingService.getLatestPendingForeshadowing(
                projectId, plan.getChapterNumber()
        );
        context.setPendingForeshadowing(pendingForeshadowing);
        log.debug("待回收伏笔：{}条", pendingForeshadowing.size());

        // 3. 上一章结束场景
        String lastChapterEndingScene = getLastChapterEndingScene(projectId, plan.getChapterNumber());
        context.setLastChapterEndingScene(lastChapterEndingScene);

        // 4. 中期记忆：分卷信息
        if (plan.getVolumePlanId() != null) {
            NovelVolumePlan volumePlan = volumePlanMapper.selectById(plan.getVolumePlanId());
            context.setVolumePlan(volumePlan);
            log.debug("中期记忆：分卷信息={}", volumePlan != null ? volumePlan.getVolumeTitle() : "无");
        }

        // 5. 长期记忆：世界观（可能有多条，取最新的一条）
        LambdaQueryWrapper<NovelWorldview> worldviewQuery = new LambdaQueryWrapper<>();
        worldviewQuery.eq(NovelWorldview::getProjectId, projectId)
                .orderByDesc(NovelWorldview::getId)
                .last("LIMIT 1");
        NovelWorldview worldview = worldviewMapper.selectOne(worldviewQuery);
        context.setWorldview(worldview);

        // 6. 长期记忆：基础设置
        ProjectBasicSettings basicSettings = projectBasicSettingsService.getByProjectId(projectId);
        context.setBasicSettings(basicSettings);

        // 7. 本章涉及的角色（从章节规划中提取）
        List<NovelCharacter> chapterCharacters = getChapterCharacters(projectId, plan);
        context.setChapterCharacters(chapterCharacters);
        log.debug("本章涉及角色：{}个", chapterCharacters.size());

        log.info("章节上下文构建完成，projectId={}, chapterNumber={}", projectId, plan.getChapterNumber());
        return context;
    }

    /**
     * 获取本章涉及的角色
     * 从项目角色列表中获取主角和重要配角
     */
    private List<NovelCharacter> getChapterCharacters(Long projectId, NovelChapterPlan plan) {
        List<NovelCharacter> characters = new ArrayList<>();

        // 首先获取主角
        LambdaQueryWrapper<NovelCharacter> protagonistQuery = new LambdaQueryWrapper<>();
        protagonistQuery.eq(NovelCharacter::getProjectId, projectId)
                        .eq(NovelCharacter::getRoleType, "protagonist")
                        .last("LIMIT 3");
        List<NovelCharacter> protagonists = novelCharacterMapper.selectList(protagonistQuery);
        characters.addAll(protagonists);

        // 然后获取一些重要配角
        LambdaQueryWrapper<NovelCharacter> supportingQuery = new LambdaQueryWrapper<>();
        supportingQuery.eq(NovelCharacter::getProjectId, projectId)
                       .eq(NovelCharacter::getRoleType, "supporting")
                       .last("LIMIT 5");
        List<NovelCharacter> supporting = novelCharacterMapper.selectList(supportingQuery);
        characters.addAll(supporting);

        log.debug("获取到{}个角色信息", characters.size());
        return characters;
    }

    /**
     * 生成章节剧情概要
     *
     * @param content 章节内容
     * @param title 章节标题
     * @param chapterNumber 章节序号
     * @return 剧情概要
     */
    private String generatePlotSummary(Long projectId, NovelChapterPlan plan, Chapter chapter, String content, String title, Integer chapterNumber) {
        var summaryPrompt = String.format("""
            # 章节剧情概要生成任务

            请总结以下章节的剧情，生成一个简洁的剧情概要（100-200字）。

            **章节信息：**
            - 章节序号：第%d章
            - 章节标题：%s

            **章节内容：**
            %s

            **要求：**
            1. 总结本章节的核心情节
            2. 包含主要人物、关键事件、重要转折
            3. 突出对本章剧情推进有重要意义的内容
            4. 字数控制在100-200字
            5. 使用简洁、清晰的语言
            6. 只输出概要内容，不要包含其他说明文字
            """, chapterNumber, title, content.length() > 2000 ? content.substring(0, 2000) + "..." : content);

        try {
            AIGenerateRequest request = new AIGenerateRequest();
            request.setProjectId(projectId);
            request.setVolumePlanId(plan.getVolumePlanId());  // 记录所属分卷计划
            request.setChapterPlanId(plan.getId());              // 记录所属章节规划
            request.setChapterId(chapter.getId());               // 记录所属章节
            request.setRequestType("llm_chapter_summary_generate");
            request.setTask(summaryPrompt);
            request.setMaxTokens(500);
            request.setTemperature(0.3); // 使用较低的温度以获得更稳定、准确的概要

            AIGenerateResponse response = aiGenerateService.generate(request);
            var summary = response.getContent().trim();

            // 清理可能的格式符号
            summary = summary.replaceAll("^#+\\s*", "")
                    .replaceAll("^\\*+\\s*", "")
                    .replaceAll("^-+\\s*", "")
                    .trim();

            log.info("生成剧情概要成功，概要长度：{}", summary.length());
            return summary;
        } catch (Exception e) {
            log.error("生成剧情概要失败", e);
            // 如果AI生成失败，返回简单概要
            return String.format("本章讲述：%s。内容包括主角的经历和重要情节发展。", title);
        }
    }

    /**
     * 构建前N章的剧情概要（增强版，使用剧情记忆系统）
     */
    private String buildPreviousChaptersSummary(Long projectId, Integer currentChapterNumber) {
        if (currentChapterNumber <= 1) {
            return null;
        }

        try {
            // 查询前N章的详细剧情记忆
            var recentMemories = plotMemoryMapper.selectRecentMemories(
                    projectId, currentChapterNumber, PLOT_MEMORY_DEPTH);

            if (recentMemories.isEmpty()) {
                // 如果没有剧情记忆，回退到旧的方式
                log.warn("未找到剧情记忆，使用基础模式，projectId={}, currentChapter={}",
                        projectId, currentChapterNumber);
                return buildBasicPreviousSummary(projectId, currentChapterNumber);
            }

            var summaryBuilder = new StringBuilder();
            summaryBuilder.append("## 前").append(recentMemories.size()).append("章剧情记忆（必须严格遵循）\n\n");

            // 按时间顺序排列（从旧到新）
            for (int i = recentMemories.size() - 1; i >= 0; i--) {
                var memory = recentMemories.get(i);
                summaryBuilder.append(String.format("### 第%d章：%s\n",
                        memory.getChapterNumber(), memory.getChapterTitle()));

                // 剧情摘要
                if (memory.getPlotSummary() != null && !memory.getPlotSummary().isEmpty()) {
                    summaryBuilder.append("**剧情：** ").append(memory.getPlotSummary()).append("\n");
                }

                // 核心事件
                if (memory.getKeyEvents() != null && !memory.getKeyEvents().isEmpty()) {
                    summaryBuilder.append("**核心事件：** ").append(memory.getKeyEvents()).append("\n");
                }

                // 人物状态变化
                if (memory.getCharacterStatus() != null && !memory.getCharacterStatus().isEmpty()) {
                    summaryBuilder.append("**人物状态：** ").append(memory.getCharacterStatus()).append("\n");
                }

                // 新设定
                if (memory.getNewSettings() != null && !memory.getNewSettings().isEmpty()) {
                    summaryBuilder.append("**新设定：** ").append(memory.getNewSettings()).append("\n");
                }

                summaryBuilder.append("\n");
            }

            // 添加待回收的伏笔
            var pendingForeshadowing = plotMemoryMapper.selectPendingForeshadowing(projectId);
            if (!pendingForeshadowing.isEmpty()) {
                summaryBuilder.append("### 【🎯 待回收伏笔清单 - 必须处理】\n");
                summaryBuilder.append("**以下伏笔必须在近期章节（尤其是本章）给予回收或推进：**\n\n");

                int count = 0;
                for (ChapterPlotMemory memory : pendingForeshadowing) {
                    if (memory.getPendingForeshadowing() != null && !memory.getPendingForeshadowing().isEmpty()) {
                        var pending = memory.getPendingForeshadowing();
                        // 清理JSON格式，使其更易读
                        pending = pending.replace("[", "").replace("]", "").replace("\"", "").trim();
                        if (!pending.isEmpty() && !pending.equals("无") && !pending.equals("暂无")) {
                            summaryBuilder.append(String.format("%d. **第%d章埋下**：%s\n",
                                    ++count, memory.getChapterNumber(), pending));
                        }
                    }
                }

                if (count > 0) {
                    summaryBuilder.append("\n**⚠️ 伏笔使用要求：**\n");
                    summaryBuilder.append("- 如果本章情节适合，请优先回收最早的1-2个伏笔\n");
                    summaryBuilder.append("- 回收时要有戏剧性，制造'原来如此'的效果\n");
                    summaryBuilder.append("- 暂时不回收的伏笔，可以在本章给予新的线索或暗示\n\n");
                }
            }

            // 添加上一章结尾状态（最重要！）
            if (!recentMemories.isEmpty()) {
                ChapterPlotMemory lastMemory = recentMemories.get(0); // 最新的记忆
                var lastChapter = chapterMapper.selectById(lastMemory.getChapterId());

                summaryBuilder.append("## 【⚠️ 上章结尾状态 - 本章必须从此开始】\n");
                summaryBuilder.append(String.format("**第%d章《%s》结尾时的状态：**\n",
                        lastMemory.getChapterNumber(), lastMemory.getChapterTitle()));

                // 【新增】传递上一章的最后3-4段实际内容（不只是摘要）
                if (lastChapter != null && lastChapter.getContent() != null) {
                    String lastChapterContent = lastChapter.getContent();
                    // 按段落分割，取最后几段
                    String[] paragraphs = lastChapterContent.split("\n\n");
                    int lastParagraphsCount = Math.min(4, paragraphs.length);
                    StringBuilder lastParagraphs = new StringBuilder();
                    for (int i = paragraphs.length - lastParagraphsCount; i < paragraphs.length; i++) {
                        if (!paragraphs[i].trim().isEmpty()) {
                            lastParagraphs.append(paragraphs[i].trim()).append("\n\n");
                        }
                    }

                    summaryBuilder.append("**📖 上一章最后几段（本章必须直接继续）：**\n");
                    summaryBuilder.append(lastParagraphs.toString());
                    summaryBuilder.append("**⚠️ 本章开头第一段必须紧接着上面这段内容，不能跳过、不能重置！**\n\n");
                }

                // 新增：结尾场景（最重要！）
                if (lastMemory.getChapterEndingScene() != null && !lastMemory.getChapterEndingScene().isEmpty()) {
                    summaryBuilder.append("**📍 结尾场景摘要（本章必须从此开始）：** ")
                            .append(lastMemory.getChapterEndingScene()).append("\n");
                    summaryBuilder.append("**⚠️ 严禁**：本章开头不能改变时间、地点、状态，必须从上述场景直接继续！\n\n");
                }

                if (lastMemory.getCharacterStatus() != null && !lastMemory.getCharacterStatus().isEmpty()) {
                    summaryBuilder.append("**人物状态：** ").append(lastMemory.getCharacterStatus()).append("\n");
                }
                if (lastMemory.getCurrentSuspense() != null && !lastMemory.getCurrentSuspense().isEmpty()) {
                    summaryBuilder.append("**未解悬念：** ").append(lastMemory.getCurrentSuspense()).append("\n");
                }
                summaryBuilder.append(String.format("**剧情进度：** 第%d章结束后的状态\n", lastMemory.getChapterNumber()));
                summaryBuilder.append("\n**⚠️ 严禁**：本章开头不能重置时间线、不能忽略上章结尾的事件结果！\n\n");
            }

            // 添加剧情连贯性强制要求
            summaryBuilder.append("## 【剧情连贯性与合理性强制要求】\n");
            summaryBuilder.append("1. **严格承接**：本章开头必须紧接第").append(currentChapterNumber - 1).append("章结尾的状态\n");
            summaryBuilder.append("2. **人物状态连续性**：人物的能力、情绪、关系必须与上一章保持一致\n");
            summaryBuilder.append("3. **设定一致性**：已出现的设定不能矛盾，不能突然引入未铺垫的新设定\n");
            summaryBuilder.append("4. **伏笔回收**：优先回收上述清单中的伏笔\n");
            summaryBuilder.append("5. **悬念延续**：本章结尾要设置新的悬念\n");
            summaryBuilder.append("6. **循序渐进**：禁止能力/关系/情节跳跃式发展\n");
            summaryBuilder.append("7. **因果逻辑**：每个事件必须有合理的前因\n\n");

            return summaryBuilder.toString();

        } catch (Exception e) {
            log.error("构建剧情记忆概要失败，使用基础模式", e);
            return buildBasicPreviousSummary(projectId, currentChapterNumber);
        }
    }

    /**
     * 基础模式：构建前文概要（旧方式，用于降级）
     */
    private String buildBasicPreviousSummary(Long projectId, Integer currentChapterNumber) {
        LambdaQueryWrapper<Chapter> prevChaptersQuery = new LambdaQueryWrapper<>();
        prevChaptersQuery.eq(Chapter::getProjectId, projectId)
                .lt(Chapter::getChapterNumber, currentChapterNumber)
                .orderByAsc(Chapter::getChapterNumber);
        List<Chapter> previousChapters = chapterMapper.selectList(prevChaptersQuery);

        if (previousChapters.isEmpty()) {
            return null;
        }

        var summaryBuilder = new StringBuilder();
        summaryBuilder.append("## 已完成章节剧情概要\n\n");

        for (Chapter prevChapter : previousChapters) {
            summaryBuilder.append(String.format("**第%d章：%s**\n",
                    prevChapter.getChapterNumber(),
                    prevChapter.getTitle()));

            if (prevChapter.getPlotSummary() != null && !prevChapter.getPlotSummary().isEmpty()) {
                summaryBuilder.append(prevChapter.getPlotSummary()).append("\n");
            } else if (prevChapter.getContent() != null && prevChapter.getContent().length() > 0) {
                int previewLength = Math.min(200, prevChapter.getContent().length());
                summaryBuilder.append(prevChapter.getContent().substring(0, previewLength))
                        .append("...\n");
            }
            summaryBuilder.append("\n");
        }

        return summaryBuilder.toString();
    }

    /**
     * 保存章节剧情记忆（用于后续章节的连贯性）
     */
    private void saveChapterPlotMemory(Long projectId, Chapter chapter, NovelChapterPlan plan,
                                       String content, String plotSummary) {
        try {
            // 构建提示词，让AI提取关键信息
            String memoryPrompt = buildMemoryExtractionPrompt(chapter, content, plotSummary);

            AIGenerateRequest request = new AIGenerateRequest();
            request.setProjectId(projectId);
            request.setVolumePlanId(plan.getVolumePlanId());  // 记录所属分卷计划
            request.setChapterPlanId(plan.getId());              // 记录所属章节规划
            request.setChapterId(chapter.getId());               // 记录所属章节
            request.setRequestType("llm_chapter_memory_extract");
            request.setTask(memoryPrompt);
            request.setTemperature(0.3);

            AIGenerateResponse response = aiGenerateService.generate(request);
            String memoryXml = response.getContent();

            // 解析XML并保存（如果已存在则更新）
            parseAndSaveMemoryXml(projectId, chapter, plan, memoryXml, plotSummary, content);

        } catch (Exception e) {
            log.error("保存剧情记忆失败，chapterId={}", chapter.getId(), e);
            // 保存基础记忆
            saveBasicPlotMemory(projectId, chapter, plan, plotSummary);
        }
    }

    /**
     * 构建记忆提取提示词（使用模板系统）
     */
    private String buildMemoryExtractionPrompt(Chapter chapter, String content, String plotSummary) {
        try {
            // 构建模板变量
            Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("chapterNumber", chapter.getChapterNumber());
            variables.put("chapterTitle", chapter.getTitle());
            variables.put("wordCount", content.length());
            variables.put("plotSummary", plotSummary != null ? plotSummary : "");
            variables.put("chapterContent", content);

            // 执行模板
            String templateCode = "llm_chapter_memory_extract";
            return promptTemplateService.executeTemplate(templateCode, variables);

        } catch (Exception e) {
            log.error("使用模板构建记忆提取提示词失败，降级到基础模式", e);
            // 降级到基础模式
            return buildBasicMemoryExtractionPrompt(chapter, content, plotSummary);
        }
    }

    /**
     * 基础模式：构建记忆提取提示词（用于降级）
     */
    private String buildBasicMemoryExtractionPrompt(Chapter chapter, String content, String plotSummary) {
        return String.format("""
            # 网文章节剧情记忆提取
            从下述章节内容提取核心信息，用于后续剧情连贯承接。

            **章节信息**：
            - 第%d章：%s
            - 约%d字
            - 概要：%s

            **章节完整内容**：
            %s

            请以XML格式返回提取的信息。
            """,
                chapter.getChapterNumber(),
                chapter.getTitle(),
                content.length(),
                plotSummary != null ? plotSummary : "",
                content
        );
    }

    /**
     * 解析并保存记忆XML（使用通用XmlParser）
     */
    @Autowired
    private XmlParser xmlParser;

    private void parseAndSaveMemoryXml(Long projectId, Chapter chapter, NovelChapterPlan plan,
                                       String memoryXml, String plotSummary, String content) {
        try {
            // 使用通用XmlParser解析XML
            var memoryDto = xmlParser.parse(memoryXml, com.aifactory.dto.ChapterMemoryXmlDto.class);

            var memory = new ChapterPlotMemory();
            memory.setProjectId(projectId);
            memory.setChapterId(chapter.getId());
            memory.setChapterNumber(chapter.getChapterNumber());
            memory.setChapterTitle(chapter.getTitle());
            memory.setPlotSummary(plotSummary);
            memory.setChapterSummary(memoryDto.getChapterSummary());

            // 使用ObjectMapper转换数据
            var mapper = new ObjectMapper();

            // 核心事件列表
            if (memoryDto.getKeyEvents() != null && !memoryDto.getKeyEvents().isEmpty()) {
                memory.setKeyEvents(mapper.writeValueAsString(memoryDto.getKeyEvents()));
            }

            // 角色状态（转换为JSON字符串）
            if (memoryDto.getCharacterStatusMap() != null && !memoryDto.getCharacterStatusMap().isEmpty()) {
                memory.setCharacterStatus(formatCharacterStatus(memoryDto.getCharacterStatusMap()));
            }

            // 新设定（字符串）
            if (memoryDto.getNewSettings() != null && !memoryDto.getNewSettings().isEmpty()) {
                java.util.List<String> settingsList = java.util.List.of(memoryDto.getNewSettings());
                memory.setNewSettings(mapper.writeValueAsString(settingsList));
            }

            // 新埋伏笔（字符串）
            if (memoryDto.getForeshadowingPlanted() != null && !memoryDto.getForeshadowingPlanted().isEmpty()) {
                java.util.List<String> plantedList = java.util.List.of(memoryDto.getForeshadowingPlanted());
                memory.setForeshadowingPlanted(mapper.writeValueAsString(plantedList));
            }

            // 回收伏笔（字符串）
            if (memoryDto.getForeshadowingResolved() != null && !memoryDto.getForeshadowingResolved().isEmpty()) {
                java.util.List<String> resolvedList = java.util.List.of(memoryDto.getForeshadowingResolved());
                memory.setForeshadowingResolved(mapper.writeValueAsString(resolvedList));
            }

            // 结尾场景
            memory.setChapterEndingScene(memoryDto.getChapterEndingScene());

            // 当前悬念
            memory.setCurrentSuspense(memoryDto.getCurrentSuspense());

            // 构建待回收伏笔列表
            String pendingForeshadowing = buildPendingForeshadowing(projectId, chapter.getChapterNumber(),
                    memory.getForeshadowingPlanted(), memory.getForeshadowingResolved());
            memory.setPendingForeshadowing(pendingForeshadowing);

            memory.setUpdateTime(LocalDateTime.now());

            // 检查是否已存在，如果存在则更新，否则插入
            ChapterPlotMemory existingMemory = plotMemoryMapper.selectByChapterId(chapter.getId());
            if (existingMemory != null) {
                // 更新现有记录
                memory.setId(existingMemory.getId());
                memory.setCreateTime(existingMemory.getCreateTime()); // 保持创建时间不变
                plotMemoryMapper.updateById(memory);
                log.info("章节记忆XML解析并更新成功，chapterId={}", chapter.getId());
            } else {
                // 插入新记录
                memory.setCreateTime(LocalDateTime.now());
                plotMemoryMapper.insert(memory);
                log.info("章节记忆XML解析并保存成功，chapterId={}", chapter.getId());
            }

        } catch (Exception e) {
            log.error("解析记忆XML失败，保存基础记忆，chapterId={}", chapter.getId(), e);
            saveBasicPlotMemory(projectId, chapter, plan, plotSummary);
        }
    }

    /**
     * 提取XML标签内容
     */
    private String extractTagContent(String line, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = line.indexOf(startTag);
        int end = line.lastIndexOf(endTag);
        if (start != -1 && end != -1) {
            return line.substring(start + startTag.length(), end);
        }
        return null;
    }

    /**
     * 格式化角色状态为字符串
     */
    private String formatCharacterStatus(java.util.Map<String, String> characterStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (var entry : characterStatus.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 构建待回收伏笔列表
     */
    private String buildPendingForeshadowing(Long projectId, Integer currentChapterNumber,
                                             String newForeshadowing, String resolvedForeshadowing) {
        try {
            var mapper = new ObjectMapper();
            
            // 查询前一章的待回收伏笔
            List<ChapterPlotMemory> prevMemories = plotMemoryMapper.selectRecentMemories(
                    projectId, currentChapterNumber, 1);

            // 使用List保持顺序，使用智能去重
            java.util.List<String> pending = new java.util.ArrayList<>();

            // 继承前一章的待回收伏笔
            if (!prevMemories.isEmpty() && prevMemories.get(0).getPendingForeshadowing() != null) {
                String prevPending = prevMemories.get(0).getPendingForeshadowing();
                pending.addAll(parseJsonArray(prevPending));
            }

            // 添加本章新埋下的伏笔（智能去重）
            if (newForeshadowing != null && !newForeshadowing.isEmpty()) {
                java.util.List<String> newItems = parseJsonArray(newForeshadowing);
                for (String newItem : newItems) {
                    if (!isSimilarForeshadowingExists(pending, newItem)) {
                        pending.add(newItem);
                    }
                }
            }

            // 移除本章回收的伏笔（智能匹配）
            if (resolvedForeshadowing != null && !resolvedForeshadowing.isEmpty()) {
                java.util.List<String> resolvedItems = parseJsonArray(resolvedForeshadowing);
                for (String resolved : resolvedItems) {
                    // 如果resolved是"无回收伏笔"或类似值，跳过
                    if (resolved.contains("无回收") || resolved.contains("暂无")) {
                        continue;
                    }
                    // 智能移除相似的伏笔
                    pending.removeIf(p -> isForeshadowingResolved(p, resolved));
                }
            }

            // 限制最大数量，避免过多
            if (pending.size() > 10) {
                pending = pending.subList(0, 10);
            }

            // 使用Jackson序列化为JSON数组
            return mapper.writeValueAsString(pending);

        } catch (Exception e) {
            log.warn("构建待回收伏笔列表失败", e);
            return "[]";
        }
    }

    /**
     * 解析JSON数组字符串
     */
    private java.util.List<String> parseJsonArray(String jsonArray) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (jsonArray == null || jsonArray.isEmpty()) {
            return result;
        }
        
        try {
            var mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonArray);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    String text = item.asText().trim();
                    if (!text.isEmpty() && !text.equals("暂无") && !text.equals("无")) {
                        result.add(text);
                    }
                }
            }
        } catch (Exception e) {
            // 降级处理：手动分割
            if (jsonArray.startsWith("[") && jsonArray.endsWith("]")) {
                String[] items = jsonArray.substring(1, jsonArray.length() - 1).split(",");
                for (String item : items) {
                    String text = item.trim().replace("\"", "");
                    if (!text.isEmpty() && !text.equals("暂无") && !text.equals("无")) {
                        result.add(text);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 检查是否存在相似的伏笔
     */
    private boolean isSimilarForeshadowingExists(java.util.List<String> existing, String newItem) {
        String newLower = newItem.toLowerCase();
        for (String existingItem : existing) {
            String existingLower = existingItem.toLowerCase();
            // 检查包含关系或高相似度
            if (existingLower.contains(newLower) || newLower.contains(existingLower)) {
                return true;
            }
            // 检查关键词重叠（简单版）
            if (hasSignificantOverlap(existingItem, newItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查两个伏笔是否有显著重叠
     */
    private boolean hasSignificantOverlap(String s1, String s2) {
        // 提取关键词（扩展关键词库）
        String[] keyWords = {"玉佩", "身世", "体质", "神秘", "灵根", "伏笔", 
                            "地图", "纹路", "共鸣", "灵体", "试炼", "天剑宗"};
        int commonWords = 0;
        for (String keyword : keyWords) {
            if (s1.contains(keyword) && s2.contains(keyword)) {
                commonWords++;
            }
        }
        // 如果有1个以上关键词重叠，认为是相似的（降低阈值）
        return commonWords >= 1;
    }

    /**
     * 判断一个伏笔是否已被回收
     */
    private boolean isForeshadowingResolved(String pending, String resolved) {
        // 直接匹配
        if (pending.equalsIgnoreCase(resolved)) {
            return true;
        }
        // 包含关系
        String pendingLower = pending.toLowerCase();
        String resolvedLower = resolved.toLowerCase();
        if (pendingLower.contains(resolvedLower) || resolvedLower.contains(pendingLower)) {
            return true;
        }
        // 检查关键词重叠
        return hasSignificantOverlap(pending, resolved);
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        var jsonStr = response;

        // 如果响应包含```json标记，提取其中的JSON
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                jsonStr = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                jsonStr = response.substring(start, end).trim();
            }
        }

        // 查找第一个{和最后一个}
        int firstBrace = jsonStr.indexOf("{");
        int lastBrace = jsonStr.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
        }

        return jsonStr;
    }

    /**
     * 从AI响应中提取XML
     */
    private String extractXmlFromResponse(String response) {
        var xmlStr = response.trim();

        // 如果响应包含```xml标记，提取其中的XML
        if (response.contains("```xml")) {
            int start = response.indexOf("```xml") + 6;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                xmlStr = response.substring(start, end).trim();
            }
        }

        // 查找第一个<M>和最后一个</M>
        int firstTag = xmlStr.indexOf("<M>");
        int lastTag = xmlStr.lastIndexOf("</M>");
        if (firstTag >= 0 && lastTag > firstTag) {
            xmlStr = xmlStr.substring(firstTag, lastTag + 5);
        }

        return xmlStr;
    }

    /**
     * 保存基础剧情记忆（降级方案）
     */
    private void saveBasicPlotMemory(Long projectId, Chapter chapter, NovelChapterPlan plan, String plotSummary) {
        try {
            var memory = new ChapterPlotMemory();
            memory.setProjectId(projectId);
            memory.setChapterId(chapter.getId());
            memory.setChapterNumber(chapter.getChapterNumber());
            memory.setChapterTitle(chapter.getTitle());
            memory.setPlotSummary(plotSummary);
            memory.setKeyEvents(plan.getKeyEvents() != null ? "[\"" + plan.getKeyEvents() + "\"]" : "[]");
            memory.setCreateTime(LocalDateTime.now());
            memory.setUpdateTime(LocalDateTime.now());

            plotMemoryMapper.insert(memory);
            log.info("基础剧情记忆保存成功，chapterId={}", chapter.getId());
        } catch (Exception e) {
            log.error("保存基础剧情记忆失败，chapterId={}", chapter.getId(), e);
        }
    }

    // ==================== AI辅助功能 ====================

    /**
     * AI剧情修复
     * 检查并修复章节中的逻辑错误、重复内容、设定不一致等问题
     */
    public ChapterAiFixResponse fixChapterWithAI(
            Long chapterId,
            ChapterAiFixRequest request) {

        log.info("AI剧情修复开始，chapterId={}", chapterId);

        // 获取当前章节
        Chapter currentChapter = chapterMapper.selectById(chapterId);
        if (currentChapter == null) {
            throw new RuntimeException("章节不存在");
        }

        // 获取前后章节内容
        String prevContent = null;
        String nextContent = null;
        ChapterPlotMemory prevMemory = null;

        // 尝试获取前一章内容（支持两种方式：通过ID或通过章节序号）
        Chapter prevChapter = null;

        // 方式1：通过prevChapterId查询
        if (request.getPrevChapterId() != null) {
            try {
                prevChapter = chapterMapper.selectById(Long.valueOf(request.getPrevChapterId()));
            } catch (NumberFormatException e) {
                log.warn("prevChapterId格式错误，尝试作为章节序号查询: {}", request.getPrevChapterId());
            }
        }

        // 方式2：如果方式1失败，尝试通过当前章节序号-1查询
        if (prevChapter == null && currentChapter.getChapterNumber() != null && currentChapter.getChapterNumber() > 1) {
            Integer prevChapterNumber = currentChapter.getChapterNumber() - 1;
            log.info("通过章节序号查询前一章，prevChapterNumber={}", prevChapterNumber);

            prevChapter = chapterMapper.selectOne(
                    new LambdaQueryWrapper<Chapter>()
                            .eq(Chapter::getProjectId, currentChapter.getProjectId())
                            .eq(Chapter::getChapterNumber, prevChapterNumber)
            );
        }

        if (prevChapter != null) {
            prevContent = prevChapter.getContent();
            log.info("成功获取前一章内容，章节ID={}, 章节序号={}", prevChapter.getId(), prevChapter.getChapterNumber());

            // 获取前一章的情节记忆
            List<ChapterPlotMemory> memories = plotMemoryMapper.selectList(
                    new LambdaQueryWrapper<ChapterPlotMemory>()
                            .eq(ChapterPlotMemory::getChapterId, prevChapter.getId())
            );
            if (!memories.isEmpty()) {
                prevMemory = memories.get(0);
                log.info("找到前一章情节记忆，章节ID={}", prevChapter.getId());
            }
        } else {
            log.warn("未找到前一章内容，currentChapterNumber={}", currentChapter.getChapterNumber());
        }

        if (request.getNextChapterId() != null) {
            Chapter nextChapter = chapterMapper.selectById(request.getNextChapterId());
            if (nextChapter != null) {
                nextContent = nextChapter.getContent();
            }
        }

        // ========== 阶段1：检查问题 ==========
        log.info("开始阶段1：检查章节问题");
        ChapterIssueCheckResponse checkResponse = checkChapterIssues(
                currentChapter,
                request.getContent(),
                prevContent,
                nextContent,
                request.getFixOptions(),
                request.getCustomRequirements(),
                prevMemory
        );

        if (checkResponse.getTotalIssues() == 0) {
            log.info("未发现问题，返回原始内容");
            ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();
            fixResponse.setFixedContent(request.getContent());
            fixResponse.setTotalFixes(0);
            fixResponse.setFixSummary("未发现问题，内容正常");
            fixResponse.setFixReport(new ArrayList<>());
            return fixResponse;
        }

        log.info("阶段1完成，发现{}个问题", checkResponse.getTotalIssues());

        // ========== 阶段2：应用修复 ==========
        log.info("开始阶段2：应用修复");
        ChapterAiFixResponse fixResponse = applyFixes(
                currentChapter,
                request.getContent(),
                prevContent,
                checkResponse
        );

        log.info("AI剧情修复完成，chapterId={}，共修复{}处问题", chapterId, fixResponse.getTotalFixes());

        return fixResponse;
    }

    /**
     * AI润色
     * 对章节内容进行文学润色，提升文笔和描写
     */
    public ChapterAiPolishResponse polishChapterWithAI(
            Long chapterId,
            ChapterAiPolishRequest request) {

        log.info("AI润色开始，chapterId={}，style={}", chapterId, request.getStyle());

        // 获取当前章节
        Chapter currentChapter = chapterMapper.selectById(chapterId);
        if (currentChapter == null) {
            throw new RuntimeException("章节不存在");
        }

        // 构建润色提示词
        var prompt = buildPolishPrompt(
                request.getContent(),
                request.getStyle(),
                request.getPolishLevel(),
                request.getCustomRequirements()
        );

        // 调用AI生成
        AIGenerateRequest aiRequest = new AIGenerateRequest();
        aiRequest.setProjectId(currentChapter.getProjectId());
        aiRequest.setVolumePlanId(currentChapter.getVolumePlanId());  // 记录所属分卷计划
        aiRequest.setChapterPlanId(currentChapter.getChapterPlanId());  // 记录所属章节规划
        aiRequest.setChapterId(currentChapter.getId());                 // 记录章节ID
        aiRequest.setRequestType("llm_chapter_polish");
        aiRequest.setTask(prompt);
        aiRequest.setMaxTokens(8000);
        aiRequest.setTemperature(0.4);

        AIGenerateResponse aiResponse = aiGenerateService.generate(aiRequest);
        String response = aiResponse.getContent();

        // 解析响应
        ChapterAiPolishResponse polishResponse = parsePolishResponse(response);

        log.info("AI润色完成，chapterId={}", chapterId);

        return polishResponse;
    }

    /**
     * 构建剧情修复Prompt（使用Java 17 Text Blocks）
     */
    private String buildFixPrompt(String content, String prevContent, String nextContent,
                                  List<String> fixOptions, String customRequirements,
                                  ChapterPlotMemory prevMemory) {
        // 基础提示词模板
        var basePrompt = """
            # 网络小说剧情修复任务

            ## 你的角色
            你是一位资深网络小说编辑，擅长发现并修复剧情问题。

            """;

        var prompt = new StringBuilder(basePrompt);

        // 【新增】添加前一章情节记忆信息（结构化关键信息）
        if (prevMemory != null) {
            prompt.append("## 【重要】前一章结构化信息\n");

            if (prevMemory.getKeyEvents() != null && !prevMemory.getKeyEvents().isEmpty()) {
                prompt.append("**关键事件**：\n").append(prevMemory.getKeyEvents()).append("\n\n");
            }

            if (prevMemory.getCharacterStatus() != null && !prevMemory.getCharacterStatus().isEmpty()) {
                prompt.append("**人物状态**：\n").append(prevMemory.getCharacterStatus()).append("\n\n");
            }

            if (prevMemory.getNewSettings() != null && !prevMemory.getNewSettings().isEmpty()) {
                prompt.append("**新增设定**（物品、功法、地名等专有名词）：\n");
                prompt.append(prevMemory.getNewSettings()).append("\n\n");
            }

            if (prevMemory.getChapterEndingScene() != null && !prevMemory.getChapterEndingScene().isEmpty()) {
                prompt.append("**⚠️ 前一章结尾场景**（本章必须从此开始）：\n");
                prompt.append(prevMemory.getChapterEndingScene()).append("\n");
                prompt.append("**【强制要求】** 本章开头必须直接从上述场景继续，不能改变时间、地点、状态！\n\n");
            }

            if (prevMemory.getCurrentSuspense() != null && !prevMemory.getCurrentSuspense().isEmpty()) {
                prompt.append("**前一章留下的悬念**（本章必须回应或处理）：\n");
                prompt.append(prevMemory.getCurrentSuspense()).append("\n\n");
            }
        }

        // 添加前后文内容
        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("## 前一章内容（供参考）\n");
            var truncatedPrevContent = prevContent.length() > 500 ? prevContent.substring(0, 500) + "..." : prevContent;
            prompt.append(truncatedPrevContent).append("\n\n");
        }

        prompt.append("## 需要修复的章节内容\n");
        prompt.append(content);
        prompt.append("\n\n");

        if (nextContent != null && !nextContent.isEmpty()) {
            prompt.append("## 后一章内容（供参考）\n");
            var truncatedNextContent = nextContent.length() > 500 ? nextContent.substring(0, 500) + "..." : nextContent;
            prompt.append(truncatedNextContent).append("\n\n");
        }

        prompt.append("""
            ## 修复要求
            请严格检查以下问题类型：

            1. **【章节衔接】时间线倒流（最严重！）**：
               - 本章开头是否重复了前一章已经叙述过的情节（如告别家人、再次出发等）
               - 本章开头是否从错误的时间点开始（应该从【前一章结尾场景】直接继续）
               - 如果有【前一章结尾场景】，本章开头第一段必须从该场景继续，不能跳过、不能重置
               - 修复方式：删除重复部分，直接从正确的承接点开始

            2. **【人物名字冲突】（重点检查）**：
            """);

        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("""
               - 检查本章是否引入了与前一章**同名但不同人**的角色
               - 例如：前一章已有'赵明'（少年弟子），本章不能再出现'赵明'（中年执事）
               - 如果必须出现同名角色，必须明确说明两者的关系（如父子、师徒）

            """);
        }
        prompt.append("   - 每个重要角色必须有唯一的名字，不能重复使用\n\n");

        prompt.append("""
            3. **【人物出现/消失】（重点检查）**：
            """);

        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("""
               - 检查前一章出现的重要角色在本章中的去向
               - 如果前一章结尾时某角色在场（如'围坐篝火旁'），本章开头必须说明该角色的去向
               - 不能让重要角色莫名消失（如：前一章的'赵明、李雪、张阳'三人，本章必须交代他们去哪了）

            """);
        }
        prompt.append("   - 如果角色离开，必须有合理的过渡情节\n\n");

        prompt.append("4. **【专有名词一致性】**：\n");
        if (prevMemory != null && prevMemory.getNewSettings() != null && !prevMemory.getNewSettings().isEmpty()) {
            prompt.append("   前一章引入的设定：").append(prevMemory.getNewSettings()).append("\n");
        }
        prompt.append("""
           - 检查物品名称、功法名称、地名、门派名称是否与【前一章结构化信息-新增设定】一致
           - 例如：前一章叫'紫云剑诀'，本章不能叫'青木剑典'，必须保持一致
           - 如果发现不一致，必须统一为前一章的叫法

            5. **【悬念回收】（重点检查）**：
            """);

        if (prevMemory != null && prevMemory.getCurrentSuspense() != null && !prevMemory.getCurrentSuspense().isEmpty()) {
            prompt.append("   前一章悬念：").append(prevMemory.getCurrentSuspense()).append("\n");
        }
        prompt.append("""
           - 本章是否处理了前一章结尾留下的悬念或危机
           - 如果前一章结尾主角'被困山洞'，本章开头必须解决这个困境
           - 如果没有处理，必须在本章开头补充解决情节

            6. **【人物状态一致性】**：
            """);

        if (prevMemory != null && prevMemory.getCharacterStatus() != null && !prevMemory.getCharacterStatus().isEmpty()) {
            prompt.append("   前一章人物状态：").append(prevMemory.getCharacterStatus()).append("\n");
        }
        prompt.append("""
           - 检查本章人物的位置、状态是否与【前一章结构化信息-人物状态】一致
           - 人物称呼要准确，不能用简写（如'枫'应该改为'林枫'）

            7. **设定不一致**：人物能力忽高忽低、物品属性变化、与前后文矛盾
            8. **伏笔回收**：检查本章是否揭晓了前文埋下的伏笔
            9. **时间线问题**：时间跳跃不合理、场景切换突兀

            """);

        // 添加前后章关键信息帮助AI判断
        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("## 前一章结尾信息（供衔接判断）\n");
            var prevEnding = prevContent.length() > 300 ?
                    prevContent.substring(prevContent.length() - 300) : prevContent;
            prompt.append(prevEnding).append("\n\n");
        }

        if (customRequirements != null && !customRequirements.isEmpty()) {
            prompt.append("## 额外修复要求\n");
            prompt.append(customRequirements).append("\n\n");
        }

        prompt.append("""
            ## 修复原则
            - 保持原有情节主线不变，只修复问题
            - 不添加新的情节或人物
            - 确保修复后的内容与前后文衔接自然

            ## 输出格式（必须是合法JSON）
            【重要】返回的必须是标准合法JSON格式！

            JSON格式要求：
            1. 不能有markdown代码块标记（不要 ```json 和 ``` ）
            2. 所有字符串必须用英文双引号"包裹
            3. 字符串内部的"必须转义为\"
            4. 【重要】fixedContent字段是可选的，可以省略
            5. fixReport是必填字段，包含了所有修改的详细信息

            JSON结构如下：
            {
              "fixSummary": "修复说明摘要",
              "totalFixes": 5,
              "fixReport": [
                {
                  "type": "问题类型",
                  "severity": "high/medium/low",
                  "original": "原文的具体片段（不要描述，要实际文本）",
                  "fixed": "修改后的具体片段（不要描述，要实际文本）",
                  "reason": "修改原因"
                }
              ]
            }

            【说明】系统会根据fixReport中的original和fixed字段自动对原文进行精确替换，
            因此你只需要返回详细的fixReport，无需手动拼接完整的fixedContent。

            **【重要】fixReport字段说明：**
            - original字段：必须是原文中的具体文本片段，不要写描述
            - fixed字段：必须是修改后的具体文本片段，不要写描述
            - ❌ 错误示例：original="出现了名字冲突", fixed="改为李长老"
            - ✅ 正确示例：original="他正是天剑宗外门执事赵明", fixed="他正是天剑宗外门执事李长老"
            - ✅ 正确示例：original="林枫说道：'好的，枫。'", fixed="林枫说道：'好的，林枫。'"

            **【重要】如果问题是整个段落缺失或多余**：
            - original可以填：缺失的段落位置描述（如"第3段开头缺少过渡"）
            - fixed可以填：应该添加的完整段落内容

            【严禁】返回的JSON必须可以被标准JSON解析器解析，否则会报错！
            """);

        return prompt.toString();
    }

    /**
     * 构建润色Prompt（使用Java 17 Text Blocks）
     */
    private String buildPolishPrompt(String content, String style, String polishLevel,
                                     String customRequirements) {
        var prompt = new StringBuilder();

        prompt.append("""
            # 网络小说文学润色任务

            ## 你的角色
            你是一位资深网络小说作家，擅长文字润色与优化。

            ## 需要润色的章节内容
            """);

        prompt.append(content);
        prompt.append("\n\n");

        prompt.append("## 润色要求\n");

        // 根据风格设置不同的润色重点（使用Switch Expressions - Java 14+）
        var styleDesc = switch (style != null ? style : "vivid") {
            case "fast" -> "紧凔节奏型：加快叙事节奏，删减冗余描述，让文章更紧凑刹达";
            case "literary" -> "文学优化型：提升文学性，增强修辞和典故运用";
            default -> "细腻描写型：增加感官细节（视觉、听觉、触觉），让场景更有画面感";
        };

        prompt.append("润色风格：").append(styleDesc).append("\n");

        var levelDesc = switch (polishLevel != null ? polishLevel : "medium") {
            case "light" -> "轻度：保持原有风格，只做必要优化";
            case "heavy" -> "深度：全面优化，显著提升文学质量";
            default -> "中度：平衡优化，在保持原意的基础上提升表达";
        };

        prompt.append("润色程度：").append(levelDesc).append("\n\n");

        prompt.append("""
            润色范围：
            1. **描写生动**：增加感官细节，让场景更有画面感
            2. **对话自然**：让对话更符合人物性格，避免生硬
            3. **节奏流畅**：调整句子长短，控制叙事节奏
            4. **用词精准**：替换平淡词汇，使用更有表现力的词语

            """);

        if (customRequirements != null && !customRequirements.isEmpty()) {
            prompt.append("## 额外润色要求\n");
            prompt.append(customRequirements).append("\n\n");
        }

        prompt.append("""
            ## 润色原则
            - 保持原有情节不变
            - 保持人物性格不变
            - 保持原有段落结构

            ## 输出格式（JSON）
            【重要】返回的必须是标准JSON格式！

            JSON格式要求：
            1. 不能有markdown代码块标记（不要 ```json 和 ``` ）
            2. polishedContent字段是字符串，段落之间用\n分隔（两个字符：反斜杠+n）
            3. 例如："第一段内容\n\n第二段内容\n\n第三段内容"

            {
              "polishedContent": "润色后的完整内容",
              "polishSummary": "润色说明摘要",
              "polishReport": {
                "descriptionImprovements": 10,
                "dialogueOptimizations": 5,
                "pacingAdjustments": 3,
                "wordOptimizations": 8,
                "sentenceOptimizations": 6,
                "totalOptimizations": 32
              }
            }

            【重要】直接返回纯JSON字符串，不要包含markdown代码块标记（不要 ```json 和 ``` ）！
            """);

        return prompt.toString();
    }

    /**
     * 解析修复响应（增强版：添加正则提取兜底方案）
     */
    private ChapterAiFixResponse parseFixResponse(String response, String originalContent) {
        ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();

        try {
            // 首先尝试 XML 解析（新格式）
            if (response.trim().startsWith("<")) {
                log.info("检测到 XML 格式响应，尝试 XML 解析");
                try {
                    return parseFixResponseXml(response, originalContent);
                } catch (Exception e) {
                    log.warn("XML 解析失败，回退到 JSON 解析: {}", e.getMessage());
                }
            }

            // 回退到 JSON 解析（旧格式兼容）
            var jsonStr = extractJsonFromResponse(response);

            // 记录原始响应用于调试
            log.info("AI修复响应原始内容长度: {}", jsonStr.length());

            // 尝试解析JSON
            var mapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                jsonNode = mapper.readTree(jsonStr);
            } catch (com.fasterxml.jackson.core.JsonParseException e) {
                log.warn("JSON解析失败，尝试修复JSON格式: {}", e.getMessage());
                try {
                    // 尝试修复可能的JSON格式问题
                    String fixedJson = attemptToFixJson(jsonStr);
                    jsonNode = mapper.readTree(fixedJson);
                    log.info("JSON格式修复成功");
                } catch (Exception e2) {
                    log.warn("JSON修复后仍然解析失败，尝试使用正则提取: {}", e2.getMessage());
                    // 最后的兜底方案：使用正则表达式提取数据
                    extractFixResponseByRegex(jsonStr, fixResponse);

                    // 如果正则提取成功（fixedContent被设置了），则直接返回
                    if (fixResponse.getFixedContent() != null && !fixResponse.getFixedContent().isEmpty()) {
                        log.info("正则提取成功，返回提取的数据");
                        return fixResponse;
                    }

                    // 如果正则提取也失败，继续抛出异常
                    throw e2;
                }
            }

            if (jsonNode != null) {
                // 优先使用fixedContent（如果AI提供了）
                if (jsonNode.has("fixedContent") && !jsonNode.get("fixedContent").isNull()) {
                    var content = jsonNode.get("fixedContent").asText();
                    if (!content.contains("\n") && content.contains("\\n")) {
                        content = content.replace("\\n", "\n");
                    }
                    content = content.replace("\r", "");
                    fixResponse.setFixedContent(content);
                    log.info("使用AI返回的fixedContent，长度: {}", content.length());
                } else {
                    // 【新增】如果没有fixedContent，则根据fixReport对原文进行替换
                    log.info("AI未返回fixedContent，根据fixReport进行替换");
                    String fixedContent = applyFixReportToContent(originalContent, jsonNode);
                    fixResponse.setFixedContent(fixedContent);
                }

                if (jsonNode.has("fixSummary")) {
                    fixResponse.setFixSummary(jsonNode.get("fixSummary").asText());
                }

                if (jsonNode.has("totalFixes")) {
                    fixResponse.setTotalFixes(jsonNode.get("totalFixes").asInt());
                }

                // 解析修复报告列表
                if (jsonNode.has("fixReport") && jsonNode.get("fixReport").isArray()) {
                    var reportItems = new ArrayList<ChapterAiFixResponse.FixReportItem>();

                    for (JsonNode item : jsonNode.get("fixReport")) {
                        var reportItem = new ChapterAiFixResponse.FixReportItem();

                        if (item.has("type")) reportItem.setType(item.get("type").asText());
                        if (item.has("severity")) reportItem.setSeverity(item.get("severity").asText());
                        if (item.has("original")) reportItem.setOriginal(item.get("original").asText());
                        if (item.has("fixed")) reportItem.setFixed(item.get("fixed").asText());
                        if (item.has("reason")) reportItem.setReason(item.get("reason").asText());

                        reportItems.add(reportItem);
                    }

                    fixResponse.setFixReport(reportItems);
                }
            }

        } catch (Exception e) {
            log.error("解析AI修复响应失败", e);
            log.error("原始响应内容: {}", response.length() > 1000 ? response.substring(0, 1000) + "..." : response);
            fixResponse.setFixedContent(response);
            fixResponse.setFixSummary("响应解析失败，返回原始内容");
            fixResponse.setTotalFixes(0);
        }

        return fixResponse;
    }

    /**
     * 尝试修复JSON格式问题（简化版：只做最基本的修复）
     */
    private String attemptToFixJson(String jsonStr) {
        try {
            // 只做最基本的修复：替换中文引号
            String fixed = jsonStr.replace('"', '"').replace('"', '"');
            return fixed;
        } catch (Exception e) {
            log.warn("JSON修复过程出错，返回原始字符串: {}", e.getMessage());
            return jsonStr;
        }
    }

    /**
     * 解析 XML 格式的修复响应
     */
    private ChapterAiFixResponse parseFixResponseXml(String xmlResponse, String originalContent) {
        ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();

        try {
            // 提取 fixedContent（支持 CDATA）
            String fixedContent = extractXmlTagContent(xmlResponse, "fixedContent");
            if (fixedContent != null && !fixedContent.isEmpty()) {
                fixResponse.setFixedContent(fixedContent);
                log.info("从 XML 中提取 fixedContent，长度: {}", fixedContent.length());
            } else {
                // 如果没有 fixedContent，使用原文
                fixResponse.setFixedContent(originalContent);
            }

            // 提取 fixSummary
            String fixSummary = extractXmlTagContent(xmlResponse, "fixSummary");
            if (fixSummary != null) {
                fixResponse.setFixSummary(fixSummary);
            }

            // 提取 totalFixes
            java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("<totalFixes>(\\d+)</totalFixes>");
            java.util.regex.Matcher totalMatcher = totalPattern.matcher(xmlResponse);
            if (totalMatcher.find()) {
                fixResponse.setTotalFixes(Integer.parseInt(totalMatcher.group(1)));
            }

            // 提取 fixReport 列表
            java.util.List<ChapterAiFixResponse.FixReportItem> reportItems = new ArrayList<>();
            java.util.regex.Pattern itemPattern = java.util.regex.Pattern.compile("<fixItem>(.*?)</fixItem>", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher itemMatcher = itemPattern.matcher(xmlResponse);

            while (itemMatcher.find()) {
                String itemXml = itemMatcher.group(1);
                ChapterAiFixResponse.FixReportItem reportItem = new ChapterAiFixResponse.FixReportItem();

                // 提取各个字段
                String type = extractXmlTagContent(itemXml, "type");
                if (type != null) reportItem.setType(type);

                String severity = extractXmlTagContent(itemXml, "severity");
                if (severity != null) reportItem.setSeverity(severity);

                String original = extractXmlTagContent(itemXml, "original");
                if (original != null) reportItem.setOriginal(original);

                String fixed = extractXmlTagContent(itemXml, "fixed");
                if (fixed != null) reportItem.setFixed(fixed);

                String reason = extractXmlTagContent(itemXml, "reason");
                if (reason != null) reportItem.setReason(reason);

                reportItems.add(reportItem);
            }

            fixResponse.setFixReport(reportItems);
            log.info("XML 解析修复响应成功，共修复 {} 处", reportItems.size());

        } catch (Exception e) {
            log.error("XML 解析修复响应失败", e);
            throw new RuntimeException("XML 解析失败", e);
        }

        return fixResponse;
    }

    /**
     * 根据fixReport对原文进行精确替换
     * Java 17特性：使用var类型推断
     */
    private String applyFixReportToContent(String originalContent, JsonNode jsonResponse) {
        var fixedContent = originalContent;

        if (!jsonResponse.has("fixReport") || !jsonResponse.get("fixReport").isArray()) {
            log.warn("JSON中没有fixReport数组，返回原文");
            return originalContent;
        }

        var fixReport = jsonResponse.get("fixReport");
        var fixCount = 0;

        for (var item : fixReport) {
            if (!item.has("original") || !item.has("fixed")) {
                continue;
            }

            var original = item.get("original").asText();
            var fixed = item.get("fixed").asText();

            // 精确替换：使用String.replace()进行精确匹配
            if (original != null && !original.isEmpty() && fixed != null) {
                // 处理转义字符
                var normalizedOriginal = original.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
                var normalizedFixed = fixed.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");

                // 检查原文中是否包含需要替换的内容
                if (fixedContent.contains(normalizedOriginal)) {
                    fixedContent = fixedContent.replace(normalizedOriginal, normalizedFixed);
                    fixCount++;
                    log.info("应用修复：替换了 {} 个字符", normalizedOriginal.length());
                } else {
                    log.warn("未找到需要替换的文本: {}", normalizedOriginal.substring(0, Math.min(50, normalizedOriginal.length())));
                }
            }
        }

        log.info("根据fixReport完成 {} 处修复", fixCount);
        return fixedContent;
    }

    /**
     * 使用正则表达式提取修复响应数据（兜底方案 - 改进版）
     */
    private void extractFixResponseByRegex(String response, ChapterAiFixResponse fixResponse) {
        try {
            // 策略：直接从 fixedContent": " 开始，找到最后的 "} " 或 "}" 结束
            int startIndex = response.indexOf("\"fixedContent\"");
            if (startIndex == -1) {
                log.warn("未找到fixedContent字段");
                return;
            }

            // 找到fixedContent值的开始引号
            int valueStart = response.indexOf("\"", startIndex + 16);
            if (valueStart == -1) {
                log.warn("未找到fixedContent值的开始引号");
                return;
            }

            // 从开始引号往后找，找到匹配的结束引号
            // 需要计数，因为字符串值中可能有转义的引号
            int searchIndex = valueStart + 1;
            boolean endFound = false;
            StringBuilder content = new StringBuilder();

            while (searchIndex < response.length()) {
                char c = response.charAt(searchIndex);

                if (c == '\\' && searchIndex + 1 < response.length()) {
                    // 转义字符，读取下一个字符
                    char nextChar = response.charAt(searchIndex + 1);
                    if (nextChar == 'n') {
                        content.append('\n');
                    } else if (nextChar == 'r') {
                        content.append('\r');
                    } else if (nextChar == 't') {
                        content.append('\t');
                    } else if (nextChar == '\\') {
                        content.append('\\');
                    } else if (nextChar == '"') {
                        content.append('"');
                    } else {
                        // 其他转义字符，保持原样
                        content.append(c);
                        content.append(nextChar);
                    }
                    searchIndex += 2;
                } else if (c == '"') {
                    // 找到结束引号
                    // 检查后面是否是 JSON 分隔符
                    if (searchIndex + 1 < response.length()) {
                        char nextChar = response.charAt(searchIndex + 1);
                        if (nextChar == ',' || nextChar == '}' || Character.isWhitespace(nextChar)) {
                            endFound = true;
                            break;
                        }
                    }
                    content.append(c);
                    searchIndex++;
                } else {
                    content.append(c);
                    searchIndex++;
                }
            }

            if (endFound) {
                String contentStr = content.toString();
                // 将JSON转义的换行符转换为真实换行符
                contentStr = contentStr.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
                fixResponse.setFixedContent(contentStr);
                log.info("使用正则提取到fixedContent，长度: {}", contentStr.length());
            } else {
                log.warn("未找到fixedContent的结束引号");
            }

            // 提取fixSummary
            java.util.regex.Pattern summaryPattern = java.util.regex.Pattern.compile("\"fixSummary\"\\s*:\\s*\"([^\"]*)\"");
            java.util.regex.Matcher summaryMatcher = summaryPattern.matcher(response);
            if (summaryMatcher.find()) {
                fixResponse.setFixSummary(summaryMatcher.group(1));
            }

            // 提取totalFixes
            java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("\"totalFixes\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher totalMatcher = totalPattern.matcher(response);
            if (totalMatcher.find()) {
                fixResponse.setTotalFixes(Integer.parseInt(totalMatcher.group(1)));
            }

            // fixReport数组提取较为复杂，这里简化处理
            fixResponse.setFixReport(new ArrayList<>());

            log.info("使用正则提取修复响应成功");

        } catch (Exception e) {
            log.warn("正则提取修复响应失败: {}", e.getMessage());
            // 即使正则提取失败，也不要抛出异常，让外层catch处理
        }
    }

    /**
     * 解析润色响应
     */
    private ChapterAiPolishResponse parsePolishResponse(String response) {
        ChapterAiPolishResponse polishResponse = new ChapterAiPolishResponse();

        try {
            var jsonStr = extractJsonFromResponse(response);
            var mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(jsonStr);

            if (jsonNode.has("polishedContent")) {
                var content = jsonNode.get("polishedContent").asText();
                // 处理换行符 - Jackson会自动将JSON中的\n转换为真正的换行符
                // 但如果AI返回的是字面量\n（两个字符），需要手动转换
                if (content.contains("\\n")) {
                    content = content.replace("\\n", "\n");
                }
                polishResponse.setPolishedContent(content);
            } else {
                polishResponse.setPolishedContent(response);
            }

            if (jsonNode.has("polishSummary")) {
                polishResponse.setPolishSummary(jsonNode.get("polishSummary").asText());
            }

            // 解析润色报告
            if (jsonNode.has("polishReport")) {
                JsonNode report = jsonNode.get("polishReport");
                ChapterAiPolishResponse.PolishReport polishReport =
                        new ChapterAiPolishResponse.PolishReport();

                if (report.has("descriptionImprovements")) {
                    polishReport.setDescriptionImprovements(report.get("descriptionImprovements").asInt());
                }
                if (report.has("dialogueOptimizations")) {
                    polishReport.setDialogueOptimizations(report.get("dialogueOptimizations").asInt());
                }
                if (report.has("pacingAdjustments")) {
                    polishReport.setPacingAdjustments(report.get("pacingAdjustments").asInt());
                }
                if (report.has("wordOptimizations")) {
                    polishReport.setWordOptimizations(report.get("wordOptimizations").asInt());
                }
                if (report.has("sentenceOptimizations")) {
                    polishReport.setSentenceOptimizations(report.get("sentenceOptimizations").asInt());
                }
                if (report.has("totalOptimizations")) {
                    polishReport.setTotalOptimizations(report.get("totalOptimizations").asInt());
                }

                polishResponse.setPolishReport(polishReport);
            }

        } catch (Exception e) {
            log.error("解析AI润色响应失败", e);
            polishResponse.setPolishedContent(response);
            polishResponse.setPolishSummary("响应解析失败，返回原始内容");
        }

        return polishResponse;
    }

    /**
     * 从章节内容中提取结尾场景
     * 当AI没有返回chapterEndingScene时使用
     */
    private String extractEndingSceneFromContent(String content) {
        try {
            // 按段落分割
            String[] paragraphs = content.split("\n\n");
            if (paragraphs.length == 0) {
                return "未知场景";
            }

            // 获取最后一段（去除全角空格）
            String lastParagraph = paragraphs[paragraphs.length - 1].trim().replaceAll("^[　\\s]+", "");

            // 如果最后一段太长（超过200字），截取前100字作为场景描述
            if (lastParagraph.length() > 200) {
                lastParagraph = lastParagraph.substring(0, 100) + "...";
            }

            // 尝试从最后一段中提取关键信息：地点、时间、状态
            // 简单的做法是直接使用最后一段作为场景描述
            return lastParagraph;

        } catch (Exception e) {
            log.warn("提取结尾场景失败", e);
            return "未知场景";
        }
    }

    /**
     * 从章节内容中推断悬念
     * 当AI没有返回currentSuspense时使用
     */
    private String inferSuspenseFromContent(String content) {
        try {
            // 按段落分割
            String[] paragraphs = content.split("\n\n");
            if (paragraphs.length == 0) {
                return "待续";
            }

            // 获取最后一段
            String lastParagraph = paragraphs[paragraphs.length - 1].trim().replaceAll("^[　\\s]+", "");

            // 检查最后一段是否包含钩子词
            String[] hookKeywords = {
                    "突然", "就在这时", "没想到", "原来", "发现", "没想到", "突然间",
                    "?", "！", "吗", "呢", "了", "要", "想", "道"
            };

            boolean hasHook = false;
            for (String keyword : hookKeywords) {
                if (lastParagraph.contains(keyword)) {
                    hasHook = true;
                    break;
                }
            }

            if (hasHook) {
                // 从最后一段截取50字作为悬念描述
                String suspense = lastParagraph;
                if (suspense.length() > 50) {
                    suspense = suspense.substring(0, 50) + "...";
                }
                return suspense;
            }

            return "待续";

        } catch (Exception e) {
            log.warn("推断悬念失败", e);
            return "待续";
        }
    }

    // ==================== 两阶段修复相关方法 ====================

    /**
     * 阶段1：检查章节问题（不修改，只检查）
     */
    private ChapterIssueCheckResponse checkChapterIssues(
            Chapter chapter,
            String content,
            String prevContent,
            String nextContent,
            List<String> fixOptions,
            String customRequirements,
            ChapterPlotMemory prevMemory) {

        try {
            // 构建检查提示词
            var prompt = buildCheckPrompt(content, prevContent, nextContent, fixOptions, customRequirements, prevMemory);

            // 调用AI检查
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(chapter.getProjectId());
            aiRequest.setVolumePlanId(chapter.getVolumePlanId());  // 记录所属分卷计划
            aiRequest.setChapterPlanId(chapter.getChapterPlanId());  // 记录所属章节规划
            aiRequest.setChapterId(chapter.getId());                 // 记录章节ID
            aiRequest.setRequestType("llm_chapter_issue_check");
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(3000); // 检查阶段不需要太多token
            aiRequest.setTemperature(0.3);

            AIGenerateResponse aiResponse = aiGenerateService.generate(aiRequest);
            String response = aiResponse.getContent();

            // 解析检查结果（支持 XML 格式）
            return parseCheckResponse(response);

        } catch (Exception e) {
            log.error("检查章节问题失败", e);
            // 返回空问题列表
            ChapterIssueCheckResponse checkResponse = new ChapterIssueCheckResponse();
            checkResponse.setIssues(new ArrayList<>());
            checkResponse.setTotalIssues(0);
            checkResponse.setCheckSummary("检查失败：" + e.getMessage());
            checkResponse.setHasSevereIssues(false);
            return checkResponse;
        }
    }

    /**
     * 阶段2：应用修复（根据检查结果修改）
     */
    private ChapterAiFixResponse applyFixes(
            Chapter chapter,
            String content,
            String prevContent,
            ChapterIssueCheckResponse checkResponse) {

        try {
            // 构建修复提示词
            var prompt = buildApplyFixesPrompt(content, prevContent, checkResponse);

            // 调用AI修复
            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(chapter.getProjectId());
            aiRequest.setVolumePlanId(chapter.getVolumePlanId());  // 记录所属分卷计划
            aiRequest.setChapterPlanId(chapter.getChapterPlanId());  // 记录所属章节规划
            aiRequest.setChapterId(chapter.getId());                 // 记录章节ID
            aiRequest.setRequestType("llm_chapter_fix");
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(8000);
            aiRequest.setTemperature(0.3);

            AIGenerateResponse aiResponse = aiGenerateService.generate(aiRequest);
            String response = aiResponse.getContent();

            // 解析修复结果（支持 XML 格式）
            return parseFixResponse(response, content);

        } catch (Exception e) {
            log.error("应用修复失败", e);
            ChapterAiFixResponse fixResponse = new ChapterAiFixResponse();
            fixResponse.setFixedContent(content);
            fixResponse.setTotalFixes(0);
            fixResponse.setFixSummary("修复失败：" + e.getMessage());
            fixResponse.setFixReport(new ArrayList<>());
            return fixResponse;
        }
    }

    /**
     * 解析检查响应（支持 XML 和 JSON 格式）
     */
    private ChapterIssueCheckResponse parseCheckResponse(String response) {
        ChapterIssueCheckResponse checkResponse = new ChapterIssueCheckResponse();

        try {
            // 首先尝试 XML 解析（新格式）
            if (response.trim().startsWith("<")) {
                log.info("检测到 XML 格式响应，尝试 XML 解析");
                try {
                    return parseCheckResponseXml(response);
                } catch (Exception e) {
                    log.warn("XML 解析失败，回退到 JSON 解析: {}", e.getMessage());
                }
            }

            // 回退到 JSON 解析（旧格式兼容）
            var jsonStr = extractJsonFromResponse(response);

            // 尝试解析JSON
            try {
                parseCheckJson(jsonStr, checkResponse);
            } catch (JsonProcessingException e) {
                log.warn("JSON解析失败，尝试修复JSON格式: {}", e.getMessage());
                try {
                    String fixedJson = attemptToFixJson(jsonStr);
                    parseCheckJson(fixedJson, checkResponse);
                } catch (Exception e2) {
                    log.warn("JSON修复后仍然解析失败，尝试使用正则提取: {}", e2.getMessage());
                    // 最后的兜底方案：使用正则表达式提取数据
                    extractCheckResponseByRegex(response, checkResponse);
                }
            }

        } catch (Exception e) {
            log.error("解析检查响应完全失败", e);
            checkResponse.setIssues(new ArrayList<>());
            checkResponse.setTotalIssues(0);
            checkResponse.setCheckSummary("解析失败");
            checkResponse.setHasSevereIssues(false);
        }

        return checkResponse;
    }

    /**
     * 解析 XML 格式的检查响应
     */
    private ChapterIssueCheckResponse parseCheckResponseXml(String xmlResponse) {
        ChapterIssueCheckResponse checkResponse = new ChapterIssueCheckResponse();

        try {
            // 使用简单的字符串解析来提取 XML 内容（避免复杂的 XML 解析器配置）
            // 提取 checkResult 标签内的内容

            // 提取 totalIssues
            java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("<totalIssues>(\\d+)</totalIssues>");
            java.util.regex.Matcher totalMatcher = totalPattern.matcher(xmlResponse);
            if (totalMatcher.find()) {
                checkResponse.setTotalIssues(Integer.parseInt(totalMatcher.group(1)));
            }

            // 提取 hasSevereIssues
            java.util.regex.Pattern severePattern = java.util.regex.Pattern.compile("<hasSevereIssues>(true|false)</hasSevereIssues>");
            java.util.regex.Matcher severeMatcher = severePattern.matcher(xmlResponse);
            if (severeMatcher.find()) {
                checkResponse.setHasSevereIssues(Boolean.parseBoolean(severeMatcher.group(1)));
            }

            // 提取 checkSummary（支持 CDATA）
            String checkSummary = extractXmlTagContent(xmlResponse, "checkSummary");
            if (checkSummary != null) {
                checkResponse.setCheckSummary(checkSummary);
            }

            // 提取 issues 列表
            java.util.List<ChapterIssueCheckResponse.IssueItem> issues = new ArrayList<>();
            java.util.regex.Pattern issuePattern = java.util.regex.Pattern.compile("<issue>(.*?)</issue>", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher issueMatcher = issuePattern.matcher(xmlResponse);

            while (issueMatcher.find()) {
                String issueXml = issueMatcher.group(1);
                ChapterIssueCheckResponse.IssueItem issue = new ChapterIssueCheckResponse.IssueItem();

                // 提取各个字段
                String type = extractXmlTagContent(issueXml, "type");
                if (type != null) issue.setType(type);

                String severity = extractXmlTagContent(issueXml, "severity");
                if (severity != null) issue.setSeverity(severity);

                String location = extractXmlTagContent(issueXml, "location");
                if (location != null) issue.setLocation(location);

                String originalText = extractXmlTagContent(issueXml, "originalText");
                if (originalText != null) issue.setOriginalText(originalText);

                String description = extractXmlTagContent(issueXml, "description");
                if (description != null) issue.setDescription(description);

                String suggestion = extractXmlTagContent(issueXml, "suggestion");
                if (suggestion != null) issue.setSuggestion(suggestion);

                issues.add(issue);
            }

            checkResponse.setIssues(issues);
            log.info("XML 解析检查响应成功，共发现 {} 个问题", issues.size());

        } catch (Exception e) {
            log.error("XML 解析检查响应失败", e);
            throw new RuntimeException("XML 解析失败", e);
        }

        return checkResponse;
    }

    /**
     * 提取 XML 标签内容（支持 CDATA）
     */
    private String extractXmlTagContent(String xml, String tagName) {
        // 尝试匹配 CDATA 格式：<tagName><![CDATA[content]]></tagName>
        java.util.regex.Pattern cdataPattern = java.util.regex.Pattern.compile(
            "<" + tagName + ">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</" + tagName + ">",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher cdataMatcher = cdataPattern.matcher(xml);
        if (cdataMatcher.find()) {
            return cdataMatcher.group(1);
        }

        // 尝试匹配普通格式：<tagName>content</tagName>
        java.util.regex.Pattern normalPattern = java.util.regex.Pattern.compile(
            "<" + tagName + ">(.*?)</" + tagName + ">",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher normalMatcher = normalPattern.matcher(xml);
        if (normalMatcher.find()) {
            return normalMatcher.group(1);
        }

        return null;
    }

    /**
     * 使用正则表达式提取检查响应数据（兜底方案）
     */
    private void extractCheckResponseByRegex(String response, ChapterIssueCheckResponse checkResponse) {
        try {
            java.util.List<ChapterIssueCheckResponse.IssueItem> issues = new ArrayList<>();

            // 提取checkSummary
            java.util.regex.Pattern summaryPattern = java.util.regex.Pattern.compile("checkSummary[^:]*:[^\"]*\"([^\"]+)\"");
            java.util.regex.Matcher summaryMatcher = summaryPattern.matcher(response);
            if (summaryMatcher.find()) {
                checkResponse.setCheckSummary(summaryMatcher.group(1));
            }

            // 提取totalIssues
            java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("totalIssues[^:]*:\\s*(\\d+)");
            java.util.regex.Matcher totalMatcher = totalPattern.matcher(response);
            if (totalMatcher.find()) {
                checkResponse.setTotalIssues(Integer.parseInt(totalMatcher.group(1)));
            }

            // 提取hasSevereIssues
            java.util.regex.Pattern severePattern = java.util.regex.Pattern.compile("hasSevereIssues[^:]*:\\s*(true|false)");
            java.util.regex.Matcher severeMatcher = severePattern.matcher(response);
            if (severeMatcher.find()) {
                checkResponse.setHasSevereIssues(Boolean.parseBoolean(severeMatcher.group(1)));
            }

            // 简化的提取逻辑：只提取问题类型和描述，不提取完整的issues数组
            // 如果需要提取完整的issues，需要更复杂的正则表达式
            log.info("使用正则提取成功，提取到摘要: {}", checkResponse.getCheckSummary());

        } catch (Exception e) {
            log.warn("正则提取失败: {}", e.getMessage());
            checkResponse.setIssues(new ArrayList<>());
            checkResponse.setTotalIssues(0);
            checkResponse.setCheckSummary("JSON解析失败，且正则提取也失败");
            checkResponse.setHasSevereIssues(false);
        }
    }

    /**
     * 解析检查JSON（辅助方法）
     */
    private void parseCheckJson(String jsonStr, ChapterIssueCheckResponse checkResponse)
            throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var jsonNode = mapper.readTree(jsonStr);

        // 解析issues数组
        if (jsonNode.has("issues") && jsonNode.get("issues").isArray()) {
            java.util.List<ChapterIssueCheckResponse.IssueItem> issues =
                    new ArrayList<>();

            for (JsonNode item : jsonNode.get("issues")) {
                ChapterIssueCheckResponse.IssueItem issueItem =
                        new ChapterIssueCheckResponse.IssueItem();

                if (item.has("type")) issueItem.setType(item.get("type").asText());
                if (item.has("severity")) issueItem.setSeverity(item.get("severity").asText());
                if (item.has("location")) issueItem.setLocation(item.get("location").asText());
                if (item.has("originalText")) issueItem.setOriginalText(item.get("originalText").asText());
                if (item.has("description")) issueItem.setDescription(item.get("description").asText());
                if (item.has("suggestion")) issueItem.setSuggestion(item.get("suggestion").asText());

                issues.add(issueItem);
            }

            checkResponse.setIssues(issues);
            checkResponse.setTotalIssues(issues.size());
        }

        if (jsonNode.has("checkSummary")) {
            checkResponse.setCheckSummary(jsonNode.get("checkSummary").asText());
        }

        if (jsonNode.has("hasSevereIssues")) {
            checkResponse.setHasSevereIssues(jsonNode.get("hasSevereIssues").asBoolean());
        } else {
            // 根据问题列表判断是否有严重问题
            boolean hasSevere = checkResponse.getIssues() != null &&
                    checkResponse.getIssues().stream().anyMatch(issue -> "high".equals(issue.getSeverity()));
            checkResponse.setHasSevereIssues(hasSevere);
        }
    }

    /**
     * 构建检查提示词（阶段1：只检查，不修改）
     */
    private String buildCheckPrompt(String content, String prevContent, String nextContent,
                                    List<String> fixOptions, String customRequirements,
                                    ChapterPlotMemory prevMemory) {
        var prompt = new StringBuilder();

        prompt.append("""
            # 章节问题检查任务

            ## 你的角色
            你是一位资深网络小说编辑，擅长发现剧情问题。

            **【重要】你的任务：只检查问题，不修改内容！**

            """);

        // 添加前一章记忆信息
        if (prevMemory != null) {
            prompt.append("## 【重要】前一章结构化信息\n\n");
            if (prevMemory.getKeyEvents() != null && !prevMemory.getKeyEvents().isEmpty()) {
                prompt.append("**关键事件**：\n").append(prevMemory.getKeyEvents()).append("\n\n");
            }
            if (prevMemory.getCharacterStatus() != null && !prevMemory.getCharacterStatus().isEmpty()) {
                prompt.append("**人物状态**：\n").append(prevMemory.getCharacterStatus()).append("\n\n");
            }
            if (prevMemory.getNewSettings() != null && !prevMemory.getNewSettings().isEmpty()) {
                prompt.append("**新增设定**（物品、功法、地名、专有名词）：\n").append(prevMemory.getNewSettings()).append("\n\n");
            }
            if (prevMemory.getChapterEndingScene() != null && !prevMemory.getChapterEndingScene().isEmpty()) {
                prompt.append("**⚠️ 前一章结尾场景**：\n").append(prevMemory.getChapterEndingScene()).append("\n\n");
            }
        }

        // 【新增】添加前一章结尾的实际内容（用于检查在场角色）
        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("## 【重要】前一章结尾实际内容（最后4段）\n\n");
            var prevParagraphs = prevContent.split("\n\n");
            var lastParagraphsCount = Math.min(4, prevParagraphs.length);
            var lastParagraphs = new StringBuilder();
            for (var i = prevParagraphs.length - lastParagraphsCount; i < prevParagraphs.length; i++) {
                if (!prevParagraphs[i].trim().isEmpty()) {
                    lastParagraphs.append(prevParagraphs[i].trim()).append("\n\n");
                }
            }
            prompt.append(lastParagraphs.toString());
            prompt.append("**【强制要求】**：上述内容中提到的所有角色，在本章开头必须合理交代去向！\n\n");
        }

        prompt.append("## 需要检查的章节内容\n\n");
        prompt.append(content);
        prompt.append("\n\n");

        prompt.append("""
            ## 检查要求

            请严格检查以下问题类型，发现问题时记录到issues数组中：

            1. **【章节衔接】**：
               - 本章开头是否从【前一章结尾场景】直接继续
               - 地点、时间、状态是否一致

            2. **【人物名字冲突】（重点检查）**：
            """);

        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("""
               **步骤1**：从【前一章结尾实际内容】中提取所有角色名字
               **步骤2**：检查本章是否使用了相同的名字
               **步骤3**：如果有重名，判断是否为同一人
               - 如果不是同一人，必须报告为问题（严重）
               - 例如：前一章有'少年弟子赵明'，本章出现'外门执事赵明'，这是名字冲突！

            """);
        }

        prompt.append("""
            3. **【人物消失检查】（重点检查）**：
            """);

        if (prevContent != null && !prevContent.isEmpty()) {
            prompt.append("""
               **步骤1**：从【前一章结尾实际内容】中列出所有在场角色
               **步骤2**：逐一检查这些角色在本章开头是否还在场
               **步骤3**：如果角色在前一章结尾时在场（如'围坐篝火旁'），本章开头必须交代去向
               - 如果角色莫名消失（没有交代离开），必须报告为问题（严重）
               - 例如：前一章结尾有'赵明、李雪、张阳三人围坐篝火旁'，本章开头必须交代他们去哪了

            """);
        }

        prompt.append("4. **【专有名词一致性】**：\n");
        if (prevMemory != null && prevMemory.getNewSettings() != null && !prevMemory.getNewSettings().isEmpty()) {
            prompt.append("   前一章引入的设定：").append(prevMemory.getNewSettings()).append("\n");
        }
        prompt.append("   - 物品、功法、地名、门派名称是否一致\n\n");

        prompt.append("5. **【悬念回收】**：\n");
        if (prevMemory != null && prevMemory.getCurrentSuspense() != null && !prevMemory.getCurrentSuspense().isEmpty()) {
            prompt.append("   前一章悬念：").append(prevMemory.getCurrentSuspense()).append("\n");
        }
        prompt.append("   - 是否处理了前一章留下的悬念\n\n");

        prompt.append("""
            6. **【新角色介绍】**：
               - 本章出现的新角色是否有适当的介绍
               - 是否与现有情节冲突

            7. **【与知识库设定的一致性】**：
               - 检查当前章节是否与知识库中的世界观设定冲突
               - 检查角色行为是否符合知识库中的角色设定
               - 检查是否遗漏了知识库中记录的重要伏笔

            ## 输出格式（必须是XML格式）

            **【XML格式要求】**：
            1. 必须使用XML格式，不能使用JSON格式
            2. 根节点必须是<checkResult>
            3. 字符串内容可以使用中文引号，需要正确转义XML特殊字符（< > & " '）
            4. 每个问题用<issue>标签表示
            5. severity只能是：high（严重）、medium（中等）、low（轻微）

            **【特别注意suggestion字段】**：
            - suggestion字段可以使用正常的中文描述
            - 使用CDATA包裹建议内容，避免特殊字符问题

            XML结构如下：
            <checkResult>
              <issues>
                <issue>
                  <type>问题类型</type>
                  <severity>high</severity>
                  <location>位置描述</location>
                  <originalText><![CDATA[原文片段]]></originalText>
                  <description><![CDATA[问题描述]]></description>
                  <suggestion><![CDATA[修改建议，可以使用完整句子和中文标点]]></suggestion>
                </issue>
              </issues>
              <checkSummary><![CDATA[检查总结]]></checkSummary>
              <totalIssues>问题总数</totalIssues>
              <hasSevereIssues>true或false</hasSevereIssues>
            </checkResult>

            **【重要】**：如果没有发现问题，issues标签为空即可。直接返回纯XML字符串，不要包含markdown代码块标记！
            """);

        return prompt.toString();
    }

    /**
     * 构建应用修复提示词（阶段2：根据检查结果修改）
     */
    private String buildApplyFixesPrompt(String content, String prevContent,
                                         ChapterIssueCheckResponse checkResponse) {
        var prompt = new StringBuilder();

        prompt.append("""
            # 章节修复任务

            ## 你的角色
            你是一位资深网络小说编辑，擅长修复剧情问题。

            **【重要】**：根据下面的问题清单，修复章节内容。

            """);

        prompt.append("## 需要修复的内容\n\n");

        prompt.append(content);
        prompt.append("\n\n");

        prompt.append("## 发现的问题清单\n\n");
        if (checkResponse.getIssues() != null && !checkResponse.getIssues().isEmpty()) {
            var index = 1;
            for (var issue : checkResponse.getIssues()) {
                prompt.append("### 问题").append(index++).append("\n");
                prompt.append("- **类型**：").append(issue.getType()).append("\n");
                prompt.append("- **严重程度**：").append(issue.getSeverity()).append("\n");
                prompt.append("- **位置**：").append(issue.getLocation()).append("\n");
                prompt.append("- **原文片段**：").append(issue.getOriginalText()).append("\n");
                prompt.append("- **问题描述**：").append(issue.getDescription()).append("\n");
                prompt.append("- **修改方向**：").append(issue.getSuggestion()).append("\n\n");
            }
        }

        prompt.append("""
            ## 修复要求

            1. 只修复上述列出的问题，不要改动其他内容
            2. 保持原有情节主线不变
            3. 确保修复后与前后文衔接自然
            4. 严格遵守知识库中的世界观和角色设定

            ## 输出格式（必须是XML格式）

            **【XML格式要求】**：
            1. 必须使用XML格式，不能使用JSON格式
            2. 根节点必须是<fixResult>
            3. 字符串内容可以使用中文引号和标点
            4. 使用CDATA包裹内容，避免特殊字符转义问题

            XML结构如下：
            <fixResult>
              <fixedContent><![CDATA[修改后的完整内容]]></fixedContent>
              <fixSummary><![CDATA[修复说明摘要，比如这次修复主要包含了什么内容]]></fixSummary>
              <totalFixes>修复数量</totalFixes>
              <fixReport>
                <fixItem>
                  <type>问题类型</type>
                  <severity>high</severity>
                  <original><![CDATA[修改前的原文片段]]></original>
                  <fixed><![CDATA[修改后的原文片段]]></fixed>
                  <reason><![CDATA[修改的原因，需要具体说明]]></reason>
                </fixItem>
              </fixReport>
            </fixResult>

            **【重要】**：
            - original和fixed必须是实际的文本片段，不是描述
            - 例如：original="他正是赵明", fixed="他正是李长老"
            - 不要返回markdown标记，直接返回XML
            """);

        return prompt.toString();
    }

    /**
     * 构建用于知识库存储的结构化章节文档
     *
     * @param chapter 章节对象
     * @param plotSummary 剧情摘要
     * @return 格式化的 Markdown 文档
     */
    private String buildChapterDocument(Chapter chapter, String plotSummary) {
        StringBuilder document = new StringBuilder();

        // 文档标题
        document.append("# 【第").append(chapter.getChapterNumber())
                .append("章】").append(chapter.getTitle()).append("\n\n");

        // 章节号和标题
        document.append("## 章节信息\n");
        document.append("- **章节号**：").append(chapter.getChapterNumber()).append("\n");
        document.append("- **标题**：").append(chapter.getTitle()).append("\n");
        document.append("- **字数**：").append(chapter.getWordCount()).append("\n");
        document.append("- **状态**：").append(chapter.getStatus()).append("\n\n");

        // 剧情摘要
        if (plotSummary != null && !plotSummary.isEmpty()) {
            document.append("## 剧情摘要\n").append(plotSummary).append("\n\n");
        }

        // 章节内容
        document.append("## 章节内容\n").append(chapter.getContent()).append("\n");

        return document.toString();
    }

    /**
     * 获取前N章的完整内容（用于模板系统）
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节号
     * @param count 获取前几章
     * @return 章节列表
     */
    private List<Chapter> getRecentChapters(Long projectId, Integer currentChapterNumber, int count) {
        if (currentChapterNumber <= 1) {
            return List.of();
        }

        int startChapter = Math.max(1, currentChapterNumber - count);

        LambdaQueryWrapper<Chapter> query = new LambdaQueryWrapper<>();
        query.eq(Chapter::getProjectId, projectId)
              .ge(Chapter::getChapterNumber, startChapter)
              .lt(Chapter::getChapterNumber, currentChapterNumber)
              .orderByAsc(Chapter::getChapterNumber);

        return chapterMapper.selectList(query);
    }

    /**
     * 获取上一章的结束场景（用于连贯性）
     *
     * @param projectId 项目ID
     * @param currentChapterNumber 当前章节号
     * @return 上一章的结束场景，如果不存在返回空字符串
     */
    private String getLastChapterEndingScene(Long projectId, Integer currentChapterNumber) {
        if (currentChapterNumber <= 1) {
            return "";  // 第一章没有上一章
        }

        Integer lastChapterNumber = currentChapterNumber - 1;

        // 查询上一章的剧情记忆
        List<ChapterPlotMemory> memories = plotMemoryMapper.selectRecentMemories(
            projectId, currentChapterNumber, 1
        );

        if (memories.isEmpty()) {
            log.debug("未找到第 {} 章的剧情记忆", lastChapterNumber);
            return "";
        }

        ChapterPlotMemory lastMemory = memories.get(0);
        String endingScene = lastMemory.getChapterEndingScene();

        if (endingScene != null && !endingScene.isEmpty()) {
            log.info("成功获取第 {} 章的结束场景", lastChapterNumber);
            return endingScene;
        }

        return "";
    }

    /**
     * 构建章节生成提示词（使用ChapterContext - 数据库优先策略）
     *
     * @param projectId 项目ID
     * @param chapterPlan 章节规划
     * @param volumePlan 分卷规划
     * @param worldview 世界观设定
     * @param recentChapters 前置章节列表
     * @param chapterContext 章节上下文（包含三层记忆）
     * @return 完整提示词
     */
    private String buildPromptFromTemplate(
            Long projectId,
            NovelChapterPlan chapterPlan,
            NovelVolumePlan volumePlan,
            NovelWorldview worldview,
            List<Chapter> recentChapters,
            ChapterContext chapterContext) {

        // 1. 构建上下文字符串
        String contextText = buildChapterContextText(chapterContext);

        // 2. 获取上一章结束场景
        String lastChapterEndingScene = chapterContext.getLastChapterEndingScene();

        // 3. 使用模板系统构建提示词
        return promptTemplateBuilder.buildChapterPrompt(
            projectId, chapterPlan, volumePlan, worldview, recentChapters, contextText, lastChapterEndingScene
        );
    }

    /**
     * 构建章节上下文字符串（用于模板变量）
     * 基于数据库中的结构化记忆数据
     */
    private String buildChapterContextText(ChapterContext context) {
        StringBuilder section = new StringBuilder();
        section.append("## 【章节上下文 - 用于保持连贯性】\n\n");

        // 1. 短期记忆：前章记忆
        if (context.hasRecentMemories()) {
            section.append("### 前章记忆（短期）\n");
            section.append(context.buildRecentMemoriesText()).append("\n");
        }

        // 2. 待回收伏笔
        if (context.hasPendingForeshadowing()) {
            section.append("### 待回收伏笔\n");
            section.append(foreshadowingService.buildPendingForeshadowingText(context.getPendingForeshadowing()));
            section.append("\n");
        }

        // 3. 上一章结束场景
        if (context.hasLastChapterEndingScene()) {
            section.append("### 上一章结束场景\n");
            section.append(context.getLastChapterEndingScene()).append("\n\n");
        }

        // 4. 中期记忆：分卷主题
        if (context.hasVolumePlan()) {
            NovelVolumePlan volumePlan = context.getVolumePlan();
            section.append("### 当前分卷信息（中期）\n");
            section.append("- 分卷标题：").append(volumePlan.getVolumeTitle()).append("\n");
            if (volumePlan.getVolumeTheme() != null) {
                section.append("- 分卷主题：").append(volumePlan.getVolumeTheme()).append("\n");
            }
            if (volumePlan.getMainConflict() != null) {
                section.append("- 主要冲突：").append(volumePlan.getMainConflict()).append("\n");
            }
            section.append("\n");
        }

        // 5. 本章涉及的角色
        if (context.hasChapterCharacters()) {
            section.append("### 本章涉及角色\n");
            for (NovelCharacter character : context.getChapterCharacters()) {
                section.append("- **").append(character.getName()).append("**");
                if (character.getRole() != null) {
                    section.append("（").append(character.getRole()).append("）");
                }
                section.append("\n");
                if (character.getPersonality() != null) {
                    section.append("  性格：").append(character.getPersonality()).append("\n");
                }
            }
            section.append("\n");
        }

        section.append("**【重要提醒】**：以上内容是从数据库中精确查询得到的，请严格遵守这些设定，确保剧情连贯！\n\n");

        return section.toString();
    }

    /**
     * 从章节记忆同步人物信息到 novel_character 表（新版，支持完整角色信息）
     *
     * @param projectId 项目ID
     * @param chapter   章节
     * @param characterDetailList 角色详情列表（从XML解析得到）
     */
    @Autowired
    private NovelCharacterChapterService novelCharacterChapterService;

    private void syncCharactersFromMemory(Long projectId, Chapter chapter,
                                           List<ChapterMemoryXmlDto.CharacterDetailDto> characterDetailList) {
        log.info("开始同步人物信息，projectId={}, chapterId={}, characterDetailList size={}",
                projectId, chapter.getId(), characterDetailList == null ? "null" : characterDetailList.size());

        if (characterDetailList == null || characterDetailList.isEmpty()) {
            log.warn("角色详情列表为空，跳过人物同步，chapterId={}", chapter.getId());
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (ChapterMemoryXmlDto.CharacterDetailDto characterDetail : characterDetailList) {
            String characterName = characterDetail.getName();
            log.info("处理角色：name={}, roleType={}, gender={}, age={}, status={}",
                    characterName, characterDetail.getRoleType(), characterDetail.getGender(),
                    characterDetail.getAge(), characterDetail.getStatus());

            // 跳过空名称或第一人称代词
            if (characterName == null || characterName.trim().isEmpty()) {
                log.debug("跳过空名称角色");
                continue;
            }
            if ("我".equals(characterName) || "主角".equals(characterName)) {
                log.debug("跳过第一人称代词角色：{}", characterName);
                continue;
            }

            try {
                // 查询人物是否已存在
                NovelCharacter existingCharacter = novelCharacterMapper.selectOne(
                    new LambdaQueryWrapper<NovelCharacter>()
                        .eq(NovelCharacter::getProjectId, projectId)
                        .eq(NovelCharacter::getName, characterName)
                );

                if (existingCharacter != null) {
                    // 人物已存在，更新信息
                    log.info("人物已存在，更新信息：name={}, characterId={}", characterName, existingCharacter.getId());
                    updateExistingCharacter(existingCharacter, characterDetail, chapter);
                    novelCharacterMapper.updateById(existingCharacter);

                    // 创建角色-章节关联（非首次出现）
                    log.info("创建角色-章节关联（非首次出现）：characterId={}, chapterId={}",
                            existingCharacter.getId(), chapter.getId());
                    novelCharacterChapterService.saveCharacterChapterRelation(
                            existingCharacter.getId(), chapter, characterDetail, false);
                    successCount++;
                } else {
                    // 人物不存在，创建新人物
                    log.info("人物不存在，创建新人物：name={}", characterName);
                    NovelCharacter newCharacter = createNewCharacter(projectId, characterDetail, chapter);
                    novelCharacterMapper.insert(newCharacter);
                    log.info("新人物创建成功，name={}, characterId={}, roleType={}, 首次出现在chapterId={}",
                            characterName, newCharacter.getId(), newCharacter.getRoleType(), chapter.getId());

                    // 创建角色-章节关联（首次出现）
                    log.info("创建角色-章节关联（首次出现）：characterId={}, chapterId={}",
                            newCharacter.getId(), chapter.getId());
                    novelCharacterChapterService.saveCharacterChapterRelation(
                            newCharacter.getId(), chapter, characterDetail, true);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("处理角色失败，跳过该角色继续处理其他角色。角色名：{}，chapterId={}，错误信息：{}",
                        characterName, chapter.getId(), e.getMessage(), e);
            }
        }

        log.info("人物同步完成，chapterId={}，成功：{}，失败：{}", chapter.getId(), successCount, failCount);
    }

    /**
     * 更新已存在的角色信息
     *
     * @param existingCharacter 已存在的角色
     * @param characterDetail   角色详情（从XML解析）
     * @param chapter           当前章节
     */
    private void updateExistingCharacter(NovelCharacter existingCharacter,
                                          ChapterMemoryXmlDto.CharacterDetailDto characterDetail,
                                          Chapter chapter) {
        // 追加状态信息到 background 字段
        String existingBackground = existingCharacter.getBackground();
        String newBackground = String.format("第%d章状态：%s",
            chapter.getChapterNumber(), characterDetail.getStatus());

        if (existingBackground == null || existingBackground.isEmpty()) {
            existingCharacter.setBackground(newBackground);
        } else {
            existingCharacter.setBackground(existingBackground + "\n" + newBackground);
        }

        // 如果原角色类型为空或为默认值supporting，且AI给出了更明确的类型，则更新
        String newRoleType = determineRoleType(characterDetail.getRoleType());
        if (newRoleType != null &&
            (existingCharacter.getRoleType() == null || "supporting".equals(existingCharacter.getRoleType()))) {
            existingCharacter.setRoleType(newRoleType);
            log.debug("更新角色类型，name={}, oldType={}, newType={}",
                    existingCharacter.getName(), existingCharacter.getRoleType(), newRoleType);
        }

        // 如果原字段为空，则填充新信息
        if (existingCharacter.getGender() == null && characterDetail.getGender() != null) {
            existingCharacter.setGender(normalizeGender(characterDetail.getGender()));
        }
        if (existingCharacter.getAge() == null && characterDetail.getAge() != null) {
            existingCharacter.setAge(characterDetail.getAge());
        }
        if (existingCharacter.getPersonality() == null && characterDetail.getPersonality() != null) {
            existingCharacter.setPersonality(characterDetail.getPersonality());
        }
        if (existingCharacter.getAppearance() == null && characterDetail.getAppearance() != null) {
            existingCharacter.setAppearance(characterDetail.getAppearance());
        }

        existingCharacter.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 创建新角色
     *
     * @param projectId       项目ID
     * @param characterDetail 角色详情（从XML解析）
     * @param chapter         当前章节
     * @return 新创建的角色实体
     */
    private NovelCharacter createNewCharacter(Long projectId,
                                              ChapterMemoryXmlDto.CharacterDetailDto characterDetail,
                                              Chapter chapter) {
        NovelCharacter newCharacter = new NovelCharacter();
        newCharacter.setProjectId(projectId);
        newCharacter.setName(characterDetail.getName());

        // 设置角色类型（智能判断）
        newCharacter.setRoleType(determineRoleType(characterDetail.getRoleType()));

        // 设置性别
        newCharacter.setGender(normalizeGender(characterDetail.getGender()));

        // 设置年龄（直接使用字符串)
        newCharacter.setAge(characterDetail.getAge());

        // 设置性格特点
        newCharacter.setPersonality(characterDetail.getPersonality());

        // 设置外貌描述
        newCharacter.setAppearance(characterDetail.getAppearance());

        // 设置背景信息
        newCharacter.setBackground(String.format("第%d章首次出现，状态：%s",
            chapter.getChapterNumber(), characterDetail.getStatus()));

        // 设置时间戳
        newCharacter.setCreateTime(LocalDateTime.now());
        newCharacter.setUpdateTime(LocalDateTime.now());

        return newCharacter;
    }

    /**
     * 确定角色类型
     * 优先使用AI返回的类型，如果无效则默认为supporting
     *
     * @param roleType AI返回的角色类型
     * @return 标准化的角色类型
     */
    private String determineRoleType(String roleType) {
        if (roleType == null || roleType.trim().isEmpty()) {
            return "supporting";
        }

        // 标准化角色类型
        String normalized = roleType.toLowerCase().trim();
        return switch (normalized) {
            case "protagonist", "主角", "main" -> "protagonist";
            case "antagonist", "反派", "villain" -> "antagonist";
            case "npc", "路人", "minor" -> "npc";
            default -> "supporting";
        };
    }

    /**
     * 标准化性别字段
     *
     * @param gender AI返回的性别
     * @return 标准化的性别
     */
    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return null;
        }

        String normalized = gender.toLowerCase().trim();
        return switch (normalized) {
            case "male", "男", "男性" -> "male";
            case "female", "女", "女性" -> "female";
            default -> "other";
        };
    }

    /**
     * 解析年龄字符串为整数
     *
     * @param ageStr 年龄字符串
     * @return 年龄整数，解析失败返回null
     */
    private Integer parseAge(String ageStr) {
        if (ageStr == null || ageStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试直接解析数字
            return Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            // 尝试提取数字部分
            String digits = ageStr.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
            return null;
        }
    }
}
