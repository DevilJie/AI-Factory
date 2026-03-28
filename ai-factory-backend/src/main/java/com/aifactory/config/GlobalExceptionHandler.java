package com.aifactory.config;

import com.aifactory.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.error("参数校验异常", ex);

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            logger.error("字段: {}, 错误: {}", error.getField(), error.getDefaultMessage());
        }

        String message = errors.values().iterator().next();
        return Result.userErrorParam(message);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException ex) {
        logger.error("运行时异常: {}", ex.getMessage(), ex);
        return Result.error(ex.getMessage());
    }

    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception ex) {
        logger.error("系统异常", ex);
        return Result.error("系统错误：" + ex.getMessage());
    }
}
