package com.project.quiz.controller;

import com.project.quiz.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequiredArgsConstructor
@RequestMapping("/forgot")
public class ForgotController {

    private final UserService userService;

    // 1. 이메일 입력 페이지
    @GetMapping
    public String forgotPage(Principal principal) {
        if (principal != null) return "redirect:/main";
        return "forgot";
    }

    // 2. [POST] 이메일 전송 (세션 시작)
    @PostMapping("/send")
    public String sendVerificationCode(@RequestParam("email") String email, 
                                       HttpSession session, 
                                       Model model) {
        try {
            userService.sendVerificationCode(email);
            
            // ★ 중요: 변수명 통일 (verifyEmail, verifyExpireAt)
            session.setAttribute("verifyEmail", email);
            session.setAttribute("verifyExpireAt", LocalDateTime.now().plusSeconds(180));
            
            return "redirect:/forgot/verify";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "forgot";
        }
    }

    // 3. [GET] 인증 페이지 (타이머 계산)
    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        // ★ 중요: 저장한 이름 그대로 꺼내기
        String email = (String) session.getAttribute("verifyEmail");
        LocalDateTime expireAt = (LocalDateTime) session.getAttribute("verifyExpireAt");

        // 세션 없으면 퇴장
        if (email == null || expireAt == null) {
            return "redirect:/login?invalid";
        }

        // 시간 계산
        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expireAt);

        if (remainingSeconds <= 0) {
            session.invalidate();
            return "redirect:/login?invalid";
        }

        model.addAttribute("remainingSeconds", remainingSeconds);
        return "verify";
    }

    // 4. [POST] 코드 검증 (틀렸을 때 처리 포함)
    @PostMapping("/check")
    public String checkCode(@RequestParam("code") String code, 
                            HttpSession session, 
                            Model model) {
        // ★ 중요: 저장한 이름 그대로 꺼내기
        String email = (String) session.getAttribute("verifyEmail");
        LocalDateTime expireAt = (LocalDateTime) session.getAttribute("verifyExpireAt");

        if (email == null || expireAt == null) {
            return "redirect:/login?invalid";
        }

        boolean isVerified = userService.verifyCode(email, code);

        if (isVerified) {
            // 성공
            session.setAttribute("isVerified", true);
            session.removeAttribute("verifyExpireAt"); // 타이머 제거
            return "redirect:/forgot/reset";
        } else {
            // 실패: 에러 메시지와 함께 다시 verify 페이지로 (타이머 유지)
            model.addAttribute("error", "인증 코드가 일치하지 않습니다.");
            
            long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), expireAt);
            model.addAttribute("remainingSeconds", remaining);
            
            return "verify"; 
        }
    }

    // 5. [GET] 비밀번호 변경 페이지
    @GetMapping("/reset")
    public String resetPasswordPage(HttpSession session) {
        if (session.getAttribute("verifyEmail") == null || session.getAttribute("isVerified") == null) {
            return "redirect:/login?invalid";
        }
        return "resetpassword";
    }

    // 6. [POST] 비밀번호 변경 수행
    @PostMapping("/reset")
    public String updatePassword(@RequestParam("password") String password, 
                                 HttpSession session) {
        String email = (String) session.getAttribute("verifyEmail");
        
        userService.updatePassword(email, password);

        session.removeAttribute("verifyEmail");
        session.removeAttribute("isVerified");
        
        return "redirect:/forgot/success"; 
    }

    // 7. 성공 페이지
    @GetMapping("/success")
    public String successPage() {
        return "resetsuccess";
    }
}