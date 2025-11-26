package com.project.quiz.dto;

import com.project.quiz.domain.ShopItem;
import lombok.Getter;

@Getter
public class ShopItemResponse {
    private Long id;
    private String itemName;
    private int price;
    private String description;
    private String imageUrl;
    private boolean isOwned; // 핵심: 보유 여부

    // 엔티티 -> DTO 변환 생성자
    public ShopItemResponse(ShopItem item, boolean isOwned) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.price = item.getPrice();
        this.description = item.getDescription();
        this.imageUrl = item.getImageUrl();
        this.isOwned = isOwned;
    }
}