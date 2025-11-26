package com.project.quiz.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.domain.User;
import com.project.quiz.dto.UserSearchResponse;
import com.project.quiz.service.FriendshipService;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController // ⭐ @Controller → @RestController로 변경
@RequestMapping("/api/friends") // ⭐ 경로 추가
@RequiredArgsConstructor
public class FriendsController {

	private final FriendshipService friendshipService;
	private final UserService userService;

	@GetMapping("/search")
	public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam("email") String email,
			@AuthenticationPrincipal UserDetails userDetails) {

		System.out.println("검색 요청 받음: " + email);

		// ⭐ 실제 로그인한 사용자 ID 가져오기
		User currentUser = userService.findByEmail(userDetails.getUsername());

		if (currentUser == null) {
			System.out.println("로그인 사용자를 찾을 수 없습니다.");
			return ResponseEntity.badRequest().build();
		}

		Long currentUserId = currentUser.getId();
		System.out.println("현재 로그인 사용자 ID: " + currentUserId);

		List<UserSearchResponse> results = friendshipService.searchUsersByEmail(email, currentUserId);
		System.out.println("검색 결과: " + results.size() + "명");

		return ResponseEntity.ok(results);
	}

	@GetMapping("/all")
	public ResponseEntity<List<UserSearchResponse>> getAllFriendships(
			@AuthenticationPrincipal UserDetails userDetails) {

		try {
			Long currentUserId;

			if (userDetails == null) {
				System.out.println("⚠️ 로그인 안 됨 - 임시 ID 사용");
				currentUserId = 1L;
			} else {
				User currentUser = userService.findByEmail(userDetails.getUsername());

				if (currentUser == null) {
					System.out.println("⚠️ DB에 사용자 없음 - 임시 ID 사용");
					currentUserId = 1L;
				} else {
					currentUserId = currentUser.getId();
				}
			}

			System.out.println("모든 친구 관계 조회: 사용자 ID " + currentUserId);

			List<UserSearchResponse> friendships = friendshipService.getAllFriendships(currentUserId);

			System.out.println("조회 결과: " + friendships.size() + "명");

			return ResponseEntity.ok(friendships);

		} catch (Exception e) {
			System.err.println("친구 관계 조회 실패: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.ok(List.of());
		}
	}

	/**
	 * ⭐ 특정 상태의 친구 관계만 조회 GET /api/friends/status?status=PENDING
	 */
	@GetMapping("/status")
	public ResponseEntity<List<UserSearchResponse>> getFriendshipsByStatus(
			@RequestParam(value = "status", defaultValue = "PENDING") String status,
			@AuthenticationPrincipal UserDetails userDetails) {

		try {
			Long currentUserId;

			if (userDetails == null) {
				currentUserId = 1L;
			} else {
				User currentUser = userService.findByEmail(userDetails.getUsername());
				currentUserId = currentUser != null ? currentUser.getId() : 1L;
			}

			System.out.println("상태별 친구 관계 조회: " + status);

			List<UserSearchResponse> friendships = friendshipService.getFriendshipsByStatus(currentUserId, status);

			return ResponseEntity.ok(friendships);

		} catch (Exception e) {
			System.err.println("상태별 조회 실패: " + e.getMessage());
			return ResponseEntity.ok(List.of());
		}
	}

	@PostMapping("/request")
	public ResponseEntity<?> sendFriendRequest(@RequestParam("receiverId") Long receiverId,
			@AuthenticationPrincipal UserDetails userDetails) {

		try {
			// 현재 로그인한 사용자
			User currentUser = userService.findByEmail(userDetails.getUsername());

			if (currentUser == null) {
				return ResponseEntity.badRequest().body("로그인이 필요합니다.");
			}

			System.out.println("친구 요청: " + currentUser.getId() + " → " + receiverId);

			// 친구 요청 보내기
			friendshipService.sendFriendRequest(currentUser.getId(), receiverId);

			return ResponseEntity.ok("친구 요청을 보냈습니다.");

		} catch (RuntimeException e) {
			System.err.println("친구 요청 실패: " + e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
