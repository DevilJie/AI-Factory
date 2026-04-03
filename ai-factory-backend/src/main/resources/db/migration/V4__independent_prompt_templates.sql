-- V4__independent_prompt_templates.sql
-- 世界观生成任务拆分 - 创建3个独立提示词模板，精简原有统一模板
-- 地理环境、力量体系、阵营势力各创建独立模板，原世界观模板仅保留世界类型/背景/时间线/规则

SET NAMES utf8mb4;

-- ============================================================
-- Template 1: llm_geography_create (PROMPT-01)
-- 地理环境独立生成模板
-- ============================================================

-- Step 1: Insert master record
INSERT INTO ai_prompt_template (template_code, template_name, service_type, scenario, current_version_id, description, tags, is_active, is_system)
VALUES ('llm_geography_create', '地理环境独立生成', 'llm', 'worldview_geography_generate', 0, '独立生成地理环境设定，输出<g>标签格式XML', '地理环境,世界观,独立生成', 1, 1);

-- Step 2: Insert version record with geography template content
INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, version_comment, is_active)
VALUES (
  (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_geography_create'),
  1,
  '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以下项目描述，为这部小说创建地理环境设定。

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

请严格按照以下XML格式返回（使用简化标签节省token）：
<g>
  <r><n>大陆/星球名称</n><d><![CDATA[整体描述]]></d>
    <r><n>子区域名称</n><d><![CDATA[区域描述]]></d></r>
  </r>
</g>

【地理环境XML格式详解】
<g>标签下使用<r>标签表示区域节点，每个区域包含<n>名称和<d>描述子标签，支持无限层级嵌套：
<g>
  <r>
    <n>九洲大陆</n>
    <d><![CDATA[灵气最盛的大陆，仙门林立，修行文明高度发达。总面积约三千万平方公里，四面环海。]]></d>
    <r>
      <n>中土神州</n>
      <d><![CDATA[九洲大陆核心区域，灵气浓度最高，三大仙门均坐落于此。]]></d>
      <r><n>太清山</n><d><![CDATA[太清宗所在地，万丈高峰直插云霄，山间灵气氤氲如海。]]></d></r>
      <r><n>玉虚城</n><d><![CDATA[玉虚殿所在，建城已有万年，是修仙界最大的交易枢纽。]]></d></r>
    </r>
    <r><n>东极青洲</n><d><![CDATA[妖族势力范围，原始密林覆盖，灵兽横行。]]></d></r>
    <r><n>南荒炎洲</n><d><![CDATA[散修与魔道活跃之地，环境恶劣但遗迹众多。]]></d></r>
  </r>
  <r><n>无尽星海</n><d><![CDATA[九洲大陆之外的无尽海域，充斥虚空乱流。]]></d></r>
</g>

要求：
- 至少生成2-3个顶级区域（大陆/星球）
- 每个顶级区域下至少有2-3个子区域
- 子区域可以继续嵌套更深层级
- 名称放在<n>子标签中，描述放在<d>子标签中
- 描述要结合世界背景，合理且详细

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记，直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据',
  '[{"name":"projectDescription","type":"string","desc":"项目描述","required":true},{"name":"storyTone","type":"string","desc":"故事基调","required":true},{"name":"storyGenre","type":"string","desc":"故事类型","required":true},{"name":"tagsSection","type":"string","desc":"标签信息","required":false}]',
  '从统一模板中提取地理环境部分',
  1
);

-- Step 3: Update master record's current_version_id
UPDATE ai_prompt_template
SET current_version_id = (SELECT MAX(id) FROM ai_prompt_template_version
  WHERE template_id = (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_geography_create'))
WHERE template_code = 'llm_geography_create';

-- ============================================================
-- Template 2: llm_power_system_create (PROMPT-02)
-- 力量体系独立生成模板
-- ============================================================

-- Step 1: Insert master record
INSERT INTO ai_prompt_template (template_code, template_name, service_type, scenario, current_version_id, description, tags, is_active, is_system)
VALUES ('llm_power_system_create', '力量体系独立生成', 'llm', 'worldview_power_system_generate', 0, '独立生成力量体系设定，输出<p>标签格式XML', '力量体系,世界观,独立生成', 1, 1);

-- Step 2: Insert version record with power system template content
INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, version_comment, is_active)
VALUES (
  (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_power_system_create'),
  1,
  '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以上项目描述，为这部小说创建力量体系设定。支持多套修炼体系，包含等级划分、境界划分、突破条件等详细信息。

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

请严格按照以下XML格式返回（使用简化标签节省token）：
<p>
  <ss>
    <name>体系名称（如：修仙）</name>
    <sf>能量来源（如：天地灵气）</sf>
    <cr>核心资源（如：灵石）</cr>
    <cm>修炼方式（如：打坐冥想）</cm>
    <d><![CDATA[体系整体描述，体系战斗方式，侧重点，比如主修神通书法，或者主修肉身不灭等]]></d>
    <lls>
      <ll>
        <ln>大境界名称（如：练气期、筑基期、元婴期等）</ln>
        <dd><![CDATA[等级描述，能做什么]]></dd>
        <bc><![CDATA[突破到下一等级的条件]]></bc>
        <lsp>寿命范围（如：约150年）</lsp>
        <pr><![CDATA[战力描述]]></pr>
        <la>标志性能力（如：灵气外放）</la>
        <steps>
          <step>每个大境界划分的小境界，比如初期、中期、后期，每一个大境界可以有多个小境界</step>
        </steps>
      </ll>
    </lls>
  </ss>
</p>

力量体系规则：
1. <p>标签下可以包含多个<ss>，代表多套修炼体系
2. 每个体系的大境界要尽可能完善，小境界也要合理
3. 每个修炼体系之间相对应，要有相同的大境界数量
4. 对应大境界等级相同则实力相似，只是侧重点不一样
5. 如果故事类型不需要力量体系，<p>标签可以为空

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记，直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据

【严禁出现的情况】
每个修炼体系大境界对应的小境界，严禁出现一行文字概括的情况，比如"一层到九层"，必须具体列出第一层、第二层...第九层',
  '[{"name":"projectDescription","type":"string","desc":"项目描述","required":true},{"name":"storyTone","type":"string","desc":"故事基调","required":true},{"name":"storyGenre","type":"string","desc":"故事类型","required":true},{"name":"tagsSection","type":"string","desc":"标签信息","required":false}]',
  '从统一模板中提取力量体系部分',
  1
);

-- Step 3: Update master record's current_version_id
UPDATE ai_prompt_template
SET current_version_id = (SELECT MAX(id) FROM ai_prompt_template_version
  WHERE template_id = (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_power_system_create'))
WHERE template_code = 'llm_power_system_create';

-- ============================================================
-- Template 3: llm_faction_create (PROMPT-03)
-- 阵营势力独立生成模板（需要已生成的地理环境和力量体系数据作为上下文）
-- ============================================================

-- Step 1: Insert master record
INSERT INTO ai_prompt_template (template_code, template_name, service_type, scenario, current_version_id, description, tags, is_active, is_system)
VALUES ('llm_faction_create', '阵营势力独立生成', 'llm', 'worldview_faction_generate', 0, '独立生成阵营势力设定，输出<f>标签格式XML，需要已生成的地理环境和力量体系数据作为上下文', '阵营势力,世界观,独立生成', 1, 1);

-- Step 2: Insert version record with faction template content
INSERT INTO ai_prompt_template_version (template_id, version_number, template_content, variable_definitions, version_comment, is_active)
VALUES (
  (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_faction_create'),
  1,
  '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以上项目描述和已有的地理环境、力量体系设定，为这部小说创建阵营势力设定。势力分布要与地理环境和力量体系紧密关联。

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

以下是本项目已生成的地理环境设定（AI看到的数据格式与生成时一致）：
<existing_geography>
{geographyContext}
</existing_geography>

以下是本项目已生成的力量体系设定：
<existing_power_systems>
{powerSystemContext}
</existing_power_systems>

请严格按照以下XML格式返回（使用简化标签节省token）：
<f>
  <faction>
    <n>势力名称</n>
    <type>势力类型（正派/反派/中立）</type>
    <power>力量体系名称</power>
    <regions>活动地区（逗号分隔）</regions>
    <d><![CDATA[势力描述]]></d>
    <relation><target>目标势力名称</target><type>关系类型</type></relation>
    <faction>
      <n>子势力名称</n>
      <d><![CDATA[子势力描述]]></d>
    </faction>
  </faction>
</f>

势力规则：
1. 顶级势力必须设置type（正派/反派/中立）和power
2. 子势力不写type和power，自动继承顶级势力
3. <power>中的名称必须与已有力量体系名称完全一致
4. <regions>中的名称必须与已有地理环境区域名称完全一致
5. 每个势力应有独特且符合题材的名称
6. 势力间关系使用<relation>标签对表示

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记，直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据',
  '[{"name":"projectDescription","type":"string","desc":"项目描述","required":true},{"name":"storyTone","type":"string","desc":"故事基调","required":true},{"name":"storyGenre","type":"string","desc":"故事类型","required":true},{"name":"tagsSection","type":"string","desc":"标签信息","required":false},{"name":"geographyContext","type":"string","desc":"已生成的地理环境XML数据","required":true},{"name":"powerSystemContext","type":"string","desc":"已生成的力量体系XML数据","required":true}]',
  '从统一模板中提取阵营势力部分，增加地理环境和力量体系上下文注入',
  1
);

-- Step 3: Update master record's current_version_id
UPDATE ai_prompt_template
SET current_version_id = (SELECT MAX(id) FROM ai_prompt_template_version
  WHERE template_id = (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_faction_create'))
WHERE template_code = 'llm_faction_create';

-- ============================================================
-- Template 4: llm_worldview_create UPDATE (PROMPT-04)
-- 精简统一世界观模板，移除地理环境/力量体系/阵营势力生成指令
-- ============================================================

UPDATE ai_prompt_template_version
SET template_content = '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以上项目描述，为这部小说创建世界观基础设定（地理环境、力量体系、阵营势力将在其他任务中独立生成）。

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

请严格按照以下XML格式返回（使用简化标签节省token）：
<w>
  <t>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</t>
  <b><![CDATA[世界背景描述（200-300字）]]></b>
  <l><![CDATA[时间线设定]]></l>
  <r><![CDATA[世界的基本规则和限制]]></r>
</w>

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记，直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据

内容要求：
1. 世界观要符合故事类型和基调
2. 世界背景描述要详细、生动，为后续的地理环境、力量体系、阵营势力生成提供充分的基础
3. 时间线设定要清晰，包含关键历史节点
4. 世界规则要合理，有内在逻辑和限制
5. 返回的必须是纯XML格式，不要有任何其他说明文字',
    version_comment = '精简世界观模板，移除地理环境/力量体系/阵营势力生成指令（已拆分为独立模板）'
WHERE template_id = (SELECT id FROM ai_prompt_template WHERE template_code = 'llm_worldview_create')
  AND is_active = 1;
