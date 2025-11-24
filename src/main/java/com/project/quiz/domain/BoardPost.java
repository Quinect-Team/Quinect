package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "board_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(length = 50, nullable = false)
    private String boardTypeCode;     // FK는 없으므로 단순 문자열

    @Column(nullable = false)
    private Long userId;              // FK, 실제 User와 연관관계 필요하면 매핑

    @Column(length = 255, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    // 댓글 연관관계 (양방향 필요시)
    @OneToMany(mappedBy = "boardPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardComment> comments;
}
