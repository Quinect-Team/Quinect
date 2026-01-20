package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Entity
@Table(name = "quiz")
@Getter
@Setter
public class Quiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long quizId;

	private String title;

	private String description;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<QuizQuestion> questions = new ArrayList<>();

	public void addQuestion(QuizQuestion question) {
		questions.add(question);
		question.setQuiz(this);
	}

	@Column(name = "score_public", nullable = false)
	private boolean scorePublic = true;

}