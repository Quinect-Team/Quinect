package com.project.quiz.dto;

import com.project.quiz.domain.UserActivityPointChange;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class PointHistoryDto {
    private String date;   // "2025.11.27"
    private String reason; // "상점 아이템 구매"
    private int amount;    // -500
    private String type;   // "USE" or "EARN" (색상 구분용)

    public PointHistoryDto(UserActivityPointChange entity) {
        // 날짜 포맷팅 (yyyy.MM.dd)
        this.date = entity.getUserActivityLog().getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        
        this.reason = entity.getReason();
        this.amount = entity.getAmount();
        this.type = (entity.getAmount() > 0) ? "EARN" : "USE";
    }
}