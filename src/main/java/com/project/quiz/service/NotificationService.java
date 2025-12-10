package com.project.quiz.service;

import com.project.quiz.domain.*;
import com.project.quiz.dto.NotificationResponse;
import com.project.quiz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final SimpMessagingTemplate messagingTemplate; // 웹소켓 발송기

    @Transactional
    public void send(User user, String title, String content, String type) {
        // 1. 알림 내용 생성 (Notification 테이블)
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotificationType(type);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsBroadcast(false); 
        
        // ★★★ [수정] 저장된 객체(ID가 있는 객체)를 변수로 받습니다.
        Notification savedNotification = notificationRepository.save(notification);

        // 2. 수신자 매핑 (NotificationRecipient 테이블)
        NotificationRecipient recipient = new NotificationRecipient();
        
        // ★★★ [수정] 위에서 저장한 savedNotification을 넣어줍니다.
        recipient.setNotification(savedNotification); 
        
        recipient.setUser(user);
        recipient.setIsRead(false); // 안 읽음
        
        // Recipient 저장 (이제 부모 ID가 확실하므로 에러가 안 납니다)
        NotificationRecipient savedRecipient = recipientRepository.save(recipient);

        // [추가/수정] URL 결정 로직 (이전 단계에서 적용한 내용 유지)
        String targetUrl = "/"; 
        if ("ACHIEVEMENT".equals(type)) {
            targetUrl = "/achievement";
        }
        
        // 3. 실시간 웹소켓 전송
        NotificationResponse response = NotificationResponse.builder()
                .id(savedRecipient.getId())
                .title(title)
                .content(content)
                .type(type)
                .url(targetUrl)
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                user.getEmail(), 
                "/queue/notifications", 
                response
        );
    }
}