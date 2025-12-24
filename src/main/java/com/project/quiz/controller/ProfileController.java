package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserAchievement;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.TimelineDto;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserProfileRepository;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.InventoryService;
import com.project.quiz.service.TimelineService;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProfileController {

	private final UserService userService;
	private final InventoryService inventoryService;
	private final UserRepository userRepository;// 서비스 주입
	private final TimelineService timelineService;
	private final UserAchievementRepository userAchievementRepository;
	private final UserProfileRepository userProfileRepository;

	// 프로필 페이지 이동
	@GetMapping({ "/profile", "/profile/{profileId}" })
	public String profilePage(@PathVariable(value = "profileId", required = false) String profileId, // ⭐ String으로 받음
			Model model, Principal principal) {

		// 1. 로그인 체크 (내 정보)
		User currentUser = null;
		if (principal != null) {
			currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
		}

		// 2. 보여줄 대상(Target User) 결정
		User targetUser = null;

		if (profileId != null) {
			// (A) URL에 ID가 있다 -> 남의 프로필 (또는 링크 타고 온 내 프로필)
			// UUID 문자열로 UserProfile을 먼저 찾고 -> 그 주인의 User 정보를 가져옴
			UserProfile targetProfile = userProfileRepository.findById(profileId)
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로필입니다."));
			targetUser = targetProfile.getUser();
		} else if (currentUser != null) {
			// (B) URL에 ID가 없다 -> 내 프로필 메뉴 클릭
			targetUser = currentUser;
		} else {
			// (C) 로그인도 안 했고 ID도 없음 -> 로그인 페이지로
			return "redirect:/login";
		}

		// 3. 주인 여부 확인 (isOwner)
		// 내 프로필이면 true -> [설정] 버튼 보임
		// 남의 프로필이면 false -> [설정] 숨김, [친구추가] 보임
		boolean isOwner = (currentUser != null && targetUser.getId().equals(currentUser.getId()));

		// 4. 모델에 데이터 담기 (targetUser 기준!)
		if (targetUser != null) {
			// 화면에는 "user"라는 이름으로 targetUser 정보를 넘겨주면, HTML 수정 없이 그대로 뜸
			model.addAttribute("user", targetUser);
			model.addAttribute("isOwner", isOwner); // ⭐ HTML에서 버튼 분기 처리용

			// 인벤토리, 업적 등도 모두 'targetUser' 기준으로 조회
			String borderUrl = inventoryService.getEquippedItemUrl(targetUser, "BORDER");
			model.addAttribute("equippedBorderUrl", borderUrl);

			String themeUrl = inventoryService.getEquippedItemUrl(targetUser, "THEME");
			model.addAttribute("equippedThemeUrl", themeUrl);

			List<UserAchievement> achievements = userAchievementRepository
					.findByUserAndIsAchievedTrueOrderByAchievedAtAsc(targetUser);
			model.addAttribute("achievements", achievements);

			// 타임라인도 targetUser 것 조회
			List<TimelineDto> timeline = timelineService.getProfileTimeline(targetUser.getEmail());
			model.addAttribute("timelineList", timeline);
		}

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
		return "timeline"; // timeline.html 껍데기만 렌더링
	}

	// ▼▼▼ [POST] 프로필 저장 로직 추가 ▼▼▼
	@PostMapping("/profile/settings/profilesave")
	public String saveProfile(@RequestParam("username") String username,
			@RequestParam("organization") String organization, @RequestParam("bio") String bio,
			@RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
			@RequestParam(value = "defaultProfileImage", required = false) String defaultProfileImage, // [추가]
			Principal principal) {

		if (principal != null) {
			// 파라미터 5개 모두 서비스로 전달
			userService.updateProfile(principal.getName(), username, organization, bio, profileImageFile,
					defaultProfileImage);
		}

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