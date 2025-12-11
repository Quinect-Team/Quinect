package com.project.quiz.dto;

import java.util.List;

// AI가 이 구조로 JSON을 만들어줍니다.
public record AiQuizResponse(
    String title,
    String description,
    List<AiQuestion> questions
) {
    public record AiQuestion(
        String questionText,
        List<String> options, // 보기 4개
        int answerIndex,      // 정답 번호 (1~4)
        String explanation    // 해설
    ) {}
}