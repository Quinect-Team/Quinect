package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
	private Long id;
    private String title;       // 예: "업적 달성!"
    private String content;     // 예: "[성실한 출석러] 메달을 획득했습니다."
    private String type;        // 예: "ACHIEVEMENT"
    private String url;         // 클릭 시 이동할 링크 (옵션)
    private LocalDateTime createdAt;
}