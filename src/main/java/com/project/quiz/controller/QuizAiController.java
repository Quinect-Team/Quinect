package com.project.quiz.controller;

import com.project.quiz.dto.AiGenRequest;
import com.project.quiz.service.QuizAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizAiController {

    private final QuizAiService quizAiService;

    @PostMapping("/generate-ai")
    public ResponseEntity<?> generateQuizAi(@RequestBody AiGenRequest request) {
        try {
            // 1. 프론트엔드에서 온 type 코드("multiple", "short", "mixed")를 
            //    AI가 이해하기 쉬운 '구체적 지시사항'으로 변환
            String typeInstruction = switch (request.getType()) {
                case "multiple" -> "모든 문제를 4지선다 객관식(quizTypeCode: 2)으로만 출제하세요. 주관식은 포함하지 마세요.";
                case "short" -> "모든 문제를 단답형 주관식(quizTypeCode: 1)으로만 출제하세요. 객관식 보기는 절대 만들지 마세요.";
                case "mixed" -> "객관식(quizTypeCode: 2)과 주관식(quizTypeCode: 1)을 적절히 섞어서 출제하세요.";
                default -> "객관식 위주로 출제하세요.";
            };

            // 2. 변환된 지시사항(typeInstruction)을 AI 서비스에 전달
            String jsonResult = quizAiService.generateQuiz(
                    request.getTopic(),
                    request.getDifficulty(),
                    request.getCount(),
                    typeInstruction // ⭐ 여기가 핵심! ("multiple" 대신 긴 문장이 들어감)
            );

            // 3. 마크다운 제거 후 반환 (기존 로직)
            if (jsonResult.contains("```json")) {
                jsonResult = jsonResult.replaceAll("```json", "").replaceAll("```", "");
            }
            jsonResult = jsonResult.trim();

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(jsonResult);

        } catch (Exception e) {
            log.error("AI 생성 실패", e);
            return ResponseEntity.internalServerError().body("AI 문제 생성 중 오류 발생");
        }
    }
}