package com.project.quiz.repository;

import com.project.quiz.domain.Notification; // Entity는 만들었다고 가정
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {}