package com.project.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;

public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {
	List<UserInventory> findAllByUser(User user);
    // "내(User)가 가진 것 중에, 아이템 카테고리(Item.Category)가 일치하는 것들" 조회
    List<UserInventory> findAllByUserAndItem_Category(User user, String category);
    Optional<UserInventory> findByUserAndItem_CategoryAndIsEquippedTrue(User user, String category);
}