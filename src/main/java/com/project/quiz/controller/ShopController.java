package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.quiz.dto.ShopItemResponse;
import com.project.quiz.service.ShopService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ShopController {

	private final ShopService shopService;

	// 상점 페이지 (DB 조회)
	@GetMapping("/shop")
	public String shopPage(Model model, Principal principal,
	        @RequestParam(name = "category", required = false) String category,
	        @RequestParam(name = "ajax", required = false) String ajax) { // [추가] AJAX 헤더 확인용

        // 로그인한 사람(principal) 확인
        String email = (principal != null) ? principal.getName() : "";

        // 서비스에 category 전달
        List<ShopItemResponse> items = shopService.getAvailableItems(email, category);
        model.addAttribute("items", items);

        // [핵심 변경] AJAX 요청("XMLHttpRequest")이 들어오면 타임리프 조각(Fragment)만 반환
        if ("true".equals(ajax)) {
            return "layout/shop :: itemGrid"; 
        }

        return "shop"; // 일반 요청은 전체 페이지 반환
    }

	// [POST] 아이템 구매 처리
	@PostMapping("/shop/buy")
	public String buyItem(@RequestParam("itemId") Long itemId, Principal principal,
			RedirectAttributes redirectAttributes) {
		try {
			shopService.buyItem(principal.getName(), itemId);
			redirectAttributes.addFlashAttribute("message", "구매가 완료되었습니다!");
		} catch (IllegalStateException e) {
			// 포인트 부족 등의 에러
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "구매 중 오류가 발생했습니다.");
		}
		return "redirect:/shop";
	}
}