package com.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class AnswerSubmitRequest {
    private Long studentId;
    private List<Item> answers;

    @Data
    public static class Item {
        private Long subQuestionId;
        private String answer;
    }
}