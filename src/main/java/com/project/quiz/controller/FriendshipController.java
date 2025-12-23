package com.project.quiz.controller;

import java.util.*;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.domain.User;
import com.project.quiz.dto.UserSearchResponse;
import com.project.quiz.service.FriendshipService;
import com.project.quiz.service.UserService;

import lombok.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

	private final FriendshipService friendshipService;
	private final UserService userService;

	/**
	 * ⭐ 현재 로그인한 사용자 ID 가져오기
	 * 
	 * @AuthenticationPrincipal 대신 SecurityContextHolder 사용
	 */
	private Long getCurrentUserId() {
		// SecurityContextHolder에서 인증 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		System.out.println("Authentication: " + authentication);
		System.out.println("Authentication null? " + (authentication == null));

		if (authentication == null || !authentication.isAuthenticated()) {
			System.out.println("❌ 인증되지 않음");
			throw new RuntimeException("로그인이 필요합니다.");
		}

		// 인증된 사용자의 이메일(username) 가져오기
		String email = authentication.getName();
		System.out.println("로그인 이메일: " + email);

		// DB에서 사용자 조회
		User currentUser = userService.findByEmail(email);
		if (currentUser == null) {
			System.out.println("❌ DB에서 사용자를 찾을 수 없음: " + email);
			throw new RuntimeException("사용자를 찾을 수 없습니다.");
		}

		System.out.println("✅ 현재 사용자 ID: " + currentUser.getId());
		return currentUser.getId();
	}

	/**
	 * 모든 친구 관계 조회
	 */
	@GetMapping("/all")
	public ResponseEntity<?> getAllFriendships() {

		System.out.println("\n==============================");
		System.out.println("요청 URI: /api/friends/all");
		System.out.println("==============================\n");

		try {
			Long currentUserId = getCurrentUserId();

			// ⭐ 수정: getAllFriendships() 사용
			Map<String, Object> result = friendshipService.getAllFriendships(currentUserId);

			System.out.println("✅ 응답 반환 완료\n");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			System.err.println("❌ 에러: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 사용자 검색 (이메일로)
	 */
	@GetMapping("/search")
	public ResponseEntity<?> searchUsers(@RequestParam("email") String email) {

		try {
			Long currentUserId = getCurrentUserId();
			List<UserSearchResponse> results = friendshipService.searchUsersByEmail(email, currentUserId);
			return ResponseEntity.ok(results);
		} catch (RuntimeException e) {
			return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 내가 받은 친구 요청 목록
	 */
	@GetMapping("/requests/received")
	public ResponseEntity<?> getReceivedRequests() {

		try {
			Long currentUserId = getCurrentUserId();
			List<UserSearchResponse> requests = friendshipService.getReceivedRequests(currentUserId);
			return ResponseEntity.ok(requests);
		} catch (RuntimeException e) {
			return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 내가 보낸 친구 요청 목록
	 */
	@GetMapping("/requests/sent")
	public ResponseEntity<?> getSentRequests() {

		try {
			Long currentUserId = getCurrentUserId();
			List<UserSearchResponse> requests = friendshipService.getSentRequests(currentUserId);
			return ResponseEntity.ok(requests);
		} catch (RuntimeException e) {
			return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 수락된 친구 목록
	 */
	@GetMapping("/accepted")
	public ResponseEntity<?> getAcceptedFriends() {

		try {
			Long currentUserId = getCurrentUserId();
			List<UserSearchResponse> friends = friendshipService.getAcceptedFriends(currentUserId);
			return ResponseEntity.ok(friends);
		} catch (RuntimeException e) {
			return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 친구 요청 보내기
	 */
	@PostMapping("/request")
	public ResponseEntity<?> sendFriendRequest(@RequestParam("receiverId") Long receiverId) {

		try {
			Long currentUserId = getCurrentUserId();
			friendshipService.sendFriendRequest(currentUserId, receiverId);
			return ResponseEntity.ok("친구 요청을 보냈습니다.");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 친구 요청 수락
	 */
	@PostMapping("/{friendshipId}/accept")
	public ResponseEntity<?> acceptFriendRequest(@PathVariable("friendshipId") Long friendshipId) {

		try {
			Long currentUserId = getCurrentUserId();
			friendshipService.acceptFriendRequest(friendshipId, currentUserId);
			return ResponseEntity.ok("친구 요청을 수락했습니다.");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 친구 요청 거절
	 */
	@PostMapping("/{friendshipId}/reject")
	public ResponseEntity<?> rejectFriendRequest(@PathVariable Long friendshipId) {

		try {
			Long currentUserId = getCurrentUserId();
			friendshipService.rejectFriendRequest(friendshipId, currentUserId);
			return ResponseEntity.ok("친구 요청을 거절했습니다.");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * ⭐ 친구 요청 차단
	 */
	@PostMapping("/{friendshipId}/ban")
	public ResponseEntity<?> banFriendRequest(@PathVariable Long friendshipId) {

		try {
			Long currentUserId = getCurrentUserId();
			friendshipService.banFriendRequest(friendshipId, currentUserId);
			return ResponseEntity.ok("사용자를 차단했습니다.");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		}
	}

	/**
	 * 친구 삭제
	 */
	@DeleteMapping("/{friendshipId}")
	public ResponseEntity<?> removeFriend(@PathVariable("friendshipId") Long friendshipId) {

		try {
			Long currentUserId = getCurrentUserId();
			friendshipService.removeFriend(friendshipId, currentUserId);
			return ResponseEntity.ok("친구를 삭제했습니다.");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		}
	}

}

// ⭐ ErrorResponse 클래스
@Data
@NoArgsConstructor
@AllArgsConstructor
class ErrorResponse {
	private String message;
}
