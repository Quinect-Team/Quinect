package com.project.quiz.dto;

import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendMessageDTO {
 private Long id;
 private String messageText;
 private Boolean isRead;
 private LocalDateTime sentAt;
 private Long senderId;
 private String senderName;
}
