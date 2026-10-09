package com.example.dto;

import lombok.Data;

@Data
public class UploadQuestionRequest {
    private Long uploaderId;
    private String uploaderName;
    private String subject;
    private String sourceType;   // TEXT / IMAGE / DOC
    private String text;         // 文本或文档抽取出的文字
    private String imageBase64;  // 图片 base64（sourceType=IMAGE 时使用）
    private String originalAnswer;
}