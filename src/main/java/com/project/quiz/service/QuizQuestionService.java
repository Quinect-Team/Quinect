package com.project.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.quiz.repository.QuizQuestionRepository;

@Service
public class QuizQuestionService {

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	// 👇 전체 문제 개수 조회
	public long getTotalQuestionCount() {
		return quizQuestionRepository.count();
	}
}
