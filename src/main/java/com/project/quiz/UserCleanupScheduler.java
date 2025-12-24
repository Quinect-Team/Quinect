package com.project.quiz;

import com.project.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserService userService;

    // 매일 자정(0초 0분 0시)에 실행
    //@Scheduled(cron = "0/30 * * * * *") 테스트용
    @Scheduled(cron = "0 0 0 * * *")
    public void runCleanup() {
        System.out.println("🧹 [Scheduler] 탈퇴 유저 데이터 익명화 작업 시작...");
        userService.cleanupWithdrawnUsers();
        System.out.println("✨ [Scheduler] 작업 완료.");
    }
}