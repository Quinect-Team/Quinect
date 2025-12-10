package com.project.quiz.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
	private final SimpMessagingTemplate messagingTemplate; // ⭐ WebSocket 전송용

	/**
	 * 메시지 전송 POST /api/friend-messages/send ✅ WebSocket + HTTP 하이브리드 방식
	 */
	@PostMapping("/send")
	@Transactional // ⭐ 추가: LAZY 로딩 문제 해결
	public ResponseEntity<?> sendMessage(@RequestParam("friendshipId") Long friendshipId,
			@RequestParam("content") String content, Authentication authentication) {

		try {
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			System.out.println("\n=== 📨 메시지 전송 시작 ===");
			System.out.println("현재 사용자 ID: " + currentUserId);
			System.out.println("FriendshipId: " + friendshipId);
			System.out.println("Content: " + content);

			// ⭐ 1단계: Service로 메시지 저장
			FriendMessageDTO message = friendMessageService.sendMessage(friendshipId, currentUserId, content);

			if (message == null) {
				System.out.println("❌ message가 null입니다!");
				return ResponseEntity.badRequest().body("메시지 저장에 실패했습니다");
			}

			System.out.println("✅ 메시지 저장 완료!");
			System.out.println("   - ID: " + message.getId());
			System.out.println("   - 내용: " + message.getMessageText());
			System.out.println("   - 발신자 ID: " + message.getSenderId());

			// ⭐ 2단계: 발신자 정보 설정 (userProfile에서 닉네임 가져오기)
			User sender = userRepository.findById(currentUserId).orElse(null);
			String senderName = "알 수 없음";

			if (sender != null && sender.getUserProfile() != null) {
				senderName = sender.getUserProfile().getUsername();
			} else if (sender != null) {
				senderName = sender.getEmail();
			}

			message.setSenderName(senderName);
			System.out.println("   - 발신자 이름: " + senderName);

			// ⭐ 3단계: 상대방(수신자) 찾기
			Friendship friendship = friendshipRepository.findById(friendshipId)
					.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

			User receiver;
			if (friendship.getUser().getId().equals(currentUserId)) {
				receiver = friendship.getFriendUser();
			} else {
				receiver = friendship.getUser();
			}

			System.out.println("   - 수신자 ID: " + receiver.getId());
			System.out.println("   - 수신자 이메일: " + receiver.getEmail());

			// ⭐ 4단계: WebSocket으로 상대방에게 실시간 전송!
			System.out.println("\n📢 [WebSocket 메시지 전송]");
			System.out.println("   받는사람 ID: " + receiver.getId());
			System.out.println("   목적지: /user/" + receiver.getId() + "/queue/friend-messages");

			try {
				messagingTemplate.convertAndSendToUser(receiver.getEmail(), // 받는 사람 ID
						"/queue/friend-messages", // 목적지
						message // 메시지 객체
				);
				System.out.println("✅ WebSocket 메시지 전송 완료!");
			} catch (Exception e) {
				System.out.println("⚠️ WebSocket 전송 실패 (HTTP 응답으로 보상): " + e.getMessage());
				// WebSocket 실패 시에도 HTTP 응답으로 메시지 반환 (클라이언트에서 처리 가능)
			}

			System.out.println("=== 메시지 전송 완료 ===\n");

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
	 * 
	 * ✅ friendshipId로 조회하는 버전 (기존: friendUserId로 조회)
	 */
	@GetMapping("/{friendshipId}")
	public ResponseEntity<List<FriendMessageDTO>> getMessages(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {
		try {
			// ⭐ getCurrentUserId() 사용
			Long currentUserId = getCurrentUserId(authentication);
			if (currentUserId == null) {
				return ResponseEntity.badRequest().body(new ArrayList<>());
			}

			// 메시지 조회
			List<FriendMessageDTO> messages = friendMessageService.getMessages(friendshipId, currentUserId);

			// ⭐ [중요] 모든 메시지에 senderName 설정
			for (FriendMessageDTO msg : messages) {
				if (msg.getSenderName() == null || msg.getSenderName().isEmpty()) {
					// senderId로 User 조회해서 이름 설정
					User sender = userRepository.findById(msg.getSenderId()).orElse(null);
					if (sender != null && sender.getUserProfile() != null) {
						msg.setSenderName(sender.getUserProfile().getUsername());
					} else if (sender != null) {
						msg.setSenderName(sender.getEmail());
					} else {
						msg.setSenderName("Unknown");
					}

					System.out.println("📢 메시지 #" + msg.getId() + " - senderName 설정: " + msg.getSenderName());
				}
			}

			System.out.println("✅ 메시지 조회 완료: " + messages.size() + "개");
			for (FriendMessageDTO msg : messages) {
				System.out.println("   - ID: " + msg.getId() + ", Sender: " + msg.getSenderName() + ", Content: "
						+ msg.getMessageText());
			}

			return ResponseEntity.ok(messages);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(new ArrayList<>()); // ← 빈 리스트 반환
		}
	}

	/**
	 * 친구 삭제
	 */
	@DeleteMapping("/friendship/{friendshipId}")
	@Transactional // ⭐ 추가
	public ResponseEntity<?> deleteFriendship(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {

		try {
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

			System.out.println("✅ 친구 삭제 완료: friendshipId=" + friendshipId);

			return ResponseEntity.ok("친구가 삭제되었습니다");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			System.err.println("❌ 친구 삭제 실패: " + e.getMessage());
			return ResponseEntity.status(500).body("친구 삭제에 실패했습니다");
		}
	}

	/**
	 * 친구 복구
	 */
	@PutMapping("/friendship/{friendshipId}/restore")
	@Transactional // ⭐ 추가
	public ResponseEntity<?> restoreFriendship(@PathVariable("friendshipId") Long friendshipId,
			Authentication authentication) {

		try {
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

			System.out.println("✅ 친구 복구 완료: friendshipId=" + friendshipId);

			return ResponseEntity.ok("친구가 복구되었습니다");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			System.err.println("❌ 친구 복구 실패: " + e.getMessage());
			return ResponseEntity.status(500).body("친구 복구에 실패했습니다");
		}
	}

}