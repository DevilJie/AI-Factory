package com.aifactory.service.task.impl;

import com.aifactory.constants.BasicSettingsDictionary;
import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.entity.NovelContinentRegion;
import com.aifactory.entity.Project;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.ProjectMapper;
import com.aifactory.service.ContinentRegionService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

/**
 * 地理环境独立生成任务策略
 * <p>
 * 三步流程：
 * 1. 清理旧地理环境数据 — 删除项目下所有 continent_region
 * 2. AI生成地理环境 — 调用 llm_geography_create 独立提示词模板
 * 3. 保存地理环境 — DOM 解析 <g>/<r> XML 标签，调用 ContinentRegionService.saveTree 入库
 * <p>
 * 设计要点：
 * - 使用 DOM 而非 Jackson XML 解析，因为嵌套同名 <r> 标签 Jackson 无法正确处理
 * - 解析逻辑从 WorldviewTaskStrategy.saveGeographyRegionsFromXml 复制（Phase 8 统一重构）
 * - @Component 注解确保 AsyncTaskExecutor 的 strategyMap 自动注册
 *
 * @Author AI Factory
 * @Date 2026-04-03
 */
@Slf4j
@Component
public class GeographyTaskStrategy implements TaskStrategy {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ContinentRegionService continentRegionService;

    @Override
    public String getTaskType() {
        return "geography";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        List<StepConfig> steps = new ArrayList<>();
        steps.add(new StepConfig(1, "清理旧地理环境数据", "clean_geography", new HashMap<>()));
        steps.add(new StepConfig(2, "AI生成地理环境", "generate_geography", new HashMap<>()));
        steps.add(new StepConfig(3, "保存地理环境", "save_geography", new HashMap<>()));
        return steps;
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        String stepType = step.getStepType();
        switch (stepType) {
            case "clean_geography":
                return cleanGeography(step, context);
            case "generate_geography":
                return generateGeography(step, context);
            case "save_geography":
                return saveGeography(step, context);
            default:
                return StepResult.failure("未知的步骤类型: " + stepType);
        }
    }

    // ======================== clean_geography ========================

    /**
     * 清理旧地理环境数据 — 删除项目下所有 continent_region 记录
     */
    private StepResult cleanGeography(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            continentRegionService.deleteByProjectId(projectId);
            log.info("清理旧地理环境数据完成, projectId={}", projectId);
            return StepResult.success(Map.of("projectId", projectId), 100);
        } catch (Exception e) {
            log.error("清理旧地理环境数据失败", e);
            return StepResult.failure("清理旧地理环境数据失败: " + e.getMessage());
        }
    }

    // ======================== generate_geography ========================

    /**
     * AI生成地理环境 — 加载项目信息，构建模板变量，调用 LLM
     */
    private StepResult generateGeography(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                return StepResult.failure("项目不存在, projectId=" + projectId);
            }

            JsonNode config = context.getConfig();
            String projectDescription = getTextOrDefault(config, "projectDescription", project.getDescription(), "待补充");
            String storyTone = getTextOrDefault(config, "storyTone", project.getStoryTone(), null);
            String storyGenre = getTextOrDefault(config, "storyGenre", project.getNovelType(), null);
            String tags = config != null && config.has("tags") ? config.get("tags").asText() : project.getTags();

            Map<String, Object> variables = new HashMap<>();
            variables.put("projectDescription",
                projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
            variables.put("storyTone",
                storyTone != null && !storyTone.isEmpty() ? BasicSettingsDictionary.getStoryTone(storyTone) : "待补充");
            variables.put("storyGenre",
                storyGenre != null && !storyGenre.isEmpty() ? BasicSettingsDictionary.getNovelType(storyGenre) : "待补充");
            variables.put("tagsSection",
                tags != null && !tags.isEmpty() ? "【标签】" + tags : "");

            String prompt = promptTemplateService.executeTemplate("llm_geography_create", variables);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(projectId);
            aiRequest.setRequestType("llm_geography_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info("AI生成地理环境完成, projectId={}, 响应长度={}", projectId, responseContent.length());

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("AI生成地理环境失败", e);
            return StepResult.failure("AI生成地理环境失败: " + e.getMessage());
        }
    }

    // ======================== save_geography ========================

    /**
     * 保存地理环境 — DOM 解析 AI 响应中的 <g> 标签，调用 ContinentRegionService.saveTree 入库
     */
    private StepResult saveGeography(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的地理环境内容");
            }

            saveGeographyRegionsFromXml(projectId, aiResponse);
            return StepResult.success(Map.of("projectId", projectId), 100);

        } catch (Exception e) {
            log.error("保存地理环境失败", e);
            return StepResult.failure("保存地理环境失败: " + e.getMessage());
        }
    }

    // ======================== DOM 解析方法（从 WorldviewTaskStrategy 复制） ========================

    /**
     * 从 AI 响应 XML 中手动 DOM 解析 <g> 地理区域并保存
     * <p>
     * 使用 DOM 而非 Jackson XML 是因为嵌套同名 <r> 标签 Jackson 无法正确处理
     */
    private void saveGeographyRegionsFromXml(Long projectId, String aiResponse) {
        try {
            // 提取 <g>...</g> 片段
            int start = aiResponse.indexOf("<g>");
            int end = aiResponse.indexOf("</g>");
            if (start < 0 || end < 0) {
                log.info("未找到 <g> 地理区域标签，跳过入库");
                return;
            }
            String geographyXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(geographyXml)));

            Element root = doc.getDocumentElement();
            NodeList gNodes = root.getElementsByTagName("g");
            if (gNodes.getLength() == 0) {
                log.info("<g> 标签内无内容，跳过入库");
                return;
            }

            Element gElement = (Element) gNodes.item(0);
            List<NovelContinentRegion> rootNodes = parseRegionNodes(gElement, projectId);

            if (rootNodes.isEmpty()) {
                log.info("地理区域解析结果为空，跳过入库");
                return;
            }

            continentRegionService.saveTree(projectId, rootNodes);
            log.info("地理区域入库完成, projectId={}, 根节点数={}", projectId, rootNodes.size());

        } catch (Exception e) {
            log.error("保存地理区域失败, projectId={}", projectId, e);
        }
    }

    /**
     * 递归解析 <r> 节点为 NovelContinentRegion 列表
     * <p>
     * XML格式：<r><n>区域名称</n><d><![CDATA[描述]]></d><r>子区域</r></r>
     */
    private List<NovelContinentRegion> parseRegionNodes(Element parent, Long projectId) {
        List<NovelContinentRegion> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "r".equals(node.getNodeName())) {
                result.add(parseSingleRegion((Element) node, projectId));
            }
        }
        return result;
    }

    /**
     * 解析单个 <r> 区域节点：读取 <n>（名称）和 <d>（描述），递归解析直接子 <r>
     */
    private NovelContinentRegion parseSingleRegion(Element rElement, Long projectId) {
        NovelContinentRegion region = new NovelContinentRegion();
        region.setProjectId(projectId);

        // 从 <n> 子标签读取名称（兼容旧格式 name 属性）
        NodeList nNodes = rElement.getElementsByTagName("n");
        if (nNodes.getLength() > 0) {
            region.setName(nNodes.item(0).getTextContent().trim());
        } else {
            region.setName(rElement.getAttribute("name"));
        }

        // 从 <d> 子标签读取描述
        NodeList dNodes = rElement.getElementsByTagName("d");
        if (dNodes.getLength() > 0) {
            region.setDescription(dNodes.item(0).getTextContent().trim());
        }

        // 递归解析直接子 <r> 节点（避免误取孙子节点）
        List<NovelContinentRegion> childRegions = new ArrayList<>();
        NodeList children = rElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "r".equals(child.getNodeName())) {
                childRegions.add(parseSingleRegion((Element) child, projectId));
            }
        }
        if (!childRegions.isEmpty()) {
            region.setChildren(childRegions);
        }

        return region;
    }

    // ======================== 工具方法 ========================

    /**
     * 从 config 中读取文本值，如果不存在则使用 fallback
     */
    private String getTextOrDefault(JsonNode config, String key, String primaryFallback, String secondaryFallback) {
        if (config != null && config.has(key)) {
            String value = config.get(key).asText();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        if (primaryFallback != null && !primaryFallback.isEmpty()) {
            return primaryFallback;
        }
        return secondaryFallback;
    }
}
