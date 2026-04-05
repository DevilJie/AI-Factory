---
status: awaiting_human_verify
trigger: "power-system-not-generated: 在一次性生成世界观时，力量体系（power system）LLM 有返回但数据未生成到数据库。其他模块（地理环境、势力阵营）正常。"
created: 2026-04-04T00:00:00Z
updated: 2026-04-04T00:20:00Z
---

## Current Focus

hypothesis: CONFIRMED - PowerSystemTaskStrategy.savePowerSystem() 使用 XmlParser 将 <p> 根元素 XML 解析为 WorldSettingXmlDto (@root=<w>)，根元素不匹配导致 systems=null
test: FIXED - 使用 WorldviewXmlParser.parsePowerSystemXml() DOM 解析替代 Jackson XmlParser
expecting: 力量体系数据能正确解析并保存到数据库
next_action: 等待用户验证

## Symptoms

expected: 一次性生成世界观时，力量体系应该像地理环境一样被正确解析并保存到数据库
actual: LLM 返回了力量体系的数据，但最终数据库里没有生成力量体系记录
errors: 力量体系解析结果为空 (systems=null)，无异常抛出
reproduction: 在前端一次性生成世界观，观察力量体系是否生成
started: 一直存在此问题

## Eliminated

## Evidence

- timestamp: 2026-04-04T00:01:00Z
  checked: logs/ai-factory.log 力量体系相关日志
  found: "AI生成力量体系完成, projectId=35, 响应长度=7830" -> "XML解析成功" -> "力量体系解析结果为空" -> "世界观-力量体系关联建立完成, systemCount=0"
  implication: LLM 确实返回了 7830 字符的数据，但 XmlParser 解析后 systems 为 null

- timestamp: 2026-04-04T00:03:00Z
  checked: PowerSystemTaskStrategy.savePowerSystem() 第190行 - 使用 xmlParser.parse(aiResponse, WorldSettingXmlDto.class)
  found: WorldSettingXmlDto 使用 @JacksonXmlRootElement(localName = "w")，但 llm_power_system_create 提示词模板要求返回 <p> 根元素 XML
  implication: 根元素不匹配！<p> XML 被解析为期望 <w> 的 DTO，Jackson 静默返回空对象，systems=null

- timestamp: 2026-04-04T00:05:00Z
  checked: 对比 GeographyTaskStrategy 的做法 - 它使用 WorldviewXmlParser.parseGeographyXml() 做 DOM 解析
  found: 地理环境用 DOM 解析提取 <g> 标签内容，不走 Jackson XmlParser；力量体系却用 Jackson XmlParser 解析 <p> 标签
  implication: 力量体系应该改用 DOM 解析（与地理环境一致），或为 <p> 根元素创建专用 DTO

- timestamp: 2026-04-04T00:15:00Z
  checked: 编译和测试
  found: mvn compile 成功，全部54个测试通过 (0 failures, 0 errors)
  implication: 修复代码编译正确且未引入回归

## Resolution

root_cause: PowerSystemTaskStrategy.savePowerSystem() 使用 XmlParser(Jackson XML) 解析 AI 响应，将 <p> 根元素的 XML 解析为 WorldSettingXmlDto（期望 <w> 根元素）。根元素名不匹配导致 Jackson 静默返回空对象，systems 字段为 null，所有力量体系数据被丢弃。
fix: 在 WorldviewXmlParser 中添加 parsePowerSystemXml() DOM 解析方法（与 parseGeographyXml 模式一致），修改 PowerSystemTaskStrategy.savePowerSystem() 使用 DOM 解析替代 Jackson XmlParser。所有54个测试通过。
verification: 编译通过，全部测试通过，等待用户端到端验证
files_changed:
  - ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java
  - ai-factory-backend/src/main/java/com/aifactory/service/task/impl/PowerSystemTaskStrategy.java
