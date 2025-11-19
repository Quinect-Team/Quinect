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
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // 회원번호

	@Column(nullable = false)
	private String username; // 유저 닉네임 (아이디)

	@Column(nullable = false)
	private String password; // 유저 비밀번호

	@Column(nullable = false)
	private String email; // 유저 이메일

	@Column(length = 50)
	private String role; // 유저 역할 (관리자, 일반회원-교사용,학생용)

	private LocalDateTime createdAt; // 생성 일시
}
