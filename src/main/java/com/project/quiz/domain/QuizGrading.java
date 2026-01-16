package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_grading")
@Getter @Setter
public class QuizGrading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradingId;

    /** 채점 대상 답안 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private QuizAnswer answer;


    /** 정답 여부 */
    @Column(nullable = false)
    private Boolean correct;

    /** 획득 점수 */
    @Column(nullable = false)
    private Integer score;

    /** 채점자 */
    @Column(length = 20)
    private String grader;

    /** 채점 시각 */
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    
}
