package com.project.quiz.repository;

import com.project.quiz.domain.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
	
	long countByUserId(Long userId);
}
