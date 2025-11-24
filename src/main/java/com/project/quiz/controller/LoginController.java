package com.project.quiz.controller;

import com.project.quiz.dto.UserCreateForm;
import com.project.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor // 생성자 주입을 위해 추가
@Controller
public class LoginController {

    private final UserService userService; // 서비스 주입

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup"; // signup.html 보여줌
    }

    // ▼▼▼ 회원가입 처리 로직 추가 ▼▼▼
    @PostMapping("/register")
    public String signup(UserCreateForm userCreateForm) {
        // 서비스 호출하여 회원 생성
        userService.create(userCreateForm.getUsername(), 
                           userCreateForm.getEmail(), 
                           userCreateForm.getPassword());
        
        // 가입 완료 후 로그인 페이지로 이동
        return "redirect:/login"; 
    }
    
    @GetMapping("/forgot")
    public String forgotPasswordPage() {
        return "forgot";
    }
}