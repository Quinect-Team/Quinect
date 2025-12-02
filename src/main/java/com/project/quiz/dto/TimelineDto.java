package com.project.quiz.dto;

import com.project.quiz.domain.UserActivityLog;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class TimelineDto {
    private String description; // "OOO님이..."
    private String date;        // "2025.12.02"
    private String iconClass;   // 아이콘 (fa-flag, fa-gift)
    private String colorClass;  // 배경색 (bg-primary, bg-success)

    public TimelineDto(UserActivityLog log) {
        this.description = log.getDescription();
        this.date = log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        
        String type = log.getActivityType(); // REGISTER, ATTENDANCE, POINT_USE, POINT_EARN, QUIZ
        
        // 타입별 디자인 분기
        if ("REGISTER".equals(type)) {
            this.iconClass = "fa-flag";
            this.colorClass = "bg-primary"; // 파랑
        } 
        else if ("ATTENDANCE".equals(type)) {
            this.iconClass = "fa-calendar-check";
            this.colorClass = "bg-success"; // 초록
        } 
        else if ("POINT_USE".equals(type)) { 
            // 상점 구매는 포인트 사용으로 기록됨
            this.iconClass = "fa-shopping-cart";
            this.colorClass = "bg-warning"; // 노랑
        }
        else if ("QUIZ".equals(type)) {
            this.iconClass = "fa-gift";
            this.colorClass = "bg-info"; // 하늘색
        }
        else if ("UPDATE_NICKNAME".equals(type)) {
            this.iconClass = "fa-user-edit";
            this.colorClass = "bg-info"; // 하늘색
        }
        else {
            // 그 외 (단순 포인트 획득 등)
            this.iconClass = "fa-star";
            this.colorClass = "bg-secondary"; // 회색
        }
    }
}