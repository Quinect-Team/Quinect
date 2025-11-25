package com.project.quiz.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // 1. 루트 접속 시 /index로 리다이렉트 (또는 바로 index 보여주기)
	@GetMapping("/")
    public String root(Principal principal) {
        // 1. 로그인한 사용자(Principal이 null이 아님)라면 -> 메인으로
        if (principal != null) {
            return "redirect:/main";
        }
        
        // 2. 로그인 안 한 손님이라면 -> 인덱스로
        return "redirect:/index";
    }

    // 2. 인덱스 페이지 (비로그인 접근 가능)
	@GetMapping("/index")
    public String index(Principal principal) {
        if (principal != null) {
            return "redirect:/main";
        }
        return "index";
    }

    // 3. 메인 페이지 (로그인 후 접근)
    @GetMapping("/main")
    public String mainPage() {
        return "main";  // main.html 필요
    }
}