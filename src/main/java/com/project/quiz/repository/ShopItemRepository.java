package com.project.quiz.repository;

import com.project.quiz.domain.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    // 판매 중인 상품만 가져오기
    List<ShopItem> findByIsAvailableTrue();
}