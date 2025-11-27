package com.project.quiz.service;

import com.project.quiz.domain.Friendship;
import com.project.quiz.domain.User;
import com.project.quiz.dto.UserSearchResponse;
import com.project.quiz.repository.FriendshipRepository;
import com.project.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
		List<User> users = userRepository.findByEmailContaining(email);

		List<User> filteredUsers = users.stream().filter(user -> !user.getId().equals(currentUserId))
				.collect(Collectors.toList());

		if (filteredUsers.isEmpty()) {
			return List.of();
		}

		// 내가 보낸 요청 (user_id = 나)
		Map<Long, String> sentRequests = friendshipRepository.findByUserIdAndStatus(currentUserId, "pending").stream()
				.collect(Collectors.toMap(f -> f.getFriendUser().getId(), f -> "PENDING_SENT"));

		// 내가 받은 요청 (friend_user_id = 나)
		Map<Long, String> receivedRequests = friendshipRepository.findByFriendUserIdAndStatus(currentUserId, "pending")
				.stream().collect(Collectors.toMap(f -> f.getUser().getId(), f -> "PENDING_RECEIVED"));

		// 수락된 친구 (user_id = 나인 경우)
		List<Friendship> acceptedFriendshipsAsSender = friendshipRepository.findByUserIdAndStatus(currentUserId,
				"accepted");
		Map<Long, String> acceptedFriendsAsSender = acceptedFriendshipsAsSender.stream()
				.collect(Collectors.toMap(f -> f.getFriendUser().getId(), f -> "ACCEPTED"));

		// 수락된 친구 (friend_user_id = 나인 경우)
		List<Friendship> acceptedFriendshipsAsReceiver = friendshipRepository.findByFriendUserIdAndStatus(currentUserId,
				"accepted");
		Map<Long, String> acceptedFriendsAsReceiver = acceptedFriendshipsAsReceiver.stream()
				.collect(Collectors.toMap(f -> f.getUser().getId(), f -> "ACCEPTED"));

		return filteredUsers.stream().map(user -> {
			String status = sentRequests.getOrDefault(user.getId(),
					receivedRequests.getOrDefault(user.getId(), acceptedFriendsAsSender.getOrDefault(user.getId(),
							acceptedFriendsAsReceiver.getOrDefault(user.getId(), "NONE"))));
			return UserSearchResponse.from(user, status);
		}).collect(Collectors.toList());
	}

	/**
	 * 내가 받은 친구 요청 (Friendship ID 포함)
	 */
	public List<UserSearchResponse> getReceivedRequests(Long currentUserId) {
		List<Friendship> friendships = friendshipRepository.findByFriendUserIdAndStatus(currentUserId, "pending");

		return friendships.stream()
				.map(f -> UserSearchResponse.fromFriendship(f.getUser(), f.getId(), "PENDING_RECEIVED"))
				.collect(Collectors.toList());
	}

	/**
	 * 내가 보낸 친구 요청 (Friendship ID 포함)
	 */
	public List<UserSearchResponse> getSentRequests(Long currentUserId) {
		List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(currentUserId, "pending");

		return friendships.stream()
				.map(f -> UserSearchResponse.fromFriendship(f.getFriendUser(), f.getId(), "PENDING_SENT"))
				.collect(Collectors.toList());
	}

	/**
	 * ⭐ 수락된 친구 목록 (Friendship ID 포함) user_id = 나인 경우 + friend_user_id = 나인 경우 모두 포함
	 */
	public List<UserSearchResponse> getAcceptedFriendsWithIds(Long currentUserId) {
		// user_id = 나인 경우 (내가 보낸 요청이 수락됨)
		List<Friendship> friendshipsAsSender = friendshipRepository.findByUserIdAndStatus(currentUserId, "accepted");

		// friend_user_id = 나인 경우 (내가 받은 요청이 수락됨)
		List<Friendship> friendshipsAsReceiver = friendshipRepository.findByFriendUserIdAndStatus(currentUserId,
				"accepted");

		List<UserSearchResponse> accepted = new ArrayList<>();

		// 내가 보낸 요청이 수락된 경우
		accepted.addAll(friendshipsAsSender.stream()
				.map(f -> UserSearchResponse.fromFriendship(f.getFriendUser(), f.getId(), "ACCEPTED"))
				.collect(Collectors.toList()));

		// 내가 받은 요청이 수락된 경우
		accepted.addAll(friendshipsAsReceiver.stream()
				.map(f -> UserSearchResponse.fromFriendship(f.getUser(), f.getId(), "ACCEPTED"))
				.collect(Collectors.toList()));

		return accepted;
	}

	/**
	 * 수락된 친구 목록 (기존 메서드 유지)
	 */
	public List<UserSearchResponse> getAcceptedFriends(Long currentUserId) {
		List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(currentUserId, "accepted");

		return friendships.stream().map(f -> UserSearchResponse.from(f.getFriendUser(), "ACCEPTED"))
				.collect(Collectors.toList());
	}

	/**
	 * ⭐ 모든 친구 관계 로드 (받은 요청 + 보낸 요청 + 수락된 친구)
	 */
	public Map<String, Object> getAllFriendships(Long currentUserId) {
		List<UserSearchResponse> received = getReceivedRequests(currentUserId);
		List<UserSearchResponse> sent = getSentRequests(currentUserId);
		List<UserSearchResponse> accepted = getAcceptedFriendsWithIds(currentUserId);

		Map<String, Object> result = new HashMap<>();
		result.put("received", received);
		result.put("sent", sent);
		result.put("accepted", accepted);

		return result;
	}

	/**
	 * 친구 요청 보내기
	 */
	@Transactional
	public void sendFriendRequest(Long senderId, Long receiverId) {
		System.out.println("친구 요청 보내기: senderId=" + senderId + ", receiverId=" + receiverId);

		// ⭐ banned, deleted, rejected가 아닌 기존 관계만 확인
		if (friendshipRepository.existsByUserIdAndFriendUserIdAndStatusNotIn(senderId, receiverId,
				java.util.Arrays.asList("deleted", "rejected", "banned"))) {
			System.out.println("이미 친구 요청을 보냈거나 친구임");
			throw new RuntimeException("이미 친구 요청을 보냈거나 친구입니다.");
		}

		if (friendshipRepository.existsByUserIdAndFriendUserIdAndStatusNotIn(receiverId, senderId,
				java.util.Arrays.asList("deleted", "rejected", "banned"))) {
			System.out.println("상대방이 이미 친구 요청을 보냈음");
			throw new RuntimeException("상대방이 이미 친구 요청을 보냈습니다.");
		}

		// ⭐ 차단되었는지 확인 (receiver가 sender를 차단했는지)
		if (friendshipRepository.existsByUserIdAndFriendUserIdAndStatus(receiverId, senderId, "banned")) {
			System.out.println("상대방에게 차단되어 요청 불가");
			throw new RuntimeException("상대방이 차단한 사용자는 친구 요청을 할 수 없습니다.");
		}

		User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("보내는 사용자를 찾을 수 없습니다."));
		User receiver = userRepository.findById(receiverId)
				.orElseThrow(() -> new RuntimeException("받는 사용자를 찾을 수 없습니다."));

		// deleted 또는 rejected 상태로 있는 관계가 있으면 상태 변경
		Friendship existingFriendship = friendshipRepository.findByUserIdAndFriendUserId(senderId, receiverId)
				.orElse(null);

		if (existingFriendship != null && ("deleted".equals(existingFriendship.getStatus())
				|| "rejected".equals(existingFriendship.getStatus()))) {
			System.out.println("기존 " + existingFriendship.getStatus() + " 상태의 친구 관계를 pending으로 변경");
			existingFriendship.setStatus("pending");
			friendshipRepository.save(existingFriendship);
		} else {
			Friendship friendship = Friendship.builder().user(sender).friendUser(receiver).status("pending").build();
			friendshipRepository.save(friendship);
		}

		System.out.println("친구 요청 보내기 완료");
	}

	/**
	 * ⭐ 친구 요청 수락
	 */
	@Transactional
	public void acceptFriendRequest(Long friendshipId, Long currentUserId) {
		System.out.println("친구 요청 수락 시작: friendshipId=" + friendshipId + ", currentUserId=" + currentUserId);

		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("친구 요청을 찾을 수 없습니다."));

		System.out.println("friendship.getFriendUser().getId()=" + friendship.getFriendUser().getId());

		// 나(currentUserId)가 friendUser(받은 사람)인지 확인
		if (!friendship.getFriendUser().getId().equals(currentUserId)) {
			throw new RuntimeException("수락할 권한이 없습니다.");
		}

		System.out.println("이전 상태: " + friendship.getStatus());
		friendship.setStatus("accepted");
		System.out.println("변경된 상태: " + friendship.getStatus());

		// ⭐ 변경사항 저장
		friendshipRepository.save(friendship);
		System.out.println("친구 요청 수락 완료");
	}

	/**
	 * 친구 요청 거절 (상대방이 다시 요청 가능)
	 */
	@Transactional
	public void rejectFriendRequest(Long friendshipId, Long currentUserId) {
		System.out.println("친구 요청 거절 시작: friendshipId=" + friendshipId + ", currentUserId=" + currentUserId);

		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("친구 요청을 찾을 수 없습니다."));

		if (!friendship.getFriendUser().getId().equals(currentUserId)) {
			throw new RuntimeException("거절할 권한이 없습니다.");
		}

		System.out.println("이전 상태: " + friendship.getStatus());
		friendship.setStatus("rejected");
		System.out.println("변경된 상태: " + friendship.getStatus());

		friendshipRepository.save(friendship);
		System.out.println("✅ 친구 요청 거절 완료");
	}

	/**
	 * 친구 요청 차단 (상대방이 다시 요청 불가)
	 */
	@Transactional
	public void banFriendRequest(Long friendshipId, Long currentUserId) {
		System.out.println("친구 요청 차단 시작: friendshipId=" + friendshipId + ", currentUserId=" + currentUserId);

		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("친구 요청을 찾을 수 없습니다."));

		if (!friendship.getFriendUser().getId().equals(currentUserId)) {
			throw new RuntimeException("차단할 권한이 없습니다.");
		}

		System.out.println("이전 상태: " + friendship.getStatus());
		friendship.setStatus("banned");
		System.out.println("변경된 상태: " + friendship.getStatus());

		friendshipRepository.save(friendship);
		System.out.println("✅ 친구 요청 차단 완료");
	}

	/**
	 * 친구 삭제 (DB 삭제 아니라 상태 변경: deleted)
	 */
	@Transactional
	public void removeFriend(Long friendshipId, Long currentUserId) {
		System.out.println("친구 삭제 시작: friendshipId=" + friendshipId + ", currentUserId=" + currentUserId);

		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("친구 관계를 찾을 수 없습니다."));

		if (!friendship.getUser().getId().equals(currentUserId)
				&& !friendship.getFriendUser().getId().equals(currentUserId)) {
			throw new RuntimeException("삭제할 권한이 없습니다.");
		}

		System.out.println("이전 상태: " + friendship.getStatus());
		// ⭐ DB 삭제 대신 상태를 "deleted"로 변경
		friendship.setStatus("deleted");
		System.out.println("변경된 상태: " + friendship.getStatus());

		friendshipRepository.save(friendship);
		System.out.println("✅ 친구 삭제(상태 변경) 완료");
	}

}
