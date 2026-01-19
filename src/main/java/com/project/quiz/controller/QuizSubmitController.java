package com.project.quiz.controller;

import com.project.quiz.domain.QuizSubmission;
import com.project.quiz.domain.User;
import com.project.quiz.dto.QuizSubmitRequest;
import com.project.quiz.repository.QuizSubmissionRepository;
import com.project.quiz.service.QuizGradingService;
import com.project.quiz.service.QuizSubmitService;
import com.project.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
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
    private final UserService userService; // ⭐ 유저 정보 조회를 위해 추가
    

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submit(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizSubmitRequest request,
            Principal principal // ⭐ 로그인한 사용자 정보 (세션)
    ) {
        // 1. 로그인 체크 (로그인 안 된 상태면 에러 처리)
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // 2. 현재 로그인한 사용자의 이메일로 실제 User 정보 조회
        // (프론트엔드에서 보낸 userId가 무엇이든 상관없이, 여기서 진짜 ID로 덮어씁니다.)
        User user = userService.getUserByEmail(principal.getName());
        request.setUserId(user.getId()); // ⭐ 핵심: 진짜 유저 ID로 교체

        // 3. 제출 저장 (이제 request에는 올바른 userId가 들어있습니다)
        Long submissionId = quizSubmitService.submit(quizId, request);

        // 5. 채점 결과 조회
        QuizSubmission submission = quizSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("제출 정보를 찾을 수 없습니다."));

        int score = submission.getTotalScore() != null ? submission.getTotalScore() : 0;

        // 6. 총점(만점) 계산
        int maxScore = submission.getQuiz()
                .getQuestions()
                .stream()
                .mapToInt(q -> q.getPoint() != null ? q.getPoint() : 0)
                .sum();
        
        boolean scorePublic = submission.getQuiz().isScorePublic();
        

        // 7. 결과 반환
        return ResponseEntity.ok(
                Map.of(
                        "submissionId", submissionId,
                        "score", score,
                        "maxScore", maxScore,
                        "scorePublic", scorePublic
                )
        );
    }
}