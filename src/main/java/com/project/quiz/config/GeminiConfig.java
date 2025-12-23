package com.project.quiz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.quiz.service.GeminiTestService; // 기존꺼
import com.project.quiz.service.QuizAiService;   // ⭐ [추가] 새로 만든 인터페이스

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

@Configuration
public class GeminiConfig {
    
    @Value("${gemini.api.key}") // application.properties 확인 필수
    private String apiKey;

    // 1. 모델 생성
    @Bean
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash-lite") // ⭐ 모델명 확인 (2.5는 아직 없을 수 있음, 1.5 flash 추천)
                .temperature(0.7)
                .build();
    }

    // 2. 기존 테스트 서비스 (유지)
    @Bean
    public GeminiTestService geminiTestService(ChatLanguageModel model) {
        return AiServices.create(GeminiTestService.class, model);
    }

    // 3. ⭐ [추가] 퀴즈 생성 서비스 등록
    @Bean
    public QuizAiService quizAiService(ChatLanguageModel model) {
        return AiServices.create(QuizAiService.class, model);
    }
}