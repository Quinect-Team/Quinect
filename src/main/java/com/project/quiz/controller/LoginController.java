package com.project.quiz.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.quiz.dto.UserCreateForm;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor // 생성자 주입을 위해 추가
@Controller
public class LoginController {

	private final UserService userService; // 서비스 주입

	@GetMapping("/login")
	public String loginPage(
	        @RequestParam(value = "status", required = false) String status, // ⭐ 파라미터 받기
	        Principal principal, 
	        Model model) {

	    // ⭐ [수정] 로그인이 되어있더라도, status가 pending이면 튕겨내지 않음
	    if (principal != null && !"pending".equals(status)) {
	        return "redirect:/main";
	    }

	    // pending 상태라면 모델에 값을 담아 HTML로 전달
	    if ("pending".equals(status)) {
	        model.addAttribute("isPending", true);
	    }
	    
	    return "login";
	}
	
	@GetMapping("/signup")
	public String signupPage(Principal principal) {
		if (principal != null) {
			return "redirect:/main";
		}
		return "signup"; // signup.html 보여줌
	}

	// ▼▼▼ 회원가입 처리 로직 추가 ▼▼▼
	@PostMapping("/register")
    public String signup(UserCreateForm userCreateForm) {
        userService.create(userCreateForm.getUsername(), 
                           userCreateForm.getEmail(), 
                           userCreateForm.getPassword());
        
        // [변경] 로그인 페이지가 아니라 -> 가입 성공 페이지로 이동
        return "redirect:/signup/success"; 
    }

    // ▼▼▼ [추가] 가입 성공 페이지 매핑 ▼▼▼
    @GetMapping("/signup/success")
    public String signupSuccess() {
        return "signupsuccess"; // templates/signupsuccess.html 호출
    }
}