package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.quiz.domain.FriendMessage;

@Repository
public interface FriendMessageRepository extends JpaRepository<FriendMessage, Long> {
	List<FriendMessage> findByFriendshipIdOrderBySentAtAsc(Long friendshipId);
}