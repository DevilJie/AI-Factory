package com.aifactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request DTO for creating/updating character-faction association
 *
 * @Author AI-Factory
 * @Date 2026-04-06
 */
@Data
@Schema(description = "角色-势力关联请求")
public class CharacterFactionRequest {

    @Schema(description = "势力ID", required = true, example = "1")
    private Long factionId;

    @Schema(description = "在势力中的职位/角色", example = "长老")
    private String role;
}
