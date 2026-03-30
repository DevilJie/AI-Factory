-- power_system_migration.sql
-- 力量体系重构迁移脚本

SET NAMES utf8mb4;
CREATE TABLE `novel_power_system` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `project_id` bigint NULL DEFAULT NULL COMMENT '归属项目ID',
                                      `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '体系名称',
                                      `source_from` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '能量来源',
                                      `core_resource` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '核心资源',
                                      `cultivation_method` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修炼方式',
                                      `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '体系整体描述',
                                      `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '力量体系表' ROW_FORMAT = Dynamic;

-- 2. 新建体系等级表
CREATE TABLE `novel_power_system_level` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `power_system_id` bigint NOT NULL COMMENT '关联力量体系ID',
                                            `level` int NULL DEFAULT NULL COMMENT '等级索引（1开始，越小越低）',
                                            `level_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级名称',
                                            `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '等级描述',
                                            `breakthrough_condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '突破条件',
                                            `lifespan` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '寿命范围',
                                            `power_range` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '战力描述',
                                            `landmark_ability` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标志性能力',
                                            `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            PRIMARY KEY (`id`) USING BTREE,
                                            INDEX `idx_power_system_id`(`power_system_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '体系等级表' ROW_FORMAT = Dynamic;

-- 3. 新建等级境界表
CREATE TABLE `novel_power_system_level_step` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `power_system_level_id` bigint NOT NULL COMMENT '关联等级ID',
                                                 `level` int NULL DEFAULT NULL COMMENT '境界序号（1开始，越小越低）',
                                                 `level_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '境界名称',
                                                 `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                 PRIMARY KEY (`id`) USING BTREE,
                                                 INDEX `idx_power_system_level_id`(`power_system_level_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '等级境界表' ROW_FORMAT = Dynamic;

-- 4. 新建世界观-力量体系关联表
CREATE TABLE `novel_worldview_power_system` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `worldview_id` bigint NOT NULL COMMENT '世界观ID',
                                                `power_system_id` bigint NOT NULL COMMENT '力量体系ID',
                                                PRIMARY KEY (`id`) USING BTREE,
                                                INDEX `idx_worldview_id`(`worldview_id` ASC) USING BTREE,
                                                INDEX `idx_power_system_id`(`power_system_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '世界观-力量体系关联表' ROW_FORMAT = Dynamic;

-- 5. 移除世界观表的power_system字段
ALTER TABLE `novel_worldview` DROP COLUMN `power_system`;

-- 6. 更新世界观提示词模板（id=3）
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
3. 地理环境（大陆、国家、重要地点）
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
      <d><![CDATA[体系整体描述]]></d>
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
  <g><![CDATA[地理环境描述]]></g>
  <f><![CDATA[势力分布描述]]></f>
  <l><![CDATA[时间线设定]]></l>
  <r><![CDATA[世界的基本规则和限制]]></r>
</w>

【XML格式要求】
1. 对于长文本内容（可能包含特殊字符），请使用CDATA标签包裹：<![CDATA[内容]]>
2. 不要包含markdown代码块标记（```xml），直接返回XML
3. 不要包含任何解释或说明文字，只返回XML数据
4. 力量体系<p>标签下可以包含多个<system>，代表多套修炼体系
5. 每个体系的大境界要尽可能完善，然后对应的小境界也要合理，小境界一定要具体到每一个等级，不要一行文字概括，比如 "一到九层"
6. 每个修炼体系之间都是相对应的，对应大境界等级相同则实力相似，只是侧重点不一样，比如修仙注重神通术法，炼体则注重肉体强横等
7. 如果故事类型不需要力量体系（如纯都市日常），<p>标签可以为空

内容要求：
1. 世界观要符合故事类型和基调
2. 力量体系要清晰、合理，有可发展性，等级之间要有明显的实力差距
3. 各个要素之间要相互关联，形成完整的世界
4. 返回的必须是纯XML格式，不要有任何其他说明文字',
    version_comment = '力量体系结构化重构，支持XML子节点格式的多套修炼体系',
    variable_definitions = '[{"desc":"项目描述","name":"projectDescription","type":"string","required":true},{"desc":"故事类型","name":"storyGenre","type":"string","required":true},{"desc":"故事基调","name":"storyTone","type":"string","required":true},{"desc":"标签信息","name":"tagsSection","type":"string","required":false}]'
WHERE id = 3;