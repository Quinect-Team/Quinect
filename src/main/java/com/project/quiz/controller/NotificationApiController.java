package com.project.quiz.controller;

import com.project.quiz.domain.NotificationRecipient;
import com.project.quiz.repository.NotificationRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationRecipientRepository recipientRepository;

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<?> readNotification(@PathVariable("id") Long id) {
        NotificationRecipient recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림이 없습니다."));
        
        recipient.markAsRead(); // isRead = true, readAt = now() 처리
        
        return ResponseEntity.ok().build();
    }
}