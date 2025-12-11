package com.project.quiz.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import com.project.quiz.dto.AiQuizResponse;

public interface GeminiTestService {

    @SystemMessage("""
        당신은 퀴즈 생성 도우미입니다.
        사용자가 주제를 주면 4지 선다형 퀴즈 3문제를 만들어주세요.
        난이도는 '중'입니다.
        반드시 주어진 JSON 형식(AiQuizResponse)에 맞춰서 대답하세요.
    """)
    @UserMessage("주제: {{topic}}")
    AiQuizResponse createQuiz(@V("topic") String topic);
}