package com.aifactory.service.task.impl;

import com.aifactory.dto.AIGenerateRequest;
import com.aifactory.dto.AIGenerateResponse;
import com.aifactory.dto.PowerSystemLevelSaveRequest;
import com.aifactory.dto.PowerSystemLevelStepSaveRequest;
import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.*;
import com.aifactory.enums.AIRole;
import com.aifactory.mapper.*;
import com.aifactory.service.PowerSystemService;
import com.aifactory.service.llm.LLMProviderFactory;
import com.aifactory.service.prompt.PromptTemplateService;
import com.aifactory.service.task.TaskStrategy;
import com.aifactory.util.XmlParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 世界观生成任务策略
 * <p>
 * 改造要点：
 * 1. 提示词模板化 — 所有 prompt 内容由 DB 模板驱动，不再硬编码 XML 格式要求
 * 2. XML 解析力量体系入库 — AI 返回的 <p><system>...</system></p> 会被解析并写入
 * novel_power_system / novel_power_system_level / novel_power_system_level_step 三张表
 * 3. 关联表 novel_worldview_power_system 在保存时自动建立
 * 4. 移除 novel_worldview.powerSystem 文本字段的一切读写
 *
 * @Author CaiZy
 * @Date 2025-02-02
 */
@Slf4j
@Component
public class WorldviewTaskStrategy implements TaskStrategy {

    @Autowired
    private NovelWorldviewMapper worldviewMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PowerSystemService powerSystemService;

    @Autowired
    private NovelPowerSystemMapper powerSystemMapper;

    @Autowired
    private NovelPowerSystemLevelMapper levelMapper;

    @Autowired
    private NovelPowerSystemLevelStepMapper stepMapper;

    @Autowired
    private NovelWorldviewPowerSystemMapper worldviewPowerSystemMapper;

    @Override
    public String getTaskType() {
        return "worldview";
    }

    @Override
    public List<StepConfig> createSteps(AiTask task) {
        try {
            List<StepConfig> steps = new ArrayList<>();

            // 步骤: 检查是否已有世界观设定（删除旧数据 + 关联）
            steps.add(new StepConfig(1, "检查现有世界观", "check_existing", new HashMap<>()));

            // 步骤: 生成世界观设定
            steps.add(new StepConfig(2, "生成世界观设定", "generate_worldview", new HashMap<>()));

            // 步骤: 保存世界观设定
            steps.add(new StepConfig(3, "保存世界观设定", "save_worldview", new HashMap<>()));

            log.info("创建世界观任务步骤完成，项目ID: {}", task.getProjectId());
            return steps;

        } catch (Exception e) {
            log.error("创建世界观任务步骤失败", e);
            throw new RuntimeException("创建世界观任务步骤失败: " + e.getMessage());
        }
    }

    @Override
    public StepResult executeStep(AiTaskStep step, TaskContext context) {
        try {
            String stepType = step.getStepType();

            switch (stepType) {
                case "check_existing":
                    return checkExisting(step, context);
                case "generate_worldview":
                    return generateWorldview(step, context);
                case "save_worldview":
                    return saveWorldview(step, context);
                default:
                    return StepResult.failure("未知的步骤类型: " + stepType);
            }

        } catch (Exception e) {
            log.error("执行步骤失败: {}", step.getStepName(), e);
            return StepResult.failure("执行步骤失败: " + e.getMessage());
        }
    }

    // ======================== check_existing ========================

    /**
     * 检查是否已有世界观设定，如有则删除旧世界观 + 关联的力量体系行
     */
    private StepResult checkExisting(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();
            log.info("检查项目 {} 是否已有世界观设定", projectId);

            NovelWorldview existingWorldview = worldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>()
                    .eq(NovelWorldview::getProjectId, projectId)
            );

            boolean hadExisting = existingWorldview != null;

            if (hadExisting) {
                log.info("项目 {} 已有世界观设定，ID={}，将删除并重新生成", projectId, existingWorldview.getId());

                // 1. 删除关联表行
                worldviewPowerSystemMapper.delete(
                    new LambdaQueryWrapper<NovelWorldviewPowerSystem>()
                        .eq(NovelWorldviewPowerSystem::getWorldviewId, existingWorldview.getId())
                );

                // 2. 删除旧世界观
                worldviewMapper.deleteById(existingWorldview.getId());
            } else {
                log.info("项目 {} 没有世界观设定，需要生成", projectId);
            }

            context.putSharedData("skipGeneration", false);
            return StepResult.success(Map.of("hasExisting", hadExisting), 100);

        } catch (Exception e) {
            log.error("检查现有世界观失败", e);
            return StepResult.failure("检查现有世界观失败: " + e.getMessage());
        }
    }

    // ======================== generate_worldview ========================

    /**
     * 生成世界观设定 — 提示词完全由 DB 模板驱动
     */
    private StepResult generateWorldview(AiTaskStep step, TaskContext context) {
        try {
            Project project = projectMapper.selectById(context.getProjectId());
            String projectDescription = project.getDescription();
            String storyTone = project.getStoryTone();
            String storyGenre = project.getStoryGenre();
            String tags = project.getTags();

            // 构建提示词（模板已包含 XML 格式要求，不再手动追加）
            String prompt = buildWorldviewPrompt(projectDescription, storyTone, storyGenre, tags);

            AIGenerateRequest aiRequest = new AIGenerateRequest();
            aiRequest.setProjectId(context.getProjectId());
            aiRequest.setRequestType("llm_worldview_create");
            aiRequest.setRole(AIRole.NOVEL_WRITER);
            aiRequest.setTask(prompt);
            aiRequest.setMaxTokens(8000);

            AIGenerateResponse aiResponse = llmProviderFactory.getDefaultProvider().generate(aiRequest);
            String responseContent = aiResponse.getContent();

            log.info(">>> AI生成世界观原始响应:\n{}", responseContent);

            context.putSharedData("aiResponse", responseContent);
            return StepResult.success(Map.of("aiResponse", responseContent), 100);

        } catch (Exception e) {
            log.error("生成世界观设定失败", e);
            return StepResult.failure("生成世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== save_worldview ========================

    /**
     * 保存世界观设定：解析 XML、存储世界观基本信息 + 结构化力量体系入库 + 建立关联
     */
    private StepResult saveWorldview(AiTaskStep step, TaskContext context) {
        try {
            Long projectId = context.getProjectId();

            String aiResponse = (String) context.getSharedData("aiResponse");
            if (aiResponse == null) {
                return StepResult.failure("未找到AI生成的世界观内容");
            }

            JsonNode config = context.getConfig();
            String storyGenre = config.has("storyGenre") ? config.get("storyGenre").asText() : "";

            // 解析并保存
            NovelWorldview worldview = parseAndSaveWorldview(projectId, aiResponse, storyGenre);

            if (worldview != null) {
                log.info("世界观设定保存成功，ID: {}", worldview.getId());

                updateProjectSetupStage(projectId, "worldview_configured");

                return StepResult.success(Map.of("worldviewId", worldview.getId()), 100);
            } else {
                return StepResult.failure("解析并保存世界观失败");
            }

        } catch (Exception e) {
            log.error("保存世界观设定失败", e);
            return StepResult.failure("保存世界观设定失败: " + e.getMessage());
        }
    }

    // ======================== buildWorldviewPrompt ========================

    /**
     * 构建生成世界观的提示词 — 完全由 DB 模板驱动
     * <p>
     * 模板（llm_worldview_create）已包含完整的 XML 格式要求，
     * 这里只负责准备模板变量和补充 tags 信息。
     */
    private String buildWorldviewPrompt(String projectDescription, String storyTone, String storyGenre, String tags) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectDescription", projectDescription != null && !projectDescription.isEmpty() ? projectDescription : "待补充");
        variables.put("storyTone", storyTone != null && !storyTone.isEmpty() ? storyTone : "待补充");
        variables.put("storyGenre", storyGenre != null && !storyGenre.isEmpty() ? storyGenre : "待补充");

        // tags 是可选的，模板中使用 {tagsSection} 占位
        if (tags != null && !tags.isEmpty()) {
            variables.put("tagsSection", "【标签】" + tags);
        } else {
            variables.put("tagsSection", "");
        }

        String templateCode = "llm_worldview_create";
        String prompt = promptTemplateService.executeTemplate(templateCode, variables);

        return prompt;
    }

    // ======================== parseAndSaveWorldview ========================

    /**
     * 解析 AI 响应并保存世界观 + 力量体系
     * <p>
     * 流程：
     * 1. 用 XmlParser 解析 <w> 根标签下的 t/b/p/g/f/l/r 字段
     * 2. 将 t/b/g/f/l/r 保存到 novel_worldview（不含 powerSystem）
     * 3. 解析 <p> 下的 <system> 子节点，结构化写入 novel_power_system / level / step
     * 4. 建立 novel_worldview_power_system 关联
     */
    private NovelWorldview parseAndSaveWorldview(Long projectId, String aiResponse, String storyGenre) {
        try {
            // Step 1: 解析顶层 XML 字段
            Map<String, String> worldviewData = XmlParser.parseXml(
                aiResponse,
                "w",
                "t", "b", "p", "g", "f", "l", "r"
            );

            if (worldviewData.isEmpty()) {
                log.warn("解析世界观XML失败，数据为空");
                return null;
            }

            // Step 2: 获取项目信息
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.error("项目不存在，projectId={}", projectId);
                return null;
            }

            LocalDateTime now = LocalDateTime.now();

            // Step 3: 保存世界观基本信息（不含 powerSystem）
            NovelWorldview worldview = new NovelWorldview();
            worldview.setUserId(project.getUserId());
            worldview.setProjectId(projectId);
            worldview.setWorldType(worldviewData.getOrDefault("t", storyGenre));
            worldview.setWorldBackground(worldviewData.getOrDefault("b", ""));
            worldview.setGeography(worldviewData.getOrDefault("g", ""));
            worldview.setForces(worldviewData.getOrDefault("f", ""));
            worldview.setTimeline(worldviewData.getOrDefault("l", ""));
            worldview.setRules(worldviewData.getOrDefault("r", ""));
            worldview.setCreateTime(now);
            worldview.setUpdateTime(now);

            worldviewMapper.insert(worldview);
            log.info("世界观基本信息保存成功，ID: {}", worldview.getId());

            // Step 4: 解析并保存结构化力量体系
            String powerSystemXml = worldviewData.get("p");
            if (powerSystemXml != null && !powerSystemXml.trim().isEmpty()) {
                savePowerSystemsFromXml(projectId, worldview.getId(), powerSystemXml, now);
            } else {
                log.info("未检测到力量体系内容，跳过");
            }

            return worldview;

        } catch (Exception e) {
            log.error("解析世界观失败", e);
            log.error("AI响应: {}", aiResponse);
            return null;
        }
    }

    // ======================== 力量体系 XML 解析 & 入库 ========================

    /**
     * 从 <p> 标签内容中解析 <system> 列表并入库
     * <p>
     * 支持的格式：
     * <pre>
     * &lt;system&gt;
     *   &lt;name&gt;修仙体系&lt;/name&gt;
     *   &lt;sourceFrom&gt;天地灵气&lt;/sourceFrom&gt;
     *   &lt;coreResource&gt;灵石&lt;/coreResource&gt;
     *   &lt;cultivationMethod&gt;打坐冥想&lt;/cultivationMethod&gt;
     *   &lt;description&gt;...&lt;/description&gt;
     *   &lt;levels&gt;
     *     &lt;level&gt;
     *       &lt;levelName&gt;练气期&lt;/levelName&gt;
     *       &lt;description&gt;...&lt;/description&gt;
     *       &lt;breakthroughCondition&gt;...&lt;/breakthroughCondition&gt;
     *       &lt;lifespan&gt;150年&lt;/lifespan&gt;
     *       &lt;powerRange&gt;...&lt;/powerRange&gt;
     *       &lt;landmarkAbility&gt;灵气外放&lt;/landmarkAbility&gt;
     *       &lt;steps&gt;
     *         &lt;step&gt;前期&lt;/step&gt;
     *         &lt;step&gt;中期&lt;/step&gt;
     *       &lt;/steps&gt;
     *     &lt;/level&gt;
     *   &lt;/levels&gt;
     * &lt;/system&gt;
     * </pre>
     */
    private void savePowerSystemsFromXml(Long projectId, Long worldviewId, String powerSystemXml, LocalDateTime now) {
        // 将 <p> 包裹内容构造成完整的 XML 以便 DOM 解析
        String wrappedXml = "<p>" + powerSystemXml + "</p>";

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(wrappedXml)));

            NodeList systemNodes = doc.getElementsByTagName("system");
            log.info("解析到 {} 个力量体系", systemNodes.getLength());

            for (int i = 0; i < systemNodes.getLength(); i++) {
                Element systemEl = (Element) systemNodes.item(i);
                try {
                    saveOneSystem(projectId, worldviewId, systemEl, now);
                } catch (Exception e) {
                    log.warn("保存第 {} 个力量体系失败，跳过: {}", i + 1, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.warn("力量体系 XML DOM 解析失败，尝试正则回退: {}", e.getMessage());
            savePowerSystemsWithRegex(projectId, worldviewId, powerSystemXml, now);
        }
    }

    /**
     * 保存单个 <system> 节点
     */
    private void saveOneSystem(Long projectId, Long worldviewId, Element systemEl, LocalDateTime now) {
        String name = getTextContent(systemEl, "name");
        String sourceFrom = getTextContent(systemEl, "sourceFrom");
        String coreResource = getTextContent(systemEl, "coreResource");
        String cultivationMethod = getTextContent(systemEl, "cultivationMethod");
        String description = getTextContent(systemEl, "description");

        if (name == null || name.trim().isEmpty()) {
            name = "未命名体系";
        }

        // 构建保存请求
        PowerSystemSaveRequest request = new PowerSystemSaveRequest();
        request.setProjectId(projectId);
        request.setName(name);
        request.setSourceFrom(sourceFrom);
        request.setCoreResource(coreResource);
        request.setCultivationMethod(cultivationMethod);
        request.setDescription(description);

        // 解析 levels
        NodeList levelNodes = systemEl.getElementsByTagName("level");
        List<PowerSystemLevelSaveRequest> levels = new ArrayList<>();

        for (int i = 0; i < levelNodes.getLength(); i++) {
            Element levelEl = (Element) levelNodes.item(i);
            // 跳过嵌套在其他 level 内的 level（避免重复）
            if (levelEl.getParentNode() != null
                && "levels".equals(levelEl.getParentNode().getNodeName())) {
                levels.add(parseLevel(levelEl, i));
            }
        }

        request.setLevels(levels);

        // 调用 PowerSystemService 保存
        NovelPowerSystem saved = powerSystemService.savePowerSystem(request);
        log.info("力量体系入库成功，ID={}, name={}", saved.getId(), saved.getName());

        // 建立关联
        NovelWorldviewPowerSystem rel = new NovelWorldviewPowerSystem();
        rel.setWorldviewId(worldviewId);
        rel.setPowerSystemId(saved.getId());
        worldviewPowerSystemMapper.insert(rel);
        log.info("世界观-力量体系关联已建立，worldviewId={}, powerSystemId={}", worldviewId, saved.getId());
    }

    /**
     * 解析单个 <level> 元素
     */
    private PowerSystemLevelSaveRequest parseLevel(Element levelEl, int index) {
        PowerSystemLevelSaveRequest levelReq = new PowerSystemLevelSaveRequest();
        levelReq.setLevel(index + 1);
        levelReq.setLevelName(getTextContent(levelEl, "levelName"));
        levelReq.setDescription(getTextContent(levelEl, "description"));
        levelReq.setBreakthroughCondition(getTextContent(levelEl, "breakthroughCondition"));
        levelReq.setLifespan(getTextContent(levelEl, "lifespan"));
        levelReq.setPowerRange(getTextContent(levelEl, "powerRange"));
        levelReq.setLandmarkAbility(getTextContent(levelEl, "landmarkAbility"));

        // 解析 steps
        NodeList stepNodes = levelEl.getElementsByTagName("step");
        List<PowerSystemLevelStepSaveRequest> steps = new ArrayList<>();

        for (int j = 0; j < stepNodes.getLength(); j++) {
            // 只取直接子节点的 step，避免嵌套
            if (stepNodes.item(j).getParentNode() != null
                && "steps".equals(stepNodes.item(j).getParentNode().getNodeName())) {
                PowerSystemLevelStepSaveRequest stepReq = new PowerSystemLevelStepSaveRequest();
                stepReq.setLevel(j + 1);
                stepReq.setLevelName(stepNodes.item(j).getTextContent().trim());
                steps.add(stepReq);
            }
        }

        levelReq.setSteps(steps);
        return levelReq;
    }

    /**
     * 安全获取子元素文本内容
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    /**
     * 正则表达式回退方案：当 DOM 解析失败时使用
     * 从 <system>...</system> 块中提取基本信息
     */
    private void savePowerSystemsWithRegex(Long projectId, Long worldviewId, String powerSystemXml, LocalDateTime now) {
        Pattern systemPattern = Pattern.compile("<system>(.*?)</system>", Pattern.DOTALL);
        Matcher systemMatcher = systemPattern.matcher(powerSystemXml);

        int count = 0;
        while (systemMatcher.find()) {
            count++;
            String systemBlock = systemMatcher.group(1);

            try {
                String name = extractTag(systemBlock, "name");
                String sourceFrom = extractTag(systemBlock, "sourceFrom");
                String coreResource = extractTag(systemBlock, "coreResource");
                String cultivationMethod = extractTag(systemBlock, "cultivationMethod");
                String description = extractTag(systemBlock, "description");

                if (name == null || name.trim().isEmpty()) {
                    name = "未命名体系" + count;
                }

                PowerSystemSaveRequest request = new PowerSystemSaveRequest();
                request.setProjectId(projectId);
                request.setName(name);
                request.setSourceFrom(sourceFrom);
                request.setCoreResource(coreResource);
                request.setCultivationMethod(cultivationMethod);
                request.setDescription(description);
                // 正则回退不解析 levels，保持简单

                NovelPowerSystem saved = powerSystemService.savePowerSystem(request);
                log.info("力量体系入库成功（正则回退），ID={}, name={}", saved.getId(), saved.getName());

                NovelWorldviewPowerSystem rel = new NovelWorldviewPowerSystem();
                rel.setWorldviewId(worldviewId);
                rel.setPowerSystemId(saved.getId());
                worldviewPowerSystemMapper.insert(rel);

            } catch (Exception e) {
                log.warn("正则回退：保存第 {} 个力量体系失败: {}", count, e.getMessage());
            }
        }

        if (count == 0) {
            log.info("正则回退：未提取到 <system> 块");
        }
    }

    /**
     * 从 XML 片段中提取标签内容（支持 CDATA）
     */
    private String extractTag(String xml, String tagName) {
        // 先尝试 CDATA
        Pattern cdataPattern = Pattern.compile(
            "<" + tagName + ">\\s*<!\\[CDATA\\[([^]]*?)\\]\\]>\\s*</" + tagName + ">", Pattern.DOTALL);
        Matcher m = cdataPattern.matcher(xml);
        if (m.find()) return m.group(1).trim();

        // 普通标签
        Pattern normalPattern = Pattern.compile(
            "<" + tagName + ">(.*?)</" + tagName + ">", Pattern.DOTALL);
        m = normalPattern.matcher(xml);
        if (m.find()) return m.group(1).trim();

        return null;
    }

    // ======================== updateProjectSetupStage ========================

    /**
     * 更新项目的 setup_stage 状态
     */
    private void updateProjectSetupStage(Long projectId, String stage) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                project.setSetupStage(stage);
                projectMapper.updateById(project);
                log.info("项目状态已更新，projectId={}, setupStage={}", projectId, stage);
            }
        } catch (Exception e) {
            log.error("更新项目状态失败，projectId={}, stage={}", projectId, stage, e);
        }
    }
}
