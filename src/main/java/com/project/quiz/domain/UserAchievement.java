package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievement",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "achievement_id"}) // 유저당 업적은 하나씩만
       })
@Getter
@Setter // 진행도는 계속 바뀌므로 Setter 허용 (또는 비즈니스 메서드 사용)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(nullable = false)
    @Builder.Default
    private Long currentValue = 0L; // 현재 달성 수치 (예: 35/50 이면 35)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAchieved = false; // 목표 달성 여부

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRewarded = false; // 보상 수령 여부 (보상받기 버튼 클릭 체크용)

    private LocalDateTime achievedAt; // 달성 시각

    private LocalDateTime lastUpdatedAt; // 마지막 진행 시간 (연속 출석 체크용)

    // 편의 메서드: 진행도 증가
    public void incrementValue() {
        this.currentValue++;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    // 편의 메서드: 달성 처리
    public void markAchieved() {
        this.isAchieved = true;
        this.achievedAt = LocalDateTime.now();
    }

    // 편의 메서드: 보상 수령 처리
    public void markRewarded() {
        this.isRewarded = true;
    }
}