package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Character-PowerSystem association entity
 *
 * Join table linking characters to their power systems with current realm tracking.
 * Mirrors the novel_faction_character pattern for simple association tables.
 *
 * @Author AI-Factory
 * @Date 2026-04-06
 */
@Data
@TableName("novel_character_power_system")
public class NovelCharacterPowerSystem {

    /** Primary key */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Character ID (novel_character.id) */
    private Long characterId;

    /** Power system ID (novel_power_system.id) */
    private Long powerSystemId;

    /** Current realm level (novel_power_system_level.id) */
    private Long currentRealmId;

    /** Current sub-realm step (novel_power_system_level_step.id) */
    private Long currentSubRealmId;
}
