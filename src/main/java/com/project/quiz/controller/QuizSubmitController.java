package com.project.quiz.controller;

import com.project.quiz.domain.QuizSubmission;
import com.project.quiz.dto.QuizSubmitRequest;
import com.project.quiz.repository.QuizSubmissionRepository;
import com.project.quiz.service.QuizGradingService;
import com.project.quiz.service.QuizSubmitService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizSubmitController {

    private final QuizSubmitService quizSubmitService;
    private final QuizGradingService quizGradingService;
    private final QuizSubmissionRepository quizSubmissionRepository;


    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submit(
    		@PathVariable("quizId") Long quizId,
            @RequestBody QuizSubmitRequest request
    ) {
        // 1. 제출 저장
        Long submissionId =
                quizSubmitService.submit(quizId, request);

        // 2. 자동 채점 실행 ⭐
        quizGradingService.grade(submissionId);

        // 3. 채점 결과 조회
        QuizSubmission submission =
                quizSubmissionRepository.findById(submissionId)
                        .orElseThrow();

        int score = submission.getTotalScore();

        // 4. 총점 계산
        int maxScore = submission.getQuiz()
                .getQuestions()
                .stream()
                .mapToInt(q -> q.getPoint() != null ? q.getPoint() : 0)
                .sum();

        // 5. 프론트로 반환
        return ResponseEntity.ok(
                Map.of(
                        "submissionId", submissionId,
                        "score", score,
                        "maxScore", maxScore
                )
        );
    }
    
    
}
