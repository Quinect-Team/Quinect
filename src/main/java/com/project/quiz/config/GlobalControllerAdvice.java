package com.project.quiz.config;

import java.security.Principal;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.project.quiz.domain.NotificationRecipient;
import com.project.quiz.domain.User;
import com.project.quiz.repository.BoardRepository;
import com.project.quiz.repository.NotificationRecipientRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ControllerAdvice // 모든 컨트롤러 전역에서 동작하게 함
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final BoardRepository boardRepository;

    // 모든 요청마다 이 메서드가 먼저 실행되어 model에 "user"라는 이름으로 정보를 담아줌
    @ModelAttribute
    public void addUserDetails(Model model, Principal principal) {
    	boardRepository.findFirstByBoardTypeCodeOrderByCreatedAtDesc("notice")
        .ifPresent(notice -> model.addAttribute("latestNotice", notice));
    	
    	if (principal != null) {
            String email = principal.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                // 1. 로그인 유저 정보 담기 (기존 코드)
                model.addAttribute("user", user);

                // ----------------------------------------------------
                // [추가] 2. 알림 데이터 담기 (새로고침 시 유지용)
                // ----------------------------------------------------
                
                // (1) 내 알림 목록 가져오기 (최신순)
                List<NotificationRecipient> notifications = 
                    notificationRecipientRepository.findByUserOrderByNotification_CreatedAtDesc(user);
                
                // (2) 안 읽은 개수 가져오기
                long unreadCount = notificationRecipientRepository.countByUserAndIsReadFalse(user);

                // (3) HTML(topbar2.html)에서 쓸 이름으로 모델에 추가
                model.addAttribute("notifications", notifications);
                model.addAttribute("unreadCount", unreadCount);
                // ----------------------------------------------------
            }
        }
    }
}