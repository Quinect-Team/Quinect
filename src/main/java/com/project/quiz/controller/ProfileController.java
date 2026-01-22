package com.project.quiz.controller;

import java.security.Principal;
import java.time.LocalDateTime;
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
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.TimelineDto;
import com.project.quiz.repository.QuizSubmissionRepository;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserProfileRepository;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.FriendshipService;
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
	private final UserActivityLogRepository userActivityLogRepository;
	private final FriendshipService friendshipService;
    private final QuizSubmissionRepository quizSubmissionRepository;

	// 프로필 페이지 이동
	@GetMapping({ "/profile", "/profile/{profileId}" })
	public String profilePage(
	        @PathVariable(value = "profileId", required = false) String profileId,
	        Model model, 
	        Principal principal
	) { 
	    // 1. 로그인 체크 및 사용자 로드
	    User currentUser = null;
	    if (principal != null) {
	        currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
	    }

	    // 2. 보여줄 대상(Target User) 결정
	    User targetUser = null;
	    if (profileId != null) {
	        // (A) 타인 프로필
	        UserProfile targetProfile = userProfileRepository.findById(profileId)
	                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로필입니다."));
	        targetUser = targetProfile.getUser();
	    } else if (currentUser != null) {
	        // (B) 내 프로필
	        targetUser = currentUser;
	    } else {
	        return "redirect:/login";
	    }

	    // =================================================================
	    // ▼▼▼ [수정됨] 탈퇴한 유저 처리 로직 (Alert 대신 화면 분기용 플래그 전달) ▼▼▼
	    // =================================================================
	    if (targetUser != null && !"ACTIVE".equals(targetUser.getStatus())) {
	        model.addAttribute("isWithdrawn", true); // 🚩 탈퇴 상태 플래그
	        model.addAttribute("isOwner", false);    // 탈퇴한 계정은 내 계정이 아님(또는 접근 불가)
	        // 여기서 바로 리턴하여, 아래의 인벤토리/업적 조회 로직을 건너뜁니다.
	        // (탈퇴한 유저의 정보를 조회하다 에러가 날 수 있으므로 안전하게 스킵)
	        return "profile"; 
	    }
	    // ▲▲▲ 수정 끝 ▲▲▲


	    // 3. 정상 회원인 경우 나머지 데이터 로드 (기존 로직)
	    boolean isOwner = (currentUser != null && targetUser.getId().equals(currentUser.getId()));
	    
	    if (targetUser != null) {
	        model.addAttribute("user", targetUser); // 🚩 정상 유저 정보 담기
	        model.addAttribute("isOwner", isOwner);
	        model.addAttribute("isWithdrawn", false);
	        
	        long solvedQuizCount = quizSubmissionRepository.countByUserId(targetUser.getId());
            model.addAttribute("solvedQuizCount", solvedQuizCount);

            // 2. 친구 수 (FriendshipService 활용)
            // (기존 서비스 메서드가 리스트를 반환하므로 .size()로 개수 파악)
            long friendCount = friendshipService.getAcceptedFriends(targetUser.getId()).size();
            model.addAttribute("friendCount", friendCount);

	        // 인벤토리, 업적, 타임라인 등 조회
	        String borderUrl = inventoryService.getEquippedItemUrl(targetUser, "BORDER");
	        model.addAttribute("equippedBorderUrl", borderUrl);

	        String themeUrl = inventoryService.getEquippedItemUrl(targetUser, "THEME");
	        model.addAttribute("equippedThemeUrl", themeUrl);

	        List<UserAchievement> achievements = userAchievementRepository
	                .findByUserAndIsAchievedTrueOrderByAchievedAtAsc(targetUser);
	        model.addAttribute("achievements", achievements);

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
			
			boolean canChangeNickname = true;
            String nicknameMessage = null;

            UserActivityLog lastUpdate = userActivityLogRepository
                    .findTopByUserAndActivityTypeOrderByCreatedAtDesc(user, "UPDATE_NICKNAME")
                    .orElse(null);

            if (lastUpdate != null) {
                // 마지막 변경 시간 + 7일
                LocalDateTime availableDate = lastUpdate.getCreatedAt().plusDays(7);
                
                // 현재 시간이 제한 시간보다 이전이면 -> 변경 불가
                if (LocalDateTime.now().isBefore(availableDate)) {
                    canChangeNickname = false;
                    // 예: "2025-01-23 15:30 이후 변경 가능"
                    nicknameMessage = availableDate.toString().replace("T", " ").substring(0, 16) + " 이후 변경 가능";
                }
            }

			List<UserInventory> myBorders = inventoryService.getMyInventoryByCategory(user, "BORDER");
			List<UserInventory> myThemes = inventoryService.getMyInventoryByCategory(user, "THEME");

			model.addAttribute("myBorders", myBorders);
			model.addAttribute("myThemes", myThemes);
			
			model.addAttribute("canChangeNickname", canChangeNickname);
            model.addAttribute("nicknameMessage", nicknameMessage);

			// 화면에 source 정보 전달 (뒤로가기 버튼용)
			model.addAttribute("source", source);
		}
		return "profilesettings";
	}

	// ▼▼▼ 타임라인 페이지 (기존에 있었으므로 유지) ▼▼▼
	@GetMapping({ "/profile/timeline", "/profile/timeline/{profileId}" }) // ⭐ URL 패턴 추가
	public String timelinePage(
			@PathVariable(value = "profileId", required = false) String profileId,
			Model model,
			Principal principal) {

		// 1. 로그인 유저 확인
		User currentUser = null;
		if (principal != null) {
			currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
		}

		// 2. 타겟 유저 결정 (profilePage 로직과 동일하게 처리)
		User targetUser = null;
		if (profileId != null) {
			// (A) 타인 프로필 ID가 있는 경우
			UserProfile targetProfile = userProfileRepository.findById(profileId)
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로필입니다."));
			targetUser = targetProfile.getUser();
		} else if (currentUser != null) {
			// (B) 내 프로필인 경우
			targetUser = currentUser;
		} else {
			// 로그인 안 했고 ID도 없으면 로그인 페이지로
			return "redirect:/login";
		}

		// 3. 모델에 데이터 전달
		if (targetUser != null) {
			// 탈퇴한 유저라면 메인으로 튕겨내거나 처리
			if (!"ACTIVE".equals(targetUser.getStatus())) {
				return "redirect:/main";
			}
			
			// ⭐ HTML(Javascript)에서 API 호출 시 사용할 targetEmail 전달
			model.addAttribute("targetEmail", targetUser.getEmail());
			model.addAttribute("targetUser", targetUser); // 상단 정보 표시용
		}

		return "timeline"; // timeline.html 렌더링
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