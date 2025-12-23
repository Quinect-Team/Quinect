package com.project.quiz.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface QuizAiService {

    @SystemMessage("""
        역할: 당신은 퀴즈 출제 전문가입니다.
        
        [제약사항]
        1. 서론, 결론, 마크다운(```json) 없이 **오직 순수 JSON**만 반환하세요.
        2. **quizTypeCode**: 1(주관식), 2(객관식) 준수.
        
        [필수 JSON 반환 구조]
        {
          "title": "제목",
          "description": "설명",
          "questions": [
            {
              "questionText": "질문 내용",
              "quizTypeCode": 1 또는 2,
              "point": 10,
              "options": [ "보기1", "보기2", "보기3", "보기4" ] (※ 중요: 객체 금지. 단순 문자열 배열로 반환. 주관식은 빈배열 []),
              "answerOption": "1" (※ 객관식 정답 번호 '1'~'4'. 주관식 null),
              "subjectiveAnswer": "정답" (※ 주관식 정답. 객관식 null)
            }
          ]
        }
    """)
    @UserMessage("""
        주제: {{topic}}
        난이도: {{difficulty}}
        문제 수: {{count}}개
        유형: {{type}}
        
        위 조건에 맞춰 한국어로 퀴즈를 생성하세요.
    """)
    String generateQuiz(
        @V("topic") String topic, 
        @V("difficulty") String difficulty, 
        @V("count") int count, 
        @V("type") String type
    );
}