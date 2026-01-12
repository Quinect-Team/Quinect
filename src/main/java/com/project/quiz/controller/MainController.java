package com.project.quiz.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.quiz.domain.User;
import com.project.quiz.repository.QuizRepository;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.AttendanceService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class MainController {

	private final AttendanceService attendanceService;
	private final UserAchievementRepository userAchievementRepository;
	private final UserRepository userRepository;
	private final QuizRepository quizRepository;

	// 1. 루트 접속 시 /index로 리다이렉트 (또는 바로 index 보여주기)
	@GetMapping("/")
	public String root(Principal principal) {
		// 1. 로그인한 사용자(Principal이 null이 아님)라면 -> 메인으로
		if (principal != null) {
			return "redirect:/main";
		}

		// 2. 로그인 안 한 손님이라면 -> 인덱스로
		return "redirect:/index";
	}

	// 2. 인덱스 페이지 (비로그인 접근 가능)
	@GetMapping("/index")
	public String index(Principal principal) {
		if (principal != null) {
			return "redirect:/main";
		}
		return "index";
	}

	// 3. 메인 페이지 (로그인 후 접근 후 바로 출석체크 불러옴. 수정-2025-11-27)
	@GetMapping("/main")
	public String mainPage(Model model, Principal principal) {
	    if (principal != null) {
	        User user = userRepository.findByEmail(principal.getName()).orElse(null);
	        
	        if (user != null) {
	            
	        	Long userId = user.getId();
	        	
	            // ▼▼▼ [수정됨] 리다이렉트 대신 모델에 플래그 추가 ▼▼▼
	            if ("pending".equals(user.getStatus())) {
	                model.addAttribute("isAccountPending", true);
	                // 여기서 return을 하지 않고 아래 로직을 그대로 수행하여 main 페이지를 렌더링합니다.
	            }
	            
	            // 삭제된 계정 차단 (기존 로직 유지)
	            if ("deleted".equals(user.getStatus())) {
	                SecurityContextHolder.clearContext();
	                return "redirect:/login?error=deleted";
	            }

	            // 기존 로직들 (출석, 업적 등) 수행
	            boolean checkedIn = attendanceService.hasCheckedInToday(principal.getName());
	            model.addAttribute("checkedIn", checkedIn);

	            long achievedCount = userAchievementRepository.countByUserAndIsAchievedTrue(user);
	            model.addAttribute("achievedCount", achievedCount);
	            
	            long createdCount = quizRepository.countByUserId(userId);
	            model.addAttribute("createdCount", createdCount);
	        }
	    }
	    return "main";
	}

	// 출석체크 AJAX 요청 처리
	@PostMapping("/api/attendance/check")
	@ResponseBody
	public ResponseEntity<String> doAttendance(Principal principal) {
		try {
			attendanceService.checkIn(principal.getName());
			return ResponseEntity.ok("출석체크 완료! 100P가 적립되었습니다.");
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body("오류가 발생했습니다.");
		}
	}
}