package com.project.quiz.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.project.quiz.domain.Quiz;
import com.project.quiz.service.QuizService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class QuizViewController {

//    private final TestGeminiController testGeminiController;
	private final QuizService quizService;

//    QuizViewController(TestGeminiController testGeminiController) {
//        this.testGeminiController = testGeminiController;
//		this.quizService = null;
//    }

	@GetMapping("/list")
	public String list(Model model) {

		List<Quiz> quizzes = quizService.findAll();

		System.out.println("===== quiz count: " + quizzes.size());

		model.addAttribute("quizzes", quizzes);
		return "quiz_list";
	}

	@GetMapping("/setquestion")
	public String setquestion(@RequestParam(value = "quizId", required = false) Long quizId, Model model) {
		model.addAttribute("quizId", quizId);
		return "setquestion";
	}

	// 템플릿 반환 ONLY
	@GetMapping("/quiz/view/{id}")
	public String view(@PathVariable("id") Long id, Model model) {
		model.addAttribute("quizId", id);
		return "layout/quiz_view";
	}

	@GetMapping("/my")
	public String myQuizPage() {
		return "/my_quiz";
	}
}