package com.project.quiz.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Optional;


import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User create(String username, String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        // ▼▼▼ 프로필 별도 생성 후 연결 ▼▼▼
        UserProfile profile = UserProfile.builder()
                .username(username)       // 닉네임
                .pointBalance(100L)       // 기본 포인트
                .build();

        user.setUserProfile(profile); // 연관관계 설정 (User 저장 시 Profile도 자동 저장됨)
        
        return userRepository.save(user);
    }


	public User findByEmail(String name) {
		return userRepository.findByEmail(name).orElse(null);
	}

    @Transactional // DB 변경이 일어나므로 트랜잭션 필수
    public void updateProfile(String email, String newNickname, String newOrganization, String newBio) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        
        // 2. 연결된 프로필 가져오기
        UserProfile profile = user.getUserProfile();
        
        // 3. 값 변경 (Dirty Checking에 의해 자동 저장됨)
        if(profile != null) {
            profile.setUsername(newNickname);
            profile.setOrganization(newOrganization);
            profile.setBio(newBio);
        }
        // (만약 profile이 null일 경우는 회원가입 로직상 없겠지만, 필요하다면 여기서 생성 로직 추가 가능)
    }
}