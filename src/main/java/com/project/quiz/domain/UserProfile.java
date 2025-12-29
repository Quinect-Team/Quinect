package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {

	@Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // ▼▼▼ User 테이블의 PK를 FK로 가짐 (여기가 주인) ▼▼▼
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String username;      // 닉네임 (이사옴)
    
    @Builder.Default
    @Column(nullable = true)
    private String profileImage = "/img/undraw_profile.svg";  // 기본 이미지
    
    @Builder.Default
    private Long pointBalance = 100L;    // 포인트 (이사옴)
    
    @Column(length = 255)
    private String bio;           // 상태 메시지

    @Column(length = 100)
    private String organization;  // 소속 (새로 추가됨)
}