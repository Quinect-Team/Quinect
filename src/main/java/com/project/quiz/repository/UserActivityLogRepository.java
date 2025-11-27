package com.project.quiz.repository;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    
    // 오늘 날짜 범위(start~end)에 해당 유저의 특정 활동(ATTENDANCE)이 있는지 확인
    // 인덱스를 타기 때문에 매우 빠름
    boolean existsByUserAndActivityTypeAndCreatedAtBetween(
        User user, 
        String activityType, 
        LocalDateTime start, 
        LocalDateTime end
    );
}