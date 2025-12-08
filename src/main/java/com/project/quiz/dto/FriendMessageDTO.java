package com.project.quiz.dto;

import com.project.quiz.domain.FriendMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendMessageDTO {
	private Long id;
	private String messageText;
	private Boolean isRead;
	private LocalDateTime sentAt;
	private Long senderId;
	private String senderName;

	// ⭐ 이 생성자를 추가하세요!
	public FriendMessageDTO(FriendMessage message) {
		this.id = message.getId();
		this.messageText = message.getMessageText();
		this.isRead = message.getIsRead();
		this.sentAt = message.getSentAt();
		this.senderId = message.getSenderId();
	}
}
