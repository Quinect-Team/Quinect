package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;

@Data
public class QuizDto {

    /* ================== Quiz ================== */
    private Long quizId;
    private String title;
    private String description;
    private Long userId;
    private boolean scorePublic;


    private List<QuestionDto> questions;

    /* ================== Question ================== */
    @Data
    public static class QuestionDto {

        private Long questionId;

        /** 1: 서술형, 2: 객관식 */
        private Integer quizTypeCode;

        private String questionText;
        private Integer point;

        /** 객관식 정답 (option_number) */
        private String answerOption;

        /** 서술형 정답 */
        private String subjectiveAnswer;

        private String image;

        /** 객관식 보기 */
        private List<OptionDto> options;
    }

    /* ================== Option ================== */
    @Data
    public static class OptionDto {
        private Integer optionNumber;
        private String optionText;
    }

    /* ================== Entity → DTO ================== */
    public static QuizDto fromEntity(Quiz quiz) {

        QuizDto dto = new QuizDto();
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setUserId(quiz.getUserId());
        dto.setScorePublic(quiz.isScorePublic());


        List<QuestionDto> questionDtos = quiz.getQuestions()
                .stream()
                .map(q -> {
                    QuestionDto qd = new QuestionDto();

                    qd.setQuestionId(q.getQuestionId());
                    qd.setQuizTypeCode(q.getQuizTypeCode());
                    qd.setQuestionText(q.getQuestionText());
                    qd.setPoint(q.getPoint());
                    qd.setAnswerOption(q.getAnswerOption());
                    qd.setSubjectiveAnswer(q.getSubjectiveAnswer());
                    qd.setImage(q.getImage());

                    if (q.getOptions() != null) {
                        List<OptionDto> optionDtos = q.getOptions()
                                .stream()
                                .map(opt -> {
                                    OptionDto od = new OptionDto();
                                    od.setOptionNumber(opt.getOptionNumber());
                                    od.setOptionText(opt.getOptionText());
                                    return od;
                                })
                                .toList();
                        qd.setOptions(optionDtos);
                    }

                    return qd;
                })
                .toList();

        dto.setQuestions(questionDtos);
        return dto;
    }

    /* ================== List 화면용 ================== */
    @Data
    @AllArgsConstructor
    public static class ListResponse {
        private Long quizId;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ListResponse fromEntity(Quiz quiz) {
            return new ListResponse(
                    quiz.getQuizId(),
                    quiz.getTitle(),
                    quiz.getCreatedAt(),
                    quiz.getUpdatedAt()
            );
        }
    }
}
