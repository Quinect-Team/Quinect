package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
	public FriendMessageDTO sendMessage(Long friendshipId, Long senderId, String content) {
		// 1. friendship 조회
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("친구 관계가 없습니다"));

		// 2. status 확인 (accepted만 메시지 가능)
		if (!"accepted".equals(friendship.getStatus())) {
			throw new IllegalStateException("친구 관계가 아닙니다");
		}

		// 3. 메시지 생성
		FriendMessage message = new FriendMessage();
		message.setFriendship(friendship);
		message.setSenderId(senderId);
		message.setMessageText(content);
		message.setIsRead(false);

		// 4. 메시지 저장
		FriendMessage saved = friendMessageRepository.save(message);

		// 5. friendship 업데이트 시간
		friendship.setUpdatedAt(LocalDateTime.now());
		friendshipRepository.save(friendship);

		return convertToDTO(saved);
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
