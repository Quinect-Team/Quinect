package com.project.quiz.service;

import com.project.quiz.domain.*;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    public void saveQuiz(QuizDto quizDto) {

        // Quiz 엔티티 생성
        Quiz quiz = new Quiz();
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setUserId(quizDto.getUserId());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUpdatedAt(LocalDateTime.now());

        // 문제 리스트 생성
        quizDto.getQuestions().forEach(qDto -> {
            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setQuizTypeCode(qDto.getQuizTypeCode());
            question.setQuestionText(qDto.getQuestionText());
            question.setAnswerOption(qDto.getAnswerOption());
            question.setPoint(qDto.getPoint());
            question.setSubjectiveAnswer(qDto.getSubjectiveAnswer());
            question.setImage(qDto.getImage());

            // 선지 리스트 생성
            if (qDto.getOptions() != null) {
                int idx = 1;
                for (String opt : qDto.getOptions()) {
                    QuizOption option = new QuizOption();
                    option.setQuestion(question);
                    option.setOptionNumber(idx++);
                    option.setOptionText(opt);
                    question.getOptions().add(option);
                }
            }

            quiz.getQuestions().add(question);
        });

        // DB 저장
        quizRepository.save(quiz);
    }
}