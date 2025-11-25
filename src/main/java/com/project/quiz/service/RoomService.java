package com.project.quiz.service;

import com.project.quiz.domain.Room;
import com.project.quiz.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
	@Autowired
	private RoomRepository roomRepository;

	@Transactional
	public Room createRoom(Long hostUserId, String roomTypeCode, String statusCode) {
		// 고유 room_code 중복 체크
		String roomCode;
		do {
			roomCode = Room.generateRoomCode();
		} while (roomRepository.existsByRoomCode(roomCode));

		Room room = Room.builder().createdAt(java.time.LocalDateTime.now()).hostUserId(hostUserId)
				.roomTypeCode(roomTypeCode).statusCode(statusCode).roomCode(roomCode).build();

		return roomRepository.save(room);
	}

	public Room getRoomByCode(String code) {
		return roomRepository.findByRoomCode(code).orElse(null);
	}
}
