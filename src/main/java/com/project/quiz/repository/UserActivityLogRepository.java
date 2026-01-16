package com.project.quiz.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    
    // 오늘 날짜 범위(start~end)에 해당 유저의 특정 활동(ATTENDANCE)이 있는지 확인
    // 인덱스를 타기 때문에 매우 빠름
    boolean existsByUserAndActivityTypeAndCreatedAtBetween(
        User user, 
        String activityType, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    Page<UserActivityLog> findAllByUserAndActivityTypeIn(User user, List<String> activityTypes, Pageable pageable);
    Optional<UserActivityLog> findTopByUserAndActivityTypeOrderByCreatedAtDesc(User user, String activityType);
}