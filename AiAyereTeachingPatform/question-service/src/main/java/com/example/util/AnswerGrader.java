package com.example.util;

/**
 * 简单的自动批改：数值题按数值误差比较，其余按归一化后相等/包含比较。
 */
public class AnswerGrader {

    public static boolean isCorrect(String studentAnswer, String referenceAnswer) {
        if (studentAnswer == null || referenceAnswer == null) return false;
        String s = normalize(studentAnswer);
        String r = normalize(referenceAnswer);
        if (s.isEmpty() || r.isEmpty()) return false;

        // 数值比较
        Double sn = toNumber(s);
        Double rn = toNumber(r);
        if (sn != null && rn != null) {
            return Math.abs(sn - rn) < 1e-6;
        }

        // 文本比较：相等或互相包含
        return s.equals(r) || s.contains(r) || r.contains(s);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("\\s+", "")
                .replace("＝", "=")
                .replace("（", "(").replace("）", ")")
                .trim();
    }

    private static Double toNumber(String s) {
        // 去掉结尾常见单位后再尝试解析
        String t = s.replaceAll("[a-z\u00B0/%^]+$", "").replace("，","").replace(",","").trim();
        t = t.replaceAll("[=答答案是为]+", "");
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return null;
        }
    }
}