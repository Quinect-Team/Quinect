package com.project.quiz.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.quiz.domain.BoardPost;
import com.project.quiz.service.BoardPostService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BoardController {

	private final BoardPostService boardPostService;

	@GetMapping("/board")
	public String showBoardList(Model model) {
		List<BoardPost> posts = boardPostService.findAll(); // 또는 페이징 방식 등
		model.addAttribute("posts", posts);
		return "board_list";
	}

}
