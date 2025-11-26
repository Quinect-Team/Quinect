package com.project.quiz.service;

import com.project.quiz.domain.Friendship;
import com.project.quiz.domain.User;
import com.project.quiz.dto.UserSearchResponse;
import com.project.quiz.repository.FriendshipRepository;
import com.project.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {

	private final UserRepository userRepository;
	private final FriendshipRepository friendshipRepository;

	/**
	 * 이메일로 사용자 검색하고 친구 상태 포함해서 반환
	 */
	public List<UserSearchResponse> searchUsersByEmail(String email, Long currentUserId) {
		// 1. 이메일로 사용자 검색
		List<User> users = userRepository.findByEmailContaining(email);

		// 2. 자기 자신 제외
		List<User> filteredUsers = users.stream().filter(user -> !user.getId().equals(currentUserId))
				.collect(Collectors.toList());

		if (filteredUsers.isEmpty()) {
			return List.of();
		}

		// 3. 검색된 사용자들의 ID 목록
		List<Long> userIds = filteredUsers.stream().map(User::getId).collect(Collectors.toList());

		// 4. 현재 사용자와 검색된 사용자들 간의 친구 관계 조회
		List<Friendship> friendships = friendshipRepository.findByUserIdAndFriendUserIdIn(currentUserId, userIds);

		// 5. Map으로 변환 (friendUserId -> status)
		Map<Long, String> friendshipStatusMap = friendships.stream()
				.collect(Collectors.toMap(f -> f.getFriendUser().getId(), Friendship::getStatus));

		// 6. 응답 DTO 생성 (친구 상태 포함)
		return filteredUsers.stream().map(user -> {
			String status = friendshipStatusMap.get(user.getId());
			return UserSearchResponse.from(user, status);
		}).collect(Collectors.toList());
	}

	/**
	 * ⭐⭐⭐ 내가 포함된 모든 친구 관계 조회 내가 보낸 요청도 있고, 받은 요청도 있고, 이미 친구인 것도 있고... 모든 관계를 내 관점에서
	 * 조회
	 * 
	 * @param currentUserId 현재 로그인한 사용자 ID
	 * @return 내 관점의 친구 관계 목록
	 */
	public List<UserSearchResponse> getAllFriendships(Long currentUserId) {
		// 1. 내가 포함된 모든 친구 관계 조회 (보낸 것 + 받은 것)
		List<Friendship> friendships = friendshipRepository.findByUser(currentUserId);

		System.out.println("총 친구 관계 수: " + friendships.size());

		// 2. Friendship → UserSearchResponse 변환
		return friendships.stream().map(friendship -> {
			// ⭐ 상대방 사용자 결정
			User otherUser;
			boolean iSentRequest; // 내가 요청을 보냈는가?

			if (friendship.getUser().getId().equals(currentUserId)) {
				// 내가 보낸 요청 (나 = user, 상대 = friendUser)
				otherUser = friendship.getFriendUser();
				iSentRequest = true;
			} else {
				// 내가 받은 요청 (상대 = user, 나 = friendUser)
				otherUser = friendship.getUser();
				iSentRequest = false;
			}

			// ⭐ 내 관점의 상태 결정
			String statusLabel = getStatusLabelFromMyPerspective(friendship.getStatus(), iSentRequest);

			System.out.println("상대방: " + otherUser.getEmail() + ", 보낸 요청: " + iSentRequest + ", 상태: " + statusLabel);

			return UserSearchResponse.from(otherUser, statusLabel);
		}).collect(Collectors.toList());
	}

	/**
	 * ⭐ 특정 상태의 친구 관계만 조회 예: PENDING만 조회, ACCEPTED만 조회 등
	 */
	public List<UserSearchResponse> getFriendshipsByStatus(Long currentUserId, String status) {
		List<Friendship> friendships = friendshipRepository.findByUserAndStatus(currentUserId, status);

		System.out.println(status + " 상태의 친구 관계 수: " + friendships.size());

		return friendships.stream().map(friendship -> {
			User otherUser;
			boolean iSentRequest;

			if (friendship.getUser().getId().equals(currentUserId)) {
				otherUser = friendship.getFriendUser();
				iSentRequest = true;
			} else {
				otherUser = friendship.getUser();
				iSentRequest = false;
			}

			String statusLabel = getStatusLabelFromMyPerspective(friendship.getStatus(), iSentRequest);

			return UserSearchResponse.from(otherUser, statusLabel);
		}).collect(Collectors.toList());
	}

	/**
	 * ⭐⭐⭐ 내 관점에서의 상태 라벨 결정
	 * 
	 * DB에는 status가 단순히 "PENDING", "ACCEPTED" 등으로 저장되지만, 클라이언트에게는 내 역할에 따라 다르게 보여줘야
	 * 함
	 * 
	 * PENDING 상태: - iSentRequest = true (내가 보낸) → "PENDING_SENT" (대기중) -
	 * iSentRequest = false (내가 받은) → "PENDING_RECEIVED" (수신)
	 * 
	 * ACCEPTED, REJECTED, BLOCKED 등은 양쪽 동일하게 반환
	 * 
	 * @param status       DB에 저장된 상태 (PENDING, ACCEPTED, REJECTED, BLOCKED)
	 * @param iSentRequest 내가 요청을 보냈는지 여부
	 * @return 내 관점의 상태 라벨
	 */
	private String getStatusLabelFromMyPerspective(String status, boolean iSentRequest) {
		if ("PENDING".equals(status)) {
			// PENDING이면 내가 보낸 건지, 받은 건지에 따라 구분
			return iSentRequest ? "PENDING_SENT" : "PENDING_RECEIVED";
		}

		// ACCEPTED, REJECTED, BLOCKED 등은 그대로 반환
		return status;
	}

	/**
	 * 친구 요청 보내기
	 * 
	 * @param senderId   요청 보내는 사람 ID
	 * @param receiverId 요청 받는 사람 ID
	 * @throws RuntimeException 이미 요청이 있으면 예외
	 */
	@Transactional
	public void sendFriendRequest(Long senderId, Long receiverId) {
		// 1. 이미 같은 방향으로 친구 요청이 있는지 확인
		if (friendshipRepository.existsByUserIdAndFriendUserId(senderId, receiverId)) {
			throw new RuntimeException("이미 친구 요청을 보냈습니다.");
		}

		// 2. 반대 방향 요청도 확인 (상대방이 먼저 보냈는지)
		if (friendshipRepository.existsByUserIdAndFriendUserId(receiverId, senderId)) {
			throw new RuntimeException("상대방이 이미 친구 요청을 보냈습니다.");
		}

		// 3. 사용자 확인
		User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("보내는 사용자를 찾을 수 없습니다."));
		User receiver = userRepository.findById(receiverId)
				.orElseThrow(() -> new RuntimeException("받는 사용자를 찾을 수 없습니다."));

		// 4. 친구 요청 생성 및 저장
		Friendship friendship = Friendship.builder().user(sender).friendUser(receiver).status("PENDING").build();

		friendshipRepository.save(friendship);

		System.out.println("친구 요청 저장됨: " + senderId + " → " + receiverId);
	}
}
