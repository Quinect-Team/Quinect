package com.project.quiz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.project.quiz.dto.AiQuizResponse;
import com.project.quiz.service.GeminiTestService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestGeminiController {

    private final GeminiTestService geminiTestService;

    // 테스트 주소: http://localhost:8080/test/gemini?topic=한국역사
    @GetMapping("/test/gemini")
    public AiQuizResponse testGemini(@RequestParam("topic") String topic) {
        System.out.println("Gemini에게 요청 보냄: " + topic);
        return geminiTestService.createQuiz(topic);
    }
}