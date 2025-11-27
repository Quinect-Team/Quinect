package com.project.quiz.service;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.repository.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserInventoryRepository userInventoryRepository;

    // 1. 목록 조회 (화면 렌더링용)
    @Transactional(readOnly = true)
    public List<UserInventory> getMyInventoryByCategory(User user, String category) {
        return userInventoryRepository.findAllByUserAndItem_Category(user, category);
    }

    // 2. 아이템 장착 (교체)
    @Transactional
    public void equipItem(User user, String category, Long itemId) {
        // (1) 내 인벤토리에서 해당 카테고리 아이템 싹 가져옴
        List<UserInventory> myItems = userInventoryRepository.findAllByUserAndItem_Category(user, category);

        // (2) 일단 전부 장착 해제 (초기화)
        for (UserInventory inventory : myItems) {
            inventory.setEquipped(false);
        }

        // (3) 선택한 아이템(itemId)이 null이 아니면(기본 아님) 찾아서 장착
        if (itemId != null) {
            myItems.stream()
                .filter(inv -> inv.getItem().getId().equals(itemId))
                .findFirst()
                .ifPresent(target -> target.setEquipped(true));
        }
    }
}