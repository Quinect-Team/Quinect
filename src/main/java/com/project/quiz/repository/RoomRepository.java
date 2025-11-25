package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.Room;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
	Optional<Room> findByRoomCode(String roomCode);
	
	boolean existsByRoomCode(String roomCode);
}
