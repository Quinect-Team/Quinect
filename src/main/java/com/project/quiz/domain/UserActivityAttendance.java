package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_activity_attendance")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ 핵심: user_id 컬럼 삭제! 오직 로그 ID로만 연결 (1:1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id", nullable = false)
    private UserActivityLog userActivityLog;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "points_earned")
    private int pointsEarned;
}