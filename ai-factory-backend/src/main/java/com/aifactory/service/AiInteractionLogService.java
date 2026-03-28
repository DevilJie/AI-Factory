package com.aifactory.service;

import com.aifactory.dto.AiInteractionLogDTO;
import com.aifactory.entity.AiInteractionLog;
import com.aifactory.mapper.AiInteractionLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI交互日志服务
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Slf4j
@Service
public class AiInteractionLogService {

    @Autowired
    private AiInteractionLogMapper aiInteractionLogMapper;

    /**
     * 生成新的追踪ID
     */
    public String generateTraceId() {
        return "AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 记录AI交互日志
     *
     * @param traceId 追踪ID
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param requestType 请求类型
     * @param provider AI提供商
     * @param model 模型名称
     * @param requestPrompt 请求提示词
     * @param requestParams 请求参数（JSON格式）
     * @param responseContent 响应内容
     * @param responseTokens 响应token数
     * @param responseDuration 响应耗时（毫秒）
     * @param isSuccess 是否成功
     * @param errorMessage 错误信息
     */
    public void logInteraction(
            String traceId,
            Long projectId,
            Long userId,
            String requestType,
            String provider,
            String model,
            String requestPrompt,
            String requestParams,
            String responseContent,
            Integer responseTokens,
            Long responseDuration,
            Boolean isSuccess,
            String errorMessage
    ) {
        logInteraction(traceId, projectId, null, null, null, userId, requestType, provider, model,
                requestPrompt, requestParams, responseContent, responseTokens, responseDuration, isSuccess, errorMessage);
    }

    /**
     * 记录AI交互日志（带上下文信息）
     *
     * @param traceId 追踪ID
     * @param projectId 项目ID
     * @param volumePlanId 分卷计划ID
     * @param chapterPlanId 章节规划ID
     * @param chapterId 章节ID
     * @param userId 用户ID
     * @param requestType 请求类型
     * @param provider AI提供商
     * @param model 模型名称
     * @param requestPrompt 请求提示词
     * @param requestParams 请求参数（JSON格式）
     * @param responseContent 响应内容
     * @param responseTokens 响应token数
     * @param responseDuration 响应耗时（毫秒）
     * @param isSuccess 是否成功
     * @param errorMessage 错误信息
     */
    public void logInteraction(
            String traceId,
            Long projectId,
            Long volumePlanId,
            Long chapterPlanId,
            Long chapterId,
            Long userId,
            String requestType,
            String provider,
            String model,
            String requestPrompt,
            String requestParams,
            String responseContent,
            Integer responseTokens,
            Long responseDuration,
            Boolean isSuccess,
            String errorMessage
    ) {
        try {
            AiInteractionLog interactionLog = new AiInteractionLog();
            interactionLog.setTraceId(traceId);
            interactionLog.setProjectId(projectId);
            interactionLog.setVolumePlanId(volumePlanId);
            interactionLog.setChapterPlanId(chapterPlanId);
            interactionLog.setChapterId(chapterId);
            interactionLog.setUserId(userId);
            interactionLog.setRequestType(requestType);
            interactionLog.setProvider(provider);
            interactionLog.setModel(model);
            interactionLog.setRequestPrompt(requestPrompt);
            interactionLog.setRequestParams(requestParams);
            interactionLog.setResponseContent(responseContent);
            interactionLog.setResponseTokens(responseTokens);
            interactionLog.setResponseDuration(responseDuration);
            interactionLog.setIsSuccess(isSuccess);
            interactionLog.setErrorMessage(errorMessage);
            interactionLog.setCreatedTime(LocalDateTime.now());

            aiInteractionLogMapper.insert(interactionLog);

            // 在日志中打印流水号，便于追踪
            log.info("AI交互记录已保存，traceId={}, projectId={}, volumePlanId={}, chapterPlanId={}, chapterId={}, requestType={}, provider={}, model={}, 成功={}, 耗时={}ms",
                    traceId, projectId, volumePlanId, chapterPlanId, chapterId, requestType, provider, model, isSuccess, responseDuration);

        } catch (Exception e) {
            // 记录失败不应影响主流程
            log.error("保存AI交互日志失败，traceId={}", traceId, e);
        }
    }

    /**
     * 根据traceId查询日志详情
     *
     * @param traceId 追踪ID
     * @return 日志详情
     */
    public AiInteractionLogDTO getByTraceId(String traceId) {
        AiInteractionLog logEntity = aiInteractionLogMapper.selectOne(
                new LambdaQueryWrapper<AiInteractionLog>()
                        .eq(AiInteractionLog::getTraceId, traceId)
        );

        if (logEntity == null) {
            return null;
        }

        return convertToDTO(logEntity);
    }

    /**
     * 分页查询日志列表
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param projectId 项目ID（可选）
     * @param requestType 请求类型（可选）
     * @param provider 提供商（可选）
     * @param isSuccess 是否成功（可选）
     * @return 分页结果
     */
    public Page<AiInteractionLogDTO> queryLogs(
            int pageNum,
            int pageSize,
            Long projectId,
            String requestType,
            String provider,
            Boolean isSuccess
    ) {
        Page<AiInteractionLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiInteractionLog> wrapper = new LambdaQueryWrapper<AiInteractionLog>()
                .eq(projectId != null, AiInteractionLog::getProjectId, projectId)
                .eq(requestType != null, AiInteractionLog::getRequestType, requestType)
                .eq(provider != null, AiInteractionLog::getProvider, provider)
                .eq(isSuccess != null, AiInteractionLog::getIsSuccess, isSuccess)
                .orderByDesc(AiInteractionLog::getCreatedTime);

        Page<AiInteractionLog> resultPage = aiInteractionLogMapper.selectPage(page, wrapper);

        // 转换为DTO
        Page<AiInteractionLogDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<AiInteractionLogDTO> dtoList = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 实体转DTO
     */
    private AiInteractionLogDTO convertToDTO(AiInteractionLog entity) {
        AiInteractionLogDTO dto = new AiInteractionLogDTO();
        dto.setId(entity.getId());
        dto.setTraceId(entity.getTraceId());
        dto.setProjectId(entity.getProjectId());
        dto.setUserId(entity.getUserId());
        dto.setRequestType(entity.getRequestType());
        dto.setProvider(entity.getProvider());
        dto.setModel(entity.getModel());
        dto.setRequestParams(entity.getRequestParams());
        dto.setResponseTokens(entity.getResponseTokens());
        dto.setResponseDuration(entity.getResponseDuration());
        dto.setIsSuccess(entity.getIsSuccess());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedTime(entity.getCreatedTime());

        // 设置请求提示词（预览和完整）
        if (entity.getRequestPrompt() != null) {
            dto.setRequestPrompt(entity.getRequestPrompt());
            if (entity.getRequestPrompt().length() > 500) {
                dto.setRequestPromptPreview(entity.getRequestPrompt().substring(0, 500) + "...");
            } else {
                dto.setRequestPromptPreview(entity.getRequestPrompt());
            }
        }

        // 设置响应内容（预览和完整）
        if (entity.getResponseContent() != null) {
            dto.setResponseContent(entity.getResponseContent());
            if (entity.getResponseContent().length() > 500) {
                dto.setResponseContentPreview(entity.getResponseContent().substring(0, 500) + "...");
            } else {
                dto.setResponseContentPreview(entity.getResponseContent());
            }
        }

        return dto;
    }
}
