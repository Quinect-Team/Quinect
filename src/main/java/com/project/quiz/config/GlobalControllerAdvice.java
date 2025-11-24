package com.project.quiz.config;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@RequiredArgsConstructor
@ControllerAdvice // 모든 컨트롤러 전역에서 동작하게 함
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    // 모든 요청마다 이 메서드가 먼저 실행되어 model에 "user"라는 이름으로 정보를 담아줌
    @ModelAttribute
    public void addUserDetails(Model model, Principal principal) {
        if (principal != null) {
            // 로그인한 상태라면 (principal.getName()은 우리가 설정한 email임)
            String email = principal.getName();
            
            // DB에서 최신 정보 조회 (포인트 변동 등을 실시간 반영하기 위해)
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                model.addAttribute("user", user); // HTML에서 ${user}로 사용 가능!
            }
        }
    }
}