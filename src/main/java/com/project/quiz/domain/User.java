package com.project.quiz.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;
    
    @JsonIgnore
    private String password;
    
    private String provider;    // google, kakao
    private String providerId;  // sub, id
    private String role;        // USER, ADMIN
    private String status;      // ACTIVE, INACTIVE (계정 상태 유지)
    
    private LocalDateTime createdAt;

    // ▼▼▼ 핵심: UserProfile과 1:1 연결 ▼▼▼
    // CascadeType.ALL: User를 저장하면 Profile도 같이 저장됨
    // orphanRemoval = true: 연결 끊으면 Profile 데이터도 삭제됨
    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private UserProfile userProfile;

    // 편의 메서드: 연관관계 편의 메서드 (양방향 세팅용)
    public void setUserProfile(UserProfile profile) {
        this.userProfile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }
}