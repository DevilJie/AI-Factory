package com.aifactory.service.task.validator;

import com.aifactory.entity.NovelChapterPlan;
import com.aifactory.entity.NovelVolumePlan;
import com.aifactory.mapper.NovelChapterPlanMapper;
import com.aifactory.mapper.NovelVolumePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 分卷完成状态验证器
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component
public class VolumeCompletionValidator {

    @Autowired
    private NovelChapterPlanMapper chapterPlanMapper;

    @Autowired
    private NovelVolumePlanMapper volumePlanMapper;

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, error);
        }

        public boolean isValid() {
            return valid;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * 检查分卷是否完成（所有4个阶段都完成）
     */
    public boolean isVolumeCompleted(Long volumeId) {
        List<String> stages = Arrays.asList("introduction", "development", "climax", "conclusion");

        for (String stage : stages) {
            // 检查每个阶段是否有章节且标记为完成
            Long count = chapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getVolumePlanId, volumeId)
                    .eq(NovelChapterPlan::getPlotStage, stage)
                    .eq(NovelChapterPlan::getStageCompleted, true)
            );

            if (count == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 验证是否可以在分卷生成章节
     */
    public ValidationResult canGenerateInVolume(Long projectId, Long volumeId) {
        // 获取所有分卷，按卷号排序
        List<NovelVolumePlan> volumes = volumePlanMapper.selectList(
            new LambdaQueryWrapper<NovelVolumePlan>()
                .eq(NovelVolumePlan::getProjectId, projectId)
                .orderByAsc(NovelVolumePlan::getVolumeNumber)
        );

        int currentVolumeNumber = 0;
        for (NovelVolumePlan volume : volumes) {
            if (volume.getId().equals(volumeId)) {
                currentVolumeNumber = volume.getVolumeNumber();
                break;
            }
        }

        // 检查前一卷是否完成
        if (currentVolumeNumber > 1) {
            final int targetVolumeNumber = currentVolumeNumber - 1;
            Long previousVolumeId = volumes.stream()
                .filter(v -> v.getVolumeNumber() == targetVolumeNumber)
                .map(NovelVolumePlan::getId)
                .findFirst()
                .orElse(null);

            if (previousVolumeId != null && !isVolumeCompleted(previousVolumeId)) {
                return ValidationResult.failure("上一分卷尚未完成，请先完成上一分卷的所有阶段");
            }
        }

        return ValidationResult.success();
    }
}
