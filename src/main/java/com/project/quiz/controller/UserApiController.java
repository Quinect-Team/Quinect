package com.project.quiz.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.repository.UserProfileRepository;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserApiController {

	private final UserRepository userRepository;
	private final UserService userService;
	private final UserProfileRepository userProfileRepository; // ⭐ 추가

	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmail(@RequestParam("email") String email) {
		return ResponseEntity.ok(userRepository.existsByEmail(email));
	}

	// ⭐ 현재 로그인한 사용자 정보 반환
	@GetMapping("/current")
	public ResponseEntity<?> getCurrentUser(Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다");
		}

		try {
			String email = principal.getName();
			System.out.println("현재 사용자 이메일: " + email);

			User user = userService.getUserByEmail(email);

			// ⭐ UserProfile에서 username 가져오기
			UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElse(null);

			String username = (userProfile != null) ? userProfile.getUsername() : user.getEmail();

			// 응답 JSON 생성
			Map<String, Object> response = new HashMap<>();
			response.put("id", user.getId());
			response.put("email", user.getEmail());
			response.put("username", username);

			System.out.println(
					"✅ 현재 사용자 조회 - ID: " + user.getId() + ", Email: " + user.getEmail() + ", Username: " + username);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			System.err.println("❌ 사용자 조회 실패: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body("사용자 정보를 조회할 수 없습니다");
		}
	}
}
