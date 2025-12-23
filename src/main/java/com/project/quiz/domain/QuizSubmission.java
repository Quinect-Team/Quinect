package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "quiz_submission")
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;

    /* 어떤 퀴즈에 대한 제출인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /* 제출자 (비회원이면 nullable 가능) */
    @Column(name = "user_id")
    private Long userId;

    /* 총점 (채점 후 계산) */
    @Column(name = "total_score")
    private Integer totalScore;

    /* 채점 완료 여부 */
    @Column(nullable = false)
    private Boolean graded = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    /* 문제별 답안 */
    @OneToMany(
        mappedBy = "submission",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<QuizAnswer> answers = new ArrayList<>();
}
