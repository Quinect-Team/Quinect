package com.project.quiz.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.quiz.domain.Quiz;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.service.QuizService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class QuizViewController {
    private final QuizService quizService;
	
	@GetMapping("/list")
    public String list(Model model) {

        List<Quiz> quizzes = quizService.findAll();

        System.out.println("===== quiz count: " + quizzes.size());

        model.addAttribute("quizzes", quizzes);
        return "layout/quiz_list";
    }
	
	@GetMapping("/setquestion")
    public String setquestion() {
        return "/layout/setquestion";  // src/main/resources/templates/layout/setquestion.html을 렌더링
    }
	
	 // 템플릿 반환 ONLY
    @GetMapping("/view/{id}")
    public String view(@PathVariable("id") Long id, Model model) {
        model.addAttribute("quizId", id);
        return "layout/quiz_view"; 
    }

    // JSON ONLY
    @ResponseBody
    @GetMapping("/api/quiz/{id}")
    public QuizDto getQuiz(@PathVariable("id") Long id) {
        return quizService.findQuizDto(id);
    }
}