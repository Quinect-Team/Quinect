package com.project.quiz.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
	private String type; // 예: "CHAT", "JOIN", "LEAVE"
	private String content; // 메시지 내용
	private String sender; // 보낸 사람 이름

}
