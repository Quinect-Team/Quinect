package com.project.quiz.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.OAuthAttributes;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 통해 소셜 서비스에서 유저 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 어떤 서비스인지(google, kakao) 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 3. OAuthAttributes를 통해 데이터 정제
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 4. 유저 저장 또는 업데이트
        User user = saveOrGet(attributes);
        
        Map<String, Object> newAttributes = new HashMap<>(attributes.getAttributes());
        
        // 우리가 DTO에서 꺼내둔 email을 맵에 확실하게 넣어줍니다.
        newAttributes.put("email", attributes.getEmail()); 

        // 반환할 때 키값(nameAttributeKey)을 "email"로 지정합니다.
        // 이제 principal.getName()을 호출하면 이메일이 반환됩니다!
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                newAttributes,
                "email");
    }

    private User saveOrGet(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .orElseGet(() -> {
                    // 신규 유저 생성 시
                    User newUser = attributes.toEntity(); // 여기엔 email, role 등만 있음
                    
                    // 프로필 생성
                    UserProfile profile = UserProfile.builder()
                            .username(attributes.getUsername())
                            .pointBalance(100L)
                            .build();
                    
                    newUser.setUserProfile(profile);
                    return newUser;
                });

        return userRepository.save(user);
    }
}