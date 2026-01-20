package com.project.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.quiz.repository.QuizAnswerRepository;

@Service
public class QuizAnswerService {
	
	@Autowired
	private QuizAnswerRepository quizAnswerRepository;
	
	public long getTotalAnswer() {
		return quizAnswerRepository.count();
	}
}
