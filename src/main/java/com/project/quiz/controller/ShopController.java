package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ShopController {
	@GetMapping("/shop")
    public String shopPage() {
        return "shop";
    }

    // [POST] 아이템 구매 처리
    @PostMapping("/shop/buy")
    public String buyItem(@RequestParam String itemName) {
        // 1. 구매 로직 처리 (DB 포인트 차감, 아이템 지급 등)
        // 2. 처리 후 상점 페이지로 리다이렉트
        return "redirect:/shop";
    }
}
