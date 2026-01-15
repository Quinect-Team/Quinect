package com.project.quiz.domain;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    public int getDisplayLength() {
        if (username == null) return 0;

        int length = 0;
        for (char c : username.toCharArray()) {
            // 한글 범위 (Hangul Syllables) 체크
            if (c >= '가' && c <= '힣') {
                length += 2; // 한글은 2로 계산 (EUC-KR 처럼)
            } else {
                length += 1; // 영어, 숫자, 특수문자는 1로 계산
            }
        }
        return length;
    }
}