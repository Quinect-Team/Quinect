package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_quiz")
@Data
public class RoomQuiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId; // room과의 외래키

	@Column(name = "quiz_id", nullable = false)
	private Long quizId; // quiz와의 외래키

	@Column(name = "assigned_at")
	private LocalDateTime assignedAt;

	// ✅ 관계 설정 (선택사항)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", insertable = false, updatable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quiz_id", insertable = false, updatable = false)
	private Quiz quiz;
}
