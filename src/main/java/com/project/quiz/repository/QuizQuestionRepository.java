package com.project.quiz.repository;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizQuestion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
	
}