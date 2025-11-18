package com.project.quiz.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.dto.QuizDTO;

@RestController
public class QuizSocketController {
	
	@MessageMapping("/messages")
	@SendTo("/sub/message")
	public QuizDTO sendMessage(@RequestBody QuizDTO quizDto) {
		return quizDto;
	}
}
