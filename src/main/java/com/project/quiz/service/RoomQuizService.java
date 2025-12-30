package com.project.quiz.service;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.RoomQuiz;
import com.project.quiz.repository.RoomQuizRepository;
import com.project.quiz.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RoomQuizService {

	@Autowired
	private RoomQuizRepository roomQuizRepository;

	@Autowired
	private QuizRepository quizRepository;

	/**
	 * 방에 퀴즈 추가
	 */
	public RoomQuiz createRoomQuiz(Long roomId, Long quizId) {
		RoomQuiz roomQuiz = new RoomQuiz();
		roomQuiz.setRoomId(roomId);
		roomQuiz.setQuizId(quizId);
		roomQuiz.setAssignedAt(LocalDateTime.now());

		return roomQuizRepository.save(roomQuiz);
	}

	/**
	 * 방의 가장 최근 퀴즈 조회
	 */
	public Quiz getLatestQuizByRoom(Long roomId) {
		Optional<RoomQuiz> roomQuiz = roomQuizRepository.findFirstByRoomIdOrderByAssignedAtDesc(roomId);

		if (roomQuiz.isPresent()) {
			return quizRepository.findById(roomQuiz.get().getQuizId()).orElse(null);
		}

		return null;
	}
}
