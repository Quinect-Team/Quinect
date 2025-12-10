package com.project.quiz.dto;

import com.project.quiz.domain.Achievement;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AchievementDisplayDto {
    private Achievement achievement; // 업적 정보 (제목, 목표치 등)
    private long currentValue;       // 내 현재 수치 (예: 12)
    private boolean isAchieved;      // 달성 여부
    private int percent;             // 퍼센트 (프로그레스 바용, 0~100)

    public static AchievementDisplayDto of(Achievement achievement, long currentValue, boolean isAchieved) {
        // 퍼센트 계산 (목표가 0이면 0%)
        int percent = 0;
        if (achievement.getGoalValue() > 0) {
            percent = (int) ((double) currentValue / achievement.getGoalValue() * 100);
            if (percent > 100) percent = 100;
        }

        return AchievementDisplayDto.builder()
                .achievement(achievement)
                .currentValue(currentValue)
                .isAchieved(isAchieved)
                .percent(percent)
                .build();
    }
}