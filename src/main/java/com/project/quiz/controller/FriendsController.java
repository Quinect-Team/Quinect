package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FriendsController {
	@GetMapping("/friendsList")
	public String shopPage() {
		return "/layout/friendsList";
	}
}
