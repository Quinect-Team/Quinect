package com.project.quiz.service;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User create(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        
        // 비밀번호 암호화 (SecurityConfig에 등록된 Bean 사용)
        user.setPassword(passwordEncoder.encode(password));
        
        user.setRole("USER");            // 기본 권한
        user.setPointBalance(100L);     // 가입 축하 기본 포인트 (원하는 만큼 설정)
        user.setStatus("ACTIVE");        // 계정 상태
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
}