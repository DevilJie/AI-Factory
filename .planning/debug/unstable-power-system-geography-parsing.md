---
status: investigating
trigger: "一次性生成世界观时，力量体系和地理环境的解析不稳定（有时成功有时失败）。用户怀疑是提示词需要加强，让 LLM 返回更稳定的格式。需要分析最近的 ai_interaction_log 记录，对比成功和失败的案例，判断是提示词问题还是解析逻辑问题，然后提出修复建议。"
created: 2026-04-04T12:00:00Z
updated: 2026-04-04T16:30:00Z
---

## Current Focus

hypothesis: CONFIRMED - LLM sometimes returns malformed XML (unclosed tags like `<r>` or `<lls>`), and the DOM parser in WorldviewXmlParser has zero fault tolerance - any XML malformation causes SAXParseException and total data loss for that module
test: Analyze error logs for SAXParseException patterns, compare success vs failure runs
expecting: Two-pronged fix needed: (1) add XML repair in parser, (2) strengthen prompt for XML well-formedness
next_action: Implement XML auto-repair in WorldviewXmlParser and strengthen prompt templates

## Symptoms

expected: 力量体系和地理环境每次生成都应该稳定解析成功
actual: 解析不稳定，有时失败。最近的交互记录中力量体系和地理环境没有解析成功
errors: SAXParseException - unclosed tags: "元素类型 'r' 必须由匹配的结束标记 '</r>' 终止" and "元素类型 'lls' 必须由匹配的结束标记 '</lls>' 终止"
reproduction: 一次性生成世界观，观察解析结果是否稳定
started: 一直存在，修复了 DOM 解析后仍然不稳定

## Eliminated

## Evidence

- timestamp: 2026-04-04T16:05:00Z
  checked: 6次地理环境生成记录 (ai-factory.log lines 236-4854)
  found: |
    地理环境解析结果统计（同项目 projectId=35）：
    - 06:56 (1964字符) - 成功, 2根节点
    - 09:53 (2532字符) - 成功, 2根节点
    - 10:11 (2718字符) - 成功, 2根节点
    - 15:07 (2665字符) - 失败 SAXParseException: 元素类型 "r" 必须由匹配的结束标记 "</r>" 终止
    - 15:14 (2679字符) - 成功, 2根节点
    - 16:23 (2656字符) - 失败 SAXParseException: 元素类型 "r" 必须由匹配的结束标记 "</r>" 终止
    成功率: 4/6 = 67%
  implication: LLM 在约33%的情况下返回未闭合的 `<r>` 标签，DOM 解析器直接抛异常导致数据全部丢失

- timestamp: 2026-04-04T16:10:00Z
  checked: 6次力量体系生成记录 (ai-factory.log)
  found: |
    力量体系解析结果统计：
    - 06:57 (7830字符) - 失败 (旧Jackson根元素不匹配问题，已修复)
    - 09:55 (8516字符) - 失败 (同上)
    - 10:12 (8665字符) - 成功, 2套体系
    - 15:08 (6495字符) - 成功, 2套体系
    - 15:15 (6573字符) - 成功, 2套体系
    - 16:25 (9397字符) - 失败 SAXParseException: 元素类型 "lls" 必须由匹配的结束标记 "</lls>" 终止
    DOM修复后成功率: 3/4 = 75%
  implication: 力量体系也有LLM返回未闭合标签的问题，特别是 `<lls>` 标签

- timestamp: 2026-04-04T16:15:00Z
  checked: WorldviewXmlParser.parseGeographyXml() 第96-133行, parsePowerSystemXml() 第207-266行
  found: |
    两个解析方法都用标准 DOM parser (DocumentBuilderFactory + DocumentBuilder.parse)，没有任何XML容错处理。
    解析失败时 catch(Exception) 直接返回空列表，日志只打印 ERROR 级别日志，没有任何重试或修复尝试。
  implication: DOM 解析器严格模式导致任何格式错误都会导致100%数据丢失

- timestamp: 2026-04-04T16:20:00Z
  checked: V4__independent_prompt_templates.sql 中的提示词模板内容
  found: |
    地理环境模板（第64-66行）要求：
    "1. 对于长文本内容，请使用CDATA标签包裹
     2. 不要包含markdown代码块标记，直接返回XML
     3. 不要包含任何解释或说明文字，只返回XML数据"

    力量体系模板（第135-141行）要求同上，额外加了：
    "严禁出现一行文字概括的情况，必须具体列出"

    但两个模板都没有强调：
    - XML必须格式正确（所有标签必须闭合）
    - 不要省略结束标签
  implication: 提示词缺乏对XML格式正确性的明确约束，LLM可能在生成大量嵌套标签时偶尔遗漏闭合标签

- timestamp: 2026-04-04T16:25:00Z
  checked: 级联影响 - 16:25 力量体系解析失败后
  found: |
    力量体系解析失败 -> systems为空 -> 16:26 势力的力量体系匹配全部失败（天道窃法、古神血契均未匹配）
    -> 地区匹配也全部失败（因为地理环境也没解析成功）
    -> 16:26 systemCount=0 关联为空
    上游模块的解析失败会级联影响所有下游模块
  implication: 这是一个高严重性问题，因为一个模块的解析失败会导致整个世界观数据不完整

## Resolution

root_cause: LLM (DeepSeek) 在生成嵌套XML时偶尔产生未闭合的标签（如 `<r>` 或 `<lls>` 缺少对应的 `</r>` 或 `</lls>`）。WorldviewXmlParser 使用严格模式 DOM 解析器，任何格式错误都导致 SAXParseException，整个模块数据全部丢弃。提示词也缺乏对XML格式正确性的明确约束。
fix: 待实施 - 两方面修复：(1) 在解析器中添加XML自动修复逻辑（尝试修复常见格式问题），(2) 在提示词中加强对XML格式正确性的约束
verification:
files_changed: []
