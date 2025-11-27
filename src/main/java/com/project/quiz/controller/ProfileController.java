package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.InventoryService;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProfileController {

	private final UserService userService;
	private final InventoryService inventoryService;
	private final UserRepository userRepository;// 서비스 주입

	// 프로필 페이지 이동
	@GetMapping("/profile")
	public String profilePage() {
		return "profile";
	}

	// 설정 페이지 이동 // 이제 목록 불러오기 추가됨
	@GetMapping("/profile/settings")
	public String settingsPage(Model model, Principal principal,
			// ▼▼▼ [Back] 어디서 왔는지 체크 (기본값: profile) ▼▼▼
			@RequestParam(value = "source", defaultValue = "profile") String source) {

		if (principal != null) {
			User user = userRepository.findByEmail(principal.getName()).orElseThrow();

			List<UserInventory> myBorders = inventoryService.getMyInventoryByCategory(user, "BORDER");
			List<UserInventory> myThemes = inventoryService.getMyInventoryByCategory(user, "THEME");

			model.addAttribute("myBorders", myBorders);
			model.addAttribute("myThemes", myThemes);

			// 화면에 source 정보 전달 (뒤로가기 버튼용)
			model.addAttribute("source", source);
		}
		return "profilesettings";
	}

	// ▼▼▼ 타임라인 페이지 (기존에 있었으므로 유지) ▼▼▼
	@GetMapping("/profile/timeline")
	public String timelinePage() {
		return "timeline";
	}

	// ▼▼▼ [POST] 프로필 저장 로직 추가 ▼▼▼
	@PostMapping("/profile/settings/profilesave")
	public String saveProfile(@RequestParam("username") String username,
			@RequestParam("organization") String organization, @RequestParam("bio") String bio, Principal principal) {

		if (principal != null) {
			userService.updateProfile(principal.getName(), username, organization, bio);
		}

		// ▼▼▼ [Next] 성공 신호 + 이동할 목적지(profile) 지정 ▼▼▼
		return "redirect:/profile/settings?status=success&next=/profile";
	}

	// [POST] 장착 저장
	@PostMapping("/profile/settings/equip")
	public String equipItems(@RequestParam(value = "borderId", required = false) Long borderId,
			@RequestParam(value = "themeId", required = false) Long themeId, Principal principal) {

		User user = userRepository.findByEmail(principal.getName()).orElseThrow();

		inventoryService.equipItem(user, "BORDER", borderId);
		inventoryService.equipItem(user, "THEME", themeId);

		// ▼▼▼ [Next] 성공 신호 + 이동할 목적지(profile) 지정 ▼▼▼
		return "redirect:/profile/settings?status=success&next=/profile";
	}
}