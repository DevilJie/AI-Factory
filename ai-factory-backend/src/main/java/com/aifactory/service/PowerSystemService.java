package com.aifactory.service;

import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.NovelPowerSystem;
import java.util.List;

public interface PowerSystemService {
    List<NovelPowerSystem> listByProjectId(Long projectId);
    NovelPowerSystem getById(Long id);
    NovelPowerSystem savePowerSystem(PowerSystemSaveRequest request);
    void deleteById(Long id);
    String buildPowerSystemConstraint(Long projectId);
}
