package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;

@Data
public class QuizDto {
    private Long quizId;           // <-- 추가 (퀴즈 식별자)
    private String title;
    private String description;
    private Long userId;
    private List<QuestionDto> questions;

    @Data
    public static class QuestionDto {
        private Long questionId;      // <-- 추가 (질문 식별자)
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
    
    public static QuizDto fromEntity(Quiz quiz) {

        QuizDto dto = new QuizDto();
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setUserId(quiz.getUserId());

        // 질문 리스트 매핑
        List<QuestionDto> questionDtos = quiz.getQuestions()
                .stream()
                .map(q -> {
                    QuestionDto qd = new QuestionDto();

                    qd.setQuestionId(q.getQuestionId());

                    qd.setQuizTypeCode(q.getQuizTypeCode());
                    qd.setQuestionText(q.getQuestionText());
                    qd.setAnswerOption(q.getAnswerOption());
                    qd.setPoint(q.getPoint());
                    qd.setSubjectiveAnswer(q.getSubjectiveAnswer());
                    qd.setImage(q.getImage());

                    // 객관식 보기 목록(List<String>) 매핑
                    if (q.getOptions() != null) {
                        List<String> optionTexts = q.getOptions()
                                .stream()
                                .map(QuizOption::getOptionText)
                                .toList();
                        qd.setOptions(optionTexts);
                    }

                    return qd;
                })
                .toList();

        dto.setQuestions(questionDtos);
        return dto;
    }
    
    @Data
    @AllArgsConstructor
    public static class ListResponse {
        private Long quizId;
        private String title;
    }
}
