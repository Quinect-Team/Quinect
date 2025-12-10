package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.Achievement;
import com.project.quiz.domain.AchievementType;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    // 특정 행동(예: 출석)에 걸려있는 활성 업적들을 모두 가져옴 (캐싱 권장)
    List<Achievement> findByAchievementTypeAndIsActiveTrue(AchievementType achievementType);
}
