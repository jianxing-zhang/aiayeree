package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploaderId;
    private String uploaderName;
    private String subject;
    private String sourceType;      // TEXT / IMAGE / DOC
    private String sourceImage;     // base64 或文件路径
    private String sourceText;      // 文本/抽取出的原文
    private String originalAnswer;
    private String recognizedText;  // OCR 识别文本
    private String aiResult;        // AI 拆分原始结果(markdown)
    private String status;          // DRAFT / CALIBRATED / PUBLISHED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}