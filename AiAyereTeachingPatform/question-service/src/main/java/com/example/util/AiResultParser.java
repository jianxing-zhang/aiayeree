package com.example.util;

import com.example.dto.ParsedAiItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 agent 返回的 markdown 三级拆分文本解析为结构化子题列表。
 * 兼容格式：
 *   ### 一、简单题（基础知识点列出）
 *   题目：...
 *   【解法】 ...
 *   答案：...
 */
public class AiResultParser {

    private static final Pattern HEADER = Pattern.compile("(?m)^#{1,4}\\s*");

    public static List<ParsedAiItem> parse(String markdown) {
        List<ParsedAiItem> items = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) return items;

        for (String section : HEADER.split(markdown)) {
            if (section == null || section.isBlank()) continue;
            String[] lines = section.split("\\r?\\n");
            String headerLine = lines[0].trim();
            String body = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));

            ParsedAiItem item = new ParsedAiItem();
            item.setDifficulty(detectDifficulty(headerLine));
            item.setKnowledgeName(detectKnowledgeName(headerLine));
            parseBody(item, body);
            items.add(item);
        }
        return items;
    }

    /** 按「题目 / 解法 / 答案」出现顺序切分正文 */
    private static void parseBody(ParsedAiItem item, String body) {
        List<String> lines = new ArrayList<>(List.of(body.split("\\r?\\n")));
        int cIdx = findField(lines, "题目");
        int sIdx = findField(lines, "解法");
        int aIdx = findField(lines, "答案");

        item.setContent(collect(lines, cIdx, sIdx, "题目"));
        item.setSolution(collect(lines, sIdx, aIdx, "解法"));
        item.setAnswer(collect(lines, aIdx, lines.size(), "答案"));
    }

    private static int findField(List<String> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            String cleaned = t.replace("【", "").replace("】", "").trim();
            if (cleaned.startsWith(key)) {
                String rest = cleaned.substring(key.length()).trim();
                if (rest.isEmpty() || rest.startsWith("：") || rest.startsWith(":")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 收集字段起始行到结束行之间的文本（去掉行首字段标签） */
    private static String collect(List<String> lines, int from, int to, String key) {
        if (from < 0) return null;
        if (to < 0 || to > lines.size()) to = lines.size();
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to && i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (i == from) {
                t = stripFieldLabel(t, key);
            }
            if (!t.isEmpty()) {
                sb.append(t).append("\n");
            }
        }
        String r = sb.toString().trim();
        return r.isEmpty() ? null : r;
    }

    /** 去掉行首的「KEY」/「KEY：」/「【KEY】」标签 */
    private static String stripFieldLabel(String t, String key) {
        t = t.trim();
        // 【解法】 或 【解法】 xxx
        if (t.startsWith("【") || t.startsWith("[")) {
            int close = t.indexOf("】");
            if (close < 0) close = t.indexOf("]");
            if (close >= 0) {
                return t.substring(close + 1).trim();
            }
        }
        if (t.startsWith(key)) {
            String rest = t.substring(key.length()).trim();
            if (rest.startsWith("：")) rest = rest.substring(1).trim();
            else if (rest.startsWith(":")) rest = rest.substring(1).trim();
            return rest;
        }
        return t;
    }

    private static String detectDifficulty(String header) {
        if (header.contains("简单") || header.contains("基础")) return "基础";
        if (header.contains("中等") || header.contains("进阶")) return "进阶";
        if (header.contains("困难") || header.contains("挑战") || header.contains("综合")) return "挑战";
        return "进阶";
    }

    private static String detectKnowledgeName(String headerLine) {
        Matcher m = Pattern.compile("[（(]([^）)]*)[）)]").matcher(headerLine);
        if (m.find()) {
            String name = m.group(1).replace("列出", "").trim();
            if (!name.isBlank()) return name;
        }
        return "相关知识点";
    }
}