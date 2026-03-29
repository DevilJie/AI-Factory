package com.aifactory.dto;
import lombok.Data;
import java.util.List;

@Data
public class PowerSystemLevelSaveRequest {
    private Long id;
    private Integer level;
    private String levelName;
    private String description;
    private String breakthroughCondition;
    private String lifespan;
    private String powerRange;
    private String landmarkAbility;
    private List<PowerSystemLevelStepSaveRequest> steps;
}
