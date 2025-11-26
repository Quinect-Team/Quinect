package com.project.quiz.repository;

import com.project.quiz.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	// 특정 사용자 간의 친구 관계 조회
	Optional<Friendship> findByUserIdAndFriendUserId(Long userId, Long friendUserId);

	// 이미 친구 요청이 있는지 확인 (양방향)
	boolean existsByUserIdAndFriendUserId(Long userId, Long friendUserId);

	@Query("SELECT f FROM Friendship f WHERE f.user.id = :userId AND f.friendUser.id IN :friendUserIds")
	List<Friendship> findByUserIdAndFriendUserIdIn(@Param("userId") Long userId,
			@Param("friendUserIds") List<Long> friendUserIds);

	@Query("SELECT f FROM Friendship f " + "WHERE (f.user.id = :userId OR f.friendUser.id = :userId) "
			+ "AND f.status = :status " + "ORDER BY f.createdAt DESC")
	List<Friendship> findByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

	@Query("SELECT f FROM Friendship f " + "WHERE (f.user.id = :userId OR f.friendUser.id = :userId) "
			+ "ORDER BY f.createdAt DESC")
	List<Friendship> findByUser(@Param("userId") Long userId);
}
