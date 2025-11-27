package com.project.quiz.dto;

import com.project.quiz.domain.UserActivityAttendance;
import lombok.Getter;

@Getter
public class AttendanceEventDto {
    private String title; // 달력에 표시될 글자 (예: "+100P")
    private String start; // 날짜 (YYYY-MM-DD)
    private String color; // 이벤트 배경색 (초록색)
    private String textColor; // 글자색 (흰색)
    private boolean allDay; // 하루 종일 이벤트 여부 (true)

    public AttendanceEventDto(UserActivityAttendance entity) {
        this.title = "✅ 출석"; // 또는 "+" + entity.getPointsEarned() + "P";
        this.start = entity.getCheckInDate().toString(); // LocalDate -> String
        this.color = "#1cc88a"; // sb-admin-2의 success 색상
        this.textColor = "#ffffff";
        this.allDay = true;
    }
}