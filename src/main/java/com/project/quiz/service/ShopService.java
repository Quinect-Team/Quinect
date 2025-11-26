package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.ShopItem;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.ShopItemResponse;
import com.project.quiz.repository.ShopItemRepository;
import com.project.quiz.repository.UserInventoryRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final UserRepository userRepository;
    private final UserInventoryRepository userInventoryRepository;

    // 1. 상점 목록 조회
    public List<ShopItem> getAvailableItems() {
        return shopItemRepository.findByIsAvailableTrue();
    }

    // 2. 아이템 구매 로직
    @Transactional
    public void buyItem(String userEmail, Long itemId) {
        // 유저 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 아이템 조회
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        UserProfile profile = user.getUserProfile();

        // 포인트 부족 체크
        if (profile.getPointBalance() < item.getPrice()) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }

        // [핵심] 1. 포인트 차감
        profile.setPointBalance(profile.getPointBalance() - item.getPrice());

        // [핵심] 2. 인벤토리에 지급
        UserInventory inventory = UserInventory.builder()
                .user(user)
                .item(item)
                .purchasedAt(LocalDateTime.now())
                .build();
        
        userInventoryRepository.save(inventory);
        
        // (선택사항) 여기서 포인트 변동 내역(History) 테이블에도 insert 하면 좋습니다.
    }
    
    @Transactional(readOnly = true)
    public List<ShopItemResponse> getAvailableItems(String email) {
        // 1. 현재 로그인한 유저 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 2. 상점의 모든 아이템 가져오기
        List<ShopItem> allItems = shopItemRepository.findByIsAvailableTrue();

        // 3. [핵심] 내가 가진 아이템 ID들만 뽑아서 Set으로 만듦 (검색 속도 O(1)로 최적화)
        // 예: [1, 5, 7]
        Set<Long> myInventoryItemIds = userInventoryRepository.findAllByUser(user)
                .stream()
                .map(inventory -> inventory.getItem().getId())
                .collect(Collectors.toSet());

        // 4. 아이템을 DTO로 변환하면서 '보유 여부(isOwned)' 세팅
        return allItems.stream()
                .map(item -> new ShopItemResponse(item, myInventoryItemIds.contains(item.getId())))
                .collect(Collectors.toList());
    }
}