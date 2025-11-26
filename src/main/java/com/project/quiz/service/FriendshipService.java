package com.project.quiz.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipService {
	private final UserRepository userRepository;

	/**
	 * 이메일로 사용자 검색 (자기 자신 제외)
	 */
	public List<User> searchUsersByEmail(String email, Long currentUserId) {
		List<User> users = userRepository.findByEmailContaining(email);

		// 자기 자신 제외
		return users.stream().filter(user -> !user.getId().equals(currentUserId)).collect(Collectors.toList());
	}
}
