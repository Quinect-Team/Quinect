package com.project.quiz.controller;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import com.project.quiz.dto.ChatMessage;

@Controller
public class ChatController {

	// 클라이언트가 /app/chat/{roomCode}로 메시지 전송 시 호출됨
	@MessageMapping("/chat/{roomCode}")
	@SendTo("/topic/chat/{roomCode}")
	public ChatMessage handleRoomChat(@DestinationVariable("roomCode") String roomCode, ChatMessage message) {
		// 필요 시 메시지 검증/필터링/로그 저장 로직 추가 가능
		return message; // 같은 방 참가자에게 브로드캐스트
	}
}
