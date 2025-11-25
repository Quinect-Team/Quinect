package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Room;
import com.project.quiz.service.ParticipantService;
import com.project.quiz.service.RoomService;

@Controller
@RequiredArgsConstructor
public class ParticipantController {
	private final ParticipantService participantService;
	@Autowired
	private final RoomService roomService;

	@PostMapping("/participant/save")
	@ResponseBody
	public String saveParticipant(@RequestBody Participant participant) {
		participantService.saveParticipant(participant);
		return "saved";
	}

	@GetMapping("/participant/list/{roomCode}")
	@ResponseBody
	public List<Participant> getParticipants(@PathVariable("roomCode") String roomCode) {
		Room room = roomService.getRoomByCode(roomCode); // 일관성 있게 roomCode 사용
		return participantService.findByRoom(room);
	}
}
