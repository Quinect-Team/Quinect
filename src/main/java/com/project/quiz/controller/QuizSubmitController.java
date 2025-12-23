package com.project.quiz.controller;

import com.project.quiz.dto.QuizSubmitRequest;
import com.project.quiz.service.QuizSubmitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizSubmitController {

    private final QuizSubmitService quizSubmitService;

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submit(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizSubmitRequest request
    ) {
        Long submissionId = quizSubmitService.submit(quizId, request);
        return ResponseEntity.ok(submissionId);
    }
}
