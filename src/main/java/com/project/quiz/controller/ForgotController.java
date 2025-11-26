package com.project.quiz.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.quiz.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/forgot")
public class ForgotController {

    private final UserService userService;

    // 1. 이메일 입력 페이지 보여주기
    @GetMapping
    public String forgotPage(Principal principal) {
    	if (principal != null) {
            return "redirect:/main";
        }
        return "forgot"; 
    }

    // 2. [POST] 인증 코드 발송 요청
    @PostMapping("/send")
    public String sendVerificationCode(@RequestParam("email") String email, 
                                       HttpSession session, 
                                       Model model) {
        try {
            // 서비스 호출 (인증번호 생성 -> 저장 -> 메일 발송)
            userService.sendVerificationCode(email);
            
            // 다음 단계에서 쓰기 위해 세션에 이메일 임시 저장
            session.setAttribute("resetEmail", email);
            
            return "redirect:/forgot/verify"; // 인증 코드 입력 화면으로 이동

        } catch (IllegalArgumentException e) {
            // 예: 가입되지 않은 이메일 등 에러 발생 시
            model.addAttribute("error", e.getMessage());
            return "forgot"; // 다시 이메일 입력 화면으로
        }
    }

    // 3. 인증 코드 입력 페이지 보여주기
    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        // 세션에 이메일이 없으면(비정상 접근) 처음으로 돌려보냄
        if (session.getAttribute("resetEmail") == null) {
            return "redirect:/forgot";
        }
        return "verify";
    }

    // 4. [POST] 인증 코드 검증
    @PostMapping("/check")
    public String checkCode(@RequestParam("code") String code, 
                            HttpSession session, 
                            Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot";

        // 검증 서비스 호출
        boolean isVerified = userService.verifyCode(email, code);

        if (isVerified) {
            // 인증 성공! (비밀번호 변경 권한 부여)
            session.setAttribute("isVerified", true);
            return "redirect:/forgot/reset";
        } else {
            // 인증 실패
            model.addAttribute("error", "인증 코드가 올바르지 않습니다.");
            return "verify"; // 다시 코드 입력 화면
        }
    }

    // 5. 새 비밀번호 입력 페이지 보여주기
    @GetMapping("/reset")
    public String resetPasswordPage(HttpSession session) {
        // 인증된 사용자(isVerified)만 접근 가능
        if (session.getAttribute("resetEmail") == null || session.getAttribute("isVerified") == null) {
            return "redirect:/forgot";
        }
        return "resetpassword";
    }

    // 6. [POST] 비밀번호 변경 수행
    @PostMapping("/reset")
    public String updatePassword(@RequestParam("password") String password, 
                                 HttpSession session) {
        String email = (String) session.getAttribute("resetEmail");
        
        // 비밀번호 업데이트 로직 실행
        userService.updatePassword(email, password);

        // 세션 정리 (인증 정보 삭제)
        session.removeAttribute("resetEmail");
        session.removeAttribute("isVerified");
        
        // ▼▼▼ 로그인 페이지 대신 성공 페이지로 이동 ▼▼▼
        return "redirect:/forgot/success";
    }

    // 7. [GET] 성공 페이지 보여주기 (추가됨)
    @GetMapping("/success")
    public String successPage() {
        return "resetsuccess";
    }
}