package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.FriendMessage;
import com.project.quiz.domain.Friendship;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.FriendMessageDTO;
import com.project.quiz.repository.FriendMessageRepository;
import com.project.quiz.repository.FriendshipRepository;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendMessageService {

	private final FriendMessageRepository friendMessageRepository;
	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;

	/**
	 * 메시지 전송
	 */
	public FriendMessageDTO sendMessage(Long friendshipId, Long userId, String content) {
		// ✅ 친구 관계 존재 여부만 확인
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

		// ✅ 메시지만 저장 (friendship 업데이트 X)
		FriendMessage message = new FriendMessage();
		message.setFriendship(friendship);
		message.setSenderId(userId);
		message.setMessageText(content);
		message.setIsRead(false);
		message.setSentAt(LocalDateTime.now());

		FriendMessage saved = friendMessageRepository.save(message);

		return new FriendMessageDTO(saved);
	}

	/**
	 * 메시지 조회 (닉네임 처리 포함)
	 */
	public List<FriendMessageDTO> getMessages(Long friendshipId, Long currentUserId) {
		// 1. friendship 조회
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

		// 2. 상대방 User 객체 구하기
		User otherUser;
		if (friendship.getUser().getId().equals(currentUserId)) {
			otherUser = friendship.getFriendUser();
		} else {
			otherUser = friendship.getUser();
		}

		// 3. 친구 삭제 여부 확인 (status가 "accepted"가 아님)
		boolean isFriendDeleted = !"accepted".equals(friendship.getStatus());

		// 4. 메시지 조회
		List<FriendMessage> messages = friendMessageRepository.findByFriendshipIdOrderBySentAtAsc(friendshipId);

		// 5. 메시지를 DTO로 변환하며 닉네임 처리
		return messages.stream().map(msg -> {
			FriendMessageDTO dto = convertToDTO(msg);

			// 상대방이 보낸 메시지이고 친구가 삭제된 경우
			if (msg.getSenderId().equals(otherUser.getId()) && isFriendDeleted) {
				dto.setSenderName("알 수 없음");
			} else {
				// 보낸 사람의 닉네임 조회 (UserProfile에서)
				String senderName = getUserName(msg.getSenderId());
				dto.setSenderName(senderName);
			}

			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 * ⭐ 현재 사용자의 안 읽은 친구 메시지 5개 조회 현재 사용자가 포함된 friendship의 메시지만 조회
	 */
	public List<Map<String, Object>> getUnreadMessages(Long currentUserId) {
		// 1단계: 현재 사용자가 포함된 모든 friendship ID 조회
		// (user_id가 currentUserId OR friend_user_id가 currentUserId인 것들)
		List<Friendship> myFriendships = friendshipRepository.findByUserIdOrFriendUserId(currentUserId, currentUserId);

		List<Long> friendshipIds = myFriendships.stream().map(Friendship::getId).collect(Collectors.toList());

		if (friendshipIds.isEmpty()) {
			return new ArrayList<>();
		}

		// 2단계: 그 friendshipId들에 속한 unread 메시지만 조회
		List<FriendMessage> unreadMessages = friendMessageRepository
				.findByFriendshipIdInAndSenderIdNotAndIsReadFalseOrderBySentAtDesc(friendshipIds, currentUserId);

		Map<Long, FriendMessage> latestByUser = new LinkedHashMap<>();
		for (FriendMessage msg : unreadMessages) {
			latestByUser.putIfAbsent(msg.getSenderId(), msg); // 첫 번째만 추가 (이미 최신순)
		}

		// 3단계: DTO로 변환
		return unreadMessages.stream().limit(5).map(msg -> {
			String senderName = getUserName(msg.getSenderId());
			String profileImage = getUserProfileImage(msg.getSenderId()); // ⭐ 추가

			Map<String, Object> resultMap = new java.util.HashMap<>();
			resultMap.put("messageId", msg.getId());
			resultMap.put("senderId", msg.getSenderId());
			resultMap.put("senderName", senderName);
			resultMap.put("profileImage", profileImage);
			resultMap.put("content", msg.getMessageText());
			resultMap.put("sentAt", msg.getSentAt().toString());

			return resultMap;
		}).collect(Collectors.toList());
	}

	// ⭐ 프로필 이미지 조회 메서드
	private String getUserProfileImage(Long userId) {
		UserProfile userProfile = userProfileRepository.findById(userId).orElse(null);
		if (userProfile != null && userProfile.getProfileImage() != null) {
			return userProfile.getProfileImage();
		}
		return null; // 프로필 이미지가 없으면 null 반환 (프론트에서 기본 이미지 사용)
	}

	/**
	 * 친구 삭제 (status 변경)
	 */
	@Transactional
	public void deleteFriendship(Long friendshipId) {
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

		friendship.setStatus("rejected"); // 또는 "blocked" 등으로 변경
		friendship.setUpdatedAt(LocalDateTime.now());
		friendshipRepository.save(friendship);
	}

	/**
	 * 친구 복구
	 */
	@Transactional
	public void restoreFriendship(Long friendshipId) {
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

		friendship.setStatus("accepted");
		friendship.setUpdatedAt(LocalDateTime.now());
		friendshipRepository.save(friendship);
	}

	/**
	 * ⭐ 특정 friendship의 사용자가 받은 모든 메시지를 읽음 처리
	 */
	@Transactional
	public void markChatRoomAsRead(Long friendshipId, Long currentUserId) {
		// 1단계: 현재 사용자가 받은 안 읽은 메시지들 조회
		List<FriendMessage> unreadMessages = friendMessageRepository
				.findByFriendshipIdAndSenderIdNotAndIsReadFalse(friendshipId, currentUserId);

		// 2단계: 각 메시지를 읽음 처리
		unreadMessages.forEach(message -> {
			message.markAsRead();
		});

		// 3단계: DB에 저장 (변경감지로 자동 update)
		friendMessageRepository.saveAll(unreadMessages);

		System.out.println("✅ 채팅방 #" + friendshipId + "의 " + unreadMessages.size() + "개 메시지 읽음 처리 완료 (사용자: "
				+ currentUserId + ")");
	}

	/**
	 * UserProfile에서 닉네임 조회 (Helper)
	 */
	private String getUserName(Long userId) {
		try {
			User user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				return "알 수 없음";
			}

			UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
			if (profile != null && profile.getUsername() != null) {
				return profile.getUsername();
			}

			return "알 수 없음";
		} catch (Exception e) {
			return "알 수 없음";
		}
	}

	/**
	 * Entity → DTO 변환
	 */
	private FriendMessageDTO convertToDTO(FriendMessage message) {
		String senderName = getUserName(message.getSenderId());

		return new FriendMessageDTO(message.getId(), message.getMessageText(), message.getIsRead(), message.getSentAt(),
				message.getSenderId(), senderName);
	}
}
