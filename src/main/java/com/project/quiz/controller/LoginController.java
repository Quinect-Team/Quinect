package com.project.quiz.controller; // 패키지명은 본인 프로젝트에 맞게 수정

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; 
    }
    
    @GetMapping("/signup") 
    public String signupPage() {
        return "signup";
    }
    
    @GetMapping("/forgot")
    public String forgotPasswordPage() {
        return "forgot";
    }
}