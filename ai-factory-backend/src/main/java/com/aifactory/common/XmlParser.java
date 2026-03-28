package com.aifactory.common;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 通用XML解析器
 *
 * 职责：
 * - 将XML字符串解析为Java POJO对象
 * - 支持Jackson XML注解（@JacksonXmlRootElement、@JacksonXmlProperty等）
 * - 支持单字母标签以节省token
 * - 自动处理CDATA标签
 * - 提供类型安全的解析方法
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Slf4j
@Component
public class XmlParser {

    private final XmlMapper xmlMapper;

    public XmlParser() {
        this.xmlMapper = new XmlMapper();

        // 配置XmlMapper
        this.xmlMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
    }

    /**
     * 将XML字符串解析为指定的POJO类型
     *
     * 使用示例：
     * <pre>
     * // 1. 定义POJO类（使用Jackson XML注解）
     * {@code
     * @JacksonXmlRootElement(localName = "M")
     * public static class ChapterMemory {
     *     @JacksonXmlElementWrapper(useWrapping = false)
     *     @JacksonXmlProperty(localName = "E")
     *     private List<String> keyEvents;
     *
     *     @JacksonXmlProperty(localName = "S")
     *     private String newSettings;
     * }
     *
     * // 2. 解析XML
     * ChapterMemory memory = xmlParser.parse(xmlString, ChapterMemory.class);
     * }
     * </pre>
     *
     * @param xmlString XML字符串
     * @param targetClass 目标POJO类的Class对象
     * @param <T> 泛型类型
     * @return 解析后的POJO对象
     * @throws XmlParseException 解析失败时抛出
     */
    public <T> T parse(String xmlString, Class<T> targetClass) throws XmlParseException {
        if (xmlString == null || xmlString.trim().isEmpty()) {
            throw new XmlParseException("XML字符串不能为空");
        }

        try {
            // 清理可能的markdown代码块标记
            String cleanedXml = cleanXmlString(xmlString);

            log.debug("开始解析XML，目标类型: {}, XML长度: {}", targetClass.getSimpleName(), cleanedXml.length());

            // 使用XmlMapper解析
            T result = xmlMapper.readValue(cleanedXml, targetClass);

            log.debug("XML解析成功，目标类型: {}", targetClass.getSimpleName());

            return result;

        } catch (IOException e) {
            log.error("XML解析失败: {}", e.getMessage(), e);
            throw new XmlParseException("XML解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将POJO对象序列化为XML字符串
     *
     * @param object POJO对象
     * @return XML字符串
     * @throws XmlParseException 序列化失败时抛出
     */
    public String toXml(Object object) throws XmlParseException {
        if (object == null) {
            throw new XmlParseException("对象不能为空");
        }

        try {
            return xmlMapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("XML序列化失败: {}", e.getMessage(), e);
            throw new XmlParseException("XML序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理XML字符串，移除可能的干扰内容
     */
    private String cleanXmlString(String xmlString) {
        String cleaned = xmlString.trim();

        // 移除markdown代码块标记
        if (cleaned.startsWith("```xml")) {
            cleaned = cleaned.substring(6);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * XML解析异常类
     */
    public static class XmlParseException extends Exception {
        public XmlParseException(String message) {
            super(message);
        }

        public XmlParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
