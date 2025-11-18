package com.project.quiz.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.QuizRoom;
import com.project.quiz.repository.QuizRoomRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
public class QuizRoomService {

	private final QuizRoomRepository quizRoomRepository;
	private final Random random = new Random();

	public QuizRoomService(QuizRoomRepository quizRoomRepository) {
		this.quizRoomRepository = quizRoomRepository;
	}

	/**
	 * 중복되지 않는 4자리 숫자 문자열 방 코드를 생성합니다.
	 */
	public String generateUniqueRoomCode() {
		String roomCode;
		do {
			roomCode = String.format("%04d", random.nextInt(10000));
		} while (quizRoomRepository.existsByRoomCode(roomCode));
		return roomCode;
	}

	/**
	 * 새로운 방을 생성합니다.
	 */
	public QuizRoom createRoom(String hostUserId, String roomType) {
		String roomCode = generateUniqueRoomCode();
		QuizRoom room = QuizRoom.builder().roomCode(roomCode).hostUserId(hostUserId).status("OPEN").roomType(roomType)
				.createdAt(LocalDateTime.now()).build();

		return quizRoomRepository.save(room);
	}

	public boolean existsRoomCode(String roomCode) {
		return quizRoomRepository.existsByRoomCode(roomCode);
	}

	public QuizRoom findByRoomCode(String roomCode) {
		return quizRoomRepository.findByRoomCode(roomCode).orElse(null);
	}
}
