package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerResult {
	private boolean isCorrect;
	private int points;
	private int questionPoint;
}
