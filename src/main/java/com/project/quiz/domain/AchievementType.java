package com.project.quiz.domain;

public enum AchievementType {
    ATTENDANCE_COUNT,   // 누적 출석
    CONSECUTIVE_LOGIN,  // 연속 출석 (로직 복잡)
    QUIZ_SOLVED,        // 퀴즈 풀기 (정답 무관)
    QUIZ_CORRECT,       // 퀴즈 정답
    ITEM_COLLECTOR      // 아이템 수집
}