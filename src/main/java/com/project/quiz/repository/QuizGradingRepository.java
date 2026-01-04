package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.quiz.domain.QuizGrading;

public interface QuizGradingRepository extends JpaRepository<QuizGrading, Long> {
}
