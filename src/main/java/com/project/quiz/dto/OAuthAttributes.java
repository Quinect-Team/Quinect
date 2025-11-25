package com.project.quiz.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;

import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String username;
    private String email;
    private String picture;
    private String provider;
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, 
                           String username, String email, String picture, 
                           String provider, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.username = username;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 서비스에서 호출하는 메소드 (구글인지 카카오인지 구분)
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .username((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .provider("google")
                .providerId((String) attributes.get("sub")) // 구글의 PK
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 카카오는 kakao_account 안에 profile이 있는 중첩 구조
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .username((String) kakaoProfile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .picture((String) kakaoProfile.get("profile_image_url"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id"))) // 카카오의 PK
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // User 엔티티 생성 (가입 시점)
    public User toEntity() {
        // 1. User 객체 생성 (인증/계정 정보)
        User user = User.builder()
                .email(email)
                .role("USER")
                .status("ACTIVE")
                .provider(provider)
                .providerId(providerId)
                .password(UUID.randomUUID().toString()) // 임시 비밀번호
                .createdAt(LocalDateTime.now())
                .build();

        // 2. UserProfile 객체 생성 (프로필 정보)
        UserProfile profile = UserProfile.builder()
                .username(username)         // 소셜 닉네임
                .profileImage(null)         // 아까 요청하신 대로 사진은 저장 안 함 (null)
                .pointBalance(100L)         // 기본 포인트
                .build();

        // 3. 두 객체 연결 (User가 주인이므로 set 메서드로 연결)
        user.setUserProfile(profile);

        return user; // Profile을 품은 User 반환
    }
}