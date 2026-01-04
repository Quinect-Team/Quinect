package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_answer")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    /* 어떤 제출에 속한 답안인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private QuizSubmission submission;

    /* 어떤 문제에 대한 답안인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    /* 서술형 답안 */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    /* 객관식 선택 번호 */
    @Column(name = "selected_option")
    private Integer selectedOption;
    
    @OneToOne(mappedBy = "answer", cascade = CascadeType.ALL)
    private QuizGrading grading;
    
    public void setGrading(QuizGrading grading) {
        this.grading = grading;
        grading.setAnswer(this);
    }


}
