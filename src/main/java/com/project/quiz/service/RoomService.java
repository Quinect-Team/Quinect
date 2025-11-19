package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.Room;
import com.project.quiz.repository.RoomRepository;

@Service
@Transactional
public class RoomService {

	private final RoomRepository RoomRepository;
	private final Random random = new Random();

	public RoomService(RoomRepository RoomRepository) {
		this.RoomRepository = RoomRepository;
	}

	/**
	 * 중복되지 않는 4자리 숫자 문자열 방 코드를 생성합니다.
	 */
	public String generateUniqueRoomCode() {
		String roomCode;
		do {
			roomCode = String.format("%04d", random.nextInt(10000));
		} while (RoomRepository.existsByRoomCode(roomCode));
		return roomCode;
	}

	/**
	 * 새로운 방을 생성합니다.
	 */
	public Room createRoom(String hostUserId, String roomType) {
		String roomCode = generateUniqueRoomCode();
		Room room = Room.builder().roomCode(roomCode).hostUserId(hostUserId).status("OPEN").roomType(roomType)
				.createdAt(LocalDateTime.now()).build();

		return RoomRepository.save(room);
	}

	public boolean existsRoomCode(String roomCode) {
		return RoomRepository.existsByRoomCode(roomCode);
	}

	public Room findByRoomCode(String roomCode) {
		return RoomRepository.findByRoomCode(roomCode).orElse(null);
	}
}
