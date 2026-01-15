package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.quiz.domain.QuizGrading;

public interface QuizGradingRepository extends JpaRepository<QuizGrading, Long> {

	// ⭐ QuizGrading → QuizAnswer → QuizSubmission → userId 경로로 조회
	@Query("SELECT COUNT(qg) FROM QuizGrading qg "
			+ "WHERE qg.answer.submission.userId = :userId AND qg.correct = true")
	long countByUserIdAndIsCorrectTrue(@Param("userId") Long userId);

	@Query("SELECT COUNT(qg) FROM QuizGrading qg "
			+ "WHERE qg.answer.submission.userId = :userId AND qg.correct = false")
	long countByUserIdAndIsCorrectFalse(@Param("userId") Long userId);

}
