package com.backend.util;

/**
 * LLM 输出清洗——参考 MaiBot 的多轮解析思路。
 */
public final class DataCleaner {

    private DataCleaner() {}

    /**
     * 从 LLM 输出提取 JSON。多轮尝试：
     * 1. 去掉 markdown 包裹
     * 2. 找到最外层 {...} 或 [...]
     * 3. 修复未闭合括号
     * 4. 去掉尾逗号
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        String s = raw;

        // 第1轮：去掉 markdown 和空白
        s = s.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        // 第2轮：找到 JSON 起始位置（跳过可能的简短说明文字）
        int start = -1, end = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '[') { start = i; break; }
        }
        if (start < 0) return "{}";

        // 找出对应的闭合括号（跳过字符串内）
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        char openChar = s.charAt(start);
        char closeChar = openChar == '{' ? '}' : ']';
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == openChar) depth++;
            else if (c == closeChar) { depth--; if (depth == 0) { end = i; break; } }
        }

        if (end < start) {
            // 未闭合：补上缺失的闭合括号
            s = s.substring(start);
            int unclosed = depth;
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < unclosed; i++) suffix.append(closeChar);
            s = s + suffix;
        } else {
            s = s.substring(start, end + 1);
        }

        // 第3轮：去掉尾逗号和注释
        s = s.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        s = s.replaceAll("//[^\n]*", "").replaceAll("#[^\n]*", "");

        // 第4轮：修复中文引号（在 JSON 值内部可能被误用）
        s = s.replace('\u201c', '"').replace('\u201d', '"');

        return s;
    }

    /** 截断，用于日志预览 */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
