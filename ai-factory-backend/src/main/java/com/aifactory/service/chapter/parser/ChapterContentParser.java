package com.aifactory.service.chapter.parser;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 章节内容解析器
 *
 * 职责：
 * - 解析LLM返回的XML格式章节内容
 * - 提取标题、正文、概要等字段
 * - 处理CDATA标签
 * - 字数统计
 *
 * @Author CaiZy
 * @Date 2025-02-04
 */
@Component
public class ChapterContentParser {

    /**
     * 解析章节XML内容
     *
     * @param xmlContent LLM返回的XML字符串
     * @return 解析后的章节内容对象
     * @throws ParseException 解析失败时抛出
     */
    public ChapterContent parse(String xmlContent) throws ParseException {
        try {
            ChapterContent content = new ChapterContent();

            // 提取最外层的XML对象
            String cleanedXml = extractRootXml(xmlContent);

            // 解析各个字段
            content.setTitle(extractXMLTag(cleanedXml, "t"));
            content.setContent(extractXMLTag(cleanedXml, "c"));
            content.setPlotSummary(extractXMLTag(cleanedXml, "p"));

            // 解析字数统计
            String wordCountStr = extractXMLTag(cleanedXml, "w");
            if (wordCountStr != null && !wordCountStr.isEmpty()) {
                try {
                    content.setWordCount(Integer.parseInt(wordCountStr.trim()));
                } catch (NumberFormatException e) {
                    // 如果解析失败，重新统计
                    content.setWordCount(countWords(content.getContent()));
                }
            } else {
                content.setWordCount(countWords(content.getContent()));
            }

            // 验证必要字段
            if (content.getContent() == null || content.getContent().isEmpty()) {
                throw new ParseException("章节内容为空");
            }

            return content;

        } catch (Exception e) {
            throw new ParseException("解析章节XML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取根XML元素
     */
    private String extractRootXml(String xmlStr) {
        // 尝试提取 <chap> 标签
        String[] commonRootTags = {"<chap>", "<c>", "<chapter>"};

        for (String tag : commonRootTags) {
            String closingTag = "</" + tag.substring(1);
            int firstTag = xmlStr.indexOf(tag);
            int lastTag = xmlStr.lastIndexOf(closingTag);

            if (firstTag >= 0 && lastTag > firstTag) {
                return xmlStr.substring(firstTag, lastTag + closingTag.length());
            }
        }

        // 如果没有找到标准标签，直接返回原字符串
        return xmlStr;
    }

    /**
     * 提取XML标签内容（处理CDATA标签）
     */
    private String extractXMLTag(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";

        int start = xml.indexOf(openTag);
        if (start == -1) {
            return "";
        }

        int end = xml.indexOf(closeTag, start);
        if (end == -1) {
            return "";
        }

        String content = xml.substring(start + openTag.length(), end).trim();

        // 处理CDATA标签：<![CDATA[内容]]>
        if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
            content = content.substring(9, content.length() - 3);
        }

        return content;
    }

    /**
     * 统计字数（去除空格、换行）
     * 统计中文字符和英文单词数
     */
    public int countWords(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 去除空白字符
        String trimmed = content.replaceAll("\\s+", "");

        // 统计中文字符
        int chineseChars = 0;
        // 统计英文单词数
        int englishWords = 0;
        boolean inEnglishWord = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            // 判断是否为中文字符（CJK统一汉字）
            if (isChineseChar(c)) {
                chineseChars++;
                inEnglishWord = false;
            }
            // 判断是否为英文字母
            else if (isEnglishLetter(c)) {
                if (!inEnglishWord) {
                    englishWords++;
                    inEnglishWord = true;
                }
            } else {
                inEnglishWord = false;
            }
        }

        // 返回中文字符数 + 英文单词数
        return chineseChars + englishWords;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChineseChar(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || // CJK统一汉字
               (c >= 0x3400 && c <= 0x4DBF) || // CJK扩展A
               (c >= 0x20000 && c <= 0x2A6DF) || // CJK扩展B
               (c >= 0x2A700 && c <= 0x2B73F) || // CJK扩展C
               (c >= 0x2B740 && c <= 0x2B81F) || // CJK扩展D
               (c >= 0x2B820 && c <= 0x2CEAF) || // CJK扩展E
               (c >= 0x2CEB0 && c <= 0x2EBEF);   // CJK扩展F
    }

    /**
     * 判断是否为英文字母
     */
    private boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 解析异常类
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 解析结果对象
     */
    @Data
    public static class ChapterContent {
        private String title;           // 章节标题
        private String content;         // 章节正文
        private String plotSummary;     // 剧情概要
        private Integer wordCount;      // 字数统计
    }
}
