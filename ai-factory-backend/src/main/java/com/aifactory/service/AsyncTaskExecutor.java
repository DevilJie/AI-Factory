package com.aifactory.service;

import com.aifactory.common.UserContext;
import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.entity.AiTask;
import com.aifactory.entity.AiTaskStep;
import com.aifactory.enums.TaskStatus;
import com.aifactory.mapper.AiTaskMapper;
import com.aifactory.mapper.AiTaskStepMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.aifactory.service.task.TaskStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步任务执行器
 * 专门用于异步执行任务，确保@Async注解生效
 *
 * @Author CaiZy
 * @Date 2025-01-25
 */
@Slf4j
@Service
public class AsyncTaskExecutor {

    @Autowired
    private AiTaskMapper taskMapper;

    @Autowired
    private AiTaskStepMapper taskStepMapper;

    @Autowired
    private Map<String, TaskStrategy> strategyMap;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 将下划线命名转换为驼峰命名
     * 例如: volume_optimize -> volumeOptimize
     */
    private String toCamelCase(String snakeCase) {
        if (snakeCase == null || !snakeCase.contains("_")) {
            return snakeCase;
        }
        StringBuilder result = new StringBuilder();
        String[] parts = snakeCase.split("_");
        result.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    /**
     * 根据任务类型获取对应的策略
     */
    private TaskStrategy getStrategy(String taskType) {
        log.info(">>> 查找任务策略，taskType: {}, 可用策略: {}", taskType, strategyMap.keySet());

        // 先尝试驼峰命名: volume_optimize -> volumeOptimizeTaskStrategy
        String camelCaseName = toCamelCase(taskType) + "TaskStrategy";
        TaskStrategy strategy = strategyMap.get(camelCaseName);
        log.info(">>> 尝试驼峰命名: {} -> {}", camelCaseName, strategy != null ? "找到" : "未找到");

        if (strategy != null) {
            return strategy;
        }

        // 再尝试原始命名: volume_optimize -> volume_optimizeTaskStrategy
        strategy = strategyMap.get(taskType + "TaskStrategy");
        log.info(">>> 尝试原始命名: {}TaskStrategy -> {}", taskType, strategy != null ? "找到" : "未找到");

        if (strategy != null) {
            return strategy;
        }

        // 遍历所有策略，通过getTaskType()匹配
        for (TaskStrategy s : strategyMap.values()) {
            log.info(">>> 检查策略: {}, getTaskType: {}", s.getClass().getSimpleName(), s.getTaskType());
            if (taskType.equals(s.getTaskType())) {
                log.info(">>> 通过getTaskType匹配到策略: {}", s.getClass().getSimpleName());
                return s;
            }
        }

        log.warn("!!! 未找到匹配的策略，taskType: {}", taskType);
        return null;
    }

    /**
     * 异步执行任务
     */
    @Async("taskExecutor")
    public void executeTaskAsync(Long taskId, CreateTaskRequest request) {
        log.info(">>> 异步任务开始执行，任务ID: {}, 线程: {}", taskId, Thread.currentThread().getName());

        try {
            // 给一点时间让主线程的事务提交
            Thread.sleep(100);

            AiTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.error("!!! 任务不存在，任务ID: {} !!!", taskId);
                return;
            }

            // 设置用户上下文到异步线程，确保LLMProviderFactory能获取到userId
            UserContext.setUserId(task.getCreatedBy());

            log.info(">>> 任务查询成功，任务ID: {}, 状态: {}, 用户ID: {}", taskId, task.getStatus(), task.getCreatedBy());

            // 1. 创建任务步骤（如果还没有创建）
            List<AiTaskStep> steps = taskStepMapper.selectList(
                new LambdaQueryWrapper<AiTaskStep>()
                    .eq(AiTaskStep::getTaskId, taskId)
            );

            if (steps.isEmpty()) {
                // 获取对应的策略并创建步骤
                TaskStrategy strategy = getStrategy(request.getTaskType());
                if (strategy == null) {
                    strategy = strategyMap.get("outlineTaskStrategy");
                }

                if (strategy != null) {
                    List<TaskStrategy.StepConfig> stepConfigs = strategy.createSteps(task);

                    for (TaskStrategy.StepConfig stepConfig : stepConfigs) {
                        AiTaskStep step = new AiTaskStep();
                        step.setTaskId(task.getId());
                        step.setStepOrder(stepConfig.getOrder());
                        step.setStepName(stepConfig.getName());
                        step.setStepType(stepConfig.getType());
                        step.setConfigJson(objectMapper.writeValueAsString(stepConfig.getConfig()));
                        step.setStatus("pending");
                        step.setProgress(0);
                        step.setCreateTime(LocalDateTime.now());
                        step.setUpdateTime(LocalDateTime.now());
                        taskStepMapper.insert(step);
                    }
                }

                // 重新加载步骤
                steps = taskStepMapper.selectList(
                    new LambdaQueryWrapper<AiTaskStep>()
                        .eq(AiTaskStep::getTaskId, taskId)
                        .orderByAsc(AiTaskStep::getStepOrder)
                );
            }

            // 2. 更新任务状态为运行中
            task.setStatus(TaskStatus.RUNNING.getCode());
            task.setStartedTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);

            // 3. 获取对应的策略
            TaskStrategy strategy = getStrategy(task.getTaskType());
            if (strategy == null) {
                strategy = strategyMap.get("outlineTaskStrategy");
            }

            if (strategy == null) {
                throw new RuntimeException("找不到对应的任务策略: " + task.getTaskType());
            }

            // 4. 创建任务上下文
            Map<String, Object> sharedData = new ConcurrentHashMap<>();
            TaskStrategy.TaskContext context = new TaskStrategy.TaskContext(
                taskId,
                task.getProjectId(),
                task.getTaskType(),
                objectMapper.readTree(task.getConfigJson()),
                sharedData
            );

            // 执行每个步骤
            for (AiTaskStep step : steps) {
                // 检查任务是否被取消
                AiTask currentTask = taskMapper.selectById(taskId);
                if (TaskStatus.CANCELLED.getCode().equals(currentTask.getStatus())) {
                    log.info("任务已被取消，任务ID: {}", taskId);
                    return;
                }

                // 更新步骤状态为运行中
                step.setStatus("running");
                step.setProgress(50);
                step.setStartedTime(LocalDateTime.now());
                step.setUpdateTime(LocalDateTime.now());
                taskStepMapper.updateById(step);

                // 更新任务当前步骤
                task.setCurrentStep(step.getStepName());
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);

                try {
                    // 执行步骤
                    TaskStrategy.StepResult result = strategy.executeStep(step, context);

                    if (result.isSuccess()) {
                        // 步骤成功
                        step.setStatus("completed");
                        step.setProgress(100);
                        step.setResultJson(objectMapper.writeValueAsString(result.getResult()));
                        step.setCompletedTime(LocalDateTime.now());
                    } else {
                        // 步骤失败
                        step.setStatus("failed");
                        step.setErrorMessage(result.getErrorMessage());
                        step.setCompletedTime(LocalDateTime.now());

                        // 任务失败
                        task.setStatus(TaskStatus.FAILED.getCode());
                        task.setErrorMessage(result.getErrorMessage());
                        task.setCompletedTime(LocalDateTime.now());
                        taskMapper.updateById(task);
                        taskStepMapper.updateById(step);
                        return;
                    }

                } catch (Exception e) {
                    log.error("执行步骤失败: {}", step.getStepName(), e);
                    step.setStatus("failed");
                    step.setErrorMessage(e.getMessage());
                    step.setCompletedTime(LocalDateTime.now());

                    task.setStatus(TaskStatus.FAILED.getCode());
                    task.setErrorMessage(e.getMessage());
                    task.setCompletedTime(LocalDateTime.now());
                    taskMapper.updateById(task);
                    taskStepMapper.updateById(step);
                    return;
                }

                step.setUpdateTime(LocalDateTime.now());
                taskStepMapper.updateById(step);

                // 更新任务总进度
                int progress = strategy.calculateProgress(step, steps);
                task.setProgress(progress);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);
            }

            // 任务完成 - 获取最后一步的结果保存到任务主表
            AiTaskStep lastStep = steps.get(steps.size() - 1);
            if (lastStep.getResultJson() != null) {
                task.setResultJson(lastStep.getResultJson());
            }

            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setProgress(100);
            task.setCurrentStep("全部完成");
            task.setCompletedTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);

            log.info(">>> 任务执行完成，任务ID: {}", taskId);

        } catch (Exception e) {
            log.error("!!! 异步任务执行失败，任务ID: {}, 错误: {} !!!", taskId, e.getMessage(), e);

            try {
                AiTask task = taskMapper.selectById(taskId);
                if (task != null) {
                    task.setStatus(TaskStatus.FAILED.getCode());
                    task.setErrorMessage(e.getMessage());
                    task.setCompletedTime(LocalDateTime.now());
                    task.setUpdateTime(LocalDateTime.now());
                    taskMapper.updateById(task);
                    log.info(">>> 任务失败状态已更新，任务ID: {}", taskId);
                } else {
                    log.error("!!! 无法更新任务状态，任务不存在，任务ID: {} !!!", taskId);
                }
            } catch (Exception ex) {
                log.error("!!! 更新任务失败状态时出错，任务ID: {} !!!", taskId, ex);
            }
        } finally {
            // 清理用户上下文，防止内存泄漏
            UserContext.clear();
        }
    }
}
