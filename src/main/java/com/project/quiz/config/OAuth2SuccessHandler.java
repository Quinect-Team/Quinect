package com.project.quiz.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Boolean isNew = (Boolean) oAuth2User.getAttribute("isNew");

        if (Boolean.TRUE.equals(isNew)) {
            // ⭐ [수정] 세션을 폭파하지 않고, '인증 정보'만 삭제합니다.
            // 이렇게 하면 SecurityConfig의 invalidSessionUrl 감지 로직에 걸리지 않습니다.
            
            // 1. 시큐리티 컨텍스트 비우기 (실질적 로그아웃)
            SecurityContextHolder.clearContext();
            
            // 2. 세션에 남아있는 시큐리티 속성만 제거 (세션 자체는 유지)
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute("SPRING_SECURITY_CONTEXT");
            }

            // 3. 이제 안전하게 가입 성공 페이지로 이동 (비로그인 상태)
            getRedirectStrategy().sendRedirect(request, response, "/signup/success");
            
        } else {
            // 기존 회원은 로그인 유지한 채 메인으로
            getRedirectStrategy().sendRedirect(request, response, "/main");
        }
    }
}