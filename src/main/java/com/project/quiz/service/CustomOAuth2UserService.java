package com.project.quiz.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final PasswordEncoder passwordEncoder;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		// 1. 소셜 유저 정보 가져오기 (기존 코드)
		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
				.getUserNameAttributeName();

		// 2. 데이터 정제 (기존 코드)
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
				oAuth2User.getAttributes());

		// ⭐ [수정] 신규 회원인지 먼저 확인! (DB에 이메일이 없으면 신규)
		boolean isNewUser = !userRepository.existsByEmail(attributes.getEmail());

		// 3. 저장 또는 업데이트 (기존 코드)
		User user = saveOrGet(attributes);

		// 4. 리턴할 속성 맵 구성
		Map<String, Object> newAttributes = new HashMap<>(attributes.getAttributes());
		newAttributes.put("email", attributes.getEmail());

		// ⭐ [추가] 'isNew'라는 이름으로 신규 회원 여부를 담음
		newAttributes.put("isNew", isNewUser);

		return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRole())), newAttributes,
				"email");
	}

	private User saveOrGet(OAuthAttributes attributes) {
		User user = userRepository.findByEmail(attributes.getEmail()).orElseGet(() -> {
			// 신규 유저 생성 시
			User newUser = attributes.toEntity(); // 여기엔 email, role 등만 있음
			
			String randomPassword = UUID.randomUUID().toString();
            newUser.setPassword(passwordEncoder.encode(randomPassword));

			// 프로필 생성
			UserProfile profile = UserProfile.builder().username(attributes.getUsername()).pointBalance(100L).build();

			newUser.setUserProfile(profile);
			return newUser;
		});

		return userRepository.save(user);
	}
}