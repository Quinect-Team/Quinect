package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "host_user_id")
    private Long hostUserId;

    @Column(name = "status_code", length = 50)
    private String statusCode;

    @Column(name = "closed_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime closedAt;

    @Column(name = "room_code", length = 4, unique = true)
    private String roomCode;

    // 방 생성 시 4자리 영문 대문자 roomCode 자동 생성
    public static String generateRoomCode() {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append((char) ('A' + rnd.nextInt(26)));
        }
        return sb.toString();
    }

    // 엔티티 생성용 팩토리(빌더)
    public static Room create(Long hostUserId, String roomTypeCode, String statusCode) {
        return Room.builder()
                .createdAt(LocalDateTime.now())
                .hostUserId(hostUserId)
                .statusCode(statusCode)
                .roomCode(generateRoomCode())
                .build();
    }
}
