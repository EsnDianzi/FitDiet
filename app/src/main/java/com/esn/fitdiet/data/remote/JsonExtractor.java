package com.esn.fitdiet.data.remote;

/**
 * 从模型返回文本中抽取 JSON 数组片段，剥离前后的说明性噪声文字。
 *
 * <p>通义千问 VL 的 {@code content[].text} 常被模型包上「以下是识别结果：…」之类的
 * 自然语言说明。本工具用括号深度计数（兼容嵌套数组/对象与转义引号）定位第一个
 * {@code [} 到匹配 {@code ]} 的子串，交给 Gson 解析。
 *
 * <p>纯函数，无 Android 依赖，可被 JVM 单测直接覆盖（TC-F3-02）。
 */
public final class JsonExtractor {

    private JsonExtractor() { }

    /**
     * 抽取第一个 JSON 数组子串；未找到或括号不匹配时返回 {@code null}。
     *
     * @param text 模型原始返回文本（可能含前后噪声）
     * @return 形如 {@code [ ... ]} 的数组文本，或 {@code null}
     */
    public static String extractJsonArray(String text) {
        if (text == null) return null;
        int start = text.indexOf('[');
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++; // 跳过转义字符
                    continue;
                }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null; // 括号不匹配
    }
}
