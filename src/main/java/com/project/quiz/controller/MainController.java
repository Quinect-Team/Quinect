package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // 1. 루트 접속 시 /index로 리다이렉트 (또는 바로 index 보여주기)
    @GetMapping("/")
    public String root() {
        return "redirect:/index";
    }

    // 2. 인덱스 페이지 (비로그인 접근 가능)
    @GetMapping("/index")
    public String index() {
        return "index"; // index.html 필요 (미완성이라도 빈 파일 필요)
    }

    // 3. 메인 페이지 (로그인 후 접근)
    @GetMapping("/main")
    public String mainPage() {
        return "main";  // main.html 필요
    }
}