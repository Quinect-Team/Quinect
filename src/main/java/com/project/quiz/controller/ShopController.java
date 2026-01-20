package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
			@RequestParam(name = "category", required = false) String category) {

		// 로그인한 사람(principal) 확인
		String email = (principal != null) ? principal.getName() : "";

		// [수정] 서비스에 category 전달
		List<ShopItemResponse> items = shopService.getAvailableItems(email, category);

		model.addAttribute("items", items);

		// (선택 사항) 현재 선택된 카테고리가 무엇인지 뷰에 다시 전달할 필요가 있을 수 있음
		// 하지만 shop.html 수정본에서 ${param.category}를 사용하므로 model에 안 담아도 동작합니다.

		return "shop";
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