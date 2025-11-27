package com.project.quiz.repository;

import com.project.quiz.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	// 내가 보낸 요청 (user_id = 나)
	List<Friendship> findByUserIdAndStatus(Long userId, String status);

	// 내가 받은 요청 (friend_user_id = 나)
	List<Friendship> findByFriendUserIdAndStatus(Long friendUserId, String status);

	// 중복 체크
	boolean existsByUserIdAndFriendUserIdAndStatus(Long receiverId, Long senderId, String string);

	// 특정 두 사용자 간의 관계 조회
	Optional<Friendship> findByUserIdAndFriendUserId(Long userId, Long friendUserId);

	boolean existsByUserIdAndFriendUserIdAndStatusNotIn(Long senderId, Long receiverId, List<String> asList);

}
