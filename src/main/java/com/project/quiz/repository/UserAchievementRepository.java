package com.project.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.Achievement;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserAchievement;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    // 유저의 특정 업적 진행 상황 조회
    Optional<UserAchievement> findByUserAndAchievement(User user, Achievement achievement);
    
    // 유저가 완료한 업적 목록 (프로필 표시용)
    List<UserAchievement> findByUserAndIsAchievedTrue(User user);
    
    List<UserAchievement> findAllByUser(User user);
    
    long countByUserAndIsAchievedTrue(User user);
}