package com.project.quiz.service;

import com.project.quiz.domain.*;
import com.project.quiz.dto.QuizSubmitRequest;
import com.project.quiz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizSubmitService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizAnswerRepository answerRepository;

    public Long submit(Long quizId, QuizSubmitRequest request) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈 없음"));

        /* 1️⃣ submission 생성 */
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setUserId(request.getUserId());
        submission.setGraded(false);

        submissionRepository.save(submission);

        /* 2️⃣ answer 생성 */
        for (QuizSubmitRequest.AnswerRequest ar : request.getAnswers()) {

            QuizQuestion question = quizQuestionRepository.findById(ar.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("문제 없음"));

            QuizAnswer answer = new QuizAnswer();
            answer.setSubmission(submission);
            answer.setQuestion(question);
            answer.setAnswerText(ar.getAnswerText());
            answer.setSelectedOption(ar.getSelectedOption());

            answerRepository.save(answer);
        }

        return submission.getSubmissionId();
    }
}
