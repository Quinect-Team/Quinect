package com.project.quiz.dto;

import com.project.quiz.domain.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
	private Long id;
	private String email;
	private String username; // UserProfile에서
	private String profileImage; // UserProfile에서
	private String friendshipStatus; // 추가: null, "PENDING", "ACCEPTED", "REJECTED"

	// User 엔터티를 받아서 DTO로 변환
	public static UserSearchResponse from(User user) {
		return UserSearchResponse.builder().id(user.getId()).email(user.getEmail())
				.username(user.getUserProfile() != null ? user.getUserProfile().getUsername() : null)
				.profileImage(user.getUserProfile() != null ? user.getUserProfile().getProfileImage() : null)
				.friendshipStatus(null) // 기본값, Service에서 설정
				.build();
	}

	public static UserSearchResponse from(User user, String friendshipStatus) {
		return UserSearchResponse.builder().id(user.getId()).email(user.getEmail())
				.username(user.getUserProfile() != null ? user.getUserProfile().getUsername() : null)
				.profileImage(user.getUserProfile() != null ? user.getUserProfile().getProfileImage() : null)
				.friendshipStatus(friendshipStatus).build();
	}
}
