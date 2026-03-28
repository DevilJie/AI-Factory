package com.aifactory.dto;

import com.aifactory.enums.AIRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * AI通用生成请求DTO
 *
 * @Author CaiZy
 * @Date 2025-01-22
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Schema(description = "AI通用生成请求参数，用于调用AI生成文本内容")
@Data
public class AIGenerateRequest {

    /**
     * 交互流水号（用于日志追踪）
     */
    @Schema(description = "交互流水号，用于日志追踪和关联。格式：trace_yyyyMMdd_UUID",
            example = "trace_20250203_abc123def",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String traceId;

    /**
     * 项目ID（用于日志关联）
     */
    @Schema(description = "项目ID，用于将AI请求与具体项目关联，便于日志查询和统计",
            example = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Long projectId;

    /**
     * 分卷计划ID（用于日志关联）
     */
    @Schema(description = "分卷计划ID，用于关联具体的分卷规划",
            example = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Long volumePlanId;

    /**
     * 章节规划ID（用于日志关联）
     */
    @Schema(description = "章节规划ID，用于关联具体的章节规划",
            example = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Long chapterPlanId;

    /**
     * 章节ID（用于日志关联） - 用于AI生成完成后保存
     */
    @Schema(description = "章节ID，用于AI生成完成后自动保存到对应章节",
            example = "1",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Long chapterId;

    /**
     * 请求类型（用于日志分类）
     * 例如：chapter_generate、outline_generate、worldview_generate、character_fix等
     */
    @Schema(description = "请求类型，用于日志分类和统计。" +
                          "常用类型：chapter_generate(章节生成)、outline_generate(大纲生成)、" +
                          "worldview_generate(世界观生成)、character_fix(角色修复)等",
            example = "chapter_generate",
            allowableValues = {"chapter_generate", "outline_generate", "worldview_generate",
                              "character_fix", "llm_project_name_generate", "quick_generate"},
            accessMode = Schema.AccessMode.READ_WRITE)
    private String requestType;

    /**
     * AI角色
     */
    @Schema(description = "AI角色，决定使用的系统提示词和生成风格。" +
                          "NOVEL_WRITER-小说作家, WORLDVIEW_BUILDER-世界观构建师, " +
                          "CHARACTER_DESIGNER-角色设计师, OUTLINE_PLANNER-大纲规划师, " +
                          "CHAPTER_WRITER-章节写手, FORESHADOWING_MANAGER-伏笔管理师, " +
                          "STORYBOARD_CREATOR-分镜创作者",
            example = "NOVEL_WRITER",
            accessMode = Schema.AccessMode.READ_WRITE)
    private AIRole role;

    /**
     * 用户输入的任务描述
     */
    @Schema(description = "用户输入的任务描述，描述需要AI完成的具体任务内容。" +
                          "支持使用提示词模板变量，如${idea}、${genre}等",
            example = "请根据以下设定生成第一章的正文内容：主角是一个普通少年...",
            maxLength = 2000,
            required = true,
            accessMode = Schema.AccessMode.READ_WRITE)
    @NotBlank(message = "任务描述不能为空")
    @Size(max = 2000, message = "任务描述不能超过2000个字符")
    private String task;

    /**
     * 提示词模板参数（可选）
     * 用于替换提示词模板中的占位符
     */
    @Schema(description = "提示词模板参数，用于替换提示词模板中的占位符变量。" +
                          "如：{\"idea\": \"修仙小说\", \"genre\": \"玄幻\"}",
            example = "{\"idea\": \"一个少年获得上古传承的故事\", \"genre\": \"玄幻\", \"tone\": \"热血\"}",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Map<String, Object> templateParams;

    /**
     * 自定义系统提示词（可选）
     * 如果提供，将覆盖角色默认的系统提示词
     */
    @Schema(description = "自定义系统提示词，如果提供将覆盖角色默认的系统提示词。" +
                          "适用于需要特殊定制的生成场景",
            example = "你是一个专业的玄幻小说作家，擅长描写战斗场景...",
            accessMode = Schema.AccessMode.READ_WRITE)
    private String customSystemPrompt;

    /**
     * 温度参数（可选）
     * 控制生成内容的随机性，范围0-1，默认0.7
     */
    @Schema(description = "温度参数，控制生成内容的随机性和创造性。" +
                          "范围0-1，值越低生成越稳定一致，值越高生成越随机创新。" +
                          "推荐值：0.7（平衡），0.3（稳定），0.9（创新）",
            example = "0.7",
            minimum = "0",
            maximum = "1",
            defaultValue = "0.7",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Double temperature;

    /**
     * 最大Token数（可选）
     * 默认2000
     */
    @Schema(description = "最大生成Token数，控制生成内容的长度。" +
                          "建议根据任务类型设置：章节生成建议4000-8000，大纲生成建议2000-4000，" +
                          "简单对话建议500-1000",
            example = "4000",
            minimum = "100",
            maximum = "32000",
            defaultValue = "2000",
            accessMode = Schema.AccessMode.READ_WRITE)
    private Integer maxTokens;
}
