package com.project.quiz.controller;

import java.util.*;
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
	private final FriendMessageRepository friendMessageRepository;
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

			// ⭐ 1단계: Service로 메시지 저장
			FriendMessageDTO dto = friendMessageService.sendMessage(friendshipId, currentUserId, content);

			// ⭐ 2단계: 발신자 정보 설정 (userProfile에서 닉네임 가져오기)
			User sender = userRepository.findById(currentUserId).orElse(null);
			if (sender != null && sender.getUserProfile() != null) {
				dto.setSenderName(sender.getUserProfile().getUsername());
			} else if (sender != null) {
				dto.setSenderName(sender.getEmail());
			} else {
				dto.setSenderName("알 수 없음");
			}

			// ⭐ 3단계: 상대방(수신자) 찾기
			Friendship friendship = friendshipRepository.findById(friendshipId)
					.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

			User receiver = friendship.getUser().getId().equals(currentUserId) ? friendship.getFriendUser()
					: friendship.getUser();

			try {
				messagingTemplate.convertAndSendToUser(receiver.getEmail(), // Principal name
						"/queue/friend-messages", dto // ⭐ DTO ONLY
				);
			} catch (Exception e) {
				System.out.println("⚠️ WebSocket 전송 실패: " + e.getMessage());
			}

			return ResponseEntity.ok(dto);

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

		// 1. Principal 타입 확인 없이 바로 이름을 가져옵니다. (가장 안전한 방법)
		// 일반 로그인: 이메일/ID, OAuth2: 이메일/Sub ID 등이 들어옴
		String emailOrUsername = authentication.getName();

		// 2. DB에서 조회
		User user = userRepository.findByEmail(emailOrUsername).orElse(null);

		// 3. 만약 이메일로 못 찾았다면, 혹시 닉네임일 수도 있으니 추가 로직 (필요 시)
		// (지금 구조상 이메일이 ID라면 위 코드로 충분합니다)

		if (user != null) {
			return user.getId();
		}

		return null;
	}

	/**
	 * ⭐ 현재 사용자의 안 읽은 친구 메시지 5개 조회 (드롭다운용) GET /api/friend-messages/unread/list
	 */
	@GetMapping("/unread/list")
	public ResponseEntity<List<Map<String, Object>>> getUnreadMessages(Authentication authentication) {
		try {
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body(new ArrayList<>());
			}

			// ⭐ 발신자별 그룹화된 메시지 조회
			List<Map<String, Object>> messages = friendMessageService.getUnreadMessages(currentUserId);

			return ResponseEntity.ok(messages);

		} catch (Exception e) {
			System.err.println("❌ 안 읽은 메시지 조회 실패: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body(new ArrayList<>());
		}
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
				}
			}

			return ResponseEntity.ok(messages);

		} catch (

		Exception e) {
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

			return ResponseEntity.ok("친구가 복구되었습니다");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			System.err.println("❌ 친구 복구 실패: " + e.getMessage());
			return ResponseEntity.status(500).body("친구 복구에 실패했습니다");
		}
	}

	/**
	 * ⭐ 상대방 사용자 ID로 friendshipId 조회 GET
	 */
	@GetMapping("/friendships/find/{userId}")
	public ResponseEntity<?> findFriendshipIdByUserId(@PathVariable("userId") Long userId,
			Authentication authentication) {

		try {
			Long currentUserId = getCurrentUserId(authentication);

			if (currentUserId == null) {
				return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다");
			}

			// 현재 사용자와 상대방 사이의 친구 관계 조회
			Friendship friendship = friendshipRepository.findByUserIdAndFriendUserId(currentUserId, userId).orElseGet(
					() -> friendshipRepository.findByUserIdAndFriendUserId(userId, currentUserId).orElse(null));

			if (friendship == null) {
				return ResponseEntity.status(404).body(Map.of("error", "친구 관계를 찾을 수 없습니다", "userId", userId));
			}

			return ResponseEntity.ok(Map.of("id", friendship.getId(), "friendshipId", friendship.getId(), "status",
					friendship.getStatus()));

		} catch (Exception e) {
			System.err.println("❌ friendshipId 조회 실패: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "friendshipId 조회에 실패했습니다"));
		}
	}

	/**
	 * 채팅방 열 때 - 해당 대화의 모든 메시지를 읽음 처리 PUT
	 * /api/friend-messages/friendship/{friendshipId}/mark-as-read
	 */
	@PutMapping("/friendship/{friendshipId}/mark-as-read")
	@Transactional
	public ResponseEntity<?> markFriendshipAsRead(@PathVariable("friendshipId") Long friendshipId,
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

			// ⭐ 읽음 처리 전에 메시지 개수 조회
			List<FriendMessage> unreadMessages = friendMessageRepository
					.findByFriendshipIdAndSenderIdNotAndIsReadFalse(friendshipId, currentUserId);

			int messageCount = unreadMessages.size();

			// 읽음 처리
			friendMessageService.markChatRoomAsRead(friendshipId, currentUserId);

			// ⭐ messageCount와 함께 반환
			return ResponseEntity.ok(Map.of("message", "채팅 메시지가 읽음 처리되었습니다", "messageCount", messageCount // ⭐ 추가
			));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			System.err.println("❌ 읽음 처리 실패: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body("읽음 처리에 실패했습니다");
		}
	}

}