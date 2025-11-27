package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_activity_point_change")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityPointChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id")
    private UserActivityLog userActivityLog; // 로그와 1:1 연결

    private int amount; // 변동량 (+500, -300)

    private String reason; // 상세 사유 ("전설의 검 구매")
}