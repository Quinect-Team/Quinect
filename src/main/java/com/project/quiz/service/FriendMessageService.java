package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.*;
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
	private final FriendshipService friendshipService; // ⭐ 추가
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
	/**
	 * ⭐ 안 읽은 메시지를 발신자별로 그룹화하여 조회
	 */
	/**
	 * ⭐ 안 읽은 메시지를 발신자별로 그룹화하여 조회
	 */
	public List<Map<String, Object>> getUnreadMessages(Long currentUserId) {
		// ⭐ Step 1: 현재 사용자의 모든 accepted friendship 조회
		List<Friendship> friendships = friendshipService.findAllFriendshipsForUser(currentUserId);

		List<Long> friendshipIds = friendships.stream().map(Friendship::getId).collect(Collectors.toList());

		if (friendshipIds.isEmpty()) {
			return new ArrayList<>();
		}

		// ⭐ Step 2: 현재 사용자가 받은 미읽은 메시지 조회
		List<FriendMessage> unreadMessages = friendMessageRepository
				.findByFriendshipIdInAndSenderIdNotAndIsReadFalseOrderBySentAtDesc(friendshipIds, currentUserId);

		if (unreadMessages.isEmpty()) {
			return new ArrayList<>();
		}

		// ⭐ Step 3: 발신자별로 그룹화
		Map<Long, List<FriendMessage>> groupedBySender = unreadMessages.stream()
				.collect(Collectors.groupingBy(msg -> msg.getSenderId()));

		// ⭐ Step 4: 결과 맵 생성
		List<Map<String, Object>> result = new ArrayList<>();

		for (Map.Entry<Long, List<FriendMessage>> entry : groupedBySender.entrySet()) {
			Long senderId = entry.getKey();
			List<FriendMessage> messages = entry.getValue();

			FriendMessage lastMessage = messages.get(messages.size() - 1);

			User sender = userRepository.findById(senderId).orElse(null);

			if (sender == null) {
				continue;
			}

			String senderName = "알 수 없음";
			if (sender.getUserProfile() != null) {
				senderName = sender.getUserProfile().getUsername();
			} else {
				senderName = sender.getEmail();
			}

			String profileImage = null;
			if (sender.getUserProfile() != null) {
				profileImage = sender.getUserProfile().getProfileImage();
			}

			Map<String, Object> messageGroup = new HashMap<>();
			messageGroup.put("senderId", senderId);
			messageGroup.put("senderName", senderName);
			messageGroup.put("messageCount", messages.size()); // ⭐ 중요!
			messageGroup.put("lastMessage", lastMessage.getMessageText());
			messageGroup.put("profileImage", profileImage);
			messageGroup.put("sentAt", lastMessage.getSentAt());

			result.add(messageGroup);

		}

		// ⭐ Step 5: 시간 역순 정렬
		result.sort((a, b) -> {
			LocalDateTime timeA = (LocalDateTime) a.get("sentAt");
			LocalDateTime timeB = (LocalDateTime) b.get("sentAt");
			return timeB.compareTo(timeA);
		});

		return result;
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

	private FriendMessageDTO convertToDTO(FriendMessage message) {
		FriendMessageDTO dto = new FriendMessageDTO(message);
		dto.setSenderName(getUserName(message.getSenderId()));
		return dto;
	}

}
