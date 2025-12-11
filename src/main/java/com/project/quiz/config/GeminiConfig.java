package com.project.quiz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.quiz.service.GeminiTestService; // 인터페이스 import

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices; // 이 import가 중요합니다!

@Configuration
public class GeminiConfig {
	
	@Value("${gemini.api.key}")
    private String apiKey;

    // 1. Gemini 모델 생성 (이건 기존과 동일)
    @Bean
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey) // ★ 꼭 확인: 키가 비어있지 않은지!
                .modelName("gemini-2.5-flash-lite")
                .temperature(0.7)
                .build();
    }

    // 2. 서비스와 모델을 강제로 연결 (★ 여기를 추가하세요)
    @Bean
    public GeminiTestService geminiTestService(ChatLanguageModel model) {
        // "GeminiTestService 인터페이스를 저 모델(model)을 써서 구현해라" 라고 명시
        return AiServices.create(GeminiTestService.class, model);
    }
}