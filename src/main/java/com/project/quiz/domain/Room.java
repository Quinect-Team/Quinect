package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // 방 고유번호 (DB PK)

	@Column(nullable = false, unique = true, length = 4)
	private String roomCode; // 4자리 방 코드 (유니크)

	@Column(nullable = false)
	private String hostUserId; // 호스트 사용자 식별자 (FK로 확장 가능)

	@Column(nullable = false)
	private String status; // 방 상태 ("OPEN", "CLOSED" 등)

	@Column(length = 50)
	private String roomType; // 방 유형 (예: "객관식", "주관식", 나중에 FK로 확장 가능)

	private LocalDateTime createdAt; // 생성 일시

	private LocalDateTime closedAt; // 종료 일시 (종료 시 기록)
}
