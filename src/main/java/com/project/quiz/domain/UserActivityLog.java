package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "activity_type")
    private String activityType; // 예: "PURCHASE", "QUIZ", "ATTENDANCE"

    @Column(name = "description")
    private String description; // 간단 설명

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}