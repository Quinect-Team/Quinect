package com.project.quiz.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuizSubmitRequest {

    private Long userId;
    private List<AnswerRequest> answers;

    @Data
    public static class AnswerRequest {
        private Long questionId;
        private String answerText;
        private Integer selectedOption;
    }
}
