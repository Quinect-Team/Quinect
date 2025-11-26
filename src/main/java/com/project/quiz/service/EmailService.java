package com.project.quiz.service;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    
    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendEmail(String to, String subject, String text) {
        // MimeMessage 생성
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            // MimeMessageHelper: 복잡한 설정을 쉽게 도와주는 도우미
            // 두 번째 인자 true: 멀티파트(첨부파일 등) 허용 여부 (여기선 크게 상관없음)
            // "UTF-8": 한글 깨짐 방지
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false); // false: html 아님 (일반 텍스트)

            // ▼▼▼ 핵심: 보낸 사람 이름 설정 ▼▼▼
            // setFrom("보내는이메일", "표시될이름")
            // 주의: 보내는 이메일은 application.properties의 계정과 일치해야 오류가 안 납니다.
            helper.setFrom(senderEmail, "Quinect 비밀번호 찾기");

            javaMailSender.send(message);

        } catch (MessagingException | UnsupportedEncodingException e) {
            // 에러 로그 출력 또는 예외 처리
            e.printStackTrace();
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
}