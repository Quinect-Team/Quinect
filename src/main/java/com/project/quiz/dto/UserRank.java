package com.project.quiz.dto;

import java.util.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRank {
	private Long userId;
	private String nickname;
	private Integer score;
	private Integer rank;

	private Set<Long> correctQuestionIds = new HashSet<>();

	public void markCorrect(Long questionId) {
		correctQuestionIds.add(questionId);
	}

	public int getCorrectCount() {
		return correctQuestionIds.size();
	}
}
