package com.aifactory.service;

import com.aifactory.response.Result;
import com.aifactory.dto.CreateTaskRequest;
import com.aifactory.dto.TaskDto;
import com.aifactory.dto.TaskStepDto;
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
 * 异步任务Service
 *
 * @Author CaiZy
 * @Date 2025-01-24
 */
@Slf4j
@Service
public class TaskService {

    @Autowired
    private AiTaskMapper taskMapper;

    @Autowired
    private AiTaskStepMapper taskStepMapper;

    @Autowired
    private Map<String, TaskStrategy> strategyMap;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建任务（快速返回，后台异步执行）
     * 不使用事务，确保数据能够立即提交
     */
    public Result<TaskDto> createTask(CreateTaskRequest request) {
        try {
            Long userId = com.aifactory.common.UserContext.getUserId();

            // 1. 快速创建任务记录
            AiTask task = new AiTask();
            task.setProjectId(request.getProjectId());
            task.setTaskType(request.getTaskType());
            task.setTaskName(request.getTaskName());
            task.setConfigJson(objectMapper.writeValueAsString(request.getConfig()));
            task.setStatus(TaskStatus.PENDING.getCode());
            task.setProgress(0);
            task.setCreatedBy(userId);
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());

            taskMapper.insert(task);

            log.info(">>> 任务插入成功，生成的雪花ID: {}, 数据库实际ID: {}", task.getId(), task.getId());

            // 验证：立即查询回来确认ID是否一致
            AiTask insertedTask = taskMapper.selectById(task.getId());
            if (insertedTask != null) {
                log.info(">>> 验证查询成功，查询到的ID: {}, 任务名称: {}", insertedTask.getId(), insertedTask.getTaskName());
            } else {
                log.error("!!! 验证查询失败，无法用ID {} 查询到任务 !!!", task.getId());
            }

            log.info("创建任务成功，任务ID: {}, 任务类型: {}", task.getId(), task.getTaskType());

            // 2. 构建返回结果
            TaskDto result = buildTaskDto(task);

            // 3. 立即异步执行任务（通过专门的AsyncTaskExecutor确保真正异步）
            asyncTaskExecutor.executeTaskAsync(task.getId(), request);

            // 4. 立即返回结果（不等待异步任务完成）
            return Result.ok(result);

        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error("创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务状态
     */
    public Result<TaskDto> getTaskStatus(Long taskId) {
        try {
            AiTask task = taskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("任务不存在");
            }

            return Result.ok(buildTaskDto(task));

        } catch (Exception e) {
            log.error("获取任务状态失败", e);
            return Result.error("获取任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目任务列表
     */
    public Result<List<TaskDto>> getProjectTasks(Long projectId) {
        try {
            List<AiTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<AiTask>()
                    .eq(AiTask::getProjectId, projectId)
                    .orderByDesc(AiTask::getCreateTime)
            );

            List<TaskDto> dtos = new ArrayList<>();
            for (AiTask task : tasks) {
                dtos.add(buildTaskDto(task));
            }

            return Result.ok(dtos);

        } catch (Exception e) {
            log.error("获取项目任务列表失败", e);
            return Result.error("获取项目任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     * 不使用事务，确保数据能够立即提交
     */
    public Result<Void> cancelTask(Long taskId) {
        try {
            AiTask task = taskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("任务不存在");
            }

            if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus()) ||
                "cancelled".equals(task.getStatus())) {
                return Result.error("任务已结束，无法取消");
            }

            task.setStatus(TaskStatus.CANCELLED.getCode());
            task.setUpdateTime(LocalDateTime.now());
            task.setCompletedTime(LocalDateTime.now());
            taskMapper.updateById(task);

            // 取消所有等待中的步骤
            taskStepMapper.update(null,
                new LambdaUpdateWrapper<AiTaskStep>()
                    .eq(AiTaskStep::getTaskId, taskId)
                    .set(AiTaskStep::getStatus, "cancelled")
                    .set(AiTaskStep::getUpdateTime, LocalDateTime.now())
            );

            log.info("取消任务成功，任务ID: {}", taskId);
            return Result.ok();

        } catch (Exception e) {
            log.error("取消任务失败", e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }

    /**
     * 构建任务DTO
     */
    private TaskDto buildTaskDto(AiTask task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setProjectId(task.getProjectId());
        dto.setTaskType(task.getTaskType());
        dto.setTaskName(task.getTaskName());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setCurrentStep(task.getCurrentStep());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setCreateTime(task.getCreateTime());
        dto.setUpdateTime(task.getUpdateTime());
        dto.setStartedTime(task.getStartedTime());
        dto.setCompletedTime(task.getCompletedTime());

        try {
            if (task.getResultJson() != null) {
                dto.setResult(objectMapper.readTree(task.getResultJson()));
            }
        } catch (Exception e) {
            log.warn("解析任务结果失败", e);
        }

        // 获取步骤列表
        List<AiTaskStep> steps = taskStepMapper.selectList(
            new LambdaQueryWrapper<AiTaskStep>()
                .eq(AiTaskStep::getTaskId, task.getId())
                .orderByAsc(AiTaskStep::getStepOrder)
        );

        List<TaskStepDto> stepDtos = new ArrayList<>();
        for (AiTaskStep step : steps) {
            TaskStepDto stepDto = new TaskStepDto();
            stepDto.setId(step.getId());
            stepDto.setTaskId(step.getTaskId());
            stepDto.setStepOrder(step.getStepOrder());
            stepDto.setStepName(step.getStepName());
            stepDto.setStepType(step.getStepType());
            stepDto.setStatus(step.getStatus());
            stepDto.setProgress(step.getProgress());
            stepDto.setErrorMessage(step.getErrorMessage());
            stepDto.setCreateTime(step.getCreateTime());
            stepDto.setUpdateTime(step.getUpdateTime());
            stepDto.setStartedTime(step.getStartedTime());
            stepDto.setCompletedTime(step.getCompletedTime());

            try {
                if (step.getResultJson() != null) {
                    stepDto.setResult(objectMapper.readTree(step.getResultJson()));
                }
            } catch (Exception e) {
                log.warn("解析步骤结果失败", e);
            }

            stepDtos.add(stepDto);
        }
        dto.setSteps(stepDtos);

        return dto;
    }
}
