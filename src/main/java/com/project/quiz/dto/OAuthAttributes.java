package com.project.quiz.dto;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    // 서비스에서 호출하는 진입점
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        // "kakao"로 들어오면 카카오 로직 실행
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        // 나머지는 구글 로직 실행
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .username((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .provider("google")
                .providerId((String) attributes.get("sub"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // ▼▼▼ [핵심] 카카오 전용 데이터 추출 로직 ▼▼▼
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 1. kakao_account 꺼내기
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        
        // 2. profile 꺼내기 (닉네임은 여기 있음)
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .username((String) kakaoProfile.get("nickname")) // 닉네임
                .email((String) kakaoAccount.get("email"))       // 이메일
                .picture((String) kakaoProfile.get("thumbnail_image_url"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id"))) // 카카오의 고유 ID (숫자)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // toEntity는 이전에 수정한 것(User/UserProfile 분리 버전) 그대로 유지
    public User toEntity() {
        User user = User.builder()
                .email(email)
                .role("USER")
                .status("ACTIVE")
                .provider(provider)
                .providerId(providerId)
                .password(UUID.randomUUID().toString()) 
                .createdAt(LocalDateTime.now())
                .build();

        UserProfile profile = UserProfile.builder()
                .username(username)
                .profileImage(null)  // 사진 저장 안 함
                .pointBalance(100L)
                .organization(null)  // 소속 빈칸
                .build();

        user.setUserProfile(profile);

        return user;
    }
}