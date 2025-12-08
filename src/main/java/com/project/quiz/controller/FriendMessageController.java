package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.domain.*;
import com.project.quiz.dto.*;
import com.project.quiz.repository.*;
import com.project.quiz.service.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friend-messages")
@RequiredArgsConstructor
public class FriendMessageController {

	private final FriendMessageService friendMessageService;
	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;

	/**
	 * 메시지 전송 POST /api/friend-messages/send
	 */
	@PostMapping("/send")
	public ResponseEntity<?> sendMessage(@RequestParam("friendshipId") Long friendshipId,
			@RequestParam("content") String content, Authentication authentication) {

		try {
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			System.out.println("=== 메시지 전송 시작 ===");
			System.out.println("현재 사용자 ID: " + currentUserId);
			System.out.println("FriendshipId: " + friendshipId);
			System.out.println("Content: " + content);

			// ⭐ Service 호출
			FriendMessageDTO message = friendMessageService.sendMessage(friendshipId, currentUserId, content);

			System.out.println("Service 반환 값 (message): " + message);

			if (message == null) {
				System.out.println("❌ message가 null입니다!");
				return ResponseEntity.badRequest().body("메시지 저장에 실패했습니다");
			}

			System.out.println("message.getId(): " + message.getId());
			System.out.println("message.getMessageText(): " + message.getMessageText());
			System.out.println("message.getSentAt(): " + message.getSentAt());
			System.out.println("message.getSenderId(): " + message.getSenderId());
			System.out.println("message.getSenderName(): " + message.getSenderName());
			System.out.println("message.getIsRead(): " + message.getIsRead());

			System.out.println("=== 메시지 전송 완료 ===");

			return ResponseEntity.ok(message);
		} catch (Exception e) {
			System.err.println("❌ 예외 발생: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body("메시지 전송 실패: " + e.getMessage());
		}
	}

	/**
	 * 현재 사용자 ID 가져오기
	 */
	private Long getCurrentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();

		// Spring Security의 User 객체인 경우
		if (principal instanceof org.springframework.security.core.userdetails.User) {
			String email = ((org.springframework.security.core.userdetails.User) principal).getUsername();
			System.out.println("로그인한 사용자 이메일: " + email);

			// ⭐ 이메일로 User 조회
			User user = userRepository.findByEmail(email).orElse(null);

			if (user != null) {
				return user.getId();
			}
		}

		return null;
	}

	/**
	 * 메시지 조회 GET /api/friend-messages/{friendshipId}
	 */
	@GetMapping("/{friendshipId}")
	public ResponseEntity<?> getMessages(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {

		try {
			// ⭐ getCurrentUserId() 사용
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			// 메시지 조회
			List<FriendMessageDTO> messages = friendMessageService.getMessages(friendshipId, currentUserId);

			return ResponseEntity.ok(messages);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body("메시지 조회 실패: " + e.getMessage());
		}
	}

	@DeleteMapping("/friendship/{friendshipId}")
	public ResponseEntity<?> deleteFriendship(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {

		try {
			// ⭐ getCurrentUserId() 사용
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			Friendship friendship = friendshipRepository.findById(friendshipId)
					.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

			// 본인이 친구 목록에 있는지 확인
			if (!friendship.getUser().getId().equals(currentUserId)
					&& !friendship.getFriendUser().getId().equals(currentUserId)) {
				return ResponseEntity.status(403).body("권한이 없습니다");
			}

			// 친구 삭제
			friendMessageService.deleteFriendship(friendshipId);

			return ResponseEntity.ok("친구가 삭제되었습니다");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("친구 삭제에 실패했습니다");
		}
	}

	@PutMapping("/friendship/{friendshipId}/restore")
	public ResponseEntity<?> restoreFriendship(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {

		try {
			// ⭐ getCurrentUserId() 사용
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			Friendship friendship = friendshipRepository.findById(friendshipId)
					.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

			if (!friendship.getUser().getId().equals(currentUserId)
					&& !friendship.getFriendUser().getId().equals(currentUserId)) {
				return ResponseEntity.status(403).body("권한이 없습니다");
			}

			// 친구 복구
			friendMessageService.restoreFriendship(friendshipId);

			return ResponseEntity.ok("친구가 복구되었습니다");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("친구 복구에 실패했습니다");
		}
	}

}
