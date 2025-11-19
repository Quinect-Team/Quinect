package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.QuizRoom;

import java.util.Optional;

public interface QuizRoomRepository extends JpaRepository<QuizRoom, Long> {
	Optional<QuizRoom> findByRoomCode(String roomCode);

	boolean existsByRoomCode(String roomCode);
}
