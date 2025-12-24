package com.project.quiz.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // 1. 로그인한 유저 정보 조회
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        // 2. 계정 상태 확인
        if (user != null && "pending".equals(user.getStatus())) {
            // (A) 탈퇴 대기 중이면 -> 로그인 페이지로 다시 이동 (파라미터 추가)
            // 로그인 세션은 유지된 상태지만, 화면만 로그인 페이지를 보여줌
            response.sendRedirect("/login?status=pending");
        } else {
            // (B) 정상 회원이면 -> 메인으로 이동
            response.sendRedirect("/main");
        }
    }
}