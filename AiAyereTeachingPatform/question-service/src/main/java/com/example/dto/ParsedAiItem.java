package com.example.dto;

import lombok.Data;

/**
 * AI 拆分结果中的一条子题（对应一个难度层级/知识点）
 */
@Data
public class ParsedAiItem {
    private String difficulty;   // 基础 / 进阶 / 挑战
    private String knowledgeName; // 知识点名称
    private String content;      // 题干
    private String solution;     // 解法/解析
    private String answer;       // 答案
}