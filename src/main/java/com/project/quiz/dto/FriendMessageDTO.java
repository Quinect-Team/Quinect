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
	private Long friendshipId; // ⭐ 추가
	private String messageText;
	private Boolean isRead;
	private LocalDateTime sentAt;
	private Long senderId;
	private String senderName;
	private String profileImage;

	public FriendMessageDTO(FriendMessage message) {
		this.id = message.getId();
		this.friendshipId = message.getFriendship().getId(); // ⭐ 핵심
		this.messageText = message.getMessageText();
		this.isRead = message.getIsRead();
		this.sentAt = message.getSentAt();
		this.senderId = message.getSenderId();
	}
}
