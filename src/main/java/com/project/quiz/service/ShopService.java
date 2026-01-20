package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.ShopItem;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.dto.ItemPurchasedEvent;
import com.project.quiz.dto.ShopItemResponse;
import com.project.quiz.repository.ShopItemRepository;
import com.project.quiz.repository.UserInventoryRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopService {

	private final PointService pointService;
	private final ShopItemRepository shopItemRepository;
	private final UserRepository userRepository;
	private final UserInventoryRepository userInventoryRepository;
	private final ApplicationEventPublisher eventPublisher;

	// 1. 상점 목록 조회
	public List<ShopItem> getAvailableItems() {
		return shopItemRepository.findByIsAvailableTrue();
	}

	// 2. 아이템 구매 로직

	@Transactional
	public void buyItem(String email, Long itemId) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
		ShopItem item = shopItemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("아이템 없음"));

		String logDescription = user.getUserProfile().getUsername() + "님이 상점에서 [" + item.getItemName()
				+ "] 아이템을 구매했습니다.";

		pointService.usePoint(user, item.getPrice(), logDescription);

		// 인벤토리 지급 로직은 그대로 ShopService가 담당
		UserInventory inventory = UserInventory.builder().user(user).item(item).purchasedAt(LocalDateTime.now())
				.build();

		userInventoryRepository.save(inventory);
		eventPublisher.publishEvent(new ItemPurchasedEvent(user));
	}

	@Transactional(readOnly = true)
	public List<ShopItemResponse> getAvailableItems(String email, String category) {

		// 1. 아이템 목록 조회 (카테고리 유무에 따라 분기)
		List<ShopItem> items;
		if (category != null && !category.isEmpty()) {
			// 카테고리가 선택된 경우 (예: "BORDER", "THEME")
			items = shopItemRepository.findByIsAvailableTrueAndCategory(category);
		} else {
			// 전체보기인 경우
			items = shopItemRepository.findByIsAvailableTrue();
		}

		// 2. 유저의 보유 아이템 확인 (기존 로직 유지)
		Set<Long> ownedItemIds;
		if (email != null && !email.isEmpty()) {
			User user = userRepository.findByEmail(email).orElse(null);
			if (user != null) {
				ownedItemIds = userInventoryRepository.findAllByUser(user).stream().map(inv -> inv.getItem().getId())
						.collect(Collectors.toSet());
			} else {
				ownedItemIds = Set.of();
			}
		} else {
			ownedItemIds = Set.of();
		}

		// 3. DTO 변환 (기존 로직 유지)
		return items.stream().map(item -> new ShopItemResponse(item, ownedItemIds.contains(item.getId())))
				.collect(Collectors.toList());
	}
}