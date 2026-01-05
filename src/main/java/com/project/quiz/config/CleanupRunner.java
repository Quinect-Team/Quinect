package com.project.quiz.config;

import com.project.quiz.service.UserHardDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CleanupRunner implements CommandLineRunner {

    private final UserHardDeleteService userHardDeleteService;

    @Override
    public void run(String... args) throws Exception {
        // 실행 인자에 "--cleanup"이 있는지 확인
        if (Arrays.asList(args).contains("--cleanup")) {
            System.out.println("🚀 [Cleanup Mode] 데이터 정리 작업을 시작합니다...");
            
            userHardDeleteService.executeHardDeleteProcess();
            
            System.out.println("🏁 [Cleanup Mode] 작업 완료. 애플리케이션을 종료합니다.");
            System.exit(0); // 작업 후 서버 종료
        }
    }
}