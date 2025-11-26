package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;

public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {
	List<UserInventory> findAllByUser(User user);
}