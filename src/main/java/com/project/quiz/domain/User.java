package com.project.quiz.domain;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "password", length = 255)
	private String password;

	@Column(name = "username", length = 255)
	private String username;

	@Column(name = "profile_image", length = 512)
	private String profileImage;

	@Column(name = "point_balance")
	private Long pointBalance;

	@Column(name = "provider", length = 50)
	private String provider;

	@Column(name = "provider_id", length = 255)
	private String providerId;

	@Column(name = "status", length = 20)
	private String status;
	
	@Column(name = "role", length = 50)
    private String role;
}
