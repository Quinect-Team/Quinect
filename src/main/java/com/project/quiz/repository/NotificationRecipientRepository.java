package com.project.quiz.repository;

import com.project.quiz.domain.NotificationRecipient;
import com.project.quiz.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    // 1. 특정 유저의 알림 목록을 최신순으로 가져오기 (User 객체 기준)
    List<NotificationRecipient> findByUserOrderByNotification_CreatedAtDesc(User user);

    // 2. 특정 유저의 '안 읽은' 알림 개수 세기
    long countByUserAndIsReadFalse(User user);
}