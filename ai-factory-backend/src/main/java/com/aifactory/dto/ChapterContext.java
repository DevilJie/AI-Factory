package com.aifactory.dto;

import com.aifactory.entity.ChapterPlotMemory;
import com.aifactory.entity.NovelCharacter;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.entity.NovelWorldview;
import com.aifactory.entity.ProjectBasicSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节生成上下文
 * 统一管理章节生成所需的三层记忆模型：
 * - 短期记忆：前几章的结构化记忆（影响当前章节写作风格和情节承接）
 * - 中期记忆：当前分卷的关键伏笔和角色发展弧线
 * - 长期记忆：世界观设定、人物基础信息（整个项目周期）
 *
 * @Author AI Factory
 * @Date 2026-02-27
 */
@Data
@Schema(description = "章节生成上下文对象，统一管理AI生成章节所需的三层记忆模型数据")
public class ChapterContext {

    // ==================== 短期记忆 ====================

    /**
     * 前3章的结构化记忆
     * 用于保持剧情连贯性和写作风格一致性
     */
    @Schema(description = "短期记忆：前3章的结构化记忆列表，用于保持剧情连贯性和写作风格一致性")
    private List<ChapterPlotMemory> recentMemories = new ArrayList<>();

    /**
     * 待回收伏笔列表
     * 累积前面所有章节的待回收伏笔，排除已回收的
     */
    @Schema(description = "短期记忆：待回收伏笔列表，累积前面所有章节未回收的伏笔",
            example = "[\"主角身世之谜\", \"第一卷提到的神秘老者\"]")
    private List<String> pendingForeshadowing = new ArrayList<>();

    /**
     * 上一章结束场景
     * 用于当前章节开头的场景承接
     */
    @Schema(description = "短期记忆：上一章结束场景，用于当前章节开头的场景承接",
            example = "李云站在城门前，望着远处的灯火，心中充满期待...")
    private String lastChapterEndingScene;

    // ==================== 中期记忆 ====================

    /**
     * 当前分卷信息
     * 包含分卷主题、主要冲突、情节走向等
     */
    @Schema(description = "中期记忆：当前分卷信息，包含分卷主题、主要冲突、情节走向等")
    private NovelVolumePlan volumePlan;

    /**
     * 当前分卷的角色发展弧线
     */
    @Schema(description = "中期记忆：当前分卷的角色发展弧线，描述角色在本卷中的成长轨迹",
            example = "主角从初入江湖的懵懂少年，经历挫折后逐渐成长")
    private String characterArc;

    // ==================== 长期记忆 ====================

    /**
     * 世界观设定
     * 包含世界类型、力量体系、地理环境、势力分布等
     */
    @Schema(description = "长期记忆：世界观设定，包含世界类型、力量体系、地理环境、势力分布等")
    private NovelWorldview worldview;

    /**
     * 项目基础设置
     * 包含叙事结构、写作风格、叙事视角等
     */
    @Schema(description = "长期记忆：项目基础设置，包含叙事结构、写作风格、叙事视角等")
    private ProjectBasicSettings basicSettings;

    /**
     * 本章涉及的角色列表
     * 从章节规划中提取或在生成时动态确定
     */
    @Schema(description = "长期记忆：本章涉及的角色列表，从章节规划中提取或在生成时动态确定")
    private List<NovelCharacter> chapterCharacters = new ArrayList<>();

    // ==================== 辅助方法 ====================

    /**
     * 检查是否有短期记忆
     */
    @Schema(description = "检查是否有短期记忆数据", example = "true")
    public boolean hasRecentMemories() {
        return recentMemories != null && !recentMemories.isEmpty();
    }

    /**
     * 检查是否有待回收伏笔
     */
    @Schema(description = "检查是否有待回收伏笔", example = "true")
    public boolean hasPendingForeshadowing() {
        return pendingForeshadowing != null && !pendingForeshadowing.isEmpty();
    }

    /**
     * 检查是否有上一章结束场景
     */
    @Schema(description = "检查是否有上一章结束场景", example = "true")
    public boolean hasLastChapterEndingScene() {
        return lastChapterEndingScene != null && !lastChapterEndingScene.isEmpty();
    }

    /**
     * 检查是否有分卷信息
     */
    @Schema(description = "检查是否有分卷信息", example = "true")
    public boolean hasVolumePlan() {
        return volumePlan != null;
    }

    /**
     * 检查是否有世界观设定
     */
    @Schema(description = "检查是否有世界观设定", example = "true")
    public boolean hasWorldview() {
        return worldview != null;
    }

    /**
     * 检查是否有基础设置
     */
    @Schema(description = "检查是否有基础设置", example = "true")
    public boolean hasBasicSettings() {
        return basicSettings != null;
    }

    /**
     * 检查是否有章节角色
     */
    @Schema(description = "检查是否有章节角色", example = "true")
    public boolean hasChapterCharacters() {
        return chapterCharacters != null && !chapterCharacters.isEmpty();
    }

    /**
     * 获取最近一章的记忆（用于获取结束场景等）
     */
    @Schema(description = "获取最近一章的记忆，用于获取结束场景等信息")
    public ChapterPlotMemory getLatestMemory() {
        if (hasRecentMemories()) {
            return recentMemories.get(0);
        }
        return null;
    }

    /**
     * 构建待回收伏笔的文本描述
     */
    @Schema(description = "构建待回收伏笔的文本描述，用于AI提示词",
            example = "### 待回收伏笔\n1. 主角身世之谜\n2. 第一卷提到的神秘老者\n")
    public String buildPendingForeshadowingText() {
        if (!hasPendingForeshadowing()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 待回收伏笔\n");
        for (int i = 0; i < pendingForeshadowing.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, pendingForeshadowing.get(i)));
        }
        return sb.toString();
    }

    /**
     * 构建近期记忆的文本描述
     */
    @Schema(description = "构建近期记忆的文本描述，用于AI提示词")
    public String buildRecentMemoriesText() {
        if (!hasRecentMemories()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 前章记忆\n\n");

        for (ChapterPlotMemory memory : recentMemories) {
            sb.append(String.format("**第%d章 %s**\n",
                    memory.getChapterNumber(),
                    memory.getChapterTitle() != null ? memory.getChapterTitle() : ""));

            if (memory.getChapterSummary() != null && !memory.getChapterSummary().isEmpty()) {
                sb.append("总结：").append(memory.getChapterSummary()).append("\n");
            }

            if (memory.getChapterEndingScene() != null && !memory.getChapterEndingScene().isEmpty()) {
                sb.append("结尾场景：").append(memory.getChapterEndingScene()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
