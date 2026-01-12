package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project.quiz.service.BoardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
	
	@GetMapping("")
	public String leaderboardPage() {
		return "leaderboard"; // timeline.html 껍데기만 렌더링
	}
}
