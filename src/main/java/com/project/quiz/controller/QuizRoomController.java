package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.quiz.domain.QuizRoom;
import com.project.quiz.service.QuizRoomService;

@Controller
public class QuizRoomController {

	private final QuizRoomService quizRoomService;

	public QuizRoomController(QuizRoomService quizRoomService) {
		this.quizRoomService = quizRoomService;
	}
	
	@GetMapping("/")
	public String home() {
	    return "index";
	}

	// 방 생성 요청
	@GetMapping("/create-room")
	public String createRoom(RedirectAttributes redirectAttributes) {
		String hostUserId = "hostUser"; // 나중에 인증된 사용자 정보로 대체
		String roomType = "객관식"; // 나중에 선택 가능하도록 개선
		QuizRoom newRoom = quizRoomService.createRoom(hostUserId, roomType);
		redirectAttributes.addFlashAttribute("roomCode", newRoom.getRoomCode());
		return "redirect:/layout/room/" + newRoom.getRoomCode();
	}

	// 방 입장 페이지
	@GetMapping("/layout/room/{roomCode}")
	public String room(@PathVariable("roomCode") String roomCode, Model model) {
		QuizRoom room = quizRoomService.findByRoomCode(roomCode);
		if (room == null) {
			model.addAttribute("error", "존재하지 않는 방 코드입니다.");
			return "error"; // 에러 처리 뷰
		}
		model.addAttribute("room", room);
	    model.addAttribute("mainContent", "layout/room :: room");
		return "layout/room"; // room.html 렌더링
	}
}
