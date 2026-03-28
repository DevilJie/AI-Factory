package com.aifactory.exception;

/**
 * 提示词模板异常
 *
 * @Author CaiZy
 * @Date 2025-02-05
 */
public class PromptTemplateException extends RuntimeException {

    private String templateCode;

    public PromptTemplateException(String message) {
        super(message);
    }

    public PromptTemplateException(String message, String templateCode) {
        super(message);
        this.templateCode = templateCode;
    }

    public PromptTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    public PromptTemplateException(String message, String templateCode, Throwable cause) {
        super(message, cause);
        this.templateCode = templateCode;
    }

    public String getTemplateCode() {
        return templateCode;
    }
}
