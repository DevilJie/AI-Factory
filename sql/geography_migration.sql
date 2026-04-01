-- geography_migration.sql
-- 地理环境结构化重构迁移脚本

SET NAMES utf8mb4;

-- 1. 新建大陆区域表（树形结构）
CREATE TABLE `novel_continent_region` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID（NULL表示根节点）',
    `deep` int NOT NULL DEFAULT 0 COMMENT '树层级深度（0=根节点）',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '同级排序（越小越靠前）',
    `project_id` bigint NOT NULL COMMENT '归属项目ID',
    `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区域名称',
    `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '区域描述',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
    INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '大陆区域表（树形结构）' ROW_FORMAT = Dynamic;

-- 2. 移除世界观表的geography字段（地理信息已迁移到novel_continent_region表）
ALTER TABLE `novel_worldview` DROP COLUMN `geography`;

-- 3. 更新世界观提示词模板（id=3）—— 地理环境改为结构化XML
UPDATE ai_prompt_template_version
SET template_content = '你是一位资深的网文世界观架构师，擅长构建宏大、自洽、富有吸引力的虚构世界。

根据以下项目描述，为这部小说创建完整的世界观设定：

【项目描述】
{projectDescription}

【故事类型】{storyGenre}
【故事基调】{storyTone}
{tagsSection}

请创建包含以下内容的世界观设定：
1. 世界类型（现实/奇幻/科幻/修真等）
2. 力量体系（支持多套修炼体系，包含等级划分、境界划分、突破条件等详细信息）
3. 地理环境（大陆、国家、重要地点，使用树形结构，支持多层级嵌套）
4. 势力分布（阵营、组织、重要势力）
5. 核心设定（与故事相关的特殊规则）

【重要】请严格按照以下XML格式返回世界观设定（使用简化标签节省token）：
<w>
  <t>世界类型（如：架空/现代/古代/未来/玄幻/仙侠等）</t>
  <b><![CDATA[世界背景描述（200-300字）]]></b>
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
  <g>
    <r name="大陆/星球名称"><d><![CDATA[整体描述]]></d></r>
    <r name="大陆/星球名称2"><d><![CDATA[整体描述2]]></d></r>
  </g>
  <f><![CDATA[势力分布描述]]></f>
  <l><![CDATA[时间线设定]]></l>
  <r><![CDATA[世界的基本规则和限制]]></r>
</w>

【地理环境XML格式详解】
<g>标签下使用<r>标签表示区域节点，描述放在<d>子标签中，支持无限层级嵌套：
<g>
  <r name="九洲大陆">
    <d><![CDATA[灵气最盛的大陆，仙门林立，修行文明高度发达。总面积约三千万平方公里，四面环海。]]></d>
    <r name="中土神州">
      <d><![CDATA[九洲大陆核心区域，灵气浓度最高，三大仙门均坐落于此。]]></d>
      <r name="太清山"><d><![CDATA[太清宗所在地，万丈高峰直插云霄，山间灵气氤氲如海。]]></d></r>
      <r name="玉虚城"><d><![CDATA[玉虚殿所在，建城已有万年，是修仙界最大的交易枢纽。]]></d></r>
    </r>
    <r name="东极青洲"><d><![CDATA[妖族势力范围，原始密林覆盖，灵兽横行。]]></d></r>
    <r name="南荒炎洲"><d><![CDATA[散修与魔道活跃之地，环境恶劣但遗迹众多。]]></d></r>
  </r>
  <r name="无尽星海"><d><![CDATA[九洲大陆之外的无尽海域，充斥虚空乱流。]]></d></r>
</g>
要求：
- 至少生成2-3个顶级区域（大陆/星球）
- 每个顶级区域下至少有2-3个子区域
- 子区域可以继续嵌套更深层级
- name属性为区域名称，描述必须放在<d>子标签中，不要直接写在<r>标签内
- 描述要结合世界背景，合理且详细

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记（```xml），直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据
4. 力量体系<p>标签下可以包含多个<system>，代表多套修炼体系
5. 每个体系的大境界要尽可能完善，然后对应的小境界也要合理
6. 每个修炼体系之间都是相对应的，要有相同的大境界数量
7. 对应大境界等级相同则实力相似，只是侧重点不一样，比如修仙注重神通术法，炼体则注重肉体强横等
8. 如果故事类型不需要力量体系（如纯都市日常），<p>标签可以为空

【严禁出现的情况】
每个修炼体系大境界对应的小境界，严禁出现一行文字概括的情况，比如 "一层到九层"，一定要具体到 第一层、第二层...第九层

内容要求：
1. 世界观要符合故事类型和基调
2. 力量体系要清晰、合理，有可发展性，等级之间要有明显的实力差距
3. 各个要素之间要相互关联，形成完整的世界
4. 返回的必须是纯XML格式，不要有任何其他说明文字',
    version_comment = '地理环境结构化重构，支持树形嵌套的区域节点'
WHERE id = 3;
