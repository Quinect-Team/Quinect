package com.project.quiz.domain;

import lombok.*;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "friend_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "friendship_id", nullable = false)
	private Friendship friendship;

	@Column(nullable = false)
	private Long senderId;

	@Column(columnDefinition = "TEXT")
	private String messageText;

	@Column(nullable = false)
	private Boolean isRead;

	@Column(nullable = false, updatable = false)
	private LocalDateTime sentAt;

	@PrePersist
	protected void onCreate() {
		this.sentAt = LocalDateTime.now();
	}

	public void markAsRead() {
		if (!this.isRead) {
			this.isRead = true;
		}
	}
}
