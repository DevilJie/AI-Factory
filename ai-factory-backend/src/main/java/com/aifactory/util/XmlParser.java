package com.aifactory.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * XML解析工具类
 * 使用Java内置DOM解析器解析XML字符串，支持CDATA
 *
 * @Author CaiZy
 * @Date 2025-02-03
 */
@Slf4j
public class XmlParser {

    /**
     * 解析XML字符串并提取指定字段的值（容错模式，支持不完整的XML）
     *
     * @param xmlString XML字符串
     * @param rootElementName 根元素名称
     * @param fieldNames 需要提取的字段名称列表
     * @return 字段名到字段值的映射
     */
    public static Map<String, String> parseXml(String xmlString, String rootElementName, String... fieldNames) {
        Map<String, String> result = new HashMap<>();

        try {
            // 创建DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // 禁用外部实体以防止XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);

            // 容错模式：解析不完整的XML
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

            // 创建DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析XML字符串
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));

            // 获取根元素
            Element root = document.getDocumentElement();

            if (!root.getNodeName().equals(rootElementName)) {
                log.warn("根元素名称不匹配，期望: {}, 实际: {}", rootElementName, root.getNodeName());
            }

            // 提取各个字段的值
            for (String fieldName : fieldNames) {
                String value = getElementText(root, fieldName);
                if (value != null && !value.isEmpty()) {
                    result.put(fieldName, value);
                }
            }

            log.info("XML解析成功，提取到 {} 个字段", result.size());

        } catch (SAXException e) {
            log.warn("XML解析失败，尝试使用正则表达式提取: {}", e.getMessage());
            // 如果DOM解析失败，使用正则表达式作为后备方案
            return parseXmlWithRegex(xmlString, fieldNames);
        } catch (Exception e) {
            log.error("解析XML失败", e);
            // 最后尝试：使用正则表达式
            return parseXmlWithRegex(xmlString, fieldNames);
        }

        return result;
    }

    /**
     * 使用正则表达式提取XML字段（容错后备方案）
     */
    private static Map<String, String> parseXmlWithRegex(String xmlString, String... fieldNames) {
        Map<String, String> result = new HashMap<>();

        for (String fieldName : fieldNames) {
            try {
                // 尝试匹配CDATA格式
                String cdataPattern = "<" + fieldName + ">\\s*<!\\[CDATA\\[([^\\]]*?)\\]\\]>\\s*</" + fieldName + ">";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(cdataPattern, java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher matcher = pattern.matcher(xmlString);

                if (matcher.find()) {
                    result.put(fieldName, matcher.group(1));
                } else {
                    // 尝试匹配普通格式
                    String normalPattern = "<" + fieldName + ">([^<]+)</" + fieldName + ">";
                    pattern = java.util.regex.Pattern.compile(normalPattern);
                    matcher = pattern.matcher(xmlString);
                    if (matcher.find()) {
                        result.put(fieldName, matcher.group(1));
                    }
                }
            } catch (Exception e) {
                log.debug("提取字段 {} 失败", fieldName);
            }
        }

        log.info("正则表达式解析，提取到 {} 个字段", result.size());
        return result;
    }

    /**
     * 获取元素的文本内容（包括CDATA）
     *
     * @param parent 父元素
     * @param tagName 子元素标签名
     * @return 文本内容，如果不存在则返回null
     */
    private static String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);

        if (nodeList.getLength() == 0) {
            return null;
        }

        Node node = nodeList.item(0);
        return node.getTextContent();
    }

    /**
     * 从XML字符串中提取指定根元素的内容
     *
     * @param xmlString 完整的XML字符串
     * @param rootElementName 根元素名称
     * @return 提取的XML字符串
     */
    public static String extractRootElement(String xmlString, String rootElementName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));

            Element root = document.getDocumentElement();

            if (root.getNodeName().equals(rootElementName)) {
                // 返回包含根元素的完整XML
                return xmlString.substring(
                    xmlString.indexOf("<" + rootElementName + ">"),
                    xmlString.lastIndexOf("</" + rootElementName + ">") + ("</" + rootElementName + ">").length()
                );
            }

            log.warn("未找到根元素: {}", rootElementName);
            return null;

        } catch (Exception e) {
            log.error("提取根元素失败", e);
            return null;
        }
    }
}
