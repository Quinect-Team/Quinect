package com.project.quiz.controller;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.dto.ChatMessage;

@Controller
public class RoomController {
	
	@GetMapping("/waitroom")
    public String waitroom() {
        return "waitroom";  // src/main/resources/templates/waitroom.html을 렌더링
    }
	
	@MessageMapping("/waitroom/join")
	@SendTo("/topic/waitroom")
	public ChatMessage joinRoom(ChatMessage message) {
		// 방 참가 처리 로직 예: 방 참가자 목록에 추가
		return message;
	}

	@MessageMapping("/waitroom/message")
	@SendTo("/topic/waitroom")
	public ChatMessage sendMessage(ChatMessage message) {
		// 메시지 브로드캐스트 처리
		return message;
	}
}
