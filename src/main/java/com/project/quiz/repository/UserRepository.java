package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
}