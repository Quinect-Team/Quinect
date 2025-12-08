package com.project.quiz.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuizDto {
    private String title;
    private String description;
    private Long userId;
    private List<QuestionDto> questions;

    @Data
    public static class QuestionDto {
        private Integer quizTypeCode;
        private String questionText;
        private String answerOption;
        private Integer point;
        private String subjectiveAnswer;
        private String image;
        private List<String> options;
    }
    
    @Data
    public static class OptionDto {
        private Integer optionNumber;
        private String optionText;
    }
}