package com.project.quiz.controller;

import com.project.quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 탈퇴 버튼 클릭 시
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(Principal principal, HttpServletRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();

        // 1) DB 상태 변경
        userService.withdraw(principal.getName());

        // 2) 강제 로그아웃 처리
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok("탈퇴 처리가 완료되었습니다.");
    }

    // 2. 복구 버튼 클릭 시
    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivate(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        userService.reactivate(principal.getName());
        return ResponseEntity.ok("계정이 복구되었습니다.");
    }
}