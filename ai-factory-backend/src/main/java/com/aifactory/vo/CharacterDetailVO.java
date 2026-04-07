package com.aifactory.vo;

import com.aifactory.entity.NovelCharacter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Character detail VO with aggregated power system and faction associations
 *
 * Extends basic NovelCharacter fields with structured association data
 * from novel_character_power_system and novel_faction_character tables.
 *
 * @Author AI-Factory
 * @Date 2026-04-06
 */
@Data
@Schema(description = "角色详情VO，包含基础信息和关联的力量体系/势力数据")
public class CharacterDetailVO {

    // Basic character fields
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "年龄")
    private String age;

    @Schema(description = "角色类型")
    private String roleType;

    @Schema(description = "角色定位")
    private String role;

    @Schema(description = "性格特点")
    private String personality;

    @Schema(description = "外貌描述")
    private String appearance;

    @Schema(description = "图像生成描述词")
    private String appearancePrompt;

    @Schema(description = "背景故事")
    private String background;

    @Schema(description = "能力/技能")
    private String abilities;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Aggregated association data
    @Schema(description = "关联的力量体系列表")
    private List<PowerSystemAssociation> powerSystemAssociations;

    @Schema(description = "关联的势力列表")
    private List<FactionAssociation> factionAssociations;

    /**
     * Power system association embedded object
     */
    @Data
    @Schema(description = "力量体系关联信息")
    public static class PowerSystemAssociation {
        @Schema(description = "关联记录ID")
        private Long id;

        @Schema(description = "力量体系ID")
        private Long powerSystemId;

        @Schema(description = "力量体系名称")
        private String powerSystemName;

        @Schema(description = "当前境界ID")
        private Long currentRealmId;

        @Schema(description = "当前境界名称")
        private String currentRealmName;

        @Schema(description = "当前子境界ID")
        private Long currentSubRealmId;

        @Schema(description = "当前子境界名称")
        private String currentSubRealmName;
    }

    /**
     * Faction association embedded object
     */
    @Data
    @Schema(description = "势力关联信息")
    public static class FactionAssociation {
        @Schema(description = "关联记录ID")
        private Long id;

        @Schema(description = "势力ID")
        private Long factionId;

        @Schema(description = "势力名称")
        private String factionName;

        @Schema(description = "在势力中的职位/角色")
        private String role;
    }

    /**
     * Create from NovelCharacter entity, copying all basic fields
     */
    public static CharacterDetailVO fromCharacter(NovelCharacter character) {
        CharacterDetailVO vo = new CharacterDetailVO();
        vo.setId(character.getId());
        vo.setProjectId(character.getProjectId());
        vo.setName(character.getName());
        vo.setAvatar(character.getAvatar());
        vo.setGender(character.getGender());
        vo.setAge(character.getAge());
        vo.setRoleType(character.getRoleType());
        vo.setRole(character.getRole());
        vo.setPersonality(character.getPersonality());
        vo.setAppearance(character.getAppearance());
        vo.setAppearancePrompt(character.getAppearancePrompt());
        vo.setBackground(character.getBackground());
        vo.setAbilities(character.getAbilities());
        vo.setTags(character.getTags());
        vo.setCreateTime(character.getCreateTime());
        vo.setUpdateTime(character.getUpdateTime());
        return vo;
    }
}
