package com.project.quiz.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService; // 서비스 주입

    // 프로필 페이지 이동
    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }

    // 설정 페이지 이동
    @GetMapping("/profile/settings")
    public String settingsPage() {
        return "profilesettings";
    }

    // ▼▼▼ 타임라인 페이지 (기존에 있었으므로 유지) ▼▼▼
    @GetMapping("/profile/timeline")
    public String timelinePage() {
        return "timeline";
    }

    // ▼▼▼ [POST] 프로필 저장 로직 추가 ▼▼▼
    @PostMapping("/profile/settings/save")
    public String saveProfile(
            @RequestParam("username") String username,
            @RequestParam("organization") String organization,
            @RequestParam("bio") String bio,
            Principal principal) { // Principal: 로그인한 사람 정보
        
        if (principal != null) {
            // 서비스 호출해서 업데이트
            userService.updateProfile(principal.getName(), username, organization, bio);
        }

        // 저장 후 다시 프로필 페이지로 이동
        return "redirect:/profile";
    }
}