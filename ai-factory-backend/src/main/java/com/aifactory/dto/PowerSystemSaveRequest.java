package com.aifactory.dto;
import lombok.Data;
import java.util.List;

@Data
public class PowerSystemSaveRequest {
    private Long id;
    private Long projectId;
    private String name;
    private String sourceFrom;
    private String coreResource;
    private String cultivationMethod;
    private String description;
    private List<PowerSystemLevelSaveRequest> levels;
}
