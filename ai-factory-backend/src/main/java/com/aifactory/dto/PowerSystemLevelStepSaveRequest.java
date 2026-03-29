package com.aifactory.dto;
import lombok.Data;

@Data
public class PowerSystemLevelStepSaveRequest {
    private Long id;
    private Integer level;
    private String levelName;
}
