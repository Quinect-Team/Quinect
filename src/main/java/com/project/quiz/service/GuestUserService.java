package com.project.quiz.service;

import org.springframework.stereotype.Service;

@Service
public class GuestUserService {

    // 닉네임/캐릭터 유효성 체크 등 비즈니스 로직 예시
    public boolean validateNickname(String nickname) {
        // 예시: 금칙어 필터, 길이, 중복 등
        return nickname != null && !nickname.trim().isEmpty() && nickname.length() < 20;
    }

    public boolean validateCharacter(String characterImageUrl) {
        // 예시: 허용된 이미지 목록, URL 패턴 등
        return characterImageUrl != null && characterImageUrl.endsWith(".svg");
    }
}
