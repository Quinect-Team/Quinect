package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.repository.QuizRepository;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.AttendanceService;
import com.project.quiz.service.QuizAnswerService;
import com.project.quiz.service.QuizQuestionService;
import com.project.quiz.service.RoomQuizService;
import com.project.quiz.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class MainController {

	private final AttendanceService attendanceService;
	private final UserAchievementRepository userAchievementRepository;
	private final UserRepository userRepository;
	private final QuizRepository quizRepository;
	private final UserService userService;
	private final QuizQuestionService quizQuestionService;
	private final QuizAnswerService quizAnswerService;
	private final RoomQuizService roomQuizService;

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
	public String index(Principal principal, Model model) {
		if (principal != null) {
			return "redirect:/main";
		}
		long totalQuestions = quizQuestionService.getTotalQuestionCount();
		long activeUsers = userService.getActiveUsers();
		long totalAnswers = quizAnswerService.getTotalAnswer();
		long totalRoomQuizs = roomQuizService.getTotalRoomQuiz();


		model.addAttribute("totalQuestionCount", totalQuestions);
		model.addAttribute("activeUsersCount", activeUsers);
		model.addAttribute("totalAnswersCount", totalAnswers);
		model.addAttribute("totalRoomQuizsCount", totalRoomQuizs);
		
		return "index";
	}

	// 3. 메인 페이지 (로그인 후 접근 후 바로 출석체크 불러옴. 수정-2025-11-27)
	@GetMapping("/main")
	public String mainPage(Model model, Principal principal) {
		if (principal != null) {
			// ⭐⭐⭐ 디버깅 시작 ⭐⭐⭐
			System.out.println("========== mainPage 시작 ==========");
			System.out.println("principal.getName(): " + principal.getName());

			User user = userRepository.findByEmail(principal.getName()).orElse(null);

			System.out.println("DB에서 찾은 user: " + user);

			if (user != null) {
				Long userId = user.getId();

				System.out.println("사용자 ID: " + userId);
				System.out.println("사용자 이메일: " + user.getEmail());
				System.out.println("사용자 상태: " + user.getStatus());

				if ("pending".equals(user.getStatus())) {
					model.addAttribute("isAccountPending", true);
				}

				if ("deleted".equals(user.getStatus())) {
					SecurityContextHolder.clearContext();
					return "redirect:/login?error=deleted";
				}

				boolean checkedIn = attendanceService.hasCheckedInToday(principal.getName());
				model.addAttribute("checkedIn", checkedIn);

				long achievedCount = userAchievementRepository.countByUserAndIsAchievedTrue(user);
				model.addAttribute("achievedCount", achievedCount);

				long createdCount = quizRepository.countByUserId(userId);
				model.addAttribute("createdCount", createdCount);

				// ⭐⭐⭐ 여기서 userId 확인 ⭐⭐⭐
				System.out.println("getUserAnswerStats 호출 전 userId: " + userId);

				Map<String, Long> stats = userService.getUserAnswerStats(userId);

				System.out.println("정답 개수: " + stats.get("correct"));
				System.out.println("오답 개수: " + stats.get("wrong"));
				System.out.println("========== 로드 완료 ==========");

				model.addAttribute("correctCount", stats.get("correct"));
				model.addAttribute("wrongCount", stats.get("wrong"));
			} else {
				System.out.println("❌ findByEmail 실패 - 사용자를 찾을 수 없음!");
			}
		} else {
			System.out.println("❌ principal이 null입니다!");
		}

		List<UserProfile> topUsers = userService.getTopUsersByPointBalance();
		model.addAttribute("topUsers", topUsers);

		System.out.println("📊 메인 페이지 로드 - 상위 " + topUsers.size() + "명 리더보드");

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