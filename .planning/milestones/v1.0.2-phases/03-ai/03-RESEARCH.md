# Phase 3: AI 集成与提示词迁移 - Research

**Researched:** 2026-04-02
**Domain:** AI prompt template migration, DOM XML parsing, data migration (forces text to structured faction tables)
**Confidence:** HIGH

## Summary

Phase 3 bridges the gap between the structured faction data tables (Phase 1) / service layer (Phase 2) and the AI generation + prompt construction pipeline. The core work has three dimensions: (1) updating the AI worldview generation prompt template to produce structured faction XML instead of plain-text CDATA, (2) adding a DOM parser in WorldviewTaskStrategy to parse that XML into the novel_faction tree and association tables, and (3) migrating all `worldview.getForces()` call sites to call `factionService.fillForces(worldview)` so the transient `forces` field is populated from the structured tables.

The existing codebase has well-established patterns for all three dimensions. The geography restructuring used the exact same approach (DOM parsing of `<g>/<r>` tags, two-pass insert, `fillGeography()` for transient field). The power system restructuring used name-to-ID matching. These patterns should be replicated directly.

**Primary recommendation:** Follow the geography/power-system patterns exactly. The `saveFactionsFromXml()` method should mirror `saveGeographyRegionsFromXml()` structure, and the 6+ `getForces()` migration sites should each add `factionService.fillForces(worldview)` right after the existing `continentRegionService.fillGeography(worldview)` call.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 外层 `<f>` 容器，内层 `<faction>` 标签，子势力通过嵌套 `<faction>` 表达层级关系（与地理 `<g>/<r>` 模式一致）
- **D-02:** XML 节点不使用属性，所有数据通过子元素表达：`<n>` 名称、`<d>` 描述（CDATA 包裹）、`<type>` 类型、`<power>` 力量体系引用、`<regions>` 地区引用
- **D-03:** 地区引用用逗号分隔的名称列表，力量体系用单个名称引用
- **D-04:** 势力间关系用 `<relation>` 标签对，包含 `<target>` 和 `<type>`
- **D-05:** type 和 power 仅顶级势力设置，子势力继承顶级势力值
- **D-06:** 三级容错匹配策略：精确匹配 -> 去后缀匹配（去除宗/派/门等后缀再比较）-> contains 包含匹配
- **D-07:** XML 解析失败时跳过失败的势力项，成功的正常入库，只记录 WARN 日志
- **D-08:** DOM 解析使用 getChildNodes() 直接子元素迭代
- **D-09:** 6 个 getForces() 调用点一次性全部迁移为 fillForces()，不做增量迁移
- **D-10:** 迁移后直接调用 factionService.fillForces(worldview) 替换原有的 worldview.getForces() 调用
- **D-11:** 势力 XML 提示词使用严格规则描述
- **D-12:** 提示词中势力引用地区名称和力量体系名称，确保与已输出的名称一致
- **D-13:** 两遍插入策略：第一遍存所有势力构建名称->ID 映射，第二遍建立关联
- **D-14:** 已有项目重新生成世界观时先清除旧势力数据再解析新数据

### Claude's Discretion
- 名称模糊匹配的具体后缀列表（宗/派/门/殿/阁等）
- DOM 解析的异常处理细节
- 提示词模板中的具体措辞和格式说明
- 日志记录的格式和级别

### Deferred Ideas (OUT OF SCOPE)
None - discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AI-01 | 更新 AI 提示词模板（id=3），势力部分从 `<f><![CDATA[纯文本]]></f>` 改为结构化 XML 格式 | Prompt template stored in DB table `ai_prompt_template_version`, code=llm_worldview_create. Template uses `{variable}` placeholders via PromptTemplateService.executeTemplate(). See "提示词模板设计" section. |
| AI-02 | 提示词中势力 XML 通过名称引用力量体系和地理区域 | Power systems already saved by savePowerSystems() before factions are parsed. Regions already saved by saveGeographyRegionsFromXml() before factions. Both name-to-ID maps can be built from database after those steps complete. |
| AI-03 | WorldviewTaskStrategy 新增 saveFactionsFromXml() 方法，DOM 解析 `<f>` 标签 | Exact same pattern as saveGeographyRegionsFromXml() (lines 368-406). Uses DocumentBuilderFactory with XXE protection, getChildNodes() iteration, recursive parsing. |
| AI-04 | 两遍插入策略：第一遍存所有势力，第二遍建立关联 | SaveTree() handles faction insert with parent-child. After all inserts, name->ID map is built. Then iterate again to insert NovelFactionRegion and NovelFactionRelation records. |
| AI-05 | 名称->ID 模糊匹配容错 | Three-tier matching: exact -> strip suffix (宗/派/门/殿/阁/会/帮/谷/山/城/族/教/院/宫/楼/庄/寨/盟/宗门/派门) -> contains. See "名称匹配策略" section. |
| AI-06 | checkExisting 步骤中增加删除旧势力数据 | WorldviewTaskStrategy.checkExisting() (line 141) already deletes geography. Add `factionService.deleteByProjectId(projectId)` at same location. |
| AI-07 | DOM 解析使用 getChildNodes() 直接子元素迭代 | Pattern: `NodeList children = parent.getChildNodes(); for (int i...) { if (node.getNodeType() == ELEMENT_NODE && "faction".equals(node.getNodeName())) }`. See parseRegionNodes() lines 413-424. |
| AI-08 | 所有关联表存储数据库 ID，不存储名称 | NovelFactionRegion stores factionId/regionId (Long). NovelFactionRelation stores factionId/targetFactionId (Long). Name->ID resolution happens during parsing, only IDs stored. |
| PROMPT-01 | PromptTemplateBuilder 中 getForces() 迁移 | PromptTemplateBuilder.buildWorldviewString() line 382-383. Add `factionService.fillForces(worldview)` before `getForces()` call. |
| PROMPT-02 | ChapterPromptBuilder 中 getForces() 迁移 | ChapterPromptBuilder.buildWorldviewInfo() line 160-161. Add `factionService.fillForces(worldview)` before `getForces()` call. |
| PROMPT-03 | PromptContextBuilder 中 getForces() 迁移 | PromptContextBuilder.buildWorldviewContext() line 178-179. Add `factionService.fillForces(worldview)` in getWorldview() method or buildWorldviewContext(). |
| PROMPT-04 | ChapterFixTaskStrategy 中 getForces() 迁移 | ChapterFixTaskStrategy.getWorldview() line 421-422. Add `factionService.fillForces(worldview)` before `getForces()` call. |
| PROMPT-05 | VolumeOptimizeTaskStrategy 中 getForces() 迁移 | VolumeOptimizeTaskStrategy.buildWorldviewInfo() line 367-368. Add `factionService.fillForces(worldview)` before `getForces()` call. |
| PROMPT-06 | OutlineTaskStrategy 中 getForces() 迁移 | OutlineTaskStrategy has 4 separate getForces() call sites (lines 907, 977, 1723, 1770). Add `factionService.fillForces(worldview)` at each. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| org.w3c.dom | JDK built-in | XML DOM parsing for nested `<faction>` tags | Already used for geography parsing in same class. Jackson XML cannot handle nested same-name tags. |
| MyBatis-Plus 3.5.5 | 3.5.5 | ORM for novel_faction, novel_faction_region, novel_faction_relation | Existing stack, all mapper interfaces already exist |
| Spring @Transactional | Spring Boot 3.2.0 | Transaction boundaries for two-pass insert | WorldviewTaskStrategy already uses implicit transactions via step execution |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| XmlParser (custom) | existing | Jackson-based XML parsing for non-nested fields | For parsing `<w>` root tag fields (t, b, l, r). NOT for `<f>` which needs DOM. |
| FactionService | Phase 2 | fillForces(), saveTree(), deleteByProjectId() | Already implemented, just need injection and calling |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| org.w3c.dom DOM | JAXB / Jackson XML | DOM chosen: Jackson XML cannot handle nested same-name `<faction>` tags. JAXB heavyweight. Already established pattern. |

**Installation:**
No new dependencies needed. All libraries are already in the project.

## Architecture Patterns

### Recommended Project Structure
No new files needed. All changes are modifications to existing files:
- `WorldviewTaskStrategy.java` - add saveFactionsFromXml()
- `PromptTemplateBuilder.java` - inject FactionService, add fillForces() call
- `ChapterPromptBuilder.java` - inject FactionService, add fillForces() call
- `PromptContextBuilder.java` - inject FactionService, add fillForces() call
- `ChapterFixTaskStrategy.java` - inject FactionService, add fillForces() call
- `VolumeOptimizeTaskStrategy.java` - inject FactionService, add fillForces() call
- `OutlineTaskStrategy.java` - inject FactionService, add fillForces() call
- DB: `ai_prompt_template_version` table - update template row for code=llm_worldview_create

### Pattern 1: DOM Parsing Nested XML (replicate from geography)
**What:** Parse `<f>` container with nested `<faction>` elements using org.w3c.dom
**When to use:** When AI output contains nested same-name tags that Jackson XML cannot handle
**Example:**
```java
// Source: WorldviewTaskStrategy.saveGeographyRegionsFromXml() lines 368-406
private void saveFactionsFromXml(Long projectId, String aiResponse) {
    try {
        int start = aiResponse.indexOf("<f>");
        int end = aiResponse.indexOf("</f>");
        if (start < 0 || end < 0) {
            log.info("未找到 <f> 势力标签，跳过入库");
            return;
        }
        String factionXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(factionXml)));

        Element root = doc.getDocumentElement();
        Element fElement = (Element) root.getElementsByTagName("f").item(0);
        // Parse and save factions...
    } catch (Exception e) {
        log.error("保存势力失败，projectId={}", projectId, e);
    }
}
```

### Pattern 2: Two-Pass Insert with Name-to-ID Mapping
**What:** First pass inserts all factions and builds name->ID map. Second pass creates associations.
**When to use:** When association records need IDs that only exist after insert.
**Example:**
```java
// Pass 1: Insert all factions, build name->ID map
Map<String, Long> nameToIdMap = new HashMap<>();
for (ParsedFaction pf : allFactions) {
    NovelFaction entity = new NovelFaction();
    entity.setName(pf.getName());
    // ... set fields
    factionMapper.insert(entity);
    nameToIdMap.put(pf.getName(), entity.getId());
}

// Pass 2: Create associations using the map
for (ParsedFaction pf : allFactionsWithAssociations) {
    Long factionId = nameToIdMap.get(pf.getName());
    // Insert NovelFactionRegion records
    // Insert NovelFactionRelation records
}
```

### Pattern 3: fillForces() Migration (consistent across all call sites)
**What:** Before accessing worldview.getForces(), call factionService.fillForces(worldview)
**When to use:** At every location that reads worldview.getForces() for prompt construction
**Example:**
```java
// Before migration:
continentRegionService.fillGeography(worldview);
if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
    sb.append("- 势力分布：").append(worldview.getForces()).append("\n");
}

// After migration:
continentRegionService.fillGeography(worldview);
factionService.fillForces(worldview);
if (worldview.getForces() != null && !worldview.getForces().isEmpty()) {
    sb.append("- 势力分布：").append(worldview.getForces()).append("\n");
}
```

### Anti-Patterns to Avoid
- **Using getElementsByTagName() for nested faction parsing**: This returns ALL descendants, not just direct children. Must use getChildNodes() and check node name. This is explicitly called out in AI-07.
- **Storing names instead of IDs in association tables**: NovelFactionRegion.regionId and NovelFactionRelation.targetFactionId must be Long IDs, not string names.
- **Calling fillForces() in WorldviewTaskStrategy.parseAndSaveWorldview()**: The setForces() on line 337 writes the raw XML text to the transient field. After migration, this should be removed or left as-is (since `forces` is @TableField(exist=false), the raw text won't persist). The actual structured data goes through saveFactionsFromXml().

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| XML parsing nested same-name tags | Custom regex/string parsing for `<faction>` | org.w3c.dom DOM with getChildNodes() | Already proven in geography parsing. Regex breaks on CDATA, nested tags, whitespace variations. |
| Tree save with parent-child | Manual insert loop with parent tracking | FactionService.saveTree() | Already handles deep/sortOrder/parentId correctly via saveNodeRecursive(). |
| Name-to-ID fuzzy matching | Custom string similarity algorithm | Three-tier matching (exact -> strip suffix -> contains) | Per D-06 decision. Simple, predictable, handles common AI name variations. |
| Transient forces field population | Manual string building at each call site | FactionService.fillForces(worldview) | Already implemented in Phase 2. Handles null/empty, builds text from tree. |

**Key insight:** The geography restructuring already solved every problem this phase faces. The only novel aspect is the three-tier name matching for region/power-system references and the faction-relation table.

## Common Pitfalls

### Pitfall 1: getForces() Call Sites Are More Than 6
**What goes wrong:** CONTEXT.md lists 6 call sites, but grep found getForces() used in 8 files (10+ locations). ChapterGenerationTaskStrategy (2 locations) and ChapterService (1 location) also use it.
**Why it happens:** The original scope analysis missed these files.
**How to avoid:** Check ChapterGenerationTaskStrategy (lines 423, 557) and ChapterService (line 1003) and include them in migration. PROMPT requirements only list 6, so the additional ones may need to be flagged as scope extension.
**Warning signs:** grep for `getForces()` returns more results than expected.

### Pitfall 2: OutlineTaskStrategy Has 4 Separate getForces() Calls
**What goes wrong:** OutlineTaskStrategy has getForces() calls in 4 different methods (buildChapterPromptUsingTemplate, buildChapterPrompt, buildCharacterPromptUsingTemplate, buildCharacterPrompt), not just one.
**Why it happens:** The class has template-based methods AND legacy fallback methods, both of which read forces.
**How to avoid:** Each of the 4 locations needs the fillForces() call added. The worldview object is obtained from context.getSharedData() in some cases, so fillForces must be called each time before reading.

### Pitfall 3: Order of Operations in parseAndSaveWorldview
**What goes wrong:** Factions reference regions and power systems by name, but those must be inserted first.
**Why it happens:** If saveFactionsFromXml() runs before saveGeographyRegionsFromXml() or savePowerSystems(), the name-to-ID lookups will fail.
**How to avoid:** Ensure saveFactionsFromXml() is called AFTER saveGeographyRegionsFromXml() and savePowerSystems() in parseAndSaveWorldview(). Current order: saveGeography (Step 4) -> savePowerSystems (Step 5). Add saveFactions as Step 6.

### Pitfall 4: checkExisting Must Also Delete Old Faction Data
**What goes wrong:** Regenerating worldview without deleting old faction data causes duplicate/ghost factions.
**Why it happens:** checkExisting() deletes geography (line 192) but does not delete factions.
**How to avoid:** Add `factionService.deleteByProjectId(projectId)` in checkExisting() alongside the existing `continentRegionService.deleteByProjectId(projectId)`.

### Pitfall 5: setForces() in parseAndSaveWorldview Still Writes Raw Text
**What goes wrong:** Line 337 `worldview.setForces(worldSetting.getForces())` sets the transient field to raw XML text, which is then useless.
**Why it happens:** The XmlParser extracts `<f>` content as string. After migration, this content is now structured XML, not plain text.
**How to avoid:** Either remove the setForces() call entirely, or leave it (since @TableField(exist=false) means it won't persist). The actual data goes through saveFactionsFromXml(). The forces field for prompt building is populated by fillForces() at consumption time.

### Pitfall 6: AI Prompt Template Requires Name Consistency
**What goes wrong:** AI outputs faction XML with region/power names that don't exactly match the names in `<g>` and `<p>` sections.
**Why it happens:** LLMs don't guarantee exact string reproduction.
**How to avoid:** Three-tier matching (D-06) handles this. But the prompt template should explicitly instruct the AI to reuse exact names from previous sections.

## Code Examples

### Complete saveFactionsFromXml() Method Pattern
```java
// Source: Adapted from saveGeographyRegionsFromXml() + savePowerSystems()
private void saveFactionsFromXml(Long projectId, String aiResponse) {
    try {
        int start = aiResponse.indexOf("<f>");
        int end = aiResponse.indexOf("</f>");
        if (start < 0 || end < 0) {
            log.info("未找到 <f> 势力标签，跳过入库");
            return;
        }
        String factionXml = "<root>" + aiResponse.substring(start, end + 4) + "</root>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(factionXml)));

        Element root = doc.getDocumentElement();
        NodeList fNodes = root.getElementsByTagName("f");
        if (fNodes.getLength() == 0) {
            log.info("<f> 标签内无内容，跳过入库");
            return;
        }
        Element fElement = (Element) fNodes.item(0);

        // Pass 1: Parse all factions and save via saveTree (builds name->ID map)
        Map<String, Long> nameToIdMap = new LinkedHashMap<>();
        List<ParsedFactionRelations> pendingRelations = new ArrayList<>();
        List<NovelFaction> rootFactions = parseFactionNodes(fElement, projectId, nameToIdMap, pendingRelations);

        if (rootFactions.isEmpty()) {
            log.info("势力解析结果为空，跳过入库");
            return;
        }

        factionService.saveTree(projectId, rootFactions);
        // After saveTree, nameToIdMap is populated with generated IDs

        // Pass 2: Create associations using nameToIdMap
        for (ParsedFactionRelations pr : pendingRelations) {
            Long factionId = nameToIdMap.get(pr.factionName);
            if (factionId == null) continue;

            // Region associations
            for (String regionName : pr.regionNames) {
                Long regionId = findRegionIdByName(projectId, regionName);
                if (regionId != null) {
                    NovelFactionRegion assoc = new NovelFactionRegion();
                    assoc.setFactionId(factionId);
                    assoc.setRegionId(regionId);
                    factionRegionMapper.insert(assoc);
                } else {
                    log.warn("未匹配到地区名称: {}, 跳过关联", regionName);
                }
            }

            // Faction-faction relations
            for (ParsedRelation rel : pr.relations) {
                Long targetId = nameToIdMap.get(rel.targetName);
                if (targetId != null) {
                    NovelFactionRelation relation = new NovelFactionRelation();
                    relation.setFactionId(factionId);
                    relation.setTargetFactionId(targetId);
                    relation.setRelationType(rel.type);
                    factionRelationMapper.insert(relation);
                } else {
                    log.warn("未匹配到势力名称: {}, 跳过关系", rel.targetName);
                }
            }
        }

        log.info("势力入库完成，projectId={}, 根节点数={}", projectId, rootFactions.size());
    } catch (Exception e) {
        log.error("保存势力失败，projectId={}", projectId, e);
    }
}
```

### Recursive parseFactionNode (using getChildNodes)
```java
// Source: Adapted from parseSingleRegion() lines 429-461
private NovelFaction parseFactionNode(Element factionElement, Long projectId,
                                       Map<String, Long> nameToIdMap,
                                       List<ParsedFactionRelations> pendingRelations) {
    NovelFaction faction = new NovelFaction();
    faction.setProjectId(projectId);

    // Parse child elements using getChildNodes() (not getElementsByTagName)
    NodeList children = factionElement.getChildNodes();
    List<String> regionNames = new ArrayList<>();
    List<ParsedRelation> relations = new ArrayList<>();

    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() != Node.ELEMENT_NODE) continue;
        String tag = child.getNodeName();

        switch (tag) {
            case "n" -> faction.setName(child.getTextContent().trim());
            case "d" -> faction.setDescription(child.getTextContent().trim());
            case "type" -> faction.setType(child.getTextContent().trim());
            case "power" -> {
                String powerName = child.getTextContent().trim();
                Long powerId = findPowerSystemIdByName(projectId, powerName);
                if (powerId != null) {
                    faction.setCorePowerSystem(powerId);
                } else {
                    log.warn("未匹配到力量体系: {}", powerName);
                }
            }
            case "regions" -> {
                String regionsText = child.getTextContent().trim();
                regionNames = Arrays.stream(regionsText.split("[,，]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            case "relation" -> {
                ParsedRelation rel = parseRelationElement((Element) child);
                if (rel != null) relations.add(rel);
            }
        }
    }

    // Parse nested child factions
    List<NovelFaction> childFactions = new ArrayList<>();
    for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() == Node.ELEMENT_NODE && "faction".equals(child.getNodeName())) {
            childFactions.add(parseFactionNode((Element) child, projectId, nameToIdMap, pendingRelations));
        }
    }
    if (!childFactions.isEmpty()) {
        faction.setChildren(childFactions);
    }

    // Register for pass 2
    String factionName = faction.getName();
    if (factionName != null && (!regionNames.isEmpty() || !relations.isEmpty())) {
        pendingRelations.add(new ParsedFactionRelations(factionName, regionNames, relations));
    }

    return faction;
}
```

### Three-Tier Name Matching
```java
private Long findRegionIdByName(Long projectId, String name) {
    if (name == null || name.isEmpty()) return null;

    // Tier 1: Exact match
    List<NovelContinentRegion> regions = continentRegionMapper.selectList(
        new LambdaQueryWrapper<NovelContinentRegion>()
            .eq(NovelContinentRegion::getProjectId, projectId)
    );

    for (NovelContinentRegion r : regions) {
        if (name.equals(r.getName())) return r.getId();
    }

    // Tier 2: Strip common suffixes and compare
    String stripped = name.replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
    for (NovelContinentRegion r : regions) {
        String rStripped = r.getName().replaceAll("[宗派门殿阁会帮谷山城族教院宫楼庄寨盟]$", "");
        if (!stripped.isEmpty() && stripped.equals(rStripped)) return r.getId();
    }

    // Tier 3: Contains match
    for (NovelContinentRegion r : regions) {
        if (r.getName().contains(name) || name.contains(r.getName())) return r.getId();
    }

    log.warn("三级匹配均失败，未找到地区: {}", name);
    return null;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `forces` as plain text in novel_worldview.forces column | `forces` as @TableField(exist=false), populated from novel_faction tree | Phase 1 (DB-06) | Column still exists in DB but not read. Data lives in novel_faction tables. |
| Geography as plain text | Geography from novel_continent_region tree | Prior restructuring | Exact same pattern to follow |
| Power system as plain text | Power system from novel_power_system tables | Prior restructuring | Exact same name-to-ID pattern to follow |

**Deprecated/outdated:**
- `novel_worldview.forces` column: Still exists in DB (kept for migration safety per DB-07) but is no longer read by code. The @TableField(exist=false) annotation means MyBatis-Plus ignores it.

## Open Questions

1. **ChapterGenerationTaskStrategy and ChapterService not in scope**
   - What we know: These two files also call getForces() but are not listed in PROMPT-01 through PROMPT-06.
   - What's unclear: Whether they should be migrated in this phase or handled separately.
   - Recommendation: Flag this to the planner. ChapterGenerationTaskStrategy (2 calls) and ChapterService (1 call) will still read the transient `forces` field which will be null unless fillForces() is called first. Either migrate them or add fillForces() calls.

2. **Prompt Template Content (AI-01)**
   - What we know: The template is stored in DB table `ai_prompt_template_version` with code `llm_worldview_create`. It uses `{variable}` placeholders.
   - What's unclear: The exact current content of the template. Only the code and variable names are known.
   - Recommendation: The planner should include a step to read the current template from DB and update the faction section. The exact XML format is defined in D-01 through D-05.

3. **Type field enum mapping**
   - What we know: NovelFaction.type stores "ally/hostile/neutral" in DB. The AI output XML uses `<type>正派</type>` format (Chinese labels).
   - What's unclear: Whether the prompt should instruct AI to output English enum values or Chinese labels.
   - Recommendation: Based on D-11 ("type枚举：正派/反派/中立"), the AI outputs Chinese labels. The parser should map: 正派->ally, 反派->hostile, 中立->neutral. This mapping already exists in FactionServiceImpl.formatTypeLabel() (line 428-436) as reverse logic.

## Environment Availability

Step 2.6: SKIPPED (no external dependencies beyond existing MySQL and Redis - purely code/config changes)

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | pom.xml (maven-surefire-plugin) |
| Quick run command | `cd ai-factory-backend && mvn test -pl . -Dtest=FactionServiceTest -DfailIfNoTests=false` |
| Full suite command | `cd ai-factory-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AI-03 | saveFactionsFromXml parses nested faction XML correctly | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testSaveFactionsFromXml` | Wave 0 needed |
| AI-04 | Two-pass insert builds correct name->ID map and associations | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testTwoPassInsert` | Wave 0 needed |
| AI-05 | Three-tier name matching works for fuzzy AI names | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testNameMatching` | Wave 0 needed |
| AI-06 | checkExisting deletes old faction data | unit | `mvn test -Dtest=WorldviewTaskStrategyTest#testCheckExistingDeletesFactions` | Wave 0 needed |
| PROMPT-01~06 | fillForces() populates worldview.getForces() correctly | unit | `mvn test -Dtest=FactionServiceTest#testFillForces` | Wave 0 needed |

### Sampling Rate
- **Per task commit:** `mvn test -pl .`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green before /gsd:verify-work

### Wave 0 Gaps
- [ ] `WorldviewTaskStrategyTest.java` - covers AI-03, AI-04, AI-05, AI-06
- [ ] `FactionServiceTest.java` - covers PROMPT-01~06 (fillForces behavior)
- [ ] Test relies on Mockito for FactionService, ContinentRegionService, mapper mocks

## Sources

### Primary (HIGH confidence)
- Source code analysis of WorldviewTaskStrategy.java (all 558 lines read)
- Source code analysis of FactionServiceImpl.java (all 453 lines read)
- Source code analysis of all 6 listed getForces() migration targets + 2 additional files found
- Source code analysis of NovelWorldview.java, NovelFaction.java, NovelFactionRegion.java, NovelFactionRelation.java entities
- CONTEXT.md decisions (locked by user)

### Secondary (MEDIUM confidence)
- Pattern replication from geography/power-system restructuring (verified in code)
- FactionService interface and implementation (verified in Phase 2)

### Tertiary (LOW confidence)
- AI prompt template exact content (stored in DB, not in code - planner should read from DB during implementation)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all libraries already in project, no new deps
- Architecture: HIGH - exact same pattern as geography restructuring
- Pitfalls: HIGH - discovered through comprehensive grep of getForces() usage
- Scope: MEDIUM - 2 additional call sites found beyond CONTEXT.md list (ChapterGenerationTaskStrategy, ChapterService)

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable - no external API or fast-moving dependency)
