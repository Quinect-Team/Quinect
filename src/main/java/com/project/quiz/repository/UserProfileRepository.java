package com.project.quiz.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.quiz.domain.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

	Optional<UserProfile> findByUserId(Long userId);

	@Query("SELECT up FROM UserProfile up " + "ORDER BY up.pointBalance DESC, up.user.id ASC " + "LIMIT 10")
	List<UserProfile> findTopByPointBalance();

}