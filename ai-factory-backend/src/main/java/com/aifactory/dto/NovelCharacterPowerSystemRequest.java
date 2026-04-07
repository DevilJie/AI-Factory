package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request DTO for creating/updating character-power system association
 *
 * @Author AI-Factory
 * @Date 2026-04-06
 */
@Data
@Schema(description = "角色-力量体系关联请求")
public class NovelCharacterPowerSystemRequest {

    @Schema(description = "力量体系ID", required = true, example = "1")
    private Long powerSystemId;

    @Schema(description = "当前境界ID (novel_power_system_level.id)", example = "3")
    private Long currentRealmId;

    @Schema(description = "当前子境界ID (novel_power_system_level_step.id)", example = "5")
    private Long currentSubRealmId;
}
