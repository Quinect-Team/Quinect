package com.project.quiz.repository;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
	
}