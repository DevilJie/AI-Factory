package com.aifactory.service.prompt;

import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.ProjectBasicSettings;
import com.aifactory.mapper.NovelWorldviewMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.FactionService;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.ProjectBasicSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公共提示词上下文构建服务
 * 提取基础设置、世界观等公共提示词构建逻辑为可复用的服务
 *
 * @Author AI Factory
 * @Date 2026-03-28
 */
@Slf4j
@Service
public class PromptContextBuilder {

    @Autowired
    private ProjectBasicSettingsService projectBasicSettingsService;

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Autowired
    private FactionService factionService;

    /**
     * 构建基础设置上下文（叙事结构、写作视角、叙事节奏等）
     *
     * @param projectId 项目ID
     * @return 格式化的基础设置字符串
     */
    public String buildBasicSettingsContext(Long projectId) {
        try {
            ProjectBasicSettings settings = projectBasicSettingsService.getByProjectId(projectId);
            if (settings == null) {
                return "暂无特殊设置";
            }

            StringBuilder sb = new StringBuilder();

            // 叙事结构
            String narrativeStructure = BasicSettingsDictionary.getNarrativeStructure(settings.getNarrativeStructure());
            if (narrativeStructure != null && !narrativeStructure.isEmpty()) {
                sb.append("- 叙事结构：").append(narrativeStructure).append("\n");
            }

            // 写作视角
            String writingPerspective = BasicSettingsDictionary.getWritingPerspective(settings.getWritingPerspective());
            if (writingPerspective != null && !writingPerspective.isEmpty()) {
                sb.append("- 写作视角：").append(writingPerspective).append("\n");
            }

            // 叙事节奏
            String narrativePace = BasicSettingsDictionary.getNarrativePace(settings.getNarrativePace());
            if (narrativePace != null && !narrativePace.isEmpty()) {
                sb.append("- 叙事节奏：").append(narrativePace).append("\n");
            }

            // 语言风格
            String languageStyle = BasicSettingsDictionary.getLanguageStyle(settings.getLanguageStyle());
            if (languageStyle != null && !languageStyle.isEmpty()) {
                sb.append("- 语言风格：").append(languageStyle).append("\n");
            }

            // 描写重点
            if (settings.getDescriptionFocus() != null && !settings.getDescriptionFocus().isEmpty()) {
                try {
                    List<String> focusList = objectMapper.readValue(
                        settings.getDescriptionFocus(),
                        new TypeReference<List<String>>() {}
                    );
                    List<String> focusDescriptions = BasicSettingsDictionary.getDescriptionFocusList(focusList);
                    sb.append("- 描写重点：").append(String.join("、", focusDescriptions)).append("\n");
                } catch (Exception e) {
                    log.warn("解析描写重点失败: {}", e.getMessage());
                }
            }

            // 写作风格
            String writingStyle = BasicSettingsDictionary.getWritingStyle(settings.getWritingStyle());
            if (writingStyle != null && !writingStyle.isEmpty()) {
                sb.append("- 写作风格：").append(writingStyle).append("\n");
            }

            return sb.length() > 0 ? sb.toString() : "暂无特殊设置";
        } catch (Exception e) {
            log.error("构建基础设置上下文失败, 项目ID: {}: {}", projectId, e.getMessage(), e);
            return "暂无特殊设置";
        }
    }

    /**
     * 构建结局设置上下文（结局类型、结局基调）
     *
     * @param projectId 项目ID
     * @return 格式化的结局设置字符串
     */
    public String buildEndingSettingsContext(Long projectId) {
        try {
            ProjectBasicSettings settings = projectBasicSettingsService.getByProjectId(projectId);
            if (settings == null) {
                return "暂无结局规划";
            }

            StringBuilder sb = new StringBuilder();

            // 结局类型
            String endingType = BasicSettingsDictionary.getEndingType(settings.getEndingType());
            if (endingType != null && !endingType.isEmpty()) {
                sb.append("- 结局类型：").append(endingType).append("\n");
            }

            // 结局基调
            String endingTone = BasicSettingsDictionary.getEndingTone(settings.getEndingTone());
            if (endingTone != null && !endingTone.isEmpty()) {
                sb.append("- 结局基调：").append(endingTone).append("\n");
            }

            return sb.length() > 0 ? sb.toString() : "暂无结局规划";
        } catch (Exception e) {
            log.error("构建结局设置上下文失败, 项目ID: {}: {}", projectId, e.getMessage(), e);
            return "暂无结局规划";
        }
    }

    /**
     * 构建世界观上下文
     *
     * @param projectId 项目ID
     * @return 格式化的世界观设定字符串
     */
    public String buildWorldviewContext(Long projectId) {
        try {
            NovelWorldview worldview = getWorldview(projectId);
            if (worldview == null) {
                return "暂无世界观设定";
            }

            StringBuilder sb = new StringBuilder();

            // 世界类型（与原始代码一致，worldType 总是附加）
            if (worldview.getWorldType() != null && !worldview.getWorldType().isEmpty()) {
                sb.append("- 世界类型：").append(worldview.getWorldType()).append("\n");
            }

            if (worldview.getWorldBackground() != null && !worldview.getWorldBackground().isEmpty()) {
                sb.append("- 世界背景：").append(worldview.getWorldBackground()).append("\n");
            }

            String powerConstraint = powerSystemService.buildPowerSystemConstraint(worldview.getProjectId());
            if (powerConstraint != null && !powerConstraint.trim().isEmpty()) {
                sb.append("- 力量体系：").append(powerConstraint).append("\n");
            }

            if (worldview.getGeography() != null && !worldview.getGeography().isEmpty()) {
                sb.append("- 地理环境：").append(worldview.getGeography()).append("\n");
            }

            if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
                sb.append("- 势力分布：").append(worldview.getForces()).append("\n");
            }

            if (worldview.getTimeline() != null && !worldview.getTimeline().isEmpty()) {
                sb.append("- 时间线：").append(worldview.getTimeline()).append("\n");
            }

            if (worldview.getRules() != null && !worldview.getRules().isEmpty()) {
                sb.append("- 世界规则：").append(worldview.getRules()).append("\n");
            }

            return sb.length() > 0 ? sb.toString() : "暂无世界观设定";
        } catch (Exception e) {
            log.error("构建世界观上下文失败, 项目ID: {}: {}", projectId, e.getMessage(), e);
            return "暂无世界观设定";
        }
    }

    /**
     * 获取世界观实体
     *
     * @param projectId 项目ID
     * @return 世界观设定实体，可能为null
     */
    public NovelWorldview getWorldview(Long projectId) {
        try {
            LambdaQueryWrapper<NovelWorldview> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NovelWorldview::getProjectId, projectId);
            NovelWorldview worldview = worldviewMapper.selectOne(queryWrapper);
            if (worldview != null) {
                continentRegionService.fillGeography(worldview);
                factionService.fillForces(worldview);
            }
            return worldview;
        } catch (Exception e) {
            log.error("查询世界观设定失败, 项目ID: {}: {}", projectId, e.getMessage(), e);
            return null;
        }
    }
}
