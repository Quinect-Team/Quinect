package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보낸 사람 ID (시스템 알림인 경우 null 또는 관리자 ID, 필요시 User 연관관계로 변경 가능)
    @Column(name = "sender_id")
    private Long senderId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "notification_type", length = 50)
    private String notificationType; // 예: "ACHIEVEMENT", "NOTICE", "FRIEND_REQUEST"

    @Column(name = "is_broadcast")
    @Builder.Default
    private Boolean isBroadcast = false; // 전체 공지 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt; // 알림 만료 기간 (옵션)

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}