package com.project.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "achievement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;       // 업적명 (예: "성실한 출석러")

    @Column(nullable = false)
    private String description; // 설명 (예: "출석 50회 달성 시 지급")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementType achievementType; // 달성 조건 타입 (ATTENDANCE, QUIZ, etc.)

    @Column(nullable = false)
    private Long goalValue;     // 목표 수치 (예: 50)

    // 보상 아이템 (메달 등) - ShopItem과 연결 (없을 수도 있으니 nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_item_id")
    private ShopItem rewardItem;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 지금 진행 중인 업적인지 여부
}