package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime joinAt;

    // FK: room 테이블
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // FK: user 테이블 (nullable 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(nullable = true, length = 100)
    private String guestId;     // 게스트만 값(UUID 등)

    @Column(length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatarUrl;

    private Long score;

    private Long ranking;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean ready;
}
