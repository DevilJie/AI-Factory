package com.aifactory.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 章节实际登场角色VO
 * 用于前端对比视图获取章节中实际出场的角色列表
 */
@Data
@Builder
public class ChapterCharacterVO {
    private Long characterId;
    private String characterName;
    private String roleType;
    private String importanceLevel;
}
