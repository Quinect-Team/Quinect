package com.project.quiz.domain;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friendship")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	// 친구 요청을 보낸 사용자
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// 친구 요청을 받은 사용자
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "friend_user_id", nullable = false)
	private User friendUser;

	// 친구 상태 (pending, accepted, rejected, blocked)
	@Column(name = "status", length = 20, nullable = false)
	private String status;

	// 친구 신청 날짜
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수락/거절/차단 날짜
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
