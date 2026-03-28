package com.aifactory.service.task;

import com.aifactory.dto.TaskDto;
import com.aifactory.dto.TaskStepDto;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 任务策略接口
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
public interface TaskStrategy {

    /**
     * 获取任务类型
     */
    String getTaskType();

    /**
     * 创建任务步骤
     *
     * @param task 任务
     * @return 步骤配置列表
     */
    List<StepConfig> createSteps(AiTask task);

    /**
     * 执行单个步骤
     *
     * @param step 步骤
     * @param context 上下文
     * @return 步骤执行结果
     */
    StepResult executeStep(AiTaskStep step, TaskContext context);

    /**
     * 计算总进度
     *
     * @param currentStep 当前步骤
     * @param allSteps 所有步骤
     * @return 进度 0-100
     */
    default int calculateProgress(AiTaskStep currentStep, List<AiTaskStep> allSteps) {
        if (allSteps == null || allSteps.isEmpty()) {
            return 0;
        }

        int totalSteps = allSteps.size();
        int completedSteps = 0;
        int currentStepIndex = 0;

        for (int i = 0; i < totalSteps; i++) {
            AiTaskStep step = allSteps.get(i);
            if ("completed".equals(step.getStatus()) || "skipped".equals(step.getStatus())) {
                completedSteps++;
            }
            if (step.getId().equals(currentStep.getId())) {
                currentStepIndex = i;
            }
        }

        // 基础进度：已完成的步骤
        int baseProgress = (completedSteps * 100) / totalSteps;

        // 当前步骤的进度贡献
        int currentStepProgress = currentStep.getProgress() != null ? currentStep.getProgress() : 0;
        int stepProgress = currentStepProgress / totalSteps;

        return Math.min(100, baseProgress + stepProgress);
    }

    /**
     * 步骤配置
     */
    class StepConfig {
        private Integer order;
        private String name;
        private String type;
        private Map<String, Object> config;

        public StepConfig(Integer order, String name, String type, Map<String, Object> config) {
            this.order = order;
            this.name = name;
            this.type = type;
            this.config = config;
        }

        public Integer getOrder() { return order; }
        public String getName() { return name; }
        public String getType() { return type; }
        public Map<String, Object> getConfig() { return config; }
    }

    /**
     * 步骤执行结果
     */
    class StepResult {
        private boolean success;
        private Object result;
        private String errorMessage;
        private Integer progress;

        public StepResult(boolean success, Object result, String errorMessage, Integer progress) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.progress = progress;
        }

        public static StepResult success(Object result, Integer progress) {
            return new StepResult(true, result, null, progress);
        }

        public static StepResult failure(String errorMessage) {
            return new StepResult(false, null, errorMessage, 0);
        }

        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public Integer getProgress() { return progress; }
    }

    /**
     * 任务上下文
     */
    class TaskContext {
        private Long taskId;
        private Long projectId;
        private String taskType;
        private JsonNode config;
        private Map<String, Object> sharedData;

        public TaskContext(Long taskId, Long projectId, String taskType, JsonNode config, Map<String, Object> sharedData) {
            this.taskId = taskId;
            this.projectId = projectId;
            this.taskType = taskType;
            this.config = config;
            this.sharedData = sharedData;
        }

        public Long getTaskId() { return taskId; }
        public Long getProjectId() { return projectId; }
        public String getTaskType() { return taskType; }
        public JsonNode getConfig() { return config; }
        public Map<String, Object> getSharedData() { return sharedData; }
        public void putSharedData(String key, Object value) { sharedData.put(key, value); }
        public Object getSharedData(String key) { return sharedData.get(key); }
    }
}
