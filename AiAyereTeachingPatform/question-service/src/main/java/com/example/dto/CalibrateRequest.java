package com.example.dto;

import com.example.entity.KnowledgePoint;
import com.example.entity.SubQuestion;
import lombok.Data;

import java.util.List;

/**
 * 人工校准请求：老师/学生编辑 AI 生成结果后提交。
 * 后端会按难度把小问题关联到对应知识点。
 */
@Data
public class CalibrateRequest {
    private Long questionId;
    private List<KnowledgePoint> knowledgePoints;
    private List<SubQuestion> subQuestions;
}