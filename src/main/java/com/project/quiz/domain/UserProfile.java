package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ▼▼▼ User 테이블의 PK를 FK로 가짐 (여기가 주인) ▼▼▼
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String username;      // 닉네임 (이사옴)
    private String profileImage;  // 이미지 (이사옴)
    private Long pointBalance;    // 포인트 (이사옴)
    
    @Column(length = 255)
    private String bio;           // 상태 메시지

    @Column(length = 100)
    private String organization;  // 소속 (새로 추가됨)
}