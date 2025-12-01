package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QuizViewController {
	@GetMapping("/list")
    public String list() {
        return "/layout/quiz_list";  // src/main/resources/templates/layout/quiz_list.html을 렌더링
    }
	
	@GetMapping("/setquestion")
    public String setquestion() {
        return "/layout/setquestion";  // src/main/resources/templates/layout/setquestion.html을 렌더링
    }

}