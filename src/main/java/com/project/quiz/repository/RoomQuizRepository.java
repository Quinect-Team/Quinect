package com.project.quiz.repository;

import com.project.quiz.domain.RoomQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomQuizRepository extends JpaRepository<RoomQuiz, Long> {

	// 특정 방의 모든 퀴즈 조회
	List<RoomQuiz> findByRoomId(Long roomId);

	// 특정 방의 가장 최근 퀴즈 조회
	Optional<RoomQuiz> findFirstByRoomIdOrderByAssignedAtDesc(Long roomId);
}
